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
import com.google.caja.lexer.Token;
import com.google.caja.lexer.TokenQueue;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.RenderContext;
import com.google.caja.util.Criterion;
import com.google.caja.util.TestUtil;

import junit.framework.TestCase;

/**
 *
 * @author mikesamuel@gmail.com
 */
public class CssTreeTest extends TestCase {

  public void testRender() throws Exception {
    String golden = TestUtil.readResource(getClass(), "cssrendergolden1.txt");
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
    RenderContext rc = new RenderContext(new MessageContext(), sb);
    stylesheet.render(rc);
    assertEquals(golden.trim(), sb.toString().trim());
  }

  // TODO(msamuel): test escaping of characters in identifiers, urls,  and
  // strings.  Test rendering of !important and combinators in selectors.
}
