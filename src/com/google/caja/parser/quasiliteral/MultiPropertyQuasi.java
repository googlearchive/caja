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
import com.google.caja.parser.js.Expression;
import com.google.caja.parser.js.ObjProperty;
import com.google.caja.parser.js.StringLiteral;
import com.google.caja.parser.js.ValueProperty;
import com.google.common.collect.Lists;

import java.util.List;
import java.util.Map;

/**
 * A quasi hole matching an object constructor. It matches any number of
 * properties (adjacent key value pairs) of the abbreviate object syntax, e.g.:
 *
 * <pre>
 *   { k1: v1, k2: v2, ..., kn: vn}
 * </pre>
 *
 * and binds the key nodes as a {@code ParseTreeNodeContainer} under {@code k},
 * and the value nodes as a {@code ParseTreeNodeContainer} under {@code v}.
 *
 * This node type is always a child of {@link ObjectCtorQuasiNode}.
 * {@link SinglePropertyQuasi} is similar but only matches a single
 * key/value pair.
 *
 * @author ihab.awad@gmail.com (Ihab Awad)
 */
final class MultiPropertyQuasi extends QuasiNode {
  private final String keyIdentifier;
  private final String valueIdentifier;

  MultiPropertyQuasi(String keyIdentifier, String valueIdentifier) {
    this.keyIdentifier = keyIdentifier;
    this.valueIdentifier = valueIdentifier;
  }

  @Override
  protected boolean consumeSpecimens(
      List<ParseTreeNode> specimens, Map<String, ParseTreeNode> bindings) {
    List<ParseTreeNode> keyList = Lists.newArrayList();
    List<ParseTreeNode> valueList = Lists.newArrayList();
    for (ParseTreeNode quasi : specimens) {
      if (!(quasi instanceof ValueProperty)) { return false; }
      ValueProperty prop = (ValueProperty) quasi;
      keyList.add(prop.getPropertyNameNode());
      valueList.add(prop.getValueExpr());
    }
    if (putIfDeepEquals(
            bindings, keyIdentifier, new ParseTreeNodeContainer(keyList))
        && putIfDeepEquals(
              bindings, valueIdentifier,
              new ParseTreeNodeContainer(valueList))) {
      specimens.clear();
      return true;
    }
    return false;
  }

  @Override
  protected boolean createSubstitutes(
      List<ParseTreeNode> substitutes,
      Map<String, ParseTreeNode> bindings) {
    if (bindings.containsKey(keyIdentifier)
        && bindings.containsKey(valueIdentifier)) {
      ParseTreeNode keyNode = bindings.get(keyIdentifier);
      ParseTreeNode valueNode = bindings.get(valueIdentifier);
      assert keyNode.children().size() == valueNode.children().size();
      List<ObjProperty> children = Lists.newArrayList();
      for (int i = 0; i < keyNode.children().size(); i++) {
        children.add(
            new ValueProperty(
                (StringLiteral) keyNode.children().get(i),
                (Expression) valueNode.children().get(i)));
      }
      substitutes.addAll(children);
      return true;
    }
    return false;
  }
}
