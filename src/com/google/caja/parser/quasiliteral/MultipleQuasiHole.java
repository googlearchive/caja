// Copyright (C) 2007 Google Inc.
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
import com.google.caja.parser.ParseTreeNodeContainer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Quasiliteral "hole" matching zero to many values (regexp "*"). The match is always
 * greedy, and no backtracking is done.
 *
 * @author ihab.awad@gmail.com (Ihab Awad)
 */
public class MultipleQuasiHole extends AbstractQuasiHole {
  public MultipleQuasiHole(Class<? extends ParseTreeNode> matchedClass, String identifier) {
    super(matchedClass, identifier);
  }

  @Override
  protected boolean consumeSpecimens(
      List<ParseTreeNode> specimens,
      Map<String, ParseTreeNode> bindings) {
    List<ParseTreeNode> matches = new ArrayList<ParseTreeNode>();
    while (specimens.size() > 0 && isCompatibleClass(specimens.get(0))) {
      matches.add(specimens.remove(0));
    }
    return putIfDeepEquals(bindings, getIdentifier(), new ParseTreeNodeContainer(matches));
  }

  @Override
  protected boolean createSubstitutes(
      List<ParseTreeNode> substitutes,
      Map<String, ParseTreeNode> bindings) {
    if (bindings.containsKey(getIdentifier())) {
      for (ParseTreeNode child : bindings.get(getIdentifier()).children()) {
        substitutes.add(child.clone());
      }
      return true;
    }
    return false;
  }

  @Override
  protected String getQuantifierSuffix() { return "*"; }
}
