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

import com.google.caja.lexer.FilePosition;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.js.Identifier;

import java.util.List;
import java.util.Map;

/**
 * A quasi-hole that matches an identifier with a null value.
 * @author mikesamuel@gmail.com
 */
final class SingleOptionalIdentifierQuasiNode extends QuasiNode {
  private final QuasiNode qn;

  SingleOptionalIdentifierQuasiNode(QuasiNode qn) { this.qn = qn; }

  @Override
  protected boolean consumeSpecimens(
      List<ParseTreeNode> specimens, Map<String, ParseTreeNode> bindings) {
    if (specimens.isEmpty()) { return false; }
    ParseTreeNode specimen = specimens.get(0);
    if (specimen instanceof Identifier && null == specimen.getValue()) {
      specimens.remove(0);
      return true;
    }
    return qn.consumeSpecimens(specimens, bindings);
  }

  @Override
  protected boolean createSubstitutes(
      List<ParseTreeNode> substitutes, Map<String, ParseTreeNode> bindings) {
    // Do we need to back out changes to bindings?
    if (qn.createSubstitutes(substitutes, bindings)) {
      return true;
    } else {
      substitutes.add(new Identifier(FilePosition.UNKNOWN, null));
      return true;
    }
  }
}
