// Copyright (C) 2005-2006 Google Inc.
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
import com.google.caja.util.SyntheticAttributeKey;
import com.google.caja.util.SyntheticAttributes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * An abstract base class for a mutable parse tree node implementations.
 *
 * @param <T> A base class for all children of this node.
 * @author mikesamuel@gmail.com
 */
public abstract class AbstractParseTreeNode<T extends ParseTreeNode>
    implements MutableParseTreeNode {
  private FilePosition pos;
  private List<Token<?>> comments = Collections.<Token<?>>emptyList();
  private SyntheticAttributes attributes;
  /**
   * The list of children.  This can be appended to for efficient initialization
   * but any operations that remove or insert except at the end require
   * copy-on-write to provide for efficient visitors.
   */
  private List<T> children = new ArrayList<T>();
  private List<T> childrenExtern = Collections.<T>unmodifiableList(children);

  protected AbstractParseTreeNode() {
    pos = FilePosition.UNKNOWN;
    // initialized via mutators
  }

  public FilePosition getFilePosition() { return pos; }
  public List<Token<?>> getComments() { return comments; }
  public final List<? extends T> children() { return childrenExtern; }

  @SuppressWarnings("unchecked")
  protected <T2 extends T> List<T2> childrenPart(
      int start, int end, Class<T2> cl) {
    List<T> sub = children.subList(start, end);
    for (T el : sub) {
      if (!cl.isInstance(el)) {
        throw new ClassCastException(
            "element not an instance of " + cl + " : "
            + (null != el ? el.getClass() : "<null>"));
      }
    }
    return Collections.unmodifiableList((List<T2>) sub);
  }

  public abstract Object getValue();
  public SyntheticAttributes getAttributes() {
    if (null == this.attributes) {
      this.attributes = new SyntheticAttributes();
    }
    return this.attributes;
  }
  public void setFilePosition(FilePosition pos) {
    this.pos = (pos == null) ? FilePosition.UNKNOWN : pos;
  }
  @SuppressWarnings("unchecked")
  public void setComments(List<? extends Token> comments) {
    List<Token<?>> tokens = (List<Token<?>>) comments;
    this.comments = !comments.isEmpty()
        ? Collections.unmodifiableList(new ArrayList<Token<?>>(tokens))
        : Collections.<Token<?>>emptyList();
  }

  public void replaceChild(ParseTreeNode replacement, ParseTreeNode child) {
    createMutation().replaceChild(replacement, child).execute();
  }

  public void insertBefore(ParseTreeNode toAdd, ParseTreeNode before) {
    createMutation().insertBefore(toAdd, before).execute();
  }

  public void appendChild(ParseTreeNode toAppend) {
    insertBefore(toAppend, null);
  }

  public void removeChild(ParseTreeNode toRemove) {
    createMutation().removeChild(toRemove).execute();
  }

  public Mutation createMutation() { return new MutationImpl(); }

  @SuppressWarnings("unchecked")
  private void setChild(int i, ParseTreeNode child) {
    children.set(i, (T) child);
  }

  @SuppressWarnings("unchecked")
  private void addChild(int i, ParseTreeNode child) {
    children.add(i, (T) child);
  }

  private void copyOnWrite() {
    children = new ArrayList<T>(children);
    childrenExtern = Collections.<T>unmodifiableList(children);
  }

  private int indexOf(ParseTreeNode child) {
    return children.indexOf(child);
  }

  /**
   * Called to perform consistency checks on the child list after changes have
   * been made.  This can be overridden to do additional checks by subclasses,
   * and to update derived state, but all subclasses must chain to super after
   * performing their own checks.
   *
   * <p>This method may throw any RuntimeException on an invalid child.
   * TODO(mikesamuel): maybe reliably throw an exception type, that includes
   * information about the troublesome node.</p>
   */
  protected void childrenChanged() {
    if (children.contains(null)) { throw new NullPointerException(); }
  }

  protected void formatSelf(MessageContext context, Appendable out)
      throws IOException {
    String cn = this.getClass().getName();
    cn = cn.substring(cn.lastIndexOf(".") + 1);
    cn = cn.substring(cn.lastIndexOf("$") + 1);
    out.append(cn);
    Object value = getValue();
    if (null != value) {
      out.append(" : ");
      if (value instanceof MessagePart) {
        ((MessagePart) value).format(context, out);
      } else {
        out.append(value.toString());
      }
    }
    if (!context.relevantKeys.isEmpty() && null != attributes) {
      for (SyntheticAttributeKey<?> k : context.relevantKeys) {
        if (attributes.containsKey(k)) {
          out.append(" ; ").append(k.getName()).append('=');
          Object attribValue = attributes.get(k);
          if (attribValue instanceof MessagePart) {
            ((MessagePart) attribValue).format(context, out);
          } else {
            out.append(String.valueOf(attribValue));
          }
        }
      }
    }
  }

  public void format(MessageContext context, Appendable out)
      throws IOException {
    formatTree(context, out);
  }

  public final void formatTree(MessageContext context, Appendable out)
      throws IOException {
    formatTree(context, 0, out);
  }

  public final void formatTree(
      MessageContext context, int depth, Appendable out)
      throws IOException {
    for (int d = depth; --d >= 0;) { out.append("  "); }
    formatSelf(context, out);
    for (ParseTreeNode child : children()) {
      out.append("\n");
      child.formatTree(context, depth + 1, out);
    }
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    try {
      formatSelf(new MessageContext(), sb);
    } catch (IOException ex) {
      throw new AssertionError("StringBuilders shouldn't throw IOExceptions");
    }
    return sb.toString();
  }

  public String toStringDeep() { return toStringDeep(0); }

  public String toStringDeep(int d) {
    StringBuilder sb = new StringBuilder();
    try {
      formatTree(new MessageContext(), d, sb);
    } catch (IOException ex) {
      throw new AssertionError("StringBuilders shouldn't throw IOExceptions");
    }
    return sb.toString();
  }

  @Deprecated
  public final boolean equivalentTo(ParseTreeNode that) {
    Object valueA = this.getValue(), valueB = that.getValue();
    if (!(null != valueA ? valueA.equals(valueB) : null == valueB)) {
      return false;
    }

    List<? extends ParseTreeNode> aChildren = this.children();
    List<? extends ParseTreeNode> bChildren = that.children();
    int n = aChildren.size();
    if (n != bChildren.size()) { return false; }
    while (--n >= 0) {
      if (!aChildren.get(n).equals(bChildren.get(n))) {
        return false;
      }
    }
    return true;
  }

  private enum TraversalType { PREORDER, POSTORDER; }

  private boolean visitChildren(
       Visitor v, AncestorChain<?> ancestors, TraversalType traversalType) {
    if (this.children.isEmpty()) { return true; }

    boolean result = true;
    // This loop is complicated because it needs to survive mutations to the
    // child list.
    List<? extends ParseTreeNode> childrenCache = this.children;

    ParseTreeNode next = childrenCache.get(0);
    childLoop:
    for (int i = 0; i < childrenCache.size(); ++i) {
      if (childrenCache != this.children) {
        // Used lastIndexOf so we make progress in case a child is on the
        // children list multiple times.
        int j = this.children.lastIndexOf(next);
        if (j < 0) {
          // Try to find the next one to use by looking at children we've
          // already visited.
          for (int k = i; --k >= 0;) {
            j = this.children.lastIndexOf(childrenCache.get(k));
            if (j >= 0) { break; }
          }
          if (j >= 0 && j < this.children.size()) {
            ++j;  // Add one since we don't want to reprocess childrenCache[k].
          } else {
            // Check if children from the cached list that we haven't
            // processed yet are still in the new list.
            for (int k = i + 1; k < childrenCache.size(); ++k) {
              j = this.children.lastIndexOf(childrenCache.get(k));
              if (j >= 0) { break; }
            }
            // No children left to process.
            if (j < 0) { break childLoop; }
          }
        }
        i = j;
        childrenCache = this.children;
        next = childrenCache.get(i);
      }

      ParseTreeNode child = next;
      next = (i + 1 < childrenCache.size() ? childrenCache.get(i + 1) : null);
      switch (traversalType) {
        case PREORDER:
          child.acceptPreOrder(v, ancestors);
          break;
        case POSTORDER:
          if (!child.acceptPostOrder(v, ancestors)) {
            result = false;
            break childLoop;
          }
          break;
      }
    }
    return result;
  }

  private boolean stillInParent(AncestorChain<?> ancestors) {
    // If ancestors is empty, then it can't have been removed from its parent
    // by the Visitor unless the visitor has some handle to the parent through
    // another mechanism.
    return ancestors == null || ancestors.node.children().contains(this);
  }

  public final boolean acceptPreOrder(Visitor v, AncestorChain<?> ancestors) {
    ancestors = new AncestorChain<AbstractParseTreeNode<T>>(ancestors, this);
    if (!v.visit(ancestors)) { return false; }

    // Handle the case where v.visit() replaces this with another, inserts
    // another following, or deletes the node or a following node.
    if (!stillInParent(ancestors.parent)) { return true; }

    // Not removed or replaced, so recurse to children.
    visitChildren(v, ancestors, TraversalType.PREORDER);
    return true;
  }

  public final boolean acceptPostOrder(Visitor v, AncestorChain<?> ancestors) {
    ancestors = new AncestorChain<AbstractParseTreeNode<T>>(ancestors, this);
    // Descend into this node's children.
    if (!visitChildren(v, ancestors, TraversalType.POSTORDER)) {
      return false;
    }

    // If this node has been orphaned, don't visit it...
    if (stillInParent(ancestors.parent)) {
      return v.visit(ancestors);
    }

    return true;
  }

  /** Uses identity hash code since this is mutable. */
  @Override
  public final int hashCode() { return super.hashCode(); }

  /** Uses identity hash code since this is mutable. */
  @Override
  public final boolean equals(Object o) { return this == o; }

  @Override
  public ParseTreeNode clone() {
    List<ParseTreeNode> clonedChildren
        = new ArrayList<ParseTreeNode>(children.size());
    for (ParseTreeNode child : children) {
      clonedChildren.add(child.clone());
    }
    ParseTreeNode cloned = ParseTreeNodes.newNodeInstance(
        getClass(), getValue(), clonedChildren);
    ((AbstractParseTreeNode<?>) cloned).setFilePosition(getFilePosition());
    cloned.getAttributes().putAll(getAttributes());
    return cloned;
  }

  private final class MutationImpl implements MutableParseTreeNode.Mutation {

    private List<Change> changes = new ArrayList<Change>();

    public Mutation replaceChild(
        ParseTreeNode replacement, ParseTreeNode child) {
      changes.add(new Replacement(replacement, child));
      return this;
    }

    public Mutation insertBefore(ParseTreeNode toAdd, ParseTreeNode before) {
      changes.add(new Insertion(toAdd, before));
      return this;
    }

    public Mutation appendChild(ParseTreeNode toAppend) {
      return insertBefore(toAppend, null);
    }

    public Mutation appendChildren(Iterable<? extends ParseTreeNode> nodes) {
      for (ParseTreeNode node : nodes) {
        changes.add(new Insertion(node, null));
      }
      return this;
    }

    public Mutation removeChild(ParseTreeNode toRemove) {
      changes.add(new Removal(toRemove));
      return this;
    }

    @SuppressWarnings("finally")
    public void execute() {
      boolean copied = false;
      for (Change change : changes) {
        copied = change.apply(copied);
      }
      try {
        childrenChanged();
      } catch (RuntimeException ex) {
        for (int i = changes.size(); --i >= 0;) { changes.get(i).rollback(); }
        try {
          childrenChanged();
        } finally {
          throw ex;
        }
      }
    }
  }

  private abstract class Change {

    /**
     * Index of modified child in original set by apply, so that we can
     * rollback.
     */
    int backupIndex = -1;

    /**
     * Change the parse tree and store enough information so that rollback can
     * reverse it.
     * @param copied true if the children list has already been copied by an
     *     operation that requires copy on write.
     * @return true if the children list has been copied by an operation
     *     that requires copy on write.
     */
    abstract boolean apply(boolean copied);

    /**
     * Rolls back the change effected by apply, and can assume that apply
     * was the most recent change to this node, and that it will be called
     * at most once after a given apply.
     */
    abstract void rollback();
  }

  private final class Replacement extends Change {
    private final ParseTreeNode replacement;
    private final ParseTreeNode replaced;

    Replacement(ParseTreeNode replacement, ParseTreeNode replaced) {
      this.replacement = replacement;
      this.replaced = replaced;
    }

    @Override
    boolean apply(boolean copied) {
      if (!copied) { copyOnWrite(); }

      // Find where to insert
      int childIndex = indexOf(replaced);
      if (childIndex < 0) {
        throw new NoSuchElementException(
            "Node to replace is not a child of this node.");
      }

      if (indexOf(replacement) >= 0) {
        throw new NoSuchElementException(
            "Node to add is already a child of this node.");
      }

      // Update the child list
      backupIndex = childIndex;
      setChild(childIndex, replacement);

      return true;
    }

    @Override
    void rollback() {
      int childIndex = backupIndex;

      // This check corresponds to the replacement.parent == null check in apply
      // which has the effect of asserting that replacement is not rooted.
      if (children.contains(replaced)) { return; }

      setChild(childIndex, replaced);  // roll back
    }
  }

  private final class Removal extends Change {
    private final ParseTreeNode toRemove;

    Removal(ParseTreeNode toRemove) {
      this.toRemove = toRemove;
    }

    @Override
    boolean apply(boolean copied) {
      if (!copied) { copyOnWrite(); }

      // Find which to remove
      int childIndex = indexOf(toRemove);
      if (childIndex < 0) {
        throw new NoSuchElementException("child not in parent");
      }

      // Update the child list
      backupIndex = childIndex;
      children.remove(childIndex);

      return true;
    }

    @Override
    void rollback() {
      if (children.contains(toRemove)) { return; }

      addChild(backupIndex, toRemove);
    }
  }

  private final class Insertion extends Change {
    private final ParseTreeNode toAdd;
    private final ParseTreeNode before;

    Insertion(ParseTreeNode toAdd, ParseTreeNode before) {
      this.toAdd = toAdd;
      this.before = before;
    }

    @Override
    boolean apply(boolean copied) {
      // Find where to insert
      int childIndex;
      if (null == before) {
        childIndex = children.size();
      } else {
        childIndex =  indexOf(before);
        if (childIndex < 0) {
          throw new NoSuchElementException("Child not in parent");
        }
        if (!copied) {
          copyOnWrite();
          copied = true;
        }
      }

      // Update the child list
      backupIndex = childIndex;
      addChild(childIndex, toAdd);

      return copied;
    }

    @Override
    void rollback() {
      int childIndex = backupIndex;

      ParseTreeNode removed = children.remove(childIndex);
      if (removed != toAdd) {
        setChild(childIndex, removed);
        throw new IllegalStateException();
      }
    }
  }
}
