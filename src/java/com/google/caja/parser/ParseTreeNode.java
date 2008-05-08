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

import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.Token;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.Renderable;
import com.google.caja.util.SyntheticAttributes;
import com.google.caja.util.SyntheticAttributeKey;

import java.io.IOException;
import java.util.List;

/**
 * A node in a parse tree.
 *
 * @author mikesamuel@gmail.com
 */
public interface ParseTreeNode extends MessagePart, Renderable, Cloneable {

  /**
   * Attribute key that marks a node as being "tainted" in some way.
   *
   * This is used to ensure that all nodes have been "seen" by a security checking
   * or other critical process. The input is first marked "tainted", and the code is
   * written such that any concrete step that recognizes a node un-taints it (or
   * produces an un-tainted result as the output). The process can then check to make
   * sure that all its output is not tainted.
   */
  SyntheticAttributeKey<Boolean> TAINTED
      = new SyntheticAttributeKey<Boolean>(Boolean.class, "tainted");

  FilePosition getFilePosition();
  List<Token<?>> getComments();
  /**
   * @return null or a value with subclass specific meaning which encapsulates
   *     all parsed state separate from the children.
   */
  Object getValue();
  /**
   * A set of properties that may be used by visitors and inspectors to
   * store information computed about a node such as it's type, the symbol
   * it refers to, etc.
   */
  SyntheticAttributes getAttributes();

  void formatTree(MessageContext context, int depth, Appendable out)
      throws IOException;

  /** An immutable list of children. */
  List<? extends ParseTreeNode> children();

  /**
   * Applies the given visitor to children in a pre-order traversal, skipping
   * traversal of a subtree if {@link Visitor#visit} of the root node returns
   * false.
   *
   * @param v the visitor to apply.
   * @param ancestors an initial set of ancestor nodes not containing this.
   * @return true iff visiting the root node yielded true.
   */
  boolean acceptPreOrder(Visitor v, AncestorChain<?> ancestors);

  /**
   * Like {@link #acceptPreOrder}, but post-order.
   *
   * @param v the visitor to apply.
   * @param ancestors an initial set of ancestor nodes not containing this.
   * @return true iff visiting the root node yielded true.
   */
  boolean acceptPostOrder(Visitor v, AncestorChain<?> ancestors);

  /**
   * Create a deep clone of this {@code ParseTreeNode}.
   *
   * @return a deep clone of the node tree rooted at this {@code ParseTreeNode}.
   */
  ParseTreeNode clone();
}
