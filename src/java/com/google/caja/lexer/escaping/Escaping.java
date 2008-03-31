// Copyright (C) 2007 Google Inc.
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

package com.google.caja.lexer.escaping;

import com.google.caja.util.SparseBitSet;
import java.io.IOException;

/**
 * Escaping of strings and regular expressions.
 *
 * @author mikesamuel@gmail.com (Mike Samuel)
 */
public class Escaping {

  /**
   * Given a plain text string writes an unquoted javascript string literal.
   *
   * @param s the plain text string to escape.
   * @param asciiOnly Makes sure that only ASCII characters are written to out.
   *     This is a good idea if you don't have control over the charset that
   *     the javascript will be served with.
   * @param paranoid True to make sure that nothing is written to out that could
   *     interfere with embedding inside a script tag or CDATA section, or
   *     other tag that typically contains markup.
   *     This does not make it safe to embed in an HTML attribute without
   *     further escaping.
   * @param out written to.
   */
  public static void escapeJsString(
      CharSequence s, boolean asciiOnly, boolean paranoid, Appendable out)
      throws IOException {
    new Escaper(s, paranoid ? STRING_PARANOID_ESCAPES : STRING_MINIMAL_ESCAPES,
                asciiOnly ? NO_NON_ASCII : ALLOW_NON_ASCII, JS_ENCODER, out)
        .escape();
  }

  /** @see #escapeJsString(CharSequence, boolean, boolean, Appendable) */
  public static void escapeJsString(
      CharSequence s, boolean asciiOnly, boolean paranoid, StringBuilder out) {
    try {
      escapeJsString(s, asciiOnly, paranoid, (Appendable) out);
    } catch (IOException ex) {
      // StringBuilders don't throw IOException
      throw new RuntimeException(ex);
    }
  }

  /**
   * Given a plain text string, write to out unquoted regular expression text
   * that would match that substring and only that substring.
   *
   * @param s the plain text string to escape.
   * @param asciiOnly Makes sure that only ASCII characters are written to out.
   *     This is a good idea if you don't have control over the charset that
   *     the javascript will be served with.
   * @param paranoid True to make sure that nothing is written to out that could
   *     interfere with embedding inside a script tag or CDATA section, or
   *     other tag that typically contains markup.
   *     This does not make it safe to embed in an HTML attribute without
   *     further escaping.
   * @param out written to.
   */
  public static void escapeRegex(
      CharSequence s, boolean asciiOnly, boolean paranoid, Appendable out)
      throws IOException {
    new Escaper(
        s, paranoid ? REGEX_LITERAL_PARANOID_ESCAPES : REGEX_LITERAL_ESCAPES,
        asciiOnly ? NO_NON_ASCII : ALLOW_NON_ASCII, JS_ENCODER, out)
        .escape();
  }

  /** @see #escapeRegex(CharSequence, boolean, boolean, Appendable) */
  public static void escapeRegex(
      CharSequence s, boolean asciiOnly, boolean paranoid, StringBuilder out) {
    try {
      escapeRegex(s, asciiOnly, paranoid, (Appendable) out);
    } catch (IOException ex) {
      // StringBuilders don't throw IOException
      throw new RuntimeException(ex);
    }
  }

  /**
   * Given a regular expression pattern, write a version to out that has the
   * same meaning, but with enough characters escaped to satisfy the conditions
   * imposed by the flags passed to this method.
   *
   * @param s the plain text string to escape.
   * @param asciiOnly Makes sure that only ASCII characters are written to out.
   *     This is a good idea if you don't have control over the charset that
   *     the javascript will be served with.
   * @param paranoid True to make sure that nothing is written to out that could
   *     interfere with embedding inside a script tag or CDATA section, or
   *     other tag that typically contains markup.
   *     This does not make it safe to embed in an HTML attribute without
   *     further escaping.
   * @param out written to.
   */
  public static void normalizeRegex(
      CharSequence s, boolean asciiOnly, boolean paranoid, Appendable out)
      throws IOException {
    new Escaper(requireEndUnescaped(rebalance(s, '[', ']')),
                paranoid ? REGEX_PARANOID_ESCAPES : REGEX_MINIMAL_ESCAPES,
                asciiOnly ? NO_NON_ASCII : ALLOW_NON_ASCII, JS_ENCODER, out)
        .normalize();
  }

  /** @see #normalizeRegex(CharSequence, boolean, boolean, Appendable) */
  public static void normalizeRegex(
      CharSequence s, boolean asciiOnly, boolean paranoid, StringBuilder out) {
    try {
      normalizeRegex(s, asciiOnly, paranoid, (Appendable) out);
    } catch (IOException ex) {
      // StringBuilders don't throw IOException
      throw new RuntimeException(ex);
    }
  }

  /**
   * Given plain text, output html/XML with the same meaning.
   *
   * @param s the plain text string to escape.
   * @param asciiOnly Makes sure that only ASCII characters are written to out.
   *     This is a good idea if you don't have control over the charset that
   *     the javascript will be served with.
   * @param out written to.
   */
  public static void escapeXml(
      CharSequence s, boolean asciiOnly, Appendable out)
      throws IOException {
    new Escaper(s, XML_ESCAPES, asciiOnly ? NO_NON_ASCII : PROBLEMATIC_XML,
                XML_ENCODER, out)
        .escape();
  }

  /** @see #escapeXml(CharSequence, boolean, Appendable) */
  public static void escapeXml(
      CharSequence s, boolean asciiOnly, StringBuilder out) {
    try {
      escapeXml(s, asciiOnly, (Appendable) out);
    } catch (IOException ex) {
      // StringBuilders don't throw IOException
      throw new RuntimeException(ex);
    }
  }

  /**
   * Escape the body of a CSS string.
   *
   * @param s the plain text string to escape.
   * @param paranoid True to make sure that nothing is written to out that could
   *     interfere with embedding inside a script tag or CDATA section, or
   *     other tag that typically contains markup.
   *     This does not make it safe to embed in an HTML attribute without
   *     further escaping.
   * @param out written to.
   */
  public static void escapeCssString(
      CharSequence s, boolean paranoid, Appendable out)
      throws IOException {
    new Escaper(s, EMPTY_ESCAPES,
                paranoid ? CSS_PARANOID_STR_ESCAPES : CSS_STR_ESCAPES,
                CSS_ENCODER, out).escape();
  }

  /** @see #escapeCssString(CharSequence, boolean, Appendable) */
  public static void escapeCssString(
      CharSequence s, boolean paranoid, StringBuilder out) {
    try {
      escapeCssString(s, paranoid, (Appendable) out);
    } catch (IOException ex) {
      // StringBuilders don't throw IOException
      throw new RuntimeException(ex);
    }
  }

  /**
   * Escape non-identifier characters in a CSS identifier.
   *
   * @param s the plain text string to escape.
   * @param out written to.
   */
  public static void escapeCssIdent(CharSequence s, Appendable out)
      throws IOException {
    if (s.length() == 0) { return; }
    char ch0 = s.charAt(0);
    if (ch0 >= '0' && ch0 <= '9') {
      // [0-9] is valid in identifier but not at start
      CSS_ENCODER.encode(
          ch0, s.length() > 1 ? Character.codePointAt(s, 1) : -1, out);
      s = s.subSequence(1, s.length());
    }
    new Escaper(s, EMPTY_ESCAPES, CSS_IDENT_ESCAPES, CSS_ENCODER, out).escape();
  }

  /** @see #escapeCssIdent(CharSequence, Appendable) */
  public static void escapeCssIdent(CharSequence s, StringBuilder out) {
    try {
      escapeCssIdent(s, (Appendable) out);
    } catch (IOException ex) {
      // StringBuilders don't throw IOException
      throw new RuntimeException(ex);
    }
  }

  // Escape only the characters in string that must be escaped.
  private static final EscapeMap STRING_MINIMAL_ESCAPES = new EscapeMap(
      new Escape('\0', "\\000"),
      new Escape('\b', "\\b"),
      new Escape('\r', "\\r"),
      new Escape('\n', "\\n"),
      new Escape('\\', "\\\\"),
      new Escape('\'', "\\'"),
      new Escape('\"', "\\\"")
      );
  // Escape enough characters in a string to make sure it can be safely embedded
  // in the body of an XML and HTML document.
  private static final EscapeMap STRING_PARANOID_ESCAPES = new EscapeMap(
      new Escape('\b', "\\b"),
      new Escape('\t', "\\t"),
      new Escape('\n', "\\n"),
      new Escape('\13', "\\v"),
      new Escape('\f', "\\f"),
      new Escape('\r', "\\r"),
      new Escape('\\', "\\\\"),
      new Escape('\'', "\\'"),
      new Escape('\"', "\\\""),
      new Escape('<', "\\074"),
      new Escape('>', "\\076")
      ).plus(octalEscapes('\0', '\u001f'));
  // Escape minimal characters in a regular expression that guarantee it will
  // parse properly, without escaping regular expression specials.
  private static final EscapeMap REGEX_MINIMAL_ESCAPES = new EscapeMap(
      new Escape('\0', "\\000"),
            new Escape('\b', "\\b"),
      new Escape('\r', "\\r"),
      new Escape('\n', "\\n"),
      new Escape('/', "\\/"));
  // Escape enough characters in a string to make sure it can be safely embedded
  // in XML and HTML without changing the meaning of regular expression
  // specials.
  private static final EscapeMap REGEX_PARANOID_ESCAPES = new EscapeMap(
      new Escape('\b', "\\b"),
      new Escape('\t', "\\t"),
      new Escape('\n', "\\n"),
      new Escape('\13', "\\v"),
      new Escape('\f', "\\f"),
      new Escape('\r', "\\r"),
      new Escape('/', "\\/"),
      new Escape('<', "\\074"),
      new Escape('>', "\\076")
      ).plus(octalEscapes('\0', '\u001f'));

  // Used when there are no non-precomputed escapes.
  private static final EscapeMap EMPTY_ESCAPES = new EscapeMap();

  // Escape all characters that have a special meaning in a regular expression
  private static final EscapeMap REGEX_LITERAL_ESCAPES
      = REGEX_MINIMAL_ESCAPES.plus(
            simpleEscapes("()[]{}*+?.^$|\\".toCharArray()));

  // Escape all characters that have a special meaning in a regular expression
  private static final EscapeMap REGEX_LITERAL_PARANOID_ESCAPES
      = REGEX_PARANOID_ESCAPES.plus(
            simpleEscapes("()[]{}*+?.^$|\\".toCharArray()));

  // Escapes for XML special characters.
  // From http://www.w3.org/TR/REC-xml/#charsets:
  // Char   ::=   #x9 | #xA | #xD | [#x20-#xD7FF] | [#xE000-#xFFFD]
  //          |   [#x10000-#x10FFFF]
  private static final EscapeMap XML_ESCAPES = new EscapeMap(
      new Escape('\u0000', "&#0;"),
      new Escape('\u0001', "&#1;"),
      new Escape('\u0002', "&#2;"),
      new Escape('\u0003', "&#3;"),
      new Escape('\u0004', "&#4;"),
      new Escape('\u0005', "&#5;"),
      new Escape('\u0006', "&#6;"),
      new Escape('\u0007', "&#7;"),
      new Escape('\u0008', "&#8;"),
      new Escape('\u000B', "&#xB;"),
      new Escape('\u000C', "&#xC;"),
      new Escape('&', "&amp;"),
      new Escape('<', "&lt;"),
      new Escape('>', "&gt;"),
      new Escape('"', "&quot;"),
      new Escape('\'', "&#" + ((int) '\'') + ";"),
      new Escape('`', "&#" + ((int) '`') + ";")
      );

  private static final SparseBitSet CSS_IDENT_ESCAPES = SparseBitSet.withRanges(
     new int[] {
       0, 0x2D,  // 0x2D is '-'
       0x2E, 0x30,  // 0x3A-0x40 are digits
       0x3A, 0x41,  // 0x41-0x5A are uppercase letters
       0x5B, 0x5f,  // 0x5F is '_'
       0x60, 0x61,  // 0x61-0x7A are lowercase letters
       0x7B, Character.MAX_CODE_POINT + 1,
     });

  private static final SparseBitSet CSS_STR_ESCAPES = SparseBitSet.withRanges(
     new int[] {
       0, 0x09,  // control characters
       0x0A, 0x20,
       0x22, 0x23,  // double quotes
       0x27, 0x28,  // single quotes
       0x5C, 0x5D,  // back slash
       0x7F, Character.MAX_CODE_POINT + 1,  // Exclude non-ascii codepoints
     });

  private static final SparseBitSet CSS_PARANOID_STR_ESCAPES
       = SparseBitSet.withRanges(new int[] {
       0, 0x20,  // control characters
       0x22, 0x23,  // double quotes
       0x27, 0x28,  // single quotes
       // Escape asterisks to make sure that IE 5's nested comment lexer
       // can't be confused by string literals, so no:
       //  /* /* */ content: '  */ expression(...) /* ' /* */
       0x2A, 0x2B,  // asterisk
       0x3C, 0x3D,  // <
       0x3E, 0x3F,  // >
       0x5C, 0x5D,  // back slash
       0x7F, Character.MAX_CODE_POINT + 1,  // Exclude non-ascii codepoints
     });

  // TODO(mikesamuel): we can't use UnicodeSet [:Cf:] since IE 6 and other
  // older browsers disagree on what format control characters are.
  // We need to come up with a character set empirically.
  private static final SparseBitSet ALLOW_NON_ASCII = SparseBitSet.withRanges(
      new int[] { 0xad, 0xae, 0x600, 0x604, 0x70f, 0x710,
                  0x17b4, 0x17b6, 0x200c, 0x2010, 0x2028, 0x202f,
                  0x2060, 0x2070, 0xfeff, 0xff00, 0xfff9, 0xfffc,
                  0x1d173, 0x1d17b, 0xe0001, 0xe0002, 0xe0020, 0xe0080 });

  private static final SparseBitSet NO_NON_ASCII = SparseBitSet.withRanges(
      new int[] { 0x7f, Character.MAX_CODE_POINT + 1 });

  // From http://www.w3.org/TR/REC-xml/#charsets:
  // Char   ::=   #x9 | #xA | #xD | [#x20-#xD7FF] | [#xE000-#xFFFD]
  //          |   [#x10000-#x10FFFF]
  // The characters defined in the following ranges are also discouraged. They
  // are either control characters or permanently undefined Unicode characters:
  // [#x7F-#x84], [#x86-#x9F], [#xFDD0-#xFDDF],
  // [#x1FFFE-#x1FFFF], [#x2FFFE-#x2FFFF], [#x3FFFE-#x3FFFF],
  // [#x4FFFE-#x4FFFF], [#x5FFFE-#x5FFFF], [#x6FFFE-#x6FFFF],
  // [#x7FFFE-#x7FFFF], [#x8FFFE-#x8FFFF], [#x9FFFE-#x9FFFF],
  // [#xAFFFE-#xAFFFF], [#xBFFFE-#xBFFFF], [#xCFFFE-#xCFFFF],
  // [#xDFFFE-#xDFFFF], [#xEFFFE-#xEFFFF], [#xFFFFE-#xFFFFF],
  // [#x10FFFE-#x10FFFF].
  private static final SparseBitSet PROBLEMATIC_XML = SparseBitSet.withRanges(
      new int[] { 0x7f, 0x85,
                  0x86, 0xA0,
                  0xD800, 0xE000,
                  0xFDD0, 0xFDE0,
                  0xFFFE, 0xFFFF,
                  0x1FFFE, 0x20000,
                  0x2FFFE, 0x30000,
                  0x3FFFE, 0x40000,
                  0x4FFFE, 0x50000,
                  0x5FFFE, 0x60000,
                  0x6FFFE, 0x70000,
                  0x7FFFE, 0x80000,
                  0x8FFFE, 0x90000,
                  0x9FFFE, 0xA0000,
                  0xAFFFE, 0xB0000,
                  0xBFFFE, 0xC0000,
                  0xCFFFE, 0xD0000,
                  0xDFFFE, 0xE0000,
                  0xEFFFE, 0xF0000,
                  0xFFFFE, 0x100000,
                  0x10FFFE, 0x110000 });

  static final Encoder JS_ENCODER = new Encoder() {
      public void encode(int codepoint, int nextCodepoint, Appendable out)
          throws IOException {
        if (!Character.isSupplementaryCodePoint(codepoint)) {
          if (codepoint < 0x100) {
            octalEscape((char) codepoint, out);
          } else {
            unicodeEscape((char) codepoint, out);
          }
        } else {
          for (char surrogate : Character.toChars(codepoint)) {
            unicodeEscape(surrogate, out);
          }
        }
      }
    };

  static final Encoder XML_ENCODER = new Encoder() {
      public void encode(int codepoint, int nextCodepoint, Appendable out)
          throws IOException {
        out.append("&#").append(Integer.toString(codepoint)).append(";");
      }
    };

  static final Encoder CSS_ENCODER = new Encoder() {
      public void encode(int codepoint, int nextCodepoint, Appendable out)
          throws IOException {
        if (!Character.isSupplementaryCodePoint(codepoint)) {
          hexEscape((char) codepoint, out);
        } else {
          for (char surrogate : Character.toChars(codepoint)) {
            hexEscape(surrogate, out);
          }
        }
        // We need a space if the character following is a hex digit or
        // a space character since the CSS {unicode} production specifies that
        // any following space character is part of the escape.
        // From http://www.w3.org/TR/CSS21/syndata.html#tokenization
        // unicode        \\[0-9a-f]{1,6}(\r\n|[ \n\r\t\f])?
        // S              [ \t\r\n\f]+
        // We add a space if the next codepoint is -1 so that if the escape
        // function is called twice and the second string starts with a
        // character in this range, there is a space.
        if ((nextCodepoint >= '0' && nextCodepoint <= '9')
            || (nextCodepoint >= 'a' && nextCodepoint <= 'f')
            || (nextCodepoint >= 'A' && nextCodepoint <= 'F')
            || nextCodepoint == '\t' || nextCodepoint == '\n'
            || nextCodepoint == '\f' || nextCodepoint == '\r'
            || nextCodepoint == ' ' || nextCodepoint == -1) {
          out.append(' ');
        }
      }
    };

  static void octalEscape(char ch, Appendable out) throws IOException {
    out.append('\\').append((char) ('0' + ((ch & 0x1c0) >> 6)))
        .append((char) ('0' + ((ch & 0x38) >> 3)))
        .append((char) ('0' + (ch & 0x7)));
  }

  static void unicodeEscape(char ch, Appendable out) throws IOException {
    out.append("\\u").append("0123456789abcdef".charAt((ch >> 12) & 0xf))
        .append("0123456789abcdef".charAt((ch >> 8) & 0xf))
        .append("0123456789abcdef".charAt((ch >> 4) & 0xf))
        .append("0123456789abcdef".charAt(ch & 0xf));
  }

  static void hexEscape(char ch, Appendable out) throws IOException {
    out.append("\\");
    if (ch < 0x100) {
      if (ch >= 0x10) {
        out.append("0123456789ABCDEF".charAt((ch >> 4) & 0xf));
      }
      out.append("0123456789ABCDEF".charAt(ch & 0xf));
    } else {
      if (ch >= 0x1000) {
        out.append("0123456789ABCDEF".charAt((ch >> 12) & 0xf));
      }
      out.append("0123456789ABCDEF".charAt((ch >> 8) & 0xf))
          .append("0123456789ABCDEF".charAt((ch >> 4) & 0xf))
          .append("0123456789ABCDEF".charAt(ch & 0xf));
    }
  }

  /** Produces octal escapes for all characters in the given inclusive range. */
  private static Escape[] octalEscapes(char min, char max) {
    Escape[] out = new Escape[max - min + 1];
    for (int i = 0; i < out.length; ++i) {
      StringBuilder sb = new StringBuilder(4);
      char ch = (char) (min + i);
      try {
        octalEscape(ch, sb);
      } catch (IOException ex) {
        // StringBuilders do not throw IOException
        throw new RuntimeException(ex);
      }
      out[i] = new Escape(ch, sb.toString());
    }
    return out;
  }

  /**
   * For each character, produces an escape that simply prefixes that character
   * with a backslash, so "*" => "\\*"
   */
  private static Escape[] simpleEscapes(char[] chars) {
    Escape[] out = new Escape[chars.length];
    for (int i = 0; i < out.length; ++i) {
      out[i] = new Escape(chars[i], "\\" + chars[i]);
    }
    return out;
  }

  /**
   * Make sure brackets are are balanced, because otherwise malformed regular
   * expressions can cause bad tokenization.
   */
  private static CharSequence rebalance(CharSequence s, char open, char close) {
    int n = s.length();
    if (n == 0) { return s; }

    StringBuilder sb = null;
    int pos = 0;  // position past last character in s written to sb
    int lOpen = -1;  // index of unclosed open element in s or -1 if none
    int lOpenInSb = -1;  // index of unclosed open element in sb or -1 if none

    if (s.charAt(0) == '*' || s.charAt(0) == '/') {
      // Escape any regex string starting with * to avoid /*rest of regex/.
      sb = new StringBuilder();
      sb.append('\\');
    }

    for (int i = 0; i < n; ++i) {
      char ch = s.charAt(i);
      if (ch == '\\') {
        ++i;
      } else if (ch == open) {
        if (lOpen == -1) {
          lOpen = i;
          lOpenInSb = -1;
        } else {
          if (sb == null) { sb = new StringBuilder(); }
          if (lOpenInSb == -1) {
            lOpenInSb = lOpen + sb.length() - pos;
          }
          sb.append(s, pos, i).append('\\');
          pos = i;
        }
      } else if (ch == close) {
        lOpen = -1;
      }
    }
    if (lOpen != -1) {
      if (sb == null) { sb = new StringBuilder(); }
      if (lOpenInSb != -1) {
        sb.insert(lOpenInSb, '\\');
      } else {
        sb.append(s, pos, lOpen);
        sb.append('\\');
        pos = lOpen;
      }
    } else if (sb == null) {
      return s;
    }
    sb.append(s, pos, n);
    return sb.toString();
  }

  /**
   * Make sure that s does not end with a backslash that would escape any
   * delimiter placed after it.
   */
  private static CharSequence requireEndUnescaped(CharSequence s) {
    int n = s.length();
    int nBackslashes = 0;
    while (nBackslashes < n && '\\' == s.charAt(n - nBackslashes - 1)) {
      ++nBackslashes;
    }
    if ((nBackslashes & 1) == 1) {
      return s + "\\";
    }
    return s;
  }

  private Escaping() { /* non instantiable */ }
}
