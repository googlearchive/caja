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
import com.google.caja.lexer.TokenConsumer;
import com.google.caja.parser.AbstractParseTreeNode;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.ParseTreeNodes;
import com.google.caja.parser.js.BooleanLiteral;
import com.google.caja.parser.js.Declaration;
import com.google.caja.parser.js.Expression;
import com.google.caja.parser.js.ExpressionStmt;
import com.google.caja.parser.js.FormalParam;
import com.google.caja.parser.js.FunctionConstructor;
import com.google.caja.parser.js.Identifier;
import com.google.caja.parser.js.Literal;
import com.google.caja.parser.js.Operation;
import com.google.caja.parser.js.Operator;
import com.google.caja.parser.js.Reference;
import com.google.caja.parser.js.StringLiteral;
import com.google.caja.parser.js.UndefinedLiteral;
import com.google.caja.plugin.ReservedNames;
import com.google.caja.plugin.SyntheticNodes;
import static com.google.caja.plugin.SyntheticNodes.s;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.RenderContext;
import com.google.caja.util.Callback;
import com.google.caja.util.Pair;

import static com.google.caja.parser.quasiliteral.QuasiBuilder.match;
import static com.google.caja.parser.quasiliteral.QuasiBuilder.substV;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.LinkedHashMap;

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

  public Rule() {}

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
  public String getName() { return name; }

  /**
   * Set the name of this {@code Rule}.
   */
  public void setName(String name) {
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

  protected ParseTreeNode getFunctionHeadDeclarations(
      Rule rule,
      Scope scope,
      MessageQueue mq) {
    List<ParseTreeNode> stmts = new ArrayList<ParseTreeNode>();

    if (scope.hasFreeArguments()) {
      stmts.add(substV(
          "var @la = ___.args(@ga);",
          "la", s(new Identifier(ReservedNames.LOCAL_ARGUMENTS)),
          "ga", newReference(ReservedNames.ARGUMENTS)));
    }
    if (scope.hasFreeThis()) {
      stmts.add(substV(
          "var @lt = @gt;",
          "lt", s(new Identifier(ReservedNames.LOCAL_THIS)),
          "gt", newReference(ReservedNames.THIS)));
    }

    return new ParseTreeNodeContainer(stmts);
  }

  protected Reference newReference(String name) {
    return s(new Reference(s(new Identifier(name))));
  }

  protected Expression newCommaOperation(List<? extends ParseTreeNode> operands) {
    if (operands.size() == 0) return new UndefinedLiteral();
    Expression result = (Expression)operands.get(0);
    for (int i = 1; i < operands.size(); i++) {
      result = Operation.create(Operator.COMMA, result, (Expression)operands.get(i));
    }
    return result;
  }

  protected Pair<ParseTreeNode, ParseTreeNode> reuse(
      ParseTreeNode value,
      Rule rule,
      Scope scope,
      MessageQueue mq) {
    ParseTreeNode reference = s(new Reference(scope.declareStartOfScopeTempVariable()));
    ParseTreeNode variableDefinition = substV(
        "@ref = @rhs;",
        "ref", reference,
        "rhs", rewriter.expand(value, scope, mq));
    return new Pair<ParseTreeNode, ParseTreeNode>(
        reference,
        variableDefinition);
  }

  protected Pair<ParseTreeNode, ParseTreeNode> reuseAll(
      ParseTreeNode arguments,
      Rule rule,
      Scope scope,
      MessageQueue mq) {
    List<ParseTreeNode> refs = new ArrayList<ParseTreeNode>();
    List<ParseTreeNode> rhss = new ArrayList<ParseTreeNode>();

    for (int i = 0; i < arguments.children().size(); i++) {
      Pair<ParseTreeNode, ParseTreeNode> p = reuse(
          arguments.children().get(i),
          rule,
          scope,
          mq);
      refs.add(p.a);
      rhss.add(p.b);
    }

    return new Pair<ParseTreeNode, ParseTreeNode>(
        new ParseTreeNodeContainer(refs),
        new ParseTreeNodeContainer(rhss));
  }

  // TODO(ihab.awad): Refactor so the global case of this is not redundant with the
  // rewriting we do for assignment in the global scope. Part of the problem is that
  // the helper functions here "pretend" not to know about the rewriting rules, when
  // in fact they are pretty closely coupled with them.
  protected ParseTreeNode expandDef(
      ParseTreeNode symbol,
      ParseTreeNode value,
      Rule rule,
      Scope scope,
      MessageQueue mq) {
    if (!(symbol instanceof Reference)) {
      throw new RuntimeException("expandDef on non-Reference: " + symbol);
    }
    String sName = getReferenceName(symbol);
    if (scope.isGlobal(sName)) {
      return s(new ExpressionStmt((Expression)substV(
          "@temp = @value," +
          "___OUTERS___.@sCanSet ?" +
          "  (___OUTERS___.@s = @temp) :" +
          "  ___.setPub(___OUTERS___, @sName, @temp);",
          "s", symbol,
          "sCanSet", newReference(sName + "_canSet___"),
          "sName", toStringLiteral(symbol),
          "temp", s(new Reference(scope.declareStartOfScopeTempVariable())),
          "value", value)));
    } else {
      return substV(
          "var @s = @v",
          "s", symbol.children().get(0),
          "v", value);
    }
  }

  protected ParseTreeNode expandMember(
      ParseTreeNode member,
      Rule rule,
      Scope scope,
      MessageQueue mq) {
    Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();

    if (match("function(@ps*) { @bs*; }", member, bindings)) {
      Scope s2 = Scope.fromFunctionConstructor(scope, (FunctionConstructor)member);
      if (s2.hasFreeThis()) {
        checkFormals(bindings.get("ps"), mq);
        return substV(
            "___.method(function(@ps*) {" +
            "  @fh*;" +
            "  @stmts*;" +
            "  @bs*;" +
            "});",
            "ps",    bindings.get("ps"),
            // It's important to expand bs before computing fh and stmts.
            "bs",    rewriter.expand(bindings.get("bs"), s2, mq),
            "fh",    getFunctionHeadDeclarations(rule, s2, mq),
            "stmts", new ParseTreeNodeContainer(s2.getStartStatements()));
      }
    }

    return rewriter.expand(member, scope, mq);
  }

  protected ParseTreeNode expandAllMembers(
      ParseTreeNode members,
      Rule rule,
      Scope scope,
      MessageQueue mq) {
    List<ParseTreeNode> results = new ArrayList<ParseTreeNode>();
    for (ParseTreeNode member : members.children()) {
      results.add(expandMember(member, rule, scope, mq));
    }
    return new ParseTreeNodeContainer(results);
  }

  protected ParseTreeNode expandMemberMap(
      ParseTreeNode memberMap,
      Rule rule,
      Scope scope,
      MessageQueue mq) {
    Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();

    if (match("({@keys*: @vals*})", memberMap, bindings)) {
      if (literalsEndWith(bindings.get("keys"), "__")) {
        mq.addMessage(
            RewriterMessageType.MEMBER_KEY_MAY_NOT_END_IN_DOUBLE_UNDERSCORE,
            memberMap.getFilePosition(), rule, memberMap);
        return memberMap;
      }

      return substV(
          "({@keys*: @vals*})",
          "keys", bindings.get("keys"),
          "vals", expandAllMembers(bindings.get("vals"), rule, scope, mq));
    }

    mq.addMessage(RewriterMessageType.MAP_EXPRESSION_EXPECTED,
        memberMap.getFilePosition(), rule, memberMap);
    return memberMap;
  }

  // TODO(erights): Remove this when first class constructors are checked in.
  protected ParseTreeNode expandReferenceToOuters(
      ParseTreeNode ref,
      Scope scope,
      MessageQueue mq) {
    String xName = getReferenceName(ref);
    if (scope.isGlobal(xName)) {
      return substV(
          "___OUTERS___.@xCanRead ? ___OUTERS___.@x : ___.readPub(___OUTERS___, @xName, true);",
          "x", ref,
          "xCanRead", newReference(xName + "_canRead___"),
          "xName", new StringLiteral(StringLiteral.toQuotedValue(xName)));
    } else {
      return ref;
    }
  }

  protected boolean checkMapExpression(
      ParseTreeNode node,
      Rule rule,
      Scope scope,
      MessageQueue mq) {
    Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
    if (!match("({@keys*: @vals*})", node, bindings)) {
      mq.addMessage(
          RewriterMessageType.MAP_EXPRESSION_EXPECTED,
          node.getFilePosition(), rule, node);
      return false;
    } else if (literalsEndWith(bindings.get("keys"), "_")) {
      mq.addMessage(
          RewriterMessageType.KEY_MAY_NOT_END_IN_UNDERSCORE,
          node.getFilePosition(), rule, node);
      return false;
    }
    return true;
  }

  protected void checkFormals(ParseTreeNode formals, MessageQueue mq) {
    for (ParseTreeNode formal : formals.children()) {
      FormalParam f = (FormalParam) formal;
      if (!isSynthetic(f) && f.getIdentifierName().endsWith("__")) {
        mq.addMessage(
            RewriterMessageType.VARIABLES_CANNOT_END_IN_DOUBLE_UNDERSCORE,
            f.getFilePosition(), this, f);
      }
    }
  }

  protected static boolean isSynthetic(ParseTreeNode node) {
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
    if (node.getFilePosition() != null) {
      sl.setFilePosition(node.getFilePosition());
    }
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

  /**
   * Split the target of a read/set operation into an lvalue, an rvalue, and
   * an ordered list of temporary variables needed to ensure proper order
   * of execution.
   * @param operand uncajoled expression that can be used as both an lvalue and
   *    an rvalue.
   * @return null if operand is not a valid lvalue, or its subexpressions do
   *    not cajole.
   */
  ReadAssignOperands deconstructReadAssignOperand(
      Expression operand, Scope scope, MessageQueue mq) {
    if (isLocalReference(operand, scope)) {
      return sideEffectlessReadAssignOperand(
          (Expression) rewriter.expand(operand, scope, mq));
    } else if (operand instanceof Reference) {
      rewriter.expand(operand, scope, mq);
      Reference outers = s(new Reference(s(new Identifier("___OUTERS___"))));
      outers.setFilePosition(FilePosition.startOf(operand.getFilePosition()));
      return sideEffectingReadAssignOperand(
          outers, toStringLiteral(operand), scope, mq);
    } else if (operand instanceof Operation) {
      Operation op = (Operation) operand;
      switch (op.getOperator()) {
        case SQUARE_BRACKET:
          return sideEffectingReadAssignOperand(
              op.children().get(0), op.children().get(1),
              scope, mq);
        case MEMBER_ACCESS:
          return sideEffectingReadAssignOperand(
              op.children().get(0), toStringLiteral(op.children().get(1)),
              scope, mq);
      }
    }
    throw new IllegalArgumentException("Not an lvalue : " + operand);
  }

  /**
   * Given a lhs that has no side effect when evaluated as an lvalue, produce
   * a ReadAssignOperands without using temporaries.
   */
  private ReadAssignOperands sideEffectlessReadAssignOperand(
      final Expression lhs) {
    assert lhs.isLeftHandSide();
    return new ReadAssignOperands(Collections.<Expression>emptyList(), lhs) {
        @Override
        public Expression makeAssignment(Expression rvalue) {
          return Operation.create(Operator.ASSIGN, lhs, rvalue);
        }
      };
  }

  private ReadAssignOperands sideEffectingReadAssignOperand(
      Expression uncajoledObject, Expression uncajoledKey,
      Scope scope, MessageQueue mq) {
    final Reference object;  // The object that contains the field to assign.
    final Expression key;  // Identifies the field to assign.
    List<Expression> temporaries = new ArrayList<Expression>();

    // Cajole the operands
    Expression left = (Expression) rewriter.expand(uncajoledObject, scope, mq);
    Expression right = (Expression) rewriter.expand(uncajoledKey, scope, mq);

    // a[b] += 2
    //   =>
    // var x___ = a;
    // var x0___ = b;

    // If the right is simple then we can assume it does not modify the
    // left, but otherwise the left has to be put into a temporary so that
    // it's evaluated before the right can muck with it.
    boolean isKeySimple = (right instanceof Literal
                           || isLocalReference(right, scope));

    // If the left is simple and the right does not need a temporary variable
    // then don't introduce one.
    if (isKeySimple && (isLocalReference(left, scope)
                        || isOutersReference(left))) {
      object = (Reference) left;
    } else {
      Identifier tmpVar = scope.declareStartOfScopeTempVariable();
      temporaries.add((Expression)substV(
          "@tmpVar = @left;",
          "tmpVar", s(new Reference(tmpVar)),
          "left", left));
      object = s(new Reference(tmpVar));
    }

    // Don't bother to generate a temporary for a simple value like 'foo'
    if (isKeySimple) {
      key = right;
    } else {
      Identifier tmpVar = scope.declareStartOfScopeTempVariable();
      temporaries.add((Expression)substV(
          "@tmpVar = @right;",
          "tmpVar", s(new Reference(tmpVar)),
          "right", right));
      key = s(new Reference(tmpVar));
    }

    // Is a property (as opposed to a public) reference.
    final boolean isProp = uncajoledObject instanceof Reference
        && Keyword.THIS.toString().equals(getReferenceName(uncajoledObject));

    Expression rvalueCajoled = (Expression) substV(
        "___.@flavorOfRead(@object, @key, @isGlobal)",
        "flavorOfRead", newReference(isProp ? "readProp" : "readPub"),
        "object", object,
        "key", key,
        // Make sure exception thrown if global variable not defined.
        "isGlobal", new BooleanLiteral(isOutersReference(object)));

    return new ReadAssignOperands(temporaries, rvalueCajoled) {
        @Override
        public Expression makeAssignment(Expression rvalue) {
          return (Expression) substV(
              "___.@flavorOfSet(@object, @key, @rvalue)",
              "flavorOfSet", newReference(isProp ? "setProp" : "setPub"),
              "object", object,
              "key", key,
              "rvalue", rvalue);
        }
      };
  }

  /**
   * True iff e is a reference to a local in scope.
   * We distinguish local references in many places because members of
   * {@code ___OUTERS___} might be backed by getters/setters, and so
   * must be evaluated exactly once as an lvalue.
   */
  private static boolean isLocalReference(Expression e, Scope scope) {
    return e instanceof Reference
        && !scope.isGlobal(((Reference) e).getIdentifierName());
  }

  /** True iff e is a reference to the global object. */
  private static boolean isOutersReference(Expression e) {
    if (!(e instanceof Reference)) { return false; }
    // TODO(mikesamuel): move ReservedNames into this package and use it here.
    return "___OUTERS___".equals(((Reference) e).getIdentifierName());
  }

  /**
   * The operands in a read/assign operation.
   * <p>
   * When we need to express a single read/assign operation such as {@code *=}
   * or {@code ++} as an operation that separates out the getting from the
   * setting.
   * <p>
   * This encapsulates any temporary variables created to prevent multiple
   * execution, and the cajoled lvalue and rvalue.
   */
  protected static abstract class ReadAssignOperands {
    private final List<Expression> temporaries;
    private final Expression rvalue;

    ReadAssignOperands(
        List<Expression> temporaries, Expression rvalue) {
      this.temporaries = temporaries;
      this.rvalue = rvalue;
    }

    /**
     * The temporaries required by lvalue and rvalue in order of
     * initialization.
     */
    public List<Expression> getTemporaries() { return temporaries; }
    public ParseTreeNodeContainer getTemporariesAsContainer() {
      return new ParseTreeNodeContainer(temporaries);
    }
    /** Produce an assignment of the given rvalue to the cajoled lvalue. */
    public abstract Expression makeAssignment(Expression rvalue);
    /** The Cajoled RValue. */
    public Expression getRValue() { return rvalue; }
    /**
     * Can the assignment be performed using the rvalue as an lvalue without
     * the need for temporaries?
     */
    public boolean isSimpleLValue() {
      return temporaries.isEmpty() && rvalue.isLeftHandSide();
    }
  }
}
