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
import java.util.Arrays;

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
                asciiOnly ? NO_NON_ASCII : ALLOW_NON_ASCII, out).escape();
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
        asciiOnly ? NO_NON_ASCII : ALLOW_NON_ASCII, out).escape();
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
    new Escaper(s, paranoid ? REGEX_PARANOID_ESCAPES : REGEX_MINIMAL_ESCAPES,
                asciiOnly ? NO_NON_ASCII : ALLOW_NON_ASCII, out).normalize();
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

  // Escape all characters that have a special meaning in a regular expression
  private static final EscapeMap REGEX_LITERAL_ESCAPES
      = REGEX_MINIMAL_ESCAPES.plus(
            simpleEscapes("()[]{}*+?.^$|\\".toCharArray()));

  // Escape all characters that have a special meaning in a regular expression
  private static final EscapeMap REGEX_LITERAL_PARANOID_ESCAPES
      = REGEX_PARANOID_ESCAPES.plus(
            simpleEscapes("()[]{}*+?.^$|\\".toCharArray()));

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

  /** Produces octal escapes for all characters in the given inclusive range. */
  private static Escape[] octalEscapes(char min, char max) {
    Escape[] out = new Escape[max - min + 1];
    for (int i = 0; i < out.length; ++i) {
      StringBuilder sb = new StringBuilder(4);
      char ch = (char) (min + i);
      try {
        Escaper.octalEscape(ch, sb);
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

  private Escaping() { /* non instantiable */ }
}
