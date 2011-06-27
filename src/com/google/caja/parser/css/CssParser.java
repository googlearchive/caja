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

import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.CssLexer;
import com.google.caja.lexer.CssTokenType;
import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.ParseException;
import com.google.caja.lexer.Token;
import com.google.caja.lexer.TokenQueue;
import com.google.caja.lexer.TokenQueue.Mark;
import com.google.caja.parser.css.CssTree.ProgIdAttribute;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.MessageType;
import com.google.caja.reporting.MessageTypeInt;
import com.google.caja.util.Criterion;
import com.google.caja.util.Lists;
import com.google.caja.util.Name;
import com.google.caja.util.Strings;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Produces a parse tree from CSS2.  This parses the CSS2 grammar plus
 * some browser-specific extensions described below.
 *
 * <h2>Error Recovery</h2>
 * This class runs in two modes: tolerant where unrecognized constructs are
 * reported on via the {@link MessageQueue} and dropped, and strict mode where
 * the parse failures result in a {@link ParseException}.
 * <p>
 * In tolerant mode, we follow the rules laid out in
 * <a href="http://www.w3.org/TR/CSS2/syndata.html#parsing-errors">
 *  "Rules for handling parsing errors"</a>
 * but we also attempt to recover from single malformed tokens, by employing
 * a variety of error recovery strategies.
 * <p>
 * In no case do we return a parse tree node for a construct that is not
 * well-formed in isolation, and when parsing a group of items (a selector list
 * or declaration group), we discard when not doing so would cause more styles
 * to apply.  E.g. in <tt>a, b##id, p { color: blue; color::red }</tt> we throw
 * away the <tt>b##id</tt> instead of turning it into <tt>b</tt> since that
 * would result in more styles being applied.  We throw away <tt>color::red</tt>
 * since it is not well-formed in isolation.  We don't need to throw away
 * anything else, since we assume properties are disjoint, and selectors in a
 * comma list are disjoint.  The result after discarding malformed content is
 * <tt>a, p { color: blue }</tt>.
 * <p>
 * When we expect a token that is not there in tolerant mode, we report an error
 * and proceed to the next token that signals the start of a similar chunk or
 * the end of the containing block.
 * Usually this means finding the next <tt>;</tt> or <tt>}</tt>.
 * <p>
 * E.g. in {@code color red; background-color: blue} we expect a <tt>:</tt>
 * after <tt>color</tt> but since none is forthcoming, we skip to the semicolon
 * and start parsing the background color property.
 * <p>
 * Mismatched curly brackets are harder to deal with, so those are handled in
 * the outer loops.
 *
 * <h2>Recovery Strategies</h2>
 * <p>We employ several recovery strategies when we encounter a parsing problem.
 * <ul>
 *   <li>Skipping the item in a list as in selector lists :
 *       {@code a.myclass, "borken", h6} &rarr; {@code a.myclass, h6}.
 *   <li>Skipping a chunk inside a block as in property pairs :
 *       {@code color:red; background -color: blue} &rarr; {@code color: red}
 *   <li>Skipping a chunk that may end with a block as in undefined symbols :
 *       <tt>@unknown { p { color: blue } }</tt>.
 * </ul>
 * We define several generic syntactic constructs and group the actual CSS
 * grammar into these.
 * <ul>
 *   <li>list item &mdash; a minimal run of tokens that does not include a
 *       comma, semicolon, curly bracket, or symbol.
 *   <li>inner chunk &mdash; a minimal run of tokens that does not contain a
 *       symbol or a close curly bracket or semicolon outside a balanced curly
 *       bracket block.
 *   <li>outer chunk &mdash; a run of tokens terminated by a curly bracket
 *       that closes a balanced block, a semicolon outside a balanced curly
 *       bracket block, or the end-of-file marker.
 * </ul>
 * We then group the constructs defined in the CSS grammar into these new
 * syntactic constructs.
 * <ul>
 *   <li>list item includes selectors and mediums.  So a ruleset is then a
 *       list of list items followed by an outer chunk.
 *   <li>inner chunks include property/expression pairs which are separated
 *       by semicolons.  A declaration group is a list of inner chunks
 *       surrounded by curly brackets aka an outer chunk.
 *   <li>outer chunks include most of the symbol based productions and the
 *       ruleset production.  A stylesheet is a list of outer chunks.
 * </ul>
 *
 * <h2>Coding Conventions around error recovery</h2>
 * <p>All ignored tokens not specified in CSS2.1 as ignorable must be reported.
 * We only apply the error recovery strategies where we make a decision about
 * how to proceed.  So the functions that parse list items and parts of list
 * items return {@code null} to indicate a tolerable failure, and the functions
 * that parse lists of list items will examine their return values, and
 * apply one of the recovery strategies.  The recovery strategies are written
 * to make sure that they enqueue a message if the malformed construct contained
 * tokens.
 *
 * <p>All the parsing functions below should obey these conventions
 * <ul>
 *   <li>public parsing functions never return null.
 *   <li>private <tt>parse*</tt> functions return null to indicate a tolerable
 *       failure to parse, or throw a {@link ParseException} to indicate an
 *       intolerable failure.
 *   <li>When a <tt>parse*</tt> function delegates parsing to another function,
 *       one of the following is true: the delegator returns null when the
 *       delegate returns null, or the delegate reports its failure to parse and
 *       the delegator does not, or the delegate does not report failure to
 *       parse and the delegator does.  This does not constrain the delegate
 *       from reporting messages about individual tokens -- only about ranges of
 *       skipped tokens.
 * </ul>
 *
 * <h2>Differences from CSS grammar</h2>
 * <p>This class parses a few extensions to the CSS grammar.
 * <ul>
 *   <li>IE Filters and Transformations are parsed using the grammar below to
 *       a {@link CssTree.ProgId special node class}.  These filters and
 *       transformations are documented on the
 *       <a href="http://msdn.microsoft.com/en-us/library/ms532847(VS.85).aspx"
 *        >MSDN</a> though the grammar below is made up.
 * {@code
 *  ProgId ::== 'progid' ':' <DottedFunctionName> <ProgIdAttributeList>? ')'
 *  DottedFunctionName ::== <Function>    // Includes an open parenthesis
 *                        | <Identifier> '.' <DottedFunctionName>
 *  ProgIdAttributeList ::== <ProgIdAttribute>
 *                         | <ProgIdAttribute> ',' <ProgIdAttributeList>
 *  ProgIdAttribute ::== <Identifier> '=' <Expr>
 * }
 *   See the test file "cssparseinput-filters.css" for examples.
 *
 *   <li>CSS frequently contains
 *       <a href="http://en.wikipedia.org/wiki/CSS_filter">CSS hacks</a>
 *       to make a style apparent to some user-agents and not others.  These are
 *       represented using special node-types so that clients which only want to
 *       deal with standards-compliant CSS can filter out those nodes.
 *       <p>
 *       The star hack described in the wiki article is very widely used, and we
 *       handle it by adding a new type node type: {@link CssTree.UserAgentHack}
 *       which has a set of user agent IDs, and the node that would be visible
 *       to those user-agents.  Clients that care about filters can transform
 *       the tree to remove inappropriate filters, or to transform the tree so
 *       that those filters will be visible in only the appropriate contexts
 *       using CSS.
 * </ul>
 *
 * @author mikesamuel@gmail.com
 */
public final class CssParser {
  private final TokenQueue<CssTokenType> tq;
  private final MessageQueue mq;
  private final MessageLevel tolerance;
  private final boolean isTolerant;

  public static TokenQueue<CssTokenType> makeTokenQueue(
      CharProducer cp, MessageQueue mq, boolean allowSubstitutions) {
    return new TokenQueue<CssTokenType>(
        new CssLexer(cp, mq, allowSubstitutions),
        cp.getCurrentPosition().source(),
        new Criterion<Token<CssTokenType>>() {
          public boolean accept(Token<CssTokenType> t) {
            // Other ignorables are handled in skipTopLevelIgnorables below.
            return CssTokenType.SPACE != t.type
                && CssTokenType.COMMENT != t.type;
          }
        });
  }

  /**
   * @param tq the token queue to parse from.  Consumed.
   * @param mq in {@link #isTolerant() tolerant} mode, receives messages about
   *      parse problems with a {@link Message#getMessageLevel() level} of
   *      {@code tolerance}.
   * @param tolerance if &lt; {@link MessageLevel#FATAL_ERROR}, then
   *      unrecognized constructs will be dropped from the token stream as
   *      described above.
   *      Otherwise, a {@link ParseException} will be thrown.
   */
  public CssParser(
      TokenQueue<CssTokenType> tq, MessageQueue mq, MessageLevel tolerance) {
    assert tq != null && tolerance != null;
    this.tq = tq;
    this.mq = mq;
    this.tolerance = tolerance;
    this.isTolerant = isTolerant();
  }

  public TokenQueue<CssTokenType> getTokenQueue() { return tq; }

  public CssTree.StyleSheet parseStyleSheet() throws ParseException {
    // stylesheet
    //   : [ CHARSET_SYM STRING ';' ]?
    //     [S|CDO|CDC]* [ import [S|CDO|CDC]* ]*
    //     [ [ ruleset | media | page ] [S|CDO|CDC]* ]*
    try {
      Mark m = tq.mark();
      List<CssTree.CssStatement> stmts = Lists.newArrayList();
      while (true) {
        skipTopLevelIgnorables();
        if (!lookaheadSymbol("@import")) { break; }
        addIfNotNull(stmts, parseImport());
      }
      while (true) {
        skipTopLevelIgnorables();
        if (tq.isEmpty()) { break; }
        addIfNotNull(stmts, parseStatement());
      }
      return new CssTree.StyleSheet(pos(m), stmts);
    } catch (RuntimeException e) {
      throw new ParseException(new Message(MessageType.PARSE_ERROR,
          tq.currentPosition()), e);
    }
  }

  /** Parse a series of CSS properties as seen in an XHTML style attribute. */
  public CssTree.DeclarationGroup parseDeclarationGroup()
      throws ParseException {
    try {
      Mark m = tq.mark();
      List<CssTree.Declaration> decls = Lists.newArrayList();
      while (!tq.isEmpty()) {
        while (tq.lookaheadToken(";")) { tq.advance(); }
        if (tq.isEmpty()) { break; }
        addIfNotNull(decls, parseDeclaration());
        if (!tq.checkToken(";")) { break; }
      }
      return new CssTree.DeclarationGroup(pos(m), decls);
    } catch (RuntimeException e) {
      throw new ParseException(new Message(MessageType.PARSE_ERROR,
          tq.currentPosition()), e);
    }
  }

  public boolean isTolerant() {
    return MessageLevel.FATAL_ERROR.compareTo(tolerance) > 0;
  }

  // All the non-public parse methods below may return null in tolerant mode.

  private CssTree.Import parseImport() throws ParseException {
    Mark m = tq.mark();
    expectSymbol("@import");
    CssTree.UriLiteral uri = parseUri();
    if (uri == null) {
      SKIP_TO_CHUNK_END_FROM_OUTSIDE_BLOCK.recover(this, m);
      return null;
    }
    List<CssTree.Medium> media = Collections.<CssTree.Medium>emptyList();
    if (!tq.checkToken(";")) {
      media = Lists.newArrayList();
      do {
        CssTree.Medium medium = parseMedium();
        if (medium == null) {
          SKIP_TO_CHUNK_END_FROM_OUTSIDE_BLOCK.recover(this, m);
          return null;
        }
        media.add(medium);
      } while (tq.checkToken(","));
      if (expect(";", SKIP_TO_CHUNK_END_FROM_OUTSIDE_BLOCK, m)) {
        return null;
      }
    }
    return new CssTree.Import(pos(m), uri, media);
  }

  private CssTree.Medium parseMedium() throws ParseException {
    Mark m = tq.mark();
    String ident = expectIdent();
    if (ident == null) { return null; }
    return new CssTree.Medium(pos(m), Name.css(ident));
  }

  private CssTree.CssStatement parseStatement() throws ParseException {
    Token<CssTokenType> t = tq.peek();
    if (t.type == CssTokenType.SYMBOL) {
      if (lookaheadSymbol("@media")) {
        return parseMedia();
      } else if (lookaheadSymbol("@page")) {
        return parsePage();
      } else if (lookaheadSymbol("@font-face")) {
        return parseFontFace();
      } else if (isTolerant) {
        Mark m = tq.mark();
        tq.advance();
        SKIP_TO_CHUNK_END_FROM_OUTSIDE_BLOCK.recover(this, m);
        return null;
      }
    } else if (isTolerant && ";".equals(t.text)) {
      tq.advance();
      reportSkipping(tq.lastPosition());
      return null;
    }
    return parseRuleSet();
  }

  private CssTree.Media parseMedia() throws ParseException {
    Mark m = tq.mark();
    expectSymbol("@media");
    List<CssTree> children = Lists.newArrayList();
    do {
      CssTree.Medium medium = parseMedium();
      if (medium == null) {
        SKIP_TO_CHUNK_END_FROM_OUTSIDE_BLOCK.recover(this, m);
        return null;
      }
      children.add(medium);
    } while (tq.checkToken(","));
    if (expect("{", SKIP_TO_CHUNK_END_FROM_OUTSIDE_BLOCK, m)) {
      return null;
    }
    while (!tq.checkToken("}")) {
      CssTree.RuleSet rs = parseRuleSet();
      if (rs == null) {
        SKIP_TO_CHUNK_END_FROM_WITHIN_BLOCK.recover(this, m);
        return null;
      } else {
        children.add(rs);
      }
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
    List<CssTree.PageElement> elements = Lists.newArrayList();
    if (tq.lookaheadToken(":")) {
      Mark m2 = tq.mark();
      tq.expectToken(":");
      String pseudoPage = expectIdent();
      if (pseudoPage == null) {
        SKIP_TO_CHUNK_END_FROM_OUTSIDE_BLOCK.recover(this, m);
        return null;
      }
      elements.add(new CssTree.PseudoPage(pos(m2), Name.css(pseudoPage)));
    }
    if (parseDeclarationBlock(elements, m)) { return null; }
    return new CssTree.Page(
        pos(m), ident == null ? null : Name.css(ident), elements);
  }

  private CssTree.FontFace parseFontFace() throws ParseException {
    Mark m = tq.mark();
    expectSymbol("@font-face");
    List<CssTree.Declaration> elements = Lists.newArrayList();
    if (parseDeclarationBlock(elements, m)) { return null; }
    return new CssTree.FontFace(pos(m), elements);
  }

  private boolean parseDeclarationBlock(
      List<? super CssTree.Declaration> decls, Mark start)
      throws ParseException {
    if (expect("{", SKIP_TO_CHUNK_END_FROM_OUTSIDE_BLOCK, start)) {
      return true;
    }
    do {
      addIfNotNull(decls, parseDeclaration());
    } while (tq.checkToken(";"));
    return expect("}", SKIP_TO_CHUNK_END_FROM_WITHIN_BLOCK, start);
  }

  private CssTree.Operation parseOperation() throws ParseException {
    Mark m = tq.mark();
    CssTree.Operator op = CssTree.Operator.NONE;
    if (!tq.isEmpty()) {
      Token<CssTokenType> t = tq.peek();
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
    }
    return new CssTree.Operation(pos(m), op);
  }

  private CssTree.Combination parseCombinator() throws ParseException {
    Mark m = tq.mark();
    CssTree.Combinator comb = CssTree.Combinator.DESCENDANT;
    if (!tq.isEmpty()) {
      Token<CssTokenType> t = tq.peek();
      if (CssTokenType.PUNCTUATION == t.type) {
        if ("+".equals(t.text)) {
          comb = CssTree.Combinator.SIBLING;
          tq.advance();
        } else if (">".equals(t.text)) {
          comb = CssTree.Combinator.CHILD;
          tq.advance();
        }
      }
    }
    return new CssTree.Combination(pos(m), comb);
  }

  private CssTree.Property parseProperty() throws ParseException {
    Mark m = tq.mark();
    String ident = expectIdent();
    if (ident == null && isTolerant) { return null; }

    // When !isTolerant, expectIdent throws instead of returning null.
    assert ident != null;

    return new CssTree.Property(pos(m), Name.css(ident));
  }


  private CssTree.RuleSet parseRuleSet() throws ParseException {
    Mark m = tq.mark();
    List<CssTree> elements = Lists.newArrayList();
    do {
      CssTree.Selector sel = parseSelector();
      addIfNotNull(elements, sel);
    } while (tq.checkToken(","));
    if (elements.isEmpty()) {
      SKIP_TO_CHUNK_END_FROM_OUTSIDE_BLOCK.recover(this, m);
      return null;
    }
    if (parseDeclarationBlock(elements, m)) { return null; }
    return new CssTree.RuleSet(pos(m), elements);
  }

  private CssTree.Selector parseSelector() throws ParseException {
    Mark m = tq.mark();
    List<CssTree> elements = Lists.newArrayList();
    while (true) {
      if (!elements.isEmpty()) {
        elements.add(parseCombinator());
      }
      CssTree.SimpleSelector sel = parseSimpleSelector();
      if (sel == null) {
        SKIP_COMMA_LIST_ITEM.recover(this, m);
        return null;
      }
      elements.add(sel);
      // Check whether the next token continues the selector.
      // See also http://www.w3.org/TR/REC-CSS2/selector.html#q1
      if (tq.isEmpty()) { break; }
      Token<CssTokenType> t = tq.peek();
      if (CssTokenType.PUNCTUATION == t.type && !":*.[+>".contains(t.text)) {
        break;
      }
    }
    if (elements.isEmpty()) {
      SKIP_COMMA_LIST_ITEM.recover(this, m);
      return null;
    }
    return new CssTree.Selector(pos(m), elements);
  }

  private CssTree.SimpleSelector parseSimpleSelector() throws ParseException {
    Mark m = tq.mark();
    List<CssTree> elements = Lists.newArrayList();
    if (!tq.isEmpty()) {
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
      CssTree selectorPart;
      if (CssTokenType.HASH == t.type) {
        String identifier = unescape(t);
        if (!CssLexer.isNmStart(identifier.charAt(1))) {
          selectorPart = null;
        } else {
          selectorPart = new CssTree.IdLiteral(t.pos, identifier);
          tq.advance();
        }
      } else if (".".equals(t.text)) {
        selectorPart = parseClass();
      } else if ("[".equals(t.text)) {
        selectorPart = parseAttrib();
      } else if (":".equals(t.text)) {
        selectorPart = parsePseudo();
      } else {
        break;
      }
      if (selectorPart == null) { return null; }
      elements.add(selectorPart);
    }
    if (elements.isEmpty()) {
      throwOrReportExpectedToken("<Selector>");
      return null;
    }
    return new CssTree.SimpleSelector(pos(m), elements);
  }

  private CssTree.ClassLiteral parseClass() throws ParseException {
    Mark m = tq.mark();
    tq.expectToken(".");
    String ident = expectIdent();
    if (ident == null) { return null; }
    return new CssTree.ClassLiteral(pos(m), "." + ident);
  }

  private CssTree.Attrib parseAttrib() throws ParseException {
    Mark m = tq.mark();
    tq.expectToken("[");
    String ident = expectIdent();
    if (ident == null) { return null; }
    CssTree.AttribOperation op = null;
    if (isTolerant && tq.isEmpty()) { return null; }
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
      if (isTolerant && tq.isEmpty()) { return null; }
      t = tq.peek();
      if (CssTokenType.STRING == t.type) {
        String s = unescape(t);
        s = s.substring(1, s.length() - 1);
        value = new CssTree.StringLiteral(t.pos, s);
        tq.advance();
      } else {
        String valuePattern = expectIdent();
        if (valuePattern == null) { return null; }
        value = new CssTree.IdentLiteral(t.pos, valuePattern);
      }
    }

    if (expect("]", DO_NOTHING, m)) { return null; }
    return new CssTree.Attrib(pos(m), ident, op, value);
  }

  private CssTree.Pseudo parsePseudo() throws ParseException {
    Mark m = tq.mark();
    tq.expectToken(":");
    Mark m2 = tq.mark();
    if (isTolerant && tq.isEmpty()) { return null; }
    Token<CssTokenType> t = tq.peek();
    CssTree.CssExprAtom atom;
    if (CssTokenType.FUNCTION == t.type) {
      String fnName = unescape(t);
      fnName = fnName.substring(0, fnName.length() - 1); // strip trailing (
      tq.advance();
      Mark m3 = tq.mark();
      String argIdent = expectIdent();
      if (argIdent == null) { return null; }
      FilePosition pos3 = pos(m3);
      CssTree.IdentLiteral lit = new CssTree.IdentLiteral(pos3, argIdent);
      CssTree.Term term = new CssTree.Term(pos3, null, lit);
      CssTree.Expr arg =
        new CssTree.Expr(pos3, Collections.singletonList(term));
      if (expect(")", DO_NOTHING, m)) { return null; }
      atom = new CssTree.FunctionCall(pos(m2), Name.css(fnName), arg);
    } else {
      String ident = expectIdent();
      if (ident == null) { return null; }
      atom = new CssTree.IdentLiteral(pos(m2), ident);
    }
    return new CssTree.Pseudo(pos(m), atom);
  }

  private CssTree.Declaration parseDeclaration() throws ParseException {
    Mark m = tq.mark();
    if (tq.lookaheadToken("}") || tq.lookaheadToken(";")) {
      return new CssTree.EmptyDeclaration(pos(m));
    }
    boolean hasStarHack = tq.checkToken("*");
    Mark declStart = tq.mark();
    CssTree.Property property = parseProperty();
    if (property == null) {
      SKIP_TO_CHUNK_END_FROM_WITHIN_BLOCK.recover(this, m);
      return null;
    }
    List<CssTree> children = Lists.newArrayList(3);
    children.add(property);
    if (expect(":", SKIP_TO_CHUNK_END_FROM_WITHIN_BLOCK, m)) {
      return null;
    }
    CssTree.Expr expr = parseExpr();
    if (expr == null) {
      SKIP_TO_CHUNK_END_FROM_WITHIN_BLOCK.recover(this, m);
      return null;
    }
    children.add(expr);
    if (!(tq.isEmpty() || tq.lookaheadToken("}") || tq.lookaheadToken(";"))) {
      CssTree.Prio prio = parsePrio();
      if (prio == null) {
        SKIP_TO_CHUNK_END_FROM_WITHIN_BLOCK.recover(this, m);
        return null;
      }
      children.add(prio);
    }
    CssTree.PropertyDeclaration d = new CssTree.PropertyDeclaration(
        pos(declStart), children);
    if (hasStarHack) {
      return new CssTree.UserAgentHack(
          pos(m), CssTree.UserAgent.ie7OrOlder(),
          Collections.singletonList(d));
    } else {
      return d;
    }
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
    return throwOrReport(
        MessageType.EXPECTED_TOKEN, t.pos,
        MessagePart.Factory.valueOf(";"),
        MessagePart.Factory.valueOf(t.text));
  }

  private CssTree.Expr parseExpr() throws ParseException {
    Mark m = tq.mark();
    List<CssTree> children = Lists.newArrayList();
    {
      CssTree.Term term = parseTerm();
      if (term == null) { return null; }
      children.add(term);
    }
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
      children.add(parseOperation());
      CssTree.Term term = parseTerm();
      if (term == null) { return null; }
      children.add(term);
    }
    return new CssTree.Expr(pos(m), children);
  }

  private CssTree.Term parseTerm() throws ParseException {
    Mark m = tq.mark();
    CssTree.UnaryOperator op = parseUnaryOperator();

    if (isTolerant && tq.isEmpty()) { return null; }

    Mark m2 = tq.mark();
    CssTree.CssExprAtom expr;
    Token<CssTokenType> t = tq.peek();
    switch (t.type) {
      case IDENT:
        String ident = unescape(t);
        tq.advance();
        if (PROG_ID_KEYWORD.equals(Name.css(ident)) && tq.checkToken(":")) {
          expr = parseProgId(m2);
        } else {
          expr = new CssTree.IdentLiteral(pos(m2), ident);
        }
        break;
      case FUNCTION:
        tq.advance();
        String fn = unescape(t);
        fn = fn.substring(0, fn.length() - 1);  // strip trailing '('
        CssTree.Expr arg = parseExpr();
        if (arg == null) { return null; }
        if (expect(")", CssParser.DO_NOTHING, m)) { return null; }
        expr = new CssTree.FunctionCall(pos(m2), Name.css(fn), arg);
        break;
      default:
        expr = parseLiteral();
        break;
    }
    if (expr == null) { return null; }

    return new CssTree.Term(pos(m), op, expr);
  }

  private CssTree.UnaryOperator parseUnaryOperator() throws ParseException {
    CssTree.UnaryOperator op = null;
    if (!tq.isEmpty()) {
      Token<CssTokenType> t = tq.peek();
      if (CssTokenType.PUNCTUATION == t.type) {
        if ("+".equals(t.text)) {
          op = CssTree.UnaryOperator.IDENTITY;
          tq.advance();
        } else if ("-".equals(t.text)) {
          op = CssTree.UnaryOperator.NEGATION;
          tq.advance();
        }
      }
    }
    return op;
  }

  private CssTree.CssLiteral parseLiteral() throws ParseException {
    Mark m = tq.mark();
    Token<CssTokenType> t = tq.pop();
    switch (t.type) {
      case QUANTITY:
        try {
          return new CssTree.QuantityLiteral(pos(m), unescape(t));
        } catch (IllegalArgumentException e) {
          try {
            // Try adding a hash to get a HashLiteral.
            // In case of color literals, #s are missed sometimes.
            return new CssTree.HashLiteral(pos(m), "#" + unescape(t));
          } catch (IllegalArgumentException e2) {
            return throwOrReport(
                MessageType.UNEXPECTED_TOKEN, t.pos,
                MessagePart.Factory.valueOf(t.text));
          }
        }
      case STRING:
      {
        String value = unescape(t);
        value = value.substring(1, value.length() - 1);
        return new CssTree.StringLiteral(pos(m), value);
      }
      case HASH:
        String color = unescape(t);
        // Require that it have 3 or 6 digits
        if (color.length() != 4 && color.length() != 7) {
          return throwOrReport(
              MessageType.UNEXPECTED_TOKEN, t.pos,
              MessagePart.Factory.valueOf(t.text));
        }
        try {
          return new CssTree.HashLiteral(pos(m), color);
        } catch (IllegalArgumentException e) {
          return throwOrReport(
              MessageType.UNEXPECTED_TOKEN, t.pos,
              MessagePart.Factory.valueOf(t.text));
        }
      case UNICODE_RANGE:
        return new CssTree.UnicodeRangeLiteral(pos(m), unescape(t));
      case URI:
        tq.rewind(m);
        return parseUri();
      case SUBSTITUTION:
        return new CssTree.Substitution(t.pos, t.text);
      case IDENT:
        return new CssTree.IdentLiteral(t.pos, unescape(t));
      default:
        tq.rewind(m);
        return throwOrReport(
            MessageType.UNEXPECTED_TOKEN, t.pos,
            MessagePart.Factory.valueOf(t.text));
    }
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
          return throwOrReport(
              MessageType.EXPECTED_TOKEN, t.pos,
              MessagePart.Factory.valueOf("<URI>"),
              MessagePart.Factory.valueOf(t.text));
      }
      return new CssTree.UriLiteral(pos(m), new URI(uriStr));
    } catch (URISyntaxException ex) {
      return throwOrReport(
          MessageType.MALFORMED_URI, t.pos,
          MessagePart.Factory.valueOf(t.text));
    }
  }

  private static final Name PROG_ID_KEYWORD = Name.css("progid");
  private CssTree.ProgId parseProgId(Mark start) throws ParseException {
    StringBuilder sb = new StringBuilder();
    nameLoop:
    while (true) {
      if (tq.isEmpty()) { return null; }
      Token<CssTokenType> t = tq.pop();
      switch (t.type) {
        case FUNCTION:
          sb.append(unescape(t.text.substring(0, t.text.length() - 1), true));
          break nameLoop;
        case IDENT:
          sb.append(unescape(t));
          break;
        default: return null;
      }
      if (!tq.checkToken(".")) { return null; }
      sb.append('.');
    }
    List<ProgIdAttribute> attrs = Lists.newArrayList();
    if (!tq.checkToken(")")) {
      do {
        CssTree.ProgIdAttribute attr = parseProgIdAttribute();
        if (attr == null) { return null; }
        attrs.add(attr);
      } while (tq.checkToken(","));
      if (!tq.checkToken(")")) { return null; }
    }
    return new CssTree.ProgId(pos(start), Name.css(sb.toString()), attrs);
  }

  private CssTree.ProgIdAttribute parseProgIdAttribute() throws ParseException {
    Mark attrStart = tq.mark();
    String name = expectIdent();
    if (name == null) { return null; }
    if (!tq.checkToken("=")) { return null; }
    Mark valueStart = tq.mark();
    CssTree.UnaryOperator op = parseUnaryOperator();
    if (tq.isEmpty()) { return null; }
    Token<CssTokenType> t = tq.peek();
    CssTree.CssLiteral lit = null;
    if (t.type == CssTokenType.HASH) {
      String color = unescape(t);
      if (9 == color.length()) {
        tq.advance();
        // IE Filters use #RRGGBBAA style color literals which are different
        // from those used by the rest of the CSS grammar.
        try {
          lit = new CssTree.HashLiteral(t.pos, color);
        } catch (IllegalArgumentException e) {
          return throwOrReport(
              MessageType.UNEXPECTED_TOKEN, t.pos,
              MessagePart.Factory.valueOf(t.text));
        }
      } else {
        lit = parseLiteral();
      }
    } else {
      lit = parseLiteral();
    }
    if (lit == null) { return null; }
    return new CssTree.ProgIdAttribute(
        pos(attrStart), Name.css(name),
        Collections.singletonList(new CssTree.Term(pos(valueStart), op, lit)));
  }


  private FilePosition pos(Mark m) throws ParseException {
    // Example input "import 'foo.css'" without a semicolon.
    FilePosition start = m.getFilePosition(),
        end = tq.lastPosition();
    return ((tq.isEmpty() || tq.currentPosition() != start)
            && start.source().equals(end.source())
            && start.endCharInFile() <= end.endCharInFile())
         ? FilePosition.span(start, tq.lastPosition())
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
    Token<CssTokenType> t = null;
    if (!tq.isEmpty()) {
      t = tq.peek();
      if (CssTokenType.IDENT == t.type) {
        tq.advance();
        return unescape(t);
      }
    }
    throwOrReportExpectedToken("<Identifier>");
    return null;
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
   * From the parser grammar:<pre>
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

  /** Iff item is not null, add it to coll. */
  private static <T> void addIfNotNull(Collection<? super T> coll, T item) {
    if (item != null) { coll.add(item); }
  }

  /**
   * Like {@link TokenQueue#expectToken(String)} but reports a message in
   * {@link #isTolerant() tolerant} mode.
   * <p>
   * The return convention is such that this method should be used in a
   * condition like {@code if (expect(...)) return null;} to abort parsing.
   *
   * @param token the token that is expected to be next.
   * @param rs the action to take if token is not the next token.
   * @param start the start of the current construct whose parsing will fail
   *     if token is not the next token.
   * @return true if parsing failed.
   * @throws ParseException if not in tolerant mode and token is not the next
   *     token, or if there is a problem lexing.
   */
  private boolean expect(String token, RecoveryStrategy rs, Mark start)
      throws ParseException {
    if (tq.checkToken(token)) { return false; }
    throwOrReportExpectedToken(token);
    rs.recover(this, start);
    return true;
  }

  /**
   * After a parse failure, consumes tokens from the stream until we are at a
   * likely good position.
   */
  private static abstract class RecoveryStrategy {
    /**
     * @param p a parser with a token queue positioned after a parse failure.
     * @param start the position of the start of the malformed construct.
     *     This is used to report which tokens are being discarded without
     *     producing content.
     */
    abstract void recover(CssParser p, Mark start) throws ParseException;
  }

  private static final RecoveryStrategy DO_NOTHING = new RecoveryStrategy() {
    @Override
    void recover(CssParser p, Mark start) { /* noop */ }
  };

  /**
   * Skips to the next item in the comma list, or the end of the comma list.
   * The next item in the comma list is assumed to start with a comma,
   * and the comma list is assumed to be ended by a "{", ";", or "}" token,
   * or a symbol.
   */
  private static final RecoveryStrategy SKIP_COMMA_LIST_ITEM
      = new RecoveryStrategy() {
    @Override
    void recover(CssParser p, Mark start) throws ParseException {
      if (start == null) { start = p.tq.mark(); }
      while (!p.tq.isEmpty()) {
        Token<CssTokenType> t = p.tq.peek();
        if (t.type == CssTokenType.PUNCTUATION) {
          if (t.text.length() == 1 && ",{};".contains(t.text)) {
            break;
          }
        } else if (t.type == CssTokenType.SYMBOL) {
          break;
        }
        p.tq.advance();
      }
      p.reportSkipping(p.pos(start));
    }
  };

  /**
   * Skips to the end of a chunk within a block.
   * A chunk is assumed to end with a semicolon or a close curly bracket, but
   * may contain balanced groups of curly brackets.
   * A symbol always ends a chunk.
   * The close curly bracket is not consumed since it is not considered part
   * of a chunk within a block -- it is part of the containing block.
   */
  private static final RecoveryStrategy SKIP_TO_CHUNK_END_FROM_WITHIN_BLOCK
      = new RecoveryStrategy() {
    @Override
    void recover(CssParser p, Mark start) throws ParseException {
      if (start == null) { start = p.tq.mark(); }
      int depth = 1;
      while (!p.tq.isEmpty()) {
        Token<CssTokenType> t = p.tq.peek();
        if (t.type == CssTokenType.PUNCTUATION) {
          if (";".equals(t.text) && (depth <= 1)) {
            break;
          } else if ("}".equals(t.text)) {
            if (--depth <= 0) { break; }
          } else if ("{".equals(t.text)) {
            ++depth;
          }
        } else if (t.type == CssTokenType.SYMBOL) {
          break;
        }
        p.tq.advance();
      }
      p.reportSkipping(p.pos(start));
    }
  };

  /**
   * Skips to the end of a chunk outside a block.  Outside a block, a semicolon
   * is treated as a chunk terminator instead of as a separator, and the
   * final curly bracket is assumed to be part of the chunk and so is included
   * in the end.
   */
  private static final RecoveryStrategy SKIP_TO_CHUNK_END_FROM_OUTSIDE_BLOCK
      = new RecoveryStrategy() {
    @Override
    void recover(CssParser p, Mark start) throws ParseException {
      if (start == null) { start = p.tq.mark(); }
      int depth = 0;
      while (!p.tq.isEmpty()) {
        Token<CssTokenType> t = p.tq.peek();
        if (t.type == CssTokenType.PUNCTUATION) {
          if (";".equals(t.text) && (depth <= 0)) {
            p.tq.advance();
            break;
          } else if ("}".equals(t.text)) {
            if (--depth <= 0) {
              p.tq.advance();
              break;
            }
          } else if ("{".equals(t.text)) {
            ++depth;
          }
        } else if (t.type == CssTokenType.SYMBOL) {
          break;
        }
        p.tq.advance();
      }
      p.reportSkipping(p.pos(start));
    }
  };

  /**
   * Adds a message to the message queue in {@link #isTolerant() tolerant} mode
   * or throws a {@link ParseException} otherwise.
   * @param token the token that was expected
   */
  private void throwOrReportExpectedToken(String token) throws ParseException {
    String actual;
    FilePosition pos;
    if (tq.isEmpty()) {
      actual = "<EOF>";
      pos = FilePosition.endOf(tq.lastPosition());
    } else {
      actual = tq.peek().text;
      pos = tq.currentPosition();
    }
    throwOrReport(
        MessageType.EXPECTED_TOKEN, pos,
        MessagePart.Factory.valueOf(token),
        MessagePart.Factory.valueOf(actual));
  }

  /**
   * Called from error recovery routines to indicate that tokens were skipped.
   */
  private void reportSkipping(FilePosition skipped) {
    if (skipped.length() != 0) {
      mq.addMessage(MessageType.SKIPPING, tolerance, skipped);
    }
  }

  /**
   * Constructs a message and either throws or reports it depending on whether
   * the parser is in {@link #isTolerant() tolerant} mode.
   * @return null which is the convention to indicate that parsing failed
   *     because of a malformed construct.
   * @param <T> this always returns null, and the type parameter allows us
   *     to return the right kind of null.
   */
  private <T> T throwOrReport(MessageTypeInt t, MessagePart... parts)
      throws ParseException {
    Message msg = new Message(t, tolerance, parts);
    if (isTolerant) {
      mq.getMessages().add(msg);
      return null;
    } else {
      throw new ParseException(msg);
    }
  }
}
