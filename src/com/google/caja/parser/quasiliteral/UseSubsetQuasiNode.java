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
import com.google.caja.parser.js.UseSubset;
import com.google.caja.parser.js.UseSubsetDirective;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A quasi mode that matches a use subset node that matches a
 * {@link UseSubsetDirective} that contains all the subset names as the quasi
 * node.
 *
 * <p>
 * So the quasiliteral {@code 'use strict'} will match {@code 'use strict'},
 * and {@code 'use strict,cajita'}, but not {@code 'use shiny'}.
 *
 * @author mikesamuel@gmail.com
 */
final class UseSubsetQuasiNode extends QuasiNode {
  private final Set<String> subsetNames;

  public UseSubsetQuasiNode(Set<String> subsetNames) {
    this.subsetNames = new LinkedHashSet<String>(subsetNames);
  }

  @Override
  protected boolean consumeSpecimens(
      List<ParseTreeNode> specimens, Map<String, ParseTreeNode> bindings) {
    if (specimens.isEmpty()) { return false; }
    ParseTreeNode specimen = specimens.get(0);
    if (!(specimen instanceof UseSubsetDirective)) { return false; }
    UseSubsetDirective usd = ((UseSubsetDirective) specimen);
    if (!usd.getSubsetNames().containsAll(subsetNames)) { return false; }
    specimens.remove(0);
    return true;
  }

  @Override
  protected boolean createSubstitutes(
      List<ParseTreeNode> substitutes, Map<String, ParseTreeNode> bindings) {
    List<UseSubset> subsets = new ArrayList<UseSubset>();
    for (String subsetName : subsetNames) {
      subsets.add(new UseSubset(FilePosition.UNKNOWN, subsetName));
    }
    substitutes.add(new UseSubsetDirective(FilePosition.UNKNOWN, subsets));
    return true;
  }
}
