// Copyright (C) 2006 Google Inc.
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
 * A parse tree node with mutating operations.
 *
 * @author mikesamuel@gmail.com (Mike Samuel)
 */
public interface MutableParseTreeNode extends ParseTreeNode {

  /**
   * {@inheritDoc}
   * <p>
   * As per {@link ParseTreeNode#acceptPreOrder}, but with the following caveats
   * relating to parse tree manipulation during visiting:
   * <p>Will work even if visitor {@link #replaceChild replaces} this node or a
   * descendant, {@link #removeChild removes} this node or a descendant, or
   * {@link #insertBefore inserts} a sibling before this node.</p>
   * <p>This implementation will have undefined behavior if the visitor modifies
   * ancestor nodes, or subsequent siblings of this node.
   */
  boolean acceptPreOrder(Visitor v, AncestorChain<?> ancestors);

  /**
   * {@inheritDoc}
   * <p>
   * As per {@link ParseTreeNode#acceptPostOrder}, but with the following
   * caveats relating to parse tree manipulation during visiting:
   * <p>Will work even if visitor {@link #replaceChild replaces} this node or a
   * descendant, {@link #removeChild removes} this node or a descendant, or
   * {@link #insertBefore inserts} a sibling before this node.</p>
   * <p>This implementation will have undefined behavior if the visitor modifies
   * ancestor nodes, or subsequent siblings of this node.
   */
  boolean acceptPostOrder(Visitor v, AncestorChain<?> ancestors);

  /**
   * Replace the given child of the current node with the given replacement.
   *
   * @param replacement a node that is not in a children list and is not an
   *   ancestor of this node.
   * @param child a node in {@link ParseTreeNode#children}.
   */
  void replaceChild(ParseTreeNode replacement, ParseTreeNode child);

  /**
   * Add the given child to the current node's child list before the given node.
   *
   * @param toAdd a node that is not in a children list and is not the
   *   root containing this node.
   * @param before a node in {@link ParseTreeNode#children}, or null to indicate
   *   add should happen at the end.
   */
  void insertBefore(ParseTreeNode toAdd, ParseTreeNode before);

  /**
   * Removes the given child from the current node's child list.
   *
   * @param toRemove a child of this node.
   */
  void removeChild(ParseTreeNode toRemove);

  /**
   * Add the given child to the current node's child list at the end.
   *
   * @param toAppend a node to add.
   */
  void appendChild(ParseTreeNode toAppend);

  /**
   * Allows multiple adds and removals when adding one at a time might leave the
   * tree in an inconsistent state.
   */
  Mutation createMutation();

  public static interface Mutation {

    /**
     * Replace the given child of the current node with the given replacement.
     * Does not take effect until {@link #execute} is called.
     *
     * @param replacement a node that is not in a children list and is not the
     *   root containing this node.
     * @param child a node in {@link ParseTreeNode#children}.
     * @return this
     */
    Mutation replaceChild(ParseTreeNode replacement, ParseTreeNode child);

    /**
     * Add the given child to the current node's child list.
     * Does not take effect until {@link #execute} is called.
     *
     * @param toAdd a node that is not in a children list and is not the
     *   root containing this node.
     * @param before a node in {@link ParseTreeNode#children}, or null to
     *   indicate add should happen at the end.
     * @return this
     */
    Mutation insertBefore(ParseTreeNode toAdd, ParseTreeNode before);

    /**
     * Remove the given child from the current node's child list.
     * Does not take effect until {@link #execute} is called.
     *
     * @param toRemove a child of this node.
     * @return this
     */
    Mutation removeChild(ParseTreeNode toRemove);

    /**
     * Add the given child to the current node's child list at the end.
     * Does not take effect until {@link #execute} is called.
     *
     * @param toAppend a child to add.
     */
    Mutation appendChild(ParseTreeNode toAppend);

    /**
     * Add the given child to the current node's child list at the end.
     * Does not take effect until {@link #execute} is called.
     *
     * @param toAppend children to add.
     */
    Mutation appendChildren(Iterable<? extends ParseTreeNode> toAppend);

    /**
     * Perform the mutation.
     */
    void execute();
  }
}
