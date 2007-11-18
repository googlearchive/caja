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

import com.google.caja.reporting.RenderContext;

import java.io.IOException;

/**
 * A literal expression whose value does not depend on the environment, and
 * whose evaluation has no side-effect.
 *
 * @author mikesamuel@gmail.com
 */
public abstract class Literal extends AbstractExpression<Expression> {
  // TODO(mikesamuel): rendering of numeric literals should not depend on the
  // default locale.
  // TODO(mikesamuel): find some way of rendering NaN and Infinity in a way
  // that doesn't allow reference masking.

  protected Literal() {
    childrenChanged();
  }

  @Override
  public abstract Object getValue();

  public abstract boolean getValueInBooleanContext();

  @Override
  protected final void childrenChanged() {
    super.childrenChanged();
    if (!children.isEmpty()) { throw new IllegalStateException(); }
  }

  public void render(RenderContext rc) throws IOException {
    rc.out.append(getValue().toString());
  }
}
