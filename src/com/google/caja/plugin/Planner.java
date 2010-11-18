// Copyright (C) 2010 Google Inc.
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

package com.google.caja.plugin;

import com.google.caja.util.Lists;
import com.google.caja.util.Sets;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Dijkstra's algo.
 *
 * <dl>
 *   <dt>Plan State
 *     <dd>A plan state is represented as a set of sets of boolean properties.
 *   <dt>Tool
 *     <dd>an element in a plan that specifies a transition from plan state to
 *     plan state.
 *     A tool has preconditions and postconditions both represented as
 *     plan states.
 *     <p>A tool is applicable if, each element of the preconditions is a subset
 *     of an element of the current plan state.
 *     <p>The result of applying a tool is the union of the postconditions and
 *     (the current plan state minus any elements that are supersets of elements
 *     in the preconditions).
 *   <dt>Plan
 *     <dd>A list of tools without repeats such that pre and post condition
 *     constraints hold between adjacent elements given a suitable set of
 *     initial conditions.
 * </dl>
 *
 * @author mikesamuel@gmail.com
 */
public final class Planner {

  /**
   * @param novelPropNames true iff strs can contain novel properties.
   *     Only tool chain builders should create novel properties, since
   *     goals and inputs with properties not mentioned in any tool chain cannot
   *     be satisfied.
   * @param strs a "+" separated group of identifiers, e.g. form foo+bar+baz.
   */
  public PlanState planState(boolean novelPropNames, String... strs) {
    Set<Long> prods = Sets.newHashSet();
    for (String s : strs) {
      if (s == null) { continue; }
      if (!s.matches("\\w+(?:\\+\\w+)*")) {
        throw new IllegalArgumentException(s);
      }
      String[] parts = s.split("\\+");
      long properties = 0;
      for (String part : parts) {
        properties |= (1L << identIndex(part, novelPropNames));
      }
      prods.add(properties);
    }
    long union = 0;
    long[] props = new long[prods.size()];
    int k = 0;
    for (Long p : prods) {
      props[k++] = p;
      union |= p;
    }
    Arrays.sort(props);
    return new PlanState(this, props, union);
  }

  public <TOOL extends Tool>
  List<TOOL> plan(List<TOOL> tools, PlanState inputs, PlanState goals)
      throws UnsatisfiableGoalException {
    assert inputs.getPlanner() == this;
    assert goals.getPlanner() == this;
    PartialPlan<TOOL> start = new PartialPlan<TOOL>(inputs, null, null);
    PlanState state;
    if ((state = start.apply(goals)) != null && state.isEmpty()) {
      return toPlan(start);
    }
    List<PartialPlan<TOOL>> stack = Lists.newLinkedList();
    stack.add(start);
    while (!stack.isEmpty()) {
      PartialPlan<TOOL> plan = stack.remove(0);
      for (TOOL tool : tools) {
        if (!plan.used.contains(tool)
            && (state = plan.apply(tool.preconds)) != null) {
          state = state.with(tool.postconds);
          PartialPlan<TOOL> next = new PartialPlan<TOOL>(state, plan, tool);
          if ((state = next.apply(goals)) != null && state.isEmpty()) {
            return toPlan(next);
          }
          stack.add(next);
        }
      }
    }
    throw new UnsatisfiableGoalException(
        "No path from " + inputs + " to " + goals);
  }

  private static <TOOL extends Tool>
  List<TOOL> toPlan(PartialPlan<TOOL> partialPlan) {
    List<TOOL> plan = Lists.newArrayList();
    while (partialPlan != null) {
      if (partialPlan.tool != null) { plan.add(partialPlan.tool); }
      partialPlan = partialPlan.prior;
    }
    Collections.reverse(plan);
    return plan;
  }

  private static final class PartialPlan<TOOL extends Tool> {
    final PlanState state;
    final PartialPlan<TOOL> prior;
    final TOOL tool;
    final Set<TOOL> used;

    PartialPlan(PlanState state, PartialPlan<TOOL> prior, TOOL tool) {
      this.state = state;
      this.prior = prior;
      this.tool = tool;
      if (tool == null) {
        used = Collections.emptySet();
      } else if (prior == null) {
        used = Collections.singleton(tool);
      } else {
        used = Sets.newIdentityHashSet(prior.used);
        used.add(tool);
      }
    }

    PlanState apply(PlanState reqs) {
      if ((reqs.union & state.union) != reqs.union) { return null; }
      boolean[] used = new boolean[state.properties.length];
      int nUsed = 0;
      for (long p : reqs.properties) {
        boolean satisfied = false;
        int i = 0;
        for (long q : state.properties) {
          if ((q & p) == p) {
            if (!used[i]) {
              used[i] = true;
              ++nUsed;
            }
            satisfied = true;
          }
          ++i;
        }
        if (!satisfied) { return null; }
      }
      int n = state.properties.length;
      if (n == nUsed) { return EMPTY; }
      int nUnused = n - nUsed;
      long[] unused = new long[nUnused];
      long union = 0;
      for (int i = 0, k = 0; k < nUnused; ++i) {
        if (!used[i]) {
          long p = state.properties[i];
          unused[k++] = p;
          union |= p;
        }
      }
      return new PlanState(state.planner, unused, union);
    }
  }

  public static abstract class Tool {
    PlanState preconds = EMPTY;
    PlanState postconds = EMPTY;

    Tool given(PlanState preconds) {
      this.preconds = this.preconds.with(preconds);
      return this;
    }

    Tool exceptNotGiven(PlanState exceptions) {
      this.preconds = this.preconds.without(exceptions);
      return this;
    }

    Tool produces(PlanState postconds) {
      this.postconds = this.postconds.with(postconds);
      return this;
    }
  }

  private final List<String> IDENTS = Collections.synchronizedList(
      Lists.<String>newArrayList());

  private int identIndex(String ident, boolean allocNew) {
    if (IDENTS.size() == 64) { throw new Error(); }
    int index = IDENTS.indexOf(ident);
    if (index < 0) {
      if (allocNew) {
        index = IDENTS.size();
        IDENTS.add(ident);
      } else {
        throw new IllegalArgumentException(ident);
      }
    }
    return index;
  }

  public static final PlanState EMPTY = new PlanState();

  /**
   * The state of a plan, such as the initial conditions, goals, and all the
   * intermediate states in a process.
   */
  public static final class PlanState {
    /**
     * The planner instance from which this part is derived, so that
     * properties can be matched to names, or possibly null if this contains
     * no properties.
     */
    final Planner planner;
    final long[] properties;
    /** The bitwise or of the elements of properties. */
    final long union;

    private PlanState() {
      this.planner = null;
      this.properties = new long[0];
      this.union = 0L;
    }

    private PlanState(Planner planner, long[] properties, long union) {
      assert union != 0 || properties.length == 0;
      this.planner = planner;
      this.properties = properties;
      this.union = union;
    }

    public boolean isEmpty() { return union == 0; }

    public PlanState with(PlanState that) {
      int m = properties.length, n = that.properties.length;
      if (n == 0) { return this; }
      if (m == 0) { return that; }
      assert this.getPlanner() == that.getPlanner();
      int count = properties.length;
      {
        int i = 0, j = 0;
        while (i < m && j < n) {
          long a = this.properties[i];
          long b = that.properties[j];
          if (a < b) {
            ++i;
          } else if (a == b) {
            ++i;
            ++j;
          } else {
            ++j;
            ++count;
          }
        }
        count += n - j;
      }
      long[] newProperties = new long[count];
      long union = this.union;
      int i = 0, j = 0, k = -1;
      while (i < m && j < n) {
        long a = this.properties[i];
        long b = that.properties[j];
        long el;
        if (a < b) {
          ++i;
          el = a;
        } else if (a == b) {
          ++i;
          ++j;
          el = a;
        } else {
          ++j;
          el = b;
          union |= b;
        }
        newProperties[++k] = el;
      }
      if (i < m) {
        do {
          newProperties[++k] = this.properties[i];
        } while (++i < m);
      } else if (j < n) {
        do {
          long el = that.properties[j];
          newProperties[++k] = el;
          union |= el;
        } while (++j < n);
      }
      return new PlanState(planner, newProperties, union);
    }

    public PlanState without(PlanState that) {
      if (this.isEmpty() || that.isEmpty()) { return this; }
      assert this.getPlanner() == that.getPlanner();
      long[] newProperties = this.properties.clone();
      int nZero = 0;
      int n = newProperties.length;
      for (int i = n; --i >= 0;) {
        for (int j = that.properties.length; --j >= 0;) {
          if (newProperties[i] == that.properties[j]) {
            newProperties[i] = 0;
            ++nZero;
            break;
          }
        }
      }
      if (nZero == 0) { return this; }
      if (nZero == n) { return EMPTY; }
      long[] newPropertiesTrim = new long[n - nZero];
      long union = 0;
      for (int i = 0, k = 0; k < newPropertiesTrim.length; ++i) {
        if (newProperties[i] == 0) { continue; }
        union |= (newPropertiesTrim[k++] = newProperties[i]);
      }
      return new PlanState(planner, newPropertiesTrim, union);
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      String delim = "";
      for (long p : properties) {
        sb.append(delim);
        delim = " ";
        planner.propertiesToBuf(p, sb);
      }
      return sb.toString();
    }

    private Planner getPlanner() { return planner; }
  }

  private void propertiesToBuf(long properties, StringBuilder sb) {
    long l = properties;
    String delim = "";
    for (int i = 0; i < 63; ++i) {
      if ((l & (1L << i)) != 0) {
        sb.append(delim);
        delim = "+";
        sb.append(IDENTS.get(i));
        l = l & ~(1L << i);
        if (l == 0) { break; }
      }
    }
  }

  static final class UnsatisfiableGoalException extends Exception {
    UnsatisfiableGoalException(String msg) { super(msg); }
  }
}
