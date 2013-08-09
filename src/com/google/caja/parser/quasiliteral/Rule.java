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

import static com.google.caja.parser.js.SyntheticNodes.s;

import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.Keyword;
import com.google.caja.lexer.TokenConsumer;
import com.google.caja.parser.AbstractParseTreeNode;
import com.google.caja.parser.MutableParseTreeNode;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.ParseTreeNodeContainer;
import com.google.caja.parser.ParseTreeNodes;
import com.google.caja.parser.ParserBase;
import com.google.caja.parser.js.Declaration;
import com.google.caja.parser.js.Expression;
import com.google.caja.parser.js.ExpressionStmt;
import com.google.caja.parser.js.FormalParam;
import com.google.caja.parser.js.FunctionConstructor;
import com.google.caja.parser.js.Identifier;
import com.google.caja.parser.js.Literal;
import com.google.caja.parser.js.Noop;
import com.google.caja.parser.js.ObjectConstructor;
import com.google.caja.parser.js.Operation;
import com.google.caja.parser.js.Operator;
import com.google.caja.parser.js.Reference;
import com.google.caja.parser.js.StringLiteral;
import com.google.caja.parser.js.SyntheticNodes;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.RenderContext;
import com.google.caja.util.Callback;
import com.google.caja.util.Lists;
import com.google.caja.util.Maps;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
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
      new AbstractParseTreeNode(FilePosition.UNKNOWN) {
        private static final long serialVersionUID = -2661372462823134153L;
        @Override public Object getValue() { return null; }
        public void render(RenderContext r) {
          throw new UnsupportedOperationException();
        }
        public TokenConsumer makeRenderer(
            Appendable out, Callback<IOException> exHandler) {
          throw new UnsupportedOperationException();
        }
      };

  private final String name;
  private Rewriter rewriter;
  private RuleDescription description;

  /**
   * Creates a new Rule, inferring name and other state from the {@link #fire}
   * method's {@link RuleDescription}.
   */
  public Rule() {
    this.name = getRuleDescription().name();
  }

  /**
   * Create a new {@code Rule}.
   *
   * @param name the unique name of this rule.
   */
  public Rule(String name, Rewriter rewriter) {
    assert name != null;
    this.name = name;
    this.rewriter = rewriter;
  }

  /**
   * @return the name of this {@code Rule}.
   */
  public String getName() {
    return name;
  }

  /**
   * @return the rewriter this {@code Rule} uses.
   */
  public Rewriter getRewriter() { return rewriter; }

  /**
   * Set the rewriter this {@code Rule} uses.
   */
  public void setRewriter(Rewriter rewriter) {
    assert this.rewriter == null || this.rewriter == rewriter;
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
              ParseTreeNode.class, Scope.class
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

  public boolean canMatch(Class<? extends ParseTreeNode> nodeType) {
    RuleDescription desc = getRuleDescription();
    Class<? extends ParseTreeNode> bound = desc.matchNode();
    if (bound != ParseTreeNode.class) {
      // If the rule has an explicit matchNode=, use it.
      bound = QuasiBuilder.fuzzType(bound);

    } else {
      // Otherwise, try parsing the matches= pattern
      String pattern = desc.matches();
      bound = quasiLowerBound(QuasiCache.parse(pattern));
    }
    return bound.isAssignableFrom(nodeType);
  }

  private static Class<? extends ParseTreeNode> quasiLowerBound(QuasiNode p) {
    if (p == null) {
      return ParseTreeNode.class;
    } else  if (p instanceof SimpleQuasiNode) {
      return QuasiBuilder.fuzzType(((SimpleQuasiNode) p).getMatchedClass());
    } else if (p instanceof ObjectCtorQuasiNode) {
      return ObjectConstructor.class;
    } else if (p instanceof StringLiteralQuasiNode) {
      return StringLiteral.class;
    } else {
      return ParseTreeNode.class;
    }
  }

  /**
   * Process the given input, returning a rewritten node.
   *
   * @param node an input node.
   * @param scope the current scope.
   * @return the rewritten node, or {@link #NONE} to indicate
   * that this rule does not apply to the given input.
   */
  public abstract ParseTreeNode fire(ParseTreeNode node, Scope scope);

  /**
   * @see MessagePart#format(MessageContext,Appendable)
   */
  public void format(MessageContext mc, Appendable out) throws IOException {
    out.append("Rule \"" + name + "\"");
  }

  protected final ParseTreeNode expandAll(ParseTreeNode node, Scope scope) {
    return expandAllTo(node, node.getClass(), scope);
  }

  protected final ParseTreeNode expandAllTo(
      ParseTreeNode node,
      Class<? extends ParseTreeNode> parentNodeClass,
      Scope scope) {
    boolean allChildrenSame = true;
    List<ParseTreeNode> rewrittenChildren = Lists.newArrayList();
    for (ParseTreeNode child : node.children()) {
      ParseTreeNode expanded = rewriter.expand(child, scope);
      allChildrenSame = allChildrenSame && (child == expanded);
      rewrittenChildren.add(expanded);
    }

    if (allChildrenSame) {
      rewriter.clearTaint(node);
      return node;
    }

    ParseTreeNode result = ParseTreeNodes.newNodeInstance(
        parentNodeClass,
        node.getFilePosition(),
        node.getValue(),
        rewrittenChildren);
    result.getAttributes().putAll(node.getAttributes());
    if (SyntheticNodes.is(node)) {
      SyntheticNodes.s(result);
    }

    result.makeImmutable();
    return result;
  }

  static final ParseTreeNode withoutNoops(ParseTreeNode n) {
    if (n instanceof ParseTreeNodeContainer) {
      MutableParseTreeNode.Mutation mut = ((ParseTreeNodeContainer) n)
          .createMutation();
      for (ParseTreeNode child : n.children()) {
        if (child instanceof Noop) { mut.removeChild(child); }
      }
      mut.execute();
    }
    return n;
  }

  public static Reference newReference(FilePosition pos, String name) {
    return new Reference(s(new Identifier(pos, name)));
  }

  protected static ExpressionStmt newExprStmt(Expression e) {
    return new ExpressionStmt(e.getFilePosition(), e);
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
      return Operation.createInfix(Operator.COMMA, left, right);
    }
  }

  protected Expression commas(Expression... operands) {
    if (operands.length == 0) {
      return Operation.undefined(FilePosition.UNKNOWN);
    }
    Expression result = operands[0];
    for (int i = 1; i < operands.length; i++) {
      result = comma(result, operands[i]);
    }
    return result;
  }

  static private final Expression[] NO_EXPRS = {};

  protected Expression newCommaOperation(List<? extends ParseTreeNode> operands) {
    return commas(operands.toArray(NO_EXPRS));
  }

  /**
   * Return a name suitable for naming a function derived from <tt>node</tt>, where
   * the name is derived from baseName and optionally ext, is distinct from baseName,
   * and is probably not used within <tt>node</tt>.
   * <p>
   * We operate under the (currently unchecked) assumption
   * that node contains no variables whose names contain a "$_".
   */
  protected static String nym(ParseTreeNode node, String baseName, String ext) {
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
  protected static ParseTreeNode nymize(
      ParseTreeNode node, String baseName, String ext) {
    Map<String, ParseTreeNode> bindings = makeBindings();
    if (QuasiBuilder.match("function (@ps*) {@bs*;}", node, bindings)) {
      return QuasiBuilder.substV(
          "function @fname(@ps*) {@bs*;}",
          "fname", new Identifier(
              FilePosition.startOf(node.getFilePosition()),
              nym(node, baseName, ext)),
          "ps", bindings.get("ps"),
          "bs", bindings.get("bs"));
    }
    return node;
  }

  protected void checkFormals(ParseTreeNode formals) {
    for (ParseTreeNode formal : formals.children()) {
      FormalParam f = (FormalParam) formal;
      if (!isSynthetic(f.getIdentifier())
          && f.getIdentifierName().endsWith("__")) {
        rewriter.mq.addMessage(
            RewriterMessageType.VARIABLES_CANNOT_END_IN_DOUBLE_UNDERSCORE,
            f.getFilePosition(), this, f);
      }
    }
  }

  protected static boolean isSynthetic(Identifier node) {
    return node.isSynthetic();
  }

  protected static boolean isSynthetic(Reference node) {
    return isSynthetic(node.getIdentifier());
  }

  protected static boolean isSynthetic(FunctionConstructor node) {
    return node.isSynthetic();
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
    return new StringLiteral(
        ident.getFilePosition(), StringLiteral.toQuotedValue(ident.getName()));
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
    return Maps.newLinkedHashMap();
  }

  /**
   * For when you just want to match(), expand() all bindings, and subst() using
   * the rule's matches and substitutes annotations.
   */
  protected ParseTreeNode transform(ParseTreeNode node, Scope scope) {
    Map<String, ParseTreeNode> bindings = match(node);
    if (bindings != null) {
      Map<String, ParseTreeNode> newBindings = makeBindings();
      for (Map.Entry<String, ParseTreeNode> entry : bindings.entrySet()) {
        entry.getValue().makeImmutable();
        newBindings.put(entry.getKey(),
            rewriter.expand(entry.getValue(), scope));
      }
      ParseTreeNode result =
          QuasiBuilder.subst(getRuleDescription().substitutes(), newBindings);
      result.makeImmutable();
      return result;
    }
    return NONE;
  }

  /**
   * Substitutes bindings into the Quasi-pattern from
   * {@link RuleDescription#substitutes}.
   * @param args quasi hole names and ParseTreeNodes per QuasiBuilder.substV.
   */
  protected ParseTreeNode substV(Object... args) {
    for (int i = 1; i < args.length; i += 2) {
      if (args[i] != null) {
        ((ParseTreeNode) args[i]).makeImmutable();
      }
    }
    ParseTreeNode result =
        QuasiBuilder.substV(getRuleDescription().substitutes(), args);
    result.makeImmutable();
    return result;
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
      Expression operand, Scope scope) {
    return deconstructReadAssignOperand(operand, scope, true);
  }

  ReadAssignOperands deconstructReadAssignOperand(
    Expression operand, Scope scope, boolean checkImported) {
    if (operand instanceof Reference) {
      // TODO(erights): These rules should be independent of whether we're writing
      // new-caja or cajita.  The check for whether it's imported only applies in the
      // cajita case.
      if (checkImported && scope.isImported(((Reference) operand).getIdentifierName())) {
        rewriter.mq.addMessage(
            RewriterMessageType.CANNOT_ASSIGN_TO_FREE_VARIABLE,
            operand.getFilePosition(), this, operand);
      }
      return sideEffectlessReadAssignOperand(operand, scope);
    } else if (operand instanceof Operation) {
      Operation op = (Operation) operand;
      switch (op.getOperator()) {
        case SQUARE_BRACKET:
          return sideEffectingReadAssignOperand(
              op.children().get(0), op.children().get(1), scope);
        case MEMBER_ACCESS:
          return sideEffectingReadAssignOperand(
              op.children().get(0), toStringLiteral(op.children().get(1)),
              scope);
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
      Expression lhs, Scope scope) {
    return new ReadAssignOperands(
        Collections.<Expression>emptyList(),
        lhs, (Expression) rewriter.expand(lhs, scope));
  }

  private ReadAssignOperands sideEffectingReadAssignOperand(
      Expression uncajoledObject, Expression uncajoledKey, Scope scope) {
    Reference object;  // The object that contains the field to assign.
    Expression key;  // Identifies the field to assign.
    List<Expression> temporaries = Lists.newArrayList();

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
      Reference tmpVar = scope.declareStartOfScopeTemp();
      temporaries.add((Expression) QuasiBuilder.substV(
          "@tmpVar = @left;",
          "tmpVar", tmpVar,
          "left", rewriter.expand(uncajoledObject, scope)));
      object = tmpVar;
    }

    // Don't bother to generate a temporary for a simple value like 'foo'
    if (isKeySimple) {
      key = uncajoledKey;
    } else {
      ParseTreeNode rightExpanded = rewriter.expand(uncajoledKey, scope);
      Reference tmpVar = scope.declareStartOfScopeTemp();
      key = tmpVar;
      if (QuasiBuilder.match("@s&(-1>>>1)", rightExpanded)) {
        // TODO(metaweta): Figure out a way to leave key alone and
        // protect propertyAccess from rewriting instead.
        key = (Expression) QuasiBuilder.substV("@key&(-1>>>1)", "key", key);
      }
      temporaries.add((Expression) QuasiBuilder.substV(
          "@tmpVar = @right;",
          "tmpVar", tmpVar,
          "right", rightExpanded));
    }

    Operation propertyAccess = null;
    if (key instanceof StringLiteral) {
      // Make sure that cases like
      //   arr.length -= 1
      // optimize arr.length in the right-hand-side usage.
      // See the array length case in testSetReadModifyWriteLocalVar.
      String keyText = ((StringLiteral) key).getUnquotedValue();
      if (ParserBase.isJavascriptIdentifier(keyText)
          && Keyword.fromString(keyText) == null) {
        Reference ident = new Reference(
            new Identifier(key.getFilePosition(), keyText));
        propertyAccess = Operation.create(
            FilePosition.span(object.getFilePosition(), key.getFilePosition()),
            Operator.MEMBER_ACCESS, object, ident);
      }
    }
    if (propertyAccess == null) {
      propertyAccess = Operation.create(
          FilePosition.span(object.getFilePosition(), key.getFilePosition()),
          Operator.SQUARE_BRACKET, object, key);
    }
    return new ReadAssignOperands(
        temporaries, propertyAccess,
        (Expression) rewriter.expand(propertyAccess, scope));
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
  protected final class ReadAssignOperands {
    private final List<Expression> temporaries;
    private final Expression uncajoled, cajoled;

    private ReadAssignOperands(
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
      return temporaries.isEmpty() && cajoled.isLeftHandSide()
          && cajoled instanceof Reference;
    }

    public Operation makeAssignment(Expression rhs) {
      Operation e = Operation.createInfix(Operator.ASSIGN, this.uncajoled, rhs);
      rewriter.setTaint(e);
      return e;
    }
  }

  /**
   * Sometimes a rewrite rule needs to emit an expression twice,
   * which might do the wrong thing if the expression has side effects.
   * So we generate temp variables to hold the value of the expressions,
   * and repeat the temp variables instead.
   * <p>
   * If all the expressions are idempotent and efficient (eg, literals),
   * then we don't generate temps, and we use the expressions as-is.
   */
  protected final class Reusable {
    private final Scope scope;
    private ParseTreeNode[] expressions;
    private Expression[] refs;
    private Expression[] inits;

    public Reusable(Scope scope, ParseTreeNode... expressions) {
      this.scope = scope;
      this.expressions = expressions;
    }

    public void addChildren(ParseTreeNode list) {
      int n = list.children().size();
      int oldSize = expressions.length;
      expressions = Arrays.copyOf(expressions, oldSize + n);
      for (int i = 0; i < n; i++) {
        expressions[oldSize + i] = list.children().get(i);
      }
    }

    // Generate the reusable value references, returning this.
    public Reusable generate() {
      refs = new Expression[expressions.length];
      inits = new Expression[expressions.length];
      boolean needTemps = false;
      for (int i = 0; i < expressions.length; i++) {
        Expression value = (Expression) rewriter.expand(expressions[i], scope);
        if (canWeaklyReuse(value)) {
          refs[i] = value;
          inits[i] = null;
        } else {
          needTemps = true;
          makeTemp(i, value);
        }
      }
      for (int i = 0; i < expressions.length; i++) {
        if (inits[i] == null) {
          if (needTemps && !canAlwaysReuse(refs[i])) {
            makeTemp(i, refs[i]);
          } else {
            inits[i] = Operation.undefined(FilePosition.UNKNOWN);
          }
        }
      }
      return this;
    }

    // Returns an expression that initializes all generated temp vars.
    public Expression init() {
      return commas(inits);
    }

    // Returns a value reference for the i'th value.
    public Expression ref(int i) {
      return refs[i];
    }

    // Returns a list of value references starting at the i'th value.
    public ParseTreeNodeContainer refListFrom(int i) {
      return new ParseTreeNodeContainer(
          Arrays.asList(Arrays.copyOfRange(refs, i, refs.length)));
    }

    private void makeTemp(int i, Expression value) {
      Reference temp = scope.declareStartOfScopeTemp();
      refs[i] = temp;
      inits[i] = (Expression) QuasiBuilder.substV(
          "@temp = @value",
          "temp", temp,
          "value", value);
    }

    // true when e can be repeated without a temp var, as long as no
    // side-effecting expressions get moved into an init clause.
    private boolean canWeaklyReuse(Expression e) {
      return e instanceof Literal || e instanceof Reference;
    }

    // true when e can always be repeated without a temp var.
    private boolean canAlwaysReuse(Expression e) {
      return e instanceof Literal;
    }
  }

  @Override
  public String toString() {
    return "<Rule " + getName() + ">";
  }
}
