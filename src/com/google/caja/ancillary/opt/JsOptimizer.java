// Copyright (C) 2009 Google Inc.
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

package com.google.caja.ancillary.opt;

import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.JsLexer;
import com.google.caja.lexer.JsTokenQueue;
import com.google.caja.lexer.ParseException;
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.Expression;
import com.google.caja.parser.js.Literal;
import com.google.caja.parser.js.ObjectConstructor;
import com.google.caja.parser.js.Parser;
import com.google.caja.parser.js.Statement;
import com.google.caja.parser.js.StringLiteral;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.SimpleMessageQueue;
import com.google.caja.util.Lists;

import java.util.List;

/**
 * Optimizes JavaScript code.
 *
 * @author mikesamuel@gmail.com
 */
public class JsOptimizer {
  private final List<Statement> compUnits = Lists.newArrayList();
  private ParseTreeKB optimizer;
  private boolean rename;
  private final MessageQueue mq;

  public JsOptimizer(MessageQueue mq) { this.mq = mq; }

  /**
   * Register an input for optimization.
   */
  public JsOptimizer addInput(Statement stmt) {
    compUnits.add(stmt);
    return this;
  }

  /**
   * Sets the environment file to use during optimization.
   * The environment file contains facts about the environment in which the
   * optimized output is expected to run, so allows for interpreter-specific
   * optimization.
   */
  public JsOptimizer setEnvJson(ObjectConstructor envJson) {
    if (optimizer == null) { optimizer = new ParseTreeKB(); }
    List<? extends Expression> parts = envJson.children();
    for (int i = 0, n = parts.size(); i < n; i += 2) {
      StringLiteral sl = (StringLiteral) parts.get(i);
      Literal value = (Literal) parts.get(i + 1).fold();  // fold negative nums
      String rawExpr = sl.getValue();
      rawExpr = " " + rawExpr.substring(1, rawExpr.length() - 1) + " ";
      CharProducer valueCp = CharProducer.Factory.fromJsString(
          CharProducer.Factory.fromString(rawExpr, sl.getFilePosition()));
      try {
        Expression expr = parser(valueCp, mq).parseExpression(true);
        optimizer.addFact(expr, Fact.is(value));
      } catch (ParseException ex) {
        ex.toMessageQueue(mq);
      }
    }
    return this;
  }

  /**
   * Sets a flag telling the optimizer whether to rename local variables where
   * doing so would not change semantics on an interpreter that does not allow
   * aliasing of {@code eval}.
   */
  public JsOptimizer setRename(boolean rename) {
    this.rename = rename;
    return this;
  }

  /**
   * Returns an optimized version of the concatenation of the programs
   * registered via {@link #addInput}.
   */
  public Statement optimize() {
    // TODO(mikesamuel): use the message queue passed to the ctor once Scope no
    // longer complains about cajita.js defining cajita.
    SimpleMessageQueue optMq = new SimpleMessageQueue();
    Block block = new Block(FilePosition.UNKNOWN, compUnits);
    // Do first since this improves the performance of the ConstVarInliner.
    VarCollector.optimize(block);
    if (optimizer != null) {
      block = optimizer.optimize(block, optMq);
    }
    if (rename) {
      block = ConstantPooler.optimize(block);
      block = new LocalVarRenamer(optMq).optimize(block);
    }
    return (Statement) StatementSimplifier.optimize(block, mq);
  }

  private static Parser parser(CharProducer cp, MessageQueue errs) {
    JsLexer lexer = new JsLexer(cp);
    JsTokenQueue tq = new JsTokenQueue(lexer, cp.getCurrentPosition().source());
    return new Parser(tq, errs);
  }
}
