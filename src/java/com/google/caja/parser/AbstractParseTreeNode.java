// Copyright (C) 2005 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.caja.parser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.Token;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessagePart;
import com.google.caja.util.SyntheticAttributeKey;
import com.google.caja.util.SyntheticAttributes;

/**
 * An abstract base class for a mutable parse tree node implementations.
 * 
 * @param <T> A base class for all children of this node.
 * @author mikesamuel@gmail.com
 * @author ihab.awad@gmail.com
 */
public abstract class AbstractParseTreeNode<T extends ParseTreeNode> implements
    MutableParseTreeNode {
  private AbstractParseTreeNode<?> parent;
  private AbstractParseTreeNode<?> nextSibling, prevSibling;
  private FilePosition pos;
  private List<Token<?>> comments = Collections.<Token<?>> emptyList();
  private SyntheticAttributes attributes;
  // TODO(mikesamuel): make this private and let children use transactions to
  // set up child lists from the constructor.
  protected final List<T> children = new ArrayList<T>();
  private List<T> childrenExtern;

  protected AbstractParseTreeNode() {
    // initialized via setters
  }

  /**
   * Return the parent of this node, a node such that
   * <code>getParent().iterator()</code> will include <code>this</code>.
   * Not valid until {@link #parentify()} has been called on the root node.
   * 
   * @return null if {@link #parentify()} has not yet been called or if this is
   *         the root node.
   */
  public ParseTreeNode getParent() {
    return parent;
  }

  public ParseTreeNode getNextSibling() {
    return nextSibling;
  }

  public ParseTreeNode getPrevSibling() {
    return prevSibling;
  }

  public FilePosition getFilePosition() {
    return pos;
  }

  public List<Token<?>> getComments() {
    return comments;
  }

  public final List<? extends T> children() {
    return childrenExtern;
  }
  
  @SuppressWarnings("unchecked")
  protected <T2 extends T> List<T2> childrenPart(int start, int end,
      Class<T2> cl) {
    List<T> sub = children.subList(start, end);
    for (T el : sub) {
      if (!cl.isInstance(el)) {
        throw new ClassCastException("element not an instance of " + cl + " : "
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
    this.pos = pos;
  }

  @SuppressWarnings("unchecked")
  public void setComments(List<? extends Token> comments) {
    List<Token<?>> tokens = (List<Token<?>>) comments;
    this.comments =
        !comments.isEmpty() ? Collections
            .unmodifiableList(new ArrayList<Token<?>>(tokens)) : Collections
            .<Token<?>> emptyList();
  }

  // TODO(mikesamuel): remove this and make sure the parent link is consistently
  // maintained.
  /** Initializes the {@link #getParent} references. */
  public void parentify() {
    parentify(true);
  }

  protected void parentify(boolean recurse) {
    AbstractParseTreeNode<?> prev = null;
    for (ParseTreeNode child : children()) {
      AbstractParseTreeNode<?> achild = (AbstractParseTreeNode<?>) child;
      if (this != achild.parent) {
        assert achild.parent == null;
        achild.parent = this;
        if (null != (achild.prevSibling = prev)) {
          prev.nextSibling = achild;
        }
        if (recurse) {
          achild.parentify(true);
        }
        prev = achild;
      }
    }
    if (null != prev) {
      prev.nextSibling = null;
    }
  }

  private AbstractParseTreeNode getRoot() {
    AbstractParseTreeNode t = this;
    while (t.parent != null) {
      t = t.parent;
    }
    return t;
  }

  public void replaceChild(ParseTreeNode replacement, ParseTreeNode child) {
    createMutation().replaceChild(replacement, child).execute();
  }

  public void insertBefore(ParseTreeNode toAdd, ParseTreeNode before) {
    createMutation().insertBefore(toAdd, before).execute();
  }

  public void removeChild(ParseTreeNode toRemove) {
    createMutation().removeChild(toRemove).execute();
  }

  public Mutation createMutation() {
    return new MutationImpl();
  }

  @SuppressWarnings("unchecked")
  private void setChild(int i, AbstractParseTreeNode<?> child) {
    children.set(i, (T) child);
  }

  @SuppressWarnings("unchecked")
  private void addChild(int i, AbstractParseTreeNode<?> child) {
    children.add(i, (T) child);
  }

  private int indexOf(AbstractParseTreeNode<?> child) {
    return children.indexOf(child);
  }

  /**
   * Called to perform consistency checks on the child list after changes have
   * been made. This can be overridden to do additional checks by subclasses,
   * and to update derived state, but all subclasses must chain to super after
   * performing their own checks.
   * 
   * <p>
   * This method may throw any RuntimeException on an invalid child.
   * TODO(mikesamuel): maybe reliably throw an exception type, that includes
   * information about the troublesome node.
   * </p>
   */
  protected void childrenChanged() {
    childrenExtern = Collections.<T> unmodifiableList(children);
    if (children.contains(null)) {
      throw new NullPointerException();
    }
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

  public void format(MessageContext context, Appendable out) throws IOException {
    formatTree(context, out);
  }

  public final void formatTree(MessageContext context, Appendable out)
      throws IOException {
    formatTree(context, 0, out);
  }

  public final void formatTree(MessageContext context, int depth, Appendable out)
      throws IOException {
    for (int d = depth; --d >= 0;) {
      out.append("  ");
    }
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

  @Deprecated
  public final boolean equivalentTo(ParseTreeNode that) {
    Object valueA = this.getValue(), valueB = that.getValue();
    if (!(null != valueA ? valueA.equals(valueB) : null == valueB)) {
      return false;
    }

    List<? extends ParseTreeNode> aChildren = this.children();
    List<? extends ParseTreeNode> bChildren = that.children();
    int n = aChildren.size();
    if (n != bChildren.size()) {
      return false;
    }
    while (--n >= 0) {
      if (!aChildren.get(n).equals(bChildren.get(n))) {
        return false;
      }
    }
    return true;
  }


  public boolean acceptPreOrder(Visitor v) {
    AbstractParseTreeNode<?> oldParent = this.parent, oldNext =
        this.nextSibling;
    if (!v.visit(this)) {
      return false;
    }

    // Descend into the replacement's children.
    // Handle the case where v.visit() replaces this with another, inserts
    // another following, or deletes the node or a following node.
    if (oldParent == this.parent && oldNext == this.nextSibling) {
      // Not removed or replaced, so recurse to children.
      // This loop is complicated because it needs to survive mutations to the
      // child list.
      if (!this.childrenExtern.isEmpty()) {
        AbstractParseTreeNode<?> child =
            (AbstractParseTreeNode<?>) this.childrenExtern.get(0);
        do {
          AbstractParseTreeNode<?> next = child.nextSibling;
          child.acceptPreOrder(v);
          child = next;
        } while (null != child);
      }
    }
    return true;
  }

  public boolean acceptPostOrder(Visitor v) {
    // This loop is complicated because it needs to survive mutations to the
    // child list.
    if (!childrenExtern.isEmpty()) {
      AbstractParseTreeNode<?> child =
          (AbstractParseTreeNode<?>) childrenExtern.get(0);
      do {
        AbstractParseTreeNode<?> next = child.nextSibling;
        if (!child.acceptPostOrder(v)) {
          return false;
        }
        child = next;
      } while (null != child);
    }

    // If this node has been orphaned, don't visit it...
    // TODO(ihab): Do a more consistent refactoring to cover this case.
    if (this.parent != null) {
      return v.visit(this);
    }

    return true;
  }

  public final boolean acceptBreadthFirst(Visitor v) {
    List<AbstractParseTreeNode> stack = new LinkedList<AbstractParseTreeNode>();
    stack.add(this);
    do {
      AbstractParseTreeNode<?> n = stack.remove(0);

      do {
        AbstractParseTreeNode<?> oldParent = n.parent, next = n.nextSibling;

        if (!v.visit(n)) {
          return false;
        }

        if (n.parent == oldParent && n.nextSibling == next
            && !n.childrenExtern.isEmpty()) {
          AbstractParseTreeNode child =
              (AbstractParseTreeNode) n.childrenExtern.get(0);
          stack.add(child);
        }

        n = next;
      } while (null != n);

    } while (!stack.isEmpty());
    return true;
  }

  /** Uses identity hash code since this is mutable. */
  @Override
  public final int hashCode() {
    return super.hashCode();
  }

  /** Uses identity hash code since this is mutable. */
  @Override
  public final boolean equals(Object o) {
    return this == o;
  }

  public boolean isQuasiliteral() {
    return false;
  }

  public QuasiliteralQuantifier getQuasiliteralQuantifier() {
    return null;
  }

  public String getQuasiliteralIdentifier() {
    return null;
  }
  
  public Class<? extends ParseTreeNode> getQuasiMatchedClass() {
    return null;
  }

  public boolean deepEquals(ParseTreeNode specimen) {
    AbstractParseTreeNode<?> aptnSpecimen;
    try {
      aptnSpecimen = (AbstractParseTreeNode<?>)specimen;
    } catch (ClassCastException e) {
      return false;
    }
    
    if (!shallowEquals(specimen)) return false;
    if (children.size() != aptnSpecimen.children.size()) return false;
  
    for (int i = 0; i < children.size(); i++) {
      if (!children.get(i).deepEquals(aptnSpecimen.children.get(i)))
        return false;
    }
    
    return true;
  }
  
  public boolean shallowEquals(ParseTreeNode specimen) {
    return
        specimen != null &&
        this.getClass() == specimen.getClass() &&
        (this.getValue() == null ?
            specimen.getValue() == null :
            this.getValue().equals(specimen.getValue()));
  }
  
  public Map<String, ParseTreeNode> matchHere(final ParseTreeNode specimen) {
    Map<String, AbstractParseTreeNode<?>> tmp =
      new HashMap<String, AbstractParseTreeNode<?>>();
      
    List<AbstractParseTreeNode<?>> specimens =
      new ArrayList<AbstractParseTreeNode<?>>();
    specimens.add((AbstractParseTreeNode<?>)specimen);

    if (consumeSpecimens(this, specimens, tmp)) {
      final Map<String, ParseTreeNode> map = new HashMap<String, ParseTreeNode>();
      for (String k : tmp.keySet()) map.put(k, tmp.get(k));
      return map;
    }
    return null;
  }

  private static boolean matchChildren(
      AbstractParseTreeNode<?> self,
      AbstractParseTreeNode<?> specimen,
      Map<String, AbstractParseTreeNode<?>> map) {
    List<AbstractParseTreeNode<?>> specimenChildren =
        new ArrayList<AbstractParseTreeNode<?>>();
    addAll(specimenChildren, specimen.children);

    for (int i = 0; i < self.children().size(); i++) {
      AbstractParseTreeNode<?> child = (AbstractParseTreeNode<?>)
          self.children.get(i);
      if (!consumeSpecimens(child, specimenChildren, map))
        return false;
    }

    return specimenChildren.size() == 0;
  }

  private static boolean consumeSpecimens(
      AbstractParseTreeNode<?> self,
      List<AbstractParseTreeNode<?>> specimens,
      Map<String, AbstractParseTreeNode<?>> map) {
    if (!self.isQuasiliteral()) {
      return consumeConcrete(self, specimens, map);
    } else {
      switch (self.getQuasiliteralQuantifier()) {
        case MULTIPLE:
          return consumeMultiple(self, specimens, map);
        case MULTIPLE_NONEMPTY:
          return consumeMultipleNonempty(self, specimens, map);
        case SINGLE:
          return consumeSingle(self, specimens, map);
        default:
          throw new Error("Unrecognized quasiliteral quantifier");
      }
    }
  }

  private static boolean consumeConcrete(
      AbstractParseTreeNode<?> self,
      List<AbstractParseTreeNode<?>> specimens,
      Map<String, AbstractParseTreeNode<?>> map) {
    if (specimens.isEmpty()) return false;
    if (self.shallowEquals(specimens.get(0)) && 
        matchChildren(self, specimens.get(0), map)) {
      specimens.remove(0);
      return true;
    }
    return false;
  }

  private static boolean consumeMultiple(
      AbstractParseTreeNode<?> self,
      List<AbstractParseTreeNode<?>> specimens,
      Map<String, AbstractParseTreeNode<?>> map) {
    AbstractParseTreeNode<?> matches = new NodeContainer();
    while (specimens.size() > 0 && isSpecimenCompatible(self, specimens.get(0)))
      add(matches.children, specimens.remove(0));
    return putIfDeepEquals(map, self.getQuasiliteralIdentifier(), matches);
  }

  private static boolean consumeMultipleNonempty(
      AbstractParseTreeNode<?> self,
      List<AbstractParseTreeNode<?>> specimens,
      Map<String, AbstractParseTreeNode<?>> map) {
    if (specimens.size() < 1) return false;
    AbstractParseTreeNode<?> matches = new NodeContainer();
    while (specimens.size() > 0 && isSpecimenCompatible(self, specimens.get(0)))
      add(matches.children, specimens.remove(0));
    return putIfDeepEquals(map, self.getQuasiliteralIdentifier(), matches);
  }

  private static boolean consumeSingle(
      AbstractParseTreeNode<?> self,
      List<AbstractParseTreeNode<?>> specimens,
      Map<String, AbstractParseTreeNode<?>> map) {
    return
        specimens.size() >= 1 &&
        isSpecimenCompatible(self, specimens.get(0)) &&
        putIfDeepEquals(map, self.getQuasiliteralIdentifier(), specimens.remove(0));
  }

  private static boolean isSpecimenCompatible(
      AbstractParseTreeNode<?> self,
      AbstractParseTreeNode<?> specimen) {
    if (!self.isQuasiliteral()) {
      throw new Error();
    }
    if (self.getQuasiMatchedClass() == null) {
      throw new Error(self.toString());
    }
    
    return self.getQuasiMatchedClass().isAssignableFrom(specimen.getClass());
  }
  
  private static boolean putIfDeepEquals(
      Map<String, AbstractParseTreeNode<?>> map,
      String key,
      AbstractParseTreeNode<?> value) {
    if (map.containsKey(key)) {
     return map.get(key).deepEquals(value);
    }
    map.put(key, value);
    return true;
  }

  private static void add(List<?> target, Object src) {
    List<Object> oTarget = (List<Object>)target;
    oTarget.add(src);
  }
  
  private static void addAll(List<?> target, List<?> src) {
    List<Object> oTarget = (List<Object>)target;
    List<Object> oSrc = (List<Object>)src;
    oTarget.addAll(oSrc);
  }

  public boolean substitute(Map<String, ParseTreeNode> map) {
    return false; // TODO
  }
  
  private final class MutationImpl implements MutableParseTreeNode.Mutation {

    private List<Change> changes = new ArrayList<Change>();

    public Mutation replaceChild(ParseTreeNode replacement, ParseTreeNode child) {
      changes.add(new Replacement((AbstractParseTreeNode) replacement,
          (AbstractParseTreeNode) child));
      return this;
    }

    public Mutation insertBefore(ParseTreeNode toAdd, ParseTreeNode before) {
      changes.add(new Insertion((AbstractParseTreeNode) toAdd,
          (AbstractParseTreeNode) before));
      return this;
    }

    public Mutation removeChild(ParseTreeNode toRemove) {
      changes.add(new Removal((AbstractParseTreeNode) toRemove));
      return this;
    }

    @SuppressWarnings("finally")
    public void execute() {
      for (Change change : changes) {
        change.apply();
      }
      try {
        childrenChanged();
      } catch (RuntimeException ex) {
        for (int i = changes.size(); --i >= 0;) {
          changes.get(i).rollback();
        }
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
     */
    abstract void apply();

    /**
     * Rolls back the change effected by apply, and can assume that apply was
     * the most recent change to this node, and that it will be called at most
     * once after a given apply.
     */
    abstract void rollback();
  }

  private final class Replacement extends Change {
    private final AbstractParseTreeNode<?> replacement;
    private final AbstractParseTreeNode<?> replaced;

    Replacement(AbstractParseTreeNode<?> replacement,
        AbstractParseTreeNode<?> replaced) {
      this.replacement = replacement;
      this.replaced = replaced;
    }

    void apply() {
      final AbstractParseTreeNode<T> owner = AbstractParseTreeNode.this;
      // Make sure that it's not part of a tree already
      if (replacement.parent != null) {
        throw new IllegalArgumentException("Node already part of a parse tree");
      }
      // Make sure that adding replacement wouldn't introduce cycles.
      // Since we know that replacement.parent == null, we know that replacement
      // is a root, so we compare the replacement against the root of the tree
      // that contains this node.
      if (owner.getRoot() == replacement) {
        throw new IllegalArgumentException(
            "Adding the node would introduce cycles in the parse tree.");
      }

      // Find where to insert
      int childIndex = indexOf(replaced);
      if (childIndex < 0) {
        throw new NoSuchElementException(
            "Node to replace is not a child of this node.");
      }

      // Update the child list
      backupIndex = childIndex;
      setChild(childIndex, replacement);

      // Update the old node's state and insert replacement into the sibling
      // list
      replacement.parent = owner;
      replaced.parent = null;
      if (null != (replacement.prevSibling = replaced.prevSibling)) {
        replacement.prevSibling.nextSibling = replacement;
        replaced.prevSibling = null;
      }
      if (null != (replacement.nextSibling = replaced.nextSibling)) {
        replacement.nextSibling.prevSibling = replacement;
        replaced.nextSibling = null;
      }

      // Make sure that the replacement is parentified.
      replacement.parentify();
    }

    void rollback() {
      final AbstractParseTreeNode<T> owner = AbstractParseTreeNode.this;
      int childIndex = backupIndex;

      // This check corresponds to the replacement.parent == null check in apply
      // which has the effect of asserting that replacement is not rooted.
      if (children.contains(replaced)) {
        return;
      }

      setChild(childIndex, replaced); // roll back

      if (owner != replaced.parent) {
        replaced.parent = owner;
        replacement.parent = null;
        if (null != (replaced.prevSibling = replacement.prevSibling)) {
          replaced.prevSibling.nextSibling = replaced;
          replacement.prevSibling = null;
        }
        if (null != (replaced.nextSibling = replacement.nextSibling)) {
          replaced.nextSibling.prevSibling = replaced;
          replacement.nextSibling = null;
        }
        replaced.parentify();
      }
    }
  }

  private final class Removal extends Change {
    private final AbstractParseTreeNode<?> toRemove;

    Removal(AbstractParseTreeNode<?> toRemove) {
      this.toRemove = toRemove;
    }

    void apply() {
      // Find which to remove
      int childIndex = indexOf(toRemove);
      if (childIndex < 0) {
        throw new NoSuchElementException("child not in parent");
      }

      // Update the child list
      backupIndex = childIndex;
      children.remove(childIndex);

      // Update the old node's state and insert replacement into the sibling
      // list
      if (toRemove.nextSibling != null) {
        toRemove.nextSibling.prevSibling = toRemove.prevSibling;
      }
      if (toRemove.prevSibling != null) {
        toRemove.prevSibling.nextSibling = toRemove.nextSibling;
      }
      toRemove.parent = toRemove.nextSibling = toRemove.prevSibling = null;
    }

    void rollback() {
      final AbstractParseTreeNode<T> owner = AbstractParseTreeNode.this;

      if (children.contains(toRemove)) {
        return;
      }

      int childIndex = backupIndex;
      addChild(childIndex, toRemove);
      toRemove.parent = owner;
      if (childIndex > 0) {
        AbstractParseTreeNode<?> prev =
            (AbstractParseTreeNode<?>) children.get(childIndex - 1);
        toRemove.prevSibling = prev;
        toRemove.prevSibling.nextSibling = toRemove;
      }
      if (childIndex + 1 < children.size()) {
        AbstractParseTreeNode<?> next =
            (AbstractParseTreeNode<?>) children.get(childIndex + 1);
        toRemove.nextSibling = next;
        toRemove.nextSibling.prevSibling = toRemove;
      }
    }
  }

  private final class Insertion extends Change {
    private final AbstractParseTreeNode<?> toAdd;
    private final AbstractParseTreeNode<?> before;

    Insertion(AbstractParseTreeNode<?> toAdd, AbstractParseTreeNode<?> before) {
      this.toAdd = toAdd;
      this.before = before;
    }

    void apply() {
      final AbstractParseTreeNode<T> owner = AbstractParseTreeNode.this;

      // Make sure that it's not part of a tree already
      if (toAdd.parent != null) {
        throw new IllegalArgumentException("Node already part of a parse tree");
      }
      // Make sure that adding replacement wouldn't introduce cycles
      if (toAdd == owner.getRoot()) {
        throw new IllegalArgumentException(
            "Adding node to parent would introduce cycles in parse tree");
      }

      // Find where to insert
      int childIndex = children.size();
      if (null != before) {
        childIndex = indexOf(before);
        if (childIndex < 0) {
          throw new NoSuchElementException("Child not in parent");
        }
      }

      // Update the child list
      backupIndex = childIndex;
      addChild(childIndex, toAdd);

      // Insert toAdd into the sibling list
      toAdd.parent = owner;
      if (null != before) {
        toAdd.nextSibling = before;
        if (null != before.prevSibling) {
          toAdd.prevSibling = before.prevSibling;
          toAdd.prevSibling.nextSibling = toAdd;
        }
        before.prevSibling = toAdd;
      } else if (childIndex > 0) {
        toAdd.prevSibling =
            (AbstractParseTreeNode<T>) children.get(childIndex - 1);
        toAdd.prevSibling.nextSibling = toAdd;
      }

      // Make sure that the added node is parentified.
      toAdd.parentify();
    }

    void rollback() {
      final AbstractParseTreeNode<T> owner = AbstractParseTreeNode.this;

      int childIndex = backupIndex;

      AbstractParseTreeNode<T> removed =
          (AbstractParseTreeNode<T>) children.remove(childIndex);
      assert removed == toAdd;
      assert toAdd.parent == owner;

      if (toAdd.nextSibling != null) {
        toAdd.nextSibling.prevSibling = toAdd.prevSibling;
      }
      if (toAdd.prevSibling != null) {
        toAdd.prevSibling.nextSibling = toAdd.nextSibling;
      }
      toAdd.parent = toAdd.nextSibling = toAdd.prevSibling = null;
    }
  }
}
