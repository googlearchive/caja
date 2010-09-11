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
 * Included future reserved keywords as per section 2.2 of
 * <a
 * href="http://wiki.ecmascript.org/lib/exe/fetch.php?id=resources%3Aresources&amp;cache=cache&amp;media=resources:jscriptdeviationsfromes3.pdf"
 * >JScript Deviations from ES3</a>
 * and section 1.8 of
 * <a href="http://www.ecmascript.org/es4/spec/incompatibilities.pdf"
 * >Compatibility between ES3 and Proposed ES4</a>.
 *
 * @author mikesamuel@gmail.com
 */
public enum Keyword implements MessagePart {
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
  FALSE("false"),           // officially a literal but not a keyword
  FINALLY("finally"),
  FOR("for"),
  FUNCTION("function"),
  IF("if"),
  IMPLEMENTS("implements"),
  IMPORT("import"),
  IN("in"),
  INSTANCEOF("instanceof"),
  INTERFACE("interface"),
  LET("let"),
  NEW("new"),
  NULL("null"),             // officially a literal but not a keyword
  PACKAGE("package"),
  PRIVATE("private"),
  PROTECTED("protected"),
  PUBLIC("public"),
  RETURN("return"),
  STATIC("static"),
  SUPER("super"),
  SWITCH("switch"),
  THIS("this"),
  THROW("throw"),
  TRUE("true"),             // officially a literal but not a keyword
  TRY("try"),
  TYPEOF("typeof"),
  VAR("var"),
  VOID("void"),
  WHILE("while"),
  WITH("with"),
  YIELD("yield"),
  ;

  private final String keywordText;

  Keyword(String keywordText) { this.keywordText = keywordText; }

  public void format(MessageContext mc, Appendable out) throws IOException {
    out.append(keywordText);
  }

  @Override
  public String toString() { return keywordText; }

  public static Keyword fromString(String keywordText) {
    return JAVASCRIPT_KEYWORDS.get(keywordText);
  }

  public static boolean isKeyword(String name) {
    return JAVASCRIPT_KEYWORDS.containsKey(name);
  }

  private static final Map<String, Keyword> JAVASCRIPT_KEYWORDS
      = new HashMap<String, Keyword>();
  static {
    for (Keyword k : Keyword.values()) {
      JAVASCRIPT_KEYWORDS.put(k.toString(), k);
    }
  }
}
