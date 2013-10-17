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

package com.google.caja.ancillary.opt;

import com.google.caja.parser.AncestorChain;
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.Declaration;
import com.google.caja.parser.js.FunctionConstructor;
import com.google.caja.parser.js.scope.ScopeType;
import com.google.caja.parser.quasiliteral.Scope;
import com.google.caja.reporting.MessageQueue;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Encapsulates a scope and information about the names declared and used
 * within it, and its relationship to other scopes.
 */
final class ScopeInfo {
  /** @see #isDynamicUsePossible */
  private boolean dynamicUsePossible;
  /**
   * The number of containing scopes between this scope and the global scope.
   */
  final int depth;
  /** The smallest scope that entirely contains this scope. */
  final ScopeInfo parent;
  final Scope s;
  final Map<String, String> mapping = Maps.newLinkedHashMap();
  /**
   * Declarations that introduce variables into s.
   */
  final List<AncestorChain<Declaration>> decls = Lists.newArrayList();
  /**
   * Function constructors live in the scope.  Zero or one.
   */
  final List<AncestorChain<FunctionConstructor>> fns = Lists.newArrayList();
  /**
   * Uses of variables in this scope.
   */
  final Set<Use> uses = Sets.newLinkedHashSet();
  /**
   * Scopes contained entirely by this scope.
   */
  final List<ScopeInfo> inners = Lists.newArrayList();

  ScopeInfo(Block program, MessageQueue mq) {
    this(new ScopeInfo(),
        Scope.fromProgram(program, mq));
    // The global scope is infected since top level declarations
    // are aliased by members of the local scope.
    this.dynamicUsePossible = true;
  }

  ScopeInfo(ScopeInfo parent, Scope s) {
    this.depth =  parent.depth + 1;
    this.parent = parent;
    this.s = s;
    parent.inners.add(this);
    if (s.getType() == ScopeType.FUNCTION) {
      mapping.put("this", "this");
      mapping.put("arguments", "arguments");
    } else if (s.getType() == ScopeType.PROGRAM) {
      mapping.put("this", "this");
    }
  }

  ScopeInfo() {
    this.depth = 0;
    this.parent = null;
    this.s = null;
    this.dynamicUsePossible = true;
  }

  ScopeInfo withScope(Scope s) {
    ScopeInfo u = this;
    while (u != null && u.s != s) { u = u.parent; }
    return u;
  }

  void setDynamicUsePossible() { this.dynamicUsePossible = true; }
  /**
   * True iff this scope contains a {@code with} statement, use of {@code eval}
   * or other construct that complicates static reasoning about the use of names
   * in this scope and containing scopes.
   */
  boolean isDynamicUsePossible() { return this.dynamicUsePossible; }
}
