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

import java.util.Map;

/**
 * Superclass of all quasiliteral "hole" nodes.
 * 
 * @author ihab.awad@gmail.com (Ihab Awad)
 */
public abstract class QuasiHole extends QuasiNode {
  private final Class<? extends ParseTreeNode> matchedClass;
  private final String identifier;

  protected QuasiHole(Class<? extends ParseTreeNode> matchedClass, String identifier) {
    super();
    this.matchedClass = matchedClass;
    this.identifier = identifier;
  }

  protected String getIdentifier() {
    return identifier;
  }

  protected boolean isCompatibleClass(ParseTreeNode specimen) {
    return matchedClass.isAssignableFrom(specimen.getClass());
  }

  protected abstract String getQuantifierSuffix();

  protected static boolean putIfDeepEquals(
      Map<String, ParseTreeNode> bindings,
      String key,
      ParseTreeNode value) {
    if (bindings.containsKey(key)) return ParseTreeNodes.deepEquals(value, bindings.get(key));
    bindings.put(key, value);
    return true;
  }

  public String toString() {
    return
        "(" + (matchedClass == null ? "<any>" : matchedClass.getSimpleName()) + ")" +
        " : @" + getIdentifier() + getQuantifierSuffix();
  }
}