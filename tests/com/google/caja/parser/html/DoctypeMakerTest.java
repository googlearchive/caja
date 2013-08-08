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

import com.google.caja.util.Function;

import org.w3c.dom.DOMImplementation;
import org.w3c.dom.DocumentType;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;

import junit.framework.TestCase;

@SuppressWarnings("static-method")
public class DoctypeMakerTest extends TestCase {
  public final void testSystemIdToNsUri() {
    assertEquals(
        Namespaces.HTML_NAMESPACE_URI,
        DoctypeMaker.systemIdToNsUri("http://www.w3.org/TR/html4/strict.dtd"));
    assertEquals(
        Namespaces.HTML_NAMESPACE_URI,
        DoctypeMaker.systemIdToNsUri("http://www.w3.org/TR/html4/loose.dtd"));
    assertEquals(
        Namespaces.HTML_NAMESPACE_URI,
        DoctypeMaker.systemIdToNsUri(
            "http://www.w3.org/TR/html4/frameset.dtd"));
    assertEquals(
        Namespaces.HTML_NAMESPACE_URI,
        DoctypeMaker.systemIdToNsUri(
            "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd"));
    assertEquals(
        Namespaces.HTML_NAMESPACE_URI,
        DoctypeMaker.systemIdToNsUri(
            "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd"));
    assertEquals(
        Namespaces.HTML_NAMESPACE_URI,
        DoctypeMaker.systemIdToNsUri(
            "http://www.w3.org/TR/xhtml1/DTD/xhtml1-frameset.dtd"));
    assertEquals(
        Namespaces.SVG_NAMESPACE_URI,
        DoctypeMaker.systemIdToNsUri(
            "http://www.w3.org/Graphics/SVG/1.1/DTD/svg-framework.dtd"));
    assertEquals(
        null,
        DoctypeMaker.systemIdToNsUri(
            "http://bobs-house-of-doctypes/ONE/WELL/DONE/DTD/coming-up.dtd"));
  }

  public final void testIsHtml() {
    assertTrue(DoctypeMaker.isHtml(
        "p", "-//W3C//DTD HTML 4.01//EN", null));
    assertTrue(DoctypeMaker.isHtml(
        "p", "-//W3C//DTD HTML 4.01//EN",
        "http://www.w3.org/TR/html4/strict.dtd"));
    assertTrue(DoctypeMaker.isHtml(
        "p", "-//W3C//DTD HTML 4.01 Transitional//EN", null));
    assertTrue(DoctypeMaker.isHtml(
        "p", "-//W3C//DTD HTML 4.01 Transitional//EN",
        "http://www.w3.org/TR/html4/loose.dtd"));
    assertTrue(DoctypeMaker.isHtml(
        "p", "-//W3C//DTD HTML 4.01 Frameset//EN", null));
    assertTrue(DoctypeMaker.isHtml(
        "p", "-//W3C//DTD HTML 4.01 Frameset//EN",
        "http://www.w3.org/TR/html4/frameset.dtd"));
    assertTrue(DoctypeMaker.isHtml(
        "p", "-//W3C//DTD XHTML 1.0 Strict//EN", null));
    assertTrue(DoctypeMaker.isHtml(
        "p", "-//W3C//DTD XHTML 1.0 Strict//EN",
        "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd"));
    assertTrue(DoctypeMaker.isHtml(
        "p", "-//W3C//DTD XHTML 1.0 Transitional//EN", null));
    assertTrue(DoctypeMaker.isHtml(
        "p", "-//W3C//DTD XHTML 1.0 Transitional//EN",
        "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd"));
    assertTrue(DoctypeMaker.isHtml(
        "p", "-//W3C//DTD XHTML 1.0 Frameset//EN", null));
    assertTrue(DoctypeMaker.isHtml(
        "p", "-//W3C//DTD XHTML 1.0 Frameset//EN",
        "http://www.w3.org/TR/xhtml1/DTD/xhtml1-frameset.dtd"));
    assertTrue(DoctypeMaker.isHtml(
        "p", "-//W3C//DTD HTML 3.2 Final//EN", null));
    assertTrue(DoctypeMaker.isHtml("p", "-//IETF//DTD HTML//EN", null));
    assertTrue(DoctypeMaker.isHtml("html", null, null));
    assertFalse(DoctypeMaker.isHtml(
        "svg", "-//W3C//ENTITIES SVG 1.1 Modular Framework//EN",
        "http://www.w3.org/Graphics/SVG/1.1/DTD/svg-framework.dtd"));
    assertFalse(DoctypeMaker.isHtml(
        "svg", null,
        "http://www.w3.org/Graphics/SVG/1.1/DTD/svg-framework.dtd"));
    assertFalse(DoctypeMaker.isHtml(
        "svg", "-//W3C//ENTITIES SVG 1.1 Modular Framework//EN", null));
    assertFalse(DoctypeMaker.isHtml("svg", null, null));
    assertFalse(DoctypeMaker.isHtml(
        "html", "-//W3C//ENTITIES SVG 1.1 Modular Framework//EN",
        "http://www.w3.org/Graphics/SVG/1.1/DTD/svg-framework.dtd"));
    assertFalse(DoctypeMaker.isHtml(
        "html", null,
        "http://www.w3.org/Graphics/SVG/1.1/DTD/svg-framework.dtd"));
    assertFalse(DoctypeMaker.isHtml(
        "html", "-//W3C//ENTITIES SVG 1.1 Modular Framework//EN", null));
  }

  public final void testParse() throws Exception {
    assertDoctype(
        "html", "-//W3C//DTD HTML 4.01//EN",
        "http://www.w3.org/TR/html4/strict.dtd",
        "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\""
        + " \"http://www.w3.org/TR/html4/strict.dtd\">");

    assertDoctype(
        "html", "-//W3C//DTD HTML 4.01 Transitional//EN",
        "http://www.w3.org/TR/html4/loose.dtd",
        "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\""
        + " \"http://www.w3.org/TR/html4/loose.dtd\">");

    assertDoctype(
        "html", "-//W3C//DTD HTML 4.01 Transitional//EN", null,
        "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\">");

    assertDoctype(
        "html", "-//W3C//DTD HTML 4.01 Frameset//EN",
        "http://www.w3.org/TR/html4/frameset.dtd",
        "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Frameset//EN\""
        + " \"http://www.w3.org/TR/html4/frameset.dtd\">");

    assertDoctype(
        "html", "-//W3C//DTD XHTML 1.0 Strict//EN",
        "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd",
        "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\""
        + " \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">");

    assertDoctype(
        "html", "-//W3C//DTD XHTML 1.0 Transitional//EN",
        "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd",
        "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\""
        + " \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">");

    assertDoctype(
        "html", "-//W3C//DTD XHTML 1.0 Frameset//EN",
        "http://www.w3.org/TR/xhtml1/DTD/xhtml1-frameset.dtd",
        "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Frameset//EN\""
        + " \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-frameset.dtd\">");

    assertDoctype(
        "html", "-//W3C//DTD HTML 3.2 Final//EN", null,
        "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 3.2 Final//EN\">");

    assertDoctype(
        "html", "-//IETF//DTD HTML//EN", null,
        "<!DOCTYPE HTML PUBLIC \"-//IETF//DTD HTML//EN\">");

    assertDoctype("html", null, null, "<!DOCTYPE HTML>");

    assertDoctype("html", null, null, "<!DOCTYPE html>");

    // Test whether omitting whitespace b/w public id and system id allows
    // doctype to be parsed correctly.
    assertDoctype("html", "-//W3C//DTD HTML 4.01 Transitional//EN",
                  "http://www.w3.org/TR/html4/loose.dtd",
                  "<!DOCTYPE HTML PUBLIC "
                  + "\"-//W3C//DTD HTML 4.01 Transitional//EN\""
                  + "\"http://www.w3.org/TR/html4/loose.dtd\">");

    assertDoctype("html", "-//W3C//DTD XHTML 1.0 Transitional//EN",
                  "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd",
                  "<!DOCTYPE html PUBLIC "
                  + "\"-//W3C//DTD XHTML 1.0 Transitional//EN\" "
                  + "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">");
}

  private static void assertDoctype(
      String name, String pubid, String systemId, String text)
      throws Exception {
    Function<DOMImplementation, DocumentType> maker = DoctypeMaker.parse(text);
    if (name == null) {
      assertNull(text, maker);
      return;
    } else {
      assertNotNull(text, maker);
      DOMImplementation impl = DOMImplementationRegistry.newInstance()
          .getDOMImplementation("XML 1.0 Traversal");
      DocumentType doctype = maker.apply(impl);
      assertEquals(text, name, doctype.getName());
      assertEquals(text, systemId, doctype.getSystemId());
      assertEquals(text, pubid, doctype.getPublicId());
    }
  }
}
