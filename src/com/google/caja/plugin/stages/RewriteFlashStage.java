// Copyright (C) 2011 Google Inc.
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

import java.io.IOException;
import java.net.URI;
import java.util.Map;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.google.caja.lexer.FilePosition;
import com.google.caja.parser.html.Dom;
import com.google.caja.parser.html.Namespaces;
import com.google.caja.parser.html.Nodes;
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.Expression;
import com.google.caja.parser.js.ExpressionStmt;
import com.google.caja.parser.js.Literal;
import com.google.caja.parser.js.NullLiteral;
import com.google.caja.parser.js.StringLiteral;
import com.google.caja.parser.quasiliteral.QuasiBuilder;
import com.google.caja.plugin.Job;
import com.google.caja.plugin.JobEnvelope;
import com.google.caja.plugin.Jobs;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.MessageType;
import com.google.caja.reporting.MessageTypeInt;
import com.google.caja.util.ContentType;
import com.google.caja.util.Maps;
import com.google.caja.util.Pipeline;
import com.google.caja.util.Strings;

/**
 * This searches for Flash embeds in unsanitized html and rewrites them.
 *
 * @author felix8a@gmail.com
 */

public final class RewriteFlashStage implements Pipeline.Stage<Jobs> {
  public boolean apply(Jobs jobs) {
    FlashRewriter rewriter = new FlashRewriter(jobs);
    for (JobEnvelope env: jobs.getJobsByType(ContentType.HTML)) {
      Node root = ((Dom) env.job.getRoot()).getValue();
      rewriter.rewriteChildren(root);
    }
    rewriter.finish();
    return true;
  }
}

final class FlashRewriter {
  final Block embedder = new Block();
  final Jobs jobs;
  final MessageQueue mq;

  FlashRewriter(Jobs jobs) {
    this.jobs = jobs;
    this.mq = jobs.getMessageQueue();
  }

  void finish() {
    if (embedder.children().size() != 0) {
      URI baseUri = null; // TODO(felix8a)
      jobs.getJobs().add(JobEnvelope.of(Job.jsJob(embedder, baseUri)));
    }
    return;
  }

  void rewriteChildren(Node node) {
    Node c = node.getFirstChild();
    for (; c != null; c = c.getNextSibling()) {
      if (c instanceof Element) {
        c = rewriteElement((Element) c);
      }
      rewriteChildren(c);
    }
  }

  private Element rewriteElement(Element el) {
    String tagName = Strings.lower(el.getLocalName());
    if ("embed".equals(tagName)) {
      if (APPLICATION_FLASH.equals(getAttr(el, "type"))) {
        return rewriteEmbed(el);
      }
    } else if ("noembed".equals(tagName)) {
      return rewriteNoembed(el);
    } else if ("object".equals(tagName)) {
      if (APPLICATION_FLASH.equals(getAttr(el, "type"))) {
        return rewriteObject(el);
      } else if (Strings.eqIgnoreCase(
          CLASSID_FLASH, getAttr(el, "classid"))) {
        return rewriteObject(el);
      }
    }
    // If an <embed> or <object> tag doesn't match a usage we recognize,
    // leave it alone, it will get stripped by HtmlSanitizerStage.
    return el;
  }

  private Element rewriteEmbed(Element el) {
    String src = getAttr(el, "src");
    if (empty(src)) {
      mq.addMessage(Messages.MISSING_SRC, Nodes.getFilePositionFor(el));
      return el;
    }
    String height = getAttr(el, "height");
    String width = getAttr(el, "width");
    Element r = emplacehold(el);
    if (r == null) {
      mq.addMessage(Messages.NO_PARENT, Nodes.getFilePositionFor(el));
      return el;
    }
    String id = getPlaceholderId(r);
    FilePosition pos = Nodes.getFilePositionFor(el);
    Expression e = (Expression) QuasiBuilder.substV(
        ""
        + "IMPORTS___.htmlEmitter___."
        + "/*@synthetic*/ handleEmbed({"
        + " id: @id,"
        + " src: @src,"
        + " height: @height,"
        + " width: @width })",
        "id", literal(id, pos),
        "src", literal(src, pos),
        "height", literal(height, pos),
        "width", literal(width, pos));
    embedder.appendChild(new ExpressionStmt(e));
    return r;
  }

  private Element rewriteNoembed(Element el) {
    String id = generateId();
    String className = el.getAttributeNS(HTML_NS, "class");
    if (empty(className)) {
      className = id;
    } else {
      className = id + " " + className;
    }
    el.setAttributeNS(HTML_NS, "class", className);
    FilePosition pos = Nodes.getFilePositionFor(el);
    Expression e = (Expression) QuasiBuilder.substV(
        ""
        + "IMPORTS___.htmlEmitter___."
        + "/*@synthetic*/ handleEmbed({"
        + " id: @id })",
        "id", literal(id, pos));
    embedder.appendChild(new ExpressionStmt(e));
    return el;
  }

  private Element rewriteObject(Element el) {
    Element r = emplacehold(el);
    if (r == null) {
      mq.addMessage(Messages.NO_PARENT, Nodes.getFilePositionFor(el));
      return el;
    }

    Map<String, String> params = Maps.newHashMap();
    Node c = el.getFirstChild();
    Node next = null;
    for (; c != null; c = next) {
      next = c.getNextSibling();
      String tagName = getTagName(c);
      if ("param".equals(tagName)) {
        String name = getAttr(c, "name");
        String value = getAttr(c, "value");
        if (!empty(name) && !empty(value)) {
          params.put(name, value);
        }
        c.getParentNode().removeChild(c);
      } else {
        r.appendChild(c);
      }
    }

    String src = getAttr(el, "data");
    if (empty(src)) {
      src = params.get("movie");
    }
    String width = getAttr(el, "width");
    if (empty(width)) {
      width = params.get("width");
    }
    String height = getAttr(el, "height");
    if (empty(height)) {
      height = params.get("height");
    }

    String id = getPlaceholderId(r);
    FilePosition pos = Nodes.getFilePositionFor(el);
    Expression e = (Expression) QuasiBuilder.substV(
        ""
        + "IMPORTS___.htmlEmitter___."
        + "/*@synthetic*/ handleEmbed({"
        + " id: @id,"
        + " src: @src,"
        + " height: @height,"
        + " width: @width })",
        // TODO(felix8a): need to add params and attrs
        "id", literal(id, pos),
        "src", literal(src, pos),
        "height", literal(height, pos),
        "width", literal(width, pos));
    embedder.appendChild(new ExpressionStmt(e));
    return r;
  }

  private Literal literal(String value, FilePosition pos) {
    if (empty(value)) {
      return new NullLiteral(pos);
    } else {
      return StringLiteral.valueOf(pos, value);
    }
  }

  private Element emplacehold(Element el) {
    Node parent = el.getParentNode();
    if (parent == null) { return null; }
    Element span = el.getOwnerDocument().createElementNS(HTML_NS, "span");
    // We're labeling the placeholder span with class rather than id,
    // because there are several complications getting ids through
    // all the HTML rewriter stages.
    span.setAttributeNS(HTML_NS, "class", generateId());
    parent.insertBefore(span, el);
    parent.removeChild(el);
    return span;
  }

  private String getTagName(Node n) {
    if (n instanceof Element) {
      return Strings.lower(n.getLocalName());
    } else {
      return null;
    }
  }

  private String getAttr(Node n, String attrib) {
    if (n instanceof Element) {
      return ((Element) n).getAttributeNS(HTML_NS, attrib);
    } else {
      return null;
    }
  }

  private boolean empty(String s) {
    return s == null || s.length() == 0;
  }

  private String generateId() {
    int guid = jobs.getPluginMeta().generateGuid();
    return "cajaEmbed" + guid;
  }

  private String getPlaceholderId(Element el) {
    return el.getAttributeNS(HTML_NS, "class");
  }

  private static final String HTML_NS = Namespaces.HTML_NAMESPACE_URI;

  private static final String APPLICATION_FLASH =
    "application/x-shockwave-flash";

  private static final String CLASSID_FLASH =
    "clsid:D27CDB6E-AE6D-11cf-96B8-444553540000";

  private enum Messages implements MessageTypeInt {

    MISSING_SRC(MessageLevel.WARNING,
        "%s: Embed is missing src attribute"),

    NO_PARENT(MessageLevel.WARNING,
        "%s: Element doesn't have a parent??");

    private final String formatString;
    private final MessageLevel level;
    private int paramCount = -1;

    Messages(MessageLevel level, String formatString) {
      this.level = level;
      this.formatString = formatString;
    }

    public int getParamCount() {
      if (paramCount < 0) {
        paramCount = MessageType.formatStringArity(formatString);
      }
      return paramCount;
    }

    public void format(MessagePart[] parts, MessageContext context,
                       Appendable out) throws IOException {
      MessageType.formatMessage(formatString, parts, context, out);
    }

    public MessageLevel getLevel() { return level; }
  }
}
