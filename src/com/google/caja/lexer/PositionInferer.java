// Copyright (C) 2009 Google Inc.
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

package com.google.caja.lexer;

import com.google.caja.util.Lists;
import com.google.caja.util.Maps;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * Does some simple constraint solving to assign reasonable position values
 * to generated parse tree nodes.
 *
 * <h3>Usage</h3>
 * <ol>
 * <li>Create a {@code PositionInferer}, and implement the abstract methods.
 *     Pass to the constructor a file position from the same
 *     {@link InputSource source} as nodes with known file positions.
 * <li>Add constraints by calling {@link #contains}, {@link #precedes} and
 *     friends.  This class is meant to be agnostic to the classes used to
 *     implement the parse tree, but a node descriptor should typically be a
 *     {@code ParseTreeNode} or {@code org.w3c.dom.Node}.  Object descriptors
 *     are compared by reference identity, not {@link Object#equals}.
 *     Any node descriptor not mentioned in a constraint will not have a
 *     position inferred.  If there are contradictory constraints, then
 *     solve will behave as if some non-contradictory subset of constraints
 *     were added, though this subset is unpredictable.
 * <li>Call {@link #solve} to solve constraints.  This will cause a flurry of
 *     calls to {@link #setPosForNode}.  Consider the file positions as advisory
 *     and ignore as you like.  Ignoring a set will not affect the quality of
 *     later inferences.
 * </ol>
 *
 * @author mikesamuel@gmail.com
 */
public abstract class PositionInferer {
  private final List<Boundary> boundaries = Lists.newLinkedList();
  private final List<Relation> relations = Lists.newLinkedList();
  private final Map<Object, Region> boundsByNode
      = Maps.newIdentityHashMap();
  /**
   * Used to construct inferred file positions.
   */
  private final SourceBreaks breaks;

  private static final int UNSPECIFIED_MAX = Integer.MAX_VALUE;
  private static final int UNSPECIFIED_MIN
      = FilePosition.startOfFile(InputSource.UNKNOWN).startCharInFile();

  public PositionInferer(FilePosition spanningPos) {
    this.breaks = spanningPos.getBreaks();
  }

  /**
   * Adds a constraint that requires that contained's start and end falls
   * (inclusively) between container's start and end.
   * @param container a valid node descriptor.
   * @param contained a valid node descriptor.
   */
  public void contains(@Nullable Object container, @Nullable Object contained) {
    Region aBounds = boundsForNode(container);
    Region bBounds = boundsForNode(contained);
    Relation left = new LessThanRelation(aBounds.start, bBounds.start);
    Relation right = new LessThanRelation(bBounds.end, aBounds.end);
    if (!left.isSatisfied()) { relations.add(left); }
    if (!right.isSatisfied()) { relations.add(right); }
  }

  /**
   * Adds a constraint that requires that the end of before is at or before the
   * start of after.
   * @param before a valid node descriptor.
   * @param after a valid node descriptor.
   */
  public void precedes(Object before, Object after) {
    Region beforeBounds = boundsForNode(before);
    Region afterBounds = boundsForNode(after);
    Relation r = new LessThanRelation(beforeBounds.end, afterBounds.start);
    if (!r.isSatisfied()) { relations.add(r); }
  }

  /**
   * Adds a constraint that requires that the end of before is the same as the
   * start of after.  Similar to, but more constraining than {@link #precedes}.
   * @param before a valid node descriptor.
   * @param after a valid node descriptor.
   */
  public void adjacent(Object before, Object after) {
    Region beforeBounds = boundsForNode(before);
    Region afterBounds = boundsForNode(after);
    Relation r = new EqualRelation(beforeBounds.end, afterBounds.start);
    if (!r.isSatisfied()) { relations.add(r); }
  }

  /**
   * Attempts to satisfy all constraints added thus far, and then invokes
   * {@link #setPosForNode} for each node descriptor for which it could conclude
   * a position.
   */
  public void solve() {
    boolean workDone;
    List<Relation> toCheck = Lists.newLinkedList();
    do {
      workDone = false;
      for (Iterator<Relation> it = relations.iterator(); it.hasNext();) {
        Relation r = it.next();
        if (!r.isSatisfied()) {
          toCheck.add(r);
          do {
            Relation head = toCheck.remove(0);
            if (head.satisfy(toCheck)) { workDone = true; }
          } while (!toCheck.isEmpty());
        }
        if (r.isSatisfied()) { it.remove(); }
      }
    } while (workDone);

    // Make guesses for any half-specified bounds.
    for (Boundary b : boundaries) {
      if (!b.isSpecified()) {
        if (b.min > UNSPECIFIED_MIN) {
          b.max = b.min;
        } else if (b.max < UNSPECIFIED_MAX) {
          b.min = b.max;
        }
      }
    }

    // Propagate positions back to nodes.
    for (Map.Entry<Object, Region> e : boundsByNode.entrySet()) {
      Object node = e.getKey();
      Region r = e.getValue();
      if (r.start.isSpecified() && r.end.isSpecified()) {
        this.setPosForNode(node, breaks.toFilePosition(r.start.min, r.end.max));
      }
    }
  }

  /**
   * Returns the file position for the given node descriptor or an unknown
   * position if the position needs to be inferred.
   *
   * @param o a node descriptor.
   * @return non null.
   *   Should return a position whose {@link FilePosition#source source} is
   *   {@link InputSource#UNKNOWN unknown} if the node does not have accurate
   *   position info.
   */
  protected abstract FilePosition getPosForNode(@Nullable Object o);

  /**
   * Informs the client that it has inferred a position for the given node.
   * The client may ignore this, and probably should if the given node already
   * has accurate position data from a {@link InputSource source} different than
   * that of the position passed to the
   * {@link #PositionInferer(FilePosition) constructor}.
   *
   * @param o a node descriptor.
   * @param pos non null.
   */
  protected abstract void setPosForNode(@Nullable Object o, FilePosition pos);

  private Region boundsForNode(@Nullable Object o) {
    Region r = boundsByNode.get(o);
    if (r == null) {
      r = new Region(new Boundary(true, o), new Boundary(false, o));
      boundsByNode.put(o, r);
      FilePosition pos = getPosForNode(o);
      if (breaks.source().equals(pos.source())) {
        r.start.min = r.start.max = pos.startCharInFile();
        r.end.min = r.end.max = pos.endCharInFile();
      }
      boundaries.add(r.start);
      boundaries.add(r.end);
      Relation rel = new LessThanRelation(r.start, r.end);
      if (!rel.isSatisfied()) { relations.add(rel); }
    }
    return r;
  }

  /** An edge of a node descriptor's position. */
  private static class Boundary {
    /** True if this is the start edge. */
    final boolean isStart;
    /** A node descriptor. */
    final Object target;
    /**
     * Possibly unsatisfied relations that have this as one of their clauses.
     */
    final List<Relation> relations = Lists.newLinkedList();
    /** Lower bound on the edge's position. */
    int min = UNSPECIFIED_MIN;
    /** Upper bound on the edge's position. */
    int max = UNSPECIFIED_MAX;

    Boundary(boolean isStart, Object target) {
      this.isStart = isStart;
      this.target = target;
    }

    boolean isSpecified() { return min == max; }

    /**
     * Adds any unsatisfied relations to the given list, clearing out any
     * satisfied relations.
     */
    void schedule(List<? super Relation> out) {
      for (Iterator<Relation> deps = relations.iterator(); deps.hasNext();) {
        Relation dep = deps.next();
        if (dep.isSatisfied()) {
          deps.remove();
        } else {
          out.add(dep);
        }
      }
    }

    /** For debugging. */
    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      String targetStr;
      if (target != null) {
        targetStr = target.toString().replace("\n", "\\n").replace("\r", "\\r");
      } else {
        targetStr = "<null>";
      }
      sb.append("(").append((isStart ? "start" : "end")).append(" ")
          .append(targetStr);
      if (min != UNSPECIFIED_MIN || max != UNSPECIFIED_MAX) {
        if (min != UNSPECIFIED_MIN) { sb.append(" min=").append(min); }
        if (max != UNSPECIFIED_MAX) { sb.append(" max=").append(max); }
      }
      sb.append(")");
      return sb.toString();
    }
  }

  /** A start boundary and an end boundary. */
  private static class Region {
    final Boundary start;
    final Boundary end;

    Region(Boundary start, Boundary end) {
      assert start.isStart && !end.isStart;
      this.start = start;
      this.end = end;
    }
  }

  /**
   * A relationship between two boundaries that constrains the positions of the
   * set of boundaries.
   */
  private static abstract class Relation {
    final Boundary a;
    final Boundary b;

    Relation(Boundary a, Boundary b) {
      this.a = a;
      this.b = b;
      if (!(a.isSpecified() && b.isSpecified())) {
        a.relations.add(this);
        b.relations.add(this);
      }
    }

    /**
     * True if the upper and lower bounds of the positions of a and b are
     * such that no choice of actual positions within those bounds would make
     * this relation inconsistent.
     */
    abstract boolean isSatisfied();

    /**
     * Attempts to narrow the bounds on this relation's boundaries.
     * @param toCheck a list to append any relations that should be rechecked
     *   in light of changes to this relation's boundaries.
     * @return true if it managed to narrow bounds.
     */
    abstract boolean satisfy(List<? super Relation> toCheck);
  }

  /**
   * A relation between a boundary that appears at or before another boundary.
   */
  private static class LessThanRelation extends Relation {
    LessThanRelation(Boundary lesser, Boundary greater) {
      super(lesser, greater);
    }

    @Override
    boolean satisfy(List<? super Relation> toCheck) {
      // Consider six cases for (A <= B):
      // 1.  A         |----|                  Inconsistent.
      //     B  |----|

      // 2.  A  |----|                         Consistent and satisfied.
      //     B         |----|

      // 3.  A  |----|                         Consistent but cannot narrow.
      //     B     |----|

      // 4.  A        |----| =>       |-|      Narrow lesser's max
      //     B     |----|             |-|      and greater's min.

      // 5.  A  |-----------| => |--------|    Narrow lesser's max.
      //     B     |-----|          |-----|

      // 6.  A     |-----|    =>    |-----|    Narrow greater's min.
      //     B  |-----------|       |--------|

      boolean narrowed = false;
      if (a.min <= b.max) {
        // Eliminated case 1 and a special case of case 3 where both are equal.
        if (a.min > b.min) {  // Cases 4 and 6 above.
          int newMin = Math.min(a.min, b.max);
          if (b.min != newMin) {
            b.min = newMin;
            b.schedule(toCheck);
            narrowed = true;
          }
        }
        if (a.max > b.max) {  // Cases 4 and 5 above
          int newMax = Math.max(b.max, a.min);
          if (a.max != newMax) {
            a.max = newMax;
            a.schedule(toCheck);
            narrowed = true;
          }
        }
      }
      return narrowed;
    }

    @Override
    boolean isSatisfied() {
      return (a.isSpecified() && b.isSpecified()) || a.max <= b.min;
    }

    @Override
    public String toString() {
      return "(" + a + " <= " + b + ")";
    }
  }

  /**
   * A relation between two boundaries that should be at the same actual
   * position.
   */
  private static class EqualRelation extends Relation {
    EqualRelation(Boundary a, Boundary b) { super(a, b); }

    @Override
    boolean satisfy(List<? super Relation> toCheck) {
      int newAMin = Math.min(a.max, Math.max(a.min, b.min));
      int newAMax = Math.max(a.min, Math.min(a.max, b.max));
      int newBMin = Math.min(b.max, Math.max(a.min, b.min));
      int newBMax = Math.max(b.min, Math.min(a.max, b.max));
      boolean narrowed = false;
      if (a.min != newAMin || a.max != newAMax) {
        a.min = newAMin;
        a.max = newAMax;
        a.schedule(toCheck);
        narrowed = true;
      }
      if (b.min != newBMin || b.max != newBMax) {
        b.min = newBMin;
        b.max = newBMax;
        b.schedule(toCheck);
        narrowed = true;
      }
      return narrowed;
    }

    @Override
    boolean isSatisfied() {
      return (a.isSpecified() && b.isSpecified())
          || (a.min == b.min && a.max == b.max);
    }

    @Override
    public String toString() {
      return "(" + a + " == " + b + ")";
    }
  }
}
