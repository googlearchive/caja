// Copyright (C) 2009 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.caja.plugin.templates;

import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.HtmlTextEscapingMode;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.html.AttribKey;
import com.google.caja.parser.html.ElKey;
import com.google.caja.parser.html.Namespaces;
import com.google.caja.parser.html.Nodes;
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.Declaration;
import com.google.caja.parser.js.Directive;
import com.google.caja.parser.js.DirectivePrologue;
import com.google.caja.parser.js.Expression;
import com.google.caja.parser.js.FunctionConstructor;
import com.google.caja.parser.js.Identifier;
import com.google.caja.parser.js.Noop;
import com.google.caja.parser.js.Reference;
import com.google.caja.parser.js.Statement;
import com.google.caja.parser.js.StringLiteral;
import com.google.caja.parser.js.SyntheticNodes;
import com.google.caja.parser.js.TranslatedCode;
import com.google.caja.plugin.ExtractedHtmlContent;
import com.google.caja.plugin.PluginMeta;
import com.google.caja.reporting.MessageContext;
import com.google.caja.util.Lists;
import com.google.caja.util.Maps;
import com.google.caja.util.Pair;
import com.google.caja.util.Sets;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

/**
 * Produces safe static HTML from a DOM tree that has been compiled by the
 * {@link TemplateCompiler}.
 * This class emits two parse trees: safe HTML that is safe stand-alone, and
 * a number of blocks of Valija/Cajita which will add dynamic attributes to the
 * static HTML interspersed with extracted scripts.
 *
 * <h3>Glossary</h3>
 * <dl>
 *   <dt>Safe HTML</dt>
 *     <dd>HTML without event handlers or script, or names that can only be
 *     resolved on the browser.  This will include styles and classNames, and
 *     when {@link PluginMeta#getIdClass()} is specified, it will also include
 *     style-sheets and most IDs which is enough for a fully styled static view.
 *     </dd>
 *   <dt>Inline Script</dt>
 *     <dd>A script block as extracted by {@link ExtractedHtmlContent} that
 *     needs to run in the context of the portion of the static HTML that
 *     precedes it.  Scripts need to happen in the right context when they
 *     generate new content as in
 *     <xmp><ul><li>Item 1</li><script>emitItem2()</script><li>Item 3</ul></xmp>
 *     </dd>
 *   <dt>Skeleton</dt>
 *     <dd>A distilled version of the safe HTML that includes only elements,
 *     text, and document fragments.</dd>
 *   <dt>Bones</dt>
 *     <dd>References to the nodes in the skeleton in DF order with inline
 *     scripts.</dd>
 *   <dt>Static Attribute</dt>
 *     <dd>Attributes that can be rewritten server-side and included in the
 *     safe HTML.</dd>
 *   <dt>Dynamic Attribute</dt>
 *     <dd>Attributes that cannot be rewritten server-side or cannot be included
 *     in the safe HTML, and so which need to be attached by javascript.</dd>
 *   <dt>Auto-generated ID</dt>
 *     <dd>An auto-generated ID attached statically to a node in the Safe HTML
 *     so that javascript can find the node later and attach dynamic attributes.
 *   <dt>HTML Emitter</dt>
 *     <dd>A class that helps attach dynamic attributes and which provides
 *     an {@code attach} that is used to make sure that inline scripts only see
 *     the relevant bit of the DOM.</dd>
 * </dl>
 *
 * @author mikesamuel@gmail.com
 */
final class SafeHtmlMaker {
  private static final AttribKey ID = AttribKey.forHtmlAttrib(
      ElKey.HTML_WILDCARD, "id");

  private final PluginMeta meta;
  private final MessageContext mc;
  private final Document doc;
  private final List<Block> js = Lists.newArrayList();
  private final Map<Node, ParseTreeNode> scriptsPerNode;
  private final List<Node> roots;
  private final Map<String, Declaration> handlers = Maps.newHashMap();
  private final List<Statement> unnamedHandlers = Lists.newArrayList();
  /** The set of handlers defined in the current module. */
  private final Set<String> handlersUsedInModule = Sets.newHashSet();
  private Block currentBlock = null;
  /** True iff the current block is in a {@link TranslatedCode} section. */
  private boolean currentBlockStyle;
  /** True iff the HTML emitter has been invoked to split up the static HTML. */
  private boolean started = false;
  /** True iff JS contains the definitions required by HtmlEmitter calls. */
  private boolean moduleDefs = false;
  /** True iff JS contains a HtmlEmitter.finish() call to release resources. */
  private boolean finished = false;
  /**
   * @param doc the owner document for the safe HTML. Used only as a
   * factory for DOM nodes.
   */
  SafeHtmlMaker(PluginMeta meta, MessageContext mc, Document doc,
                Map<Node, ParseTreeNode> scriptsPerNode,
                List<Node> roots, List<Statement> handlers) {
    this.meta = meta;
    this.mc = mc;
    this.doc = doc;
    this.scriptsPerNode = scriptsPerNode;
    this.roots = roots;
    for (Statement handler : handlers) {
      if (handler instanceof Declaration) {
        Declaration d = (Declaration) handler;
        this.handlers.put(d.getIdentifierName(), d);
      } else {
        unnamedHandlers.add(handler);
      }
    }
  }

  Pair<Node, List<Block>> make(Pair<Statement, Element> css) {
    js.clear();
    currentBlock = null;

    if (css.a != null) { emitStatement(css.a, true); }

    for (Statement handler : unnamedHandlers) { emitHandler(handler); }

    // Build the HTML and the javascript that adds dynamic attributes and that
    // executes inline scripts.

    // First we build a skeleton which maps a safe DOM to a list of "bones"
    // which include element start tags, text nodes, and embedded scripts in
    // depth-first order.
    List<DomBone> domSkeleton = Lists.newArrayList();
    List<Node> safe = Lists.newArrayList(roots.size() + 1);
    for (Node root : roots) {
      Node one = makeSkeleton(root, domSkeleton);
      if (one != null) { safe.add(one); }
    }

    fleshOutSkeleton(domSkeleton);

    Node safeHtml = consolidateHtml(safe);
    if (css.b != null) {
      if (safeHtml instanceof DocumentFragment) {
        safeHtml.insertBefore(css.b, safeHtml.getFirstChild());
      } else {
        safeHtml = consolidateHtml(Arrays.asList(css.b, safeHtml));
      }
    }
    return Pair.pair(safeHtml, Lists.newArrayList(js));
  }

  /** Part of a DOM skeleton. */
  private static class DomBone {}

  private static class NodeBone extends DomBone {
    final Node node;
    final Node safeNode;
    NodeBone(Node node, Node safeNode) {
      this.node = node;
      this.safeNode = safeNode;
    }
    @Override
    public String toString() {
      return "(" + getClass().getSimpleName()
          + " " + safeNode.getNodeName() + ")";
    }
  }

  private static class ScriptBone extends DomBone {
    final Block script;
    ScriptBone(Block script) {
      this.script = script;
    }
    @Override
    public String toString() {
      return "(" + getClass().getSimpleName() + ")";
    }
  }

  /**
   * Produces a skeletal static HTML tree containing only Elements, Text nodes
   * and DocumentFragments, and a set of "bones" including the elements and
   * extracted script elements in depth-first order.
   *
   * <xmp> <ul> <li>Hello <script>foo()</script> Bar</li> </ul> </xmp>
   * results in
   * {@code ((Node UL) (Node LI) (Text "Hello ") (Script foo()) (Text " Bar"))}.
   */
  private Node makeSkeleton(Node n, List<DomBone> bones) {
    if (!scriptsPerNode.containsKey(n)) { return null; }
    Node safe;
    switch (n.getNodeType()) {
      case Node.ELEMENT_NODE:
        Element el = (Element) n;
        Block script = ExtractedHtmlContent.getExtractedScriptFor(el);
        if (script != null) {
          bones.add(new ScriptBone(script));
          return null;
        } else {
          FilePosition pos = Nodes.getFilePositionFor(el);
          safe = doc.createElementNS(el.getNamespaceURI(), el.getTagName());
          Nodes.setFilePositionFor(safe, pos);
          bones.add(new NodeBone(n, safe));

          for (Node child : Nodes.childrenOf(el)) {
            Node safeChild = makeSkeleton(child, bones);
            if (safeChild != null) { safe.appendChild(safeChild); }
          }
        }
        break;
      case Node.TEXT_NODE:
        safe = doc.createTextNode(n.getNodeValue());
        Nodes.setFilePositionFor(safe, Nodes.getFilePositionFor(n));
        bones.add(new NodeBone(n, safe));
        break;
      case Node.DOCUMENT_FRAGMENT_NODE:
        safe = doc.createDocumentFragment();
        for (Node child : Nodes.childrenOf(n)) {
          Node safeChild = makeSkeleton(child, bones);
          if (safeChild != null) { safe.appendChild(safeChild); }
        }
        break;
      default: return null;
    }
    return safe;
  }

  /**
   * Walks the {@link #makeSkeleton skeleton}, adds static attributes, and
   * auto-generated IDs to the skeleton, and generates Javascript that adds
   * dynamic attributes to the static HTML and that executes inline scripts.
   */
  private void fleshOutSkeleton(List<DomBone> bones) {
    int n = bones.size();
    // The index of the first script bone not followed by any non-script bones.
    int firstDeferredScriptIndex = n;
    while (firstDeferredScriptIndex > 0
           && bones.get(firstDeferredScriptIndex - 1) instanceof ScriptBone) {
      --firstDeferredScriptIndex;
    }
    for (int i = 0; i < n; ++i) {
      DomBone bone = bones.get(i);
      if (bone instanceof ScriptBone) {
        fleshOutScriptBlock(((ScriptBone) bone).script);
      } else {
        NodeBone nb = (NodeBone) bone;
        boolean splitDom = i + 1 < n && bones.get(i + 1) instanceof ScriptBone
            && i + 1 != firstDeferredScriptIndex;
        if (splitDom) { requireModuleDefs(); }
        if (nb.node instanceof Text) {
          if (splitDom) { insertPlaceholderAfter(nb.safeNode); }
        } else {
          fleshOutElement((Element) nb.node, (Element) nb.safeNode, splitDom);
        }
        if (i + 1 == firstDeferredScriptIndex) {
          finish();
        }
      }
    }
    finish();
    signalLoaded();
  }

  /** Define bits needed by the emitter calls and the attribute fixup. */
  private void requireModuleDefs() {
    if (!moduleDefs) {
      emitStatement(quasiStmt("var el___;"));
      emitStatement(quasiStmt("var emitter___ = IMPORTS___.htmlEmitter___;"));
      started = moduleDefs = true;
    }
  }

  /** Release resources held by the emitter. */
  private void finish() {
    if (started && !finished) {
      requireModuleDefs();
      emitStatement(quasiStmt("el___ = emitter___./*@synthetic*/finish();"));
      finished = true;
    }
  }

  /** Call the document's "onload" listeners. */
  private void signalLoaded() {
    if (moduleDefs) {
      emitStatement(quasiStmt("emitter___./*@synthetic*/signalLoaded();"));
    } else if (!js.isEmpty()) {
      emitStatement(quasiStmt(
          "IMPORTS___.htmlEmitter___./*@synthetic*/signalLoaded();"));
    }
  }

  /** Emit an inlined script. */
  private void fleshOutScriptBlock(Block script) {
    FilePosition unk = FilePosition.UNKNOWN;

    String sourcePath = mc.abbreviate(script.getFilePosition().source());
    finishBlock();
    emitStatement(quasiStmt(
        ""
        + "try {"
        + "  @scriptBody;"
        + "} catch (ex___) {"
        + "  ___./*@synthetic*/ getNewModuleHandler()"
        // getNewModuleHandler is appropriate here since there can't be multiple
        // module handlers in play while loadModule is being called, and all
        // these exception handlers are only reachable while control is in
        // loadModule.
        + "      ./*@synthetic*/ handleUncaughtException("
        + "          ex___, onerror, @sourceFile, @line);"
        + "}",
        // TODO(ihab.awad): Will add UncajoledModule wrapper when we no longer
        // "consolidate" all scripts in an HTML file into one Caja module.
        "scriptBody", script,
        "sourceFile", StringLiteral.valueOf(unk, sourcePath),
        "line", StringLiteral.valueOf(
            unk, String.valueOf(script.getFilePosition().startLineNo()))
        ), false);
  }

  /**
   * Emit a placeholder so that we can break before a script element that is
   * not immediately preceded by an element in the skeleton.
   * @param preceder a node that is safe, e.g. not in a context where it would
   *     cause code to execute.
   */
  private void insertPlaceholderAfter(Node preceder) {
    String dynId = meta.generateUniqueName(ID.localName);
    emitStatement(quasiStmt(
        ""
        + "emitter___./*@synthetic*/discard("
        + "    emitter___./*@synthetic*/attach(@id));",
        "id", StringLiteral.valueOf(FilePosition.UNKNOWN, dynId)));

    Node follower = preceder.getNextSibling();
    Node parent = preceder.getParentNode();
    if (containsOnlyText(parent)) {
      // Elements like <textarea> and <pre> can't contain elements, so move
      // the placeholder into the parent, where it will be in the same
      // position in the depth-first-order traversal.

      // <textarea>s should contain at most a single text nodes since they can't
      // contain elements.  The DOM skeleton is normalized as it is constructed.
      assert follower == null;

      follower = parent.getNextSibling();
      parent = parent.getParentNode();

      // The new parent must be able to accept the text node since it contained
      // an element.
      assert !containsOnlyText(parent);
    }

    Element placeholder = doc.createElementNS(
        Namespaces.HTML_NAMESPACE_URI, "span");
    placeholder.setAttributeNS(
        Namespaces.HTML_NAMESPACE_URI, ID.localName, dynId);
    parent.insertBefore(placeholder, follower);
  }

  private static boolean containsOnlyText(Node n) {
    if (!(n instanceof Element)) { return false; }
    Element el = (Element) n;
    ElKey schemaKey = ElKey.forElement(el);
    if (schemaKey.isHtml()) {
      HtmlTextEscapingMode mode = HtmlTextEscapingMode.getModeForTag(
          schemaKey.localName);
      return mode != HtmlTextEscapingMode.PCDATA;
    } else {
      return false;
    }
  }

  /**
   * Attaches attributes to the safe DOM node corresponding to those on el.
   *
   * @param el an element in the input DOM.
   * @param safe the element in the safe HTML DOM corresponding to el.
   * @param splitDom true if this text node is immediately followed by an inline
   *     script block.
   */
  private void fleshOutElement(Element el, Element safe, boolean splitDom) {
    FilePosition pos = Nodes.getFilePositionFor(el);

    // An ID we attach to a node so that we can retrieve it to add dynamic
    // attributes later.
    String dynId = null;
    if (splitDom) {
      dynId = makeDynamicId(null, pos);
      // Emit first since this makes sure the node is in the DOM.
      emitStatement(quasiStmt(
          "emitter___./*@synthetic*/attach(@id);",
          "id", StringLiteral.valueOf(FilePosition.UNKNOWN, dynId)));
    }
    Nodes.setFilePositionFor(safe, pos);

    ElKey elKey = ElKey.forElement(el);
    Attr id = null;
    for (Attr a : Nodes.attributesOf(el)) {
      if (!scriptsPerNode.containsKey(a)) { continue; }
      AttribKey attrKey = AttribKey.forAttribute(elKey, a);
      // Keep track of whether there is an ID so that we know whether or
      // not to remove any auto-generated ID later.
      Expression dynamicValue = (Expression) scriptsPerNode.get(a);
      if (ID.ns.uri != attrKey.ns.uri
          || !ID.localName.equals(attrKey.localName)) {
        if (dynamicValue == null
            || dynamicValue instanceof StringLiteral) {
          emitStaticAttr(a, (StringLiteral) dynamicValue, safe);
        } else {
          dynId = makeDynamicId(dynId, pos);
          String handlerName = dynamicValue.getAttributes().get(
              HtmlAttributeRewriter.HANDLER_NAME);
          if (handlerName != null
              && handlers.containsKey(handlerName)
              && handlersUsedInModule.add(handlerName)) {
            emitHandler(handlerName);
          }
          emitDynamicAttr(a, dynamicValue);
        }
      } else {
        // A previous HTML parsing step should have ensured that each element
        // only has one instance of each attribute.
        assert id == null;
        id = a;
      }
    }
    // Output an ID
    if (id != null) {
      Expression dynamicValue = (Expression) scriptsPerNode.get(id);
      if (dynId == null
          && (dynamicValue == null
              || dynamicValue instanceof StringLiteral)) {
        emitStaticAttr(id, (StringLiteral) dynamicValue, safe);
      } else {
        dynId = makeDynamicId(dynId, pos);
        emitDynamicAttr(id, dynamicValue);
      }
    }

    // Remove the dynamic ID if it is still present.
    if (dynId != null) {
      assert !safe.hasAttributeNS(ID.ns.uri, ID.localName);
      safe.setAttributeNS(ID.ns.uri, ID.localName, dynId);
      if (id == null) {
        emitStatement(quasiStmt("el___./*@synthetic*/removeAttribute('id');"));
      }
    }
  }

  private void emitStaticAttr(
      Attr a, StringLiteral dynamicValue, Element safe) {
    // Emit an attribute with a known value in the safe HTML.
    Attr safeAttr = doc.createAttributeNS(a.getNamespaceURI(), a.getName());
    safeAttr.setValue(
        dynamicValue == null ? a.getValue() : dynamicValue.getUnquotedValue());
    Nodes.setFilePositionFor(safeAttr, Nodes.getFilePositionFor(a));
    Nodes.setFilePositionForValue(safeAttr, Nodes.getFilePositionForValue(a));
    safe.setAttributeNodeNS(safeAttr);
  }

  private void emitDynamicAttr(Attr a, Expression dynamicValue) {
    FilePosition pos = Nodes.getFilePositionFor(a);
    String name = a.getName();
    // Emit a statement to attach the dynamic attribute.
    if (dynamicValue instanceof FunctionConstructor) {
      emitStatement(quasiStmt(
          "el___.@name = @eventAdapter;",
          "name", new Reference(SyntheticNodes.s(new Identifier(pos, name))),
          "eventAdapter", dynamicValue));
    } else {
      emitStatement(quasiStmt(
          "emitter___./*@synthetic*/setAttr(el___, @name, @value);",
          "name", StringLiteral.valueOf(pos, name),
          "value", dynamicValue));
    }
    // TODO(mikesamuel): do we need to emit a static attribute when the
    // default value does not match the value criterion?
  }

  private String makeDynamicId(String dynId, FilePosition pos) {
    requireModuleDefs();
    if (dynId == null) {
      // We need a dynamic ID so that we can find the node so that
      // we can attach dynamic attributes.
      dynId = meta.generateUniqueName(ID.localName);
      emitStatement(quasiStmt(
          "el___ = emitter___./*@synthetic*/byId(@id);",
          "id", StringLiteral.valueOf(pos, dynId)));
    }
    assert isDynamicId(dynId);
    return dynId;
  }

  private static final Pattern DYNID_PATTERN = Pattern.compile("id_\\d+___");
  private static boolean isDynamicId(String id) {
    return DYNID_PATTERN.matcher(id).matches();
  }

  private static Statement quasiStmt(String quasi, Object... args) {
    return QuasiUtil.quasiStmt(quasi, args);
  }

  private void emitStatement(Statement s) { emitStatement(s, true); }

  private void emitStatement(Statement s, boolean translated) {
    if (translated != currentBlockStyle) {
      currentBlock = null;
    }
    if (currentBlock == null) {
      handlersUsedInModule.clear();
      Block block = new Block();
      js.add(block);
      if (translated) {
        // May be downgraded based on emitHandler below.
        block.appendChild(new DirectivePrologue(
            FilePosition.UNKNOWN,
            Lists.newArrayList(new Directive(
                FilePosition.UNKNOWN, "use cajita"))));
        TranslatedCode code = new TranslatedCode(currentBlock = new Block());
        block.appendChild(code);
      } else {
        currentBlock = block;
      }
      currentBlockStyle = translated;
      moduleDefs = false;
    }
    currentBlock.appendChild(s);
  }

  private void emitHandler(String handlerName) {
    emitHandler(handlers.get(handlerName));
  }

 private void emitHandler(Statement handler) {
    emitStatement(handler, true);
    if (hasNonStrictFn(handler)) {
      Block block = js.get(js.size() - 1);
      Statement s = block.children().get(0);
      // Do not put a block in cajita mode when we're outputting a non-strict
      // handler.
      if (s instanceof DirectivePrologue) {  // Added in emitStatement.
        // TODO(mikesamuel): can we get rid of this noop by getting rid of the
        // silly $v.initOuter('onerror') in DefaultValijaRewriter?
        // Can that move into valija-cajita.js.
        block.replaceChild(new Noop(s.getFilePosition()), s);
      }
    }
  }

  private Node consolidateHtml(List<Node> nodes) {
    if (nodes.isEmpty()) { return doc.createDocumentFragment(); }
    Node first = nodes.get(0);
    List<Node> rest = nodes.subList(1, nodes.size());
    if (rest.isEmpty()) { return first; }
    FilePosition pos = Nodes.getFilePositionFor(first);
    DocumentFragment f;
    if (first instanceof DocumentFragment) {
      f = (DocumentFragment) first;
    } else {
      f = doc.createDocumentFragment();
      f.appendChild(first);
    }
    for (Node one : rest) {
      pos = FilePosition.span(pos, Nodes.getFilePositionFor(one));
      if (one instanceof DocumentFragment) {
        for (Node c = one.getFirstChild(), next; c != null; c = next) {
          next = c.getNextSibling();
          f.appendChild(c);
        }
      } else {
        f.appendChild(one);
      }
    }
    Nodes.setFilePositionFor(f, pos);
    return f;
  }

  private void finishBlock() { currentBlock = null; }

  private static boolean hasNonStrictFn(ParseTreeNode n) {
    if (n instanceof FunctionConstructor) {
      Block body = ((FunctionConstructor) n).getBody();
      if (!body.children().isEmpty()) {
        Statement s0 = body.children().get(0);
        if (s0 instanceof DirectivePrologue) {
          DirectivePrologue dp = (DirectivePrologue) s0;
          for (Directive d : dp.children()) {
            if ("use cajita".equals(d.getDirectiveString())) { return false; }
          }
        }
      }
      return true;
    } else {
      for (ParseTreeNode child : n.children()) {
        if (hasNonStrictFn(child)) { return true; }
      }
      return false;
    }
  }
}
