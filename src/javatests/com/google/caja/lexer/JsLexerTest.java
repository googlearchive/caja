// Copyright (C) 2005 Google Inc.
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

import com.google.caja.util.TestUtil;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;

import junit.framework.TestCase;

/**
 * testcases for {@link JsLexer}.
 *
 * @author mikesamuel@gmail.com
 */
public class JsLexerTest extends TestCase {

  public void testLexer() throws Exception {
    InputSource input = new InputSource(
        TestUtil.getResource(getClass(), "lexertest1.js"));
    StringBuilder output = new StringBuilder();

    BufferedReader in = new BufferedReader(
        new InputStreamReader(TestUtil.getResourceAsStream(
                                  getClass(), "lexertest1.js")));
    JsLexer t = new JsLexer(in, input);
    try {
      while (t.hasNext()) {
        Token<JsTokenType> tok = t.next();
        output.append(tok.type.toString().substring(0, 4)
                      + " [" + tok.text + "]: " + tok.pos + "\n");
      }
    } finally {
      in.close();
    }

    String golden;
    BufferedReader goldenIn = new BufferedReader(
        new InputStreamReader(TestUtil.getResourceAsStream(
                                  getClass(), "lexergolden1.txt")));
    try {
      StringBuilder sb = new StringBuilder();
      char[] buf = new char[1024];
      for (int n; (n = goldenIn.read(buf)) > 0;) {
        sb.append(buf, 0, n);
      }
      golden = sb.toString();
    } finally {
      goldenIn.close();
    }

    assertEquals(golden, output.toString());
    //fail(golden + "\n\n  !=\n\n" + output);
  }

  public void testLexer2() throws Exception {
    InputSource input = new InputSource(
        TestUtil.getResource(getClass(), "lexertest2.js"));
    StringBuilder output = new StringBuilder();

    BufferedReader in = new BufferedReader(
        new InputStreamReader(TestUtil.getResourceAsStream(
                                  getClass(), "lexertest2.js")));
    JsLexer t = new JsLexer(in, input);
    try {
      while (t.hasNext()) {
        Token<JsTokenType> tok = t.next();
        output.append(tok.type.toString().substring(0, 4)
                      + " [" + tok.text + "]: " + tok.pos + "\n");
      }
    } catch (ParseException ex) {
      ex.printStackTrace();
    } finally {
      in.close();
    }

    String golden;
    BufferedReader goldenIn = new BufferedReader(
        new InputStreamReader(TestUtil.getResourceAsStream(
                                  getClass(), "lexergolden2.txt")));
    try {
      StringBuilder sb = new StringBuilder();
      char[] buf = new char[1024];
      for (int n; (n = goldenIn.read(buf)) > 0;) {
        sb.append(buf, 0, n);
      }
      golden = sb.toString();
    } finally {
      goldenIn.close();
    }

    assertEquals(golden, output.toString());
    //fail(golden + "\n\n  !=\n\n" + output);

  }

  public static void main(String[] args) throws Exception {
    InputSource input = new InputSource(URI.create("file:///proc/self/fd/0"));

    BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
    JsLexer t = new JsLexer(in, input);
    while (t.hasNext()) {
      Token<JsTokenType> tok = t.next();
      System.out.append(tok.type.toString().substring(0, 4)
        + " [" + tok.text + "]: " + tok.pos + "\n");
    }
  }
}
