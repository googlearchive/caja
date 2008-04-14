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

import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.JsLexer;
import com.google.caja.lexer.JsTokenQueue;
import com.google.caja.lexer.ParseException;
import com.google.caja.lexer.escaping.Escaping;
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.Expression;
import com.google.caja.parser.js.ExpressionStmt;
import com.google.caja.parser.js.Identifier;
import com.google.caja.parser.js.Operation;
import com.google.caja.parser.js.Operator;
import com.google.caja.parser.js.Parser;
import com.google.caja.parser.js.Reference;
import com.google.caja.parser.js.Statement;
import com.google.caja.parser.js.StringLiteral;
import com.google.caja.parser.js.UndefinedLiteral;
import com.google.caja.reporting.MessageQueue;
import static com.google.caja.plugin.SyntheticNodes.s;

import java.util.List;

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
   * @param tgtMembers the parts of a dotted name of a function to call, such as
   *   <code>{ "out___", "push" }</code> to append to an Array {@code "out___"}.
   * @param b a block to append the a statement whose side effect is to
   *   append the result of evaluating toAppend to the list described by tgt.
   */
  static void append(Expression toAppend, List<String> tgtMembers, Block b) {
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
          if (matchesChain(fn, tgtMembers)) {
            fnCall = (Operation) e;
          }
        }
      }
    }
    // Make one
    if (null == fnCall) {
      Expression target = s(new Reference(new Identifier(tgtMembers.get(0))));
      for (int i = 1; i < tgtMembers.size(); ++i) {
        target = s(Operation.create(Operator.MEMBER_ACCESS,
                                 target, s(new Reference(new Identifier(tgtMembers.get(i))))));
      }

      fnCall = s(Operation.create(Operator.FUNCTION_CALL,
                               target));
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

  static void appendString(String string, List<String> tgtChain, Block b) {
    JsWriter.append(
        s(new StringLiteral(StringLiteral.toQuotedValue(string))), tgtChain, b);
  }

  static void appendText(
      String text, Esc escaping, List<String> tgtChain, Block b) {
    if ("".equals(text)) { return; }
    JsWriter.append(
        s(new StringLiteral(
              StringLiteral.toQuotedValue(
                  escaping != JsWriter.Esc.NONE ? htmlEscape(text) : text))),
                  tgtChain, b);
  }

  static String htmlEscape(String text) {
    StringBuilder out = new StringBuilder();
    Escaping.escapeXml(text, false, out);
    return out.toString();
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

    // Expression will be sanitized in a later pass

    e = stmt.getExpression();  // Refetch e in case it was rewritten
    stmt.replaceChild(new UndefinedLiteral(), e);  // Make e not a child of stmt
    return e;
  }

  private static boolean matchesChain(Expression e, List<String> refChain) {
    return matchesChain(e, refChain, refChain.size() - 1);
  }

  private static boolean matchesChain(
      Expression e, List<String> refChain, int i) {
    if (0 == i) {
      if (!(e instanceof Reference)) { return false; }
      String ident = ((Reference) e).getIdentifierName();
      return ident.equals(refChain.get(0));
    }
    if (!(e instanceof Operation)) { return false; }
    Operation op = (Operation) e;
    if (Operator.MEMBER_ACCESS != op.getOperator()) { return false; }
    List<? extends Expression> children = op.children();
    Expression lhs = children.get(0);
    Expression rhs = children.get(1);
    if (!(rhs instanceof Reference)) { return false; }
    String ident = ((Reference) rhs).getIdentifierName();
    if (!ident.equals(refChain.get(i))) { return false; }
    return matchesChain(lhs, refChain, i - 1);
  }

  static enum Esc {
    HTML,
    HTML_ATTRIB,
    NONE,
    ;
  }
}
