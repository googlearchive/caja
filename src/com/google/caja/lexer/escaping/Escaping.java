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

import com.google.caja.SomethingWidgyHappenedError;
import com.google.caja.util.SparseBitSet;
import com.google.common.collect.Lists;

import java.io.IOException;
import java.util.List;

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
   * @param embeddable True to make sure that nothing is written to out that
   *     could interfere with embedding inside a script tag or CDATA section, or
   *     other tag that typically contains markup.
   *     This does not make it safe to embed in an HTML attribute without
   *     further escaping.
   * @param out written to.
   */
  public static void escapeJsString(
      CharSequence s, boolean asciiOnly, boolean embeddable, Appendable out)
      throws IOException {
    new Escaper(
        s, embeddable ? STRING_EMBEDDABLE_ESCAPES : STRING_MINIMAL_ESCAPES,
        asciiOnly ? NO_NON_ASCII : ALLOW_NON_ASCII, JS_ENCODER, out)
        .escape();
  }

  /** @see #escapeJsString(CharSequence, boolean, boolean, Appendable) */
  public static void escapeJsString(
      CharSequence s, boolean asciiOnly, boolean embeddable,
      StringBuilder out) {
    try {
      escapeJsString(s, asciiOnly, embeddable, (Appendable) out);
    } catch (IOException ex) {
      throw new SomethingWidgyHappenedError(
          "StringBuilders don't throw IOException", ex);
    }
  }

  /**
   * Given a plain text string writes an unquoted JSON string literal.
   *
   * @param s the plain text string to escape.
   * @param asciiOnly Makes sure that only ASCII characters are written to out.
   *     This is a good idea if you don't have control over the charset that
   *     the javascript will be served with.
   * @param out written to.
   */
  public static void escapeJsonString(
      CharSequence s, boolean asciiOnly, Appendable out)
      throws IOException {
    new Escaper(
        s, JSON_ESCAPES,
        asciiOnly ? NO_NON_ASCII : ALLOW_NON_ASCII, JS_ENCODER, out)
        .escape();
  }

  /** @see #escapeJsonString(CharSequence, boolean, Appendable) */
  public static void escapeJsonString(
      CharSequence s, boolean asciiOnly, StringBuilder out) {
    try {
      escapeJsonString(s, asciiOnly, (Appendable) out);
    } catch (IOException ex) {
      throw new SomethingWidgyHappenedError(
          "StringBuilders don't throw IOException", ex);
    }
  }

  /**
   * Given a normalized JS identifier writes a javascript identifier.
   *
   * @param s a string containing only letters, digits, and the characters
   *     '_' and '$'.
   * @param asciiOnly Makes sure that only ASCII characters are written to out.
   *     This is a good idea if you don't have control over the charset that
   *     the javascript will be served with.
   * @param out written to.
   */
  public static void escapeJsIdentifier(
      CharSequence s, boolean asciiOnly, Appendable out)
      throws IOException {
    new Escaper(s, STRING_MINIMAL_ESCAPES,
                asciiOnly ? NO_NON_ASCII : ALLOW_NON_ASCII, HEX4_ENCODER,
                out)
        .escape();
  }

  /** @see #escapeJsIdentifier(CharSequence, boolean, Appendable) */
  public static void escapeJsIdentifier(
      CharSequence s, boolean asciiOnly, StringBuilder out) {
    try {
      escapeJsIdentifier(s, asciiOnly, (Appendable) out);
    } catch (IOException ex) {
      throw new SomethingWidgyHappenedError(
          "StringBuilders don't throw IOException", ex);
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
   * @param embeddable True to make sure that nothing is written to out that
   *     could interfere with embedding inside a script tag or CDATA section, or
   *     other tag that typically contains markup.
   *     This does not make it safe to embed in an HTML attribute without
   *     further escaping.
   * @param out written to.
   */
  public static void escapeRegex(
      CharSequence s, boolean asciiOnly, boolean embeddable, Appendable out)
      throws IOException {
    new Escaper(
        s,
        embeddable ? REGEX_LITERAL_EMBEDDABLE_ESCAPES : REGEX_LITERAL_ESCAPES,
        asciiOnly ? NO_NON_ASCII : ALLOW_NON_ASCII, JS_ENCODER, out)
        .escape();
  }

  /** @see #escapeRegex(CharSequence, boolean, boolean, Appendable) */
  public static void escapeRegex(
      CharSequence s, boolean asciiOnly, boolean embeddable,
      StringBuilder out) {
    try {
      escapeRegex(s, asciiOnly, embeddable, (Appendable) out);
    } catch (IOException ex) {
      throw new SomethingWidgyHappenedError(
          "StringBuilders don't throw IOException", ex);
    }
  }

  /**
   * Given a regular expression pattern, write a version to out that has the
   * same meaning, but with enough characters escaped to satisfy the conditions
   * imposed by the flags passed to this method.
   *
   * @param s the plain text string to escape.
   * @param out written to.
   */
  public static void normalizeRegex(CharSequence s, Appendable out)
      throws IOException {
    new Escaper(requireEndUnescaped(rebalance(s, '[', ']')),
        REGEX_EMBEDDABLE_ESCAPES, NO_NON_ASCII, JS_ENCODER, out)
    .normalize();
  }

  /** @see #normalizeRegex(CharSequence, Appendable) */
  public static void normalizeRegex(CharSequence s, StringBuilder out) {
    try {
      normalizeRegex(s, (Appendable) out);
    } catch (IOException ex) {
      throw new SomethingWidgyHappenedError(ex);
    }
  }

  /**
   * @param asciiOnly unused.  Present for backwards compatibility.
   * @param embeddable unused.  Present for backwards compatibility.
   */
  @Deprecated
  public static void normalizeRegex(
      CharSequence s, boolean asciiOnly, boolean embeddable, Appendable out)
      throws IOException {
    normalizeRegex(s, out);
  }

  /**
   * @param asciiOnly unused.  Present for backwards compatibility.
   * @param embeddable unused.  Present for backwards compatibility.
   */
  @Deprecated
  public static void normalizeRegex(
      CharSequence s, boolean asciiOnly, boolean embeddable,
      StringBuilder out) {
    normalizeRegex(s, out);
  }

  /**
   * Given plain text, output HTML/XML with the same meaning.
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
      throw new SomethingWidgyHappenedError(
          "StringBuilders don't throw IOException", ex);
    }
  }

  /**
   * Escape the body of a CSS string.
   * The result is safe to embed in a style tag or CDATA section.
   *
   * @param s the plain text string to escape.
   * @param out written to.
   */
  public static void escapeCssString(CharSequence s, Appendable out)
      throws IOException {
    new Escaper(s, EMPTY_ESCAPES, CSS_STR_ESCAPES, CSS_ENCODER, out).escape();
  }

  /** @see #escapeCssString(CharSequence, Appendable) */
  public static void escapeCssString(CharSequence s, StringBuilder out) {
    try {
      escapeCssString(s, (Appendable) out);
    } catch (IOException ex) {
      throw new SomethingWidgyHappenedError(
          "StringBuilders don't throw IOException", ex);
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
      throw new SomethingWidgyHappenedError(
          "StringBuilders don't throw IOException", ex);
    }
  }

  /**
   * Encodes a string as a URI component per section 2.1 of RFC 3986.
   * To deal with a composed URI, see {@link UriUtil#normalizeUri(String)}.
   */
  public static void escapeUri(CharSequence s, StringBuilder out) {
    escapeUri(s, 0, out);
  }

  static void escapeUri(CharSequence s, int i, StringBuilder out) {
    try {
      escapeUri(s, i, (Appendable) out);
    } catch (IOException ex) {
      throw new SomethingWidgyHappenedError(
          "StringBuilders don't throw IOException", ex);
    }
  }

  /**
   * Encodes a string as a URI component per section 2.1 of RFC 3986.
   * To deal with a composed URI, see {@link UriUtil#normalizeUri(String)}.
   * @throws IOException if {@code out} throws during append.
   */
  public static void escapeUri(CharSequence s, Appendable out)
      throws IOException {
    escapeUri(s, 0, out);
  }

  private static void escapeUri(CharSequence s, int i, Appendable out)
      throws IOException {
    int pos = 0, n = s.length();
    for (; i < n; ++i) {
      char ch = s.charAt(i);
      if (ch >= 0x80 || Escaping.URI_ESCAPES.getEscape(ch) != null) {
        out.append(s, pos, i);
        pctEncode(ch, out);
        pos = i + 1;
      }
    }
    out.append(s, pos, n);
  }

  // Escape only the characters in string that must be escaped.
  private static final EscapeMap JSON_ESCAPES = new EscapeMap(
      new Escape('\0', "\\u0000"),
      new Escape('\b', "\\b"),
      new Escape('\r', "\\r"),
      new Escape('\f', "\\f"),
      new Escape('\n', "\\n"),
      new Escape('\t', "\\t"),
      new Escape('\\', "\\\\"),
      new Escape('\'', "\\u0027"),
      new Escape('\"', "\\\"")
      );
  private static final EscapeMap STRING_MINIMAL_ESCAPES = new EscapeMap(
      new Escape('\0', "\\x00"),
      new Escape('\b', "\\b"),
      new Escape('\r', "\\r"),
      new Escape('\n', "\\n"),
      new Escape('\\', "\\\\"),
      new Escape('\'', "\\'"),
      new Escape('\"', "\\\"")
      );
  // Escape enough characters in a string to make sure it can be safely embedded
  // in the body of an XML and HTML document.
  private static final EscapeMap STRING_EMBEDDABLE_ESCAPES = new EscapeMap(
      new Escape('\b', "\\b"),
      new Escape('\t', "\\t"),
      new Escape('\n', "\\n"),
      // JScript treats \v as the letter v
      new Escape('\f', "\\f"),
      new Escape('\r', "\\r"),
      new Escape('\\', "\\\\"),
      new Escape('\'', "\\'"),
      new Escape('\"', "\\\""),
      new Escape('&', "\\x26"),
      new Escape('<', "\\x3c"),
      new Escape('>', "\\x3e")
      ).plus(hex2Escapes('\0', '\u001f'));
  // Escape minimal characters in a regular expression that guarantee it will
  // parse properly, without escaping regular expression specials.
  private static final EscapeMap REGEX_MINIMAL_ESCAPES = new EscapeMap(
      new Escape('\0', "\\x00"),
      new Escape('\b', "\\b"),
      new Escape('\r', "\\r"),
      new Escape('\n', "\\n"),
      new Escape('/', "\\/"));
  // Escape enough characters in a string to make sure it can be safely embedded
  // in XML and HTML without changing the meaning of regular expression
  // specials.
  private static final EscapeMap REGEX_EMBEDDABLE_ESCAPES = new EscapeMap(
      new Escape('\b', "\\b"),
      new Escape('\t', "\\t"),
      new Escape('\n', "\\n"),
      // JScript treats \v as the letter v
      new Escape('\f', "\\f"),
      new Escape('\r', "\\r"),
      new Escape('&', "\\x26"),
      new Escape('/', "\\/"),
      new Escape('<', "\\x3c"),
      new Escape('>', "\\x3e")
      ).plus(hex2Escapes('\0', '\u001f'));

  // Used when there are no non-precomputed escapes.
  private static final EscapeMap EMPTY_ESCAPES = new EscapeMap();

  // Escape all characters that have a special meaning in a regular expression
  private static final EscapeMap REGEX_LITERAL_ESCAPES
      = REGEX_MINIMAL_ESCAPES.plus(
            simpleEscapes("()[]{}*+?.^$|\\".toCharArray()));

  // Escape all characters that have a special meaning in a regular expression
  private static final EscapeMap REGEX_LITERAL_EMBEDDABLE_ESCAPES
      = REGEX_EMBEDDABLE_ESCAPES.plus(
            simpleEscapes("()[]{}*+?.^$|\\".toCharArray()));

  /**
   * Escapes for XML special characters.
   * From http://www.w3.org/TR/REC-xml/#charsets:
   * <pre>
   * Char   ::=   #x9 | #xA | #xD | [#x20-#xD7FF] | [#xE000-#xFFFD]
   *          |   [#x10000-#x10FFFF]
   * </pre>
   */
  private static final EscapeMap XML_ESCAPES;
  static {
    List<Escape> escapes = Lists.newArrayList();
    for (int i = 0; i < 0x20; ++i) {
      switch (i) {
        // Only three control characters are allowed in XML text.
        case '\t': case '\n': case '\r': break;
        default: escapes.add(new Escape((char) i, "&#" + i + ";")); break;
      }
    }
    escapes.add(new Escape('&', "&amp;"));
    escapes.add(new Escape('<', "&lt;"));
    escapes.add(new Escape('>', "&gt;"));
    // &#34; is shorter than &quot;
    escapes.add(new Escape('"', "&#" + ((int) '\"') + ";"));
    // &#39; is shorter than &apos; and works in both HTML and XML.
    escapes.add(new Escape('\'', "&#" + ((int) '\'') + ";"));
    escapes.add(new Escape('`', "&#" + ((int) '`') + ";"));
    escapes.add(new Escape((char) 0x7f, "&#x7f;"));
    XML_ESCAPES = new EscapeMap(escapes.toArray(new Escape[escapes.size()]));
  }
  private static final SparseBitSet CSS_IDENT_ESCAPES = SparseBitSet.withRanges(
     new int[] {
       0, 0x2D,  // 0x2D is '-'
       0x2E, 0x30,  // 0x3A-0x40 are digits
       0x3A, 0x41,  // 0x41-0x5A are uppercase letters
       0x5B, 0x5f,  // 0x5F is '_'
       0x60, 0x61,  // 0x61-0x7A are lowercase letters
       0x7B, Character.MAX_CODE_POINT + 1,
     });

  // The below includes all CSS special characters.
  // When a CSS parser sees a token it does not recognize, it skips tokens
  // until it sees something that signals the start of a new construct, such as
  // a '}' or '@'.  If a parser becomes particularly confused, it could jump
  // into the middle of a string.  Escaping all CSS special characters prevents
  // code from being hidden in strings.
  private static final SparseBitSet CSS_STR_ESCAPES = SparseBitSet.withRanges(
     new int[] {
       0, 0x20,  // control characters
       0x22, 0x23,  // double quotes
       // Escape asterisks to make sure that IE 5's nested comment lexer
       // can't be confused by string literals, so no:
       //  /* /* */ content: '  */ expression(...) /* ' /* */
       0x27, 0x2D,  // single quotes, parentheses, asterisk, plus, comma
       // TODO(mikesamuel): Once IE allows escapes inside URLs, then
       // we should add '&', ':' and '=' to this list.
       // See issue 938 for details.
       0x3B, 0x3D, 0x3E, 0x3F,  // semicolon, angle brackets
       0x40, 0x41,  // @ symbol.
       0x5B, 0x5E,  // square brackets and back slash
       0x7B, 0x7E,  // curly brackets and pipe.
       0x7F, Character.MAX_CODE_POINT + 1,  // Exclude non-ascii codepoints
     });

  // We can't use UnicodeSet [:Cf:] since IE 6 and other older
  // browsers disagree on what characters are allowed in strings.
  // The below excludes [:Cf:] codepoints, unicode newlines, and
  // codepoints that empirically can't be embedded in a string.
  // See http://unicode.org/cldr/utility/list-unicodeset.jsp?a=%5B:Cf:%5D for
  // the set of [:Cf:] codepoints.
  private static final SparseBitSet ALLOW_NON_ASCII = SparseBitSet.withRanges(
      new int[] { 0xad, 0xae, 0x600, 0x604, 0x6dd, 0x6de, 0x70f, 0x710,
                  0x17b4, 0x17b6, 0x200b, 0x2010, 0x2028, 0x202f,
                  0x2060, 0x2070, 0xfdd0, 0xfdf0, 0xfeff, 0xff00,
                  0xfff0, 0xfffc, 0xfffe, 0x10000,
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

  /**
   * From RFC 3986:
   * <blockquote>
   * <h3>2.3.  Unreserved Characters</h3>
   *
   * Characters that are allowed in a URI but do not have a reserved
   * purpose are called unreserved.  These include uppercase and lowercase
   * letters, decimal digits, hyphen, period, underscore, and tilde.
   *
   * <blockquote><code>
   *    unreserved  = ALPHA / DIGIT / "-" / "." / "_" / "~"
   * </code></blockquote>
   *
   * URIs that differ in the replacement of an unreserved character with
   * its corresponding percent-encoded US-ASCII octet are equivalent: they
   * identify the same resource.  However, URI comparison implementations
   * do not always perform normalization prior to comparison (see Section
   * 6).  For consistency, percent-encoded octets in the ranges of ALPHA
   * (%41-%5A and %61-%7A), DIGIT (%30-%39), hyphen (%2D), period (%2E),
   * underscore (%5F), or tilde (%7E) should not be created by URI
   * producers and, when found in a URI, should be decoded to their
   * corresponding unreserved characters by URI normalizers.
   * </blockquote>
   */
  static final EscapeMap URI_ESCAPES;
  static {
    List<Escape> escapes = Lists.newArrayList();
    for (int i = 0; i < 0x80; ++i) {
      if (i == '-' || i == '.' || i == '_' || i == '~' || (i >= '0' && i <= '9')
          || (i >= 'A' && i <= 'Z') || (i >= 'a' && i <= 'z')) {
        continue;
      }
      StringBuilder sb = new StringBuilder(3);
      try {
        pctEncode((byte) i, sb);
      } catch (IOException ex) {
        throw new SomethingWidgyHappenedError(
            "StringBuilders shouldn't throw IOException", ex);
      }
      escapes.add(new Escape((char) i, sb.toString()));
    }
    URI_ESCAPES = new EscapeMap(escapes.toArray(new Escape[escapes.size()]));
  }

  static final Encoder JS_ENCODER = new Encoder() {
      public void encode(int codepoint, int nextCodepoint, Appendable out)
          throws IOException {
        if (!Character.isSupplementaryCodePoint(codepoint)) {
          if (codepoint < 0x100) {
            hex2Escape((char) codepoint, out);
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

  static final Encoder HEX4_ENCODER = new Encoder() {
      public void encode(int codepoint, int nextCodepoint, Appendable out)
          throws IOException {
        if (!Character.isSupplementaryCodePoint(codepoint)) {
          unicodeEscape((char) codepoint, out);
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

  static void hex2Escape(char ch, Appendable out) throws IOException {
    out.append("\\x").append("0123456789abcdef".charAt((ch >> 4) & 0xf))
        .append("0123456789abcdef".charAt(ch & 0xf));
  }

  public static void unicodeEscape(char ch, Appendable out) throws IOException {
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

  static void pctEncode(char ch, Appendable out) throws IOException {
    if (ch < 0x80) {
      pctEncode((byte) ch, out);
    } else {
      // UTF-8 encode
      if (ch < 0x800) {  // 2 byte form
        pctEncode((byte) (0xc0 | ((ch >>> 6) & 0x1f)), out);
      } else {  // 3 byte form
        pctEncode((byte) (0xe0 | ((ch >>> 12) & 0xf)), out);
        pctEncode((byte) (0x80 | ((ch >>> 6) & 0x3f)), out);
      }
      pctEncode((byte) (0x80 | (ch & 0x3f)), out);
    }
  }

  static void pctEncode(byte b, Appendable out) throws IOException {
    out.append('%')
        .append("0123456789abcdef".charAt((b >> 4) & 0xf))
        .append("0123456789abcdef".charAt(b & 0xf));
  }


  /** Produces hex escape for all characters in the given inclusive range. */
  private static Escape[] hex2Escapes(char min, char max) {
    Escape[] out = new Escape[max - min + 1];
    for (int i = 0; i < out.length; ++i) {
      StringBuilder sb = new StringBuilder(4);
      char ch = (char) (min + i);
      try {
        hex2Escape(ch, sb);
      } catch (IOException ex) {
        throw new SomethingWidgyHappenedError(
            "StringBuilders don't throw IOException", ex);
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
