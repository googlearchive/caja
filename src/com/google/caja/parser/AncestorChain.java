// Copyright 2007 Google Inc. All Rights Reserved.

package com.google.caja.parser;

/**
 * A lightweight stack that helps us keep track of ancestors as we traverse a
 * parse tree.
 *
 * @author mikesamuel@gmail.com (Mike Samuel)
 */
public final class AncestorChain<T extends ParseTreeNode> {
  public final AncestorChain<? extends ParseTreeNode> parent;
  public final T node;
  public final int depth;

  private AncestorChain(AncestorChain<? extends ParseTreeNode> parent, T node) {
    if (node == null) { throw new NullPointerException(); }
    assert parent == null || parent.node.children().contains(node);
    this.parent = parent;
    this.node = node;
    this.depth = parent == null ? 0 : parent.depth + 1;
  }

  public static <T extends ParseTreeNode> AncestorChain<T> instance(T node) {
    return new AncestorChain<T>(null, node);
  }

  public static <T extends ParseTreeNode> AncestorChain<T> instance(
       AncestorChain<? extends ParseTreeNode> parent, T node) {
    return new AncestorChain<T>(parent, node);
  }

  public <C extends ParseTreeNode> AncestorChain<C> child(C child) {
    return instance(this, child);
  }

  public ParseTreeNode getParentNode() {
    return parent != null ? parent.node : null;
  }

  @SuppressWarnings("unchecked")
  public <C extends ParseTreeNode> AncestorChain<C> cast(Class<C> clazz) {
    if (!clazz.isInstance(node)) {
      throw new ClassCastException(node.getClass().getSimpleName());
    }
    return (AncestorChain<C>) this;
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
    return depth;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof AncestorChain<?>)) { return false; }
    AncestorChain<?> a = this, b = (AncestorChain<?>) o;
    if (a.depth != b.depth) { return false; }
    do {
      if (a.node != b.node) { return false; }
      if (a.hc != 0 && b.hc != 0 && a.hc != b.hc) { return false; }
      a = a.parent;
      b = b.parent;
      if (a == b) { return true; }
    } while (a != null && b != null);
    return false;
  }

  private int hc;
  @Override
  public int hashCode() {
    if (this.hc == 0) {
      int hc = (parent != null ? 31 * parent.hashCode() : 0)
          + System.identityHashCode(node);
      if (hc == 0) { hc = -1; }
      this.hc = hc;
    }
    return this.hc;
  }
}
