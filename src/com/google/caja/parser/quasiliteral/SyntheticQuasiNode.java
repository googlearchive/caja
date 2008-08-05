// Copyright (C) 2008 Google Inc.
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

package com.google.caja.parser.quasiliteral;

import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.js.SyntheticNodes;
import java.util.List;
import java.util.Map;

/**
 * A {@link SimpleQuasiNode} that marks its output
 * {@link SyntheticNodes#SYNTHETIC synthetic}.
 *
 * @author mikesamuel@gmail.com
 */
public class SyntheticQuasiNode extends SimpleQuasiNode {
  public SyntheticQuasiNode(
      Class<? extends ParseTreeNode> clazz,
      Object value,
      QuasiNode... children) {
    super(clazz, value, children);
  }

  @Override
  protected boolean createSubstitutes(
      List<ParseTreeNode> substitutes,
      Map<String, ParseTreeNode> bindings) {
    // All the candidates after this position were added by the super call.
    int pos = substitutes.size();
    if (super.createSubstitutes(substitutes, bindings)) {
      int size = substitutes.size();
      while (pos < size) {
        SyntheticNodes.s(substitutes.get(pos++));
      }
      return true;
    }
    return false;
  }
}
