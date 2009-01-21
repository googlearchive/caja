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

import com.google.caja.lexer.TokenConsumer;
import com.google.caja.parser.AbstractParseTreeNode;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.ParseTreeNodeContainer;
import com.google.caja.parser.ParseTreeNodes;
import com.google.caja.parser.ParserBase;
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
      new AbstractParseTreeNode() {
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

  /**
   * Given two expressions in comma normal form (defined below), this returns
   * an expression equivalent to <tt>left,right</tt> that is also in comma
   * normal form.
   * <p>
   * An expression is in <i>comma normal form</i> if<ul>
   * <li>It is not a comma expression or
   * <li>It is a comma expression, but its right operand is not, and<ul>
   *     <li>its left operand is in comma normal form, and
   *     <li>its left operand is not <tt>void 0</tt>, and
   *     <li>if its left operand is a comma expression, its left
   *         operand's right operand is not <tt>void 0</tt>.
   *     </ul>
   * </ul>
   */
  private Expression comma(Expression left, Expression right) {
    Map<String, ParseTreeNode> leftBindings = makeBindings();
    Map<String, ParseTreeNode> rightBindings = makeBindings();
    if (QuasiBuilder.match("void 0", left)) {
      return right;
    } else if (QuasiBuilder.match("@leftLeft, void 0", left, leftBindings)) {
      return comma((Expression) leftBindings.get("leftLeft"), right);
    } else if (QuasiBuilder.match("@rightLeft, @rightRight", right, rightBindings)) {
      return comma(comma(left, (Expression) rightBindings.get("rightLeft")),
                   (Expression) rightBindings.get("rightRight"));
    } else {
      return Operation.create(Operator.COMMA, left, right);
    }
  }

  protected Expression commas(Expression... operands) {
    Expression result = Operation.undefined();
    for (int i = 0; i < operands.length; i++) {
      result = comma(result, operands[i]);
    }
    return result;
  }

  static private final Expression[] NO_EXPRS = {};

  protected Expression newCommaOperation(List<? extends ParseTreeNode> operands) {
    return commas(operands.toArray(NO_EXPRS));
  }

  /**
   * Returns a pair of a reusable expression and an initializing expression that
   * together represent the reusable expansion of the <tt>value</tt> expression.
   * <p>
   * In the expansion context, the initializing expression must be executed
   * exactly once and prior to evaluating the reusable expression.
   */
  protected Pair<Expression, Expression> reuse(
      ParseTreeNode value,
      Scope scope,
      MessageQueue mq) {
    Expression rhs = (Expression) rewriter.expand(value, scope, mq);
    if (rhs instanceof Reference || rhs instanceof Literal) {
      return new Pair<Expression, Expression>(rhs, Operation.undefined());
    }
    Expression tempRef = new Reference(
        scope.declareStartOfScopeTempVariable());
    Expression tempInit = (Expression) QuasiBuilder.substV(
        "@ref = @rhs;",
        "ref", tempRef,
        "rhs", rhs);
    return new Pair<Expression, Expression>(tempRef, tempInit);
  }

  /**
   * Returns a pair of a reusable expression list and an initializing expression that
   * together represent the reusable expansion of the <tt>arguments</tt> expression list.
   * <p>
   * In the expansion context, the initializing expression must be executed
   * exactly once and prior to evaluating the reusable expression.
   */
  protected Pair<ParseTreeNodeContainer, Expression> reuseAll(
      ParseTreeNode arguments,
      Scope scope,
      MessageQueue mq) {
    List<ParseTreeNode> refs = new ArrayList<ParseTreeNode>();
    Expression[] inits = new Expression[arguments.children().size()];

    for (int i = 0; i < arguments.children().size(); i++) {
      Pair<Expression, Expression> p = reuse(
          arguments.children().get(i), scope, mq);
      refs.add(p.a);
      inits[i] = p.b;
    }

    return new Pair<ParseTreeNodeContainer, Expression>(
        new ParseTreeNodeContainer(refs),
        commas(inits));
  }

  /**
   * Return a name suitable for naming a function derived from <tt>node</tt>, where
   * the name is derived from baseName and optionally ext, is distinct from baseName,
   * and is probably not used within <tt>node</tt>.
   * <p>
   * We operate under the (currently unchecked) assumption
   * that node contains no variables whose names contain a "$_".
   */
  protected String nym(ParseTreeNode node, String baseName, String ext) {
    String result;
    if (node != null && baseName.indexOf("$_") != -1) {
      result = baseName + "$";
    } else {
      result = baseName + "$_" + ext;
    }
    if (!ParserBase.isJavascriptIdentifier(result)) {
      result = "badName$_" + ext;
    }
    // TODO: If we ever have a cheap way to test whether result is used freely
    // in node <i>including any nested functions</i>, then we should modify result
    // so that it does not collide.
    return result;
  }

  /**
   * Returns a ParseTreeNode with the same meaning as node, but potentially better
   * debugging info.
   * <p>
   * If node defines an anonymous function expression, then return a new named
   * function expression, where the name is derived from baseName.
   * For all other nodes, currently returns the node itself.
   */
  protected ParseTreeNode nymize(ParseTreeNode node, String baseName, String ext) {
    Map<String, ParseTreeNode> bindings = makeBindings();
    if (QuasiBuilder.match("function (@ps*) {@bs*;}", node, bindings)) {
      return QuasiBuilder.substV(
          "function @fname(@ps*) {@bs*;}",
          "fname", new Identifier(nym(node, baseName, ext)),
          "ps", bindings.get("ps"),
          "bs", bindings.get("bs"));
    }
    return node;
  }

  /**
   * <tt>substSingleMap(k, v)</tt> should be equivalent to<pre>
   * QuasiBuilder.substV(
   *     "({&#64;key: @val})",
   *     "key", k,
   *     "val", v)</pre>
   * but currently isn't.
   * <p>
   * TODO(erights): figure out why not, and fix if appropriate.
   */
  protected ParseTreeNode substSingleMap(ParseTreeNode key, ParseTreeNode val) {
    List<ParseTreeNode> keys = new ArrayList<ParseTreeNode>();
    List<ParseTreeNode> vals = new ArrayList<ParseTreeNode>();
    keys.add(key);
    vals.add(val);
    return QuasiBuilder.substV(
        "({@keys*: @vals*})",
        "keys", new ParseTreeNodeContainer(keys),
        "vals", new ParseTreeNodeContainer(vals));
  }

  /**
   * <tt>matchSingleMap(node) != null</tt> should be equivalent to<pre>
   * QuasiBuilder.match("({&#64;key: @val})", node)</pre>
   * but currently isn't.
   * <p>
   * TODO(erights): figure out why not, and fix if appropriate.
   */
  protected Map<String, ParseTreeNode> matchSingleMap(ParseTreeNode node) {
    Map<String, ParseTreeNode> badBindings = makeBindings();
    if (QuasiBuilder.match("({@keys*: @vals*})", node, badBindings)) {
      ParseTreeNodeContainer keys = (ParseTreeNodeContainer) badBindings.get("keys");
      if (keys.children().size() == 1) {
        ParseTreeNodeContainer vals = (ParseTreeNodeContainer) badBindings.get("vals");
        Map<String, ParseTreeNode> fixedBindings = makeBindings();
        fixedBindings.put("key", keys.children().get(0));
        fixedBindings.put("val", vals.children().get(0));
        return fixedBindings;
      }
    }
    return null;
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
   * Substitutes bindings into the Quasi-pattern from
   * {@link RuleDescription#substitutes}.
   * @param args quasi hole names and ParseTreeNodes per QuasiBuilder.substV.
   */
  protected ParseTreeNode substV(Object... args) {
    return QuasiBuilder.substV(getRuleDescription().substitutes(), args);
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
        default: break;
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
