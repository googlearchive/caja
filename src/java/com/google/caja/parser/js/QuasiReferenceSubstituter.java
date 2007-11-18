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

import com.google.caja.parser.AbstractParseTreeNode;
import com.google.caja.parser.ParseTreeNode;

import java.util.List;

/**
 * Visitor that finds Reference nodes with quasiliteral text values and
 * replaces them with a QuasiReference.
 *
 * @author ihab.awad@gmail.com
 */
public class QuasiReferenceSubstituter extends QuasiRewriteVisitor {
  @Override
  protected boolean visit(ParseTreeNode node, List<ParseTreeNode> parents) {
    if (nodeIsQuasiReference(node)) {
      buildQuasiReference(node, parents);
      return true;
    } else {
      return visitChildren(node, parents);
    }
  }

  private boolean nodeIsQuasiReference(ParseTreeNode node) {
    return
      node instanceof Reference && 
      node.children().size() == 0 &&
      isQuasiIdentifier(((Reference)node).getIdentifier());
  }
  
  private void buildQuasiReference(
      ParseTreeNode node,
      List<ParseTreeNode> parents) {
    assert(parents.size() > 0);
    AbstractParseTreeNode<?> ap = (AbstractParseTreeNode<?>)parents.get(0);
    ap.replaceChild(
        new QuasiReference(
            parseQuasiIdentifier(((Reference)node).getIdentifier()),
            parseQuasiQuantifier(((Reference)node).getIdentifier())),
        node);
  }
}
