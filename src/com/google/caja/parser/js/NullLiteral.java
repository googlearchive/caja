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
import com.google.caja.lexer.Keyword;
import com.google.caja.parser.ParseTreeNode;
import java.util.List;

/**
 * A value literal for the <code>{@link Keyword#NULL null}</code> keyword.
 *
 * @author mikesamuel@gmail.com
 */
public final class NullLiteral extends Literal {
  private static final long serialVersionUID = -1719066448853208388L;

  /**
   * This ctor is provided for reflection.
   * @param value unused.
   * @param children unused.
   */
  @ReflectiveCtor
  public NullLiteral(FilePosition pos, NullPlaceholder value,
                     List<? extends ParseTreeNode> children) {
    this(pos);
  }

  public NullLiteral(FilePosition pos) {
    super(pos);
  }

  @Override
  public NullPlaceholder getValue() {
    return NullPlaceholder.VALUE;
  }

  private static class NullPlaceholder {
    static final NullPlaceholder VALUE = new NullPlaceholder();

    @Override
    public String toString() { return Keyword.NULL.toString(); }

    private NullPlaceholder() { /* singleton */ }
  }

  @Override
  public boolean getValueInBooleanContext() {
    return false;
  }

  public String typeOf() { return "object"; }
}
