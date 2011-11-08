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

import com.google.caja.SomethingWidgyHappenedError;
import com.google.caja.config.WhiteList;
import com.google.caja.lang.css.CssSchema;
import com.google.caja.lang.html.HtmlSchema;
import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.ParseException;
import com.google.caja.parser.AncestorChain;
import com.google.caja.parser.MutableParseTreeNode;
import com.google.caja.parser.Visitor;
import com.google.caja.parser.css.CssTree;
import com.google.caja.parser.html.AttribKey;
import com.google.caja.parser.html.Dom;
import com.google.caja.parser.html.ElKey;
import com.google.caja.parser.html.Namespaces;
import com.google.caja.parser.html.Nodes;
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.Expression;
import com.google.caja.parser.js.ExpressionStmt;
import com.google.caja.parser.js.Statement;
import com.google.caja.parser.js.StringLiteral;
import com.google.caja.parser.quasiliteral.QuasiBuilder;
import com.google.caja.plugin.Job;
import com.google.caja.plugin.JobEnvelope;
import com.google.caja.plugin.Jobs;
import com.google.caja.plugin.Placeholder;
import com.google.caja.plugin.PluginMessageType;
import com.google.caja.plugin.templates.HtmlAttributeRewriter;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.util.ContentType;
import com.google.caja.util.Lists;
import com.google.caja.util.Maps;
import com.google.caja.util.Name;
import com.google.caja.util.Pipeline;
import com.google.caja.util.Sets;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

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
  private final HtmlSchema htmlSchema;
  private final JobCache jobCache;

  public RewriteHtmlStage(HtmlSchema htmlSchema, JobCache jobCache) {
    this.htmlSchema = htmlSchema;
    this.jobCache = jobCache;
  }

  public boolean apply(Jobs jobs) {

    for (JobEnvelope env : jobs.getJobsByType(ContentType.HTML)) {
      if (env.fromCache) { continue; }
      if (new HtmlExtractor(htmlSchema, jobCache, jobs, env).extract()) {
        // We can't cache HTML with placeholders since we need to reincorporate
        // the cached jobs in particular places.
        // This doesn't prevent us from caching the really expensive
        // transformations though, which are the JS validation.
        env.job.getRoot().getAttributes().set(JobCache.NO_CACHE, true);
      }
    }
    return jobs.hasNoFatalErrors();
  }
}

final class HtmlExtractor {
  final HtmlSchema htmlSchema;
  final JobCache jobCache;
  final Jobs jobs;
  final JobEnvelope htmlEnv;

  HtmlExtractor(
      HtmlSchema htmlSchema, JobCache jobCache, Jobs jobs,
      JobEnvelope htmlEnv) {
    this.htmlSchema = htmlSchema;
    this.jobCache = jobCache;
    this.jobs = jobs;
    this.htmlEnv = htmlEnv;
  }

  boolean extract() {
    boolean hasPlaceholders = false;
    Node root = ((Dom) htmlEnv.job.getRoot()).getValue();
    extractBodyInfo(root, htmlEnv);
    HtmlEmbeddedContentFinder finder = new HtmlEmbeddedContentFinder(
        htmlSchema, htmlEnv.job.getBaseUri(),
        jobs.getMessageQueue(), jobs.getMessageContext());
    for (EmbeddedContent content : finder.findEmbeddedContent(root)) {
      Node src = content.getSource();
      if (content.getSource() instanceof Element) {
        // Rewrite styles and scripts.
        // <script>foo()</script>  ->  <script>(cajoled foo)</script>
        // <style>foo { ... }</style>  ->  <style>foo { ... }</style>
        // <script src=foo.js></script>
        //     ->  <script>(cajoled, inlined foo)</script>
        // <link rel=stylesheet href=foo.css>
        //     ->  <style>(cajoled, inlined styles)</style>
        Element el = (Element) content.getSource();
        if (SCRIPT.is(el)) {
          hasPlaceholders |= rewriteScriptEl(root, content);
        } else if (STYLE.is(el)) {
          rewriteStyleEl(content);
        } else if (LINK.is(el)) {
          rewriteLinkEl(content);
        } else {
          throw new SomethingWidgyHappenedError(src.getNodeName());
        }
      } else if (BODY_ONLOAD.is((Attr) src)) {
        moveOnLoadHandlerToEndOfBody(content);
      }
      // Attribute extraction handled elsewhere.
    }
    return hasPlaceholders;
  }

  private static final ElKey HTML = ElKey.forHtmlElement("html");
  private static final ElKey BODY = ElKey.forHtmlElement("body");
  private static final ElKey LINK = ElKey.forHtmlElement("link");
  private static final ElKey SCRIPT = ElKey.forHtmlElement("script");
  private static final ElKey STYLE = ElKey.forHtmlElement("style");
  private static final AttribKey BODY_ONLOAD
      = AttribKey.forHtmlAttrib(BODY, "onload");
  private static final AttribKey BODY_CLASS
      = AttribKey.forHtmlAttrib(BODY, "class");

  private boolean rewriteScriptEl(Node root, EmbeddedContent c) {
    Element scriptEl = (Element) c.getSource();
    Node parent = scriptEl.getParentNode();

    // Parse the body and create a block that will be placed inline in
    // loadModule.
    Block parsedScriptBody;
    try {
      parsedScriptBody = (Block) c.parse(
          jobs.getPluginMeta().getUriFetcher(), jobs.getMessageQueue());
    } catch (ParseException ex) {
      ex.toMessageQueue(jobs.getMessageQueue());
      parent.removeChild(scriptEl);
      return false;
    }

    if (parsedScriptBody == null || parsedScriptBody.children().isEmpty()) {
      parent.removeChild(scriptEl);
      return false;
    } else {
      Element placeholder = placeholderFor(scriptEl, parsedScriptBody);
      // Replace the script tag with a placeholder that points to the inlined
      // script.
      if (c.isDeferred()) {
        parent.removeChild(scriptEl);
        root.appendChild(placeholder);
      } else {
        parent.replaceChild(placeholder, scriptEl);
      }
      return true;
    }
  }

  // Build a replacement element, <span/>, and link it to the extracted
  // JavaScript, so that when the DOM is rendered, we can properly interleave
  // the extract scripts with the scripts that generate markup.
  private Element placeholderFor(Node n, Block parsedScriptBody) {
    String id = "$" + jobs.getPluginMeta().generateGuid();
    Element placeholder = Placeholder.make(n, id);
    URI baseUri = htmlEnv.job.getBaseUri();
    jobs.getJobs().add(new JobEnvelope(
        id, jobCache.forJob(ContentType.JS, parsedScriptBody).asSingleton(),
        ContentType.JS, false,
        Job.jsJob(parsedScriptBody, baseUri)));
    return placeholder;
  }

  private void rewriteStyleEl(EmbeddedContent c) {
    Element styleEl = (Element) c.getSource();
    styleEl.getParentNode().removeChild(styleEl);
    extractStyles(styleEl, c, null);
  }

  private void rewriteLinkEl(EmbeddedContent c) {
    Element linkEl = (Element) c.getSource();
    linkEl.getParentNode().removeChild(linkEl);
    Attr media = linkEl.getAttributeNodeNS(
        Namespaces.HTML_NAMESPACE_URI, "media");
    extractStyles(linkEl, c, media);
  }

  private void extractStyles(Element el, EmbeddedContent c, Attr media) {
    MessageQueue mq = jobs.getMessageQueue();
    CssTree.StyleSheet stylesheet = null;
    try {
      stylesheet = (CssTree.StyleSheet) c.parse(
          jobs.getPluginMeta().getUriFetcher(), mq);
    } catch (ParseException ex) {
      ex.toMessageQueue(mq);
    }
    if (stylesheet == null || stylesheet.children().isEmpty()) { return; }

    Set<Name> mediaTypes = Collections.<Name>emptySet();
    if (media != null) {
      String[] mediaTypeArr = media.getNodeValue().trim().split("\\s*,\\s*");
      if (mediaTypeArr.length != 1 || !"".equals(mediaTypeArr[0])) {
        mediaTypes = Sets.newLinkedHashSet();
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
      final List<CssTree.RuleSet> rules = Lists.newArrayList();
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
        List<CssTree> mediaChildren = Lists.newArrayList();
        for (Name mediaType : mediaTypes) {
          mediaChildren.add(
              new CssTree.Medium(Nodes.getFilePositionFor(media), mediaType));
        }
        mediaChildren.addAll(rules);
        CssTree.Media mediaBlock = new CssTree.Media(
            Nodes.getFilePositionFor(el), mediaChildren);
        stylesheet.appendChild(mediaBlock);
      }
    }

    URI baseUri = c.getBaseUri();

    jobs.getJobs().add(
        // Insert CSS stylesheets before the HTML job so that stylesheets always
        // are loaded before the HTML job even if they have different cache
        // keys.
        jobs.getJobs().indexOf(htmlEnv),
        new JobEnvelope(
            null,
            // jobCache.forJob(ContentType.CSS, stylesheet).asSingleton(),
            JobCache.none(),
            ContentType.CSS, false,
            Job.cssJob(stylesheet, baseUri)));
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
  private void moveOnLoadHandlerToEndOfBody(EmbeddedContent c) {
    Attr onload = (Attr) c.getSource();
    Element body = onload.getOwnerElement();
    body.removeAttributeNode(onload);

    MessageQueue mq = jobs.getMessageQueue();
    Block handler;
    try {
      handler = (Block) c.parse(jobs.getPluginMeta().getUriFetcher(), mq);
    } catch (ParseException ex) {
      ex.toMessageQueue(mq);
      return;
    }
    if (handler != null && !handler.children().isEmpty()) {
      Element placeholder = placeholderFor(onload, handler);
      body.appendChild(placeholder);
    }
  }

  /**
   * Find any class attributes on the {@code <body>} element and use the HTML
   * emitter to attach the classes to the virtual document body.
   */
  private void extractBodyInfo(Node node, JobEnvelope source) {
    switch (node.getNodeType()) {
      case Node.DOCUMENT_FRAGMENT_NODE:
        for (Node child : Nodes.childrenOf(node)) {
          extractBodyInfo(child, source);
        }
        break;
      case Node.ELEMENT_NODE:
        Element el = (Element) node;
        ElKey elKey = ElKey.forElement(el);
        if (HTML.equals(elKey)) {
          for (Node child : Nodes.childrenOf(node)) {
            extractBodyInfo(child, source);
          }
        } else if (BODY.equals(elKey)) {
          Attr classAttr = el.getAttributeNodeNS(
              Namespaces.HTML_NAMESPACE_URI, BODY_CLASS.localName);
          if (classAttr != null) {
            el.removeAttributeNode(classAttr);
            String identifiers = classAttr.getValue().trim();
            if (!"".equals(identifiers)) {
              WhiteList wl = WhiteList.Factory.empty();
              HtmlAttributeRewriter rw = new HtmlAttributeRewriter(
                  jobs.getPluginMeta(), new CssSchema(wl, wl),
                  htmlSchema, Maps.<Attr, EmbeddedContent>newHashMap(),
                  jobs.getMessageQueue());
              HtmlAttributeRewriter.SanitizedAttr sanitized
                  = rw.sanitizeStringValue(HtmlAttributeRewriter.fromAttr(
                      classAttr, htmlSchema.lookupAttribute(BODY_CLASS),
                      source));
              if (sanitized.isSafe) {
                FilePosition pos = Nodes.getFilePositionForValue(classAttr);
                Expression e = sanitized.result;
                if (e == null) { e = StringLiteral.valueOf(pos, identifiers); }
                Statement s = new ExpressionStmt(
                    (Expression) QuasiBuilder.substV(
                         ""
                        + "IMPORTS___.htmlEmitter___"
                        + "    ./*@synthetic*/addBodyClasses(@idents);",
                        "idents", e));
                Block b = new Block(FilePosition.UNKNOWN, Collections.singletonList(s));
                jobs.getJobs().add(new JobEnvelope(
                    null, htmlEnv.cacheKeys, ContentType.JS, false,
                    Job.jsJob(b, htmlEnv.job.getBaseUri())));
              }
            }
          }
        }
        break;
    }
  }
}
