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

package com.google.caja.parser;

import com.google.caja.lexer.JsTokenQueue;
import com.google.caja.lexer.JsTokenType;
import com.google.caja.lexer.Keyword;
import com.google.caja.lexer.ParseException;
import com.google.caja.lexer.Token;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.MessageType;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Some identifier, keyword, and number handling routines used by multiple
 * parsers.
 *
 * @author mikesamuel@gmail.com
 * @author ihab.awad@gmail.com
 */
public abstract class ParserBase {
  protected final JsTokenQueue tq;
  protected final MessageQueue mq;
  protected final boolean isQuasiliteral;

  public ParserBase(JsTokenQueue tq, MessageQueue mq) {
    this(tq, mq, false);
  }

  public ParserBase(JsTokenQueue tq, MessageQueue mq, boolean isQuasiliteral) {
    this.tq = tq;
    this.mq = mq;
    this.isQuasiliteral = isQuasiliteral;
  }

  public JsTokenQueue getTokenQueue() { return tq; }
  public MessageQueue getMessageQueue() { return mq; }

  public String parseIdentifier(boolean allowReservedWords)
      throws ParseException {
    Token<JsTokenType> t = tq.peek();
    String s = t.text;
    switch (t.type) {
      case WORD:
        if (!allowReservedWords) {
          Keyword k = Keyword.fromString(decodeIdentifier(s));
          if (null != k) {
            mq.addMessage(MessageType.RESERVED_WORD_USED_AS_IDENTIFIER,
                tq.currentPosition(), k);
          }
        }
        if (!isIdentifier(s)) {
          throw new ParseException(
              new Message(MessageType.INVALID_IDENTIFIER,
                          tq.currentPosition(), MessagePart.Factory.valueOf(s))
              );
        }
        break;
      case KEYWORD:
        if (!allowReservedWords) {
          mq.addMessage(MessageType.RESERVED_WORD_USED_AS_IDENTIFIER,
                        tq.currentPosition(), Keyword.fromString(s));
        }
        break;
      default:
        throw new ParseException(
            new Message(MessageType.EXPECTED_TOKEN, tq.currentPosition(),
                        MessagePart.Factory.valueOf("an identifier"),
                        MessagePart.Factory.valueOf(s)));
    }
    tq.advance();
    return decodeIdentifier(s);
  }

  /**
   * String form of a regular expression that matches the javascript
   * IdentifierOrKeyword production, with extensions for quasiliteral
   * syntax.
   * <p>From section 7.6 of EcmaScript 262 Edition 3 (ES3), currently found at
   * http://www.ecma-international.org/publications/files/ECMA-ST/Ecma-262.pdf
   * and based on http://www.erights.org/elang/grammar/quasi-overview.html
   * <pre>
   * <b>QuasiIdentifierOrKeyword</b> ->
   *       IdentifierOrKeyword
   *    |  QuasiliteralBegin IdentifierOrKeyword OptQuasiliteralQuantifier
   * <b>IdentifierOrKeyword</b> ->
   *       IdentifierName (but not Keyword)
   * <b>IdentifierName</b> ->
   *       IdentifierStart
   *    |  IdentifierName IdentifierPart
   * <b>IdentifierStart</b> ->
   *       UnicodeLetter  |  $  |  _  |  \ UnicodeEscapeSequence
   * <b>IdentifierPart</b> ->
   *       IdentifierStart  |  UnicodeCombiningMark  |  UnicodeDigit
   *    |  UnicodeConnectorPunctuation  |  \ UnicodeEscapeSequence
   * <b>UnicodeLetter</b> ->
   *       any character in the Unicode categories "Uppercase letter
   *       (Lu)", "Lowercase letter (Ll)", "Titlecase letter (Lt)",
   *       "Modifier letter (Lm)", "Other letter (Lo)", or "Letter
   *       number (Nl)".
   * <b>UnicodeCombiningMark</b> ->
   *       any character in the Unicode categories "Non-spacing mark (Mn)"
   *       or "Combining spacing mark (Mc)"
   * <b>UnicodeDigit</b> ->
   *       any character in the Unicode category "Decimal number (Nd)"
   * <b>UnicodeConnectorPunctuation</b> ->
   *       any character in the Unicode category "Connector punctuation (Pc)"
   * <b>UnicodeEscapeSequence</b> ->
   *       u HexDigit HexDigit HexDigit HexDigiti
   * <b>HexDigit</b> ->
   *       0  |  1  |  2  |  3  |  4  |  5  |  6  |  7  |  8  |  9  |  a
   *    |  b  |  c  |  d  |  e  |  f  |  A  |  B  |  C  |  D  |  E  |  F
   * <b>QuasiliteralBegin</b> ->
   *       '@'
   * <b>OptQuasiliteralQuantifier</b> ->
   *       &epsilon;
   *    |  '*'
   *    |  '+'
   *    |  '?'
   * </pre>
   * A <i>UnicodeEscapeSequence</i> cannot be used to put a character
   * into an identifier that would otherwise be illegal.
   */
  private static final Pattern IDENTIFIER_OR_KEYWORD_RE;
  private static final Pattern QUASI_IDENTIFIER_OR_KEYWORD_RE;
  private static final Pattern UNICODE_ESCAPE;  // hexDigits captured in group 1
  static {
    String hexDigit = "[0-9a-fA-F]";
    String letter = "\\p{javaLetter}";
    String letterOrDigit = "\\p{javaLetterOrDigit}";
    String combinerOrConnector = "\\p{Mn}\\p{Mc}\\p{Pc}";
    String identifierStart = "[" + letter + "$_]";
    String identifierPart = "[" + letterOrDigit + combinerOrConnector + "$_]";
    String quasiliteralBegin = "@";
    String optQuasiliteralQuantifier = "[\\+\\*\\?]?";
    String identifierOrKeyword = identifierStart + identifierPart + "*";
    IDENTIFIER_OR_KEYWORD_RE = Pattern.compile("^" + identifierOrKeyword + "$");
    String quasiIdentifierOrKeyword = (
        "(?:" + identifierOrKeyword + ")"
        + "|"
        + "(?:" + (
            quasiliteralBegin
            + identifierOrKeyword
            + optQuasiliteralQuantifier)
        + ")");
    QUASI_IDENTIFIER_OR_KEYWORD_RE = Pattern.compile(
        "^" + quasiIdentifierOrKeyword + "$");

    UNICODE_ESCAPE = Pattern.compile("\\\\u(" + hexDigit + "{4})");
  }

  public static boolean isJavascriptIdentifier(String s) {
    return IDENTIFIER_OR_KEYWORD_RE.matcher(decodeIdentifier(s)).matches()
        && Normalizer.isNormalized(s);
  }

  public static boolean isQuasiIdentifier(String s) {
    return QUASI_IDENTIFIER_OR_KEYWORD_RE.matcher(decodeIdentifier(s)).matches()
        && Normalizer.isNormalized(s);
  }

  public boolean isIdentifier(String s) {
    return (isQuasiliteral
            ? QUASI_IDENTIFIER_OR_KEYWORD_RE
            : IDENTIFIER_OR_KEYWORD_RE).matcher(decodeIdentifier(s)).matches()
        && Normalizer.isNormalized(s);
  }

  /**
   * Decodes escapes in an identifier to their literal codepoints so that
   * identifiers can be compared for equality via string equality of their
   * values.
   */
  public static String decodeIdentifier(String identifier) {
    // TODO(mikesamuel): is this true?
    // Javascript identifiers use a different escaping scheme from strings.
    // Specifically, \Uxxxxxxxx escapes handle extended unicode.  There are
    // 8 hex digits allowed even though extended unicode can't use more than
    // 6 of those.
    if (identifier.indexOf('\\') < 0) { return identifier; }
    StringBuffer sb = new StringBuffer();
    Matcher m = UNICODE_ESCAPE.matcher(identifier);
    while (m.find()) {
      m.appendReplacement(sb, "");
      sb.append((char) Integer.parseInt(m.group(1), 16));
    }
    m.appendTail(sb);
    return sb.toString();
  }
}
