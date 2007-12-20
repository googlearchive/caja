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

/**
 * A scope analysis of a {@link com.google.caja.parser.ParseTreeNode}.
 * 
 * <p>TODO(ihab.awad): All exceptions must be CajaExceptions.
 *
 * @author ihab.awad@gmail.com (Ihab Awad)
 */
public class Scope {
  private enum LocalType {
    // A function declaration, visible in its enclosing scope.
    // Example: "foo" in the following --
    //     function foo() { }
    DECLARED_FUNCTION,

    // A named function value, visible only within its own body.
    // Examples: "foo" in the following --
    //     var y = function foo() { };
    //     zip(function foo() { });
    NAMED_FUNCTION_VALUE,
    
    // A variable containing arbitrary data (including functions).
    // Examples: "x", "y", "z" and "t" in the following --
    //     var x;
    //     var y = 3;
    //     var z = function() { };
    //     var t = function foo() { };
    DATA
  }
  
  private Scope parent;
  private boolean containsThis;
  private boolean containsArguments;
  private Map<String, LocalType> locals = new HashMap<String, LocalType>();

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
      locals.put(root.getIdentifierName(), LocalType.NAMED_FUNCTION_VALUE);
    }

    for (ParseTreeNode n : root.getParams()) walkBlock(n);
    walkBlock(root.getBody());
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
    LocalType t = getType(name);
    return
        t == LocalType.DECLARED_FUNCTION ||
        t == LocalType.NAMED_FUNCTION_VALUE;
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
    return getType(name) == LocalType.DECLARED_FUNCTION;
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

  private void walkBlock(ParseTreeNode root) {
    root.acceptPreOrder(new Visitor() {
      public boolean visit(AncestorChain<?> chain) {
        if (chain.node instanceof FunctionConstructor) {
          return false;
        }
        if (chain.node instanceof Declaration) {
          LocalType type = chain.node instanceof FunctionDeclaration ?
              LocalType.DECLARED_FUNCTION : LocalType.DATA;
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