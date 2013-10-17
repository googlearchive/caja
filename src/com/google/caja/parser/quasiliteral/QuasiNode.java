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
import com.google.caja.parser.js.Identifier;
import com.google.caja.parser.js.StringLiteral;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A quasiliteral node that can match trees and substitute into trees of
 * {@link com.google.caja.parser.ParseTreeNode} objects, as parsed by the
 * Caja JavaScript {@link com.google.caja.parser.js.Parser}.
 *
 * @author ihab.awad@gmail.com (Ihab Awad)
 */
public abstract class QuasiNode {
  private final List<QuasiNode> children;

  protected QuasiNode(QuasiNode... children) {
    this.children = Collections.unmodifiableList(Arrays.asList(children));
  }

  public List<QuasiNode> getChildren() { return children; }

  public Map<String, ParseTreeNode> match(ParseTreeNode specimen) {
    List<ParseTreeNode> specimens = Lists.newArrayList();
    specimens.add(specimen);
    Map<String, ParseTreeNode> bindings = Maps.newLinkedHashMap();
    return consumeSpecimens(specimens, bindings) ? bindings : null;
  }

  public ParseTreeNode substitute(Map<String, ParseTreeNode> bindings) {
    List<ParseTreeNode> results = new ArrayList<ParseTreeNode>();
    return (createSubstitutes(results, bindings) && results.size() == 1)
        ? results.get(0) : null;
  }

  protected abstract boolean consumeSpecimens(
      List<ParseTreeNode> specimens,
      Map<String, ParseTreeNode> bindings);

  protected abstract boolean createSubstitutes(
      List<ParseTreeNode> substitutes,
      Map<String, ParseTreeNode> bindings);

  public String render() {
    return render(0);
  }

  private String render(int level) {
    StringBuilder result = new StringBuilder();
    for (int i = 0; i < level; i++) result.append("  ");
    result.append(this.toString());
    result.append("\n");
    for (QuasiNode child : getChildren()) result.append(child.render(level + 1));
    return result.toString();
  }

  protected static boolean safeEquals(Object x, Object y) {
    return x == y || (x != null && x.equals(y));
  }

  protected static boolean putIfDeepEquals(
      Map<String, ParseTreeNode> bindings,
      String key,
      ParseTreeNode value) {
    if (bindings.containsKey(key)) return ParseTreeNodes.deepEquals(value, bindings.get(key));
    // TODO(ihab.awad): As a special case, an Identifier with a null value is
    // considered to not match anything, so we reject it. See the following:
    // http://code.google.com/p/google-caja/issues/detail?id=397
    if (value instanceof Identifier && value.getValue() == null) return false;
    bindings.put(key, value);
    return true;
  }

  interface Equivalence {
    boolean equivalent(Object a, Object b);
  }

  static final Equivalence SAFE_EQUALS = new Equivalence() {
    public boolean equivalent(Object a, Object b) { return safeEquals(a, b); }
  };

  static final Equivalence EQUAL_UNESCAPED = new Equivalence() {
    public boolean equivalent(Object a, Object b) {
      return StringLiteral.getUnquotedValueOf((String) a)
          .equals(StringLiteral.getUnquotedValueOf((String) b));
    }
  };
}
