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

package com.google.caja.parser.js;

import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.TokenConsumer;
import com.google.caja.parser.AbstractParseTreeNode;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.render.Concatenator;
import com.google.caja.render.JsPrettyPrinter;
import com.google.caja.reporting.RenderContext;
import com.google.caja.util.Callback;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Translates to a cajoled module which renders as: <tt>___.loadModule...</tt>.
 * This is not a core JavaScript parse tree node &mdash; it is never produced by
 * {@link Parser}.
 *
 * Legacy: still used by TracingRewriter even though there's no longer such
 * things as cajoling or modules.
 *
 * @author erights@gmail.com
 * @author ihab.awad@gmail.com
 */
public final class UncajoledModule extends AbstractParseTreeNode {
  private static final long serialVersionUID = 4647984501924442035L;

  /** @param value unused.  This ctor is provided for reflection. */
  @ReflectiveCtor
  public UncajoledModule(FilePosition pos,
                         Void value,
                         List<? extends Block> children) {
    this(pos, children.get(0));
    assert children.size() == 1;
  }

  public UncajoledModule(FilePosition pos, Block body) {
    super(pos, Block.class);
    createMutation().appendChild(body).execute();
  }

  public UncajoledModule(Block body) {
    this(FilePosition.UNKNOWN, body);
  }

  public static UncajoledModule of(ParseTreeNode node) {
    if (node instanceof Block) {
      return new UncajoledModule((Block) node);

    } else if (node instanceof Statement) {
      return new UncajoledModule(new Block(
          node.getFilePosition(),
          Collections.singletonList((Statement) node)));

    } else if (node instanceof Expression) {
      return new UncajoledModule(new Block(
          node.getFilePosition(),
          Collections.singletonList(new ExpressionStmt((Expression) node))));
    } else {
      throw new ClassCastException("Unexpected node type " + node);
    }
  }

  @Override
  protected void childrenChanged() {
    super.childrenChanged();
    if (children().size() != 1) {
      throw new IllegalStateException(
          "An UncajoledModule may only have one child");
    }
    ParseTreeNode module = children().get(0);
    if (!(module instanceof Block)) {
      throw new ClassCastException("Expected block, not " + module);
    }
  }

  @Override
  public Object getValue() { return null; }

  @Override
  public List<? extends Block> children() {
    return childrenAs(Block.class);
  }

  public Block getModuleBody() { return children().get(0); }

  public final TokenConsumer makeRenderer(
      Appendable out, Callback<IOException> exHandler) {
    return new JsPrettyPrinter(new Concatenator(out, exHandler));
  }

  public void render(RenderContext rc) {
    TokenConsumer out = rc.getOut();
    out.consume("/* Start Uncajoled Module */");
    out.consume("throw");
    out.consume("'Uncajoled Module must never be executed'");
    out.consume(";");
    getModuleBody().render(rc);
    out.consume("/* End Uncajoled Module */");
  }
}
