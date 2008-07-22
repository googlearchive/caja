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

import com.google.caja.lexer.ParseException;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.js.Expression;
import com.google.caja.parser.js.ExpressionStmt;
import com.google.caja.parser.js.FunctionConstructor;
import com.google.caja.parser.js.FunctionDeclaration;
import com.google.caja.parser.js.Identifier;
import com.google.caja.parser.js.Reference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An order-significant series of rules.
 *
 * @author mikesamuel@gmail.com
 */
public final class RuleChain {
  private final List<Rule> rules = new ArrayList<Rule>();
  private final Map<Class<? extends ParseTreeNode>, List<Rule>> filtered
      = new HashMap<Class<? extends ParseTreeNode>, List<Rule>>();

  public void add(Rule r) {
    rules.add(r);
    filtered.clear();
  }

  /**
   * Return at least the rules applicable to the given node, but possibly more.
   */
  public List<Rule> applicableTo(ParseTreeNode node) {
    Class<? extends ParseTreeNode> nodeType = toNodeType(node.getClass());
    List<Rule> filteredRules = filtered.get(nodeType);
    if (filteredRules == null) {
      computeRulesFor(nodeType);
      filteredRules = filtered.get(nodeType);
    }
    return filteredRules;
  }

  public Iterable<Rule> getAllRules() {
    return Collections.unmodifiableList(rules);
  }

  /**
   * Caches quasi text to a conservative lower bound for ParseTreeNode types
   * that it might match.
   * This does not assume that quasi-strings parse to a valid parse tree.
   * Some of the quasi-strings look like "<Approximately> @foo = @bar" and
   * so on parse failures we return a lower bound of ParseTreeNode.
   */
  private static final Map<String, Class<? extends ParseTreeNode>> lowerBounds
      = Collections.synchronizedMap(
          new HashMap<String, Class<? extends ParseTreeNode>>());
  static {
    lowerBounds.put(null, ParseTreeNode.class);  // lower bound for no pattern
  }
  private void computeRulesFor(Class<? extends ParseTreeNode> nodeType) {
    List<Rule> applicableRules = new ArrayList<Rule>();
    for (Rule rule : rules) {
      String pattern = rule.getRuleDescription().matches();
      Class<? extends ParseTreeNode> lowerBound = lowerBounds.get(pattern);
      if (lowerBound == null) {
        try {
          QuasiNode p = QuasiBuilder.parseQuasiNode(pattern);
          if (p instanceof SimpleQuasiNode) {
            lowerBound = toNodeType(((SimpleQuasiNode) p).getMatchedClass());
          } else {
            lowerBound = ParseTreeNode.class;
          }
        } catch (ParseException ex) {
          // If the match pattern can't be parsed then assume the lowest lower
          // bound.  This may happen if the match string is documentation, not
          // a real pattern.
          lowerBound = ParseTreeNode.class;
        }
        lowerBounds.put(pattern, lowerBound);
      }
      if (lowerBound.isAssignableFrom(nodeType)) {
        applicableRules.add(rule);
      }
    }
    filtered.put(nodeType, Collections.unmodifiableList(applicableRules));
  }

  /** Parallels fuzzing done in QuasiBuilder.parseQuasiNode */
  private static Class<? extends ParseTreeNode> toNodeType(
      Class<? extends ParseTreeNode> nodeClass) {
    if (nodeClass == FunctionDeclaration.class) {
      return FunctionConstructor.class;
    }
    if (nodeClass == Expression.class) {
      return ExpressionStmt.class;
    }
    if (nodeClass == Reference.class) {
      return Identifier.class;
    }
    return nodeClass;
  }
}
