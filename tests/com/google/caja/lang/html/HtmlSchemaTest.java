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
    assertFalse(schema.isElementAllowed("script"));
    assertFalse(schema.isElementAllowed("style"));
    assertFalse(schema.isElementAllowed("iframe"));
    // swapping innerHTML from an XMP or LISTING tag into another tag might
    // allow bad things to happen.
    assertFalse(schema.isElementAllowed("xmp"));
    assertFalse(schema.isElementAllowed("listing"));
    assertFalse(schema.isElementAllowed("frame"));
    assertFalse(schema.isElementAllowed("frameset"));
    assertFalse(schema.isElementAllowed("body"));
    assertFalse(schema.isElementAllowed("head"));
    assertFalse(schema.isElementAllowed("html"));
    assertFalse(schema.isElementAllowed("title"));
  }

  public void testAttributeTypes() throws Exception {
    assertEquals(HTML.Attribute.Type.STYLE,
                 schema.lookupAttribute("div", "style").getType());
    assertEquals(HTML.Attribute.Type.SCRIPT,
                 schema.lookupAttribute("a", "onclick").getType());
    assertEquals(HTML.Attribute.Type.URI,
                 schema.lookupAttribute("a", "href").getType());
    assertEquals(HTML.Attribute.Type.NONE,
                 schema.lookupAttribute("a", "title").getType());
  }

  public void testAttributeMimeTypes() throws Exception {
    assertEquals("image/*",
                 schema.lookupAttribute("img", "src").getMimeTypes());
    assertEquals("text/javascript",
                 schema.lookupAttribute("script", "src").getMimeTypes());
    assertEquals(null,
                 schema.lookupAttribute("table", "cellpadding").getMimeTypes());
  }

  public void testAttributeCriteria() throws Exception {
    assertFalse(schema.getAttributeCriteria("a", "target").accept("_top"));
    assertTrue(schema.getAttributeCriteria("a", "target").accept("_blank"));

    assertFalse(schema.getAttributeCriteria("table", "cellpadding")
                .accept("six"));
    assertTrue(schema.getAttributeCriteria("table", "cellpadding").accept("6"));

    assertFalse(schema.getAttributeCriteria("script", "type")
                .accept("text/vbscript"));
    assertTrue(schema.getAttributeCriteria("script", "type")
               .accept("text/javascript"));
    assertTrue(schema.getAttributeCriteria("script", "type")
               .accept("text/javascript;charset=UTF-8"));
    assertTrue(schema.getAttributeCriteria("input", "type").accept("text"));
    assertTrue(schema.getAttributeCriteria("input", "type").accept("TEXT"));
    assertTrue(schema.getAttributeCriteria("input", "type").accept("button"));
    assertFalse(schema.getAttributeCriteria("input", "type").accept("file"));
    assertFalse(schema.getAttributeCriteria("input", "type").accept("FILE"));
    assertFalse(schema.getAttributeCriteria("input", "type").accept("bogus"));
  }
}
