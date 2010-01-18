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
import com.google.caja.lexer.InputSource;
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
import com.google.caja.render.Concatenator;
import com.google.caja.render.JsMinimalPrinter;
import com.google.caja.reporting.DevNullMessageQueue;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.RenderContext;
import com.google.caja.reporting.SimpleMessageQueue;
import com.google.caja.util.Lists;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
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
      Expression value = parts.get(i + 1).fold();  // fold negative nums
      if (!(value instanceof Literal)) {
        // True for "*useragent*" property inserted by JSKB.
        continue;
      }
      StringLiteral sl = (StringLiteral) parts.get(i);
      String rawExpr = sl.getValue();
      rawExpr = " " + rawExpr.substring(1, rawExpr.length() - 1) + " ";
      CharProducer valueCp = CharProducer.Factory.fromJsString(
          CharProducer.Factory.fromString(rawExpr, sl.getFilePosition()));
      try {
        Expression expr = jsExpr(valueCp, DevNullMessageQueue.singleton());
        optimizer.addFact(expr, Fact.is((Literal) value));
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
      // We pool after the ConstLocalOptimizer invoked by optimizer has run.
      block = ConstantPooler.optimize(block);
      // Now we shorten any long names introduced by the constant pooler.
      block = new LocalVarRenamer(optMq).optimize(block);
    }
    // Finally we rearrange statements and convert conditionals to expressions
    // where it will make things shorter.
    return (Statement) StatementSimplifier.optimize(block, mq);
  }

  public static void main(String... args) throws IOException {
    MessageQueue mq = new SimpleMessageQueue();
    MessageContext mc = new MessageContext();
    JsOptimizer opt = new JsOptimizer(mq);
    opt.setRename(true);
    try {
      for (int i = 0, n = args.length; i < n; ++i) {
        String arg = args[i];
        if ("--norename".equals(arg)) {
          opt.setRename(false);
        } else if (arg.startsWith("--envjson=")) {
          String jsonfile = arg.substring(arg.indexOf('=') + 1);
          opt.setEnvJson((ObjectConstructor) jsExpr(fromFile(jsonfile), mq));
        } else {
          if ("--".equals(arg)) { ++i; }
          for (;i < n; ++i) {
            CharProducer cp = fromFile(args[i]);
            mc.addInputSource(cp.getCurrentPosition().source());
            opt.addInput(js(cp, mq));
          }
        }
      }
    } catch (ParseException ex) {
      ex.toMessageQueue(mq);
    }
    Statement out = opt.optimize();
    for (Message msg : mq.getMessages()) {
      msg.format(mc, System.err);
      System.err.println();
    }
    JsMinimalPrinter printer = new JsMinimalPrinter(
        new Concatenator(System.out, null));
    RenderContext rc = new RenderContext(printer).withRawObjKeys(true);
    if (out instanceof Block) {
      ((Block) out).renderBody(rc);
    } else {
      out.render(rc);
    }
    printer.noMoreTokens();
  }

  private static Block js(CharProducer cp, MessageQueue mq)
      throws ParseException {
    return jsParser(cp, mq).parse();
  }

  private static Expression jsExpr(CharProducer cp, MessageQueue mq)
      throws ParseException {
    Parser p = jsParser(cp, mq);
    Expression e = p.parseExpression(true);
    p.getTokenQueue().expectEmpty();
    return e;
  }

  private static Parser jsParser(CharProducer cp, MessageQueue mq) {
    JsLexer lexer = new JsLexer(cp, false);
    JsTokenQueue tq = new JsTokenQueue(lexer, cp.getCurrentPosition().source());
    tq.setInputRange(cp.filePositionForOffsets(cp.getOffset(), cp.getLimit()));
    return new Parser(tq, mq);
  }

  private static CharProducer fromFile(String path) throws IOException {
    File f = new File(path);
    return CharProducer.Factory.create(
        new InputStreamReader(new FileInputStream(f), "UTF-8"),
        new InputSource(f.toURI()));
  }
}
