// Copyright (C) 2009 Google Inc.
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

import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.js.CatchStmt;
import com.google.caja.parser.js.Declaration;
import com.google.caja.parser.js.Expression;
import com.google.caja.parser.js.ExpressionStmt;
import com.google.caja.parser.js.FormalParam;
import com.google.caja.parser.js.FunctionConstructor;
import com.google.caja.parser.js.FunctionDeclaration;
import com.google.caja.parser.js.Identifier;
import com.google.caja.parser.js.Noop;
import com.google.caja.parser.js.Operation;
import com.google.caja.parser.js.Operator;
import com.google.caja.parser.js.Reference;
import com.google.caja.parser.js.Statement;
import com.google.caja.parser.js.TryStmt;

import java.util.Map;

/**
 * A set of rewriter rules that ignores nodes marked synthetic.
 *
 * @author mikesamuel@gmail.com
 * @see com.google.caja.parser.js.SyntheticNodes
 */
class SyntheticRuleSet {

  public static Rule[] syntheticRules(final Rewriter rw) {
    ////////////////////////////////////////////////////////////////////////
    // Do nothing if the node is already the result of some translation
    ////////////////////////////////////////////////////////////////////////
    return new Rule[] {

    new Rule() {
      @Override
      @RuleDescription(
          name="syntheticReference",
          synopsis="Pass through synthetic references.",
          reason="A variable may not be mentionable otherwise.",
          matches="/* synthetic */ @ref",
          substitutes="<expanded>")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        if (node instanceof Reference) {
          Reference ref = (Reference) node;
          if (isSynthetic(ref.getIdentifier())) {
            // noexpand needed since node itself may not be synthetic
            // even though we now know it contains a synthetic identifier.
            return rw.noexpand((Reference) node);
          }
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="syntheticCalls",
          synopsis="Pass through calls where the function name is synthetic.",
          reason="A synthetic method may not be marked callable.",
          matches="/* synthetic */ @f(@as*)",
          substitutes="<expanded>")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = this.match(node);
        if (bindings != null) {
          Expression f = (Expression) bindings.get("f");
          if (f instanceof Reference && isSynthetic((Reference) f)) {
            return expandAll(node, scope);
          }
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="syntheticMethodCalls",
          synopsis="Pass through calls where the method name is synthetic.",
          reason="A synthetic method may not be marked callable.",
          matches="/* synthetic */ @o.@m(@as*)",
          substitutes="<expanded>")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = this.match(node);
        if (bindings != null && isSynthetic((Reference) bindings.get("m"))) {
          return expandAll(node, scope);
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="syntheticDeletes",
          synopsis="Pass through deletes of synthetic members.",
          reason="A synthetic member may not be marked deletable.",
          matches="/* synthetic */ delete @o.@m",
          substitutes="<expanded>")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = this.match(node);
        if (bindings != null && isSynthetic((Reference) bindings.get("m"))) {
          return expandAll(node, scope);
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="syntheticReads",
          synopsis="Pass through reads of synthetic members.",
          reason="A synthetic member may not be marked readable.",
          matches="/* synthetic */ @o.@m",
          substitutes="<expanded>")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = this.match(node);
        if (bindings != null && isSynthetic((Reference) bindings.get("m"))) {
          return expandAll(node, scope);
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="syntheticSetMember",
          synopsis="Pass through sets of synthetic members.",
          reason="A synthetic member may not be marked writable.",
          matches="/* synthetic */ @o.@m = @v",
          substitutes="<expanded>")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = this.match(node);
        if (bindings != null && isSynthetic((Reference) bindings.get("m"))) {
          return expandAll(node, scope);
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="syntheticSetVar",
          synopsis="Pass through set of synthetic vars.",
          reason="A local variable might not be mentionable otherwise.",
          matches="/* synthetic */ @lhs = @rhs",
          substitutes="<expanded>")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = this.match(node);
        if (bindings != null && bindings.get("lhs") instanceof Reference) {
          if (isSynthetic((Reference) bindings.get("lhs"))) {
            return expandAll(node, scope);
          }
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="syntheticDeclaration",
          synopsis="Pass through synthetic variables which are unmentionable.",
          reason="Synthetic code might need local variables for safe-keeping.",
          matches="/* synthetic */ var @v = @initial?;",
          substitutes="<expanded>")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = this.match(node);
        if (bindings != null && isSynthetic((Identifier) bindings.get("v"))) {
          Declaration d = (Declaration) expandAll(node, scope);
          Statement s;
          if (d.getInitializer() == null) {
            s = new Noop(d.getFilePosition());
          } else {
            s = new ExpressionStmt(
                Operation.createInfix(
                    Operator.ASSIGN, new Reference(d.getIdentifier()),
                    d.getInitializer()));
            getRewriter().markTreeForSideEffect(s);
            d.removeChild(d.getInitializer());
          }
          scope.addStartOfScopeStatement(d);
          return s;
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="syntheticFormals",
          synopsis="Pass through synthetic formals which are unmentionable.",
          reason="Synthetic code might need local variables for safe-keeping.",
          matches="/* synthetic */ @x in a parameter list",
          substitutes="<expanded>")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        if (node instanceof FormalParam
            && isSynthetic(((FormalParam) node).getIdentifier())) {
          return expandAll(node, scope);
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="syntheticFnDeclaration",
          synopsis="Allow declaration of synthetic functions.",
          reason="Synthetic functions allow generated code to avoid introducing"
              + " unnecessary scopes.",
          matches="/* synthetic */ function @i?(@actuals*) { @body* }",
          substitutes="<expanded>")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        FunctionConstructor ctor = node instanceof FunctionDeclaration
            ? ((FunctionDeclaration) node).getInitializer()
            : (FunctionConstructor) node;
        if (isSynthetic(ctor)) {
          Scope newScope = Scope.fromFunctionConstructor(scope, ctor);
          ParseTreeNode result = expandAll(node, newScope);
          for (Statement s : newScope.getStartStatements()) {
            scope.addStartStatement(s);
          }
          return result;
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="syntheticCatches1",
          synopsis="Pass through synthetic variables which are unmentionable.",
          reason="Catching unmentionable exceptions helps maintain invariants.",
          matches=(
              "try { @body*; } catch (/* synthetic */ @ex___) { @handler*; }"),
          substitutes="try { @body*; } catch (@ex___) { @handler*; }")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = this.match(node);
        if (bindings != null) {
          Identifier ex = (Identifier) bindings.get("ex");
          TryStmt ts = (TryStmt) node;
          CatchStmt cs = ts.getCatchClause();
          if (isSynthetic(ex)) {
            return substV(
                "body", rw.expand(bindings.get("body"), scope),
                "ex", rw.noexpand(ex),
                "handler", rw.expand(
                    bindings.get("handler"), Scope.fromCatchStmt(scope, cs)));
          }
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="syntheticCatches2",
          synopsis="Pass through synthetic variables which are unmentionable.",
          reason="Catching unmentionable exceptions helps maintain invariants.",
          matches=(
               "try { @body*; } catch (/* synthetic */ @ex___) { @handler*; }"
               + " finally { @cleanup*; }"),
          substitutes=(
               "try { @body*; } catch (/* synthetic */ @ex___) { @handler*; }"
               + " finally { @cleanup*; }"))
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = this.match(node);
        if (bindings != null) {
          TryStmt ts = (TryStmt) node;
          CatchStmt cs = ts.getCatchClause();
          Identifier ex = (Identifier) bindings.get("ex");
          if (isSynthetic(ex)) {
            return substV(
                "body", rw.expand(bindings.get("body"), scope),
                "ex", rw.noexpand(ex),
                "handler", rw.expand(
                    bindings.get("handler"), Scope.fromCatchStmt(scope, cs)),
                "cleanup", rw.expand(bindings.get("cleanup"), scope)
                );
          }
        }
        return NONE;
      }
    },

    };
  }
}
