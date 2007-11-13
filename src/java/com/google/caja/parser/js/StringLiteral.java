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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A parse tree node for a javascript string literal, e.g. <code>"foo"</code>.
 *
 * @author mikesamuel@gmail.com
 */
public final class StringLiteral extends Literal {
  private String value;

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
    int end = s.length();
    for (int i = 0; i < end; ++i) {
      char ch = s.charAt(i);
      switch (ch) {
        case '\b': sb.append("\\b"); break;
        case '\r': sb.append("\\r"); break;
        case '\n': sb.append("\\n"); break;
        case '\f': sb.append("\\f"); break;
        case '\t': sb.append("\\t"); break;
        case '\u000b': sb.append("\\v"); break;
        case '\\': sb.append("\\\\"); break;
        case '\"': sb.append("\\\""); break;
        case '\'': sb.append("\\\'"); break;
        default:
          if (ch < 0x20 || ch == 0x7f) {
            octalEscape(ch, sb);
          } else if (ch >= 0x80) {
            unicodeEscape(ch, sb);
          } else {
            if (ch == delim) { sb.append('\\'); }
            sb.append(ch);
          }
          break;
      }
    }
  }

  static void octalEscape(char ch, StringBuilder sb) {
    sb.append('\\').append((char) ('0' + ((ch & 0x1c0) >> 6)))
        .append((char) ('0' + ((ch & 0x38) >> 3)))
        .append((char) ('0' + (ch & 0x7)));
  }

  static void unicodeEscape(char ch, StringBuilder sb) {
    sb.append("\\u").append("0123456789abcdef".charAt((ch >> 12) & 0xf))
        .append("0123456789abcdef".charAt((ch >> 8) & 0xf))
        .append("0123456789abcdef".charAt((ch >> 4) & 0xf))
        .append("0123456789abcdef".charAt(ch & 0xf));
  }
}
