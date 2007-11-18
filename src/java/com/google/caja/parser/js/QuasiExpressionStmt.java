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

package com.google.caja.parser.js;

import com.google.caja.parser.ParseTreeNode;
import com.google.caja.reporting.RenderContext;

import java.io.IOException;

/**
 * Quasiliteral version of an ExpressionStmt.
 * 
 * @author ihab.awad@gmail.com
 */
public final class QuasiExpressionStmt extends AbstractExpression<ParseTreeNode> {
  private final String quasiIdentifier;
  private final QuasiliteralQuantifier quasiQuantifier;

  public QuasiExpressionStmt(String identifier, QuasiliteralQuantifier quantifier) {
    this.quasiIdentifier = identifier;
    this.quasiQuantifier = quantifier;
    childrenChanged();
  }

  @Override
  public Object getValue() {
    return "@" + quasiIdentifier + quasiQuantifier.getSuffix();
  }

  public void render(RenderContext r) throws IOException {
    r.out.append("@" + quasiIdentifier + quasiQuantifier.getSuffix());
  }

  @Override
  public boolean isQuasiliteral() { return true; }

  @Override
  public QuasiliteralQuantifier getQuasiliteralQuantifier() { return quasiQuantifier; }

  @Override
  public String getQuasiliteralIdentifier() { return quasiIdentifier; }
  
  @Override
  public Class<? extends ParseTreeNode> getQuasiMatchedClass() { return Statement.class; }
}

