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
import com.google.caja.parser.js.Operation;
import com.google.caja.parser.js.Operator;

public class SpecialOpFilter extends RuleFilter<Operator> {

  public SpecialOpFilter (RuleChain ruleChain) {
    super(ruleChain);
  }

  @Override
  Operator getKey(ParseTreeNode node) {
    return ((Operation) node).getOperator();
  }

  @Override
  boolean canMatch(Rule rule, Operator op) {
    return ruleOperator(rule) == op || rule.canMatch(Operation.class);
  }

  private static Operator ruleOperator(Rule rule) {
    QuasiNode qp = QuasiCache.parse(rule.getRuleDescription().matches());
    if (qp != null && qp instanceof SimpleQuasiNode) {
      SimpleQuasiNode sqp = (SimpleQuasiNode) qp;
      if (Operation.class.isAssignableFrom(sqp.getMatchedClass())) {
        return (Operator) sqp.getValue();
      }
    }
    return null;
  }

}
