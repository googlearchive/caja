// Copyright (C) 2006 Google Inc.
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

import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.Keyword;
import com.google.caja.parser.AbstractParseTreeNode;
import com.google.caja.parser.AncestorChain;
import com.google.caja.parser.MutableParseTreeNode;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.Visitor;
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.CatchStmt;
import com.google.caja.parser.js.Declaration;
import com.google.caja.parser.js.Expression;
import com.google.caja.parser.js.ExpressionStmt;
import com.google.caja.parser.js.FunctionConstructor;
import com.google.caja.parser.js.Identifier;
import com.google.caja.parser.js.Operation;
import com.google.caja.parser.js.Operator;
import com.google.caja.parser.js.Reference;
import com.google.caja.parser.js.Statement;
import com.google.caja.parser.js.StringLiteral;
import com.google.caja.parser.js.UndefinedLiteral;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageQueue;
import static com.google.caja.plugin.SyntheticNodes.s;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Utilities for rewriting javascript parse trees to remove unsafe constructs
 * using local analysis.
 * This does not rewrite references, as that requires global analysis.
 *
 * @author mikesamuel@gmail.com
 */
public final class ExpressionSanitizer {
  private final MessageQueue mq;

  private static final Map<String, String> FIELD_REWRITE_MAP;

  static {
    // TODO(ihab): Consider moving to a different stage, since these
    // change language semantics.
    // TODO(ihab): Sync this list with FBJS.
    String[][] fieldAccessRewrites = {
      {"nodeType", "getNodeType"},
      {"nodeName", "getNodeName"},
      {"nodeValue", "getNodeValue"},
      {"id", "getId"},
      {"class", "getClass"},
      {"innerHTML", "getInnerHTML"},
      {"style", "getStyle"},
      {"firstChild", "getFirstChild"},
      {"nextSibling", "getNextSibling"},
      {"offsetLeft", "getOffsetLeft"},
      {"offsetWidth", "getOffsetWidth"},
      {"offsetTop", "getOffsetTop"},
      {"offsetHeight", "getOffsetHeight"},
    };

    HashMap<String, String> fieldRewriteMap = new HashMap<String, String>();
    for (int i = 0; i < fieldAccessRewrites.length; i++) {
      fieldRewriteMap.put(fieldAccessRewrites[i][0], fieldAccessRewrites[i][1]);
    }
    FIELD_REWRITE_MAP = Collections.unmodifiableMap(fieldRewriteMap);
  }

  ExpressionSanitizer(MessageQueue mq) {
    this.mq = mq;
  }

  boolean sanitize(AncestorChain<?> toSanitize) {
    RewritingPass1 pass1 = new RewritingPass1();
    toSanitize.node.acceptPostOrder(pass1, toSanitize.parent);
    rewriteFieldAccessesAsFunctionCalls(toSanitize);
    if (toSanitize.node instanceof AbstractParseTreeNode) {
      inferFilePositionsForSyntheticNodes(
          (AncestorChain<AbstractParseTreeNode<?>>) toSanitize);
    }
    return pass1.isValid();
  }

  private final class RewritingPass1 implements Visitor {
    private boolean valid = true;

    public boolean isValid() { return valid; }

    public boolean visit(AncestorChain<?>  ancestors) {
      ParseTreeNode node = ancestors.node;
      if (node.getAttributes().is(SyntheticNodes.SYNTHETIC)) { return true; }
      if (node instanceof Operation) {
        Operation op = (Operation) node;
        switch (op.getOperator()) {
        case FUNCTION_CALL:
          // Rewrite <fn>(...) to <fn>.call(this, ...) when <fn> is not a member
          // access operation or a new operation so that this doesn't refer to
          // the global scope.

          // In the case of member access operations, we know that this is the
          // LHS, and in the case of new, we know that this is a blank object.
          Expression fn = op.children().get(0);
          if (fn instanceof Operation) {
            Operator fnOp = ((Operation) fn).getOperator();
            if (Operator.MEMBER_ACCESS == fnOp
                || Operator.CONSTRUCTOR == fnOp) {
              break;
            }
          }

          Operation callOp = s(new Operation(
                                   Operator.MEMBER_ACCESS,
                                   fn, s(new Reference(new Identifier("call")))));
          setFilePosition(callOp, fn.getFilePosition());
          op.replaceChild(callOp, fn);
          Expression firstArg =
            op.children().size() > 1 ? op.children().get(1) : null;
          op.insertBefore(s(new Reference(new Identifier(Keyword.THIS.toString()))), firstArg);
          break;
        case SQUARE_BRACKET:
          // a[b] -> plugin_get___(a, b)

          // get__ looks something like
          // TODO(msamuel): not safe to refer to Object like this
          // function plugin_get___(m, k) {
          //   if ('number' !== (typeof k) && null != k) {
          //     k = '' + k;
          //     if (/^(?:constructor|prototype|caller)$/.test(k)
          //         && !Object.hasOwnProperty.call(m, k)) {
          //       return void 0;
          //     }
          //   }
          //   return m[k];
          // }

          // If b is always a number, boolean, null, or undefined then we
          // can skip this.
          // TODO: do some very simple type inference on b if b is an operator.

          Operation sqbracketOp = (Operation) node;
          if (!isAssignedOnly(ancestors.cast(Operation.class), false)) {
            Expression a = sqbracketOp.children().get(0),
                b = sqbracketOp.children().get(1);
            sqbracketOp.removeChild(a);
            sqbracketOp.removeChild(b);
            Operation getter = s(new Operation(
                                     Operator.FUNCTION_CALL,
                                     s(new Reference(new Identifier("plugin_get___"))),
                                     a,
                                     b));
            getter.setFilePosition(sqbracketOp.getFilePosition());
            ((MutableParseTreeNode) ancestors.parent.node).replaceChild(
                getter, sqbracketOp);
          }
          break;
        case ASSIGN:
          // a.innerHTML = b -> a.setInnerHTML(b)
          rewriteFieldAssignmentsAsFunctionCalls(
              ancestors.cast(Operation.class), "innerHTML", "setInnerHTML");
          // a.class = b -> a.setClass(b)
          rewriteFieldAssignmentsAsFunctionCalls(
              ancestors.cast(Operation.class), "class", "setClass");
          break;
        case ASSIGN_SUM:
          // a.innerHTML += b -> a.appendInnerHTML(b)
          rewriteFieldAssignmentsAsFunctionCalls(
              ancestors.cast(Operation.class), "innerHTML", "appendInnerHTML");
          break;
        default:
          break;
        }
      } else if (node instanceof CatchStmt) {
        // catch (e) ... ->
        //   catch (safe_ex___) { var e = '' + safe_ex___; ... }
        // to get around scoping ambiguities, and to make sure that they can't
        // obtain a reference from an exception
        // TODO(msmauel): where are thrown exceptions sanitized?
        CatchStmt cs = (CatchStmt) node;
        Declaration ex = cs.getException();
        Statement body = cs.getBody();
        Block bodyBlock;
        if (body instanceof Block) {
          bodyBlock = (Block) body;
        } else {
          bodyBlock = s(new Block(Arrays.asList(body)));
          bodyBlock.setFilePosition(body.getFilePosition());
          cs.replaceChild(bodyBlock, body);
        }
        Declaration redec =
          s(new Declaration(
                new Identifier(ex.getIdentifierName()),
                s(new Operation(
                      Operator.ADDITION,
                      s(new StringLiteral("''")),
                      s(new Reference(new Identifier("safe_ex___")))))));
        Statement first =
          !bodyBlock.children().isEmpty() ? bodyBlock.children().get(0) : null;
        bodyBlock.insertBefore(redec, first);

        cs.replaceChild(s(new Declaration(new Identifier("safe_ex___"), (Expression)null)), ex);
      } else if (node instanceof Declaration) {
        // Disallow creation or reference to references
        // in the protected "__xxx__" and "xxx___" namespaces.
        String ident = ((Declaration) node).getIdentifierName();
        if (inProtectedNamespace(ident)) {
          mq.addMessage(PluginMessageType.UNSAFE_ACCESS, node.getFilePosition(),
                        MessagePart.Factory.valueOf(ident));
          valid = false;
        }
      } else if (node instanceof Reference) {
        // Disallow reference to stuff in the protected namespace
        Reference r = (Reference) node;
        String ident = r.getIdentifierName();
        if (inProtectedNamespace(ident) || "caller".equals(ident)
            || "constructor".equals(ident)) {
          mq.addMessage(PluginMessageType.UNSAFE_ACCESS, node.getFilePosition(),
                        MessagePart.Factory.valueOf(ident));
          valid = false;
        // Disallow reference to .prototype except as an lvalue
        } else if ("prototype".equals(ident) &&
                   !isAssignedOnly(ancestors.cast(Expression.class), false)) {
          MutableParseTreeNode parent
              = (MutableParseTreeNode) ancestors.getParentNode();
          if (!isAssignedOnly(ancestors.cast(Expression.class), true)) {
            if (parent instanceof Operation
                && Operator.MEMBER_ACCESS == ((Operation) parent).getOperator()
                && !ancestors.isFirstSibling()) {
              // Convert to plugin_get___ so that we can make sure it's only
              // used on user defined functions
              StringLiteral key =
                s(new StringLiteral(StringLiteral.toQuotedValue(ident)));
              key.setFilePosition(r.getFilePosition());
              Expression left = (Expression) parent.children().get(0);
              // Remove left, so we can use it in callToGet
              parent.replaceChild(new UndefinedLiteral(), left);
              Operation callToGet = s(
                  new Operation(
                      Operator.FUNCTION_CALL,
                      s(new Reference(new Identifier("plugin_get___"))),
                      left,
                      key));
              MutableParseTreeNode gparent
                  = (MutableParseTreeNode) ancestors.parent.parent.node;
              gparent.replaceChild(callToGet, parent);
            } else {
              mq.addMessage(PluginMessageType.UNSAFE_ACCESS,
                            node.getFilePosition(),
                            MessagePart.Factory.valueOf(ident));
              valid = false;
            }
          } else {
            mq.addMessage(PluginMessageType.UNSAFE_ACCESS,
                          node.getFilePosition(),
                          MessagePart.Factory.valueOf(ident));
            valid = false;
          }
        }

      } else if (node instanceof FunctionConstructor) {
        FunctionConstructor fn = (FunctionConstructor) node;
        // Assert that the this parameter is not a window
        //   plugin_require___(this !== window);
        // if the function body references this.
        Block oldBody = fn.getBody();
        if (referencesThis(oldBody)) {
          ExpressionStmt require = s(
              new ExpressionStmt(
                  s(new Operation(
                      Operator.FUNCTION_CALL,
                      s(new Reference(new Identifier("plugin_require___"))),
                      s(new Operation(
                          Operator.STRICTLY_NOT_EQUAL,
                          s(new Reference(new Identifier(Keyword.THIS.toString()))),
                          s(new Reference(new Identifier("window")))
                      ))
                  ))
              ));
          Block newBody = s(new Block(Collections.singletonList(require)));

          require.setFilePosition(
              FilePosition.startOf(oldBody.getFilePosition()));
          newBody.setFilePosition(oldBody.getFilePosition());

          fn.replaceChild(newBody, oldBody);

          MutableParseTreeNode.Mutation newChanges = newBody.createMutation(),
                                        oldChanges = oldBody.createMutation();
          for (Statement stmt : oldBody.children()) {
            oldChanges.removeChild(stmt);
            newChanges.insertBefore(stmt, null);
          }
          oldChanges.execute();
          newChanges.execute();
        }
      }
      return true;
    }
  }

  private void rewriteFieldAssignmentsAsFunctionCalls(
      AncestorChain<Operation> opChain, String fieldName, String functionName) {
    Operation op = opChain.node;
    Expression lhs = op.children().get(0);
    Expression rhs = op.children().get(1);
    if (lhs instanceof Operation) {
      Operation lhsOp = (Operation) lhs;
      if (lhsOp.getOperator() == Operator.MEMBER_ACCESS) {
        Expression object = lhsOp.children().get(0);
        Expression field = lhsOp.children().get(1);
        if (field instanceof Reference &&
            ((Reference) field).getIdentifierName().equals(fieldName)) {
          ParseTreeNode parentNode = opChain.getParentNode();
          if (parentNode instanceof MutableParseTreeNode) {
            MutableParseTreeNode mutableParentNode =
                (MutableParseTreeNode) parentNode;

            Expression newFunctionCall =
                s(new Operation(
                      Operator.FUNCTION_CALL,
                      s(new Operation(
                            Operator.MEMBER_ACCESS,
                            object,
                            s(new Reference(new Identifier(functionName))))),
                      rhs));

            mutableParentNode.replaceChild(newFunctionCall, op);
          }
        }
      }
    }
  }

  private void rewriteFieldAccessesAsFunctionCalls(AncestorChain<?> toRewrite) {
    FieldAccessRewriter visitor = new FieldAccessRewriter();
    toRewrite.node.acceptPreOrder(visitor, toRewrite.parent);
  }

  private final class FieldAccessRewriter implements Visitor {

    public FieldAccessRewriter() {}

    public boolean visit(AncestorChain<?>  ancestors) {
      ParseTreeNode node = ancestors.node;
      if (node.getAttributes().is(SyntheticNodes.SYNTHETIC)) { return true; }
      if (node instanceof Operation) {
        Operation op = (Operation) node;
        if (op.getOperator() == Operator.MEMBER_ACCESS) {
          Expression lhs = op.children().get(0);
          Expression rhs = op.children().get(1);
          if (rhs instanceof Reference && FIELD_REWRITE_MAP
              .containsKey(((Reference) rhs).getIdentifierName())) {
            ParseTreeNode nodeParent = ancestors.getParentNode();
            if (nodeParent instanceof MutableParseTreeNode) {
              MutableParseTreeNode parent = (MutableParseTreeNode) nodeParent;
              String rewrittenName = FIELD_REWRITE_MAP.get(
                  ((Reference) rhs).getIdentifierName());
              Expression newFunctionCall =
                  s(new Operation(
                        Operator.FUNCTION_CALL,
                        s(new Operation(
                              Operator.MEMBER_ACCESS,
                              lhs,
                              s(new Reference(new Identifier(rewrittenName)))
                              ))));
              parent.replaceChild(newFunctionCall, op);
            }
          }
        }
      }
      return true;
    }
  }

  static void setFilePosition(AbstractParseTreeNode<?> n, FilePosition p) {
    n.setFilePosition(p);
    for (ParseTreeNode child : n.children()) {
      if (null != child.getFilePosition()) {
        setFilePosition((AbstractParseTreeNode<?>) child, p);
      }
    }
  }

  /**
   * The protected namespace that sandboxed code is not allowed to touch.
   * includes the special __proto__ member.
   */
  static boolean inProtectedNamespace(String s) {
    return s.startsWith("__") || s.endsWith("__");
  }

  static boolean isAssignedOnly(AncestorChain<? extends Expression> e,
                                boolean allowSecondary) {
    // Allow if the expression is part of an lvalue.
    // Allow E in
    //   A.E = B
    // which resolves to a parsetree like
    // 0 Statement
    // 1   Operation : ASSIGN
    // 2     Operation : MEMBER_ACCESS
    //         A
    //         E
    //       B
    // We also want to allow
    //   A.E.C = B
    // which resolves to a parsetree like
    // 0 Statement
    // 1   Operation : ASSIGN // 1 or more levels as long as is right child
    // 2    Operation : MEMBER_ACCESS      // 0 or more levels
    // 2       Operation : MEMBER_ACCESS   // ...
    //           A
    //           E
    //         C
    //       B

    AncestorChain<? extends Expression> child = e;
    AncestorChain<?> p2 = child.parent;

    if (p2 != null && p2.node instanceof Operation
        && Operator.MEMBER_ACCESS == ((Operation) p2.node).getOperator()) {
      if (child.isFirstSibling()) { return false; }
      child = p2.cast(Operation.class);
      p2 = child.parent;
    }

    while (p2 != null && p2.node instanceof Operation
           && Operator.MEMBER_ACCESS == ((Operation) p2.node).getOperator()) {
      // lhs of member access
      if (child.isLastSibling()) { return false; }
      child = p2.cast(Operation.class);
      p2 = child.parent;
    }

    AncestorChain<?> p1 = p2;
    if (!(p1 != null && p1.node instanceof Operation)) {
      return false;
    }
    if (Operator.ASSIGN != ((Operation) p1.node).getOperator()) {
      return false;
    }
    if (child.isLastSibling()) { return false; }

    AncestorChain<?> p0 = p1.parent;
    if (allowSecondary) {
      while (p0 != null && p0.node instanceof Operation
             && Operator.ASSIGN == ((Operation) p0.node).getOperator()) {
        if (p1.isFirstSibling()) { return false; }
        p1 = p0;
        p0 = p1.parent;
      }
    }
    return p0 != null && p0.node instanceof Statement;
  }

  /**
   * A lot of visitors require file position information for parse tree nodes
   * so that they can accurately report errors.
   */
  static FilePosition inferFilePositionsForSyntheticNodes(
      AncestorChain<AbstractParseTreeNode<?>> nodeChain) {

    AbstractParseTreeNode<?> node = nodeChain.node;
    // Constraints:
    // 1) A child's span must not exceed its parents
    // 2) A node's span must start on or after its previous sibling ends
    // 3) A node's span must end on or before its next sibling

    FilePosition pos = node.getFilePosition();
    if (null != pos) {
      for (ParseTreeNode child : node.children()) {
        AbstractParseTreeNode<?> achild = (AbstractParseTreeNode<?>) child;
        inferFilePositionsForSyntheticNodes(
            new AncestorChain<AbstractParseTreeNode<?>>(nodeChain, achild));
      }
      return pos;
    }

    // Recurse first to get information from children
    for (ParseTreeNode child : node.children()) {
      AbstractParseTreeNode<?> achild = (AbstractParseTreeNode<?>) child;
      FilePosition childSpan = inferFilePositionsForSyntheticNodes(
          new AncestorChain<AbstractParseTreeNode<?>>(nodeChain, achild));
      pos = (null == pos)
          ? childSpan
          : (pos.source().equals(childSpan.source()) &&
             pos.startCharInFile() <= childSpan.endCharInFile())
            ? FilePosition.span(pos, childSpan)
            : pos;
    }

    if (null != pos) {
      node.setFilePosition(pos);
      return pos;
    }

    {
      AncestorChain<?> tmp = nodeChain;
      predecessorLoop:
      do {
        FilePosition prevPos = tmp.node.getFilePosition();
        if (null != prevPos) {
          pos = FilePosition.startOf(prevPos);
          break;
        }
        int siblingIndex = tmp.indexInParent();
        while (--siblingIndex >= 0) {
          ParseTreeNode prev = tmp.parent.node.children().get(siblingIndex);
          prevPos = prev.getFilePosition();
          if (null != prevPos) {
            pos = FilePosition.endOf(prevPos);
            break predecessorLoop;
          }
        }
        tmp = tmp.parent;
      } while (null != tmp);
    }

    if (null != pos) {
      node.setFilePosition(pos);
      return pos;
    }

    {
      AncestorChain<?> tmp = nodeChain;
      successorLoop:
      do {
        int siblingIndex = tmp.indexInParent();
        int siblingLimit
            = tmp.parent != null ? tmp.parent.node.children().size() : 0;

        while (++siblingIndex < siblingLimit) {
          ParseTreeNode next = tmp.parent.node.children().get(siblingIndex);
          FilePosition nextPos = next.getFilePosition();
          if (null != nextPos) {
            pos = FilePosition.startOf(nextPos);
            break successorLoop;
          }
        }
        tmp = tmp.parent;
      } while (null != tmp);
    }

    if (null != pos) {
      node.setFilePosition(pos);
      return pos;
    }

    return null;
  }

  private static boolean referencesThis(ParseTreeNode node) {
    if (node instanceof Reference) {
      return Keyword.THIS.toString().equals(((Reference) node).getIdentifierName());
    }
    if (node instanceof FunctionConstructor) {
      return false;
    }
    for (ParseTreeNode child : node.children()) {
      if (referencesThis(child)) { return true; }
    }
    return false;
  }
}
