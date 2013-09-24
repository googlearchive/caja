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

import com.google.caja.lang.html.HTML;
import com.google.caja.lang.html.HtmlSchema;
import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.HtmlTextEscapingMode;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.html.AttribKey;
import com.google.caja.parser.html.ElKey;
import com.google.caja.parser.html.Namespaces;
import com.google.caja.parser.html.Nodes;
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.CajoledModule;
import com.google.caja.parser.js.Declaration;
import com.google.caja.parser.js.Expression;
import com.google.caja.parser.js.FunctionConstructor;
import com.google.caja.parser.js.Identifier;
import com.google.caja.parser.js.Reference;
import com.google.caja.parser.js.Statement;
import com.google.caja.parser.js.StringLiteral;
import com.google.caja.parser.js.SyntheticNodes;
import com.google.caja.parser.js.TranslatedCode;
import com.google.caja.plugin.JobEnvelope;
import com.google.caja.plugin.Placeholder;
import com.google.caja.plugin.PluginMeta;
import com.google.caja.plugin.stages.JobCache;
import com.google.caja.reporting.MessageContext;
import com.google.caja.util.Lists;
import com.google.caja.util.Pair;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

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
 * a number of blocks of synthetic JS which will add dynamic attributes to the
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
 *     <dd>A script block as specified by a {@link Placeholder} that
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

  /**
   * A source for content that doesn't correspond to any input jobs but which
   * may bracket the processed output jobs.
   */
  private static final JobEnvelope SYNTHETIC_SOURCE
      = new JobEnvelope(null, JobCache.none(), null, false, null);

  private final PluginMeta meta;
  private final HtmlSchema htmlSchema;
  private final MessageContext mc;
  private final Document doc;
  private final List<SafeJsChunk> js = Lists.newArrayList();
  private final Map<Node, ParseTreeNode> scriptsPerNode;
  private final Map<String, ScriptPlaceholder> scriptsPerPlaceholder;
  private final List<IhtmlRoot> roots;
  private final Map<String, Pair<JobEnvelope, Declaration>> handlers
      = Maps.newHashMap();
  private final List<EventHandler> unnamedHandlers = Lists.newArrayList();
  /** The set of handlers defined in the current module. */
  private final Set<String> handlersUsedInModule = Sets.newHashSet();
  private List<Statement> currentBlock = null;
  /** True iff the current block is in a {@link TranslatedCode} section. */
  private boolean currentBlockTranslated;
  /** The cache keys for the current block. */
  private JobEnvelope currentSource;
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
  SafeHtmlMaker(PluginMeta meta, HtmlSchema htmlSchema, MessageContext mc,
                Document doc, Map<Node, ParseTreeNode> scriptsPerNode,
                Map<String, ScriptPlaceholder> scriptsPerPlaceholder,
                List<IhtmlRoot> roots,
                List<EventHandler> handlers) {
    this.meta = meta;
    this.htmlSchema = htmlSchema;
    this.mc = mc;
    this.doc = doc;
    this.scriptsPerNode = scriptsPerNode;
    this.scriptsPerPlaceholder = scriptsPerPlaceholder;
    this.roots = roots;
    for (EventHandler handler : handlers) {
      if (handler.handler instanceof Declaration) {
        Declaration d = (Declaration) handler.handler;
        this.handlers.put(d.getIdentifierName(), Pair.pair(handler.source, d));
      } else {
        unnamedHandlers.add(handler);
      }
    }
  }

  Pair<List<SafeHtmlChunk>, List<SafeJsChunk>> make(List<SafeStylesheet> css) {
    js.clear();
    currentBlock = null;

    List<SafeHtmlChunk> safe = Lists.newArrayList(
        roots.size() + css.size() + 1);
    for (SafeStylesheet ss : css) {
      if (ss.jsVersion != null) {
        emitStatement(ss.jsVersion, true, ss.source);
      } else if (ss.htmlVersion != null) {
        safe.add(new SafeHtmlChunk(ss.source,
            wrapInDocumentFragment(ss.htmlVersion), ss.baseUri));
      }
    }

    for (EventHandler handler : unnamedHandlers) {
      emitHandler(handler.handler, handler.source);
    }

    // Build the HTML and the javascript that adds dynamic attributes and that
    // executes inline scripts.

    // First we build a skeleton which maps a safe DOM to a list of "bones"
    // which include element start tags, text nodes, and embedded scripts in
    // depth-first order.
    List<DomBone> domSkeleton = Lists.newArrayList();
    for (IhtmlRoot root : roots) {
      DocumentFragment one = (DocumentFragment)makeSkeleton(
          root.root, root.source, domSkeleton);
      if (one != null) {
        safe.add(new SafeHtmlChunk(root.source, one, root.baseUri));
      }
    }

    fleshOutSkeleton(domSkeleton);
    finishBlock();

    return Pair.pair(safe, Lists.newArrayList(js));
  }

  /** Part of a DOM skeleton. */
  private static class DomBone {
    final JobEnvelope source;

    DomBone(JobEnvelope source) {
      this.source = source;
    }
  }

  private static final class NodeBone extends DomBone {
    final Node node;
    final Node safeNode;

    NodeBone(JobEnvelope source, Node node, Node safeNode) {
      super(source);
      this.node = node;
      this.safeNode = safeNode;
    }

    @Override
    public String toString() {
      return "(" + getClass().getSimpleName()
          + " " + safeNode.getNodeName() + ")";
    }
  }

  private static final class ScriptBone extends DomBone {
    final ParseTreeNode body;

    ScriptBone(JobEnvelope source, ParseTreeNode body) {
      super(source);
      this.body = body;
    }

    @Override
    public String toString() {
      return "(" + getClass().getSimpleName() + ")";
    }
  }

  /** Marks the end of the document. */
  private static final class TailBone extends DomBone {
    TailBone(JobEnvelope source) {
      super(source);
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
  private Node makeSkeleton(Node n, JobEnvelope src, List<DomBone> bones) {
    String placeholderId = getPlaceholderId(n);
    if (placeholderId != null) {
      if ("finish".equals(placeholderId)) {
        bones.add(new TailBone(src));
      } else {
        ScriptPlaceholder ph = scriptsPerPlaceholder.get(placeholderId);
        bones.add(new ScriptBone(ph.source, ph.body));
      }
      return null;
    }

    if (!scriptsPerNode.containsKey(n)) { return null; }
    Node safe;
    switch (n.getNodeType()) {
      case Node.ELEMENT_NODE:
        Element el = (Element) n;
        FilePosition pos = Nodes.getFilePositionFor(el);
        safe = doc.createElementNS(el.getNamespaceURI(), el.getTagName());
        bones.add(new NodeBone(src, n, safe));

        for (Node child : Nodes.childrenOf(el)) {
          Node safeChild = makeSkeleton(child, src, bones);
          if (safeChild != null) { safe.appendChild(safeChild); }
        }
        Nodes.setFilePositionFor(safe, pos);
        break;
      case Node.TEXT_NODE:
        String text = n.getNodeValue();
        if ("".equals(text)) { return null; }
        safe = doc.createTextNode(text);
        Nodes.setFilePositionFor(safe, Nodes.getFilePositionFor(n));
        bones.add(new NodeBone(src, n, safe));
        break;
      case Node.DOCUMENT_FRAGMENT_NODE:
        safe = doc.createDocumentFragment();
        for (Node child : Nodes.childrenOf(n)) {
          Node safeChild = makeSkeleton(child, src, bones);
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
    for (int i = 0, n = bones.size(); i < n; ++i) {
      DomBone bone = bones.get(i);
      if (bone instanceof ScriptBone) {
        fleshOutScriptBlock((ScriptBone) bone);
      } else if (bone instanceof TailBone) {
        finish();
        continue;
      } else {
        NodeBone nb = (NodeBone) bone;
        boolean splitDom = i + 1 < n && bones.get(i + 1) instanceof ScriptBone;
        if (splitDom) { requireModuleDefs(bone.source); }
        if (nb.node instanceof Text) {
          if (splitDom) { insertPlaceholderAfter(nb); }
        } else {
          fleshOutElement(nb, splitDom);
        }
      }
    }
    finish();
    signalLoaded();
  }

  /** Define bits needed by the emitter calls and the attribute fixup. */
  private void requireModuleDefs(JobEnvelope source) {
    maybeBreakBlock(true, source);
    if (!moduleDefs) {
      emitStatement(quasiStmt("var el___;"), source);
      emitStatement(
          quasiStmt("var emitter___ = IMPORTS___.htmlEmitter___;"), source);
      started = moduleDefs = true;
    }
  }

  /** Release resources held by the emitter. */
  private void finish() {
    if (started && !finished) {
      requireModuleDefs(SYNTHETIC_SOURCE);
      emitStatement(
          quasiStmt("el___ = emitter___./*@synthetic*/finish();"),
          SYNTHETIC_SOURCE);
      finished = true;
    }
  }

  private void signalLoaded() {
    maybeBreakBlock(true, SYNTHETIC_SOURCE);
    if (moduleDefs) {
      emitStatement(
          quasiStmt("emitter___./*@synthetic*/signalLoaded();"),
          SYNTHETIC_SOURCE);
    } else if (!js.isEmpty()) {
      emitStatement(
          quasiStmt("IMPORTS___.htmlEmitter___./*@synthetic*/signalLoaded();"),
          SYNTHETIC_SOURCE);
    }
  }

  /** Emit an inlined script. */
  private void fleshOutScriptBlock(ScriptBone bone) {
    FilePosition unk = FilePosition.UNKNOWN;

    FilePosition pos = bone.body.getFilePosition();
    String sourcePath = mc.abbreviate(pos.source());
    if (bone.source.fromCache) {
      CajoledModule scriptFromCache = (CajoledModule) bone.body;
      finishBlock();
      this.js.add(new SafeJsChunk(bone.source, scriptFromCache));
    } else {
      Block scriptToWrapAndProcess = (Block) bone.body;
      emitStatement(quasiStmt(
          ""
          + "try {"
          + "  @scriptBody;"
          + "} catch (ex___) {"
          + "  ___./*@synthetic*/ getNewModuleHandler()"
          // getNewModuleHandler is appropriate since there can't be multiple
          // module handlers in play while loadModule is being called, and all
          // these exception handlers are only reachable while control is in
          // loadModule.
          + "      ./*@synthetic*/ handleUncaughtException("
          + "          ex___, onerror, @sourceFile, @line);"
          + "}",
          // TODO(ihab.awad): Will add UncajoledModule wrapper when we no longer
          // "consolidate" all scripts in an HTML file into one Caja module.
          "scriptBody", scriptToWrapAndProcess,
          "sourceFile", StringLiteral.valueOf(unk, sourcePath),
          "line", StringLiteral.valueOf(unk, String.valueOf(pos.startLineNo()))
          ),
          false,
          bone.source);
    }
  }

  /**
   * Emit a placeholder so that we can break before a script element that is
   * not immediately preceded by an element in the skeleton.
   * @param preceder a node that is safe, e.g. not in a context where it would
   *     cause code to execute.
   */
  private void insertPlaceholderAfter(NodeBone preceder) {
    String dynId = meta.generateUniqueName(ID.localName);
    emitStatement(quasiStmt(
        ""
        + "emitter___./*@synthetic*/discard("
        + "    emitter___./*@synthetic*/attach(@id));",
        "id", StringLiteral.valueOf(FilePosition.UNKNOWN, dynId)),
        preceder.source);

    Node follower = preceder.safeNode.getNextSibling();
    Node parent = preceder.safeNode.getParentNode();
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
   * @param bone wraps an element in the input DOM and the element in the safe
   *     HTML DOM corresponding to el.
   * @param splitDom true if this text node is immediately followed by an inline
   *     script block.
   */
  private void fleshOutElement(NodeBone bone, boolean splitDom) {
    Element el = (Element) bone.node;
    Element safe = (Element) bone.safeNode;
    FilePosition pos = Nodes.getFilePositionFor(el);
    ElKey elKey = ElKey.forElement(el);

    // If the element is supposed to have a FRAME_TARGET attribute, set that
    // to a safe value (the actual final value of all FRAME_TARGET attributes
    // will be assigned dynamically).
    HTML.Element elInfo = htmlSchema.lookupElement(elKey);
    List<HTML.Attribute> attrs = elInfo.getAttributes();
    if (attrs != null) {
      for (HTML.Attribute a : attrs) {
        if (a.getType() == HTML.Attribute.Type.FRAME_TARGET) {
          String safeValue =
              (a.getDefaultValue() != null
               && a.getValueCriterion().accept(a.getDefaultValue()))
              ? a.getDefaultValue() : a.getSafeValue();
          emitStaticAttr(
              a,
              new StringLiteral(FilePosition.UNKNOWN, safeValue),
              safe);
          Attr attr = el.getOwnerDocument().createAttributeNS(
              elInfo.getKey().ns.uri, elInfo.getKey().localName);
          el.setAttributeNode(attr);
        }
      }
    }

    // An ID we attach to a node so that we can retrieve it to add dynamic
    // attributes later.
    String dynId = null;
    if (splitDom) {
      dynId = makeDynamicId(null, pos, bone.source);
      // Emit first since this makes sure the node is in the DOM.
      emitStatement(quasiStmt(
          "emitter___./*@synthetic*/attach(@id);",
          "id", StringLiteral.valueOf(FilePosition.UNKNOWN, dynId)),
          bone.source);
    }
    Nodes.setFilePositionFor(safe, pos);

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
          dynId = makeDynamicId(dynId, pos, bone.source);
          String handlerName = dynamicValue.getAttributes().get(
              HtmlAttributeRewriter.HANDLER_NAME);
          if (handlerName != null
              && handlers.containsKey(handlerName)
              && handlersUsedInModule.add(handlerName)) {
            emitHandler(handlerName);
          }
          emitDynamicAttr(a, dynamicValue, bone.source);
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
        dynId = makeDynamicId(dynId, pos, bone.source);
        emitDynamicAttr(id, dynamicValue, bone.source);
      }
    }

    // Remove the dynamic ID if it is still present.
    if (dynId != null) {
      assert !safe.hasAttributeNS(ID.ns.uri, ID.localName);
      safe.setAttributeNS(ID.ns.uri, ID.localName, dynId);
      if (id == null) {
        emitStatement(
            quasiStmt("emitter___./*@synthetic*/rmAttr(el___, 'id');"),
            bone.source);
      }
    }
  }

  private void emitStaticAttr(
      HTML.Attribute a, StringLiteral dynamicValue, Element safe) {
    // Emit an attribute with a known value in the safe HTML.
    Attr safeAttr = doc.createAttributeNS(
        a.getKey().ns.uri, a.getKey().localName);
    safeAttr.setValue(dynamicValue.getUnquotedValue());
    safe.setAttributeNodeNS(safeAttr);
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

  private void emitDynamicAttr(
      Attr a, Expression dynamicValue, JobEnvelope source) {
    FilePosition pos = Nodes.getFilePositionFor(a);
    String name = a.getName();
    // Emit a statement to attach the dynamic attribute.
    if (dynamicValue instanceof FunctionConstructor) {
      emitStatement(
          quasiStmt(
              "el___.@name = @eventAdapter;",
              "name", new Reference(
                  SyntheticNodes.s(new Identifier(pos, name))),
              "eventAdapter", dynamicValue),
          source);
    } else {
      emitStatement(
          quasiStmt(
              "emitter___./*@synthetic*/setAttr(el___, @name, @value);",
              "name", StringLiteral.valueOf(pos, name),
              "value", dynamicValue),
          source);
    }
    // TODO(mikesamuel): do we need to emit a static attribute when the
    // default value does not match the value criterion?
  }

  private String makeDynamicId(
      String dynId, FilePosition pos, JobEnvelope source) {
    requireModuleDefs(source);
    if (dynId == null) {
      // We need a dynamic ID so that we can find the node so that
      // we can attach dynamic attributes.
      dynId = meta.generateUniqueName(ID.localName);
      emitStatement(
          quasiStmt(
              "el___ = emitter___./*@synthetic*/byId(@id);",
              "id", StringLiteral.valueOf(pos, dynId)),
          source);
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

  private void emitStatement(Statement s, JobEnvelope source) {
    emitStatement(s, true, source);
  }

  private void maybeBreakBlock(boolean translated, JobEnvelope source) {
    if (translated != currentBlockTranslated
        || !source.areFromSameSource(currentSource)) {
      finishBlock();
    }
  }

  private void emitStatement(Statement s, boolean translated, JobEnvelope src) {
    maybeBreakBlock(translated, src);
    if (currentBlock == null) {
      handlersUsedInModule.clear();
      currentBlock = Lists.newArrayList();
      currentSource = src;
      currentBlockTranslated = translated;
      moduleDefs = false;
    }
    currentBlock.add(s);
  }

  private void emitHandler(String handlerName) {
    Pair<JobEnvelope, Declaration> handler = handlers.get(handlerName);
    emitHandler(handler.b, handler.a);
  }

 private void emitHandler(Statement handler, JobEnvelope source) {
    emitStatement(handler, true, source);
  }

  private void finishBlock() {
    moduleDefs = false;
    if (currentBlock == null) { return; }

    Block block = new Block(FilePosition.UNKNOWN, currentBlock);
    if (currentBlockTranslated) {
      Block wrapper = new Block();
      wrapper.appendChild(new TranslatedCode(block));
      js.add(new SafeJsChunk(currentSource, wrapper));
    } else {
      js.add(new SafeJsChunk(currentSource, block));
    }
    currentBlock = null;
  }

  private static String getPlaceholderId(Node n) {
    if (n.getNodeType() != Node.ELEMENT_NODE) {
      return null;
    }
    if (!"span".equals(n.getLocalName())) {
      return null;
    }
    Attr a = ((Element) n).getAttributeNodeNS(
        Placeholder.ID_ATTR.ns.uri, Placeholder.ID_ATTR.localName);
    return a != null ? a.getValue() : null;
  }


  /**
   * Put an orphaned node into a DocumentFragment.
   */
  private static DocumentFragment wrapInDocumentFragment(Node node) {
    DocumentFragment wrapper = node.getOwnerDocument().createDocumentFragment();
    assert node.getParentNode() == null;
    wrapper.appendChild(node);
    return wrapper;
  }
}
