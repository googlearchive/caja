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
import com.google.caja.lexer.CssLexer;
import com.google.caja.lexer.CssTokenType;
import com.google.caja.lexer.ExternalReference;
import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.HtmlTokenType;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.JsLexer;
import com.google.caja.lexer.JsTokenQueue;
import com.google.caja.lexer.ParseException;
import com.google.caja.lexer.Token;
import com.google.caja.lexer.TokenQueue;
import com.google.caja.parser.AncestorChain;
import com.google.caja.parser.MutableParseTreeNode;
import com.google.caja.parser.Visitor;
import com.google.caja.parser.css.CssParser;
import com.google.caja.parser.css.CssTree;
import com.google.caja.parser.html.DomTree;
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.Parser;
import com.google.caja.plugin.Job;
import com.google.caja.plugin.Jobs;
import com.google.caja.plugin.PluginEnvironment;
import com.google.caja.plugin.PluginMessageType;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.util.Criterion;
import com.google.caja.util.Pipeline;
import com.google.caja.util.SyntheticAttributeKey;

import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Extract some unsafe bits from HTML for processing by later stages.
 * Specifically, extracts {@code onclick} and other handlers, the contents of
 * {@code <script>} elements, and the contents of {@code <style>} elements,
 * and the content referred to by {@code <link rel=stylesheet>} elements.
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

  /**
   * A synthetic attribute that stores the name of a function extracted from a
   * script tag.
   */
  public static final SyntheticAttributeKey<Block> EXTRACTED_SCRIPT_BODY
      = new SyntheticAttributeKey<Block>(Block.class, "extractedScript");

  public boolean apply(Jobs jobs) {
    for (Job job : jobs.getJobsByType(Job.JobType.HTML)) {
      rewriteDomTree((DomTree) job.getRoot().node, jobs);
    }
    return jobs.hasNoFatalErrors();
  }

  DomTree rewriteDomTree(DomTree t, final Jobs jobs) {
    // Rewrite styles and scripts.
    // <script>foo()</script>  ->  <script>(cajoled foo)</script>
    // <style>foo { ... }</style>  ->  <style>foo { ... }</style>
    // <script src=foo.js></script>  ->  <script>(cajoled, inlined foo)</script>
    // <link rel=stylesheet href=foo.css>
    //     ->  <style>(cajoled, inlined styles)</style>
    Visitor domRewriter = new Visitor() {
        public boolean visit(AncestorChain<?> ancestors) {
          DomTree n = (DomTree) ancestors.node;
          if (n instanceof DomTree.Tag) {
            MutableParseTreeNode parentNode
                = (MutableParseTreeNode) ancestors.getParentNode();
            DomTree.Tag tag = (DomTree.Tag) n;
            String name = tag.getTagName().toLowerCase();
            if ("script".equals(name)) {
              rewriteScriptTag(tag, parentNode, jobs);
            } else if ("style".equals(name)) {
              rewriteStyleTag(tag, parentNode, jobs);
            } else if ("link".equals(name)) {
              rewriteLinkTag(tag, parentNode, jobs);
            } else if ("body".equals(name)) {
              moveOnLoadHandlerToEndOfBody(tag);
            }
          }
          return true;
        }
      };
    t.acceptPreOrder(domRewriter, null);
    return t;
  }

  private void rewriteScriptTag(
      DomTree.Tag scriptTag, MutableParseTreeNode parent, Jobs jobs) {
    PluginEnvironment env = jobs.getPluginMeta().getPluginEnvironment();

    DomTree.Attrib type = lookupAttribute(
        scriptTag, "type", DupePolicy.YIELD_FIRST);
    DomTree.Attrib src = lookupAttribute(
        scriptTag, "src", DupePolicy.YIELD_FIRST);
    if (type != null && !isJavaScriptContentType(type.getAttribValue())) {
      jobs.getMessageQueue().addMessage(
          PluginMessageType.UNRECOGNIZED_CONTENT_TYPE,
          type.getFilePosition(),
          MessagePart.Factory.valueOf(type.getAttribValue()),
          MessagePart.Factory.valueOf(scriptTag.getTagName()));
      parent.removeChild(scriptTag);
      return;
    }
    // The script contents.
    CharProducer jsStream;
    if (src == null) {  // Parse the script tag body.
      jsStream = textNodesToCharProducer(scriptTag.children(), true);
      if (jsStream == null) {
        parent.removeChild(scriptTag);
        return;
      }
    } else {  // Load the src attribute
      URI srcUri;
      try {
        srcUri = new URI(src.getAttribValue());
      } catch (URISyntaxException ex) {
        jobs.getMessageQueue().getMessages().add(
            new Message(PluginMessageType.MALFORMED_URL, MessageLevel.ERROR,
                        src.getFilePosition(), src));
        parent.removeChild(scriptTag);
        return;
      }

      // Fetch the script source.
      jsStream = env.loadExternalResource(
          new ExternalReference(
              srcUri, src.getAttribValueNode().getFilePosition()),
          "text/javascript");
      if (jsStream == null) {
        parent.removeChild(scriptTag);
        jobs.getMessageQueue().addMessage(
            PluginMessageType.FAILED_TO_LOAD_EXTERNAL_URL,
            src.getFilePosition(), MessagePart.Factory.valueOf("" + srcUri));
        return;
      }
    }

    // Parse the body and create a block that will be placed inline in
    // loadModule.
    Block parsedScriptBody;
    try {
      parsedScriptBody = parseJs(jsStream.getCurrentPosition().source(),
                                 jsStream, jobs.getMessageQueue());
    } catch (ParseException ex) {
      ex.toMessageQueue(jobs.getMessageQueue());
      parent.removeChild(scriptTag);
      return;
    }

    // Build a replacment element, <span/>, and link it to the extracted
    // javascript, so that when the DOM is rendered, we can properly interleave
    // the extract scripts with the scripts that generate markup.
    DomTree.Tag placeholder;
    {
      Token<HtmlTokenType> startToken = Token.instance(
          "<span", HtmlTokenType.TAGBEGIN, scriptTag.getToken().pos);
      Token<HtmlTokenType> endToken = Token.instance(
          "/>", HtmlTokenType.TAGEND,
          FilePosition.endOf(scriptTag.getFilePosition()));
      placeholder = new DomTree.Tag(
          Collections.<DomTree>emptyList(), startToken, endToken);
      placeholder.getAttributes().set(EXTRACTED_SCRIPT_BODY, parsedScriptBody);
    }

    // Replace the external script tag with the inlined script.
    parent.replaceChild(placeholder, scriptTag);
  }

  private void rewriteStyleTag(
      DomTree.Tag styleTag, MutableParseTreeNode parent, Jobs jobs) {
    parent.removeChild(styleTag);

    CharProducer cssStream
        = textNodesToCharProducer(styleTag.children(), false);
    if (cssStream != null) {
      extractStyles(styleTag, cssStream, null, jobs);
    }
  }

  private void rewriteLinkTag(
      DomTree.Tag styleTag, MutableParseTreeNode parent, Jobs jobs) {
    PluginEnvironment env = jobs.getPluginMeta().getPluginEnvironment();

    parent.removeChild(styleTag);

    DomTree.Attrib rel = lookupAttribute(styleTag, "rel",
        DupePolicy.YIELD_NULL);
    if (rel == null
        || !rel.getAttribValue().trim().equalsIgnoreCase("stylesheet")) {
      // If it's not a stylesheet then ignore it.
      // The HtmlValidator should complain but that's not our problem.
      return;
    }

    DomTree.Attrib href = lookupAttribute(
        styleTag, "href", DupePolicy.YIELD_NULL);
    DomTree.Attrib media = lookupAttribute(
        styleTag, "media", DupePolicy.YIELD_FIRST);

    if (href == null) {
      jobs.getMessageQueue().addMessage(
          PluginMessageType.MISSING_ATTRIBUTE, styleTag.getFilePosition(),
          MessagePart.Factory.valueOf("href"),
          MessagePart.Factory.valueOf("link"));
      return;
    }

    URI hrefUri;
    try {
      hrefUri = new URI(href.getAttribValue());
    } catch (URISyntaxException ex) {
      jobs.getMessageQueue().getMessages().add(
          new Message(PluginMessageType.MALFORMED_URL, MessageLevel.ERROR,
                      href.getFilePosition(), href));
      return;
    }

    // Fetch the stylesheet source.
    CharProducer cssStream = env.loadExternalResource(
        new ExternalReference(
            hrefUri, href.getAttribValueNode().getFilePosition()),
        "text/css");
    if (cssStream == null) {
      jobs.getMessageQueue().addMessage(
          PluginMessageType.FAILED_TO_LOAD_EXTERNAL_URL,
          href.getFilePosition(), MessagePart.Factory.valueOf("" + hrefUri));
      return;
    }

    extractStyles(styleTag, cssStream, media, jobs);
  }

  private void extractStyles(
      DomTree.Tag styleTag, CharProducer cssStream, DomTree.Attrib media,
      Jobs jobs) {
    DomTree.Attrib type = lookupAttribute(
        styleTag, "type", DupePolicy.YIELD_FIRST);

    if (type != null && !isCssContentType(type.getAttribValue())) {
      jobs.getMessageQueue().addMessage(
          PluginMessageType.UNRECOGNIZED_CONTENT_TYPE,
          type.getFilePosition(),
          MessagePart.Factory.valueOf(type.getAttribValue()),
          MessagePart.Factory.valueOf(styleTag.getTagName()));
      return;
    }

    CssTree.StyleSheet stylesheet;
    try {
      stylesheet = parseCss(cssStream.getCurrentPosition().source(), cssStream);
      if (stylesheet == null) { return; }  // If all tokens ignorable.
    } catch (ParseException ex) {
      ex.toMessageQueue(jobs.getMessageQueue());
      return;
    }

    Set<String> mediaTypes = Collections.<String>emptySet();
    if (media != null) {
      String[] mediaTypeArr = media.getAttribValue().trim().split("\\s*,\\s*");
      if (mediaTypeArr.length != 1 || !"".equals(mediaTypeArr[0])) {
        mediaTypes = new LinkedHashSet<String>();
        for (String mediaType : mediaTypeArr) {
          if (!CssSchema.isMediaType(mediaType)) {
            jobs.getMessageQueue().addMessage(
                PluginMessageType.UNRECOGNIZED_MEDIA_TYPE,
                media.getFilePosition(),
                MessagePart.Factory.valueOf(mediaType));
            continue;
          }
          mediaTypes.add(mediaType);
        }
      }
    }
    if (!(mediaTypes.isEmpty() || mediaTypes.contains("all"))) {
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
        for (String mediaType : mediaTypes) {
          mediaChildren.add(
              new CssTree.Medium(type.getFilePosition(), mediaType));
        }
        mediaChildren.addAll(rules);
        CssTree.Media mediaBlock = new CssTree.Media(
            styleTag.getFilePosition(), mediaChildren);
        stylesheet.appendChild(mediaBlock);
      }
    }

    jobs.getJobs().add(
        new Job(new AncestorChain<CssTree.StyleSheet>(stylesheet)));
  }

  /**
   * Convert a <body> onload handler to a script tag so that it can be
   * processed correctly in the normal flow of HTML.
   *
   * <pre>
   * &lt;body onload=foo()&gt;      &lt;body&gt;
   *   Hello World              =>    Hello World
   * &lt;/body&gt;                    &lt;script&gt;foo()&lt;/script&gt;
   *                                &lt;/body&gt;
   * </pre>
   */
  private void moveOnLoadHandlerToEndOfBody(DomTree.Tag body) {
    DomTree.Attrib onload = lookupAttribute(
        body, "onload", DupePolicy.YIELD_NULL);
    DomTree.Attrib language = lookupAttribute(
        body, "language", DupePolicy.YIELD_FIRST);
    if (language != null && !isJavaScriptLanguage(language.getAttribValue())) {
      // If the onload handler is vbscript, let the validator complain.
      return;
    }

    if (onload == null) { return; }
    body.removeChild(onload);

    FilePosition pos = onload.getAttribValueNode().getFilePosition();
    String source = onload.getAttribValue();
    DomTree.Text sourceText = new DomTree.Text(
        Token.instance(source, HtmlTokenType.UNESCAPED, pos));
    DomTree.Tag scriptElement = new DomTree.Tag(
        Collections.singletonList(sourceText),
        Token.instance("<script", HtmlTokenType.TAGBEGIN,
                       FilePosition.startOf(pos)), pos);

    body.appendChild(scriptElement);
  }
  
  /**
   * A CharProducer that produces characters from the concatenation of all
   * the text nodes in the given node list.
   */
  private static CharProducer textNodesToCharProducer(
      List<? extends DomTree> nodes, boolean stripComments) {
    List<DomTree> textNodes = new ArrayList<DomTree>();
    for (DomTree node : nodes) {
      if (node instanceof DomTree.Text || node instanceof DomTree.CData) {
        textNodes.add(node);
      }
    }
    if (textNodes.isEmpty()) { return null; }
    List<CharProducer> content = new ArrayList<CharProducer>();
    for (int i = 0, n = textNodes.size(); i < n; ++i) {
      DomTree node = textNodes.get(i);
      String text = node.getValue();
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
          FilePosition.startOf(node.getFilePosition())));
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
    return contentType.substring(0, typeEnd).toLowerCase();
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
    
    language = language.toLowerCase();
    return language.startsWith("javascript") || language.startsWith("jscript");
  }

  private static boolean isCssContentType(String contentType) {
    return "text/css".equals(getMimeType(contentType));
  }

  /**
   * @param attribName a canonicalized attribute name.
   * @param onDupe what to do if there are multiple attributes with name
   *     attribName.
   * @return null if there is no match.
   */
  private static DomTree.Attrib lookupAttribute(
      DomTree.Tag el, String attribName, DupePolicy onDupe) {
    DomTree.Attrib match = null;
    for (DomTree child : el.children()) {
      if (!(child instanceof DomTree.Attrib)) { break; }
      DomTree.Attrib attrib = (DomTree.Attrib) child;
      if (attribName.equals(attrib.getAttribName())) {
        if (onDupe == DupePolicy.YIELD_NULL) { return attrib; }
        if (match == null) {
          match = attrib;
        } else {
          // If there are duplicates, it's unclear what to do, so return null.
          return null;
        }
      }
    }
    return match;
  }
  private static enum DupePolicy { YIELD_NULL, YIELD_FIRST, }


  public static Block parseJs(
      InputSource is, CharProducer cp, MessageQueue localMessageQueue)
      throws ParseException {
    JsLexer lexer = new JsLexer(cp);
    JsTokenQueue tq = new JsTokenQueue(lexer, is);
    Parser p = new Parser(tq, localMessageQueue);
    Block body = p.parse();
    tq.expectEmpty();
    return body;
  }

  public static CssTree.StyleSheet parseCss(InputSource is, CharProducer cp)
      throws ParseException {
    CssLexer lexer = new CssLexer(cp);
    CssTree.StyleSheet input;
    TokenQueue<CssTokenType> tq = new TokenQueue<CssTokenType>(
        lexer, is, new Criterion<Token<CssTokenType>>() {
          public boolean accept(Token<CssTokenType> tok) {
            return tok.type != CssTokenType.COMMENT
                && tok.type != CssTokenType.SPACE;
          }
        });
    if (tq.isEmpty()) { return null; }

    CssParser p = new CssParser(tq);
    input = p.parseStyleSheet();
    tq.expectEmpty();
    return input;
  }
}
