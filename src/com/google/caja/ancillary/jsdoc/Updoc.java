// Copyright (C) 2008 Google Inc.
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

package com.google.caja.ancillary.jsdoc;

import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.TokenConsumer;
import com.google.caja.parser.AbstractParseTreeNode;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.js.Expression;
import com.google.caja.render.Concatenator;
import com.google.caja.render.JsPrettyPrinter;
import com.google.caja.reporting.RenderContext;
import com.google.caja.util.Callback;

import java.io.IOException;
import java.util.List;

/**
 * A passage of executable documentation in a javascript doc comment.
 *
 * <p>
 * The {@code Math.abs} function could be documented as follows
 * <pre>
 * /**
 *  * Given a number, returns the magnitude aka absolute value.
 *  * The result is always either non-negative or NaN, and the following
 *  * relationship holds:
 *  *     var an = Math.abs(n);
 *  *     isNaN(n) || an === (Math.max(n, -n))
 *  * {&#64;updoc
 *  *     $ Math.abs(5)
 *  *     # 5
 *  *     $ Math.abs(-5)
 *  *     # 5
 *  *     $ Math.abs(NaN)
 *  *     # NaN
 *  *     $ Math.abs(-1/0)
 *  *     # Infinity
 *  *     $ Math.abs(1.5)
 *  *     # 1.5
 *  * }
 *  *&#47;
 * </pre>
 *
 * <p>
 * The lines that start with '$' act as inputs to an interactive interpreter
 * shell, and the following '#' lines act as the results from the interpreter.
 * These tests can be run when the documentation is produced.
 *
 * <p>
 * According to Mark Miller's E documentation
 * <blockquote>
 * The chapter serves as checkable documentation about the Vector class, where
 * you can always check whether the embedded code examples are accurate. When
 * they aren't, please complain!
 * <p>
 * The chapter serves as a regression test. Brian Marick documents that
 * regression tests normally degenerate over time as a result of maintenance.
 * To keep the tests running as the software is changed, the purpose of the test
 * is often lost, and the test is gradually changed into one that always passes.
 * When the regression tests are embedded examples in readable documentation,
 * the test-documentation combination would be rendered noticeably incoherent if
 * the test were to become meaningless.
 * <p>
 * With this chapter, you can do experimental reading. When reading a code
 * example in a programming manual, how often have you thought "But what if we
 * try it this other way?" By bringing an Updoc chapter into Elmer, the text
 * serves as a "user interface" for interactively poking at the API you're
 * reading about, and trying out hypothetical variants of the examples shown.
 * <p>
 * This chapter demonstrates interactive, or even adversarial writing. I wrote
 * the first draft of this chapter, not by intending to write a chapter on
 * Java's Vector class, but simply because I was curious about the meaning of
 * Vector's "size()" vs "capacity()", so I brought up Elmer to try these out.
 * By saving this Elmer transcript, I had my first draft. This is interactive
 * writing. When writing test cases, we should be trying to creatively break the
 * module being tested, by trying edge cases or whatever. A live Elmer session
 * gives us a sense of a live adversary to break. The transcript of the
 * resulting session will often be a great first draft of the needed
 * chapter/regression-test of the module. This is adversarial writing.
 * <blockquote>
 *
 * <p>
 * See <a href="http://www.erights.org/elang/tools/updoc.html">What's updoc</a>
 * @author mikesamuel@gmail.com
 */
public final class Updoc extends AbstractParseTreeNode {
  Updoc(FilePosition pos, List<? extends Run> runs) {
    super(pos, Run.class);
    createMutation().appendChildren(runs).execute();
  }

  @Override
  public Object getValue() { return null; }

  @Override
  public List<? extends Run> children() {
    return childrenAs(Run.class);
  }
  public List<? extends Run> getRuns() { return children(); }

  public void render(RenderContext rc) {
    rc.getOut().mark(getFilePosition());
    for (Run run : getRuns()) { run.render(rc); }
  }

  public TokenConsumer makeRenderer(
      Appendable out, Callback<IOException> handler) {
    return new JsPrettyPrinter(new Concatenator(out, handler));
  }

  @Override
  protected void childrenChanged() {
    for (ParseTreeNode node : children()) {
      if (!(node instanceof Run)) {
        throw new ClassCastException(node.getClass().getName());
      }
    }
  }

  /** A single input to and response from an interactive shell. */
  public static final class Run extends AbstractParseTreeNode {
    Run(FilePosition pos, Expression input, Expression result) {
      super(pos, Expression.class);
      createMutation().appendChild(input).appendChild(result).execute();
    }
    @Override
    protected void childrenChanged() {
      if (2 != children().size()) { throw new IllegalStateException(); }
      getInput();
      getResult();
    }
    @Override
    public List<? extends Expression> children() {
      return childrenAs(Expression.class);
    }
    public Expression getInput() { return children().get(0); }
    public Expression getResult() { return children().get(1); }
    @Override
    public Object getValue() { return null; }
    public void render(RenderContext rc) {
      rc.getOut().mark(getFilePosition());
      rc.getOut().consume("$");
      rc.getOut().consume(" ");
      getInput().render(rc);
      rc.getOut().consume(";");
      rc.getOut().consume("\n");
      rc.getOut().consume("#");
      rc.getOut().consume(" ");
      getResult().render(rc);
      rc.getOut().consume(";");
    }
    public TokenConsumer makeRenderer(
        Appendable out, Callback<IOException> handler) {
      return new JsPrettyPrinter(new Concatenator(out, handler));
    }
  }
}
