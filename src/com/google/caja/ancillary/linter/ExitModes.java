// Copyright (C) 2008 Google Inc.
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

package com.google.caja.ancillary.linter;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.caja.lexer.FilePosition;
import com.google.caja.parser.js.BreakStmt;
import com.google.caja.parser.js.ContinueStmt;
import com.google.caja.parser.js.ReturnStmt;
import com.google.caja.parser.js.Statement;
import com.google.caja.parser.js.ThrowStmt;
import com.google.caja.util.Lists;
import com.google.caja.util.Sets;
import com.google.common.collect.Maps;

/**
 * Describes the ways in which execution of a JavaScript parse tree completes.
 * <p>
 * A block of code can return normally via a {@code return} statement, or it
 * can {@code throw} an exception, {@code break} to the end of a containing
 * block, {@code continue} to the beginning of a containing block, or complete
 * and pass control to the next statement.
 * <p>
 * This class describes the ways in which control might leave a block of code.
 * Below, control always leaves via return
 * <pre>if (a) { return 1; } else { return 2; }</pre>
 * Sometimes it returns, sometimes it breaks, and sometimes it completes.
 * <pre>if (a) { return 2; } else if (b) { break; } // no else</pre>
 * <p>
 * To calculate which variables are live, we need to track variable assignments
 * along all possible flow paths within a function.
 * In the first example above, we know that control always returns, so that
 * produces an {@code ExitModes} with a single {@code return}
 * {@link ExitModes.ExitMode mode} that is marked as
 * {@link ExitModes.ExitMode#always always} exiting.
 * In the second, we have 3 possible exit modes (to account for the implicit
 * {@code else;}, none of which have the always bit set.
 * <p>
 * Associated with each {@code ExitMode} is the set of variables live at the
 * time of exit.  That allows us to keep track of live variables along paths
 * out of a loop.
 * <pre>
 *   var a, b;
 *   do {
 *     if (f()) {
 *       b = 0;
 *       a = g();       // In this branch, both b and a were set
 *     } else {
 *       a = a + 1
 *       break;
 *     }
 *     // Because b was set in all non-exiting branches above, we know that
 *     // a and b are live here
 *     ...
 *   } while (a < 10);
 *   // Now, when we're done looking at the loop, we can take the live-set for
 *   // all completing paths (a and b) and intersect it with the live-set at
 *   // the time of breaks (a) to get the live set here: (a)
 * </pre>
 *
 * <p>
 * See also the NOTE in {@link LiveSet}.
 *
 * @author mikesamuel@gmail.com
 */
final class ExitModes {
  /**
   * This is analogous to the <tt>(normal, empty, empty)</tt> triple that
   * is used in Chapter 12 of EcmaScript.  It is the same as the exit mode of
   * the empty block and the no-op statement.
   */
  static final ExitModes COMPLETES
      = new ExitModes(Collections.<String, ExitMode>emptyMap(), true);

  private final Map<String, ExitMode> exits;
  private final boolean completes;
  /**
   * @param completes does the AST being described complete instead of
   *     breaking, continuing, returning, or throwing along all code-paths?
   */
  private ExitModes(Map<String, ExitMode> exits, boolean completes) {
    this.exits = exits;
    this.completes = completes;
  }

  /** True if execution will leave the current function. */
  boolean returns() { return returnsNormally() || returnsAbruptly(); }
  /**
   * True if execution will leave the current function due to a {@code return}
   * statement.
   */
  boolean returnsNormally() {
    return hasAlwaysKey("r");
  }
  /**
   * True if execution will leave the current function due to an exception
   * being thrown.
   */
  boolean returnsAbruptly() {
    return hasAlwaysKey("t");
  }
  /**
   * The set of variables live at {@code throw} statements.
   * @return null means that no information is available.
   */
  ExitMode atThrow() {
    return exits.get("t");
  }
  /**
   * True if execution will jump to the end of the block with the given label.
   */
  boolean breaksToLabel(String label) {
    return hasAlwaysKey(prefix("b", label));
  }
  /**
   * The set of variables live at {@code break} statements for the given label.
   * @return null means that no information is available.
   */
  ExitMode atBreak(String label) {
    return exits.get(prefix("b", label));
  }
  /**
   * True if execution will jump to the start of the block with the given label.
   */
  boolean continuesToLabel(String label) {
    return hasAlwaysKey(prefix("b", label));
  }
  /**
   * The set of variables live at {@code break} statements for the given label.
   * @return null means that no information is available.
   */
  ExitMode atContinue(String label) {
    return exits.get(prefix("c", label));
  }
  /**
   * True if execution might continue to the next statement -- there
   * is a code path that does not have a {@code return,throw,break,continue}.
   * A return value of true does not imply that there is a next statement.
   */
  boolean completes() {
    return completes;
  }
  Set<Statement> liveExits() {
    List<Statement> stmts = Lists.newArrayList();
    for (ExitMode em : exits.values()) {
      stmts.addAll(em.sources);
    }
    // Impose an order on output not dependent on hashing
    Collections.sort(stmts, new Comparator<Statement>() {
      public int compare(Statement a, Statement b) {
        FilePosition pa = a.getFilePosition(), pb = b.getFilePosition();
        int delta = pa.source().toString().compareTo(pb.source().toString());
        if (delta != 0) { return delta; }
        return pa.startCharInFile() - pb.startCharInFile();
      }
    });
    return Collections.unmodifiableSet(Sets.newLinkedHashSet(stmts));
  }

  private boolean hasAlwaysKey(String key) {
    ExitMode em = exits.get(key);
    return em != null && em.always;
  }

  /** Same as this, but {@code breaksToLabel(label)}. */
  ExitModes withBreak(BreakStmt s, LiveSet atBreak) {
    return withEntry(prefix("b", s.getLabel()), atBreak, s);
  }
  /** Same as this, but {@code continuesToLabel(label)}. */
  ExitModes withContinue(ContinueStmt s, LiveSet atContinue) {
    return withEntry(prefix("c", s.getLabel()), atContinue, s);
  }
  /** Same as this, but {@code returnsNormally()}. */
  ExitModes withNormalReturn(ReturnStmt s, LiveSet atReturn) {
    return withEntry("r", atReturn, s);
  }
  /** Same as this, but {@code returnsAbruptly()}. */
  ExitModes withAbruptReturn(ThrowStmt t, LiveSet atThrow) {
    return withEntry("t", atThrow, t);
  }
  /** Same as this, but {@code !breaksToLabel(label)}. */
  ExitModes withoutBreak(String label) {
    return withoutEntry(prefix("b", label), true);
  }
  /** Same as this, but {@code !continuesToLabel(label)}. */
  ExitModes withoutBreakOrContinue(String label) {
    String b = prefix("b", label), c = prefix("c", label);
    int count = (exits.containsKey(b) ? 1 : 0) + (exits.containsKey(c) ? 1 : 0);
    if (count == exits.size()) { return ExitModes.COMPLETES; }
    Map<String, ExitMode> exits = Maps.newLinkedHashMap(this.exits);
    boolean completes = exits.remove(b) != null | this.completes;
    // The continue does not cause completes -> true since continue does not
    // go to the end of a loop.
    exits.remove(c);
    // The new exit mode completes since a break or continue to the loop in
    // question now exits.
    return new ExitModes(exits, completes);
  }
  /** Same as this, but {@code !returnsAbruptly()}. */
  ExitModes withoutAbruptReturn() {
    return withoutEntry("t", true);
  }
  /**
   * {@link ExitModes} that are true for any of the predicates above that are
   * true both for this and for m.
   */
  ExitModes intersection(ExitModes m) {
    return join(this, m, false);
  }
  /**
   * {@link ExitModes} that are true for any of the predicates above that are
   * true for this or for m.
   */
  ExitModes union(ExitModes m) {
    return join(this, m, true);
  }

  /**
   * @param unioning true means that an {@code ExitMode} in the output has its
   *   {@link ExitMode#always} bit set if at least one of {@code (this, m)}
   *   has a corresponding {@code ExitMode} with the always bit set.
   *   Otherwise, all existing corresponding {@code ExitModes} must have the
   *   always bit set.
   */
  private static ExitModes join(ExitModes a, ExitModes b, boolean unioning) {
    // TODO(mikesamuel): refactor this
    if (a == b) { return a; }
    if (unioning) {
      if (a.exits.isEmpty()) { return b; }
      if (b.exits.isEmpty()) { return a; }
    }
    // Make sure a is not smaller than b.
    if (a.exits.size() >= b.exits.size()) {
      ExitModes tmp = a;
      a = b;
      b = tmp;
    }
    Map<String, ExitMode> exits = Maps.newLinkedHashMap(a.exits);
    exits.putAll(b.exits);
    boolean same = exits.size() == a.exits.size();
    if (unioning) {
      for (Map.Entry<String, ExitMode> e : b.exits.entrySet()) {
        String k = e.getKey();
        ExitMode orig = a.exits.get(k);
        if (orig != null) {
          ExitMode combined = orig.combine(e.getValue(), true);
          if (combined != e.getValue()) {
            exits.put(k, combined);
            same = false;
          }
        }
      }
    } else {
      for (Map.Entry<String, ExitMode> e : exits.entrySet()) {
        String k = e.getKey();
        ExitMode bEl = b.exits.get(k);
        if (bEl == null) {
          if (e.getValue().always && b.completes) {
            e.setValue(e.getValue().sometimes());
            same = false;
          }
        } else {
          ExitMode aEl = a.exits.get(k);
          ExitMode combined = aEl != null
              ? aEl.combine(bEl, false)
              : a.completes
              ? bEl.sometimes()
              : bEl;
          if (combined != aEl) {
            e.setValue(combined);
            same = false;
          }
        }
      }
    }
    boolean completes = unioning
        // Series of operations only completes if all operations completes.
        ? a.completes && b.completes
        // A set of branches complete if any of the branches complete.
        : a.completes || b.completes;
    same &= completes == a.completes;
    return same ? a : new ExitModes(exits, completes);
  }
  private static String prefix(String prefix, String suffix) {
    if (suffix == null) { throw new NullPointerException(); }
    if ("".equals(suffix)) { return prefix; }
    return prefix + suffix;
  }
  private ExitModes withEntry(String e, LiveSet vars, Statement source) {
    ExitMode em = new ExitMode(vars, true, Collections.singleton(source));
    ExitMode orig = exits.get(e);
    if (orig != null) {
      ExitMode inter = orig.combine(em, false);
      if (inter == orig) { return this; }
      em = inter;
    }
    Map<String, ExitMode> exits = Maps.newLinkedHashMap(this.exits);
    exits.put(e, em);
    return new ExitModes(exits, false);
  }
  private ExitModes withoutEntry(String e, boolean completes) {
    if (!this.exits.containsKey(e)) { return this; }
    if (this.exits.size() == 1) { return ExitModes.COMPLETES; }
    Map<String, ExitMode> exits = Maps.newLinkedHashMap(this.exits);
    exits.remove(e);
    // presumably the program fragment completes because a throw was caught
    // or a break matched a loop.
    return new ExitModes(exits, completes || this.completes);
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof ExitModes)) { return false; }
    return exits.equals(((ExitModes) o).exits);
  }

  @Override
  public int hashCode() {
    return exits.hashCode();
  }

  @Override
  public String toString() {
    return exits.toString();
  }

  static final class ExitMode {
    final LiveSet vars;
    final boolean always;
    final Set<Statement> sources;

    /**
     * @param vars the set of vars live when that exit mode is reached.
     * @param always true if that exit mode always occurs.
     *     In {@code if (x) return false;}, the program returns, but
     *     not always, whereas in {@code if (x) return true; else return false;}
     *     it always returns.
     *     Unexpected exceptions are not considered for purposes of always.
     */
    private ExitMode(LiveSet vars, boolean always, Set<Statement> sources) {
      this.vars = vars;
      this.always = always;
      this.sources = sources;
    }

    /**
     * @param orAlways true means that an {@code ExitMode} in the output has its
     *   {@link ExitMode#always} bit set if at least one of {@code (this, m)}
     *   has a corresponding {@code ExitMode} with the always bit set.
     *   Otherwise, all existing corresponding {@code ExitModes} must have the
     *   always bit set.
     */
    private ExitMode combine(ExitMode other, boolean orAlways) {
      LiveSet inter = this.vars.intersection(other.vars);
      boolean always = orAlways
          ? this.always || other.always
          : this.always && other.always;
      if (inter == this.vars && always == this.always) { return this; }
      if (inter == other.vars && always == other.always) { return other; }
      Set<Statement> allSources = Sets.newHashSet(this.sources);
      allSources.addAll(other.sources);
      return new ExitMode(inter, always, allSources);
    }

    private ExitMode sometimes() {
      return always ? new ExitMode(vars, false, sources) : this;
    }

    @Override
    public int hashCode() { return vars.hashCode() ^ (always ? 1 : 0); }

    @Override
    public boolean equals(Object o) {
      if (!(o instanceof ExitMode)) { return false; }
      ExitMode that = (ExitMode) o;
      return this.vars.equals(that.vars) && this.always == that.always;
    }

    @Override
    public String toString() {
      return "(" + vars + (always ? " always" : "") + ")";
    }
  }
}
