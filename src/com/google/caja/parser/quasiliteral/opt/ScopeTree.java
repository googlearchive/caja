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

package com.google.caja.parser.quasiliteral.opt;

import com.google.caja.parser.AncestorChain;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.FunctionConstructor;
import com.google.caja.parser.js.Identifier;
import com.google.caja.parser.js.UncajoledModule;
import com.google.caja.parser.js.scope.AbstractScope;
import com.google.caja.parser.js.scope.ScopeListener;
import com.google.caja.parser.js.scope.ScopeType;
import com.google.caja.parser.js.scope.WorstCaseScopeAnalyzer;
import com.google.caja.parser.quasiliteral.Scope;
import com.google.caja.util.Lists;
import com.google.caja.util.Maps;
import com.google.caja.util.Sets;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A tree structure corresponding to the different {@link Scope}s in
 * a JavaScript program.
 *
 * @author mikesamuel@gmail.com
 */
public final class ScopeTree implements AbstractScope {
  private final AncestorChain<?> root;
  private final ScopeTree parent;
  private final List<ScopeTree> children = Lists.newArrayList();
  private final ScopeType scopeType;
  private final Map<String, Set<AncestorChain<Identifier>>> uses
      = Maps.newLinkedHashMap();

  public static ScopeTree create(AncestorChain<?> scopeRoot) {
    AncestorChain<Block> bl;
    if (scopeRoot.node instanceof UncajoledModule) {
      bl = scopeRoot.child(
          scopeRoot.cast(UncajoledModule.class).node.getModuleBody());
    } else {
      bl = scopeRoot.cast(Block.class);
    }
    ScopeListener<ScopeTree> listener = new ScopeListener<ScopeTree>() {

      public void declaration(AncestorChain<Identifier> id, ScopeTree scope) {
        Set<AncestorChain<Identifier>> uses = Sets.newLinkedHashSet();
        uses.add(id);
        scope.uses.put(id.node.getName(), uses);
      }

      public void inScope(AncestorChain<?> ac, ScopeTree scope) {
        // NOP
      }

      public void masked(
          AncestorChain<Identifier> id, ScopeTree inner, ScopeTree outer) {
        // NOP
      }

      public void splitInitialization(
          AncestorChain<Identifier> declared, ScopeTree declScope,
          AncestorChain<Identifier> initialized, ScopeTree maskingScope) {
        // NOP
      }

      public void duplicate(AncestorChain<Identifier> id, ScopeTree scope) {
        // NOP
      }

      public void read(
          AncestorChain<Identifier> id, ScopeTree useSite,
          ScopeTree definingSite) {
        if (definingSite != null) {  // Not a global
          definingSite.uses.get(id.node.getName()).add(id);
        }
      }

      public void assigned(
          AncestorChain<Identifier> id, ScopeTree useSite,
          ScopeTree definingSite) {
        if (definingSite != null) {  // Not a global
          definingSite.uses.get(id.node.getName()).add(id);
        }
      }

      public ScopeTree createScope(
          ScopeType t, AncestorChain<?> root, ScopeTree parent) {
        ScopeTree child = new ScopeTree(parent, root, t);
        if (parent != null) {
          parent.children.add(child);
        }
        return child;
      }

      public void enterScope(ScopeTree Scope) {
        // NOP
      }

      public void exitScope(ScopeTree scope) {
        // NOP
      }
    };
    return new WorstCaseScopeAnalyzer<ScopeTree>(listener).apply(bl);
  }

  private ScopeTree(ScopeTree parent, AncestorChain<?> scopeRoot, ScopeType t) {
    this.parent = parent;
    this.root = scopeRoot;
    this.scopeType = t;
  }

  public AncestorChain<?> getRoot() { return root; }

  public boolean isSymbolDeclared(String name) {
    for (ScopeTree t = this; t != null; t = t.parent) {
      if (t.uses.containsKey(name)) { return true; }
    }
    return false;
  }

  public ScopeTree getContainingScope() { return parent; }

  public ScopeType getType() { return scopeType; }

  /**
   * All the uses of the identifier in the scope in which it is defined.
   * @param identifier an identifier defined in this scope or a parent scope.
   */
  public Iterable<AncestorChain<Identifier>> usesOf(String identifier) {
    for (ScopeTree t = this; t != null; t = t.parent) {
      Set<AncestorChain<Identifier>> usesForId = t.uses.get(identifier);
      if (usesForId != null) {
        return Collections.unmodifiableSet(usesForId);
      }
    }
    return Collections.<AncestorChain<Identifier>>emptySet();
  }

  /** @param n a node that is a child of a node in this.scope. */
  public ScopeTree scopeForChild(ParseTreeNode n) {
    if (ScopeType.forNode(n) != null) {
      for (ScopeTree t : children) {
        if (t.root.node == n) { return t; }
      }
      throw new RuntimeException(
          "No scope attached to " + n + " @ " + n.getFilePosition());
    } else {
      return this;
    }
  }

  public List<ScopeTree> children() { return children; }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    toStringBuilder(0, sb);
    return sb.toString();
  }

  private void toStringBuilder(int depth, StringBuilder sb) {
    for (int d = depth; --d >= 0;) { sb.append("  "); }
    sb.append("(ScopeTree ");
    if (root.node instanceof FunctionConstructor) {
      sb.append("function");
      String name = root.cast(FunctionConstructor.class)
          .node.getIdentifierName();
      if (name != null) {
        sb.append(' ').append(name);
      }
    } else {
      String typeName = root.node.getClass().getSimpleName();
      typeName = typeName.substring(typeName.lastIndexOf(".") + 1);
      sb.append(typeName);
    }
    for (ScopeTree child : children) {
      sb.append('\n');
      child.toStringBuilder(depth + 1, sb);
    }
    sb.append(')');
  }
}
