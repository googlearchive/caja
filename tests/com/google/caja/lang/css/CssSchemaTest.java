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

package com.google.caja.lang.css;

import com.google.caja.parser.css.CssPropertySignature;
import com.google.caja.reporting.EchoingMessageQueue;
import com.google.caja.reporting.MessageContext;
import com.google.caja.util.Name;

import java.io.PrintWriter;

import junit.framework.TestCase;

/**
 *
 * @author mikesamuel@gmail.com
 */
public class CssSchemaTest extends TestCase {
  private CssSchema cssSchema;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    cssSchema = CssSchema.getDefaultCss21Schema(
        new EchoingMessageQueue(
            new PrintWriter(System.err), new MessageContext()));
  }

  @Override
  public void tearDown() throws Exception {
    super.tearDown();
    cssSchema = null;
  }

  public final void testGetCssProperty() {
    assertNull(cssSchema.getCssProperty(Name.css("bogus")));
    assertNotNull(cssSchema.getCssProperty(Name.css("font-style")));
  }

  public final void testGetSymbol() {
    CssSchema.SymbolInfo si = cssSchema.getSymbol(Name.css("color"));
    assertNotNull(si);
    assertNotNull(si.sig);
    for (CssPropertySignature sig : si.sig.children()) {
      Object value = sig.getValue();
      if (value instanceof String && "aquamarine".equals(value)) {
        return;
      }
    }
    fail("Cannot find value \"aquamarine\" for symbol \"color\"");
  }

  public final void testIsKeyword() {
    assertTrue(cssSchema.isKeyword(Name.css("inherit")));
    assertTrue(cssSchema.isKeyword(Name.css("default")));
    assertTrue(cssSchema.isKeyword(Name.css("initial")));
    assertTrue(cssSchema.isKeyword(Name.css("auto")));
    assertTrue(cssSchema.isKeyword(Name.css("sans-serif")));
    assertTrue(cssSchema.isKeyword(Name.css("monospace")));
    assertTrue(cssSchema.isKeyword(Name.css("DEFAULT")));
    assertTrue(cssSchema.isKeyword(Name.css("INHERIT")));
    assertFalse(cssSchema.isKeyword(Name.css("not-a-keyword")));
    assertFalse(cssSchema.isKeyword(Name.css("notakeyword")));
  }

  public final void testIsPropertyAllowed() {
    assertTrue(cssSchema.isPropertyAllowed(Name.css("color")));
    assertFalse(cssSchema.isPropertyAllowed(Name.css("expression")));
  }

  public final void testIsFunctionAllowed() {
    assertTrue(cssSchema.isFunctionAllowed(Name.css("rgb")));
    assertTrue(cssSchema.isFunctionAllowed(Name.css("rgba")));
    assertFalse(cssSchema.isFunctionAllowed(Name.css("rgbb")));
    assertFalse(cssSchema.isFunctionAllowed(Name.css("expression")));
  }

  public final void testPropertyInfo() {
    assertEquals(null, cssSchema.getCssProperty(Name.css("bogus")));

    CssSchema.CssPropertyInfo colorInfo
        = cssSchema.getCssProperty(Name.css("color"));
    assertEquals(null, colorInfo.defaultValue);

    // Check one with a name that is a reserved keyword.
    CssSchema.CssPropertyInfo floatInfo
        = cssSchema.getCssProperty(Name.css("float"));
    assertEquals("none", floatInfo.defaultValue);

    // Check two that share most of their definition via aliasing.
    CssSchema.CssPropertyInfo marginTopInfo
        = cssSchema.getCssProperty(Name.css("margin-top"));
    assertEquals("0", marginTopInfo.defaultValue);

    CssSchema.CssPropertyInfo marginBottomInfo
        = cssSchema.getCssProperty(Name.css("margin-bottom"));
    assertEquals("0", marginBottomInfo.defaultValue);
  }
}
