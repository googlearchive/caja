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
import com.google.caja.parser.AbstractParseTreeNode;
import com.google.caja.render.Concatenator;
import com.google.caja.render.JsPrettyPrinter;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.util.Callback;

import java.io.IOException;

import javax.annotation.Nullable;

/**
 *
 * @author mikesamuel@gmail.com
 */
public abstract class AbstractExpression
    extends AbstractParseTreeNode implements Expression {
  private static final long serialVersionUID = 6796876031318912717L;

  @ReflectiveCtor
  public AbstractExpression(
      FilePosition pos, Class<? extends ParseTreeNode> childClass) {
    super(pos, childClass);
  }

  public boolean isLeftHandSide() { return false; }

  public final TokenConsumer makeRenderer(
      Appendable out, Callback<IOException> exHandler) {
    return new JsPrettyPrinter(new Concatenator(out, exHandler));
  }

  public Expression simplifyForSideEffect() { return this; }

  public @Nullable Boolean conditionResult() { return null; }

  public Expression fold(boolean isFn) { return this; }
}
