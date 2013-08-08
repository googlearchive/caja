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

package com.google.caja.render;

import junit.framework.TestCase;

@SuppressWarnings("static-method")
public class TokenClassificationTest extends TestCase {
  public final void testClassify() {
    assertEquals(TokenClassification.LINEBREAK,
                 TokenClassification.classify("\n"));
    assertEquals(TokenClassification.LINEBREAK,
                 TokenClassification.classify("\r\n"));
    assertEquals(TokenClassification.LINEBREAK,
                 TokenClassification.classify("\r"));
    assertEquals(TokenClassification.SPACE,
                 TokenClassification.classify(" "));
    assertEquals(TokenClassification.STRING,
                 TokenClassification.classify("'foo'"));
    assertEquals(TokenClassification.STRING,
                 TokenClassification.classify("\"bar\""));
    assertEquals(TokenClassification.REGEX,
                 TokenClassification.classify("/a/"));
    assertEquals(TokenClassification.REGEX,
                 TokenClassification.classify("/a/i"));
    assertEquals(TokenClassification.REGEX,
                 TokenClassification.classify("/[/]/"));
    assertEquals(TokenClassification.REGEX,
                 TokenClassification.classify("/[/]/g"));
    assertEquals(TokenClassification.COMMENT,
                 TokenClassification.classify("//"));
    assertEquals(TokenClassification.COMMENT,
                 TokenClassification.classify("// foo"));
    assertEquals(TokenClassification.COMMENT,
                 TokenClassification.classify("/**/"));
    assertEquals(TokenClassification.COMMENT,
                 TokenClassification.classify("/* foo */"));
    assertEquals(TokenClassification.COMMENT,
                 TokenClassification.classify("/* foo\n * bar\n */"));
    assertEquals(TokenClassification.COMMENT,
                 TokenClassification.classify("/** foo **/"));
    assertEquals(TokenClassification.PUNCTUATION,
                 TokenClassification.classify(","));
    assertEquals(TokenClassification.PUNCTUATION,
                 TokenClassification.classify("+="));
    assertEquals(TokenClassification.PUNCTUATION,
                 TokenClassification.classify("/="));
    assertEquals(TokenClassification.PUNCTUATION,
                 TokenClassification.classify("/"));
    assertEquals(TokenClassification.PUNCTUATION,
                 TokenClassification.classify("++"));
    assertEquals(TokenClassification.PUNCTUATION,
                 TokenClassification.classify("("));
    assertEquals(TokenClassification.PUNCTUATION,
                 TokenClassification.classify("."));
    assertEquals(TokenClassification.PUNCTUATION,
                 TokenClassification.classify("..."));
    assertEquals(TokenClassification.OTHER,
                 TokenClassification.classify("howdy"));
    assertEquals(TokenClassification.OTHER,
                 TokenClassification.classify("$"));
    assertEquals(TokenClassification.OTHER,
                 TokenClassification.classify("_"));
    assertEquals(TokenClassification.OTHER,
                 TokenClassification.classify("$foo"));
    assertEquals(TokenClassification.OTHER,
                 TokenClassification.classify("_foo"));
    assertEquals(TokenClassification.OTHER,
                 TokenClassification.classify("_1"));
    assertEquals(TokenClassification.OTHER,
                 TokenClassification.classify(".12"));
    assertEquals(TokenClassification.OTHER,
                 TokenClassification.classify("-3.12"));
    assertEquals(TokenClassification.OTHER,
                 TokenClassification.classify("-.5"));
    assertEquals(TokenClassification.OTHER,
                 TokenClassification.classify("\\u0061"));
    assertEquals(TokenClassification.OTHER,
                 TokenClassification.classify("\\u0061bcd"));
  }
}
