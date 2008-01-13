// Copyright (C) 2007 Google Inc.
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

package com.google.caja.parser.quasiliteral;

import com.google.caja.parser.AncestorChain;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.Visitor;
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.Declaration;
import com.google.caja.parser.js.FunctionConstructor;
import com.google.caja.parser.js.FunctionDeclaration;
import com.google.caja.parser.js.Reference;
import com.google.caja.plugin.ReservedNames;

import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;
import java.util.HashSet;

/**
 * A scope analysis of a {@link com.google.caja.parser.ParseTreeNode}.
 *
 * @author ihab.awad@gmail.com (Ihab Awad)
 */
public class Scope {
  private enum LocalType {
    /**
     * A named function value, visible only within its own body.
     * Examples: "foo" in the following --
     *
     * <pre>
     * var y = function foo() { };
     * zip(function foo() { });
     * </pre>
     */
    FUNCTION(),

    /**
     * A function declaration, visible in its enclosing scope.
     * Example: "foo" in the following --
     *
     * <pre>
     * function foo() { }
     * </pre>
     */
    DECLARED_FUNCTION(FUNCTION),

    /**
     * A constructor declaration, i.e., one which mentions 'this' in its body,
     * Example: "foo" in the following --
     *
     * <pre>
     * function foo() { this.x = 3; }
     * </pre>
     */
    CONSTRUCTOR(DECLARED_FUNCTION),

    /**
     * A variable containing arbitrary data (including functions).
     * Examples: "x", "y", "z" and "t" in the following --
     *
     * <pre>
     * var x;
     * var y = 3;
     * var z = function() { };
     * var t = function foo() { };
     * </pre>
     */
    DATA();

    private final HashSet<LocalType> implications;

    private LocalType(LocalType... implications) {
      this.implications = new HashSet<LocalType>();
      this.implications.addAll(Arrays.asList(implications));
    }

    public boolean implies(LocalType type) {
      if (this == type) return true;
      for (LocalType i : implications) {
        if (i.implies(type)) return true;
      }
      return false;
    }
  }
  
  private Scope parent;
  private boolean containsThis;
  private boolean containsArguments;
  private Map<String, LocalType> locals = new HashMap<String, LocalType>();

  // TODO(ihab.awad): Create scope from static methods.
  // TODO(ihab.awad): Take a message queue for adding error messages.

  /**
   * Create a scope from some arbitrary parse tree nodes.
   *
   * @param parent the parent scope.
   * @param root the node to use as a root.
   */
  public Scope(Scope parent, ParseTreeNode root) {
    this.parent = parent;
    walkBlock(root);
  }


  /**
   * Create a top-level scope for a program.
   *
   * @param root the block representing the top level of the program.
   */
  public Scope(Block root) {
    walkBlock(root);
  }

  /**
   * Create a nested scope for a function in a program.
   *
   * @param parent the parent scope of the function.
   * @param root the function constituting the nested scope.
   */
  public Scope(Scope parent, FunctionConstructor root) {
    this.parent = parent;
    
    if (root.getIdentifierName() != null &&
        parent.getType(root.getIdentifierName()) != LocalType.DECLARED_FUNCTION) {
      locals.put(root.getIdentifierName(), LocalType.FUNCTION);
    }

    for (ParseTreeNode n : root.getParams()) walkBlock(n);
    walkBlock(root.getBody());
  }

  /**
   * The parent of this scope.
   *
   * @return a {@code Scope} or {@code null}.
   */
  public Scope getParent() {
    return parent;
  }

  /**
   * Does this scope mention "this" freely?
   *
   * <p>If "this" is only mentioned within a function definition within
   * this scope, then the result is <tt>false</tt>, since that "this"
   * isn't a free occurrence.
   *
   * @return whether this block has a free "this".
   */
  public boolean hasFreeThis() {
    return containsThis;
  }

  /**
   * Does this scope mention "arguments" freely?
   *
   * <p>If "arguments" is only mentioned within a function definition
   * within this scope, then the result is <tt>false</tt>, since that
   * "arguments" isn't a free occurrence.
   *
   * @return whether this block has a free "arguments".
   */
  public boolean hasFreeArguments() {
    return containsArguments;
  }

  /**
   * Does this scope or some enclosing scope define a name?
   *
   * @param name an identifier.
   * @return whether 'name' is defined within this scope.
   */
  public boolean isDefined(String name) {
    return getType(name) != null;
  }

  /**
   * In this scope or some enclosing scope, is a given name
   * defined as a function?
   *
   * @param name an identifier.
   * @return whether 'name' is defined as a function within this
   * scope. If 'name' is not defined, return false.
   */
  public boolean isFunction(String name) {
    return isDefined(name) && getType(name).implies(LocalType.FUNCTION);
  }

  /**
   * In this scope or some enclosing scope, is a given name
   * defined as a declared function?
   *
   * @param name an identifier.
   * @return whether 'name' is defined as a declared function within this
   * scope. If 'name' is not defined, return false.
   */
  public boolean isDeclaredFunction(String name) {
    return isDefined(name) && getType(name).implies(LocalType.DECLARED_FUNCTION);
  }

  /**
   * In this scope or some enclosing scope, is a given name defined
   * as a constructor?
   *
   * @param name an identifier.
   * @return whether 'name' is defined as a constructor within this
   * scope. If 'name' is not defined, return false.
   */
  public boolean isConstructor(String name) {
    return isDefined(name) && getType(name).implies(LocalType.CONSTRUCTOR);
  }

  /**
   * Is a given symbol global?
   *
   * @param name an identifier.
   * @return whether 'name' is a defined global variable.
   */
  public boolean isGlobal(String name) {
    return
        parent == null ||
        (!locals.containsKey(name) && parent.isGlobal(name));
  }

  private LocalType getType(String name) {
    return locals.containsKey(name) ?
        locals.get(name) :
        parent != null ? parent.getType(name) : null;
  }

  private LocalType computeDeclarationType(Declaration decl) {
    if (decl instanceof FunctionDeclaration) {
      Scope s2 = new Scope(this, ((FunctionDeclaration)decl).getInitializer());
      return s2.hasFreeThis() ? LocalType.CONSTRUCTOR : LocalType.DECLARED_FUNCTION;
    }
    return LocalType.DATA;
  }

  private void walkBlock(ParseTreeNode root) {
    root.acceptPreOrder(new Visitor() {
      public boolean visit(AncestorChain<?> chain) {
        if (chain.node instanceof FunctionConstructor) {
          return false;
        }
        if (chain.node instanceof Declaration) {
          LocalType type = computeDeclarationType((Declaration)chain.node);
          String name = ((Declaration)chain.node).getIdentifierName();
          if (locals.containsKey(name) && locals.get(name) != type) {
            throw new RuntimeException("Duplicate definition of local: " + name);
          }
          locals.put(name, type);
        }
        if (chain.node instanceof Reference) {
          String name = ((Reference)chain.node).getIdentifierName();
          if (ReservedNames.ARGUMENTS.equals(name)) { containsArguments = true; }
          if (ReservedNames.THIS.equals(name)) { containsThis = true; }
        }
        return true;
      }
    },
    null);
  }
}
