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

import java.util.regex.Pattern;

/**
 * Some identifier, keyword, and number handling routines used by multiple
 * parsers.
 *
 * @author mikesamuel@gmail.com
 */
public abstract class ParserBase {
  protected final JsTokenQueue tq;
  protected final MessageQueue mq;

  public ParserBase(JsTokenQueue tq, MessageQueue mq) {
    this.tq = tq;
    this.mq = mq;
  }

  public JsTokenQueue getTokenQueue() { return tq; }
  public MessageQueue getMessageQueue() { return mq; }

  public String parseIdentifier() throws ParseException {
    Token<JsTokenType> t = tq.peek();
    String s = t.text;
    switch (t.type) {
      case WORD:
        if (!isIdentifier(s)) {
          mq.addMessage(MessageType.INVALID_IDENTIFIER,
                        MessagePart.Factory.valueOf(s), tq.currentPosition());
        }
        break;
      case KEYWORD:
        mq.addMessage(MessageType.RESERVED_WORD_USED_AS_IDENTIFIER,
                      Keyword.fromString(s), tq.currentPosition());
        break;
      default:
        throw new ParseException(
            new Message(MessageType.EXPECTED_TOKEN, tq.currentPosition(),
                        MessagePart.Factory.valueOf("an identifier"),
                        MessagePart.Factory.valueOf(s)));
    }
    tq.advance();
    return s;
  }

  /**
   * String form of a regular expression that matches the javascript
   * IdentifierOrKeyword production.
   * <p>From http://www.mozilla.org/js/language/js20/formal/lexer-grammar.html
   * <pre>
   * <b>IdentifierOrKeyword</b> -> IdentifierName
   * IdentifierName ->
   *       InitialIdentifierCharacterOrEscape
   *    |  NullEscapes InitialIdentifierCharacterOrEscape
   *    |  IdentifierName ContinuingIdentifierCharacterOrEscape
   *    |  IdentifierName NullEscape
   * NullEscapes ->
   *       NullEscape
   *    |  NullEscapes NullEscape
   * NullEscape -> \ _
   * InitialIdentifierCharacterOrEscape ->
   *       InitialIdentifierCharacter
   *    |  \ HexEscape
   * InitialIdentifierCharacter -> UnicodeInitialAlphabetic | $ | _
   * ContinuingIdentifierCharacterOrEscape ->
   *       ContinuingIdentifierCharacter
   *    |  \ HexEscape
   * ContinuingIdentifierCharacter -> UnicodeAlphanumeric | $ | _
   * HexEscape ->
   *       x 2HexDigit
   *    |  u 4HexDigit
   *    |  U 8HexDigit
   * </pre>
   */
  private static final Pattern IDENTIFIER_OR_KEYWORD_RE;
  static {
    String hexDigit = "[0-9a-fA-F]";
    String hexEscape =
      "(?:x" + hexDigit + "{2}|u" + hexDigit + "{4}|U" + hexDigit + "{8})";
    String continuingIdentifierCharacter = "[$_\\p{javaLetterOrDigit}]";
    String continuingIdentifierCharacterOrEscape =
      "(?:" + continuingIdentifierCharacter + "|\\\\" + hexEscape + ")";
    String initialIdentifierCharacter = "[$_\\p{javaLetter}]";
    String initialIdentifierCharacterOrEscape =
      "(?:" + initialIdentifierCharacter + "|\\\\" + hexEscape + ")";
    String nullEscape = "(?:\\\\_)";
    /*
     * IdentifierName ->
     *       InitialIdentifierCharacterOrEscape
     *    |  NullEscapes InitialIdentifierCharacterOrEscape
     *    |  IdentifierName ContinuingIdentifierCharacterOrEscape
     *    |  IdentifierName NullEscape
     * is the same as
     * IdentifierName ->
     *       0#NullEscape InitialIdentifierCharacterOrEscape
     *       0#(NullEscape|ContinuingIdentifierCharacterOrEscape)
     */
    String identifierOrKeyword = (
        "(?:" + nullEscape + "*" + initialIdentifierCharacterOrEscape
        + "(?:" + nullEscape + "|" + continuingIdentifierCharacterOrEscape
        + ")*)");
    IDENTIFIER_OR_KEYWORD_RE = Pattern.compile("^" + identifierOrKeyword + "$");
  }

  public static boolean isIdentifier(String s) {
    return IDENTIFIER_OR_KEYWORD_RE.matcher(s).matches();
  }

  public static boolean isIdentifierStart(char ch) {
    return Character.isLetter(ch) || ch == '$' || ch == '_';
  }

  public static boolean isIdentifierPart(char ch) {
    return Character.isLetterOrDigit(ch) || ch == '$' || ch == '_';
  }
}
