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

package com.google.caja.parser.js;

import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.TokenConsumer;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.reporting.RenderContext;
import java.util.List;

/**
 * A top level statement that declares a variable and assigns a function to it.
 *
 * @author mikesamuel@gmail.com
 */
public final class FunctionDeclaration extends Declaration {
  private static final long serialVersionUID = 4973243536242692075L;

  @ReflectiveCtor
  public FunctionDeclaration(
      FilePosition pos, Void value, List<? extends ParseTreeNode> children) {
    super(pos, value, children);
  }

  public FunctionDeclaration(
      FunctionConstructor initializer) {
    super(initializer.getFilePosition(),
          (Identifier) initializer.getIdentifier().clone(),
          initializer);
  }

  @Override
  protected void childrenChanged() {
    super.childrenChanged();
    FunctionConstructor initializer = getInitializer();  // Checks class.
    if (null == initializer) {
      throw new NullPointerException(
          "Function declaration missing function");
    }
    if (!getIdentifierName().equals(initializer.getIdentifierName())) {
      throw new IllegalStateException("Name mismatch");
    }
  }

  @Override
  public FunctionConstructor getInitializer() {
    return (FunctionConstructor) super.getInitializer();
  }

  @Override
  public boolean isTerminal() {
    return getInitializer().getBody().isTerminal();
  }

  @Override
  public void render(RenderContext rc) {
    FunctionConstructor fc = getInitializer();
    TokenConsumer out = rc.getOut();
    out.mark(getFilePosition());
    out.consume("function");
    getIdentifier().render(rc);
    out.consume("(");
    boolean seen = false;
    for (FormalParam p : fc.getParams()) {
      if (seen) {
        out.consume(",");
      } else {
        seen = true;
      }
      p.render(rc);
    }
    out.consume(")");
    fc.getBody().renderBlock(rc, false);
  }
}
