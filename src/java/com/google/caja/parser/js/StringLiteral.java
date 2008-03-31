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

package com.google.caja.parser.js;

import com.google.caja.parser.ParseTreeNode;
import com.google.caja.lexer.TokenConsumer;
import com.google.caja.lexer.escaping.Escaping;
import com.google.caja.reporting.RenderContext;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A parse tree node for a javascript string literal, e.g. <code>"foo"</code>.
 *
 * @author mikesamuel@gmail.com
 */
public final class StringLiteral extends Literal {
  private String value;

  public StringLiteral(String value, List<? extends ParseTreeNode> children) {
    this(value);
  }

  public StringLiteral(String value) {
    if (null == value) { throw new NullPointerException(); }
    this.value = value;
  }

  @Override
  public String getValue() {
    return value;
  }

  @Override
  public boolean getValueInBooleanContext() {
    return !"".equals(getUnquotedValue());
  }

  @Override
  public void render(RenderContext rc) {
    if (rc.isParanoid()) {
      TokenConsumer out = rc.getOut();
      out.mark(getFilePosition());
      StringBuilder sb = new StringBuilder();
      sb.append('\'');
      Escaping.escapeJsString(getUnquotedValue(), true, true, sb);
      sb.append('\'');
      out.consume(sb.toString());
    } else {
      super.render(rc);
    }
  }

  /**
   * If value is a quoted javascript string, represent the String value that
   * it represents.  Otherwise returns value.
   */
  public String getUnquotedValue() {
    return getUnquotedValueOf(this.value);
  }

  /**
   * If jsLiteral is a quoted javascript string, represent the String value that
   * it represents.  Otherwise returns the parameter unchanged.
   * <p>
   * This is useful for dealing with the keys in object constructors since they
   * may be string literals, numeric literals, or bare words.
   *
   * @param jsLiteral non null.
   */
  public static String getUnquotedValueOf(String jsLiteral) {
    int n = jsLiteral.length();
    if (n >= 2) {
      char ch0 = jsLiteral.charAt(0);
      if (('\'' == ch0 || '\"' == ch0) && jsLiteral.charAt(n - 1) == ch0) {
        return unescapeJsString(jsLiteral.substring(1, n - 1));
      }
    }
    return jsLiteral;
  }

  public static String toQuotedValue(CharSequence unquotedValue) {
    StringBuilder sb = new StringBuilder(unquotedValue.length() + 16);
    sb.append('\'');
    escapeJsString(unquotedValue, sb);
    sb.append('\'');
    return sb.toString();
  }

  // TODO(msamuel): move unescaping to Escaping.java -- nobody will look there
  private static final Pattern UNESCAPE_PATTERN = Pattern.compile(
      "\\\\(?:u([0-9A-Fa-f]{4})|([0-3][0-7]{0,2}|[4-7][0-7]?)|([^u0-7]))"
      );
  public static String unescapeJsString(CharSequence s) {
    Matcher m = UNESCAPE_PATTERN.matcher(s);
    if (!m.find()) { return s.toString(); }

    StringBuffer sb = new StringBuffer(s.length());
    do {
      String g;
      char repl;
      if (null != (g = m.group(1))) {  // unicode escape
        repl = (char) Integer.parseInt(g, 16);
      } else if (null != (g = m.group(2))) {  // octal escape
        repl = (char) Integer.parseInt(g, 8);
      } else {
        char ch = s.charAt(m.start(3));
        switch (ch) {
          case 'b': repl = '\b'; break;
          case 'r': repl = '\r'; break;
          case 'n': repl = '\n'; break;
          case 'f': repl = '\f'; break;
          case 't': repl = '\t'; break;
          case 'v': repl = '\u000b'; break;
          default: repl = ch; break;
        }
      }
      m.appendReplacement(sb, "");
      sb.append(repl);
    } while (m.find());
    m.appendTail(sb);
    return sb.toString();
  }

  /** Append the escaped version of s, without quotes, onto the given buffer. */
  public static void escapeJsString(CharSequence s, StringBuilder sb) {
    escapeJsString(s, '\'', sb);
  }

  /**
   * Append the escaped version of s, without quotes, onto the given buffer.
   * @param s the string to escape
   * @param delim a delimiter character to quote
   * @param sb the buffer to receive the output
   */
  public static void escapeJsString(
      CharSequence s, char delim, StringBuilder sb) {
    Escaping.escapeJsString(s, true, false, sb);
  }
}
