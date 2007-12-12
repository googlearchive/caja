// Copyright 2007 Google Inc. All Rights Reserved.

package com.google.caja.parser;

/**
 * A lightweight stack that helps us keep track of ancestors as we traverse a
 * parse tree.
 *
 * @author msamuel@google.com (Mike Samuel)
 */
public final class AncestorChain<T extends ParseTreeNode> {
  public final AncestorChain<? extends ParseTreeNode> parent;
  public final T node;

  public AncestorChain(T node) {
    this(null, node);
  }

  public AncestorChain(AncestorChain<? extends ParseTreeNode> parent, T node) {
    if (node == null) { throw new NullPointerException(); }
    assert parent == null || parent.node.children().contains(node);
    this.parent = parent;
    this.node = node;
  }

  /** True if node is the first in its parent's child list, or is the root. */
  public boolean isFirstSibling() {
    return parent == null || parent.node.children().get(0) == node;
  }

  /** True if node is the last in its parent's child list, or is the root. */
  public boolean isLastSibling() {
    return parent == null || parent.node.children().get(
        parent.node.children().size() - 1) == node;
  }

  /** The previous sibling of parent or null. */
  public ParseTreeNode getPrevSibling() {
    int idx = indexInParent() - 1;
    if (idx < 0) { return null; }
    return parent.node.children().get(idx);
  }

  /** The next sibling of parent or null. */
  public ParseTreeNode getNextSibling() {
    int idx = indexInParent() + 1;
    if (idx <= 0 || idx >= parent.node.children().size()) { return null; }
    return parent.node.children().get(idx);
  }

  /** The index such that if node in its parent's children list or -1. */
  public int indexInParent() {
    return parent == null ? -1 : parent.node.children().indexOf(node);
  }

  public ParseTreeNode getParentNode() {
    return parent != null ? parent.node : null;
  }
  
  @SuppressWarnings("unchecked")
  public <T extends ParseTreeNode> AncestorChain<T> cast(Class<T> clazz) {
    if (!clazz.isInstance(node)) { throw new ClassCastException(); }
    return (AncestorChain<T>) this;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    toString(sb);
    return sb.toString();
  }

  private int toString(StringBuilder sb) {
    int depth = 0;
    if (parent != null) {
      depth = parent.toString(sb) + 1;
      sb.append('\n');
    }
    for (int d = depth; --d >= 0;) { sb.append("  "); }
    sb.append(node);
    return depth + 1;
  }
}
