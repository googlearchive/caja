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

import java.util.List;

import com.google.caja.lexer.TokenConsumer;
import com.google.caja.reporting.RenderContext;

/**
 * FIXME(metaweta): document me
 * 
 * @author metaweta@gmail.com (Mike Stay)
 */
public final class QuotedExpression extends AbstractExpression {
  /**
   * Create a parse tree node that expands to the given expression in a
   * rewriter.
   */
  public QuotedExpression(Expression e) {
    super(Expression.class);
    createMutation().appendChild(e).execute();
  }

  /** @param value unused.  This ctor is provided for reflection. */
  public QuotedExpression(Void value, List<? extends Expression> children) {
    this(children.get(0));
  }
  
  @Override
  public Object getValue() { return null; }

  /** This should only be called by logging code. */
  public void render(RenderContext r) {
    TokenConsumer out = r.getOut();
    out.mark(getFilePosition());
    out.consume("QuotedExpression");
    out.consume("(");
    children().get(0).render(r);
    out.consume(")");
  }

  public Expression unquote() { return (Expression) children().get(0); }
}
