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
import com.google.caja.plugin.SyntheticNodes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A simple quasiliteral node that matches the class name and value of a {@link ParseTreeNode}.
 *
 * @author ihab.awad@gmail.com (Ihab Awad)
 */
public class SimpleQuasiNode extends QuasiNode {
  private final Class<? extends ParseTreeNode> clazz;
  private final Object value;

  public SimpleQuasiNode(
      Class<? extends ParseTreeNode> clazz,
      Object value,
      QuasiNode... children) {
    super(children);
    this.clazz = clazz;
    this.value = value;
  }

  protected boolean consumeSpecimens(
      List<ParseTreeNode> specimens,
      Map<String, ParseTreeNode> bindings) {
    if (specimens.isEmpty()) return false;
    if (matchSelf(specimens.get(0)) &&
        matchChildren(specimens.get(0), bindings)) {
      specimens.remove(0);
      return true;
    }
    return false;
  }

  private boolean matchSelf(ParseTreeNode specimen) {
    return
        clazz == specimen.getClass() &&
        safeEquals(value, specimen.getValue());
  }

  private boolean matchChildren(
      ParseTreeNode specimen,
      Map<String, ParseTreeNode> bindings) {
    List<ParseTreeNode> specimenChildren = new ArrayList<ParseTreeNode>();
    specimenChildren.addAll(specimen.children());

    for (QuasiNode child : getChildren()) {
      if (!child.consumeSpecimens(specimenChildren, bindings)) return false;
    }

    return specimenChildren.size() == 0;
  }

  protected boolean createSubstitutes(
      List<ParseTreeNode> substitutes,
      Map<String, ParseTreeNode> bindings) {
    List<ParseTreeNode> children = new ArrayList<ParseTreeNode>();

    for (QuasiNode child : getChildren()) {
      if (!child.createSubstitutes(children, bindings)) return false;
    }

    substitutes.add(
        SyntheticNodes.s(
            ParseTreeNodes.newNodeInstance(clazz, value, children)));
    return true;
  }

  public String toString() {
    return clazz.getSimpleName() + (value == null ? "" : " : " + value);
  }
}
