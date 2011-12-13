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
import com.google.caja.parser.js.SpecialOperation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An order-significant series of rules.
 *
 * @author mikesamuel@gmail.com
 */
public final class RuleChain {
  private final List<Rule> rules = new ArrayList<Rule>();

  private final NodeTypeFilter nodeTypeFilter = new NodeTypeFilter(this);
  private final SpecialOpFilter specOpFilter = new SpecialOpFilter(this);

  public void add(Rule r) {
    rules.add(r);
    nodeTypeFilter.reset();
    specOpFilter.reset();
  }

  public List<Rule> getAllRules() {
    return Collections.unmodifiableList(rules);
  }

  /**
   * Return at least the rules applicable to the given node, but possibly more.
   */
  public List<Rule> applicableTo(ParseTreeNode node) {
    if (node instanceof SpecialOperation) {
      return specOpFilter.rulesFor(node);
    } else {
      return nodeTypeFilter.rulesFor(node);
    }
  }
}
