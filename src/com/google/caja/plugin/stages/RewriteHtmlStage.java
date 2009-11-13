// Copyright (C) 2007 Google Inc.
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

package com.google.caja.plugin.stages;

import com.google.caja.lang.css.CssSchema;
import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.CssTokenType;
import com.google.caja.lexer.ExternalReference;
import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.JsLexer;
import com.google.caja.lexer.JsTokenQueue;
import com.google.caja.lexer.ParseException;
import com.google.caja.lexer.TokenQueue;
import com.google.caja.parser.AncestorChain;
import com.google.caja.parser.MutableParseTreeNode;
import com.google.caja.parser.Visitor;
import com.google.caja.parser.css.CssParser;
import com.google.caja.parser.css.CssTree;
import com.google.caja.parser.html.Namespaces;
import com.google.caja.parser.html.Nodes;
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.Parser;
import com.google.caja.parser.js.StringLiteral;
import com.google.caja.plugin.Dom;
import com.google.caja.plugin.ExtractedHtmlContent;
import com.google.caja.plugin.Job;
import com.google.caja.plugin.Jobs;
import com.google.caja.plugin.PluginEnvironment;
import com.google.caja.plugin.PluginMessageType;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.util.Name;
import com.google.caja.util.Pipeline;
import com.google.caja.util.Strings;

import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

/**
 * Extract some unsafe bits from HTML for processing by later stages.
 * Specifically, extracts the contents of {@code <script>} elements,
 * and the contents of {@code <style>} elements, and the content
 * referred to by {@code <link rel=stylesheet>} elements.
 *
 * <p>
 * This stage is not responsible for producing a safe tree.  It is only meant to
 * allow later stages to do special handling of otherwise unsafe bits, by
 * rerouting pieces that would otherwise trigger alarms in
 * {@link SanitizeHtmlStage}.
 *
 * <p>
 * This stage runs *before* the HTML sanitizer.  As such, it should not assume
 * that the tree is normalized.  Specifically, DOM trees may reach this stage
 * that contain duplicate attributes; and unknown or unsafe elements and
 * attributes.
 *
 * @author mikesamuel@gmail.com
 */
public class RewriteHtmlStage implements Pipeline.Stage<Jobs> {

  public boolean apply(Jobs jobs) {
    for (Job job : jobs.getJobsByType(Job.JobType.HTML)) {
      Node root = ((Dom) job.getRoot().node).getValue();
      rewriteDomTree(root, root, jobs);
    }
    return jobs.hasNoFatalErrors();
  }

  private static final String HTML_NS = Namespaces.HTML_NAMESPACE_URI;

  void rewriteDomTree(Node root, Node n, Jobs jobs) {
    // Rewrite styles and scripts.
    // <script>foo()</script>  ->  <script>(cajoled foo)</script>
    // <style>foo { ... }</style>  ->  <style>foo { ... }</style>
    // <script src=foo.js></script>  ->  <script>(cajoled, inlined foo)</script>
    // <link rel=stylesheet href=foo.css>
    //     ->  <style>(cajoled, inlined styles)</style>
    if (n.getNodeType() == Node.ELEMENT_NODE) {
      Element el = (Element) n;
      if (HTML_NS.equals(el.getNamespaceURI())) {
        String name = el.getLocalName();
        if ("script".equals(name)) {
          rewriteScriptTag(root, el, jobs);
        } else if ("style".equals(name)) {
          rewriteStyleTag(el, jobs);
        } else if ("link".equals(name)) {
          rewriteLinkTag(el, jobs);
        } else if ("body".equals(name)) {
          moveOnLoadHandlerToEndOfBody(el);
        }
      }
    }
    for (Node child : Nodes.childrenOf(n)) {
      rewriteDomTree(root, child, jobs);
    }
  }

  private void rewriteScriptTag(Node root, Element scriptTag, Jobs jobs) {
    Node parent = scriptTag.getParentNode();
    PluginEnvironment env = jobs.getPluginMeta().getPluginEnvironment();

    Attr type = scriptTag.getAttributeNodeNS(HTML_NS, "type");
    Attr src = scriptTag.getAttributeNodeNS(HTML_NS, "src");
    if (type != null && !isJavaScriptContentType(type.getNodeValue())) {
      jobs.getMessageQueue().addMessage(
          PluginMessageType.UNRECOGNIZED_CONTENT_TYPE,
          Nodes.getFilePositionFor(type),
          MessagePart.Factory.valueOf(type.getNodeValue()),
          MessagePart.Factory.valueOf(scriptTag.getTagName()));
      parent.removeChild(scriptTag);
      return;
    }
    // The script contents.
    CharProducer jsStream;
    FilePosition scriptPos;
    if (src == null) {  // Parse the script tag body.
      jsStream = textNodesToCharProducer(Nodes.childrenOf(scriptTag), true);
      if (jsStream == null) {
        parent.removeChild(scriptTag);
        return;
      }
      scriptPos = FilePosition.span(
          Nodes.getFilePositionFor(scriptTag.getFirstChild()),
          Nodes.getFilePositionFor(scriptTag.getLastChild()));
    } else {  // Load the src attribute
      FilePosition srcPos = Nodes.getFilePositionFor(src);
      FilePosition srcValuePos = Nodes.getFilePositionForValue(src);
      URI srcUri;
      try {
        srcUri = new URI(src.getNodeValue());
      } catch (URISyntaxException ex) {
        jobs.getMessageQueue().getMessages().add(
            new Message(PluginMessageType.MALFORMED_URL, MessageLevel.ERROR,
                        srcPos, MessagePart.Factory.valueOf(src.getNodeName()))
            );
        parent.removeChild(scriptTag);
        return;
      }

      // Fetch the script source.
      URI absUri = srcValuePos.source().getUri()
          .resolve(srcUri);
      jobs.getMessageContext().addInputSource(new InputSource(absUri));
      jsStream = env.loadExternalResource(
          new ExternalReference(absUri, srcValuePos), "text/javascript");
      if (jsStream == null) {
        jobs.getMessageQueue().addMessage(
            PluginMessageType.FAILED_TO_LOAD_EXTERNAL_URL,
            srcValuePos, MessagePart.Factory.valueOf("" + srcUri));
        // Throw an exception so any user installed error handler will fire.
        jsStream = CharProducer.Factory.fromString(
            "throw new Error("
            + StringLiteral.toQuotedValue("Failed to load " + srcUri.toString())
            + ");",
            srcPos);
      }
      scriptPos = null;
    }

    // Parse the body and create a block that will be placed inline in
    // loadModule.
    Block parsedScriptBody;
    try {
      parsedScriptBody = parseJs(jsStream.getCurrentPosition().source(),
                                 jsStream, scriptPos, jobs.getMessageQueue());
    } catch (ParseException ex) {
      ex.toMessageQueue(jobs.getMessageQueue());
      parsedScriptBody = null;
    }

    if (parsedScriptBody == null) {
      parent.removeChild(scriptTag);
      return;
    }

    // Build a replacement element, <span/>, and link it to the extracted
    // javascript, so that when the DOM is rendered, we can properly interleave
    // the extract scripts with the scripts that generate markup.
    Element placeholder = parent.getOwnerDocument().createElementNS(
        HTML_NS, "span");
    Nodes.setFilePositionFor(placeholder, Nodes.getFilePositionFor(scriptTag));
    ExtractedHtmlContent.setExtractedScriptFor(
        placeholder, parsedScriptBody);

    // Replace the script tag with a placeholder that points to the inlined
    // script.
    if (Strings.equalsIgnoreCase(
            "defer", scriptTag.getAttributeNS(HTML_NS, "defer"))) {
      parent.removeChild(scriptTag);
      root.appendChild(placeholder);
    } else {
      parent.replaceChild(placeholder, scriptTag);
    }
  }

  private void rewriteStyleTag(Element styleTag, Jobs jobs) {
    styleTag.getParentNode().removeChild(styleTag);

    CharProducer cssStream = textNodesToCharProducer(
        Nodes.childrenOf(styleTag), false);
    if (cssStream != null) {
      extractStyles(styleTag, cssStream, null, jobs);
    }
  }

  private void rewriteLinkTag(Element styleTag, Jobs jobs) {
    PluginEnvironment env = jobs.getPluginMeta().getPluginEnvironment();

    styleTag.getParentNode().removeChild(styleTag);

    Attr rel = styleTag.getAttributeNodeNS(HTML_NS, "rel");
    if (rel == null || !Strings.equalsIgnoreCase(
        rel.getNodeValue().trim(), "stylesheet")) {
      // If it's not a stylesheet then ignore it.
      // The HtmlValidator should complain but that's not our problem.
      return;
    }

    Attr href = styleTag.getAttributeNodeNS(HTML_NS, "href");
    Attr media = styleTag.getAttributeNodeNS(HTML_NS, "media");

    if (href == null) {
      jobs.getMessageQueue().addMessage(
          PluginMessageType.MISSING_ATTRIBUTE,
          Nodes.getFilePositionFor(styleTag),
          MessagePart.Factory.valueOf("href"),
          MessagePart.Factory.valueOf("link"));
      return;
    }

    URI hrefUri;
    try {
      hrefUri = new URI(href.getNodeValue());
    } catch (URISyntaxException ex) {
      jobs.getMessageQueue().getMessages().add(
          new Message(PluginMessageType.MALFORMED_URL, MessageLevel.ERROR,
                      Nodes.getFilePositionFor(href),
                      MessagePart.Factory.valueOf(href.getNodeName())));
      return;
    }

    // Fetch the stylesheet source.
    URI absUri = Nodes.getFilePositionForValue(href).source().getUri()
        .resolve(hrefUri);
    jobs.getMessageContext().addInputSource(new InputSource(absUri));
    CharProducer cssStream = env.loadExternalResource(
        new ExternalReference(
            absUri, Nodes.getFilePositionForValue(href)),
        "text/css");
    if (cssStream == null) {
      jobs.getMessageQueue().addMessage(
          PluginMessageType.FAILED_TO_LOAD_EXTERNAL_URL,
          Nodes.getFilePositionForValue(href),
          MessagePart.Factory.valueOf("" + hrefUri));
      return;
    }

    extractStyles(styleTag, cssStream, media, jobs);
  }

  private void extractStyles(
      Element styleTag, CharProducer cssStream, Attr media, Jobs jobs) {
    Attr type = styleTag.getAttributeNodeNS(HTML_NS, "type");

    if (type != null && !isCssContentType(type.getNodeValue())) {
      jobs.getMessageQueue().addMessage(
          PluginMessageType.UNRECOGNIZED_CONTENT_TYPE,
          Nodes.getFilePositionFor(type),
          MessagePart.Factory.valueOf(type.getNodeValue()),
          MessagePart.Factory.valueOf(styleTag.getTagName()));
      return;
    }

    CssTree.StyleSheet stylesheet;
    try {
      stylesheet = parseCss(cssStream, jobs.getMessageQueue());
      if (stylesheet == null) { return; }  // If all tokens ignorable.
    } catch (ParseException ex) {
      ex.toMessageQueue(jobs.getMessageQueue());
      return;
    }

    Set<Name> mediaTypes = Collections.<Name>emptySet();
    if (media != null) {
      String[] mediaTypeArr = media.getNodeValue().trim().split("\\s*,\\s*");
      if (mediaTypeArr.length != 1 || !"".equals(mediaTypeArr[0])) {
        mediaTypes = new LinkedHashSet<Name>();
        for (String mediaType : mediaTypeArr) {
          if (!CssSchema.isMediaType(mediaType)) {
            jobs.getMessageQueue().addMessage(
                PluginMessageType.UNRECOGNIZED_MEDIA_TYPE,
                Nodes.getFilePositionFor(media),
                MessagePart.Factory.valueOf(mediaType));
            continue;
          }
          mediaTypes.add(Name.css(mediaType));
        }
      }
    }
    if (!(mediaTypes.isEmpty() || mediaTypes.contains(Name.css("all")))) {
      final List<CssTree.RuleSet> rules = new ArrayList<CssTree.RuleSet>();
      stylesheet.acceptPreOrder(
          new Visitor() {
            public boolean visit(AncestorChain<?> ancestors) {
              CssTree node = (CssTree) ancestors.node;
              if (node instanceof CssTree.StyleSheet) { return true; }
              if (node instanceof CssTree.RuleSet) {
                rules.add((CssTree.RuleSet) node);
                ((MutableParseTreeNode) ancestors.parent.node)
                    .removeChild(node);
              }
              // Don't touch RuleSets that are not direct children of a
              // stylesheet.
              return false;
            }
          }, null);
      if (!rules.isEmpty()) {
        List<CssTree> mediaChildren = new ArrayList<CssTree>();
        for (Name mediaType : mediaTypes) {
          mediaChildren.add(
              new CssTree.Medium(Nodes.getFilePositionFor(media), mediaType));
        }
        mediaChildren.addAll(rules);
        CssTree.Media mediaBlock = new CssTree.Media(
            Nodes.getFilePositionFor(styleTag), mediaChildren);
        stylesheet.appendChild(mediaBlock);
      }
    }

    jobs.getJobs().add(new Job(AncestorChain.instance(stylesheet)));
  }

  /**
   * Convert a {@code <body>} onload handler to a script tag so that it can be
   * processed correctly in the normal flow of HTML.
   *
   * <pre>
   * &lt;body onload=foo()&gt;      &lt;body&gt;
   *   Hello World              =>    Hello World
   * &lt;/body&gt;                    &lt;script&gt;foo()&lt;/script&gt;
   *                                &lt;/body&gt;
   * </pre>
   */
  private void moveOnLoadHandlerToEndOfBody(Element body) {
    Attr onload = body.getAttributeNodeNS(HTML_NS, "onload");
    Attr language = body.getAttributeNodeNS(HTML_NS, "language");
    if (onload == null
        // If the onload handler is vbscript, let the validator complain.
        || (language != null && !isJavaScriptLanguage(language.getNodeValue()))
        ) {
      return;
    }
    body.removeAttributeNode(onload);

    FilePosition pos = Nodes.getFilePositionForValue(onload);
    String source = onload.getNodeValue();
    Text sourceText = body.getOwnerDocument().createTextNode(source);
    Nodes.setFilePositionFor(sourceText, pos);
    Element scriptElement = body.getOwnerDocument().createElementNS(
        HTML_NS, "script");
    scriptElement.appendChild(sourceText);
    Nodes.setFilePositionFor(scriptElement, pos);

    body.appendChild(scriptElement);
  }

  /**
   * A CharProducer that produces characters from the concatenation of all
   * the text nodes in the given node list.
   */
  private static CharProducer textNodesToCharProducer(
      Iterable<? extends Node> nodes, boolean stripComments) {
    List<Text> textNodes = new ArrayList<Text>();
    for (Node node : nodes) {
      if (node instanceof Text) { textNodes.add((Text) node); }
    }
    if (textNodes.isEmpty()) { return null; }
    List<CharProducer> content = new ArrayList<CharProducer>();
    for (int i = 0, n = textNodes.size(); i < n; ++i) {
      Text node = textNodes.get(i);
      String text = node.getNodeValue();
      if (stripComments) {
        if (i == 0) {
          text = text.replaceFirst("^(\\s*)<!--", "$1     ");
        }
        if (i + 1 == n) {
          text = text.replaceFirst("-->(\\s*)$", "   $1");
        }
      }
      content.add(CharProducer.Factory.create(
          new StringReader(text),
          FilePosition.startOf(Nodes.getFilePositionFor(node))));
    }
    if (content.size() == 1) {
      return content.get(0);
    } else {
      return CharProducer.Factory.chain(content.toArray(new CharProducer[0]));
    }
  }

  /** "text/html;charset=UTF-8" -> "text/html" */
  private static String getMimeType(String contentType) {
    int typeEnd = contentType.indexOf(';');
    if (typeEnd < 0) { typeEnd = contentType.length(); }
    return Strings.toLowerCase(contentType.substring(0, typeEnd));
  }

  private static boolean isJavaScriptContentType(String contentType) {
    String mimeType = getMimeType(contentType);
    return ("text/javascript".equals(mimeType)
            || "application/x-javascript".equals(mimeType)
            || "type/ecmascript".equals(mimeType));
  }

  /**
   * Per the language attribute which identifies the programming language for
   * event handlers.
   * <p>
   * <a href=
   * "http://www.blooberry.com/indexdot/html/tagpages/attributes/language.htm">
   * Language</a>:
   *
   * This attribute is used to specify the current scripting
   * language in use for an element. 'JScript' and 'javascript' both
   * refer to Javascript engines. 'Vbs' and 'Vbscript' both refer to
   * Vbscript engines. 'XML' refers to an embedded XML
   * document/fragment.
   * <p>
   * Values: JScript [DEFAULT] | javascript | vbs | vbscript | XML
   */
  private static boolean isJavaScriptLanguage(String language) {
    language = Strings.toLowerCase(language);
    return language.startsWith("javascript") || language.startsWith("jscript");
  }

  private static boolean isCssContentType(String contentType) {
    return "text/css".equals(getMimeType(contentType));
  }

  public static Block parseJs(
      InputSource is, CharProducer cp, FilePosition scriptPos,
      MessageQueue localMessageQueue)
      throws ParseException {
    JsLexer lexer = new JsLexer(cp);
    JsTokenQueue tq = new JsTokenQueue(lexer, is);
    tq.setInputRange(scriptPos);
    if (tq.isEmpty()) { return null; }
    Parser p = new Parser(tq, localMessageQueue);
    Block body = p.parse();
    tq.expectEmpty();
    return body;
  }

  public static CssTree.StyleSheet parseCss(CharProducer cp, MessageQueue mq)
      throws ParseException {
    CssTree.StyleSheet input;
    TokenQueue<CssTokenType> tq = CssParser.makeTokenQueue(cp, mq, false);
    if (tq.isEmpty()) { return null; }

    CssParser p = new CssParser(tq, mq, MessageLevel.WARNING);
    input = p.parseStyleSheet();
    tq.expectEmpty();
    return input;
  }
}
