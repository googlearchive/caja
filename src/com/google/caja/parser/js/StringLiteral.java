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
import com.google.caja.lexer.FilePosition;
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
  private static final long serialVersionUID = 6251403659182707540L;
  /** Undecoded value. */
  private final String value;

  /** @param children unused.  This ctor is provided for reflection. */
  @ReflectiveCtor
  public StringLiteral(
      FilePosition pos, String value, List<? extends ParseTreeNode> children) {
    this(pos, value);
  }

  public StringLiteral(FilePosition pos, String value) {
    super(pos);
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
    rc.getOut().mark(getFilePosition());
    renderUnquotedValue(getUnquotedValue(), rc);
  }

  public static void renderUnquotedValue(String unquoted, RenderContext rc) {
    StringBuilder sb = new StringBuilder(unquoted.length() + 18);

    // JSON requires double quotes for all string literals but in
    // embeddable mode we need to produce output that can be safely
    // embedded in HTML or XML.  We make no guarantees for attribute
    // values, and cajoled output should not be included in an
    // attribute value, but to be on the safe side, we use single
    // quotes since when naive HTML authors use quotes at all for
    // attribute values, they tend to use double quotes, and putting
    // single quotes around strings allows us to produce output that
    // contains no double quotes.
    // TODO(felix8a): avoid-double-quotes might not be worth it
    if (rc.asJson()) {
      sb.append('"');
      Escaping.escapeJsonString(unquoted, true, sb);
      sb.append('"');
    } else {
      sb.append('\'');
      Escaping.escapeJsString(unquoted, true, true, sb);
      sb.append('\'');
    }
    rc.getOut().consume(sb.toString());
  }

  /**
   * If value is a quoted javascript string, represent the String value that
   * it represents.  Otherwise returns value.
   */
  public String getUnquotedValue() {
    return getUnquotedValueOf(getValue());
  }

  /**
   * If jsLiteral is a quoted javascript string, returns the String value that
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
    Escaping.escapeJsString(unquotedValue, true, false, sb);
    sb.append('\'');
    return sb.toString();
  }

  public static StringLiteral valueOf(
      FilePosition pos, CharSequence unquotedValue) {
    return new StringLiteral(pos, toQuotedValue(unquotedValue));
  }

  // TODO(msamuel): move unescaping to Escaping.java -- nobody will look there
  private static final Pattern UNESCAPE_PATTERN = Pattern.compile(
      "\\\\(?:u([0-9A-Fa-f]{4})"
      + "|([0-3][0-7]{0,2}|[4-7][0-7]?)"
      + "|(?:x([0-9A-Fa-f]{2}))"
      + "|([^u0-7]))");
  /**
   * Replaces javascript escape sequences with the code units they specify.
   * @param s the raw content of a javascript string literal excluding any quote
   *   delimiters.
   * @return a series of code units.  Surrogates may not form valid pairs.
   */
  public static String unescapeJsString(CharSequence s) {
    Matcher m = UNESCAPE_PATTERN.matcher(s);
    if (!m.find()) { return s.toString(); }

    StringBuffer sb = new StringBuffer(s.length());
    do {
      m.appendReplacement(sb, "");
      String g;
      char repl;
      if (null != (g = m.group(1))) {  // unicode escape
        repl = (char) Integer.parseInt(g, 16);
      } else if (null != (g = m.group(2))) {  // octal escape
        repl = (char) Integer.parseInt(g, 8);
      } else if (null != (g = m.group(3))) {  // hex escape
        repl = (char) Integer.parseInt(g, 16);
      } else {
        char ch = s.charAt(m.start(4));
        switch (ch) {
          case 'b': repl = '\b'; break;
          case 'r': repl = '\r'; break;
          case 'n': repl = '\n'; break;
          case 'f': repl = '\f'; break;
          case 't': repl = '\t'; break;
          case 'v': repl = '\u000b'; break;
          case '\n': continue;      // backslash newline is elided
          default: repl = ch; break;
        }
      }
      sb.append(repl);
    } while (m.find());
    m.appendTail(sb);
    return sb.toString();
  }

  public String typeOf() { return "string"; }
}
