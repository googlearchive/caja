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
import com.google.caja.parser.ParseTreeNode;

/**
 * A compound statement that may execute its body zero or more times.
 *
 * @author mikesamuel@gmail.com
 */
public abstract class Loop extends LabeledStatement {
  private static final long serialVersionUID = -4732426661456039338L;

  @ReflectiveCtor
  public Loop(
      FilePosition pos, String label,
      Class<? extends ParseTreeNode> childClass) {
    super(pos, label, childClass);
  }

  public abstract Expression getCondition();
  public abstract Statement getBody();

  @Override
  public final boolean isTargetForContinue() { return true; }
}
