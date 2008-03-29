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

import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.InputSource;
import com.google.caja.parser.AncestorChain;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.Visitor;
import com.google.caja.parser.js.CatchStmt;
import com.google.caja.parser.js.Declaration;
import com.google.caja.parser.js.FunctionConstructor;
import com.google.caja.parser.js.FunctionDeclaration;
import com.google.caja.parser.js.Identifier;
import com.google.caja.parser.js.Reference;
import com.google.caja.parser.js.Block;
import com.google.caja.plugin.ReservedNames;
import com.google.caja.plugin.SyntheticNodes;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.MessageType;
import com.google.caja.util.Pair;

import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.net.URI;

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
     * A constructor declaration, i.e., one which mentions 'this' in its body.
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
    DATA(),

    /**
     * A variable defined in a catch block.
     * Example: "foo" in the following --
     *
     * <pre>
     * catch (foo) { this.x = 3; }
     * </pre>
     */
    CAUGHT_EXCEPTION,
    ;

    private final HashSet<LocalType> implications = new HashSet<LocalType>();

    private LocalType(LocalType... implications) {
      this.implications.add(this);
      for (LocalType implication : implications) {
        this.implications.addAll(implication.implications);
      }
    }

    public boolean implies(LocalType type) {
      return implications.contains(type);
    }
  }

  private static final FilePosition PRIMORDIAL_OBJECTS_FILE_POSITION =
      FilePosition.instance(
          new InputSource(URI.create("built-in:///js-primordial-objects")),
          0, 0, 0, 0, 0, 0, 0, 0);

  private final Scope parent;
  private final MessageQueue mq;
  private boolean containsThis = false;
  private boolean containsArguments = false;
  private boolean fromCatchStmt = false;
  private int tempVariableCounter = 0;
  private final Map<String, Pair<LocalType, FilePosition>> locals
      = new HashMap<String, Pair<LocalType, FilePosition>>();

  public static Scope fromRootBlock(Block root, MessageQueue mq) {
    Scope s = new Scope(mq);
    addPrimordialObjects(s);
    walkBlock(s, root);
    return s;
  }
  
  public static Scope fromBlock(Scope parent, Block root) {
    Scope s = new Scope(parent);
    walkBlock(s, root);
    return s;
  }

  public static Scope fromCatchStmt(Scope parent, CatchStmt root) {
    Scope s = new Scope(parent);
    declare(s, root.getException().getIdentifier(),
            LocalType.CAUGHT_EXCEPTION);
    s.fromCatchStmt = true;
    return s;
  }

  public static Scope fromFunctionConstructor(Scope parent, FunctionConstructor root) {
    Scope s = new Scope(parent);

    // A function's name is bound to it in its body. After executing
    //    var g = function f() { return f; };
    // the following is true
    //    typeof f === 'undefined' && g === g()
    if (root.getIdentifierName() != null) {
      declare(s, root.getIdentifier(), LocalType.FUNCTION);
    }

    for (ParseTreeNode n : root.getParams()) {
      walkBlock(s, n);
    }
    
    walkBlock(s, root.getBody());

    return s;
  }
  
  public static Scope fromParseTreeNodeContainer(Scope parent, ParseTreeNodeContainer root) {
    Scope s = new Scope(parent);
    walkBlock(s, root);
    return s;
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
   * Allocate a new, uniquely named temporary variable, which is named in a manner
   * inaccessible to Caja code.
   *
   * <p>CAUTION: Creating a temporary variable in this way is effectively changing
   * the Scope's knowledge of the parse tree node from underneath it, so that this
   * Scope is now no longer an accurate reflection of the parse tree node.
   *
   * <p>Say you have user code that refers to global variable 'x'. If we assign a
   * temporary variable 'x', then subsequent consultations of this Scope would lie
   * about the fact that the user is using a global ... UNLESS we ALSO add to the
   * parse tree the corresponding 'var x' statement.
   *
   * <p>That said, our temporary variables are underscore terminated so that they
   * are not mentionable by legal Caja code.
   *
   * @return an new variable name.
   */
  public String newTempVariable() {
    if (fromCatchStmt) return parent.newTempVariable();
    String name = "x" + (tempVariableCounter++) + "___";
    declare(this, new Identifier(name), LocalType.DATA);
    return name;
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
   * Is this effectively the global scope.
   *
   * <p>If this scope represents a <tt>catch</tt> block, then its parent is
   * consulted, since the <tt>catch</tt> block provides a scope only for the
   * exception variable.
   */
  public boolean isGlobal() {
    return fromCatchStmt ? parent.isGlobal() : parent == null;
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

  private boolean isDefinedAs(String name, LocalType type) {
    return isDefined(name) && getType(name).implies(type);
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
    return isDefinedAs(name, LocalType.FUNCTION);
  }

  /**
   * True if name is the name of the variable that a {@code catch} block's
   * exception is bound to.
   *
   * @param name an identifier.
   * @return whether 'name' is defined as the exception variable of
   *   a {@code catch} block.
   */
  public boolean isException(String name) {
    return isDefinedAs(name, LocalType.CAUGHT_EXCEPTION);
  }

  /**
   * In this scope or some enclosing scope, is a given name
   * defined as a declared function?
   *
   * @param node an identifier.
   * @return whether 'name' is defined as a declared function within this
   *   scope. If 'name' is not defined, return false.
   */
  public boolean isDeclaredFunctionReference(ParseTreeNode node) {
    return node instanceof Reference &&
        isDeclaredFunction(((Reference)node).getIdentifierName());
  }

  /**
   * In this scope or some enclosing scope, is a given name
   * defined as a declared function?
   *
   * @param name an identifier.
   * @return whether 'name' is defined as a declared function within this
   *   scope. If 'name' is not defined, return false.
   */
  public boolean isDeclaredFunction(String name) {
    return isDefinedAs(name, LocalType.DECLARED_FUNCTION);
  }

  /**
   * In this scope or some enclosing scope, is a given name defined
   * as a constructor?
   *
   * @param name an identifier.
   * @return whether 'name' is defined as a constructor within this
   *   scope. If 'name' is not defined, return false.
   */
  public boolean isConstructor(String name) {
    return isDefinedAs(name, LocalType.CONSTRUCTOR);
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
    Scope current = this;
    do {
      Pair<LocalType, FilePosition> symbolDefinition = current.locals.get(name);
      if (symbolDefinition != null) { return symbolDefinition.a; }
      current = current.parent;
    } while (current != null);
    return null;
  }

  private Scope(MessageQueue mq) {    
    this.parent = null;
    this.mq = mq;
  }

  private Scope(Scope parent) {
    this.parent = parent;
    this.mq = parent.mq;
  }

  /**
   * Add the primordial objects to a top-level scope. By marking some of these as
   * CONSTRUCTORs, we allow them to be used as constructors at compile time. A
   * container writer makes the separate decision whether to make them members of
   *  ___OUTERS___ or not.
   */
  private static void addPrimordialObjects(Scope s) {
    addLocal(s, "Global", LocalType.DATA);
    addLocal(s, "Object", LocalType.CONSTRUCTOR);
    addLocal(s, "Function", LocalType.DATA);
    addLocal(s, "Array", LocalType.DATA);
    addLocal(s, "String", LocalType.DATA);
    addLocal(s, "Boolean", LocalType.DATA);
    addLocal(s, "Number", LocalType.DATA);
    addLocal(s, "Math", LocalType.DATA);
    addLocal(s, "Date", LocalType.CONSTRUCTOR);
    addLocal(s, "RegExp", LocalType.DATA);
    addLocal(s, "Error", LocalType.CONSTRUCTOR);
    addLocal(s, "EvalError", LocalType.CONSTRUCTOR);
    addLocal(s, "RangeError", LocalType.CONSTRUCTOR);
    addLocal(s, "ReferenceError", LocalType.CONSTRUCTOR);
    addLocal(s, "SyntaxError", LocalType.CONSTRUCTOR);
    addLocal(s, "TypeError", LocalType.CONSTRUCTOR);
    addLocal(s, "URIError", LocalType.CONSTRUCTOR);
  }
  
  private static void addLocal(Scope s, String name, LocalType type) {
    s.locals.put(
        name,
        new Pair<LocalType, FilePosition>(
            type,
            PRIMORDIAL_OBJECTS_FILE_POSITION));
  }

  private static LocalType computeDeclarationType(Scope s, Declaration decl) {
    if (decl instanceof FunctionDeclaration) {
      Scope s2 = fromFunctionConstructor(s, ((FunctionDeclaration)decl).getInitializer());
      return s2.hasFreeThis() ? LocalType.CONSTRUCTOR : LocalType.DECLARED_FUNCTION;
    }
    return LocalType.DATA;
  }

  private static void walkBlock(final Scope s, ParseTreeNode root) {
    root.acceptPreOrder(new Visitor() {
      public boolean visit(AncestorChain<?> chain) {
        if (chain.node instanceof FunctionConstructor) {
          return false;
        } else if (chain.node instanceof CatchStmt) {
          // We skip the CatchStmt's exception variable -- that is only defined within the
          // CatchStmt's body -- but we dig into the body itself to grab all the declarations
          // within it, which *are* hoisted into the parent scope.
          ((CatchStmt)chain.node).getBody().acceptPreOrder(this, null);
          return false;
        } else if (chain.node instanceof Declaration) {
          Declaration decl = (Declaration) chain.node;
          declare(s, decl.getIdentifier(), computeDeclarationType(s, decl));
        } else if (chain.node instanceof Reference) {
          String name = ((Reference)chain.node).getIdentifierName();
          if (ReservedNames.ARGUMENTS.equals(name)) {
            s.containsArguments = true;
          }
          if (ReservedNames.THIS.equals(name)) { s.containsThis = true; }
        }
        return true;
      }
    },
    null);
  }

  /**
   * Add a symbol to the symbol table for this scope with the given type.
   * If this symbol redefines another symbol with a different type, or masks
   * an exception, then an error will be added to this Scope's MessageQueue.
   */
  private static void declare(Scope s, Identifier ident, LocalType type) {
    String name = ident.getName();
    Pair<LocalType, FilePosition> oldDefinition = s.locals.get(name);
    if (oldDefinition != null) {
      LocalType oldType = oldDefinition.a;
      if (oldType != type) {
        // This is an error because redeclaring a function declaration as a
        // var makes analysis hard.
        s.mq.getMessages().add(new Message(
            MessageType.SYMBOL_REDEFINED,
            MessageLevel.ERROR,
            ident.getFilePosition(),
            MessagePart.Factory.valueOf(name),
            oldDefinition.b));
      }
    }
    for (Scope ancestor = s.parent; ancestor != null;
         ancestor = ancestor.parent) {
      Pair<LocalType, FilePosition> maskedDefinition
          = ancestor.locals.get(name);
      if (maskedDefinition == null) { continue; }

      LocalType maskedType = maskedDefinition.a;
      if (maskedType != type
          && !(maskedType == LocalType.DECLARED_FUNCTION
               && type == LocalType.FUNCTION)) {
        // Since different interpreters disagree about how exception
        // declarations affect local variable declarations, we need to
        // prevent exceptions masking locals and vice-versa.
        MessageLevel level = (
            (type == LocalType.CAUGHT_EXCEPTION
             || maskedType == LocalType.CAUGHT_EXCEPTION)
            ? MessageLevel.ERROR
            : MessageLevel.LINT);
        if (!ident.getAttributes().is(SyntheticNodes.SYNTHETIC) &&
            ident.getFilePosition() != null) {
          s.mq.getMessages().add(new Message(
              MessageType.MASKING_SYMBOL,
              level,
              ident.getFilePosition(),
              MessagePart.Factory.valueOf(name),
              maskedDefinition.b));
        }
      }
      break;
    }

    s.locals.put(name, Pair.pair(type, ident.getFilePosition()));
  }
}
