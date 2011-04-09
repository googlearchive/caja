// Copyright (C) 2010 Google Inc.
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

import com.google.caja.SomethingWidgyHappenedError;
import com.google.caja.lexer.FilePosition;

import java.util.Collections;
import java.util.List;

/**
 * A value literal for skipped values in array ctors, e.g. the undefined
 * value between 1 and 2 in the array {@code [1,,2]}.
 *
 * @author mikesamuel@gmail.com
 */
public final class Elision extends SpecialOperation {
  private static final long serialVersionUID = -4885250718985438681L;

  private static Operator checkOp(Operator op) {
    if (op != Operator.VOID) { throw new SomethingWidgyHappenedError(); }
    return op;
  }

  /**
   * This ctor is provided for reflection.
   * @param op unused.
   * @param operands unused.
   */
  @ReflectiveCtor
  public Elision(
      FilePosition pos, Operator op, List<? extends Expression> operands) {
    super(pos, checkOp(op), operands);
  }

  public Elision(FilePosition pos) {
    super(
        pos, Operator.VOID,
        Collections.singletonList(new IntegerLiteral(pos, 0)));
  }

  @Override protected void childrenChanged() {
    super.childrenChanged();
    IntegerLiteral ignoredValue = (IntegerLiteral) children().get(0);
    if (ignoredValue == null) { throw new NullPointerException(); }
  }
}
