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

import junit.framework.TestCase;

public class NamespacesTest extends TestCase {
  Namespaces ns = new Namespaces(
      new Namespaces(Namespaces.COMMON, "foo", "http://foo.com/bar"),
      "", "http://empty.string/");

  public final void testNamespaces() {
    assertEquals("xmlns", ns.forPrefix("xmlns").prefix);
    assertEquals(Namespaces.XMLNS_NAMESPACE_URI, ns.forPrefix("xmlns").uri);
    assertEquals("xml", ns.forPrefix("xml").prefix);
    assertEquals(Namespaces.XML_NAMESPACE_URI, ns.forPrefix("xml").uri);
    assertEquals("html", ns.forPrefix("html").prefix);
    assertEquals("html", ns.forPrefix("html").prefix);
    assertEquals(Namespaces.HTML_NAMESPACE_URI, ns.forPrefix("html").uri);
    assertEquals(Namespaces.HTML_NAMESPACE_URI, ns.forPrefix("html").uri);
    assertEquals("http://empty.string/", ns.forPrefix("").uri);
    assertEquals("", ns.forUri("http://empty.string/").prefix);
    assertEquals(null, ns.forPrefix("bogus"));
  }

  public final void testElementQNames() {
    assertEquals("http://empty.string/", ns.forElementName("foo").uri);
    assertEquals(
        Namespaces.XML_NAMESPACE_URI, ns.forElementName("xml:foo").uri);
  }

  public final void testAttrQNames() {
    assertEquals(
        Namespaces.XML_NAMESPACE_URI,
        ns.forAttrName(ns.forPrefix("xml"), "foo").uri);
    assertEquals(
        Namespaces.XML_NAMESPACE_URI,
        ns.forAttrName(ns.forPrefix("xml"), "xml:foo").uri);
    assertEquals(
        Namespaces.HTML_NAMESPACE_URI,
        ns.forAttrName(ns.forPrefix("xml"), "html:foo").uri);
    assertEquals(
        Namespaces.XML_NAMESPACE_URI,
        ns.forAttrName(ns.forPrefix("xml"), "foo").uri);
  }

  public final void testPrefixMasking() {
    Namespaces fooMasked = new Namespaces(ns, ns.prefix, "http://other.com/");
    assertEquals(fooMasked, fooMasked.forUri(fooMasked.uri));
    assertEquals(fooMasked, fooMasked.forPrefix(fooMasked.prefix));
    assertEquals(null, fooMasked.forUri(ns.uri));  // ns masked
  }
}
