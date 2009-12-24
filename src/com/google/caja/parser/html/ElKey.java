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

import com.google.caja.lang.html.HtmlSchema;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessagePart;
import com.google.caja.util.Strings;

import java.io.IOException;

import org.w3c.dom.Element;

/**
 * A normalized qualified element name used to lookup element definitions in
 * an {@link HtmlSchema HTML schema}.
 *
 * @author mikesamuel@gmail.com
 */
public final class ElKey implements MessagePart, Comparable<ElKey> {
  /**
   * The namespace in which elements described by this key appear.
   * For an element e described by this key,
   * {@code e.getNamespaceURI().equals(this.ns.uri)}.
   */
  public final Namespaces ns;
  /**
   * The namespace in which elements described by this key appear.
   * For an element e described by this key,
   * {@code e.getLocalName().equals(this.localName)} after the local name has
   * been normalized.
   */
  public final String localName;

  /**
   * @param localName normalized.
   */
  private ElKey(Namespaces ns, String localName) {
    assert ns != null && localName != null;
    this.ns = ns;
    this.localName = localName;
  }

  private static final Namespaces HTML_NS = Namespaces.HTML_DEFAULT.forUri(
      Namespaces.HTML_NAMESPACE_URI);

  public static final ElKey HTML_WILDCARD = new ElKey(
      Namespaces.HTML_DEFAULT, "*");

  /** A key that describes all elements in the given namespace. */
  public static ElKey wildcard(Namespaces ns) {
    if (ns.uri == Namespaces.HTML_NAMESPACE_URI) { return HTML_WILDCARD; }
    return new ElKey(ns, "*");
  }

  public boolean isHtml() { return ns.uri == Namespaces.HTML_NAMESPACE_URI; }

  public boolean is(Element el) {
    return ns.uri.equals(el.getNamespaceURI())
        && ("*".equals(localName) || localName.equals(el.getLocalName()));
  }

  public static ElKey forHtmlElement(String localName) {
    return new ElKey(HTML_NS, Strings.toLowerCase(localName));
  }

  public static ElKey forElement(Namespaces inScope, String qname) {
    int colon = qname.indexOf(':');
    String localName = colon < 0 ? qname : qname.substring(colon + 1);
    Namespaces ns = inScope.forElementName(qname);
    if (ns == null) { return null; }
    // Normalize namespace.  Use the topmost one with that URI.
    ns = inScope.forUri(ns.uri);
    // ns is non-null because the namespace returned by forElementName cannot
    // possibly be masked on inScope.
    if (ns.uri == Namespaces.HTML_NAMESPACE_URI) {
      localName = Strings.toLowerCase(localName);
    }
    return new ElKey(ns, localName);
  }

  public static ElKey forElement(Element el) {
    String uri = el.getNamespaceURI();
    String localName = el.getLocalName();
    Namespaces ns = Namespaces.HTML_DEFAULT.forUri(uri);
    // If there's no common name for it, create a namespace that has the
    // mandatory ancestors, and the elements's namespace URI in the default
    // namespace.
    if (ns == null) { ns = new Namespaces(Namespaces.XML_SPECIAL, "", uri); }
    if (ns.uri == Namespaces.HTML_NAMESPACE_URI) {
      localName = Strings.toLowerCase(localName);
    }
    return new ElKey(ns, localName);
  }

  public void format(MessageContext mc, Appendable out) throws IOException {
    if (!"".equals(ns.prefix)) {
      out.append(ns.prefix).append(':');
    }
    out.append(localName);
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof ElKey)) { return false; }
    ElKey that = (ElKey) o;
    return this.ns.uri == that.ns.uri && this.localName.equals(that.localName);
  }

  private int hc;
  @Override
  public int hashCode() {
    if (this.hc == 0) {
      int hc = ns.uri.hashCode() + 31 * localName.hashCode();
      this.hc = hc != 0 ? hc : -1;
    }
    return this.hc;
  }

  @Override
  public String toString() {
    StringBuilder out = new StringBuilder();
    if (!"".equals(ns.prefix)) {
      out.append(ns.prefix).append(':');
    }
    return out.append(localName).toString();
  }

  public int compareTo(ElKey that) {
    int delta = this.ns.uri.compareTo(that.ns.uri);
    if (delta != 0) { return delta; }
    return this.localName.compareTo(that.localName);
  }
}
