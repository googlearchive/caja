// Copyright (C) 2008 Google Inc.
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

package com.google.caja.lang.html;

import com.google.caja.parser.html.AttribKey;
import com.google.caja.parser.html.ElKey;
import com.google.caja.parser.html.Namespaces;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.SimpleMessageQueue;

import java.util.List;

import junit.framework.TestCase;

/**
 *
 * @author mikesamuel@gmail.com
 */
public class HtmlSchemaTest extends TestCase {
  MessageQueue mq;
  HtmlSchema schema;

  @Override
  public void setUp() throws Exception {
    mq = new SimpleMessageQueue();
    schema = HtmlSchema.getDefault(mq);
    assertTrue(mq.getMessages().isEmpty());
  }

  /** blacklist the whitelist. */
  public final void testSchema() throws Exception {
    assertFalse(schema.isElementAllowed(el("script")));
    assertFalse(schema.isElementAllowed(el("style")));
    // swapping innerHTML from an XMP or LISTING tag into another tag might
    // allow bad things to happen.
    assertFalse(schema.isElementAllowed(el("xmp")));
    assertFalse(schema.isElementAllowed(el("listing")));
    assertFalse(schema.isElementAllowed(el("frame")));
    assertFalse(schema.isElementAllowed(el("frameset")));
    assertFalse(schema.isElementAllowed(el("body")));
    assertFalse(schema.isElementAllowed(el("head")));
    assertFalse(schema.isElementAllowed(el("html")));
    assertFalse(schema.isElementAllowed(el("title")));
    assertTrue(schema.isElementAllowed(el("div")));
    assertTrue(schema.isElementAllowed(el("span")));
  }

  public final void testAttributeTypes() throws Exception {
    assertEquals(HTML.Attribute.Type.STYLE,
                 lookupAttribute("div", "style").getType());
    assertEquals(HTML.Attribute.Type.SCRIPT,
                 lookupAttribute("a", "onclick").getType());
    assertEquals(HTML.Attribute.Type.URI,
                 lookupAttribute("a", "href").getType());
    assertEquals(HTML.Attribute.Type.NONE,
                 lookupAttribute("a", "title").getType());
  }

  public final void testAttributeMimeTypes() throws Exception {
    assertEquals("image/*", lookupAttribute("img", "src").getMimeTypes());
    assertEquals(
        "text/javascript", lookupAttribute("script", "src").getMimeTypes());
    assertNull(lookupAttribute("table", "cellpadding").getMimeTypes());
  }

  public final void testAttributeCriteria() throws Exception {
    assertFalse(lookupAttribute("a", "target")
                .getValueCriterion().accept("_top"));
    assertTrue(lookupAttribute("a", "target")
               .getValueCriterion().accept("_blank"));

    assertFalse(lookupAttribute("table", "cellpadding")
                .getValueCriterion().accept("six"));
    assertTrue(lookupAttribute("table", "cellpadding")
               .getValueCriterion().accept("6"));
    assertTrue(lookupAttribute("table", "width")
               .getValueCriterion().accept("10%"));
    assertFalse(lookupAttribute("table", "width")
               .getValueCriterion().accept("%"));

    assertFalse(lookupAttribute("script", "type")
                .getValueCriterion().accept("text/vbscript"));
    assertTrue(lookupAttribute("script", "type")
               .getValueCriterion().accept("text/javascript"));
    assertTrue(lookupAttribute("script", "type")
               .getValueCriterion().accept("text/javascript;charset=UTF-8"));
    assertTrue(lookupAttribute("input", "type")
               .getValueCriterion().accept("text"));
    assertTrue(lookupAttribute("input", "type")
               .getValueCriterion().accept("TEXT"));
    assertTrue(lookupAttribute("input", "type")
               .getValueCriterion().accept("button"));
    assertFalse(lookupAttribute("input", "type")
                .getValueCriterion().accept("file"));
    assertFalse(lookupAttribute("input", "type")
                .getValueCriterion().accept("FILE"));
    assertFalse(lookupAttribute("input", "type")
                .getValueCriterion().accept("bogus"));
    assertFalse(lookupAttribute("input", "type")
                .getValueCriterion().accept(""));
    assertTrue(lookupAttribute("input", "checked")
              .getValueCriterion().accept("checked"));
    assertTrue(lookupAttribute("input", "checked")
               .getValueCriterion().accept(""));
    assertFalse(lookupAttribute("input", "checked")
                .getValueCriterion().accept("turkey"));
    assertFalse(lookupAttribute("input", "checked")
                .getValueCriterion().accept("no"));
  }

  public final void testSafeAndDefaultValues() {
    HTML.Attribute tgt = lookupAttribute("a", "target");
    assertEquals("_blank", tgt.getSafeValue());
    assertEquals("_self", tgt.getDefaultValue());
  }

  public final void testAttributeList() {
    HTML.Element a = schema.lookupElement(el("a"));
    assertEquals(lookupAttribute("A", "HREF"), withName(a, "HREF"));
    assertEquals(lookupAttribute("A", "ID"), withName(a, "ID"));
    assertEquals(null, withName(a, "COLSPAN"));
    HTML.Element b = schema.lookupElement(el("b"));
    assertEquals(lookupAttribute("B", "ID"), withName(b, "ID"));
  }

  private HTML.Attribute lookupAttribute(
      String qualifiedEl, String qualifiedAttr) {
    AttribKey attr = AttribKey.forAttribute(
        Namespaces.HTML_DEFAULT, el(qualifiedEl), qualifiedAttr);
    return schema.lookupAttribute(attr);
  }

  private static HTML.Attribute withName(HTML.Element el, String qname) {
    List<HTML.Attribute> attrs = el.getAttributes();
    AttribKey key = AttribKey.forAttribute(
        Namespaces.HTML_DEFAULT, el.getKey(), qname);
    HTML.Attribute result = null;
    for (HTML.Attribute a : attrs) {
      if (key.localName.equals(a.getKey().localName)
          && key.ns.uri == a.getKey().ns.uri) {
        if (result != null) { throw new IllegalStateException("DUPE " + key); }
        result = a;
      }
    }
    return result;
  }

  private static ElKey el(String qualifiedElName) {
    return ElKey.forElement(Namespaces.HTML_DEFAULT, qualifiedElName);
  }
}
