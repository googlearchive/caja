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

import com.google.caja.lexer.FilePosition;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.ParseTreeNodes;
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.Directive;
import com.google.caja.parser.js.DirectivePrologue;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A simple quasiliteral node that matches the class name and value of a
 * {@link ParseTreeNode}.
 *
 * @author ihab.awad@gmail.com (Ihab Awad)
 */
public class SimpleQuasiNode extends QuasiNode {
  private final Class<? extends ParseTreeNode> clazz;
  private final Object value;
  private final Equivalence valueComparator;

  protected SimpleQuasiNode(
      Class<? extends ParseTreeNode> clazz, Object value,
      Equivalence valueComparator, QuasiNode... children) {
    super(children);
    this.clazz = clazz;
    this.value = value;
    this.valueComparator = valueComparator;
  }

  @Override
  protected boolean consumeSpecimens(
      List<ParseTreeNode> specimens, Map<String, ParseTreeNode> bindings) {
    if (specimens.isEmpty()) return false;
    if (matchSelf(specimens.get(0)) &&
        matchChildren(specimens.get(0), bindings)) {
      specimens.remove(0);
      return true;
    }
    return false;
  }

  private boolean matchSelf(ParseTreeNode specimen) {
    return clazz == specimen.getClass()
        && valueComparator.equivalent(value, specimen.getValue());
  }

  private boolean matchChildren(
      ParseTreeNode specimen,
      Map<String, ParseTreeNode> bindings) {
    List<ParseTreeNode> specimenChildren = new ArrayList<>();
    specimenChildren.addAll(specimen.children());

    for (QuasiNode child : getChildren()) {
      if (!child.consumeSpecimens(specimenChildren, bindings)) { return false; }
    }

    return specimenChildren.isEmpty();
  }

  @Override
  protected boolean createSubstitutes(
      List<ParseTreeNode> substitutes,
      Map<String, ParseTreeNode> bindings) {
    List<ParseTreeNode> children = Lists.newArrayList();

    for (QuasiNode child : getChildren()) {
      if (!child.createSubstitutes(children, bindings)) { return false; }
    }

    if (Block.class.isAssignableFrom(clazz)) {
      // Move directive prologues to the front since they are only syntactically
      // significant there.
      for (int i = 1, n = children.size(); i < n; ++i) {
        ParseTreeNode child = children.get(i);
        if (child instanceof DirectivePrologue) {
          if (children.get(0) instanceof DirectivePrologue) {
            DirectivePrologue dp0 = (DirectivePrologue) children.get(0);
            DirectivePrologue dp1 = (DirectivePrologue) child;
            if (!dp1.children().isEmpty()) {
              List<Directive> all = new ArrayList<>();
              all.addAll(dp0.children());
              all.addAll(dp1.children());
              children.set(
                  0,
                  new DirectivePrologue(
                      FilePosition.span(
                          dp0.getFilePosition(), dp1.getFilePosition()),
                      all));
            }
            children.remove(i);
          } else {
            for (int j = i; j >= 1; --j) {
              children.set(j, children.get(j - 1));
            }
            children.set(0, child);
          }
        }
      }
    }

    ParseTreeNode node = ParseTreeNodes.newNodeInstance(
        clazz, FilePosition.UNKNOWN, value, children);
    substitutes.add(node);

    return true;
  }

  /** The class of node matched by this quasi node. */
  public Class<? extends ParseTreeNode> getMatchedClass() { return clazz; }

  public Object getValue() { return value; }

  @Override
  public String toString() {
    return clazz.getSimpleName() + (value == null ? "" : " : " + value);
  }
}
