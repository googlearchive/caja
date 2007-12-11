// Copyright (C) 2005 Google Inc.
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

package com.google.caja.parser;

/**
 * A visitor pattern for processing DOM subtrees.
 * Receives nodes via the {@link ParseTreeNode#acceptPreOrder pre-order} and
 * {@link ParseTreeNode#acceptPreOrder post-order} traversals.
 *
 * @author mikesamuel@gmail.com
 */
public interface Visitor {

  /**
   * Called on each node reached by the traversal to process that node.
   *
   * @param chain wraps the node to process, and its ancestors.  This is non
   *    null, and the node to process is {@code chain.node}.
   * @return whether to continue the traversal.  Returning false will end the
   *    traversal early.
   */
  public boolean visit(AncestorChain<?> chain);
}
