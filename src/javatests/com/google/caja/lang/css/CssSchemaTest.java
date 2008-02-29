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

import com.google.caja.reporting.EchoingMessageQueue;
import com.google.caja.reporting.MessageContext;

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

  public void testGetCssProperty() {
    assertNull(cssSchema.getCssProperty("bogus"));
    assertNotNull(cssSchema.getCssProperty("font-style"));

    // TODO(mikesamuel): test signature parse trees
  }

  // TODO(mikesamuel): test getSymbol

  public void testIsKeyword() {
    assertTrue(cssSchema.isKeyword("inherit"));
    assertTrue(cssSchema.isKeyword("default"));
    assertTrue(cssSchema.isKeyword("initial"));
    assertTrue(cssSchema.isKeyword("auto"));
    assertTrue(cssSchema.isKeyword("sans-serif"));
    assertTrue(cssSchema.isKeyword("monospace"));
    assertTrue(cssSchema.isKeyword("INHERIT"));
    assertFalse(cssSchema.isKeyword("not-a-keyword"));
    assertFalse(cssSchema.isKeyword("notakeyword"));
  }

  public void testIsPropertyAllowed() {
    assertTrue(cssSchema.isPropertyAllowed("color"));
    assertFalse(cssSchema.isPropertyAllowed("content"));
  }

  public void testIsFunctionAllowed() {
    assertTrue(cssSchema.isFunctionAllowed("rgb"));
    assertFalse(cssSchema.isFunctionAllowed("expression"));
  }
}
