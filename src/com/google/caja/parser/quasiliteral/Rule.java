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

import com.google.caja.lexer.Keyword;
import com.google.caja.lexer.TokenConsumer;
import com.google.caja.parser.AbstractParseTreeNode;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.ParseTreeNodeContainer;
import com.google.caja.parser.ParseTreeNodes;
import com.google.caja.parser.js.Declaration;
import com.google.caja.parser.js.Expression;
import com.google.caja.parser.js.FormalParam;
import com.google.caja.parser.js.FunctionConstructor;
import com.google.caja.parser.js.Identifier;
import com.google.caja.parser.js.Literal;
import com.google.caja.parser.js.Operation;
import com.google.caja.parser.js.Operator;
import com.google.caja.parser.js.Reference;
import com.google.caja.parser.js.StringLiteral;
import com.google.caja.parser.js.SyntheticNodes;
import static com.google.caja.parser.js.SyntheticNodes.s;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.RenderContext;
import com.google.caja.util.Callback;
import com.google.caja.util.Pair;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A rewriting rule supplied by a subclass.
 */
public abstract class Rule implements MessagePart {

  /**
   * The special return value from a rule that indicates the rule
   * does not apply to the supplied input.
   */
  public static final ParseTreeNode NONE =
      new AbstractParseTreeNode<ParseTreeNode>() {
        @Override public Object getValue() { return null; }
        public void render(RenderContext r) {
          throw new UnsupportedOperationException();
        }
        public TokenConsumer makeRenderer(
            Appendable out, Callback<IOException> exHandler) {
          throw new UnsupportedOperationException();
        }
      };

  private String name;
  private Rewriter rewriter;
  private RuleDescription description;

  /**
   * Creates a new Rule, inferring name and other state from the {@link #fire}
   * method's {@link RuleDescription}.
   */
  public Rule() {
    setName(getRuleDescription().name());
  }

  /**
   * Create a new {@code Rule}.
   *
   * @param name the unique name of this rule.
   */
  public Rule(String name, Rewriter rewriter) {
    this.name = name;
    this.rewriter = rewriter;
  }

  /**
   * @return the name of this {@code Rule}.
   */
  public String getName() {
    assert this.name != null;
    return name;
  }

  /**
   * Set the name of this {@code Rule}.
   */
  public void setName(String name) {
    assert this.name == null;
    this.name = name;
  }

  /**
   * @return the rewriter this {@code Rule} uses.
   */
  public Rewriter getRewriter() { return rewriter; }

  /**
   * Set the rewriter this {@code Rule} uses.
   */
  public void setRewriter(Rewriter rewriter) {
    this.rewriter = rewriter;
  }

  /**
   * Gets the {@link RuleDescription} annotation on the {@link #fire} method.
   * @throws IllegalStateException if there is no such annotation.
   */
  public RuleDescription getRuleDescription() {
    if (description == null) {
      Method fire;
      try {
        fire = getClass().getMethod("fire", new Class<?>[] {
              ParseTreeNode.class, Scope.class, MessageQueue.class
            });
      } catch (NoSuchMethodException e) {
        NoSuchMethodError error = new NoSuchMethodError();
        error.initCause(e);
        throw error;
      }
      description = fire.getAnnotation(RuleDescription.class);
      if (description == null) {
        throw new IllegalStateException("RuleDescription not found");
      }
    }
    return description;
  }

  /**
   * Process the given input, returning a rewritten node.
   *
   * @param node an input node.
   * @param scope the current scope.
   * @param mq a {@code MessageQueue} for error reporting.
   * @return the rewritten node, or {@link #NONE} to indicate
   * that this rule does not apply to the given input.
   */
  public abstract ParseTreeNode fire(
      ParseTreeNode node,
      Scope scope,
      MessageQueue mq);

  /**
   * @see MessagePart#format(MessageContext,Appendable)
   */
  public void format(MessageContext mc, Appendable out) throws IOException {
    out.append("Rule \"" + name + "\"");
  }

  protected final void expandEntry(
      Map<String, ParseTreeNode> bindings,
      String key,
      Scope scope,
      MessageQueue mq) {
    bindings.put(key, rewriter.expand(bindings.get(key), scope, mq));
  }

  protected final void expandEntries(
      Map<String, ParseTreeNode> bindings,
      Scope scope,
      MessageQueue mq) {
    for (String key : bindings.keySet()) {
      expandEntry(bindings, key, scope, mq);
    }
  }

  protected final ParseTreeNode expandAll(ParseTreeNode node, Scope scope, MessageQueue mq) {
    return expandAllTo(node, node.getClass(), scope, mq);
  }

  protected final ParseTreeNode expandAllTo(
      ParseTreeNode node,
      Class<? extends ParseTreeNode> parentNodeClass,
      Scope scope,
      MessageQueue mq) {
    List<ParseTreeNode> rewrittenChildren = new ArrayList<ParseTreeNode>();
    for (ParseTreeNode child : node.children()) {
      rewrittenChildren.add(rewriter.expand(child, scope, mq));
    }

    ParseTreeNode result = ParseTreeNodes.newNodeInstance(
        parentNodeClass,
        node.getValue(),
        rewrittenChildren);
    result.getAttributes().putAll(node.getAttributes());
    result.getAttributes().remove(ParseTreeNode.TAINTED);

    return result;
  }

  protected ParseTreeNode getFunctionHeadDeclarations(Scope scope) {
    List<ParseTreeNode> stmts = new ArrayList<ParseTreeNode>();

    if (scope.hasFreeArguments()) {
      stmts.add(QuasiBuilder.substV(
          "var @la = ___.args(@ga);",
          "la", s(new Identifier(ReservedNames.LOCAL_ARGUMENTS)),
          "ga", newReference(ReservedNames.ARGUMENTS)));
    }

    return new ParseTreeNodeContainer(stmts);
  }

  protected Reference newReference(String name) {
    return new Reference(s(new Identifier(name)));
  }

  protected Expression newCommaOperation(List<? extends ParseTreeNode> operands) {
    if (operands.isEmpty()) { return Operation.undefined(); }
    Expression result = (Expression)operands.get(0);
    for (int i = 1; i < operands.size(); i++) {
      result = Operation.create(Operator.COMMA, result, (Expression)operands.get(i));
    }
    return result;
  }

  protected Pair<ParseTreeNode, ParseTreeNode> reuse(
      ParseTreeNode value,
      Scope scope,
      MessageQueue mq) {
    ParseTreeNode reference = new Reference(
        scope.declareStartOfScopeTempVariable());
    ParseTreeNode variableDefinition = QuasiBuilder.substV(
        "@ref = @rhs;",
        "ref", reference,
        "rhs", rewriter.expand(value, scope, mq));
    return new Pair<ParseTreeNode, ParseTreeNode>(
        reference,
        variableDefinition);
  }

  protected Pair<ParseTreeNode, ParseTreeNode> reuseAll(
      ParseTreeNode arguments,
      Scope scope,
      MessageQueue mq) {
    List<ParseTreeNode> refs = new ArrayList<ParseTreeNode>();
    List<ParseTreeNode> rhss = new ArrayList<ParseTreeNode>();

    for (int i = 0; i < arguments.children().size(); i++) {
      Pair<ParseTreeNode, ParseTreeNode> p = reuse(
          arguments.children().get(i),
          scope,
          mq);
      refs.add(p.a);
      rhss.add(p.b);
    }

    return new Pair<ParseTreeNode, ParseTreeNode>(
        new ParseTreeNodeContainer(refs),
        new ParseTreeNodeContainer(rhss));
  }

  protected void checkFormals(ParseTreeNode formals, MessageQueue mq) {
    for (ParseTreeNode formal : formals.children()) {
      FormalParam f = (FormalParam) formal;
      if (!isSynthetic(f.getIdentifier())
          && f.getIdentifierName().endsWith("__")) {
        mq.addMessage(
            RewriterMessageType.VARIABLES_CANNOT_END_IN_DOUBLE_UNDERSCORE,
            f.getFilePosition(), this, f);
      }
    }
  }

  protected static boolean isSynthetic(Identifier node) {
    return node.getAttributes().is(SyntheticNodes.SYNTHETIC);
  }

  protected static boolean isSynthetic(Reference node) {
    return isSynthetic(node.getIdentifier());
  }

  protected static boolean isSynthetic(FunctionConstructor node) {
    return node.getAttributes().is(SyntheticNodes.SYNTHETIC);
  }

  protected static String getReferenceName(ParseTreeNode ref) {
    return ((Reference)ref).getIdentifierName();
  }

  protected static String getIdentifierName(ParseTreeNode id) {
    return ((Identifier)id).getValue();
  }

  /**
   * Produce a StringLiteral from node's identifier.
   * @param node an identifier or node that contains a single identifier like
   *   a declaration or reference.
   * @return a string literal whose unescaped content is identical to the
   *   identifier's value, and whose file position is that of node.
   */
  protected static final StringLiteral toStringLiteral(ParseTreeNode node) {
    Identifier ident;
    if (node instanceof Reference) {
      ident = ((Reference) node).getIdentifier();
    } else if (node instanceof Declaration) {
      ident = ((Declaration) node).getIdentifier();
    } else {
      ident = (Identifier) node;
    }
    StringLiteral sl = new StringLiteral(
        StringLiteral.toQuotedValue(ident.getName()));
    sl.setFilePosition(ident.getFilePosition());
    return sl;
  }

  protected boolean literalsEndWith(ParseTreeNode container, String suffix) {
    for (ParseTreeNode n : container.children()) {
      assert(n instanceof StringLiteral);
      if (((StringLiteral)n).getUnquotedValue().endsWith(suffix)) {
        return true;
      }
    }
    return false;
  }

  protected boolean literalsContain(ParseTreeNode container, String key) {
    for (ParseTreeNode n : container.children()) {
      assert(n instanceof StringLiteral);
      if (((StringLiteral)n).getUnquotedValue().equals(key)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Matches using the Quasi-pattern from {@link RuleDescription#matches} and
   * returns the bindings if the match succeeded, or null otherwise.
   * @return null iff node was not matched.
   */
  protected Map<String, ParseTreeNode> match(ParseTreeNode node) {
    Map<String, ParseTreeNode> bindings = makeBindings();
    if (QuasiBuilder.match(getRuleDescription().matches(), node, bindings)) {
      return bindings;
    }
    return null;
  }

  protected static Map<String, ParseTreeNode> makeBindings() {
    return new LinkedHashMap<String, ParseTreeNode>();
  }

  /**
   * Substitutes bindings into the Quasi-pattern from
   * {@link RuleDescription#substitutes}.
   */
  protected ParseTreeNode subst(Map<String, ParseTreeNode> bindings) {
    return QuasiBuilder.subst(getRuleDescription().substitutes(), bindings);
  }

  /**
   * Split the target of a read/set operation into an LHS, an RHS, and
   * an ordered list of temporary variables needed to ensure proper order
   * of execution.
   * @param operand uncajoled expression that can be used as both an LHS and
   *    an RHS.
   * @return null if operand is not a valid LHS, or its subexpressions do
   *    not cajole.
   */
  ReadAssignOperands deconstructReadAssignOperand(
      Expression operand, Scope scope, MessageQueue mq) {
    return deconstructReadAssignOperand(operand, scope, mq, true);
  }
  
  ReadAssignOperands deconstructReadAssignOperand(
    Expression operand, Scope scope, MessageQueue mq, boolean checkImported) {
    if (operand instanceof Reference) {
      // TODO(erights): These rules should be independent of whether we're writing
      // new-caja or cajita.  The check for whether it's imported only applies in the
      // cajita case.
      if (checkImported && scope.isImported(((Reference) operand).getIdentifierName())) {
        mq.addMessage(
            RewriterMessageType.CANNOT_ASSIGN_TO_FREE_VARIABLE,
            operand.getFilePosition(), this, operand);
        return null;
      }
      return sideEffectlessReadAssignOperand(operand, scope, mq);
    } else if (operand instanceof Operation) {
      Operation op = (Operation) operand;
      switch (op.getOperator()) {
        case SQUARE_BRACKET:
          return sideEffectingReadAssignOperand(
              op.children().get(0), op.children().get(1), scope, mq);
        case MEMBER_ACCESS:
          return sideEffectingReadAssignOperand(
              op.children().get(0), toStringLiteral(op.children().get(1)),
              scope, mq);
      }
    }
    throw new IllegalArgumentException("Not an lvalue : " + operand);
  }

  /**
   * Given a LHS that has no side effect when evaluated as an LHS, produce
   * a ReadAssignOperands without using temporaries.
   */
  private ReadAssignOperands sideEffectlessReadAssignOperand(
      final Expression lhs, Scope scope, MessageQueue mq) {
    return new ReadAssignOperands(
        Collections.<Expression>emptyList(),
        lhs, (Expression) rewriter.expand(lhs, scope, mq));
  }

  private ReadAssignOperands sideEffectingReadAssignOperand(
      Expression uncajoledObject, Expression uncajoledKey, Scope scope,
      MessageQueue mq) {
    final Reference object;  // The object that contains the field to assign.
    final Expression key;  // Identifies the field to assign.
    List<Expression> temporaries = new ArrayList<Expression>();

    // Don't cajole the operands.  We return a simple assignment operator that
    // can then itself be cajoled, so that a rewriter can use context to treat
    // the LHS differently from the RHS.

    // a[b] += 2
    //   =>
    // var x___ = a;
    // var x0___ = b;

    // If the right is simple then we can assume it does not modify the
    // left, but otherwise the left has to be put into a temporary so that
    // it's evaluated before the right can muck with it.
    boolean isKeySimple = (uncajoledKey instanceof Literal
                           || isLocalReference(uncajoledKey, scope));

    // If the left is simple and the right does not need a temporary variable
    // then don't introduce one.
    if (isKeySimple && (isLocalReference(uncajoledObject, scope)
                        || isImportsReference(uncajoledObject))) {
      object = (Reference) uncajoledObject;
    } else {
      Identifier tmpVar = scope.declareStartOfScopeTempVariable();
      temporaries.add((Expression) QuasiBuilder.substV(
          "@tmpVar = @left;",
          "tmpVar", new Reference(tmpVar),
          "left", rewriter.expand(uncajoledObject, scope, mq)));
      object = new Reference(tmpVar);
    }

    // Don't bother to generate a temporary for a simple value like 'foo'
    if (isKeySimple) {
      key = uncajoledKey;
    } else {
      Identifier tmpVar = scope.declareStartOfScopeTempVariable();
      temporaries.add((Expression) QuasiBuilder.substV(
          "@tmpVar = @right;",
          "tmpVar", new Reference(tmpVar),
          "right", rewriter.expand(uncajoledKey, scope, mq)));
      key = new Reference(tmpVar);
    }

    Operation propertyAccess = Operation.create(
        Operator.SQUARE_BRACKET, object, key);
    return new ReadAssignOperands(
        temporaries, propertyAccess,
        (Expression) rewriter.expand(propertyAccess, scope, mq));
  }

  /**
   * True iff e is a reference to a local in scope.
   * We distinguish local references in many places because members of
   * {@code IMPORTS___} might be backed by getters/setters, and so
   * must be evaluated exactly once as an lvalue.
   */
  private static boolean isLocalReference(Expression e, Scope scope) {
    return e instanceof Reference
        && !scope.isImported(((Reference) e).getIdentifierName());
  }

  /** True iff e is a reference to the global object. */
  private static boolean isImportsReference(Expression e) {
    if (!(e instanceof Reference)) { return false; }
    return ReservedNames.IMPORTS.equals(((Reference) e).getIdentifierName());
  }

  /**
   * The operands in a read/assign operation.
   * <p>
   * When we need to express a single read/assign operation such as {@code *=}
   * or {@code ++} as an operation that separates out the getting from the
   * setting.
   * <p>
   * This encapsulates any temporary variables created to prevent multiple
   * execution, and the cajoled LHS and RHS.
   */
  protected static final class ReadAssignOperands {
    private final List<Expression> temporaries;
    private final Expression uncajoled, cajoled;

    ReadAssignOperands(
        List<Expression> temporaries, Expression lhs, Expression rhs) {
      assert lhs.isLeftHandSide();
      this.temporaries = temporaries;
      this.uncajoled = lhs;
      this.cajoled = rhs;
    }

    /**
     * The temporaries required by LHS and RHS in order of initialization.
     */
    public List<Expression> getTemporaries() { return temporaries; }
    public ParseTreeNodeContainer getTemporariesAsContainer() {
      return new ParseTreeNodeContainer(temporaries);
    }
    /** The uncajoled LHS. */
    public Expression getUncajoledLValue() { return uncajoled; }
    /** The cajoled left hand side expression usable as an rvalue. */
    public Expression getCajoledLValue() { return cajoled; }
    /**
     * Can the assignment be performed using the RHS as an LHS without
     * the need for temporaries?
     */
    public boolean isSimpleLValue() {
      return temporaries.isEmpty() && cajoled.isLeftHandSide();
    }

    public Operation makeAssignment(Expression rhs) {
      Operation e = Operation.create(Operator.ASSIGN, this.uncajoled, rhs);
      e.getAttributes().set(ParseTreeNode.TAINTED, true);
      return e;
    }
  }

  @Override
  public String toString() {
    return "<Rule " + getName() + ">";
  }
}
