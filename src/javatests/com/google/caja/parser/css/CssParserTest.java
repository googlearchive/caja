// Copyright (C) 2006 Google Inc.
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

package com.google.caja.parser.css;

import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.CssLexer;
import com.google.caja.lexer.CssTokenType;
import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.Token;
import com.google.caja.lexer.TokenQueue;
import com.google.caja.reporting.MessageContext;
import com.google.caja.util.Criterion;
import com.google.caja.util.TestUtil;

import java.net.URI;

import junit.framework.TestCase;

/**
 *
 * @author mikesamuel@gmail.com
 */
public class CssParserTest extends TestCase {

  public void testUnescape() throws Exception {
    InputSource is = new InputSource(URI.create("test:///"));
    FilePosition pos = FilePosition.instance(is, 1, 1, 1, 1);
    assertEquals("", CssParser.unescape(
        Token.instance("", CssTokenType.IDENT, pos)));
    assertEquals("foo", CssParser.unescape(
        Token.instance("foo", CssTokenType.IDENT, pos)));
    assertEquals("foo", CssParser.unescape(
        Token.instance("f\\oo", CssTokenType.IDENT, pos)));
    assertEquals("!important", CssParser.unescape(
        Token.instance("! important", CssTokenType.IDENT, pos)));
    assertEquals("!important", CssParser.unescape(
        Token.instance("!   important", CssTokenType.IDENT, pos)));
    assertEquals("'foo bar'", CssParser.unescape(
        Token.instance("'foo bar'", CssTokenType.STRING, pos)));
    assertEquals("'foo bar'", CssParser.unescape(
        Token.instance("'foo\\ bar'", CssTokenType.STRING, pos)));
    assertEquals("'foo bar'", CssParser.unescape(
        Token.instance("'foo\\ b\\\nar'", CssTokenType.STRING, pos)));
    assertEquals("'foo bar'", CssParser.unescape(
        Token.instance("'foo\\ b\\\rar'", CssTokenType.STRING, pos)));
    assertEquals("'ffoo bar'", CssParser.unescape(
        Token.instance("'\\66 foo bar'", CssTokenType.STRING, pos)));
    assertEquals("foo-bar", CssParser.unescape(
        Token.instance("\\66oo-ba\\0072", CssTokenType.IDENT, pos)));
    assertEquals("\\66oo-bar", CssParser.unescape(
        Token.instance("\\\\66oo-ba\\0072", CssTokenType.IDENT, pos)));
  }

  public void testCssParser1() throws Exception {
    String golden = TestUtil.readResource(getClass(), "cssparsergolden1.txt");
    CssTree.StyleSheet stylesheet;
    CharProducer cp = TestUtil.getResourceAsProducer(
        getClass(), "cssparserinput1.css");
    try {
      CssLexer lexer = new CssLexer(cp);
      TokenQueue<CssTokenType> tq = new TokenQueue<CssTokenType>(
          lexer, cp.getCurrentPosition().source(),
          new Criterion<Token<CssTokenType>>() {
            public boolean accept(Token<CssTokenType> t) {
              return CssTokenType.SPACE != t.type
                  && CssTokenType.COMMENT != t.type;
            }
          });
      CssParser p = new CssParser(tq);
      stylesheet = p.parseStyleSheet();
      tq.expectEmpty();
    } finally {
      cp.close();
    }
    StringBuilder sb = new StringBuilder();
    stylesheet.format(new MessageContext(), sb);
    assertEquals(golden.trim(), sb.toString().trim());
  }

  public void testCssParser2() throws Exception {
    String golden = TestUtil.readResource(getClass(), "cssparsergolden2.txt");
    CssTree.StyleSheet stylesheet;
    CharProducer cp = TestUtil.getResourceAsProducer(
        getClass(), "cssparserinput2.css");
    try {
      CssLexer lexer = new CssLexer(cp);
      TokenQueue<CssTokenType> tq = new TokenQueue<CssTokenType>(
          lexer, cp.getCurrentPosition().source(),
          new Criterion<Token<CssTokenType>>() {
            public boolean accept(Token<CssTokenType> t) {
              return CssTokenType.SPACE != t.type
                  && CssTokenType.COMMENT != t.type;
            }
          });
      CssParser p = new CssParser(tq);
      stylesheet = p.parseStyleSheet();
      tq.expectEmpty();
    } finally {
      cp.close();
    }
    StringBuilder sb = new StringBuilder();
    stylesheet.format(new MessageContext(), sb);
    assertEquals(golden.trim(), sb.toString().trim());
  }
}
