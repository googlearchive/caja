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

import com.google.caja.parser.AncestorChain;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.js.Declaration;
import com.google.caja.parser.js.Reference;
import com.google.caja.util.Pair;
import com.google.common.collect.Sets;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

/**
 * The set of symbols that are definitely live at a point in a program.
 * Symbols are represented as name, scope pairs.
 * <p>
 * When a {@code LiveSet} is associate with an AST, it means that that is the
 * set of symbols definitely live when control enters that node.
 * <p>
 * NOTE:
 * There is a group of types <tt>(LiveSet, ExitModes, ExitMode)</tt>
 * that take care that methods which derive new values of the same type
 * return this if no changes would be made.  This is an optimization to avoid
 * excessive object creation.  Since they all do this,
 * {@code VariableLiveness.liveness} does not need to create any new objects
 * in the common case where a node introduces no live variables, and no new
 * exit modes.
 *
 * @author mikesamuel@gmail.com
 */
final class LiveSet {
  static final LiveSet EMPTY = new LiveSet(
      Collections.<Pair<String, LexicalScope>>emptySet());

  // LexicalScopes compare for equality by identity.
  final Set<Pair<String, LexicalScope>> symbols;

  /**
   * Creates a new scope for a DOM root or function constructor.
   *
   * @param scopeRoot normally, a node such that there exists a
   *     {@link LexicalScope scope} LS where {@code LS.root.node == scopeRoot}.
   */
  LiveSet(ParseTreeNode scopeRoot) {
    Set<Pair<String, LexicalScope>> symbols = Sets.newLinkedHashSet();
    LexicalScope scope = ScopeAnalyzer.containingScopeForNode(scopeRoot);
    // Find the set of symbols defined by the overrideable method
    // LexicalScope.initScope that were defined because of this method, not
    // as the result of a declaration which we may encounter later.
    for (String symbolName : scope.symbols.symbolNames()) {
      SymbolTable.Symbol s = scope.symbols.getSymbol(symbolName);
      for (AncestorChain<?> decl : s.getDeclarations()) {
        if (decl.node == scopeRoot) {
          symbols.add(Pair.pair(symbolName, scope));
          break;
        }
      }
    }
    this.symbols = Collections.unmodifiableSet(symbols);
  }

  private LiveSet(Set<Pair<String, LexicalScope>> symbols) {
    this.symbols = Collections.unmodifiableSet(symbols);
  }

  /**
   * Yields a {@code LiveSet} with all symbols that occur in this or other.
   * This is typically used when statements are executed in series since
   * the next statement will receive any variables that became live as a result
   * of any previous statements.
   */
  LiveSet union(LiveSet other) {
    Set<Pair<String, LexicalScope>> usymbols = Sets.newLinkedHashSet(symbols);
    usymbols.addAll(other.symbols);
    return new LiveSet(usymbols);
  }

  /**
   * Yields a {@code LiveSet} with any symbols that occur in both this and
   * other.
   * This is typically used when execution branches, because the {@code LiveSet}
   * at the point branched paths converge is the set of variables that were
   * made live in all branches.
   */
  LiveSet intersection(LiveSet other) {
    Set<Pair<String, LexicalScope>> isymbols = Sets.newLinkedHashSet(symbols);
    isymbols.retainAll(other.symbols);
    return isymbols.isEmpty() ? EMPTY : new LiveSet(isymbols);
  }

  /** The set including all in this and any introduced by d. */
  LiveSet with(Declaration d) {
    return with(d.getIdentifierName(), ScopeAnalyzer.definingScopeForNode(d));
  }

  /**
   * The set including all in this and any referenced by r.
   * @param r typically the left-hand-side of an assignment.
   */
  LiveSet with(Reference r) {
    String name = r.getIdentifierName();
    LexicalScope scope = ScopeAnalyzer.containingScopeForNode(r);
    while (scope != null) {
      if (scope.symbols.getSymbol(name) != null) {
        return with(name, scope);
      }
      scope = scope.parent;
    }
    return this;
  }

  private LiveSet with(String name, LexicalScope scope) {
    Pair<String, LexicalScope> p = Pair.pair(name, scope);
    if (this.symbols.contains(p)) { return this; }
    Set<Pair<String, LexicalScope>> wsymbols = Sets.newLinkedHashSet(symbols);
    wsymbols.add(p);
    return new LiveSet(wsymbols);
  }

  /**
   * Filters out variables from an inner-scope that are no longer live
   * as the result of the scope having been exited.
   */
  LiveSet filter(LexicalScope containingScope) {
    Iterator<Pair<String, LexicalScope>> it = symbols.iterator();
    while (it.hasNext()) {
      Pair<String, LexicalScope> s = it.next();
      if (!isAncestorOf(s.b, containingScope)) {
        Set<Pair<String, LexicalScope>> filtered
            = Sets.newLinkedHashSet(symbols);
        filtered.remove(s);
        while (it.hasNext()) {
          s = it.next();
          if (!isAncestorOf(s.b, containingScope)) {
            filtered.remove(s);
          }
        }
        return filtered.isEmpty() ? EMPTY : new LiveSet(filtered);
      }
    }
    return this;
  }
  private static final boolean isAncestorOf(LexicalScope a, LexicalScope b) {
    return a == b || a.root.depth < b.root.depth;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append('(');
    String sep = "";
    for (Pair<String, LexicalScope> s : symbols) {
      int depth = s.b != null ? s.b.root.depth : -1;
      if (!(ScopeAnalyzer.ECMASCRIPT_BUILTINS.contains(s.a) && depth == 0)) {
        sb.append(sep).append(s.a).append('@').append(depth);
        sep = " ";
      }
    }
    return sb.append(')').toString();
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof LiveSet)) { return false; }
    return this.symbols.equals(((LiveSet) o).symbols);
  }

  @Override
  public int hashCode() {
    return symbols.hashCode();
  }
}
