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

package com.google.caja.parser.html;

import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessagePart;
import com.google.caja.util.Strings;

import java.io.IOException;

import org.w3c.dom.Attr;

/**
 * Identifies an XML/HTML attribute as it appears on a particular kind of
 * XML/HTML element.
 *
 * @author mikesamuel@gmail.com
 */
public final class AttribKey implements MessagePart, Comparable<AttribKey> {
  /** The kind of element the attribute appears on. */
  public final ElKey el;
  /** The namespace in which the attribute appears. */
  public final Namespaces ns;
  /**
   * The normalized local name of the attribute.
   * For HTML, the normalized name is the lower case form of the name, but
   * for other namespaces, no normalization is done.
   */
  public final String localName;

  private AttribKey(ElKey el, Namespaces ns, String localName) {
    if (el == null || ns == null || localName == null) {
      throw new NullPointerException();
    }
    this.el = el;
    this.ns = ns;
    this.localName = this.ns.uri == Namespaces.HTML_NAMESPACE_URI
        ? Strings.toLowerCase(localName) : localName;
  }

  private static final Namespaces HTML_NS = Namespaces.HTML_DEFAULT.forUri(
      Namespaces.HTML_NAMESPACE_URI);

  /**
   * Looks up an attribute key by element and local name.
   *
   * @param el not null.
   * @param localName unnormalized.
   */
  public static AttribKey forHtmlAttrib(ElKey el, String localName) {
    return new AttribKey(el, HTML_NS, localName);
  }

  /**
   * Looks up an attribute key by qualified name.
   *
   * @param inScope the set of namespaces in scope where the attribute appears.
   * @param el not null.  The key of the element that the attribute appears on.
   * @param qname the qualified name of the attribute.
   * @return null if qname does not specify a namespace in scope.
   */
  public static AttribKey forAttribute(
      Namespaces inScope, ElKey el, String qname) {
    Namespaces ns;
    String localName;
    int colon = qname.indexOf(':');
    if (colon < 0) {  // Same namespace as element
      ns = el.ns;
      localName = qname;
    } else {
      ns = inScope.forAttrName(el.ns, qname);
      if (ns == null) { return null; }
      // Normalize namespace.  Use the topmost one.
      ns = inScope.forUri(ns.uri);
      localName = qname.substring(colon + 1);
    }
    return new AttribKey(el, ns, localName);
  }

  /**
   * @param el an element key that describes the kind of element that attr
   *    does/will appear on.
   *    Normally, {@code el.equals(ElKey.forElement(attr.getOwnerElement())}.
   * @param attr not null.
   * @return a key describing attr.
   */
  public static AttribKey forAttribute(ElKey el, Attr attr) {
    String uri = attr.getNamespaceURI();
    String localName = attr.getLocalName();
    // uri can be null for weird elements like ISINDEX which the HTML5 parser
    // emulates using other HTML elements.
    Namespaces ns = uri != null ? Namespaces.HTML_DEFAULT.forUri(uri) : el.ns;
    // If there's no common name for it, create a namespace that has the
    // mandatory ancestors, and the attribute's namespace URI in the default
    // namespace.
    if (ns == null) { ns = new Namespaces(Namespaces.XML_SPECIAL, "", uri); }
    return new AttribKey(el, ns, localName);
  }

  /**
   * Returns a key that describes this key, but appearing on any element in its
   * namespace.
   */
  public AttribKey onAnyElement() {
    return onElement(ElKey.wildcard(ns));
  }

  /**
   * Returns the attribute key for this attribute but on a different element.
   */
  public AttribKey onElement(ElKey el) {
    if (el.equals(this.el)) { return this; }
    return new AttribKey(el, ns, localName);
  }

  public void format(MessageContext mc, Appendable out) throws IOException {
    if (el.ns.uri == ns.uri) {
      out.append(localName);
    } else {
      appendQName(ns, localName, out);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof AttribKey)) { return false; }
    AttribKey that = (AttribKey) o;
    return this.ns.uri == that.ns.uri
        && this.el.equals(that.el)
        && this.localName.equals(that.localName);
  }

  @Override
  public int hashCode() {
    return ns.uri.hashCode() + 31 * (localName.hashCode() + 31 * el.hashCode());
  }

  @Override
  public String toString() {
    StringBuilder out = new StringBuilder();
    try {
      appendQName(el.ns, el.localName, out);
      out.append("::");
      appendQName(ns, localName, out);
    } catch (IOException ex) {
      throw new RuntimeException("Appending to StringBuilder", ex);
    }
    return out.toString();
  }

  private static void appendQName(
      Namespaces ns, String localName, Appendable out)
      throws IOException {
    if (!"".equals(ns.prefix)) {
      out.append(ns.prefix).append(":");
    }
    out.append(localName);
  }

  public int compareTo(AttribKey that) {
    int delta = this.el.compareTo(that.el);
    if (delta == 0) {
      delta = this.ns.uri.compareTo(that.ns.uri);
      if (delta == 0) {
        delta = this.localName.compareTo(that.localName);
      }
    }
    return delta;
  }
}
