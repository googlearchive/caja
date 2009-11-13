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

import com.google.caja.config.ConfigUtil;
import com.google.caja.lang.html.HtmlSchema;
import com.google.caja.lexer.Keyword;
import com.google.caja.lexer.ParseException;
import com.google.caja.parser.ParserBase;
import com.google.caja.parser.html.Namespaces;
import com.google.caja.parser.html.Nodes;
import com.google.caja.reporting.EchoingMessageQueue;
import com.google.caja.reporting.MessageContext;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Constants relating to the IHTML namespace and its tag structure.
 *
 * @author mikesamuel@gmail.com
 */
public class IHTML {
  /** The URI namespace for IHTML elements. */
  public static final String NAMESPACE = Namespaces.COMMON.forPrefix("ihtml")
      .uri;
  /** The tag prefix reserved for IHTML elements. */
  public static final String PREFIX = "ihtml";

  /**
   * Name of an attribute used to mark the kind of tag within which a template
   * can be called.
   */
  public static final String CALLING_CONTEXT_ATTR = "callingContext";

  public static final HtmlSchema SCHEMA;
  static {
    EchoingMessageQueue mq = new EchoingMessageQueue(
        new PrintWriter(System.err), new MessageContext());
    URI elSrc = URI.create(
        "resource:///com/google/caja/plugin/templates/ihtml-elements.json");
    URI attrSrc = URI.create(
        "resource:///com/google/caja/plugin/templates/ihtml-attributes.json");
    try {
      SCHEMA = new HtmlSchema(
          ConfigUtil.loadWhiteListFromJson(
              elSrc, ConfigUtil.RESOURCE_RESOLVER, mq),
          ConfigUtil.loadWhiteListFromJson(
              attrSrc, ConfigUtil.RESOURCE_RESOLVER, mq));
      // If the default schema is borked, there's not much we can do.
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    } catch (ParseException ex) {
      mq.getMessages().add(ex.getCajaMessage());
      throw new RuntimeException(ex);
    }
  }

  /**
   * All the {@code ihtml:ph} and {@code ihtml:eph} elements under container,
   * inclusive, in a depth-first traversal.
   */
  public static Iterable<? extends Element> getPlaceholders(Element container) {
    List<Element> els = new ArrayList<Element>();
    findPlaceholders(container, els);
    return els;
  }
  private static void findPlaceholders(Node n, List<Element> out) {
    if (isPh(n) || isEph(n)) {
      out.add((Element) n);
    }
    for (Node c = n.getFirstChild(); c != null; c = c.getNextSibling()) {
      findPlaceholders(c, out);
    }
  }

  /** True iff the given node is an IHTML element with the given local name. */
  public static boolean is(Node n, String localElementName) {
    if (!(n instanceof Element)) { return false; }
    Element el = (Element) n;
    return localElementName.equals(el.getLocalName())
        && NAMESPACE.equals(el.getNamespaceURI());
  }

  public static boolean is(Namespaces ns) {
    return ns.uri == NAMESPACE;
  }

  public static boolean isAttribute(Node n) { return is(n, "attribute"); }
  public static Attr getName(Element ihtmlEl) {
    return ihtmlEl.getAttributeNodeNS(NAMESPACE, "name");
  }
  public static boolean isCall(Node n) { return is(n, "call"); }
  public static Attr getCallTarget(Element callEl) {
    return callEl.getAttributeNodeNS(NAMESPACE, "ihtml:template");  // TODO
  }
  public static boolean isDo(Node n) { return is(n, "do"); }
  public static Attr getInit(Element doEl) {
    return doEl.getAttributeNodeNS(NAMESPACE, "init");
  }
  public static Attr getVars(Element doEl) {
    return doEl.getAttributeNodeNS(NAMESPACE, "vars");
  }
  public static Attr getWhile(Element doEl) {
    return doEl.getAttributeNodeNS(NAMESPACE, "while");
  }
  public static boolean isDynamic(Node n) { return is(n, "dynamic"); }
  public static Attr getExpr(Element dynEl) {
    return dynEl.getAttributeNodeNS(NAMESPACE, "expr");
  }
  public static boolean isElement(Node n) { return is(n, "element"); }
  public static boolean isElse(Node n) { return is(n, "else"); }
  public static boolean isEph(Node n) { return is(n, "eph"); }
  public static boolean isMessage(Node n) { return is(n, "message"); }
  public static boolean isPh(Node n) { return is(n, "ph"); }
  public static boolean isTemplate(Node n) { return is(n, "template"); }
  public static Attr getFormals(Element templateEl) {
    return templateEl.getAttributeNodeNS(NAMESPACE, "formals");
  }

  public static boolean isIhtml(Node n) {
    if (!(n instanceof Element)) { return false; }
    return NAMESPACE.equals(n.getNamespaceURI());
  }

  public static boolean isSafeIdentifier(String ident) {
    return !ident.endsWith("__")
        && ParserBase.isJavascriptIdentifier(ident)
        && !Keyword.isKeyword(ident) && !"arguments".equals(ident);
  }

  public static Iterable<String> identifiers(String idents) {
    idents = idents.trim();
    return "".equals(idents)
        ? Collections.<String>emptyList()
        : Arrays.asList(idents.trim().split("[ \r\n\t]+"));
  }

  public static Iterable<Element> allOf(Node root, String localName) {
    switch (root.getNodeType()) {
      case Node.DOCUMENT_NODE:
        return Nodes.nodeListIterable(
            ((Document) root).getElementsByTagNameNS(NAMESPACE, localName),
            Element.class);
      case Node.ELEMENT_NODE:
        return Nodes.nodeListIterable(
            ((Element) root).getElementsByTagNameNS(NAMESPACE, localName),
            Element.class);
    }
    List<Element> els = new ArrayList<Element>();
    for (Node child : Nodes.childrenOf(root)) {
      appendAllOf(child, PREFIX + ":" + localName, els);
    }
    return els;
  }

  private static void appendAllOf(
      Node n, String globalName, List<Element> out) {
    if (n.getNodeType() == Node.ELEMENT_NODE
        && globalName.equals(n.getNodeName())) {
      out.add((Element) n);
    }
    for (Node child : Nodes.childrenOf(n)) {
      appendAllOf(child, globalName, out);
    }
  }
}
