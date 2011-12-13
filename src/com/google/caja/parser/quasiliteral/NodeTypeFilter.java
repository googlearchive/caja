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

public class NodeTypeFilter extends RuleFilter<Class<? extends ParseTreeNode>> {

  public NodeTypeFilter(RuleChain ruleChain) {
    super(ruleChain);
  }

  @Override
  Class<? extends ParseTreeNode> getKey(ParseTreeNode node) {
    return QuasiBuilder.fuzzType(node.getClass());
  }

  @Override
  boolean canMatch(Rule rule, Class<? extends ParseTreeNode> key) {
    return rule.canMatch(key);
  }
}
