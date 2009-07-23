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

import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.SimpleMessageQueue;
import com.google.caja.util.Name;

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
    assertFalse(schema.isElementAllowed(id("script")));
    assertFalse(schema.isElementAllowed(id("style")));
    assertFalse(schema.isElementAllowed(id("iframe")));
    // swapping innerHTML from an XMP or LISTING tag into another tag might
    // allow bad things to happen.
    assertFalse(schema.isElementAllowed(id("xmp")));
    assertFalse(schema.isElementAllowed(id("listing")));
    assertFalse(schema.isElementAllowed(id("frame")));
    assertFalse(schema.isElementAllowed(id("frameset")));
    assertFalse(schema.isElementAllowed(id("body")));
    assertFalse(schema.isElementAllowed(id("head")));
    assertFalse(schema.isElementAllowed(id("html")));
    assertFalse(schema.isElementAllowed(id("title")));
    assertTrue(schema.isElementAllowed(id("div")));
    assertTrue(schema.isElementAllowed(id("span")));
  }

  public final void testAttributeTypes() throws Exception {
    assertEquals(HTML.Attribute.Type.STYLE,
                 schema.lookupAttribute(id("div"), id("style")).getType());
    assertEquals(HTML.Attribute.Type.SCRIPT,
                 schema.lookupAttribute(id("a"), id("onclick")).getType());
    assertEquals(HTML.Attribute.Type.URI,
                 schema.lookupAttribute(id("a"), id("href")).getType());
    assertEquals(HTML.Attribute.Type.NONE,
                 schema.lookupAttribute(id("a"), id("title")).getType());
  }

  public final void testAttributeMimeTypes() throws Exception {
    assertEquals(
        "image/*",
        schema.lookupAttribute(id("img"), id("src")).getMimeTypes());
    assertEquals(
        "text/javascript",
        schema.lookupAttribute(id("script"), id("src")).getMimeTypes());
    assertEquals(
        null,
        schema.lookupAttribute(id("table"), id("cellpadding")).getMimeTypes());
  }

  public final void testAttributeCriteria() throws Exception {
    assertFalse(schema.lookupAttribute(id("a"), id("target"))
                .getValueCriterion().accept("_top"));
    assertTrue(schema.lookupAttribute(id("a"), id("target"))
               .getValueCriterion().accept("_blank"));

    assertFalse(schema.lookupAttribute(id("table"), id("cellpadding"))
                .getValueCriterion().accept("six"));
    assertTrue(schema.lookupAttribute(id("table"), id("cellpadding"))
               .getValueCriterion().accept("6"));
    assertTrue(schema.lookupAttribute(id("table"), id("width"))
               .getValueCriterion().accept("10%"));
    assertFalse(schema.lookupAttribute(id("table"), id("width"))
               .getValueCriterion().accept("%"));

    assertFalse(schema.lookupAttribute(id("script"), id("type"))
                .getValueCriterion().accept("text/vbscript"));
    assertTrue(schema.lookupAttribute(id("script"), id("type"))
               .getValueCriterion().accept("text/javascript"));
    assertTrue(schema.lookupAttribute(id("script"), id("type"))
               .getValueCriterion().accept("text/javascript;charset=UTF-8"));
    assertTrue(schema.lookupAttribute(id("input"), id("type"))
               .getValueCriterion().accept("text"));
    assertTrue(schema.lookupAttribute(id("input"), id("type"))
               .getValueCriterion().accept("TEXT"));
    assertTrue(schema.lookupAttribute(id("input"), id("type"))
               .getValueCriterion().accept("button"));
    assertFalse(schema.lookupAttribute(id("input"), id("type"))
                .getValueCriterion().accept("file"));
    assertFalse(schema.lookupAttribute(id("input"), id("type"))
                .getValueCriterion().accept("FILE"));
    assertFalse(schema.lookupAttribute(id("input"), id("type"))
                .getValueCriterion().accept("bogus"));
  }

  public final void testSafeAndDefaultValues() {
    HTML.Attribute tgt = schema.lookupAttribute(id("a"), id("target"));
    assertEquals("_blank", tgt.getSafeValue());
    assertEquals("_self", tgt.getDefaultValue());
  }

  public final void testAttributeList() {
    HTML.Element a = schema.lookupElement(id("a"));
    assertEquals(
        schema.lookupAttribute(id("A"), id("HREF")),
        withId(a.getAttributes(), id("HREF")));
    assertEquals(
        schema.lookupAttribute(id("A"), id("ID")),
        withId(a.getAttributes(), id("ID")));
    assertEquals(null, withId(a.getAttributes(), id("COLSPAN")));
    HTML.Element b = schema.lookupElement(id("b"));
    assertEquals(
        schema.lookupAttribute(id("B"), id("ID")),
        withId(b.getAttributes(), id("ID")));
  }
  private static HTML.Attribute withId(List<HTML.Attribute> attrs, Name name) {
    HTML.Attribute result = null;
    for (HTML.Attribute a : attrs) {
      if (name.equals(a.getAttributeName())) {
        if (result != null) { throw new IllegalStateException("DUPE"); }
        result = a;
      }
    }
    return result;
  }

  private static Name id(String name) {
    return Name.html(name);
  }
}
