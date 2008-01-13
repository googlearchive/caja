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
import com.google.caja.parser.ParseTreeNodes;
import com.google.caja.parser.js.ObjectConstructor;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;

/**
 * A quasi hole matching an object constructor. It always matches expressions of
 * the form:
 *
 * <pre>
 *   { k1: v1, k2: v2, ..., kn: vn}
 * </pre>
 *
 * and binds the key nodes as a {@code ParseTreeNodeContainer} under {@code keyIdentifier},
 * and the value nodes as a {@code ParseTreeNodeContainer} under {@code valueIdentifier}.
 *
 * @author ihab.awad@gmail.com (Ihab Awad)
 */
public class ObjectConstructorHole extends QuasiNode {
  private final String keyIdentifier;
  private final String valueIdentifier;

  public ObjectConstructorHole(String keyIdentifier, String valueIdentifier) {
    this.keyIdentifier = keyIdentifier;
    this.valueIdentifier = valueIdentifier;
  }

  protected boolean consumeSpecimens(
      List<ParseTreeNode> specimens,
      Map<String, ParseTreeNode> bindings) {
    if (specimens.isEmpty()) return false;
    if (!(specimens.get(0) instanceof ObjectConstructor)) return false;
    assert(specimens.get(0).children().size() % 2 == 0);
    List<ParseTreeNode> keyList = new ArrayList<ParseTreeNode>();
    List<ParseTreeNode> valueList = new ArrayList<ParseTreeNode>();
    for (int i = 0; i < specimens.get(0).children().size(); ) {
      keyList.add(specimens.get(0).children().get(i++));
      valueList.add(specimens.get(0).children().get(i++));
    }
    specimens.remove(0);
    return
        putIfDeepEquals(bindings, keyIdentifier, new ParseTreeNodeContainer(keyList)) &&
        putIfDeepEquals(bindings, valueIdentifier, new ParseTreeNodeContainer(valueList));
  }

  protected boolean createSubstitutes(
      List<ParseTreeNode> substitutes,
      Map<String, ParseTreeNode> bindings) {
    if (bindings.containsKey(keyIdentifier) &&
        bindings.containsKey(valueIdentifier)) {
      ParseTreeNode keyNode = bindings.get(keyIdentifier);
      ParseTreeNode valueNode = bindings.get(valueIdentifier);
      assert(keyNode.children().size() == valueNode.children().size());
      List<ParseTreeNode> children = new ArrayList<ParseTreeNode>();
      for (int i = 0; i < keyNode.children().size(); i++) {
        children.add(keyNode.children().get(i));
        children.add(valueNode.children().get(i));    
      }
      substitutes.add(ParseTreeNodes.newNodeInstance(
          ObjectConstructor.class,
          null,
          children));
      return true;
    }
    return false;
  }
}
