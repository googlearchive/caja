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
 * An enumeration of all javascript keywords.
 *
 * Included future reserved keywords as per the
 * @see
 * <a href="http://www.mozilla.org/js/language/es4/formal/parser-grammar.html"
 * >EcmaScript Parser Grammar</a>
 *
 * @author mikesamuel@gmail.com
 */
public enum Keyword implements MessagePart {
  ABSTRACT("abstract"),
  AS("as"),
  BREAK("break"),
  CASE("case"),
  CATCH("catch"),
  CLASS("class"),
  CONST("const"),
  CONTINUE("continue"),
  DEBUGGER("debugger"),
  DEFAULT("default"),
  DELETE("delete"),
  DO("do"),
  ELSE("else"),
  ENUM("enum"),
  EXPORT("export"),
  EXTENDS("extends"),
  FALSE("false"),
  FINALLY("finally"),
  FOR("for"),
  FUNCTION("function"),
  GOTO("goto"),
  IF("if"),
  IMPLEMENTS("implements"),
  IMPORT("import"),
  IN("in"),
  INSTANCEOF("instanceof"),
  INTERFACE("interface"),
  IS("is"),
  NAMESPACE("namespace"),
  NATIVE("native"),
  NEW("new"),
  NULL("null"),
  PACKAGE("package"),
  PRIVATE("private"),
  PROTECTED("protected"),
  PUBLIC("public"),
  RETURN("return"),
  SUPER("super"),
  SWITCH("switch"),
  SYNCHRONIZED("synchronized"),
  THIS("this"),
  THROW("throw"),
  THROWS("throws"),
  TRANSIENT("transient"),
  TRUE("true"),
  TRY("try"),
  TYPEOF("typeof"),
  USE("use"),
  VAR("var"),
  VOID("void"),
  VOLATILE("volatile"),
  WHILE("while"),
  WITH("with"),
  ;

  private String keywordText;

  Keyword(String keywordText) { this.keywordText = keywordText; }

  public void format(MessageContext mc, Appendable out) throws IOException {
    out.append(keywordText);
  }

  @Override
  public String toString() { return keywordText; }

  public static Keyword fromString(String keywordText) {
    return JAVASCRIPT_KEYWORDS.get(keywordText);
  }

  private static final Map<String, Keyword> JAVASCRIPT_KEYWORDS
      = new HashMap<String, Keyword>();
  static {
    for (Keyword k : Keyword.values()) {
      JAVASCRIPT_KEYWORDS.put(k.toString(), k);
    }
  }
}
