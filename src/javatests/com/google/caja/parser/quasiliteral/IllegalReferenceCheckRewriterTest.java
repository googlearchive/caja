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

package com.google.caja.parser.quasiliteral;

import com.google.caja.reporting.MessageLevel;

import static com.google.caja.parser.quasiliteral.QuasiBuilder.substV;

/**
 * @author ihab.awad@gmail.com
 */
public class IllegalReferenceCheckRewriterTest extends RewriterTestCase {
  public void testIllegalRefs() throws Exception {
    testIllegal("var x__;");
    testIllegal("function f__() { }");
    testIllegal("var x = function f__() { };");
    testIllegal("x__ = 3;");
  }

  public void testLegalRefs() throws Exception {
    testLegal("var x;");
    testLegal("function f() { }");
    testLegal("var x = function f() { };");
    testLegal("x = 3;");
  }

  public void testNullIdentifiers() throws Exception {
    testLegal("function () {}");
  }

  private void testIllegal(String code) throws Exception {
    checkAddsMessage(
        js(fromString(code)),
        RewriterMessageType.ILLEGAL_IDENTIFIER_LEFT_OVER,
        MessageLevel.FATAL_ERROR);
    // substV(...) produces synthetic nodes
    checkSucceeds(substV(code), null);
  }

  private void testLegal(String code) throws Exception {
    checkSucceeds(js(fromString(code)), null);
  }

  @Override
  protected Rewriter newRewriter() {
    return new IllegalReferenceCheckRewriter(true);
  }

  @Override
  protected Object executePlain(String program) {
    return new Object();  // Placeholder to allow assertConsistent() to work
  }

  @Override
  protected Object rewriteAndExecute(String pre, String program, String post) {
    return new Object();  // Placeholder to allow assertConsistent() to work
  }
}
