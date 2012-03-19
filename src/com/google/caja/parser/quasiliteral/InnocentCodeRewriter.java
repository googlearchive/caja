// Copyright (C) 2008 Google Inc.
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
import com.google.caja.parser.ParseTreeNodeContainer;
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.Expression;
import com.google.caja.parser.js.ExpressionStmt;
import com.google.caja.parser.js.FunctionConstructor;
import com.google.caja.parser.js.Identifier;
import com.google.caja.parser.js.Reference;
import com.google.caja.reporting.MessageQueue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Rewrites a JavaScript parse tree for trusted code that needs to
 * interact with cajoled code.  Fixes for-each loops so that they
 * don't enumerate hidden Caja properties (foo___).  Also adds
 * (this != global scope) checks at the tops of functions that
 * use the THIS keyword.
 *
 * @author adriennefelt@gmail.com (Adrienne Felt)
 */
@RulesetDescription(
    name="Innocent Code Transformer",
    synopsis="Lets trusted JS code interact with cajoled code"
  )
public class InnocentCodeRewriter extends Rewriter {
  final public Rule[] innocentRules = {

    new Rule() {
      @Override
      @RuleDescription(
          name="module",
          synopsis="",
          reason="",
          matches="{@ss*;}",
          substitutes=(
              "@startStmts*;" +
              "@thisVar?;" +
              "@expanded*;"))
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        if (node instanceof Block && scope == null) {
          Scope s2 = Scope.fromProgram((Block) node, InnocentCodeRewriter.this);
          List<ParseTreeNode> expanded = new ArrayList<ParseTreeNode>();
          for (ParseTreeNode c : node.children()) {
            expanded.add(expand(c, s2));
          }

          // If the program body has a free THIS, bind this___ to the global
          // object.  This is consistent with ES5 strict.
          ParseTreeNode thisVar = null;
          if (s2.hasFreeThis()) {
            thisVar = QuasiBuilder.substV("var this___ = this;");
          }

          return substV(
              "startStmts", new ParseTreeNodeContainer(s2.getStartStatements()),
              "thisVar", thisVar,
              "expanded", new ParseTreeNodeContainer(expanded));
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="functions",
          synopsis="",
          reason="",
          matches="function @f?(@ps*) { @bs* }",
          substitutes=(
              "function @f?(@params*) {" +
              "  @startStmts*;" +
              "  @thisVar?;" +
              "  @body*" +
              "}"))
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = match(node);
        if (bindings != null) {
          Scope s2 = Scope.fromFunctionConstructor(
              scope, (FunctionConstructor) node);
          ParseTreeNode params = expandAll(bindings.get("ps"), s2);
          ParseTreeNode body = expandAll(bindings.get("bs"), s2);

          // Checks to see if the block contains a free THIS and emulate ES5
          // strict mode behavior where it is undefined if called without an
          // object to the left.  We cannot exactly emulate the ES5 strict
          // behavior without a much heavierweight rewriting as described in
          // issue 1019, so we always void out the global object.
          ParseTreeNode thisVar = null;
          if (s2.hasFreeThis()) {
            thisVar = QuasiBuilder.substV(
                "var this___ = this && this.___ ? void 0 : this;");
          }

          return substV(
              "thisVar", thisVar,
              "f", bindings.get("f"),
              "params", params,
              "startStmts", new ParseTreeNodeContainer(s2.getStartStatements()),
              "body", body);
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="this",
          synopsis="Replaces references to 'this' with references to this___",
          reason=("So that we can check whether this points to the global scope"
                  + " and substitute a reasonable value."),
          matches="this",
          substitutes="this___")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        if (match(node) != null) { return substV(); }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="foreach",
          synopsis="",
          reason="Filters out hidden properties ending in ___ in for loops",
          matches="for (@k in @o) @ss;",
          substitutes=(
            "for (@kTempStmt in @o) { " +
            "  if (@kTempRef.match(/___$/)) { " +
            "    continue; " +
            "  } " +
            "  @kAssignment;" +
            "  @ss;" +
            "}"))
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = makeBindings();

        if (QuasiBuilder.match("for (var @k in @o) @ss;", node, bindings)) {
          bindings.put("k", new Reference((Identifier) bindings.get("k")));
        } else if (QuasiBuilder.match("for (@k in @o) @ss;", node, bindings)) {
          ExpressionStmt es = (ExpressionStmt) bindings.get("k");
          bindings.put("k", es.getExpression());
        } else {
          return NONE;
        }

        Reference kTemp = scope.declareStartOfScopeTemp();
        ParseTreeNode kAssignment = QuasiBuilder.substV(
            "@k = @kTempRef;",
            "k", bindings.get("k"),
            "kTempRef", kTemp);
        kAssignment = expandAll(kAssignment, scope);
        kAssignment = newExprStmt((Expression) kAssignment);

        return substV(
            "kTempStmt", newExprStmt(kTemp),
            "kTempRef", kTemp,
            "o", bindings.get("o"),
            "kAssignment", kAssignment,
            "ss", expandAll(bindings.get("ss"), scope));
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="recurse",
          synopsis="Automatically recurse into some structures",
          reason="")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        return expandAll(node, scope);
      }
    }
  };

  public InnocentCodeRewriter(MessageQueue mq, boolean logging) {
    super(mq, false, logging);
    addRules(innocentRules);
  }
}
