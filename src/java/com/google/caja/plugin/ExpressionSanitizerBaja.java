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

import com.google.caja.lexer.Keyword;
import com.google.caja.parser.AncestorChain;
import com.google.caja.parser.MutableParseTreeNode;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.Visitor;
import com.google.caja.parser.js.ArrayConstructor;
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.Declaration;
import com.google.caja.parser.js.Expression;
import com.google.caja.parser.js.FormalParam;
import com.google.caja.parser.js.FunctionConstructor;
import com.google.caja.parser.js.FunctionDeclaration;
import com.google.caja.parser.js.Identifier;
import com.google.caja.parser.js.Operation;
import com.google.caja.parser.js.Operator;
import com.google.caja.parser.js.Reference;
import com.google.caja.parser.js.RegexpLiteral;
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
  final MessageQueue mq;
  final PluginMeta meta;

  public ExpressionSanitizerBaja(MessageQueue mq, PluginMeta meta) {
    this.mq = mq;
    this.meta = meta;
  }

  public boolean sanitize(AncestorChain<?> toSanitize) {
    rewriteFieldAccess(toSanitize.node);
    return true;
  }
  
  // $1.$2 -> $1.$2_canRead___ ? $1.$2 : __.readPub($1,"$2")
  private final class FieldAccessRewriter implements Visitor {
    // TODO(benl): if we switched FieldAccessRewriter to be pre-order, then it
    // could do this on the fly.
    private final class FindThisAndSuper implements Visitor {
      private boolean thisUsed = false;
      private boolean superUsed = false;
      private boolean argumentsUsed = false;

      // TODO(benl): Also needs to deal with closures for ES4.
      public boolean visit(AncestorChain<?> ancestors) {
        ParseTreeNode node = ancestors.node;
        if (node instanceof FunctionDeclaration) {
          // Don't descend into nested functions
          return false;
        } else if (node instanceof Reference) {
          final Reference ref = (Reference) node;
          if (isThis(ref)) {
            thisUsed = true;
          } else if (isSuper(ref)) {
            superUsed = true;
          } else if (isArguments(ref)) {
            argumentsUsed = true;
          }
        }
        // No need to look further once all are set
        if (thisUsed && superUsed && argumentsUsed) {
          return false;
        }
        return true;
      }

      public boolean usedThis() { return thisUsed; }
      public boolean usedSuper() { return superUsed; }
    }

    // var TEMP;
    private Declaration declareTmp(Expression initializer) {
      return new Declaration(new Identifier(ReservedNames.TEMP), initializer);
    }
    // instance.member
    private Operation anyMember(String instance, String member) {
      return new Operation(Operator.MEMBER_ACCESS, createReference(instance),
                           new Reference(new Identifier(member)));
    }
    // ___.what(args...)
    private Operation call___(String what, Expression... args) {
      Expression[] callChildren = new Expression[args.length + 1];
      callChildren[0] = new Operation(Operator.MEMBER_ACCESS,
          createReference("___"), createReference(what));
      System.arraycopy(args, 0, callChildren, 1, args.length);
      return new Operation(Operator.FUNCTION_CALL, callChildren);
    }
    // Replace toReplace with ___.what(args...)
    private void replaceCall___(
        AncestorChain<?> toReplace, String what, Expression... args) {
      final Operation call = call___(what, args);
      ((MutableParseTreeNode) toReplace.parent.node)
          .replaceChild(call, toReplace.node);
    }
    // ___.whatPub(foo,...) or ____.whatProp(this,....)
    private Operation call___PubOrProp(String what, Expression... args) {
      String whatType = "Pub";
      if (args[0] instanceof Reference && isThis((Reference) args[0])) {
        whatType = "Prop";
      }
      return call___(what + whatType, args);
    }
    // replace toReplace with ___.whatPub(foo,...) or ____.whatProp(this,....)
    private void replaceCall___PubOrProp(
        AncestorChain<?> toReplace, String what, Expression... args) {
      final Operation call = call___PubOrProp(what, args);
      ((MutableParseTreeNode) toReplace.parent.node)
          .replaceChild(call, toReplace.node);
    }
    private StringLiteral string(String str) {
      return new StringLiteral(StringLiteral.toQuotedValue(str));
    }
    // (function() { body })()
    private Operation privateScope(Block body) {
      final FunctionConstructor tempFn = new FunctionConstructor(
          new Identifier(null), Collections.<FormalParam>emptyList(), body);
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
      callChildren[0] = new Operation(
          Operator.MEMBER_ACCESS,
          createReference("___"),
          createReference(canWhat.toLowerCase() + canType));
      callChildren[1] = createReference(subject);
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
      final boolean isThis = isRef && isThis((Reference) lhs);
      final Operation ternary = simpleCanTernary(
          isThis, (isRef
                   ? ((Reference) lhs).getIdentifierName()
                   : ReservedNames.TEMP),
          canWhat, rhsName, op, args);
      if (isRef) {
        return ternary;
      }
      final Declaration varTmp = declareTmp(lhs);
      final Block body = new Block(Arrays.<Statement>asList(varTmp,
          new ReturnStmt(ternary)));
      return privateScope(body);
    }
    private void replaceCanTernary(
        AncestorChain<?> toReplace, Expression lhs,
        String canWhat, String rhsName, Operator op, Expression... args) {
      Operation ternary = canTernary(lhs, canWhat, rhsName, op, args);
      ((MutableParseTreeNode) toReplace.parent.node)
          .replaceChild(ternary, toReplace.node);
    }
    // func(...) { ... }
    // to <namespace>.func = ___.<wrapper>(function(...) { ... })
    private void makeFunction(
        AncestorChain<FunctionDeclaration> decl, String wrapper) {
      final FunctionConstructor func = decl.node.getInitializer();
      final Operation call = call___(wrapper, func);
      final Statement assign = TreeConstruction.assign(
          TreeConstruction.memberAccess(
              meta.namespaceName, func.getIdentifierName()),
          call);
      func.getIdentifier().clearName();
      ((MutableParseTreeNode) decl.parent.node).replaceChild(assign, decl.node);
    }

    public boolean visit(AncestorChain<?> ancestors) {
      ParseTreeNode node = ancestors.node;
      if (node.getAttributes().is(SyntheticNodes.SYNTHETIC)) {
        return true;
      }
      if (node instanceof FunctionConstructor) {
        FunctionConstructor func = (FunctionConstructor) node;
        FindThisAndSuper finder = new FindThisAndSuper();
        func.getBody().acceptPreOrder(finder, ancestors);
        if (finder.thisUsed) {
          final Statement localThis = new Declaration(
              new Identifier(ReservedNames.LOCAL_THIS),
              createReference(Keyword.THIS.toString()));
          func.getBody().prepend(localThis);
        }
        if (finder.argumentsUsed) {
          final Statement localArgs
              = new Declaration(new Identifier(ReservedNames.LOCAL_ARGUMENTS),
                                call___("args", createReference("arguments")));
          func.getBody().prepend(localArgs);
        }
      } else if (node instanceof FunctionDeclaration) {
        FunctionDeclaration decl = (FunctionDeclaration) node;
        final FunctionConstructor func = decl.getInitializer();
        FindThisAndSuper finder = new FindThisAndSuper();
        func.getBody().acceptPreOrder(
            finder, new AncestorChain<FunctionConstructor>(ancestors, func));
        if (finder.usedSuper()) {
          // TODO(benl): the spec says the function should also include "this"
          // - true? Or not?
          makeFunction(ancestors.cast(FunctionDeclaration.class), "ctor");
        } else if (finder.usedThis()) {
          makeFunction(ancestors.cast(FunctionDeclaration.class), "ctor");
        } else {
          makeFunction(ancestors.cast(FunctionDeclaration.class),
                       "simpleFunc");
        }
      } else if (node instanceof Reference) {
        final Reference ref = (Reference) node;
        if (isThis(ref)) {
          ref.getIdentifier().setName(ReservedNames.LOCAL_THIS);
        } else if (isArguments(ref)) {
          ref.getIdentifier().setName(ReservedNames.LOCAL_ARGUMENTS);
        }
      } else if (node instanceof RegexpLiteral) {
        // /regex/ becomes RegExp('regex', '')
        // /regex/modifiers becomes RegExp('regex', 'modifiers')
        final RegexpLiteral regex = (RegexpLiteral) node;
        final Operation call = new Operation(
            Operator.FUNCTION_CALL,
            createReference("RegExp"),
            string(regex.getMatchText()),
            string(regex.getModifiers()));
        ((MutableParseTreeNode) ancestors.parent.node)
            .replaceChild(call, regex);
      } else if (node instanceof Operation) {
        final Operation op = (Operation) node;
        final Operator operator = op.getOperator();
        final Expression lhs = op.children().get(0);
        Expression rhs = null;
        if (op.children().size() > 1) {
          rhs = op.children().get(1);
        }
        final MutableParseTreeNode parent
            = (MutableParseTreeNode) ancestors.parent.node;
        Operator parentOp = null;
        if (parent instanceof Operation) {
          parentOp = ((Operation) parent).getOperator();
        }
        final boolean isLHS = node == parent.children().get(0);
        if (operator == Operator.MEMBER_ACCESS) {
          final String rhsName = (rhs instanceof Reference
                                  ? ((Reference) rhs).getIdentifierName()
                                  : null);
          // this.foo_ is left untouched. Anything with __ at the end will
          // already have been rejected.
          if (lhs instanceof Reference
              && isThis((Reference) lhs) && rhsName.endsWith("_")) {
            return true;
          } else if (isLHS && parentOp == Operator.FUNCTION_CALL) {
            int numChildren = parent.children().size();
            Expression[] args = parent.children().subList(1, numChildren)
                .toArray(new Expression[numChildren - 1]);
            replaceCanTernary(
                ancestors.parent, lhs, "Call", rhsName,
                Operator.FUNCTION_CALL, args);
          } else if (isLHS && parentOp == Operator.ASSIGN) {
            replaceCanTernary(
                ancestors.parent, lhs, "Set", rhsName,
                Operator.ASSIGN, (Expression) parent.children().get(1));
          } else if (isLHS && parentOp == Operator.DELETE) {
            replaceCall___PubOrProp(
                ancestors.parent, "delete", lhs, string(rhsName));
          } else {
            replaceCanTernary(ancestors, lhs, "Read", rhsName, null);
          }
        } else if (operator == Operator.SQUARE_BRACKET) {
          if (isLHS && parentOp == Operator.ASSIGN) {
            Expression child = (Expression) parent.children().get(1);
            replaceCall___PubOrProp(ancestors.parent, "set", lhs, rhs, child);
          } if (isLHS && parentOp == Operator.DELETE) {
            replaceCall___PubOrProp(ancestors.parent, "delete", lhs, rhs);
          } else {
            replaceCall___PubOrProp(ancestors, "read", lhs, rhs);
          }
        } else if (operator == Operator.IN) {
          final Operation call = call___PubOrProp("canRead", rhs, lhs);
          final Operation andAnd
              = new Operation(Operator.LOGICAL_AND, op, call);
          parent.replaceChild(andAnd, op);
        } else if (operator == Operator.CONSTRUCTOR) {
          // FIXME(benl): what if it's not as in (new Date)?
          assert parentOp == Operator.FUNCTION_CALL;
          assert rhs == null;
          // XXX(benl): Why does the class appear as a child of both CONSTRUCTOR
          // and FUNCTION_CALL?
          List<? extends Expression> constructorArgs = ((Operation) parent)
              .children().subList(1, parent.children().size());
          final ArrayConstructor args = new ArrayConstructor(constructorArgs);
          replaceCall___(ancestors.parent, "callNew", lhs, args);
        }
      }
      return true;
    }
  }

  private void rewriteFieldAccess(ParseTreeNode node) {
    // We use PostOrder because it visits all children, even those of changed
    // nodes. Note that children are processed first!
    node.acceptPostOrder(new FieldAccessRewriter(), null);
  }

  private static Reference createReference(String identifier) {
    return new Reference(new Identifier(identifier));
  }

  private static boolean isThis(Reference ref) {
    return ref.getIdentifierName().equals(Keyword.THIS.toString())
        || ref.getIdentifierName().equals(ReservedNames.LOCAL_THIS);
  }

  private static boolean isSuper(Reference ref) {
    return ref.getIdentifierName().equals(ReservedNames.SUPER);
  }

  private static boolean isArguments(Reference ref) {
    return ref.getIdentifierName().equals(ReservedNames.ARGUMENTS)
        || ref.getIdentifierName().equals(ReservedNames.LOCAL_ARGUMENTS);
  }

}
