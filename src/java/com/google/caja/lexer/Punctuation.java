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

import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessagePart;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * The strings that javascript treats as punctuation.
 * This includes all ES3 punctuation and some ES4 so that we can make sure our
 * output remains a subset of ES4.
 * @see <a href="http://www.ecmascript.org/es4/spec/grammar.pdf">ES4 Grammar</a>
 *
 * @author mikesamuel@gmail.com
 */
public enum Punctuation implements MessagePart {
  BANG("!"),
  BANG_EQ("!="),
  BANG_EQ_EQ("!=="),
  PCT("%"),
  PCT_EQ("%="),
  AMP("&"),
  AMP_AMP("&&"),
  AMP_AMP_EQ("&&="),
  AMP_EQ("&="),
  LPAREN("("),
  RPAREN(")"),
  AST("*"),
  AST_EQ("*="),
  PLUS("+"),
  PLUS_PLUS("++"),
  PLUS_EQ("+="),
  COMMA(","),
  MINUS("-"),
  MINUS_MINUS("--"),
  MINUS_EQ("-="),
  DOT("."),
  DOT_DOT(".."),
  ELIPSIS("..."),
  COLON(":"),
  COLON_COLON("::"),
  SEMI(";"),
  LT("<"),
  LT_LT("<<"),
  LT_LT_EQ("<<="),
  LT_EQ("<="),
  EQ("="),
  EQ_EQ("=="),
  EQ_EQ_EQ("==="),
  GT(">"),
  GT_EQ(">="),
  GT_GT(">>"),
  GT_GT_EQ(">>="),
  GT_GT_GT(">>>"),
  GT_GT_GT_EQ(">>>="),
  QMARK("?"),
  LSQUARE("["),
  RSQUARE("]"),
  CARET("^"),
  CARET_EQ("^="),
  LCURLY("{"),
  PIPE("|"),
  PIPE_EQ("|="),
  PIPE_PIPE("||"),
  PIPE_PIPE_EQ("||="),
  RCURLY("}"),
  TILDE("~"),
  SLASH("/"),
  SLASH_EQ("/="),
  ;

  private String s;

  Punctuation(String s) { this.s = s; }

  public void format(MessageContext mc, Appendable out) throws IOException {
    out.append(s);
  }

  @Override
  public String toString() { return s; }

  public static Punctuation fromString(String s) {
    return INSTANCE_MAP.get(s);
  }

  private static final Map<String, Punctuation> INSTANCE_MAP =
    new HashMap<String, Punctuation>();
  static {
    for (Punctuation p : Punctuation.values()) {
      INSTANCE_MAP.put(p.toString(), p);
    }
  }
}
