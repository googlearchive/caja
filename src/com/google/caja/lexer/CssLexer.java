// Copyright (C) 2006 Google Inc.
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

import com.google.caja.reporting.DevNullMessageQueue;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.MessageType;
import com.google.caja.util.Strings;
import com.google.common.collect.Lists;

import java.util.LinkedList;
import java.util.NoSuchElementException;

/**
 * A lexer that recognizes the
 * <a href="http://www.w3.org/TR/CSS21/grammar.html#scanner">CSS 2.1 Grammar</a>
 * plus line comments as interpreted by most browsers.
 * <p>
 * TODO(mikesamuel): CSS2.1 has changed lexical conventions to effectively
 * decode escapes at lex time in most contexts.  E.g., the rule
 * <code>"@import"              IMPORT_SYM</code> now reads
 * <code>@{I}{M}{P}{O}{R}{T}    {return IMPORT_SYM;}</code> and
 * <code>{num}ms                TIME</code> now reads
 * <code>{num}{M}{S}            {return TIME;}</code>.
 *
 * @author mikesamuel@gmail.com
 */
public final class CssLexer implements TokenStream<CssTokenType> {
  private final CssSplitter splitter;
  private final LinkedList<Token<CssTokenType>> pending = Lists.newLinkedList();

  // TODO(mikesamuel): all clients should pass in a proper queue
  public CssLexer(CharProducer cp) {
    this(cp, DevNullMessageQueue.singleton(), false);
  }

  /**
   * @param allowSubstitutions true iff ${...} style substitutions should be
   *   allowed as described at {@link CssTokenType#SUBSTITUTION}
   */
  public CssLexer(
      CharProducer cp, MessageQueue mq, boolean allowSubstitutions) {
    assert null != cp;
    this.splitter = new CssSplitter(cp, mq, allowSubstitutions);
  }

  public boolean hasNext() throws ParseException {
    return !pending.isEmpty() || splitter.hasNext();
  }

  public Token<CssTokenType> next() throws ParseException {
    produce();
    if (null == pending) { throw new NoSuchElementException(); }
    return pending.removeFirst();
  }

  /**
   * True iff ${...} style substitutions should be
   * allowed as described at {@link CssTokenType#SUBSTITUTION}
   * @see #allowSubstitutions(boolean)
   */
  public boolean areSubstitutionsAllowed() {
    return splitter.areSubstitutionsAllowed();
  }

  /**
   * Changes the substitution policy for this lexer.
   * @see #areSubstitutionsAllowed()
   */
  public void allowSubstitutions(boolean allow) {
    splitter.allowSubstitutions(allow);
  }

  /**
   * Decodes escapes in an identifier
   */
  public static String decodeCssIdentifier(CharSequence ident) {
    StringBuilder sb = null;
    int pos = 0;
    for (int i = 0, n = ident.length(); i < n;) {
      if (ident.charAt(i) == '\\') {
        if (sb == null) { sb = new StringBuilder(); }
        sb.append(ident, pos, i);
        int codepoint = 0;
        while (++i < n && isHexChar(ident.charAt(i))) {
          char ch = ident.charAt(i);
          codepoint <<= 4;
          if (ch >= '0' && ch <= '9') {
            codepoint |= ch - '0';
          } else if (ch >= 'a' && ch <= 'f') {
            codepoint |= ch + 10 - 'a';
          } else {
            codepoint |= ch + 10 - 'A';
          }
        }
        sb.appendCodePoint(codepoint < Character.MAX_CODE_POINT
                           ? codepoint
                           : 0xfffd);
        if (i < n && isSpaceChar(ident.charAt(i))) { ++i; }
        pos = i;
      } else {
        ++i;
      }
    }
    if (sb == null) { return ident.toString(); }
    return sb.append(ident, pos, ident.length()).toString();
  }

  /**
   * <pre>
   * nmstart    [_a-z]|{nonascii}|{escape}
   * nonascii   [\200-\377]
   * </pre>
   * @return true iff ch is a nmstart and is not an escape.
   *     Call {@link #decodeCssIdentifier} before this
   *     method to figure out whether an escape sequence is a nmstart
   */
  public static boolean isNmStart(char ch) {
    return (ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z')
        || (ch >= 0200 && ch <= 0377) || ch == '_';
  }

  /**
   * If the character producer has not been exhausted, ensures that there is a
   * token on pending on pending.
   */
  private void produce() throws ParseException {
    if (!pending.isEmpty()) { return; }
    if (!splitter.hasNext()) { return; }

    Token<CssTokenType> t = splitter.next();
    pending.add(t);
    if (t.type == CssTokenType.PUNCTUATION && splitter.hasNext()) {
      if ("!".equals(t.text)) {  // Join !important
        // IMPORTANT_SYM        "!"({w}|{comment})*{I}{M}{P}{O}{R}{T}{A}{N}{T}
        Token<CssTokenType> t2 = splitter.next();
        while (t2 != null && (t2.type == CssTokenType.SPACE
                              || t2.type == CssTokenType.COMMENT)) {
          pending.add(t2);
          t2 = splitter.hasNext() ? splitter.next() : null;
        }
        // The !important is significant regardless of case and whether or not a
        // letter is hex escaped.
        if (null != t2) {
          pending.add(t2);
          if (t2.type == CssTokenType.IDENT
              && Strings.eqIgnoreCase(
                  "important", decodeCssIdentifier(t2.text))) {
            reduce(CssTokenType.DIRECTIVE);
          }
        }
      } else if ("-".equals(t.text)) {  // Join '-'{nmstart}{nmchar}*
        Token<CssTokenType> t2 = splitter.next();
        if (null != t2) {
          pending.add(t2);
          if (t2.type == CssTokenType.IDENT) {
            reduce(CssTokenType.IDENT);
          }
        }
      }
    }
  }

  /**
   * Reduces the pending tokens to a single token with the given type.
   * For example, if the pending list contains an identifier followed by an
   * open parenthesis, then it can be reduced to a single function token.
   * This is necessitated by CSS2's odd lexical convention which classifies as
   * single tokens things that most other languages treat as sequences of
   * primitive tokens.
   * <p>
   * Modifies the pending list in place.
   */
  private void reduce(CssTokenType type) {
    StringBuilder sb = new StringBuilder();
    for (Token<CssTokenType> t : pending) {
      sb.append(t.text);
    }
    FilePosition fp
        = FilePosition.span(pending.getFirst().pos, pending.getLast().pos);
    pending.clear();
    pending.add(Token.instance(sb.toString(), type, fp));
  }

  /**
   * Is the given character a whitespace character according to the CSS 2 spec.
   */
  public static boolean isSpaceChar(char ch) {
    // s      [ \t\r\n\f]+
    // w      {s}?
    switch (ch) {
      case ' ': case '\t': case '\r': case '\n': case '\f':
        return true;
      default:
        return false;
    }
  }

  /** Is the given character a hex digit? */
  public static boolean isHexChar(char ch) {
    // h     [0-9a-f]
    return (ch >= '0' && ch <= '9') || (ch >= 'a' && ch <= 'f')
        || (ch >= 'A' && ch <= 'F');
  }
}

final class CssSplitter implements TokenStream<CssTokenType> {
  private final CharProducer cp;
  private final MessageQueue mq;
  private boolean allowSubstitutions;
  private Token<CssTokenType> pending;

  /**
   * @param allowSubstitutions true iff ${...} style substitutions should be
   *   allowed as described at {@link CssTokenType#SUBSTITUTION}
   */
  CssSplitter(CharProducer cp, MessageQueue mq, boolean allowSubstitutions) {
    assert null != cp;
    // Longest punctuation tokens are <!-- and --> so need LA(3).
    this.cp = cp;
    this.mq = mq;
    this.allowSubstitutions = allowSubstitutions;
  }

  public boolean hasNext() throws ParseException {
    produce();
    return null != pending;
  }

  public Token<CssTokenType> next() throws ParseException {
    produce();
    if (null == pending) { throw new NoSuchElementException(); }
    Token<CssTokenType> result = pending;
    pending = null;
    return result;
  }

  boolean areSubstitutionsAllowed() { return allowSubstitutions; }

  public void allowSubstitutions(boolean allow) {
    this.allowSubstitutions = allow;
  }


  private void produce() throws ParseException {
    if (null != pending) { return; }
    if (cp.isEmpty()) { return; }

    char[] buf = cp.getBuffer();
    final int start = cp.getOffset();
    int limit = cp.getLimit();
    int end = start + 1;

    CssTokenType type;
    char ch = buf[start];

    int identEnd;
    if (CssLexer.isSpaceChar(ch)) {
      // [ \t\r\n\f]+        S

      end = parseWhitespace(buf, end, limit);
      type = CssTokenType.SPACE;
    } else if (ch == '/') {
      if (end < limit && buf[end] == '*') {
        // \/\*[^*]*\*+([^/*][^*]*\*+)*\/    /* ignore comments */
        int state = 0;  // 0 - start, 1 - in comment, 2 - saw, 3 - done
        do {
          if (end == limit) { break; }
          ch = buf[end];
          switch (state) {
            case 0: state = 1; break;
            case 1: if (ch == '*') { state = 2; } break;
            case 2:
              if (ch == '/') {
                state = 3;
              } else if (ch != '*') {
                state = 1;
              }
              break;
          }
          ++end;
        } while (state != 3);
        if (state != 3) {
          throw new ParseException(new Message(
              MessageType.UNTERMINATED_COMMENT_TOKEN,
              cp.filePositionForOffsets(start, end)));
        }
        type = CssTokenType.COMMENT;
      } else if (end < limit && buf[end] == '/') {
        do {
          if (++end == limit) { break; }
          ch = buf[end];
          // Line comment does not contain the newline character that ends it
          // since we don't want to break \r\n sequences across two tokens,
          // and for consistency with JavaScript conventions which exclude the
          // newline from the line comment token.
          if (ch == '\r' || ch == '\n') { break; }
        } while (true);
        type = CssTokenType.COMMENT;
        FilePosition commentPos = cp.filePositionForOffsets(start, end);
        mq.addMessage(MessageType.INVALID_CSS_COMMENT, commentPos);
      } else {
        //               *yytext
        type = CssTokenType.PUNCTUATION;
      }
    } else if (end < limit && '=' == buf[end] &&
        ('~' == ch || '|' == ch || '^' == ch || '$' == ch || '*' == ch)) {
      // "~="          INCLUDES
      // "|="          DASHMATCH
      // "^="          HEADMATCH
      // "$="          TAILMATCH
      // "*="          SUBSTRINGMATCH
      ++end;
      type = CssTokenType.PUNCTUATION;

    } else if (ch == '\'' || ch == '"') {
      end = parseString(cp, start);
      type = CssTokenType.STRING;

    } else if (ch == '@') {

      identEnd = parseIdent(cp, end);
      if (identEnd != -1) {
        // "@import"       IMPORT_SYM
        // "@page"         PAGE_SYM
        // "@media"        MEDIA_SYM
        // "@font-face"    FONT_FACE_SYM
        // "@charset "      CHARSET_SYM
        // "@"{ident}      ATKEYWORD
        type = CssTokenType.SYMBOL;
        end = identEnd;
        // In http://www.w3.org/TR/CSS21/grammar.html, the CHARSET_SYM is
        // allowed to match only "@charset "
        if ((end - start) == 8 && parseMatch(cp, start, "@charset ") > 0) {
          ++end;
        }
      } else {
        //        .        *yytext
        type = CssTokenType.PUNCTUATION;
      }
    } else if (ch == '!') {
      // "!{w}important" IMPORTANT_SYM
      // handled by token joining at a later pass

      //          .      *yytext

      type = CssTokenType.PUNCTUATION;
    } else if (ch == '#') {
      int nameEnd = parseName(cp, end);
      if (nameEnd >= 0) {
        // "#"{name}       HASH
        type = CssTokenType.HASH;
        end = nameEnd;
      } else {
        //          .      *yytext
        type = CssTokenType.PUNCTUATION;
      }

    } else if (ch == '<' || ch == '-') {
      // "<!--"          CDO
      // "-->"           CDC

      int tailEnd = parseMatch(cp, end, ch == '<' ? "!--" : "->");
      if (tailEnd >= 0) { end = tailEnd; }
      type = CssTokenType.PUNCTUATION;

    } else if ((ch >= '0' && ch <= '9') || '.' == ch) {
      // {num}em         EMS
      // {num}ex         EXS
      // {num}px         LENGTH
      // {num}cm         LENGTH
      // {num}mm         LENGTH
      // {num}in         LENGTH
      // {num}pt         LENGTH
      // {num}pc         LENGTH
      // {num}deg        ANGLE
      // {num}rad        ANGLE
      // {num}grad       ANGLE
      // {num}ms         TIME
      // {num}s          TIME
      // {num}Hz         FREQ
      // {num}kHz        FREQ
      // {num}{ident}    DIMEN
      // {num}%          PERCENTAGE
      // {num}           NUMBER
      boolean isNum;
      if ('.' == ch) {
        int numEnd = parseInt(cp, end);
        isNum = numEnd >= 0;
        if (isNum) { end = numEnd; }
      } else {
        isNum = true;
        end = parseNum(cp, start);
      }

      if (isNum) {
        identEnd = parseIdent(cp, end);
        if (identEnd >= 0) {
          end = identEnd;
        } else if (end < limit && '%' == buf[end]) {
          ++end;
        }
        type = CssTokenType.QUANTITY;
      } else {
        // lone .
        //          .      *yytext
        type = CssTokenType.PUNCTUATION;
      }

    } else if ((identEnd = parseIdent(cp, start)) >= 0) {
      end = identEnd;
      if (end - start == 1 && 'U' == ch && end < limit && '+' == buf[end]) {
        // U\+{range}      UNICODERANGE
        // U\+{h}{1,6}-{h}{1,6}    UNICODERANGE
        // range         \?{1,6}|{h}(\?{0,5}|{h}(\?{0,4}|{h}\
        //               (\?{0,3}|{h}(\?{0,2}|{h}(\??|{h})))))

        type = CssTokenType.UNICODE_RANGE;
        ++end;
        end = parseRange(cp, end);
      } else if (end < limit && '(' == buf[end]) {
        ++end;
        if (end - start == 4 && parseMatch(cp, start, "url(") >= 0) {
          // "url("{w}{string}{w}")" URI
          // "url("{w}{url}{w}")"    URI
          end = parseWhitespace(buf, end, limit);
          int stringEnd = parseString(cp, end);
          int uriEnd = stringEnd < 0 ? parseUri(cp, end) : -1;
          if (stringEnd < 0 && uriEnd < 0) {
            throw new ParseException(new Message(
                MessageType.EXPECTED_TOKEN,
                cp.filePositionForOffsets(end, end),
                MessagePart.Factory.valueOf("{url}"), toMessagePart(cp, end)));
          }
          end = stringEnd >= 0 ? stringEnd : uriEnd;
          end = parseWhitespace(buf, end, limit);
          if (end == limit || ')' != buf[end]) {
            throw new ParseException(new Message(
                MessageType.EXPECTED_TOKEN,
                cp.filePositionForOffsets(end, end),
                MessagePart.Factory.valueOf(")"), toMessagePart(cp, end)));
          }
          ++end;
          type = CssTokenType.URI;
        } else {
          // {ident}"("      FUNCTION
          type = CssTokenType.FUNCTION;
        }
      } else {
        // {ident}         IDENT
        type = CssTokenType.IDENT;
      }

    } else if (ch == '$' && allowSubstitutions) {
      // ${<javascript tokens>}

      if (end < limit && buf[end] != '{') {
        type = CssTokenType.PUNCTUATION;
      } else {
        // 0 - non string
        // 1 - quoted string
        // 2 - saw \ in string
        // 3 - saw close paren
        int state = 0;
        // number of parenthetical blocks entered and not exited
        int nOpen = 0;
        char delim = 0;
        do {
          if (end == limit) { break; }
          ch = buf[end];
          switch (state) {
            case 0:
              if (ch == '"' || ch == '\'') {
                delim = ch;
                state = 1;
              } else if (ch == '{') {
                ++nOpen;
              } else if (ch == '}') {
                if (--nOpen == 0) {
                  state = 3;
                }
              }
              break;
            case 1:
              if (ch == delim) {
                state = 0;
              } else if (ch == '\\') {
                state = 2;
              }
              break;
            case 2:
              state = 1;
              break;
          }
          ++end;
        } while (state != 3);
        if (state != 3) {
          throw new ParseException(new Message(
              MessageType.UNTERMINATED_STRING_TOKEN,
              cp.filePositionForOffsets(start, end)));
        }

        identEnd = parseIdent(cp, end);
        if (identEnd >= 0) {end = identEnd;
        } else if (end != limit && '%' == buf[end]) {
          ++end;
        }

        type = CssTokenType.SUBSTITUTION;
      }
    } else {
      //          .      *yytext
      type = CssTokenType.PUNCTUATION;
    }
    assert end > start;
    pending = Token.instance(cp.toString(start, end), type,
                             cp.filePositionForOffsets(start, end));
    cp.consumeTo(end);
  }

  private static int parseMatch(CharProducer cp, int start, String match) {
    int len = match.length();
    int limit = cp.getLimit();
    if (limit - start < len) { return -1; }
    char[] buf = cp.getBuffer();
    for (int i = 0; i < len; ++i) {
      char chB = buf[start + i];
      char chM = match.charAt(i);
      if (!(chB == chM || ((chB | 0x20) == chM && chB >= 'A' && chB < 'Z'))) {
        return -1;
      }
    }
    return start + len;
  }

  private static int parseString(CharProducer cp, int start)
      throws ParseException {
    int limit = cp.getLimit();
    if (start == limit) { return -1; }
    char[] buf = cp.getBuffer();
    char ch = buf[start];
    if (ch != '\'' && ch != '"') { return -1; }

    // {string}        STRING
    // string1         \"([^\n\r\f\\"]|\\{nl}|{escape})*\"
    // string2         \'([^\n\r\f\\']|\\{nl}|{escape})*\'
    // string          {string1}|{string2}

    char delim = ch;
    int end = start + 1;
    while (end < limit) {
      ch = buf[end];
      ++end;
      // escape            {unicode}|\\[^\r\n\f0-9a-f]
      // nl                \n|\r\n|\r|\f
      if (delim == ch) {
        return end;
      } else if (ch == '\\') {
        if (end < limit && isLineBreak(ch = buf[end])) {
          ++end;
          if (ch == '\r' && end < limit && buf[end] == '\n') {
            ++end;
          }
        } else {
          end = parseEscapeBody(cp, end);
        }
      } else if (isLineBreak(ch)) {
        throw new ParseException(new Message(
            MessageType.MALFORMED_STRING,
            cp.filePositionForOffsets(end - 1, end - 1),
            MessagePart.Factory.valueOf("" + ch)));
      }
    }
    throw new ParseException(new Message(
        MessageType.UNTERMINATED_STRING_TOKEN,
        cp.filePositionForOffsets(start, end)));
  }

  private static int parseUri(CharProducer cp, int start)
      throws ParseException {
    // url     ([!#$%&*-~]|{nonascii}|{escape})*
    char[] buf = cp.getBuffer();
    int limit = cp.getLimit();
    int end = start;
    while (end < limit) {
      if (isUriChar(buf[end])) {
        ++end;
      } else if (buf[end] == '\\') {
        end = parseEscapeBody(cp, end + 1);
      } else {
        break;
      }
    }
    return end;
  }
  private static boolean isUriChar(char ch) {
    switch (ch) {
    case '!':
    case '#':
    case '$':
    case '%':
    case '&':
      return true;
    default:
      return (ch >= '*' && ch <= '~') || isNonAscii(ch);
    }
  }

  private static boolean isLineBreak(char ch) {
    // nl                \n|\r\n|\r|\f
    switch (ch) {
      case '\r': case '\n': case '\f':
        return true;
      default:
        return false;
    }
  }

  private static int parseWhitespace(char[] buf, int end, int limit) {
    // w       [ \t\r\n\f]*
    while (end < limit && CssLexer.isSpaceChar(buf[end])) { ++end; }
    return end;
  }

  /**
   * Only handles the case where num does not start with a dot since it is
   * hard to distinguish a "." token from a number token with 1 char lookahead.
   */
  private static int parseNum(CharProducer cp, int start)
      throws ParseException {
    //      num     [0-9]+|[0-9]*"."[0-9]+

    int end = parseInt(cp, start);
    assert end >= 0;
    int limit = cp.getLimit();
    char[] buf = cp.getBuffer();
    if (end < limit && '.' == buf[end]) {
      ++end;
      char ch;
      // By CSS rules, 0. is an invalid number.
      if (end == limit || (ch = buf[end]) < '0' || ch > '9') {
        throw new ParseException(new Message(
            MessageType.MALFORMED_NUMBER, cp.filePositionForOffsets(start, end),
            MessagePart.Factory.valueOf(cp.toString(start, end))));
      }
      return parseInt(cp, end);
    }
    return end;
  }

  private static int parseInt(CharProducer cp, int start) {
    int limit = cp.getLimit();
    if (start == limit) { return -1; }
    char[] buf = cp.getBuffer();
    char ch = buf[start];
    if (ch >= '0' && ch <= '9') {
      int end = start;
      do {
        if (++end == limit) { break; }
        ch = buf[end];
      } while (ch >= '0' && ch <= '9');
      return end;
    } else {
      return -1;
    }
  }

  private static int parseIdent(CharProducer cp, int start)
      throws ParseException {
    // ident      -?{nmstart}{nmchar}*
    // We later join '-' to the front of an identifier, so don't start here.
    int end = parseNmStart(cp, start);
    if (end < 0) { return -1; }
    for (int nmCharEnd; (nmCharEnd = parseNmChar(cp, end)) >= 0;) {
      end = nmCharEnd;
    }
    return end;
 }

  private static int parseName(CharProducer cp, int start)
      throws ParseException {
    // name      {nmchar}+
    int end = parseNmChar(cp, start);
    if (end < 0) { return -1; }
    for (int nmCharEnd; (nmCharEnd = parseNmChar(cp, end)) >= 0;) {
      end = nmCharEnd;
    }
    return end;
  }

  private static int parseNmStart(CharProducer cp, int start)
      throws ParseException {
    if (start == cp.getLimit()) { return -1; }
    char ch = cp.getBuffer()[start];
    if (CssLexer.isNmStart(ch)) { return start + 1; }
    if (ch == '\\') { return parseEscapeBody(cp, start + 1); }
    return -1;
  }

  private static int parseNmChar(CharProducer cp, int start)
      throws ParseException {
    // nmchar     [_a-z0-9-]|{nonascii}|{escape}
    int end = parseNmStart(cp, start);
    if (end >= 0) { return end; }
    if (start != cp.getLimit()) {
      char ch = cp.getBuffer()[start];
      if ((ch >= '0' && ch <= '9') || ch == '-') { return start + 1; }
    }
    return -1;
  }

  private static int parseEscapeBody(CharProducer cp, int start)
      throws ParseException {
    // unicode    \\{h}{1,6}(\r\n|[ \t\r\n\f])?
    // escape     {unicode}|\\[^\r\n\f0-9a-f]
    int limit = cp.getLimit();
    char[] buf = cp.getBuffer();
    if (start == limit) {
      throw new ParseException(
          new Message(MessageType.EXPECTED_TOKEN,
                      cp.filePositionForOffsets(start, start),
                      MessagePart.Factory.valueOf("<hex-digit>"),
                      MessagePart.Factory.valueOf("<end-of-input>")));
    }
    char ch = buf[start];
    if (CssLexer.isHexChar(ch)) {
      int end = start + 1;
      for (int i = 5; --i >= 0; ++end) {
        if (end == limit) { break; }
        ch = buf[end];
        if (!CssLexer.isHexChar(ch)) { break; }
      }
      if (end < limit && CssLexer.isSpaceChar(ch = buf[end])) {
        ++end;

        if ('\r' == ch && end < limit && '\n' == buf[end]) {
          ++end;
        }
      }
      return end;
    } else if (isLineBreak(ch)) {
      throw new ParseException(
          new Message(
              MessageType.UNRECOGNIZED_ESCAPE,
              cp.filePositionForOffsets(start, start),
              MessagePart.Factory.valueOf(String.valueOf(ch))));
    } else {
      return start + 1;
    }
  }

  private static int parseRange(CharProducer cp, int start)
      throws ParseException {
    // range         \?{1,6}|{h}(\?{0,5}|{h}(\?{0,4}|{h}\
    //               (\?{0,3}|{h}(\?{0,2}|{h}(\??|{h})))))
    // This method also handles {h}{1,6}-{h}{1,6}

    char[] buf = cp.getBuffer();
    int limit = cp.getLimit();

    int end = start;
    int len = 6;
    boolean isRange = end < limit && buf[end] == '?';
    if (isRange) {
      while (end < limit && '?' == buf[end] && --len >= 0) { ++end; }
    }
    while (end < limit && CssLexer.isHexChar(buf[end]) && --len >= 0) { ++end; }
    if (!isRange) {
      if (end == limit || '-' != buf[end]) {
        throw new ParseException(
            new Message(
                MessageType.EXPECTED_TOKEN,
                cp.filePositionForOffsets(end, end),
                MessagePart.Factory.valueOf("-"), toMessagePart(cp, end)));
      }
      ++end;

      len = 6;
      while (end < limit && '?' == buf[end] && --len >= 0) { ++end; }
      while (end < limit && CssLexer.isHexChar(buf[end]) && --len >= 0) {
        ++end;
      }
    }
    return end != start ? end : -1;
  }

  // nonascii    [\200-\377]
  private static boolean isNonAscii(char ch) {
    return ch >= '\200' && ch <= '\377';
  }

  private static MessagePart toMessagePart(CharProducer cp, int offset) {
    return MessagePart.Factory.valueOf(
        offset == cp.getLimit()
        ? "<end-of-input>"
        : "" + cp.getBuffer()[offset]);
  }
}
