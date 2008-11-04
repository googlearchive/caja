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

import com.google.caja.util.Criterion;

/**
 * A token queue for javascript.
 *
 * @author mikesamuel@gmail.com
 */
public final class JsTokenQueue extends TokenQueue<JsTokenType> {

  public JsTokenQueue(TokenStream<JsTokenType> lexer, InputSource file) {
    this(lexer, file, NO_COMMENT);
  }

  public JsTokenQueue(
      TokenStream<JsTokenType> lexer, InputSource file,
      Criterion<Token<JsTokenType>> filter) {
    super(lexer, file, filter);
  }

  /**
   * A criterion that accepts all non-comment tokens.
   * @see #JsTokenQueue(TokenStream, InputSource, Criterion)
   */
  public static final Criterion<Token<JsTokenType>> NO_COMMENT =
    new Criterion<Token<JsTokenType>>() {
      public boolean accept(Token<JsTokenType> t) {
        return JsTokenType.COMMENT != t.type
            && JsTokenType.LINE_CONTINUATION != t.type;
      }
    };

  public boolean checkToken(Punctuation p) throws ParseException {
    return checkToken(p.toString());
  }

  public boolean checkToken(Keyword kw) throws ParseException {
    return checkToken(kw.toString());
  }

  public void expectToken(Punctuation p) throws ParseException {
    expectToken(p.toString());
  }

  public void expectToken(Keyword kw) throws ParseException {
    expectToken(kw.toString());
  }

  public boolean lookaheadToken(Punctuation p) throws ParseException {
    return lookaheadToken(p.toString());
  }

  public boolean lookaheadToken(Keyword kw) throws ParseException {
    return lookaheadToken(kw.toString());
  }
}
