// Copyright (C) 2010 Google Inc.
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
import com.google.caja.parser.js.ObjProperty;
import com.google.caja.parser.js.ObjectConstructor;
import com.google.common.collect.Lists;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

final class ObjectCtorQuasiNode extends QuasiNode {
  ObjectCtorQuasiNode(QuasiNode... propertyQuasis) {
    super(propertyQuasis);
  }

  @Override
  protected boolean consumeSpecimens(
      List<ParseTreeNode> specimens, Map<String, ParseTreeNode> bindings) {
    if (specimens.isEmpty()) { return false; }
    ParseTreeNode specimen = specimens.get(0);
    if (!(specimen instanceof ObjectConstructor)) { return false; }
    ObjectConstructor obj = (ObjectConstructor) specimen;
    List<ParseTreeNode> parts = Lists.<ParseTreeNode>newLinkedList(
        obj.children());
    MultiPropertyQuasi hole = null;
    for (QuasiNode q : getChildren()) {
      if (q instanceof MultiPropertyQuasi) {
        hole = (MultiPropertyQuasi) q;
      } else {
        if (!q.consumeSpecimens(parts, bindings)) { return false; }
      }
    }
    if (hole != null && !hole.consumeSpecimens(parts, bindings)) {
      return false;
    }
    if (!parts.isEmpty()) { return false; }
    specimens.remove(0);
    return true;
  }

  @Override
  protected boolean createSubstitutes(
      List<ParseTreeNode> substitutes, Map<String, ParseTreeNode> bindings) {
    List<ParseTreeNode> props = Lists.newArrayList();
    for (QuasiNode q : getChildren()) {
      if (!q.createSubstitutes(props, bindings)) { return false; }
    }
    ObjProperty[] propArr = new ObjProperty[props.size()];
    props.toArray(propArr);
    substitutes.add(new ObjectConstructor(
        FilePosition.UNKNOWN, Arrays.asList(propArr)));
    return true;
  }
}
