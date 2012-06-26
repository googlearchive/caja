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

import java.io.IOException;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

/**
 * A node in a parse tree.
 *
 * @author mikesamuel@gmail.com
 */
public interface ParseTreeNode extends MessagePart, Renderable, Cloneable {

  /**
   * Freeze this node deeply, making it and all its children immutable.
   *
   * @return true iff the node and all its children have been successfully
   * made immutable.
   */
  boolean makeImmutable();

  /**
   * @return whether this node and all its children are immutable.
   */
  boolean isImmutable();

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
   * Try to avoid this; it uses O(n**2) time and O(n) space (worst case).
   * <p>
   * Applies the given visitor to children in a pre-order traversal, skipping
   * traversal of a subtree if {@link Visitor#visit} of the root node returns
   * false.
   * <p>
   * TODO(felix8a): eliminate uses of acceptPreOrder/acceptPostOrder
   *
   * @param v the visitor to apply.
   * @param ancestors an initial set of ancestor nodes not containing this.
   * @return true iff visiting the root node yielded true.
   */
  boolean acceptPreOrder(Visitor v, AncestorChain<?> ancestors);

  /**
   * Try to avoid this; it uses O(n**2) time and O(n) space (worst case).
   * Like {@link #acceptPreOrder}, but post-order.
   *
   * @param v the visitor to apply.
   * @param ancestors an initial set of ancestor nodes not containing this.
   * @return true iff visiting the root node yielded true.
   */
  boolean acceptPostOrder(Visitor v, AncestorChain<?> ancestors);

  /**
   * Call {@code visit(node)} on every node in the tree in pre-order.
   * The visit function is allowed to modify node and its children,
   * but it shouldn't modify node's parent or siblings.
   * If visit returns false, traversal skips the node's children.
   *
   * @return false iff visit returns false for this node.
   */
  boolean visitPreOrder(ParseTreeNodeVisitor v);

  /**
   * Create a deep clone of this {@code ParseTreeNode}.
   *
   * @return a deep clone of the node tree rooted at this {@code ParseTreeNode}.
   */
  ParseTreeNode clone();

  /**
   * Indicates that the annotated constructor of a {@code ParseTreeNode} is to
   * be used for reflectively constructing a new node or cloning an existing
   * node.
   */
  @Documented
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.CONSTRUCTOR)
  public @interface ReflectiveCtor { /* no properties */ }
}
