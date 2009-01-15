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
  public void testSchema() throws Exception {
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
  }

  public void testAttributeTypes() throws Exception {
    assertEquals(HTML.Attribute.Type.STYLE,
                 schema.lookupAttribute(id("div"), id("style")).getType());
    assertEquals(HTML.Attribute.Type.SCRIPT,
                 schema.lookupAttribute(id("a"), id("onclick")).getType());
    assertEquals(HTML.Attribute.Type.URI,
                 schema.lookupAttribute(id("a"), id("href")).getType());
    assertEquals(HTML.Attribute.Type.NONE,
                 schema.lookupAttribute(id("a"), id("title")).getType());
  }

  public void testAttributeMimeTypes() throws Exception {
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

  public void testAttributeCriteria() throws Exception {
    assertFalse(schema.getAttributeCriteria(id("a"), id("target"))
                .accept("_top"));
    assertTrue(schema.getAttributeCriteria(id("a"), id("target"))
               .accept("_blank"));

    assertFalse(schema.getAttributeCriteria(id("table"), id("cellpadding"))
                .accept("six"));
    assertTrue(schema.getAttributeCriteria(id("table"), id("cellpadding"))
               .accept("6"));
    assertTrue(schema.getAttributeCriteria(id("table"), id("width"))
               .accept("10%"));
    assertFalse(schema.getAttributeCriteria(id("table"), id("width"))
               .accept("%"));

    assertFalse(schema.getAttributeCriteria(id("script"), id("type"))
                .accept("text/vbscript"));
    assertTrue(schema.getAttributeCriteria(id("script"), id("type"))
               .accept("text/javascript"));
    assertTrue(schema.getAttributeCriteria(id("script"), id("type"))
               .accept("text/javascript;charset=UTF-8"));
    assertTrue(schema.getAttributeCriteria(id("input"), id("type"))
               .accept("text"));
    assertTrue(schema.getAttributeCriteria(id("input"), id("type"))
               .accept("TEXT"));
    assertTrue(schema.getAttributeCriteria(id("input"), id("type"))
               .accept("button"));
    assertFalse(schema.getAttributeCriteria(id("input"), id("type"))
                .accept("file"));
    assertFalse(schema.getAttributeCriteria(id("input"), id("type"))
                .accept("FILE"));
    assertFalse(schema.getAttributeCriteria(id("input"), id("type"))
                .accept("bogus"));
  }

  private static Name id(String name) {
    return Name.html(name);
  }
}
