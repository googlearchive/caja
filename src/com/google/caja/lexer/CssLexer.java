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

import com.google.caja.lexer.CharProducer.MutableFilePosition;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageType;
import com.google.caja.util.Strings;

import java.io.IOException;

import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;

/**
 * A lexer that recognizes the
 * <a href="http://www.w3.org/TR/CSS21/grammar.html#scanner">CSS 2.1 Grammar</a>
 *
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
  private final LinkedList<Token<CssTokenType>> pending
      = new LinkedList<Token<CssTokenType>>();

  public CssLexer(CharProducer cp) {
    this(cp, false);
  }

  /**
   * @param allowSubstitutions true iff ${...} style substitutions should be
   *   allowed as described at {@link CssTokenType#SUBSTITUTION}
   */
  public CssLexer(CharProducer cp, boolean allowSubstitutions) {
    assert null != cp;
    this.splitter = new CssSplitter(cp, allowSubstitutions);
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
              && Strings.equalsIgnoreCase(
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
  private final LookaheadCharProducer cp;
  private boolean allowSubstitutions;
  private Token<CssTokenType> pending;

  /**
   * @param allowSubstitutions true iff ${...} style substitutions should be
   *   allowed as described at {@link CssTokenType#SUBSTITUTION}
   */
  CssSplitter(CharProducer cp, boolean allowSubstitutions) {
    assert null != cp;
    // Longest punctuation tokens are <!-- and --> so need LA(3).
    this.cp = new LookaheadCharProducer(cp, 3);
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


  private static final Pattern URL_RE = Pattern.compile(
      "^url\\($", Pattern.CASE_INSENSITIVE);
  private MutableFilePosition spos = new MutableFilePosition(),
                              epos = new MutableFilePosition();
  private void produce() throws ParseException {

    cp.getCurrentPosition(spos);
    StringBuilder sb = new StringBuilder();
    try {
      if (null != pending) { return; }
      int chi = cp.lookahead();
      if (chi < 0) { return; }
      char ch = (char) chi;

      CssTokenType type;
      if (CssLexer.isSpaceChar(ch)) {
        // [ \t\r\n\f]+        S
        sb.append(ch);
        cp.read();

        parseWhitespace(sb);
        type = CssTokenType.SPACE;
      } else if (ch == '/') {
        sb.append(ch);
        cp.read();

        if (cp.lookahead() == '*') {
          // \/\*[^*]*\*+([^/*][^*]*\*+)*\/    /* ignore comments */
          int state = 0;  // 0 - start, 1 - in comment, 2 - saw, 3 - done
          do {
            chi = cp.read();
            if (chi < 0) { break; }
            ch = (char) chi;
            sb.append(ch);
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
          } while (state != 3);
          if (state != 3) {
            throw new ParseException(new Message(
                MessageType.UNTERMINATED_COMMENT_TOKEN,
                spos.toFilePosition()));
          }
          type = CssTokenType.COMMENT;
        } else {
          //               *yytext
          type = CssTokenType.PUNCTUATION;
        }
      } else if ('~' == ch || '|' == ch) {
        sb.append(ch);
        cp.read();

        if ('=' == cp.lookahead()) {
          // "~="          INCLUDES
          // "|="          DASHMATCH
          sb.append('=');
          cp.read();
        } else {
          //        .      *yytext
        }
        type = CssTokenType.PUNCTUATION;
      } else if (ch == '\'' || ch == '"') {
        parseString(sb);
        type = CssTokenType.STRING;

      } else if (ch == '@') {
        sb.append(ch);
        cp.read();

        if (parseIdent(sb)) {
          // "@import"       IMPORT_SYM
          // "@page"         PAGE_SYM
          // "@media"        MEDIA_SYM
          // "@font-face"    FONT_FACE_SYM
          // "@charset"      CHARSET_SYM
          // "@"{ident}      ATKEYWORD
          type = CssTokenType.SYMBOL;
        } else {
          //        .        *yytext
          type = CssTokenType.PUNCTUATION;
        }
      } else if (ch == '!') {
        sb.append(ch);
        cp.read();

        // "!{w}important" IMPORTANT_SYM
        // handled by token joining at a later pass

        //          .      *yytext

        type = CssTokenType.PUNCTUATION;
      } else if (ch == '#') {
        sb.append(ch);
        cp.read();

        if (parseName(sb)) {
          // "#"{name}       HASH
          type = CssTokenType.HASH;
        } else {
          //          .      *yytext
          type = CssTokenType.PUNCTUATION;
        }

      } else if (ch == '<' || ch == '-') {
        // "<!--"          CDO
        // "-->"           CDC

        String tail = ch == '<' ? "!--" : "->";

        sb.append(ch);
        cp.read();

        cp.fetch(tail.length());

        boolean matchedTail = true;
        for (int i = 0; i < tail.length(); ++i) {
          if (cp.peek(i) != tail.charAt(i)) {
            matchedTail = false;
            break;
          }
        }
        type = CssTokenType.PUNCTUATION;
        if (matchedTail) {
          sb.append(tail);
          cp.consume(tail.length());
        }

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
          sb.append(ch);
          cp.read();
          isNum = parseInt(sb);
        } else {
          isNum = true;
          parseNum(sb);
        }

        if (isNum) {
          if (!parseIdent(sb) && '%' == cp.lookahead()) {
            sb.append('%');
            cp.read();
          }
          type = CssTokenType.QUANTITY;
        } else {
          // lone .
          //          .      *yytext
          type = CssTokenType.PUNCTUATION;
        }

      } else if (parseIdent(sb)) {
        if (sb.length() == 1 && 'U' == ch && '+' == cp.lookahead()) {
          // U\+{range}      UNICODERANGE
          // U\+{h}{1,6}-{h}{1,6}    UNICODERANGE
          // range         \?{1,6}|{h}(\?{0,5}|{h}(\?{0,4}|{h}\
          //               (\?{0,3}|{h}(\?{0,2}|{h}(\??|{h})))))

          type = CssTokenType.UNICODE_RANGE;
          sb.append('+');
          cp.read();
          parseRange(sb);
        } else if ('(' == cp.lookahead()) {
          sb.append('(');
          cp.read();
          if (URL_RE.matcher(sb).matches()) {
            // "url("{w}{string}{w}")" URI
            // "url("{w}{url}{w}")"    URI
            parseWhitespace(sb);
            if (!(parseString(sb) || parseUri(sb))) {
              throw new ParseException(new Message(
                  MessageType.EXPECTED_TOKEN,
                  cp.getCurrentPosition(),
                  MessagePart.Factory.valueOf("{url}"),
                  MessagePart.Factory.valueOf(ch)));
            }
            parseWhitespace(sb);
            int ch2 = cp.read();
            if (')' != ch2) {
              throw new ParseException(new Message(
                  MessageType.EXPECTED_TOKEN,
                  cp.getCurrentPosition(),
                  MessagePart.Factory.valueOf(")"),
                  MessagePart.Factory.valueOf(ch2)));
            }
            sb.append(')');
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
        sb.append(ch);
        cp.read();

        if (cp.lookahead() != '{') {
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
            chi = cp.read();
            if (chi < 0) { break; }
            ch = (char) chi;
            sb.append(ch);
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
          } while (state != 3);
          if (state != 3) {
            throw new ParseException(new Message(
                MessageType.UNTERMINATED_STRING_TOKEN,
                spos.toFilePosition()));
          }

          if (!parseIdent(sb) && '%' == cp.lookahead()) {
            sb.append('%');
            cp.read();
          }

          type = CssTokenType.SUBSTITUTION;
        }
      } else {
        //          .      *yytext
        sb.append(ch);
        cp.read();
        type = CssTokenType.PUNCTUATION;
      }
      cp.getCurrentPosition(epos);
      assert sb.length() > 0
           : "ch=" + ch + " : " + chi + " : " + spos + " : " + type;
      pending = Token.instance(sb.toString(), type,
          FilePosition.instance(
              spos.source,
              spos.lineNo, spos.lineNo, spos.charInFile, spos.charInLine,
              epos.lineNo, epos.lineNo, epos.charInFile, epos.charInLine));
    } catch (IOException ex) {
      cp.getCurrentPosition(spos);
      throw new ParseException(
          new Message(MessageType.IO_ERROR, spos.source));
    }
  }

  private boolean parseString(StringBuilder sb)
     throws IOException, ParseException {
    int chi = cp.lookahead();
    if (chi != '\'' && chi != '"') {
      return false;
    }
    sb.append((char) chi);
    cp.read();  // consume delim

    // {string}        STRING
    // string1         \"([^\n\r\f\\"]|\\{nl}|{escape})*\"
    // string2         \'([^\n\r\f\\']|\\{nl}|{escape})*\'
    // string          {string1}|{string2}

    int delim = chi;
    while (true) {
      if (!parseOneStringChar(sb)) {
        chi = cp.lookahead();
        if (delim == chi) {
          sb.append((char) chi);
          cp.read();
          break;
        } else if (chi == '\'' || chi == '"') {
          sb.append((char) chi);
          cp.read();
        } else {
          throw new ParseException(new Message(
              MessageType.MALFORMED_STRING, cp.getCurrentPosition(),
              MessagePart.Factory.valueOf(String.valueOf((char) chi))));
        }
      }
    }
    return true;
  }

  private boolean parseOneStringChar(StringBuilder sb)
      throws IOException, ParseException {
    int chi = cp.lookahead();
    if (chi < 0) {
      throw new ParseException(
          new Message(
              MessageType.UNTERMINATED_STRING_TOKEN, cp.getCurrentPosition()));
    }
    char ch = (char) chi;
    switch (ch) {
      case '\n': case '\r': case '\f': case '\"': case '\'':
        return false;
      case '\\':
        return parseEscapeOrNewline(sb);
    }

    cp.read();
    sb.append(ch);
    return true;
  }

  private boolean parseUri(StringBuilder sb)
      throws IOException, ParseException {
    // url     ([!#$%&*-~]|{nonascii}|{escape})*
    for (int chi; (chi = cp.lookahead()) >= 0;) {
      if (isUriChar((char) chi)) {
        sb.append((char) chi);
        cp.read();
      } else if (!parseEscape(sb)) {
        break;
      }
    }
    return true;
  }
  private boolean isUriChar(char ch) {
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

  private void parseWhitespace(StringBuilder sb) throws IOException {
    // w       [ \t\r\n\f]*
    for (int chi; (chi = cp.lookahead()) >= 0
         && CssLexer.isSpaceChar((char) chi);) {
      sb.append((char) chi);
      cp.read();
    }
  }

  /**
   * Only handles the case where num does not start with a dot since it is
   * hard to distinguish a "." token from a number token with 1 char lookahead.
   */
  private void parseNum(StringBuilder sb) throws IOException, ParseException {
    //      num     [0-9]+|[0-9]*"."[0-9]+

    boolean result = parseInt(sb);
    assert result;
    if ('.' == cp.lookahead()) {
      sb.append('.');
      cp.read();
      int chi = cp.lookahead();
      if (chi < '0' || chi > '9') {
        throw new ParseException(new Message(
            MessageType.MALFORMED_NUMBER, cp.getCurrentPosition(),
            MessagePart.Factory.valueOf(sb.toString())));
      }
      parseInt(sb);
    }
  }

  private boolean parseInt(StringBuilder sb) throws IOException {
    int chi = cp.lookahead();
    if (chi >= '0' && chi <= '9') {
      do {
        sb.append((char) chi);
        cp.read();
      } while ((chi = cp.lookahead()) >= '0' && chi <= '9');
      return true;
    } else {
      return false;
    }
  }

  private boolean parseIdent(StringBuilder sb)
      throws IOException, ParseException {
    // ident      -?{nmstart}{nmchar}*
    // We later join '-' to the front of an identifier, so don't start here.
    if (parseNmStart(sb)) {
      while (parseNmChar(sb)) { }
      return true;
   }
   return false;
 }

  private boolean parseName(StringBuilder sb)
      throws IOException, ParseException {
    // name      {nmchar}+
    if (parseNmChar(sb)) {
      while (parseNmChar(sb)) { }
      return true;
    }
    return false;
  }

  private boolean parseNmStart(StringBuilder sb)
      throws IOException, ParseException {
    // nmstart    [_a-z]|{nonascii}|{escape}
    // nonascii   [\200-\377]
    int chi = cp.lookahead();
    if (chi < 0) { return false; }
    if ((chi >= 'a' && chi <= 'z') || (chi >= 'A' && chi <= 'Z')
        || (chi >= 0200 && chi <= 0377) || chi == '_') {
      sb.append((char) chi);
      cp.read();
      return true;
    }
    return parseEscape(sb);
  }

  private boolean parseEscape(StringBuilder sb)
      throws IOException, ParseException {
    int chi = cp.lookahead();
    if (chi != '\\') { return false; }

    sb.append((char) chi);
    cp.read();  // skip \\
    parseEscapeBody(sb);
    return true;
  }

  private boolean parseEscapeOrNewline(StringBuilder sb)
      throws IOException, ParseException {
    // escape            {unicode}|\\[^\r\n\f0-9a-f]
    // nl                \n|\r\n|\r|\f
    int chi = cp.lookahead();
    if (chi != '\\') { return false; }

    sb.append((char) chi);
    cp.read();  // skip \\

    chi = cp.lookahead();
    switch (chi) {
      case '\n': case '\f':
        sb.append((char) chi);
        cp.read();
        break;
      case '\r':
        sb.append('\r');
        cp.read();
        if ('\n' == cp.lookahead()) {
          sb.append('\n');
          cp.read();
        }
        break;
      default:
        parseEscapeBody(sb);
        break;
    }
    return true;
  }

  private void parseEscapeBody(StringBuilder sb)
      throws IOException, ParseException {
    // unicode    \\{h}{1,6}(\r\n|[ \t\r\n\f])?
    // escape     {unicode}|\\[^\r\n\f0-9a-f]
    int chi = cp.read();
    if (chi < 0) {
      throw new ParseException(
          new Message(MessageType.EXPECTED_TOKEN,
                      cp.getCurrentPosition(),
                      MessagePart.Factory.valueOf("<hex-digit>"),
                      MessagePart.Factory.valueOf("<end-of-input>")));
    }
    char ch = (char) chi;
    if (CssLexer.isHexChar(ch)) {
      sb.append(ch);
      for (int i = 5; --i >= 0;) {
        chi = cp.lookahead();
        if (chi < 0) { break; }
        ch = (char) chi;
        if (!CssLexer.isHexChar(ch)) { break; }
        sb.append(ch);
        cp.read();
      }
      if (chi >= 0 && CssLexer.isSpaceChar(ch = (char) chi)) {
        sb.append(ch);
        cp.read();

        if ('\r' == ch && '\n' == cp.lookahead()) {
          sb.append(ch);
          cp.read();
        }
      }
    } else if (ch != '\r' && ch != '\n' && ch != '\f') {
      sb.append(ch);
    } else {
      throw new ParseException(
          new Message(
              MessageType.UNRECOGNIZED_ESCAPE, cp.getCurrentPosition(),
              MessagePart.Factory.valueOf(String.valueOf(ch))));
    }
  }

  private boolean parseNmChar(StringBuilder sb)
      throws IOException, ParseException {
    // nmchar     [_a-z0-9-]|{nonascii}|{escape}
    if (parseNmStart(sb)) { return true; }
    int chi = cp.lookahead();
    if ((chi >= '0' && chi <= '9') || chi == '-') {
      sb.append((char) chi);
      cp.read();
      return true;
    }
    return false;
  }

  private void parseRange(StringBuilder sb) throws IOException, ParseException {
    // range         \?{1,6}|{h}(\?{0,5}|{h}(\?{0,4}|{h}\
    //               (\?{0,3}|{h}(\?{0,2}|{h}(\??|{h})))))
    // This method also handles {h}{1,6}-{h}{1,6}

    int chi;
    int len = 6;
    boolean isRange = '?' == cp.lookahead();
    if (isRange) {
      while ('?' == cp.lookahead() && --len >= 0) {
        sb.append('?');
        cp.read();
      }
    }
    while ((chi = cp.lookahead()) >= 0
           && CssLexer.isHexChar((char) chi) && --len >= 0) {
      sb.append((char) chi);
      cp.read();
    }
    if (!isRange) {
      chi = cp.read();
      if ('-' != chi) {
        throw new ParseException(
            new Message(
                MessageType.EXPECTED_TOKEN,
                cp.getCurrentPosition(),
                MessagePart.Factory.valueOf("-"),
                chi < 0 ? MessagePart.Factory.valueOf((char) chi)
                        : MessagePart.Factory.valueOf("<end-of-input>")));
      }
      sb.append('-');

      len = 6;
      while ('?' == cp.lookahead() && --len >= 0) {
        sb.append('?');
        cp.read();
      }
      while ((chi = cp.lookahead()) >= 0
             && CssLexer.isHexChar((char) chi) && --len >= 0) {
        sb.append((char) chi);
        cp.read();
      }
    }
  }

  // nonascii    [\200-\377]
  private static boolean isNonAscii(char ch) {
    return ch >= '\200' && ch <= '\377';
  }
}
