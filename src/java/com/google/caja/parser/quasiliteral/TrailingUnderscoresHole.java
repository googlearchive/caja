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

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Quasiliteral "hole" matching an identifier containing trailing underscores.
 *
 * @author ihab.awad@gmail.com (Ihab Awad)
 */
public class TrailingUnderscoresHole extends AbstractQuasiHole {
  private final String trailing;

  public TrailingUnderscoresHole(String identifier, int numberOfUnderscores) {
    super(Identifier.class, identifier);
    StringBuffer b = new StringBuffer();
    for (int i = 0; i < numberOfUnderscores; i++) b.append("_");
    trailing = b.toString();
  }

  protected boolean consumeSpecimens(
      List<ParseTreeNode> specimens,
      Map<String, ParseTreeNode> bindings) {
    if (specimens.size() > 0 && isCompatibleClass(specimens.get(0))) {
      String value = ((Identifier)specimens.get(0)).getValue();
      if (value.endsWith(trailing)) {
        specimens.remove(0);
        return putIfDeepEquals(
            bindings,
            getIdentifier(),
            ParseTreeNodes.newNodeInstance(
                Identifier.class,
                value.substring(
                    0,
                    value.length() - trailing.length()),
                Collections.<ParseTreeNode>emptyList()));
      }
    }
    return false;
  }
  
  protected boolean createSubstitutes(
      List<ParseTreeNode> substitutes,
      Map<String, ParseTreeNode> bindings) {
    ParseTreeNode n = bindings.get(getIdentifier());
    if (n == null || !(n instanceof Identifier)) return false;
    substitutes.add(
        ParseTreeNodes.newNodeInstance(
            Identifier.class,
            ((Identifier)n).getValue() + trailing,
            Collections.<ParseTreeNode>emptyList()));
    return true;
  }
  
  protected String getQuantifierSuffix() { throw new UnsupportedOperationException(); }

  public String toString() {
    return "(Identifier) : @${" + getIdentifier() + "}" + trailing;
  }  
}
