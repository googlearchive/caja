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
import com.google.caja.reporting.RenderContext;

import java.util.List;

/**
 * A function parameter declaration.
 *
 * @author mikesamuel@gmail.com
 */
public final class FormalParam extends Declaration {
  private static final long serialVersionUID = 5841430129235689345L;

  @ReflectiveCtor
  public FormalParam(
      FilePosition pos, Void value, List<? extends Expression> children) {
    super(pos, value, children);
  }

  public FormalParam(Identifier identifier) {
    super(identifier.getFilePosition(), identifier, (Expression) null);
  }

  @Override
  public void render(RenderContext rc) {
    rc.getOut().mark(getFilePosition());
    getIdentifier().render(rc);
  }
}
