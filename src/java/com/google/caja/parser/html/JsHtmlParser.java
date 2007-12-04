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

package com.google.caja.parser.html;

import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.HtmlTokenType;
import com.google.caja.lexer.JsLexer;
import com.google.caja.lexer.JsTokenQueue;
import com.google.caja.lexer.ParseException;
import com.google.caja.lexer.Token;
import com.google.caja.lexer.TokenQueue;
import com.google.caja.lexer.TokenQueue.Mark;
import com.google.caja.parser.AbstractParseTreeNode;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.Visitor;
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.BooleanLiteral;
import com.google.caja.parser.js.FormalParam;
import com.google.caja.parser.js.FunctionConstructor;
import com.google.caja.parser.js.FunctionDeclaration;
import com.google.caja.parser.js.Parser;
import com.google.caja.parser.js.ReturnStmt;
import com.google.caja.parser.js.Statement;
import com.google.caja.reporting.MessageQueue;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * A parser that extracts chunks of javascript source from html and builds
 * a parse tree of sorts.
 *
 * @author mikesamuel@gmail.com
 */
public final class JsHtmlParser  {
  private final TokenQueue<HtmlTokenType> tq;
  private final MessageQueue mq;
  private String lastOpenHtmlTag;
  private String lastOpenTag;
  private String lastAttribName;
  private String lastGxpAttribName;
  private List<Token<HtmlTokenType>> pendingScripts =
      new ArrayList<Token<HtmlTokenType>>();

  public JsHtmlParser(TokenQueue<HtmlTokenType> tq, MessageQueue mq) {
    this.tq = tq;
    this.mq = mq;
  }

  public Block parse() throws ParseException {
    List<Statement> stmts = new ArrayList<Statement>();

    // getting the mark fails for an empty queue, so special case this
    if (tq.isEmpty()) {
      Block b = new Block(Collections.<Statement>emptyList());
      FilePosition pos = tq.getInputRange();
      if (null == pos) {
        pos = FilePosition.startOfFile(tq.getInputSource());
      }
      b.setFilePosition(pos);
      return b;
    }

    Mark m = tq.mark();
    do {
      Token<HtmlTokenType> t = tq.pop();
      switch (t.type) {
        case TAGBEGIN:
          String tagName = tagName(t);
          boolean isOpen = isOpen(t);
          boolean isHtml = isHtml(t);
          if (isOpen) {
            if (isHtml) { lastOpenHtmlTag = tagName; }
            lastOpenTag = tagName;
            } else {
            if (isHtml) {
              lastOpenHtmlTag = null;
              if (!pendingScripts.isEmpty()) {
                stmts.add(parseScriptTag(false));
              }
            } else if ("gxp:attr".equals(tagName)) {
              lastGxpAttribName = null;
              if (!pendingScripts.isEmpty()) {
                stmts.add(parseScriptTag(true));
              }
            }
            lastOpenTag = null;
          }
          break;
        case TAGEND:
            if (t.text.endsWith("/>")) {
            if (lastOpenTag.equals(lastOpenHtmlTag)) { lastOpenHtmlTag = null; }
            lastOpenTag = null;
          }
          break;
        case ATTRNAME:
          lastAttribName = t.text;
          break;
        case ATTRVALUE:
          if (lastAttribName.startsWith("on")
              // gxp support
              || lastAttribName.startsWith("expr:on")) {
            stmts.add(parseHandlerFunction(t));
          } else if ("a".equalsIgnoreCase(lastOpenHtmlTag)
                     && "href".equalsIgnoreCase(lastAttribName)
                     && isJavascriptUrl(t.text)) {
            stmts.add(parseHandlerFunction(t));
          }
          if ("name".equals(lastAttribName) && "gxp:attr".equals(lastOpenTag)) {
            lastGxpAttribName = attributeValue(t.text);
          }
          lastAttribName = null;
          break;
        case CDATA: case TEXT: case UNESCAPED:
          if (inScript()) {
            pendingScripts.add(t);
          }
          break;
        case QSTRING:    case COMMENT:
        case DIRECTIVE:  case IGNORABLE:
        case SERVERCODE:
          break;
      }
    } while (!tq.isEmpty());
    return finish(new Block(stmts), m);
  }

  public TokenQueue<HtmlTokenType> getTokenQueue() { return tq; }

  private <T extends ParseTreeNode> T finish(T node, Mark m)
  throws ParseException {
    FilePosition start = m.getFilePosition();
    FilePosition end = tq.lastPosition();
    ((AbstractParseTreeNode) node).setFilePosition(
        FilePosition.span(start, end));
    return node;
  }

  private static final Pattern COMMENT_MATCHER =
    Pattern.compile("^(\\s*)<!--(.*)-->(\\s*)$", Pattern.DOTALL);
  private static final Pattern CDATA_MATCHER =
    Pattern.compile("^(\\s*)<!\\[CDATA\\[(.*)\\]\\]>(\\s*)$", Pattern.DOTALL);
  private static final Pattern JAVASCRIPT_URL_MATCHER = Pattern.compile(
      "^(\\s+)javascript:(.*)", Pattern.DOTALL);

  private Statement parseScriptTag(boolean isAttribute) throws ParseException {
    CharProducer cp;
    {
      List<CharProducer> prods = new ArrayList<CharProducer>();
      for (Token<HtmlTokenType> t : this.pendingScripts) {
        String source = t.text;

        if (!isAttribute) {
          source = (COMMENT_MATCHER.matcher(source)
                    //               <!--  -->
                    .replaceFirst("$1    $2   $3"));
          source = (CDATA_MATCHER.matcher(source)
                    //               <![CDATA[  ]]>
                    .replaceFirst("$1         $2   $3"));
        }
        CharProducer p =
          CharProducer.Factory.create(new StringReader(source), t.pos);
        if (HtmlTokenType.UNESCAPED != t.type
            && HtmlTokenType.CDATA != t.type) {
          p = CharProducer.Factory.fromHtmlAttribute(p);
        }
        prods.add(p);
      }
      if (1 == prods.size()) {
        cp = prods.get(0);
      } else {
        CharProducer[] prodArr = prods.toArray(new CharProducer[0]);
        cp = CharProducer.Factory.chain(prodArr);
      }
    }
    Parser p = new Parser(new JsTokenQueue(
        new JsLexer(cp), tq.getInputSource()), mq);
    p.setRecoverFromFailure(true);
    FilePosition range = FilePosition.span(
        this.pendingScripts.get(0).pos,
        this.pendingScripts.get(this.pendingScripts.size() - 1).pos);
    p.getTokenQueue().setInputRange(range);
    Statement stmt;
    {
      Block body = p.parse();
      if (isAttribute) {
        FilePosition pos = FilePosition.span(
            pendingScripts.get(0).pos,
            pendingScripts.get(pendingScripts.size() - 1).pos);
        stmt = wrapHandler(body, pos);
      } else {
        stmt = body;
      }
    }
    p.getTokenQueue().expectEmpty();
    this.pendingScripts.clear();
    return stmt;
  }

  private static String dequoteAttribute(String text) {
    int len = text.length();
    if (len < 2) { return text; }
    char ch0 = text.charAt(0);
    if ((ch0 == '"' || ch0 == '\'') && ch0 == text.charAt(len - 1)) {
      return " " + text.substring(1, len - 1) + " ";
    }
    return text;
  }

  private static String attributeValue(String text) {
    int len = text.length();
    if (len < 2) { return text; }
    char ch0 = text.charAt(0);
    if ((ch0 == '"' || ch0 == '\'') && ch0 == text.charAt(len - 1)) {
      return text.substring(1, len - 1);
    }
    // TODO(msamuel): unescape attribute value
    return text;
  }

  private static boolean isJavascriptUrl(String text) {
    return JAVASCRIPT_URL_MATCHER.matcher(dequoteAttribute(text)).find();
  }

  private Statement parseHandlerFunction(Token<HtmlTokenType> t)
      throws ParseException {
    // parse the function body and wrap it in a function constructor of the form
    // /*!boolean::HTMLElement*/ function (/*optional not_ie Event*/ event) {
    //   body here
    //   [return true;]
    // }

    String source = dequoteAttribute(t.text);
    source = (JAVASCRIPT_URL_MATCHER.matcher(source)
              //               javascript:
              .replaceFirst("$1           $2"));
    CharProducer cp = CharProducer.Factory.fromHtmlAttribute(
        CharProducer.Factory.create(new StringReader(source), t.pos));
    Parser p = new Parser(new JsTokenQueue(
                              new JsLexer(cp), tq.getInputSource()), mq);
    p.setRecoverFromFailure(true);
    p.getTokenQueue().setInputRange(t.pos);
    Block body = p.parse();
    p.getTokenQueue().expectEmpty();

    return wrapHandler(body, t.pos);
  }

  private FunctionDeclaration wrapHandler(Block body, FilePosition pos) {
    // if the body doesn't have a return statement, add one
    boolean hasReturnStatement = !body.acceptPostOrder(new Visitor() {
        public boolean visit(ParseTreeNode node) {
          // false will bubble up
          return !(node instanceof ReturnStmt);
        }
      });
    if (!hasReturnStatement) {
      FilePosition end = FilePosition.endOf(pos);

      BooleanLiteral returnValue = new BooleanLiteral(true);
      returnValue.setFilePosition(end);
      ReturnStmt returnStmt = new ReturnStmt(returnValue);
      returnStmt.setFilePosition(end);

      List<Statement> stmts = new ArrayList<Statement>();
      stmts.add(body);
      stmts.add(returnStmt);

      Block block = new Block(stmts);
      block.setFilePosition(pos);
      body = block;
    }

    FilePosition impliedNodePosition = FilePosition.startOf(pos);

    FormalParam eventParam = new FormalParam("event");
    eventParam.setFilePosition(impliedNodePosition);

    // create the function
    FunctionConstructor fc = new FunctionConstructor(
        null, Collections.<FormalParam>singletonList(eventParam), body);
    fc.setFilePosition(impliedNodePosition);

    // wrap the function in a declaration with an auto-generated name
    String name = createName(body.getFilePosition());
    FunctionDeclaration fd = new FunctionDeclaration(name, fc);
    fd.setFilePosition(impliedNodePosition);

    return fd;
  }

  private static final Pattern JS_IDENT_SAFE =
    Pattern.compile("[^a-zA-Z0-9_$]");
  /**
   * generate an identifier for an entity at the given position.
   * This doesn't guarantee uniqueness, but is somewhat readable, and should be
   * good enough.
   */
  private String createName(FilePosition pos) {
    StringBuilder sb = new StringBuilder();
    sb.append("_");
    sb.append(pos.source().toString());
    sb.append("___ln_");
    sb.append(pos.startLineNo());
    sb.append("__ch_");
    sb.append(pos.startCharInLine());
    return JS_IDENT_SAFE.matcher(sb).replaceAll("_");
  }

  /** The name for an html TAGBEGIN token. */
  private String tagName(Token<HtmlTokenType> t) {
    assert HtmlTokenType.TAGBEGIN == t.type;
    return t.text.substring(('/' == t.text.charAt(1)) ? 2 : 1).toLowerCase();
  }
  /** Is it an open tag (&lt;b&gt;) as opposed to a close tag (&lt;/b&gt;). */
  private boolean isOpen(Token<HtmlTokenType> t) {
    assert HtmlTokenType.TAGBEGIN == t.type;
    return '/' != t.text.charAt(1);
  }

  private boolean isHtml(Token<HtmlTokenType> t) {
    assert HtmlTokenType.TAGBEGIN == t.type;
    // all the gxp:* and call:* tags have a colon separating the namespace from
    // the unqualified name.  Assumes html tags are in the default namespace.
    return t.text.indexOf(':') < 0;
  }

  private boolean inScript() {
    return "script".equals(this.lastOpenHtmlTag)
      || (null != lastGxpAttribName && lastGxpAttribName.startsWith("on"));
  }
}
