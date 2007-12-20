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
 * @author mikesamuel@gmail.com
 */
public final class NullLiteral extends Literal {
  public NullLiteral(NullPlaceholder value,
                     List<? extends ParseTreeNode> children) {
    this();
  }

  public NullLiteral() {
  }

  @Override
  public NullPlaceholder getValue() {
    return VALUE;
  }

  public static final NullPlaceholder VALUE = new NullPlaceholder();

  public static class NullPlaceholder {
    @Override
    public String toString() { return "null"; }

    private NullPlaceholder() { /* singleton */ }
  }

  @Override
  public boolean getValueInBooleanContext() {
    return false;
  }
}
