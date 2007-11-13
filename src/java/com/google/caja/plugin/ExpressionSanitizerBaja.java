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

package com.google.caja.plugin;

import com.google.caja.parser.MutableParseTreeNode;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.Visitor;
import com.google.caja.parser.js.ArrayConstructor;
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.Declaration;
import com.google.caja.parser.js.Expression;
import com.google.caja.parser.js.ExpressionStmt;
import com.google.caja.parser.js.FormalParam;
import com.google.caja.parser.js.FunctionConstructor;
import com.google.caja.parser.js.FunctionDeclaration;
import com.google.caja.parser.js.Operation;
import com.google.caja.parser.js.Operator;
import com.google.caja.parser.js.Reference;
import com.google.caja.parser.js.ReturnStmt;
import com.google.caja.parser.js.Statement;
import com.google.caja.parser.js.StringLiteral;
import com.google.caja.reporting.MessageQueue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author benl@google.com (Ben Laurie)
 */
public class ExpressionSanitizerBaja {
  MessageQueue mq;

  public ExpressionSanitizerBaja(MessageQueue mq) {
    this.mq = mq;
  }

  boolean sanitize(ParseTreeNode node) {
    rewriteFieldAccess(node);
    return true;
  }

  // $1.$2 -> $1.$2_canRead___ ? $1.$2 : __.readPub($1,"$2")
  private final class FieldAccessRewriter implements Visitor {
    // TODO(benl): make this work over nested functions. Also needs to deal with closures for ES4.
    private boolean thisUsed = false;  // has "this" been used in the current function?
    private boolean superUsed = false;  // has "super" been used in the current function?

    // var TEMP;
    private Declaration declareTmp(Expression initializer) {
      return new Declaration(ReservedNames.TEMP, initializer);
    }
    // instance.member
    private Operation anyMember(String instance, String member) {
      return new Operation(Operator.MEMBER_ACCESS, new Reference(instance),
                           new Reference(member));
    }
    // ___.what(args...)
    private Operation call___(String what, Expression... args) {
      Expression[] callChildren = new Expression[args.length + 1];
      callChildren[0] = new Operation(Operator.MEMBER_ACCESS,
          new Reference("___"), new Reference(what));
      System.arraycopy(args, 0, callChildren, 1, args.length);
      return new Operation(Operator.FUNCTION_CALL, callChildren);
    }
    // Replace toReplace with ___.what(args...)
    private void replaceCall___(ParseTreeNode toReplace, String what,
        Expression... args) {
      final Operation call = call___(what, args);
      ((MutableParseTreeNode) toReplace.getParent()).replaceChild(call, toReplace);
    }
    // ___.whatPub(foo,...) or ____.whatProp(this,....)
    private Operation call___PubOrProp(String what, Expression... args) {
      String whatType = "Pub";
      if (args[0] instanceof Reference && ((Reference) args[0]).isThis())
        whatType = "Prop";
      return call___(what + whatType, args);
    }
    // replace toReplace with ___.whatPub(foo,...) or ____.whatProp(this,....)
    private void replaceCall___PubOrProp(ParseTreeNode toReplace, String what,
        Expression... args) {
      final Operation call = call___PubOrProp(what, args);
      ((MutableParseTreeNode) toReplace.getParent()).replaceChild(call,
          toReplace);
    }
    private StringLiteral string(String str) {
      return new StringLiteral(StringLiteral.toQuotedValue(str));
    }
    // (function() { body })()
    private Operation privateScope(Block body) {
      final FunctionConstructor tempFn = new FunctionConstructor(null,
          Collections.<FormalParam>emptyList(), body);
      return new Operation(Operator.FUNCTION_CALL, tempFn);
    }
    // test ? thenValue : elseValue
    private Operation ternary(Expression test, Expression thenValue,
                              Expression elseValue) {
      return new Operation(Operator.TERNARY, test, thenValue, elseValue);
    }
    // subject.rhsName_can_canWhat___ ? subject.rhsName
    //   : ___.canWhat[Pub|Prop](subject, "rhsName", args)
    // or subject.rhsName_can_canWhat___ ? subject.rhsName op args
    //      : ___.canWhat[Pub|Prop](subject, "rhsName", args)
    private Operation simpleCanTernary(boolean isThis, String subject,
                                       String canWhat, String rhsName,
                                       Operator op, Expression... args) {
      String canType;
      if (isThis) {
        canType = "Prop";
      } else {
        canType = "Pub";
      }

      Operation test = anyMember(subject, rhsName + "_can" + canWhat + "___");

      Operation ifOp = anyMember(subject, rhsName);
      if (op != null) {
        Expression opArgs[] = new Expression[args.length + 1];
        opArgs[0] = ifOp;
        System.arraycopy(args, 0, opArgs, 1, args.length);
        ifOp = new Operation(op, opArgs);
      }

      // ___.<what>(Prop|Pub)((this|tmp), "rhsName", ...)
      Expression[] callChildren = new Expression[args.length + 3];
      callChildren[0] = new Operation(Operator.MEMBER_ACCESS,
          new Reference("___"), new Reference(canWhat.toLowerCase() + canType));
      callChildren[1] = new Reference(subject);
      callChildren[2] = string(rhsName);
      System.arraycopy(args, 0, callChildren, 3, args.length);
      Operation elseOp = new Operation(Operator.FUNCTION_CALL, callChildren);

      return ternary(test, ifOp, elseOp);
    }
    // simpleCanTernary, but wrapped like (function() { var tmp=lhs;
    //   simpleCanTernary(lhs,...) })()
    private Operation canTernary(Expression lhs, String canWhat, String rhsName,
        Operator op, Expression... args) {
      // TODO(benl): why does Reference.IsLeftHandSide() always return true?
      final boolean isRef = lhs instanceof Reference;
      final boolean isThis = isRef && ((Reference) lhs).isThis();
      final Operation ternary = simpleCanTernary(isThis, isRef
          ? (String)lhs.getValue() : ReservedNames.TEMP, canWhat, rhsName, op,
          args);
      if (isRef) {
        return ternary;
      }
      final Declaration varTmp = declareTmp(lhs);
      final Block body = new Block(Arrays.<Statement>asList(varTmp,
          new ReturnStmt(ternary)));
      return privateScope(body);
    }
    private void replaceCanTernary(ParseTreeNode toReplace,Expression lhs,
        String canWhat, String rhsName, Operator op, Expression... args) {
      Operation ternary = canTernary(lhs, canWhat, rhsName, op, args);
      ((MutableParseTreeNode) toReplace.getParent()).replaceChild(ternary,
          toReplace);
    }
    // turn func(...) { ... } to var func = ___.ctor(function (...) {
    //    ___.enter[Base|Derived](func, this);  ... }
    private void makeConstructor(final ParseTreeNode node) {
      final FunctionConstructor func
        = (FunctionConstructor) node.children().get(0);
      if (thisUsed) {
        final Statement localThis
          = new Declaration(ReservedNames.LOCAL_THIS, new Reference("this"));
        func.getBody().prepend(localThis);
      }
      final ExpressionStmt enterDerived = new ExpressionStmt(
          call___(superUsed ? "enterDerived" : "enterBase",
          new Reference((String)node.children().get(0).getValue()),
          new Reference("this")));
      func.getBody().prepend(enterDerived);
      final Operation call = call___("ctor", func);
      final Declaration decl = new Declaration((String)node.children().get(0).getValue(), call);
      func.clearName();
      ((MutableParseTreeNode) node.getParent()).replaceChild(decl, node);
    }

    public boolean visit(final ParseTreeNode node) {
      if (node.getAttributes().is(ExpressionSanitizer.SYNTHETIC)) { return true; }
      if (node instanceof FunctionConstructor) {
      } else if (node instanceof FunctionDeclaration) {
        if (superUsed) {
          // TODO(benl): the spec says the function should also include "this" - true? Or not?
          makeConstructor(node);
          superUsed = false;
        } else if (thisUsed) {
          makeConstructor(node);
        }
        thisUsed = false;
      } else if (node instanceof Reference) {
        final Reference ref = (Reference) node;
        if (ref.isThis()) {
          ref.setIdentifier(ReservedNames.LOCAL_THIS);
          thisUsed = true;
        } else if(ref.isSuper()) {
          superUsed = true;
        }
      } else if (node instanceof Operation) {
        final Operation op = (Operation) node;
        final Operator operator = op.getOperator();
        final Expression lhs = op.children().get(0);
        Expression rhs = null;
        if (op.children().size() > 1)
          rhs = op.children().get(1);
        final ParseTreeNode parent = op.getParent();
        Operator parentOp = null;
        if (parent instanceof Operation)
            parentOp = ((Operation) parent).getOperator();
        final boolean isLHS = node == parent.children().get(0);
        if (operator == Operator.MEMBER_ACCESS) {
          final String rhsName = (String)rhs.getValue();
          // this.foo_ is left untouched. Anything with __ at the end will already have been rejected.
          if (lhs instanceof Reference && ((Reference)lhs).isThis() && rhsName.endsWith("_")) {
            return true;
          } else if (isLHS && parentOp == Operator.FUNCTION_CALL) {
            int numChildren = parent.children().size();
            Expression[] args =
                parent.children().subList(1, numChildren).toArray(new Expression[numChildren - 1]);
            replaceCanTernary(parent, lhs, "Call", rhsName,
                Operator.FUNCTION_CALL, args);
          } else if (isLHS && parentOp == Operator.ASSIGN) {
            replaceCanTernary(parent, lhs, "Set", rhsName,
                Operator.ASSIGN, (Expression) parent.children().get(1));
          } else if (isLHS && parentOp == Operator.DELETE) {
            replaceCall___PubOrProp(parent, "delete", lhs, string(rhsName));
          } else {
            replaceCanTernary(op, lhs, "Read", rhsName, null);
          }
        } else if (operator == Operator.SQUARE_BRACKET) {
          if (isLHS && parentOp == Operator.ASSIGN) {
            replaceCall___PubOrProp(parent, "set", lhs, rhs, (Expression) parent.children().get(1));
          } if (isLHS && parentOp == Operator.DELETE) {
            replaceCall___PubOrProp(parent, "delete", lhs, rhs);
          } else {
            replaceCall___PubOrProp(op, "read", lhs, rhs);
          }
        } else if (operator == Operator.IN) {
          final Operation call = call___PubOrProp("canRead", rhs, lhs);
          final Operation andAnd = new Operation(Operator.LOGICAL_AND, op, call);
          ((MutableParseTreeNode)parent).replaceChild(andAnd, op);
        } else if (operator == Operator.CONSTRUCTOR) {
          assert parentOp == Operator.FUNCTION_CALL;
          assert rhs == null;
          // XXX(benl): Why does the class appear as a child of both CONSTRUCTOR and FUNCTION_CALL?
          final ArrayConstructor args
            = new ArrayConstructor((List<Expression>)parent.children().subList(1, parent.children().size()));
          replaceCall___(parent, "callNew", lhs, args);
        }
      }
      return true;
    }
  }

  private void rewriteFieldAccess(final ParseTreeNode node) {
    final FieldAccessRewriter visitor = new FieldAccessRewriter();
    // We use PostOrder because it visits all children, even those of changed
    // nodes. Note that children are processed first!
    node.acceptPostOrder(visitor);
  }


}
