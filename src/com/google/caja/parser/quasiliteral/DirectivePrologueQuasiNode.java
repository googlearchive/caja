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
import com.google.caja.parser.js.Directive;
import com.google.caja.parser.js.DirectivePrologue;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A quasi mode that matches a use subset node that matches a
 * {@link com.google.caja.parser.js.DirectivePrologue}
 * that contains all the subset names as the quasi node.
 *
 * <p>
 * So the quasiliteral {@code 'use strict';} will match {@code 'use strict';},
 * and {@code 'use strict'; 'use strict';}, but not {@code 'use shiny';} or
 * {@code 'alien directive from outer space';}.
 *
 * @author mikesamuel@gmail.com
 */
final class DirectivePrologueQuasiNode extends QuasiNode {
  private final Set<String> directives;

  public DirectivePrologueQuasiNode(Set<String> directives) {
    this.directives = new LinkedHashSet<String>(directives);
  }

  @Override
  protected boolean consumeSpecimens(
      List<ParseTreeNode> specimens, Map<String, ParseTreeNode> bindings) {
    if (specimens.isEmpty()) { return false; }
    ParseTreeNode specimen = specimens.get(0);
    if (!(specimen instanceof DirectivePrologue)) { return false; }
    DirectivePrologue usd = ((DirectivePrologue) specimen);
    if (!usd.getDirectives().containsAll(directives)) { return false; }
    specimens.remove(0);
    return true;
  }

  @Override
  protected boolean createSubstitutes(
      List<ParseTreeNode> substitutes, Map<String, ParseTreeNode> bindings) {
    List<Directive> subsets = new ArrayList<Directive>();
    for (String subsetName : directives) {
      subsets.add(new Directive(FilePosition.UNKNOWN, subsetName));
    }
    substitutes.add(new DirectivePrologue(FilePosition.UNKNOWN, subsets));
    return true;
  }
}
