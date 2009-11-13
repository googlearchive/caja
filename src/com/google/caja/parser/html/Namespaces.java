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

import com.google.caja.util.Strings;

/**
 * A mapping of namespace prefixes to namespace URIs.
 *
 * @author mikesamuel@gmail.com
 * @see <a href="http://www.w3.org/TR/xml-names/">Namespaces in XML 1.0</a>
 */
public final class Namespaces {
  public final Namespaces parent;
  public final String prefix;
  /**
   * An interned namespace URI.  Per the XML namespaces spec, namespace URIs
   * are not subject to URI normalization, and so can be compared lexically.
   */
  public final String uri;

  public static String HTML_NAMESPACE_URI = "http://www.w3.org/1999/xhtml";
  public static String XML_NAMESPACE_URI
      = "http://www.w3.org/XML/1998/namespace";
  public static String XMLNS_NAMESPACE_URI = "http://www.w3.org/2000/xmlns/";
  public static String SVG_NAMESPACE_URI = "http://www.w3.org/2000/svg";

  private static final Namespaces XMLNS = new Namespaces(
      "xmlns", XMLNS_NAMESPACE_URI);
  /**
   * Must always be present according to the XML namespaces spec.
   * @see <a href="http://www.w3.org/TR/xml-names/#ns-decl">constraints</a>
   */
  public static final Namespaces XML_SPECIAL = new Namespaces(
      XMLNS, "xml", XML_NAMESPACE_URI);
  /** Some common namespaces good for parsing XML fragments. */
  public static final Namespaces COMMON = new Namespaces(
      new Namespaces(
          new Namespaces(
              new Namespaces(
                  new Namespaces(
                      XML_SPECIAL,
                      "ihtml", "http://code.google.com/p/google-caja/ihtml"),
                  "xsl", "http://www.w3.org/1999/XSL/Transform"),
              "os", "http://ns.opensocial.org/2008/markup"),
          "svg", SVG_NAMESPACE_URI),
      "html", HTML_NAMESPACE_URI);
  /**
   * A namespace with the common namespaces and with HTML as the default
   * namespace.
   */
  public static final Namespaces HTML_DEFAULT = new Namespaces(
      COMMON, "", HTML_NAMESPACE_URI);

  public Namespaces(Namespaces parent, String prefix, String uri) {
    assert parent != null;
    assert prefix.indexOf(':') < 0;
    this.parent = parent;
    this.prefix = prefix;
    this.uri = uri.intern();
  }

  private Namespaces(String prefix, String uri) {
    this.parent = null;
    this.prefix = prefix;
    this.uri = uri.intern();
  }

  public static boolean isHtml(String namespaceUri) {
    return HTML_NAMESPACE_URI.equals(namespaceUri);
  }

  public static String localName(String uri, String qualifiedName) {
    String localName = qualifiedName.substring(qualifiedName.indexOf(':') + 1);
    return isHtml(uri) ? Strings.toLowerCase(localName) : localName;
  }

  public Namespaces forElementName(String qname) {
    int index = qname.indexOf(':');
    return forPrefix(qname, index < 0 ? 0 : index);
  }

  public Namespaces forAttrName(Namespaces elNs, String qname) {
    if ("xmlns".equals(qname)) { return XMLNS; }
    int index = qname.indexOf(':');
    if (index < 0) { return elNs; }
    return forPrefix(qname, index < 0 ? 0 : index);
  }

  private Namespaces lastForUri;
  private Namespaces lastForPrefix;

  public Namespaces forPrefix(String prefix) {
    return forPrefix(prefix, prefix.length());
  }

  private Namespaces forPrefix(String qname, int colon) {
    if (lastForPrefix != null && colon == lastForPrefix.prefix.length()
        && qname.regionMatches(0, lastForPrefix.prefix, 0, colon)) {
      return lastForPrefix;
    }
    Namespaces ns = forPrefix(this, qname, colon);
    if (ns != null) { lastForPrefix = ns; }
    return ns;
  }

  private static Namespaces forPrefix(
      Namespaces ns, String qname, int end) {
    do {
      if (end == ns.prefix.length()
          && ns.prefix.regionMatches(0, qname, 0, end)) {
        return ns;
      }
    } while ((ns = ns.parent) != null);
    return null;
  }

  public Namespaces forUri(String uri) {
    uri = uri.intern();
    if (lastForUri != null && uri == lastForUri.uri) { return lastForUri; }
    Namespaces ns = forUri(this, uri);
    if (ns != null) { lastForUri = ns; }
    return ns;
  }

  private static Namespaces forUri(Namespaces ns, String uri) {
    Namespaces p = ns;
    parentloop:
    do {
      if (p.uri == uri) {
        // Now make sure that the prefix is not masked.
        // This is worst-case O(n**2) on XML like:
        //     <a xmlns:foo="url1">
        //       <b xmlns:foo="url2">
        //         <c xmlns:foo="url3">
        //           ...
        // Don't do that.  It's dumb.
        for (Namespaces c = ns; c != p; c = c.parent) {
          if (p.prefix.equals(c.prefix)) { continue parentloop; }
        }
        return p;
      }
    } while ((p = p.parent) != null);
    return null;
  }

  @Override
  public String toString() {
    return "[" + (this.prefix.length() != 0 ? "xmlns:" + this.prefix : "xmlns")
        + "=" + this.uri + "]";
  }
}
