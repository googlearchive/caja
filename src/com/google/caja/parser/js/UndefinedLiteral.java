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

import com.google.caja.parser.ParseTreeNode;

import java.util.List;

/**
 * The literal value "undefined".
 * Undefined is not a keyword, it's just a variable initialized to
 * <code>(void 0)</code>, but we still treat undefined as a literal since it's
 * the common idiom for a special value.
 *
 * @author mikesamuel@gmail.com
 */
public final class UndefinedLiteral extends Literal {
  public static final String VALUE_NAME = "undefined";
  public static final UndefinedPlaceholder VALUE = new UndefinedPlaceholder();

  /**
   * This ctor is provided for reflection.
   * @param value unused.
   * @param children unused.
   */
  public UndefinedLiteral(UndefinedPlaceholder value,
                          List<? extends ParseTreeNode> children) {
    this();
  }

  public UndefinedLiteral() {
  }

  @Override
  public UndefinedPlaceholder getValue() {
    return VALUE;
  }

  @Override
  public boolean getValueInBooleanContext() {
    return false;
  }

  public static class UndefinedPlaceholder {
    @Override
    public String toString() { return VALUE_NAME; }

    private UndefinedPlaceholder() { /* singleton */ }
  }
}
