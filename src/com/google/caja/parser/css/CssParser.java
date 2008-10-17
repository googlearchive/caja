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

package com.google.caja.parser.css;

import com.google.caja.lexer.CssLexer;
import com.google.caja.lexer.CssTokenType;
import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.ParseException;
import com.google.caja.lexer.Token;
import com.google.caja.lexer.TokenQueue;
import com.google.caja.lexer.TokenQueue.Mark;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageType;
import com.google.caja.util.Strings;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Produces a parse tree from CSS2.
 *
 * @author mikesamuel@gmail.com
 */
public final class CssParser {
  private final TokenQueue<CssTokenType> tq;

  /**
   * @param tq the token queue to parse from.  Consumed.
   */
  public CssParser(TokenQueue<CssTokenType> tq) {
    this.tq = tq;
  }

  public CssTree.StyleSheet parseStyleSheet() throws ParseException {
    // stylesheet
    //   : [ CHARSET_SYM STRING ';' ]?
    //     [S|CDO|CDC]* [ import [S|CDO|CDC]* ]*
    //     [ [ ruleset | media | page ] [S|CDO|CDC]* ]*
    Mark m = tq.mark();
    List<CssTree.CssStatement> stmts = new ArrayList<CssTree.CssStatement>();
    while (true) {
      skipTopLevelIgnorables();
      if (!lookaheadSymbol("@import")) { break; }
      stmts.add(parseImport());
    }
    while (true) {
      skipTopLevelIgnorables();
      if (tq.isEmpty()) { break; }
      stmts.add(parseStatement());
    }
    return new CssTree.StyleSheet(pos(m), stmts);
  }

  /** Parse a series of css properties as seen in an xhtml style attribute. */
  public CssTree.DeclarationGroup parseDeclarationGroup()
      throws ParseException {
    Mark m = tq.mark();
    List<CssTree.Declaration> decls = new ArrayList<CssTree.Declaration>();
    while (!tq.isEmpty()) {
      while (tq.lookaheadToken(";")) { tq.advance(); }
      if (tq.isEmpty()) { break; }
      decls.add(parseDeclaration());
      if (!tq.checkToken(";")) { break; }
    }
    return new CssTree.DeclarationGroup(pos(m), decls);
  }

  private CssTree.Import parseImport() throws ParseException {
    Mark m = tq.mark();
    expectSymbol("@import");
    CssTree.UriLiteral uri = parseUri();
    List<CssTree.Medium> media = Collections.<CssTree.Medium>emptyList();
    if (!tq.checkToken(";")) {
      media = new ArrayList<CssTree.Medium>();
      do {
        media.add(parseMedium());
      } while (tq.checkToken(","));
      tq.expectToken(";");
    }
    return new CssTree.Import(pos(m), uri, media);
  }

  private CssTree.Medium parseMedium() throws ParseException {
    Mark m = tq.mark();
    return new CssTree.Medium(pos(m), expectIdent());
  }

  private CssTree.CssStatement parseStatement() throws ParseException {
    if (lookaheadSymbol("@media")) {
      return parseMedia();
    } else if (lookaheadSymbol("@page")) {
      return parsePage();
    } else if (lookaheadSymbol("@font-face")) {
      return parseFontFace();
    } else {
      return parseRuleSet();
    }
  }

  private CssTree.Media parseMedia() throws ParseException {
    Mark m = tq.mark();
    expectSymbol("@media");
    List<CssTree> children = new ArrayList<CssTree>();
    do {
      children.add(parseMedium());
    } while (tq.checkToken(","));
    tq.expectToken("{");
    while (!tq.checkToken("}")) {
      children.add(parseRuleSet());
    }
    return new CssTree.Media(pos(m), children);
  }

  private CssTree.Page parsePage() throws ParseException {
    Mark m = tq.mark();
    expectSymbol("@page");
    Token<CssTokenType> t = tq.peek();
    String ident = null;
    if (CssTokenType.IDENT == t.type) {
      ident = unescape(t);
      tq.advance();
    }
    List<CssTree.PageElement> elements = new ArrayList<CssTree.PageElement>();
    if (tq.lookaheadToken(":")) {
      Mark m2 = tq.mark();
      tq.expectToken(":");
      String pseudoPage = expectIdent();
      elements.add(new CssTree.PseudoPage(pos(m2), pseudoPage));
    }
    tq.expectToken("{");
    do {
      elements.add(parseDeclaration());
    } while (tq.checkToken(";"));
    tq.expectToken("}");
    return new CssTree.Page(pos(m), ident, elements);
  }

  private CssTree.FontFace parseFontFace() throws ParseException {
    Mark m = tq.mark();
    expectSymbol("@font-face");
    List<CssTree.Declaration> elements = new ArrayList<CssTree.Declaration>();
    tq.expectToken("{");
    do {
      elements.add(parseDeclaration());
    } while (tq.checkToken(";"));
    tq.expectToken("}");
    return new CssTree.FontFace(pos(m), elements);
  }

  private CssTree.Operation parseOperator() throws ParseException {
    Mark m = tq.mark();
    Token<CssTokenType> t = tq.peek();
    CssTree.Operator op = CssTree.Operator.NONE;
    if (CssTokenType.PUNCTUATION == t.type) {
      if ("/".equals(t.text)) {
        op = CssTree.Operator.DIV;
        tq.advance();
      } else if (",".equals(t.text)) {
        op = CssTree.Operator.COMMA;
        tq.advance();
      } else if ("=".equals(t.text)) {
        op = CssTree.Operator.EQUAL;
        tq.advance();
      }
    }
    return new CssTree.Operation(pos(m), op);
  }

  private CssTree.Combination parseCombinator() throws ParseException {
    Mark m = tq.mark();
    Token<CssTokenType> t = tq.peek();
    CssTree.Combinator comb = CssTree.Combinator.DESCENDANT;
    if (CssTokenType.PUNCTUATION == t.type) {
      if ("+".equals(t.text)) {
        comb = CssTree.Combinator.SIBLING;
        tq.advance();
      } else if (">".equals(t.text)) {
        comb = CssTree.Combinator.CHILD;
        tq.advance();
      }
    }
    return new CssTree.Combination(pos(m), comb);
  }

  private CssTree.Property parseProperty() throws ParseException {
    Mark m = tq.mark();
    String ident = expectIdent();
    return new CssTree.Property(pos(m), ident);
  }


  private CssTree.RuleSet parseRuleSet() throws ParseException {
    Mark m = tq.mark();
    List<CssTree> elements = new ArrayList<CssTree>();
    do {
      elements.add(parseSelector());
    } while (tq.checkToken(","));
    tq.expectToken("{");
    do {
      elements.add(parseDeclaration());
    } while (tq.checkToken(";"));
    tq.expectToken("}");
    return new CssTree.RuleSet(pos(m), elements);
  }

  private CssTree.Selector parseSelector() throws ParseException {
    Mark m = tq.mark();
    List<CssTree> elements = new ArrayList<CssTree>();
    while (true) {
      if (!elements.isEmpty()) {
        elements.add(parseCombinator());
      }
      elements.add(parseSimpleSelector());
      Token<CssTokenType> t = tq.peek();
      // Check whether the next token continues the selector.
      // See also http://www.w3.org/TR/REC-CSS2/selector.html#q1
      if (tq.isEmpty()
          || (CssTokenType.PUNCTUATION == t.type
              && !":*.[+>".contains(t.text))) {
        break;
      }
    }
    return new CssTree.Selector(pos(m), elements);
  }

  private CssTree.SimpleSelector parseSimpleSelector() throws ParseException {
    Mark m = tq.mark();
    List<CssTree> elements = new ArrayList<CssTree>();
    {
      Token<CssTokenType> t = tq.peek();
      if (CssTokenType.IDENT == t.type) {
        String elementName = unescape(t);
        tq.advance();
        elements.add(new CssTree.IdentLiteral(pos(m), elementName));
      } else if ("*".equals(t.text)) {
        // We can't treat * as an ident or store the element name as the
        // parse tree node's value since * would be ambiguous with the ident \2A
        tq.advance();
        elements.add(new CssTree.WildcardElement(pos(m)));
      }
    }
    while (!tq.isEmpty() && (elements.isEmpty() || adjacent())) {
      Token<CssTokenType> t = tq.peek();
      if (CssTokenType.HASH == t.type) {
        elements.add(new CssTree.IdLiteral(t.pos, unescape(t)));
        tq.advance();
      } else if (".".equals(t.text)) {
        elements.add(parseClass());
      } else if ("[".equals(t.text)) {
        elements.add(parseAttrib());
      } else if (":".equals(t.text)) {
        elements.add(parsePseudo());
      } else {
        break;
      }
    }
    if (elements.isEmpty()) {
      throw new ParseException(
          new Message(
              MessageType.EXPECTED_TOKEN, tq.currentPosition(),
              MessagePart.Factory.valueOf("{ident}"),
              MessagePart.Factory.valueOf(tq.peek().text)));
    }

    return new CssTree.SimpleSelector(pos(m), elements);
  }

  private CssTree.ClassLiteral parseClass() throws ParseException {
    Mark m = tq.mark();
    tq.expectToken(".");
    String name = "." + expectIdent();
    return new CssTree.ClassLiteral(pos(m), name);
  }

  private CssTree.Attrib parseAttrib() throws ParseException {
    Mark m = tq.mark();
    tq.expectToken("[");
    String ident = expectIdent();
    CssTree.AttribOperation op = null;
    Token<CssTokenType> t = tq.peek();
    if (CssTokenType.PUNCTUATION == t.type) {
      if ("=".equals(t.text)) {
        op = new CssTree.AttribOperation(t.pos, CssTree.AttribOperator.EQUAL);
        tq.advance();
      } else if ("~=".equals(t.text)) {
        op = new CssTree.AttribOperation(
            t.pos, CssTree.AttribOperator.INCLUDES);
        tq.advance();
      } else if ("|=".equals(t.text)) {
        op = new CssTree.AttribOperation(
            t.pos, CssTree.AttribOperator.DASHMATCH);
        tq.advance();
      }
    }
    CssTree.CssLiteral value = null;
    if (null != op) {
      t = tq.peek();
      if (CssTokenType.STRING == t.type) {
        String s = unescape(t);
        s = s.substring(1, s.length() - 1);
        value = new CssTree.StringLiteral(t.pos, s);
        tq.advance();
      } else {
        value = new CssTree.IdentLiteral(t.pos, expectIdent());
      }
    }

    tq.expectToken("]");
    return new CssTree.Attrib(pos(m), ident, op, value);
  }

  private CssTree.Pseudo parsePseudo() throws ParseException {
    Mark m = tq.mark();
    tq.expectToken(":");
    Mark m2 = tq.mark();
    Token<CssTokenType> t = tq.peek();
    CssTree.CssExprAtom atom;
    if (CssTokenType.FUNCTION == t.type) {
      String fnName = unescape(t);
      fnName = fnName.substring(0, fnName.length() - 1); // strip trailing (
      tq.advance();
      Mark m3 = tq.mark();
      String argIdent = expectIdent();
      FilePosition pos3 = pos(m3);
      CssTree.IdentLiteral lit = new CssTree.IdentLiteral(pos3, argIdent);
      CssTree.Term term = new CssTree.Term(pos3, null, lit);
      CssTree.Expr arg =
        new CssTree.Expr(pos3, Collections.singletonList(term));
      tq.expectToken(")");
      atom = new CssTree.FunctionCall(pos(m2), fnName, arg);
    } else {
      String ident = expectIdent();
      atom = new CssTree.IdentLiteral(pos(m2), ident);
    }
    return new CssTree.Pseudo(pos(m), atom);
  }

  private CssTree.Declaration parseDeclaration() throws ParseException {
    Mark m = tq.mark();
    List<CssTree> children = new ArrayList<CssTree>();
    if (!(tq.lookaheadToken("}") || tq.lookaheadToken(";"))) {
      children.add(parseProperty());
      tq.expectToken(":");
      children.add(parseExpr());
      if (!(tq.isEmpty() || tq.lookaheadToken("}") || tq.lookaheadToken(";"))) {
        children.add(parsePrio());
      }
    }
    return new CssTree.Declaration(pos(m), children);
  }

  private CssTree.Prio parsePrio() throws ParseException {
    Token<CssTokenType> t = tq.peek();
    if (CssTokenType.DIRECTIVE == t.type) {
      String s = Strings.toLowerCase(unescape(t));
      if ("!important".equals(s)) {
        tq.advance();
        return new CssTree.Prio(t.pos, s);
      }
    }
    throw new ParseException(
        new Message(
            MessageType.EXPECTED_TOKEN, t.pos,
            MessagePart.Factory.valueOf("!important"),
            MessagePart.Factory.valueOf(t.text)));
  }

  private CssTree.Expr parseExpr() throws ParseException {
    Mark m = tq.mark();
    List<CssTree> children = new ArrayList<CssTree>();
    children.add(parseTerm());
    while (!tq.isEmpty()) {
      Token<CssTokenType> t = tq.peek();
      if (CssTokenType.PUNCTUATION == t.type) {
        if (!("=".equals(t.text) || "/".equals(t.text) || ",".equals(t.text)
              || "-".equals(t.text))) {
          break;
        }
      } else if (CssTokenType.DIRECTIVE == t.type) {
        break;
      }
      children.add(parseOperator());
      children.add(parseTerm());
    }
    return new CssTree.Expr(pos(m), children);
  }

  private CssTree.Term parseTerm() throws ParseException {
    Mark m = tq.mark();
    Token<CssTokenType> t = tq.peek();
    CssTree.UnaryOperator op = null;
    if (CssTokenType.PUNCTUATION == t.type) {
      if ("+".equals(t.text)) {
        op = CssTree.UnaryOperator.IDENTITY;
        tq.advance();
      } else if ("-".equals(t.text)) {
        op = CssTree.UnaryOperator.NEGATION;
        tq.advance();
      }
    }

    Mark m2 = tq.mark();
    CssTree.CssExprAtom expr;
    t = tq.pop();
    switch (t.type) {
      case QUANTITY:
        expr = new CssTree.QuantityLiteral(pos(m2), unescape(t));
        break;
      case STRING:
      {
        String value = unescape(t);
        value = value.substring(1, value.length() - 1);
        expr = new CssTree.StringLiteral(pos(m2), value);
        break;
      }
      case HASH:
        String color = unescape(t);
        // Require that it have 3 or 6 digits
        if (color.length() != 4 && color.length() != 7) {
          throw new ParseException(
              new Message(MessageType.UNEXPECTED_TOKEN, t.pos,
                          MessagePart.Factory.valueOf(t.text)));
        }
        try {
          expr = new CssTree.HashLiteral(pos(m2), color);
        } catch (IllegalArgumentException e) {
          throw new ParseException(
              new Message(MessageType.UNEXPECTED_TOKEN, t.pos,
                          MessagePart.Factory.valueOf(t.text)));
        }
        break;
      case UNICODE_RANGE:
        expr = new CssTree.UnicodeRangeLiteral(pos(m2), unescape(t));
        break;
      case URI:
        tq.rewind(m2);
        expr = parseUri();
        break;
      case IDENT:
        expr = new CssTree.IdentLiteral(pos(m2), unescape(t));
        break;
      case FUNCTION: {
        String fn = unescape(t);
        fn = fn.substring(0, fn.length() - 1);  // strip trailing '('
        CssTree.Expr arg = parseExpr();
        tq.expectToken(")");
        expr = new CssTree.FunctionCall(pos(m2), fn, arg);
        break;
      }
      case SUBSTITUTION:
        expr = new CssTree.Substitution(t.pos, t.text);
        break;
      default:
        throw new ParseException(
            new Message(MessageType.UNEXPECTED_TOKEN, t.pos,
                        MessagePart.Factory.valueOf(t.text)));
    }

    return new CssTree.Term(pos(m), op, expr);
  }

  private CssTree.UriLiteral parseUri() throws ParseException {
    Token<CssTokenType> t = tq.peek();
    Mark m = tq.mark();
    try {
      String uriStr;
      switch (t.type) {
        case URI:
        {
          String s = t.text;
          s = s.substring(s.indexOf('(') + 1, s.lastIndexOf(')')).trim();
          s = unescape(s, false);
          tq.advance();
          if (s.length() >= 2) {
            char ch0 = s.charAt(0);
            if (('\'' == ch0 || '\"' == ch0)
                && ch0 == s.charAt(s.length() - 1)) {
              s = s.substring(1, s.length() - 1);
            }
          }
          uriStr = s;
          break;
        }
        case STRING:
        {
          String s = unescape(t);
          tq.advance();
          uriStr = s.substring(1, s.length() - 1);
          break;
        }
        default:
          throw new ParseException(
              new Message(
                  MessageType.EXPECTED_TOKEN, t.pos,
                  MessagePart.Factory.valueOf("{uri}"),
                  MessagePart.Factory.valueOf(t.text)));
      }
      return new CssTree.UriLiteral(pos(m), new URI(uriStr));
    } catch (URISyntaxException ex) {
      throw new ParseException(
          new Message(MessageType.MALFORMED_URI, t.pos,
                      MessagePart.Factory.valueOf(t.text)), ex);
    }
  }


  private FilePosition pos(Mark m) throws ParseException {
    // TODO(mikesamuel): fix this so it doesn't throw a parse exception at end
    // of file.
    // Example input "import 'foo.css'" without a semicolon.
    FilePosition start = m.getFilePosition(),
                   end = tq.lastPosition();
    return !(start.startCharInFile() > end.endCharInFile() &&
             start.source().equals(end.source()))
         ? FilePosition.span(start, end)
         : FilePosition.startOf(start);
  }

  /**
   * Unescapes all escape sequences from the given tokens text.
   * See sections 4.1.3 and 4.3.10 of the CSS 2 spec.
   */
  static String unescape(Token<CssTokenType> t) {
    // We want to remove spaces between ! and important, and any similar
    // declarations
    return unescape(t.text, t.type != CssTokenType.STRING);
  }

  static String unescape(String s, boolean removeSpaces) {
    int pos = 0;
    StringBuilder sb = null;
    for (int i = 0, n = s.length(); i < n; ++i) {
      char ch = s.charAt(i);
      // http://www.w3.org/TR/CSS21/syndata.html#value-def-string
      // string         {string1}|{string2}
      // string1        \"([^\n\r\f\\"]|\\{nl}|{escape})*\"
      // string2        \'([^\n\r\f\\']|\\{nl}|{escape})*\'
      // escape         {unicode}|\\[^\n\r\f0-9a-f]
      // unicode        \\[0-9a-f]{1,6}(\r\n|[ \n\r\t\f])?
      if ('\\' == ch && i + 1 < n) {
        if (null == sb) { sb = new StringBuilder(); }
        sb.append(s, pos, i);
        char ch1 = s.charAt(++i);
        if (CssLexer.isHexChar(ch1)) {
          // up to 6 hex digits
          int end = i;
          while (end < n && end - i < 6 && CssLexer.isHexChar(s.charAt(end))) {
            ++end;
          }
          int chi = Integer.parseInt(s.substring(i, end), 16);
          i = end - 1;
          // Hex escape may be followed by a single space character to separate
          // it from any following character that happens to be a hex digit.
          if (i + 1 < n) {
            char nextChar = s.charAt(i + 1);
            if (CssLexer.isSpaceChar(nextChar)) {
              ++i;
              // "\r\n" is specially handled in the {escape} production above.
              if ('\r' == nextChar && i + 1 < n && '\n' == s.charAt(i + 1)) {
                ++i;
              }
            }
          }

          // chi may have up to 6 digits, so may be outside Java's char's range,
          // but is within the range supported by java.lang.String codepoints.
          sb.appendCodePoint(chi);
        } else if ('\r' == ch1) {
          // Newline not considered part of string
          if (i + 1 < n && s.charAt(i + 1) == '\n') { ++i; }
        } else if ('\n' == ch1) {
          // Newline not considered part of string
        } else {
          sb.append(ch1);
        }
        pos = i + 1;
      } else if (CssLexer.isSpaceChar(ch) && removeSpaces) {
        if (null == sb) { sb = new StringBuilder(); }
        if (i > pos) { sb.append(s, pos, i); }
        pos = i + 1;
      }
    }
    if (null == sb) {
      return s;
    } else {
      sb.append(s, pos, s.length());
      return sb.toString();
    }
  }

  private boolean lookaheadSymbol(String symbol) throws ParseException {
    if (tq.isEmpty()) { return false; }
    Token<CssTokenType> t = tq.peek();
    return t.type == CssTokenType.SYMBOL
        && Strings.equalsIgnoreCase(symbol, unescape(t));
  }

  private void expectSymbol(String symbol) throws ParseException {
    Token<CssTokenType> t = tq.pop();
    if (t.type == CssTokenType.SYMBOL
        && Strings.equalsIgnoreCase(symbol, unescape(t))) {
      return;
    }
    throw new ParseException(
        new Message(
            MessageType.EXPECTED_TOKEN, t.pos,
            MessagePart.Factory.valueOf(symbol),
            MessagePart.Factory.valueOf(t.text)));
  }

  private String expectIdent() throws ParseException {
    Token<CssTokenType> t = tq.pop();
    if (CssTokenType.IDENT == t.type) {
      return unescape(t);
    }
    throw new ParseException(
        new Message(
            MessageType.EXPECTED_TOKEN, t.pos,
            MessagePart.Factory.valueOf("{identifier}"),
            MessagePart.Factory.valueOf(t.text)));
  }

  /**
   * Skip {@code <!--} and {@code -->} which can only be ignored in some
   * contexts.
   *
   * From the lexical grammar:<pre>
   * "<!--"              {return CDO;}
   * "-->"               {return CDC;}
   * </pre>
   *
   * From the parser grammer:<pre>
   * stylesheet
   *   : [ CHARSET_SYM STRING ';' ]?
   *     [S|<b>CDO|CDC</b>]* [ import [S|<b>CDO|CDC</b>]* ]*
   *     [ [ ruleset | media | page ] [S|<b>CDO|CDC</b>]* ]*
   * </pre>
   */
  private void skipTopLevelIgnorables() throws ParseException {
    while (!tq.isEmpty()) {
      Token<CssTokenType> t = tq.peek();
      if (CssTokenType.PUNCTUATION != t.type
          || !("<!--".equals(t.text) || "-->".equals(t.text))) {
        break;
      }
      tq.advance();
    }
  }

  /**
   * True if the current token is directly adjacent to the last with no
   * intervening whitespace.
   */
  private boolean adjacent() throws ParseException {
    FilePosition last = tq.lastPosition();
    FilePosition current = tq.currentPosition();
    return null != last && null != current
      && last.endCharInFile() == current.startCharInFile();
  }
}
