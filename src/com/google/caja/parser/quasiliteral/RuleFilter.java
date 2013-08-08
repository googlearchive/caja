// Copyright (C) 2011 Google Inc.
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
// limitations under the License.package com.google.caja.parser.quasiliteral;

package com.google.caja.parser.quasiliteral;

import com.google.caja.parser.ParseTreeNode;
import com.google.caja.util.Maps;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public abstract class RuleFilter<KeyT> {
  private final RuleChain ruleChain;
  private final Map<KeyT, List<Rule>> cache = Maps.newHashMap();

  private static boolean debug = false;

  public RuleFilter(RuleChain ruleChain) {
    this.ruleChain = ruleChain;
  }

  public void reset() {
    cache.clear();
  }

  public List<Rule> rulesFor(ParseTreeNode node) {
    KeyT key = getKey(node);
    if (key == null) {
      return ruleChain.getAllRules();
    }
    List<Rule> someRules = cache.get(key);
    if (someRules == null) {
      someRules = computeRulesFor(key);
      cache.put(key, someRules);
      if (debug) { debugShowRules(someRules, node); }
    }
    return someRules;
  }

  private List<Rule> computeRulesFor(KeyT key) {
    List<Rule> someRules = new ArrayList<Rule>();
    for (Rule rule : ruleChain.getAllRules()) {
      if (canMatch(rule, key)) {
        someRules.add(rule);
      }
    }
    return Collections.unmodifiableList(someRules);
  }

  private static void debugShowRules(List<Rule> someRules, ParseTreeNode node) {
    System.err.println(someRules.size() + " rules for " + node);
    StringBuilder names = new StringBuilder("  ");
    int ll = 2;
    for (Rule rule: someRules) {
      String name = rule.getName();
      ll += 1 + name.length();
      if (75 < ll) {
        names.append("\n  ");
        ll = 2 + 1 + name.length();
      }
      names.append(" ").append(name);
    }
    System.err.println(names);
  }

  abstract KeyT getKey(ParseTreeNode node);
  abstract boolean canMatch(Rule rule, KeyT key);
}
