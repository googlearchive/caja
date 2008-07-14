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
import com.google.caja.lexer.Keyword;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.ParseTreeNodeContainer;
import com.google.caja.parser.SyntheticNodes;
import com.google.caja.parser.js.CatchStmt;
import com.google.caja.parser.js.Declaration;
import com.google.caja.parser.js.FunctionConstructor;
import com.google.caja.parser.js.FunctionDeclaration;
import com.google.caja.parser.js.Identifier;
import com.google.caja.parser.js.Reference;
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.Statement;
import com.google.caja.parser.js.Operation;
import com.google.caja.parser.js.Operator;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.MessageType;
import com.google.caja.util.Pair;

import static com.google.caja.parser.SyntheticNodes.s;
import static com.google.caja.parser.quasiliteral.QuasiBuilder.substV;

import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * A scope analysis of a {@link com.google.caja.parser.ParseTreeNode}.
 *
 * @author ihab.awad@gmail.com (Ihab Awad)
 */
public class Scope {
  private enum LocalType {
    /**
     * A named function value.
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

  private enum ScopeType {
    /**
     * A Scope created from a plain block.
     * Example: the block containing 'foo' in the following --
     *
     * <pre>
     * if (someCondition) { foo; }
     * </pre>
     */
    PLAIN_BLOCK(false),

    /**
     * A Scope created from a catch block.
     * Example: the block containing 'foo' in the following --
     *
     * <pre>
     * try { doSomething(); } catch (e) { foo; }
     * </pre>
     */
    CATCH_BLOCK(false),

    /**
     * A Scope created from a function body.
     * Example: the blocks containing 'foo' in the following --
     *
     * <pre>
     * function someFunc() { foo; }
     * var someFunc = function() { foo; }
     * </pre>
     */
    FUNCTION_BODY(true),

    /**
     * A Scope created to represent the top level of a JavaScript program.
     * Example: the scope in which the variable 'foo' is defined in the following --
     *
     * <pre>
     * var foo = 3;
     * var bar = 4;
     * </pre>
     */
    PROGRAM(true),
    ;

    private final boolean declarationContainer;

    /**
     * @return whether this type of Scope can encapsulate unique 'var' declarations.
     */
    public boolean isDeclarationContainer() {
      return declarationContainer;
    }

    private ScopeType(boolean declarationContainer) {
      this.declarationContainer = declarationContainer;
    }
  }

  private final Scope parent;
  private final MessageQueue mq;
  private final ScopeType type;
  private final boolean sideEffecting;
  private boolean containsThis = false;
  private boolean containsArguments = false;
  private int tempVariableCounter = 0;
  private final Map<String, Pair<LocalType, FilePosition>> locals
      = new HashMap<String, Pair<LocalType, FilePosition>>();
  private final List<Statement> startStatements = new ArrayList<Statement>();
  // TODO(ihab.awad): importedVariables is only used by the root-most scope; it is
  // empty everywhere else. Define subclasses of Scope so that this confusing
  // overlapping of instance variables does not occur.
  private final Set<String> importedVariables = new TreeSet<String>();

  public static Scope fromProgram(Block root, MessageQueue mq) {
    Scope s = new Scope(ScopeType.PROGRAM, mq, true);
    walkBlock(s, root);
    return s;
  }

  public static Scope fromPlainBlock(Scope parent, Block root) {
    return new Scope(ScopeType.PLAIN_BLOCK, parent, true);
  }

  public static Scope fromCatchStmt(Scope parent, CatchStmt root) {
    Scope s = new Scope(ScopeType.CATCH_BLOCK, parent, true);
    declare(s, root.getException().getIdentifier(),
            LocalType.CAUGHT_EXCEPTION);
    return s;
  }

  public static Scope fromFunctionConstructor(Scope parent, FunctionConstructor root) {
    return fromFunctionConstructor(parent, root, true);
  }

  public static Scope fromParseTreeNodeContainer(Scope parent, ParseTreeNodeContainer root) {
    Scope s = new Scope(ScopeType.PLAIN_BLOCK, parent, true);
    walkBlock(s, root);
    return s;
  }

  private static Scope fromFunctionConstructor(Scope parent, FunctionConstructor root, boolean sideEffecting) {
    Scope s = new Scope(ScopeType.FUNCTION_BODY, parent, sideEffecting);

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

  private Scope(ScopeType type, MessageQueue mq, boolean sideEffecting) {
    this.type = type;
    this.parent = null;
    this.mq = mq;
    this.sideEffecting = sideEffecting;
  }

  private Scope(ScopeType type, Scope parent, boolean sideEffecting) {
    this.type = type;
    this.parent = parent;
    this.mq = parent.mq;
    this.sideEffecting = sideEffecting;
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
   * When a Scope is used for recursively processing a parse tree, steps taken on nodes contained
   * within the node of this Scope sometimes add statements (e.g., variable declarations) that
   * need to be rendered in the result before these nodes are rendered. These statements are the
   * Scope's "start statements". After processing of subordinate nodes, these statements are to
   * be found by calling this method.
   *
   * @return the statements that recursive processing of enclosed nodes has determined should be
   * rendered at the start of this Scope.
   */
  public List<Statement> getStartStatements() {
    return startStatements;
  }

  public Set<String> getImportedVariables() {
    return importedVariables;
  }

  /**
   * Add a start statement to the closest enclosing true Scope (i.e., a Scope that can contain
   * unique 'var' declarations).
   *
   * @param s a Statement.
   * @see #getStartStatements()
   */
  public void addStartOfScopeStatement(Statement s) {
    getClosestDeclarationContainer().addStartOfBlockStatement(s);
  }

  /**
   * Add a start statement to the block represented by this Scope directly.
   *
   * @param s a Statement.
   * @see #getStartStatements()
   */
  public void addStartOfBlockStatement(Statement s) {
    startStatements.add(s);
  }

  /**
   * Add a temporary variable declaration to the start of the closest enclosing true
   * scope, and return the name of the declared variable.
   *
   * @return the identifier for the newly declared variable.
   * @see #addStartOfScopeStatement(com.google.caja.parser.js.Statement)
   */
  public Identifier declareStartOfScopeTempVariable() {
    Scope s = getClosestDeclarationContainer();
    // TODO(ihab.awad): Uses private access to 's' which is of same class but distinct
    // instance. Violates capability discipline; kittens unduly sacrificed. Refactor.
    Identifier id = s(new Identifier("x" + (s.tempVariableCounter++) + "___"));
    s.addStartOfScopeStatement((Statement)substV(
        "var @id;",
        "id", id));
    return id;
  }

  /**
   * Add a variable declaration to the start of the closest enclosing true
   * scope.
   *
   * @see #addStartOfScopeStatement(com.google.caja.parser.js.Statement)
   */
  public void declareStartOfScopeVariable(Identifier id) {
    Scope s = getClosestDeclarationContainer();
    // TODO(ihab.awad): Uses private access to 's' which is of same class but distinct
    // instance. Violates capability discipline; kittens unduly sacrificed. Refactor.
    s.addStartOfScopeStatement((Statement)substV(
        "var @id;",
        "id", id));
  }

  private Scope getClosestDeclarationContainer() {
    if (!type.isDeclarationContainer()) {
      assert(parent != null);
      return parent.getClosestDeclarationContainer();
    }
    return this;
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
   * Is a given symbol imported by this module?
   *
   * @param name an identifier.
   * @return whether 'name' is a free variable of the enclosing module.
   */
  public boolean isImported(String name) {
    if (locals.containsKey(name)) return false;
    if (parent == null) { return importedVariables.contains(name); }
    return parent.isImported(name);
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

  private static void addImportedVariable(Scope s, String name) {
    Scope target = s;
    while (target.getParent() != null) { target = target.getParent(); }
    if (target.importedVariables.contains(name)) { return; }
    target.importedVariables.add(name);
  }

  private static LocalType computeDeclarationType(Scope s, Declaration decl) {
    if (decl instanceof FunctionDeclaration) {
      Scope s2 = fromFunctionConstructor(s, ((FunctionDeclaration)decl).getInitializer(), false);
      return s2.hasFreeThis() ? LocalType.CONSTRUCTOR : LocalType.DECLARED_FUNCTION;
    }
    return LocalType.DATA;
  }

  private static void walkBlock(final Scope s, ParseTreeNode root) {
    SymbolHarvestVisitor v = new SymbolHarvestVisitor();
    v.visit(root);

    // Record in this scope all the declarations that have been harvested
    // by the visitor.
    if (s.sideEffecting) {
      for (Declaration decl : v.getDeclarations()) {
        declare(s, decl.getIdentifier(), computeDeclarationType(s, decl));
      }
    }

    // Now resolve all the references harvested by the visitor. If they have
    // not been defined in the scope chain (including the declarations we just
    // harvested), then they must be free variables, so record them as such.
    for (String name : v.getReferences()) {
      if (ReservedNames.ARGUMENTS.equals(name)) {
        s.containsArguments = true;
      } else if (Keyword.THIS.toString().equals(name)) {
        s.containsThis = true;
      } else if (!s.isDefined(name) && s.sideEffecting) {
        addImportedVariable(s, name);
      }
    }
  }

  // A SymbolHarvestVisitor traverses a parse tree node tree and harvests
  // declarations and references for scope analysis. It stops the traversal
  // at the right places according to JavaScript scoping rules.
  //
  // TODO(ihab.awad): Refactor to use standard Caja Visitor. Currently not
  // using it because, due to the MEMBER_ACCESS case, we need more control
  // over when to stop traversing the children of a node.
  private static class SymbolHarvestVisitor {
    private final SortedSet<String> references = new TreeSet<String>();
    private final List<Declaration> declarations = new ArrayList<Declaration>();
    private final List<String> exceptionVariables = new ArrayList<String>();

    public SortedSet<String> getReferences() { return references; }

    public List<Declaration> getDeclarations() { return declarations; }

    public void visit(ParseTreeNode node) {
      // Dispatch to methods for specific node types of interest
      if (node instanceof FunctionConstructor) {
        visitFunctionConstructor((FunctionConstructor)node);
      } else if (node instanceof CatchStmt) {
        visitCatchStmt((CatchStmt)node);
      } else if (node instanceof Declaration) {
        visitDeclaration((Declaration)node);
      } else if (node instanceof Operation) {
        visitOperation((Operation)node);
      } else  if (node instanceof Reference) {
        visitReference((Reference)node);
      } else {
        visitChildren(node);
      }
    }

    private void visitChildren(ParseTreeNode node) {
      for (ParseTreeNode c : node.children()) { visit(c); }
    }

    private void visitFunctionConstructor(FunctionConstructor node) {
      if (node.getAttributes().is(SyntheticNodes.SYNTHETIC)) {
        // Synthetic function definitions are treated as "transparent"; our
        // scope analysis should "see through" them as though they were just
        // part of the surrounding code.
        visitChildren(node);
      } else {
        // Stuff inside a nested function is not part of this scope,
        // so stop the traversal.
      }
    }

    private void visitCatchStmt(CatchStmt node) {
      // Skip the CatchStmt's exception variable -- that is only defined
      // within the CatchStmt's body -- but dig into the body itself to grab
      // all the declarations within it, which *are* hoisted into this scope.
      exceptionVariables.add(node.getException().getIdentifierName());
      visit(node.getBody());
      exceptionVariables.remove(exceptionVariables.size() - 1);
    }

    private void visitDeclaration(Declaration node) {
      if (!node.getAttributes().is(SyntheticNodes.SYNTHETIC)) {
        declarations.add(node);
      }
      if (node.getInitializer() != null) {
        visit(node.getInitializer());
      }
    }

    // TODO(ihab.awad): Change the ParseTreeNode type for the right hand sides
    // of a member access to be a StringLiteral, so we can eliminate the special
    // case here. Also collapse MEMBER_ACCESS and SQUARE_BRACKET and make the
    // form of the output a rendering decision.
    private void visitOperation(Operation node) {
      if (node.getOperator() == Operator.MEMBER_ACCESS) {
        visit(node.children().get(0));
      } else {
        visitChildren(node);
      }
    }

    private void visitReference(Reference node) {
      if (!node.getAttributes().is(SyntheticNodes.SYNTHETIC) &&
          !exceptionVariables.contains(node.getIdentifierName())) {
        references.add(node.getIdentifierName());
      }
    }
  }

  /**
   * Add a symbol to the symbol table for this scope with the given type.
   * If this symbol redefines another symbol with a different type, or masks
   * an exception, then an error will be added to this Scope's MessageQueue.
   */
  private static void declare(Scope s, Identifier ident, LocalType type) {
    String name = ident.getName();

    if ("caja".equals(name)) {
      s.mq.getMessages().add(new Message(
          RewriterMessageType.CANNOT_REDECLARE_CAJA,
          ident.getFilePosition()));
    }

    Pair<LocalType, FilePosition> oldDefinition = s.locals.get(name);
    if (oldDefinition != null) {
      LocalType oldType = oldDefinition.a;
      if (oldType != type
          || oldType.implies(LocalType.FUNCTION)
          || type.implies(LocalType.FUNCTION)) {
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
      // Do not generate a LINT error in the case where a function masks
      // itself.  We recognize a self-mask when we come across a "new"
      // function in the same scope as a declared function or constructor.
      if (maskedType != type
          && !((maskedType == LocalType.DECLARED_FUNCTION ||
                  maskedType == LocalType.CONSTRUCTOR)
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
