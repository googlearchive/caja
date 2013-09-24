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
import com.google.caja.parser.js.CatchStmt;
import com.google.caja.parser.js.FunctionConstructor;
import com.google.caja.parser.js.WithStmt;
import com.google.common.collect.Lists;

import java.util.List;

/**
 * A set of adjacent AST nodes that share a common ancestor which is in the set.
 *
 * <h2>Glossary</h2>
 * <dl>
 * <dt>Scope-Chain</dt>
 * <dd>A stack-like structure used to resolve references in an EcmaScript
 *     interpreter as defined in
 *     <a href=
 *     http://www.ecma-international.org/publications/files/ECMA-ST/Ecma-262.pdf
 *     >EcmScript 262</a> section 10.1.4.</dd>
 * <dt>Lexical Scope</dt>
 * <dd>A contiguous range of source code elements that, when control enters
 *     them, cause an element to be added to the scope-chain; and that, when
 *     control leaves them, cause that element to be removed from the
 *     scope-chain.</dd>
 * <dt>Lexical Scope Root</dt>
 * <dd>The common ancestor of all source code elements in a lexical scope, and
 *     which contains no elements not in the lexical scope.</dd>
 * <dt>Symbol</dt>
 * <dd>A named property in a scope-chain frame.
 *     Symbols correspond to properties of the global object, function
 *     parameters and local variables, members of an object in a {@code with}
 *     block, etc.</dd>
 * <dt>Lexical Scope Ancestor</dt>
 * <dd>A lexical scope that is always on the scope-chain when this lexical scope
 *     is on the scope-chain.
 * </dl>
 *
 * @author mikesamuel@gmail.com
 */
final class LexicalScope {
  final AncestorChain<?> root;
  final LexicalScope parent;
  final SymbolTable symbols;
  final List<LexicalScope> innerScopes = Lists.newArrayList();

  LexicalScope(AncestorChain<?> root, LexicalScope parent) {
    this.root = root;
    this.parent = parent;
    this.symbols = new SymbolTable();
    if (parent != null) { parent.innerScopes.add(this); }
  }

  LexicalScope declaringScope(String symbolName) {
    if (symbols.getSymbol(symbolName) != null) { return this; }
    if (parent == null) { return null; }
    return parent.declaringScope(symbolName);
  }

  /**
   * True if both scopes occur in the same function, or if neither are in a
   * function, in the same global scope.
   */
  boolean inSameProgramUnit(LexicalScope that) {
    LexicalScope shallow, deep;
    if (this.root.depth >= that.root.depth) {
      deep = this;
      shallow = that;
    } else {
      deep = that;
      shallow = this;
    }
    while (shallow.parent != null && !shallow.isFunctionScope()) {
      shallow = shallow.parent;
    }
    for (LexicalScope s = deep; s != null; s = s.parent) {
      if (s == shallow) { return true; }  // shallow contains deep
      if (s.isFunctionScope()) { return false; }
    }
    return false;  // disjoint
  }

  boolean isWithScope() { return root.node instanceof WithStmt; }
  boolean isCatchScope() { return root.node instanceof CatchStmt; }
  boolean isFunctionScope() { return root.node instanceof FunctionConstructor; }
  boolean isGlobal() { return parent == null; }
}
