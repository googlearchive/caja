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

package com.google.caja.parser.js;

import java.util.ArrayList;
import java.util.List;

import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.Visitor;
import com.google.caja.parser.ParseTreeNode.QuasiliteralQuantifier;

/**
 * Superclass of all Visitors that transform a parsed tree without quasiliteral
 * nodes to one containing instances of specific quasiliteral subclasses.
 *
 * @author ihab.awad@gmail.com
 */
public abstract class QuasiRewriteVisitor implements Visitor {
  
  public boolean visit(ParseTreeNode node) {
    return visit(node, new ArrayList<ParseTreeNode>());
  }
  
  protected abstract boolean visit(ParseTreeNode node, List<ParseTreeNode> parents);
  
  protected boolean visitChildren(ParseTreeNode node, List<ParseTreeNode> parents) {
    parents.add(0, node);
    boolean result = true;
    for (int i = 0; i < node.children().size() && result; i++)
      result = result && visit(node.children().get(i), parents);
    parents.remove(0);
    return result;
  }
  
  protected boolean isQuasiIdentifier(String identifier) {
    return identifier.startsWith("@"); 
  }
  
  protected String parseQuasiIdentifier(String literal) {
    assert(literal.startsWith("@"));
    if (literal.endsWith("*") || literal.endsWith("+"))
      return literal.substring(1, literal.length() - 1);
    return literal.substring(1, literal.length());    
  }

  protected QuasiliteralQuantifier parseQuasiQuantifier(String literal) {
    if (literal.endsWith("*")) return QuasiliteralQuantifier.MULTIPLE;
    if (literal.endsWith("+")) return QuasiliteralQuantifier.MULTIPLE_NONEMPTY;
    return QuasiliteralQuantifier.SINGLE;
  }
}
