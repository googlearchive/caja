// Copyright (C) 2010 Google Inc.
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

package com.google.caja.lexer;

import com.google.caja.util.CajaTestCase;
import com.google.caja.util.ContentType;

@SuppressWarnings("static-method")
public class GuessContentTypeTest extends CajaTestCase {
  public final void testGuess() {
    assertEquals(
        ContentType.JS,
        GuessContentType.guess(null, null, "foo()"));
    assertEquals(
        ContentType.CSS,
        GuessContentType.guess(null, null, "foo { color: red }"));
    assertEquals(
        ContentType.HTML,
        GuessContentType.guess(null, null, "<foo>"));
    assertEquals(
        ContentType.JS,
        GuessContentType.guess(null, null, "foo() ? bar : baz()"));
  }
}
