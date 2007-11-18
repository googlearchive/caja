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

package com.google.caja.plugin;

import java.util.List;

import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.JsLexer;
import com.google.caja.lexer.JsTokenQueue;
import com.google.caja.lexer.ParseException;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.Expression;
import com.google.caja.parser.js.ExpressionStmt;
import com.google.caja.parser.js.Operation;
import com.google.caja.parser.js.Operator;
import com.google.caja.parser.js.Parser;
import com.google.caja.parser.js.Reference;
import com.google.caja.parser.js.Statement;
import com.google.caja.parser.js.StringLiteral;
import com.google.caja.parser.js.UndefinedLiteral;
import com.google.caja.reporting.MessageQueue;

/**
 * Functions for composing javascript template functions.
 */
final class JsWriter {

  /**
   * Appends a statement like tgt.push(toAppend) to the given block, but
   * makes sure to flatten with any like operation that would immediately
   * precede it.
   *
   * @param toAppend an expression taht should evaluate to a string
   * @param tgt the name of an Array to append to, like "out___"
   * @param b a block to append the a statement whose side effect is to
   *   append the result of evaluating toAppend to the list described by tgt.
   */
  static void append(Expression toAppend, String tgt, Block b) {
    Operation fnCall = null;
    // Look for an existing call to push
    List<? extends Statement> children = b.children();
    if (!children.isEmpty()) {
      Statement last = children.get(children.size() - 1);
      if (last instanceof ExpressionStmt) {
        Expression e = ((ExpressionStmt) last).getExpression();
        if (e instanceof Operation
            && Operator.FUNCTION_CALL == ((Operation) e).getOperator()) {
          Expression fn = ((Operation) e).children().get(0);
          if (fn instanceof Operation) {
            Operation fnOp = (Operation) fn;
            if (Operator.MEMBER_ACCESS == fnOp.getOperator()
                && matchesReference(fnOp.children().get(0), tgt)
                && matchesReference(fnOp.children().get(1), "push")) {
              fnCall = (Operation) e;
            }
          }
        }
      }
    }
    // Make one
    if (null == fnCall) {
      fnCall = s(new Operation(Operator.FUNCTION_CALL,
                               s(new Operation(Operator.MEMBER_ACCESS,
                                               s(new Reference(tgt)),
                                               s(new Reference("push"))))
                               )
                 );
      b.insertBefore(s(new ExpressionStmt(fnCall)), null);
    }
    if (toAppend instanceof StringLiteral) {
      // If toAppend and last child are both string literals, combine them
      List<? extends Expression> fnCallOperands = fnCall.children();
      if (fnCallOperands.size() > 1) {
        Expression last = fnCallOperands.get(fnCallOperands.size() - 1);
        if (last instanceof StringLiteral) {
          StringLiteral concatenation =
            s(new StringLiteral(
                  StringLiteral.toQuotedValue(
                      ((StringLiteral) last).getUnquotedValue()
                      + ((StringLiteral) toAppend).getUnquotedValue()
                      )));
          fnCall.replaceChild(concatenation, last);
          return;
        }
      }
    }
    fnCall.insertBefore(toAppend, null);
  }

  static void appendString(String string, String tgt, Block b) {
    append(s(new StringLiteral(StringLiteral.toQuotedValue(string))), tgt, b);
  }

  static void appendText(String text, Esc escaping, String tgt, Block b) {
    if ("".equals(text)) { return; }
    JsWriter.append(
        s(new StringLiteral(
              StringLiteral.toQuotedValue(
                  escaping != JsWriter.Esc.NONE ? htmlEscape(text) : text))),
                  tgt, b);
  }

  private static boolean matchesReference(Expression e, String label) {
    return e instanceof Reference
        && label.equals(((Reference) e).getIdentifier());
  }

  static String htmlEscape(String text) {
    StringBuilder out = null;
    int pos = 0;
    int len = text.length();
    for (int i = 0; i < len; ++i) {
      char ch = text.charAt(i);
      switch (ch) {
        case '&':
          if (out == null) { out = new StringBuilder(len * 2); }
          out.append(text, pos, i).append("&amp;");
          pos = i + 1;
          break;
        case '<':
          if (out == null) { out = new StringBuilder(len * 2); }
          out.append(text, pos, i).append("&lt;");
          pos = i + 1;
          break;
        case '>':
          if (out == null) { out = new StringBuilder(len * 2); }
          out.append(text, pos, i).append("&gt;");
          pos = i + 1;
          break;
        case '"':
          if (out == null) { out = new StringBuilder(len * 2); }
          out.append(text, pos, i).append("&quot;");
          pos = i + 1;
          break;
        case '\r': case '\n': case '\t': break;
        default:
          if (ch < 0x20) {
            if (out == null) { out = new StringBuilder(len * 2); }
            out.append(text, pos, i).append("&#").append((int) ch).append(';');
            pos = i + 1;
          }
          break;
      }
    }
    return out != null ? out.append(text, pos, len).toString() : text;
  }

  static Expression asExpression(
      CharProducer cp, FilePosition pos, MessageQueue mq) {
    // Parse as a javascript expression.
    JsLexer lexer = new JsLexer(cp);
    JsTokenQueue tq = new JsTokenQueue(lexer, pos.source());
    Parser p = new Parser(tq, mq);
    Expression e;
    try {
      e = p.parseExpression(true);
      p.getTokenQueue().expectEmpty();
    } catch (ParseException ex) {
      ex.toMessageQueue(mq);
      e = null;
    }

    if (null == e) {
      UndefinedLiteral ul = new UndefinedLiteral();
      ul.setFilePosition(pos);
      return ul;
    }

    // Wrap in an ExpressionStmt so the expression is guaranteed to have a
    // proper parent
    ExpressionStmt stmt = new ExpressionStmt(e);
    stmt.parentify();

    // Expression will be sanitized in a later pass

    e = stmt.getExpression();  // Refetch e in case it was rewritten
    stmt.replaceChild(new UndefinedLiteral(), e);  // Make e not a child of stmt
    return e;
  }


  static enum Esc {
    HTML,
    HTML_ATTRIB,
    NONE,
    ;
  }

  /** Make the given parse tree node synthetic. */
  private static <T extends ParseTreeNode> T s(T t) {
    t.getAttributes().set(ExpressionSanitizer.SYNTHETIC, Boolean.TRUE);
    return t;
  }
}
