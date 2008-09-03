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
import com.google.caja.parser.js.*;
import com.google.caja.reporting.MessageQueue;
import static com.google.caja.parser.quasiliteral.QuasiBuilder.substV;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;

/**
 * Rewrites a JavaScript parse tree for trusted code that needs to
 * interact with cajoled code.  Fixes for-each loops so that they
 * don't enumerate hidden Caja properties (foo___).  Also adds
 * (this != global scope) checks at the tops of functions that
 * use the THIS keyword.
 *
 * @author adriennefelt@gmail.com (Adrienne Felt)
 *
 */
@RulesetDescription(
    name="Innocent Code Transformer",
    synopsis="Lets trusted JS code interact with cajoled code"
  )
public class InnocentCodeRewriter extends Rewriter {
  final public Rule[] innocentRules = {

    new Rule () {
      @Override
      @RuleDescription(
          name="module",
          synopsis="",
          reason="",
          matches="{@ss*;}")
      public ParseTreeNode fire(
          ParseTreeNode node, Scope scope, MessageQueue mq) {
        if (node instanceof Block && scope == null) {
          Scope s2 = Scope.fromProgram((Block) node, mq);
          List<ParseTreeNode> expanded = new ArrayList<ParseTreeNode>();
          for (ParseTreeNode c : node.children()) {
            expanded.add(expand(c, s2, mq));
          }

          // Checks to see if the block contains a free THIS
          ParseTreeNode refError = null;
          if (s2.hasFreeThis()) {
            refError = substV("if (this.___) { throw ReferenceError; }");
          }

          return substV(
              "@startStmts*;" +
              "@refError?;" +
              "@expanded*;",
              "startStmts", new ParseTreeNodeContainer(s2.getStartStatements()),
              "refError", refError,
              "expanded", new ParseTreeNodeContainer(expanded));
        }
        return NONE;
      }
    },

    new Rule () {
      @Override
      @RuleDescription(
          name="functions",
          synopsis="",
          reason="",
          matches="function @f? (@ps*) { @bs* }")
      public ParseTreeNode fire(
        ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = match(node);
        if (bindings != null) {
          Scope s2 = Scope.fromFunctionConstructor(
              scope, (FunctionConstructor) node);
          ParseTreeNode params = expandAll(bindings.get("ps"), s2, mq);
          ParseTreeNode body = expandAll(bindings.get("bs"), s2, mq);

          // If the function has a free THIS, check what it binds to at runtime
          ParseTreeNode refError = null;
          if (s2.hasFreeThis()) {
            refError = substV("if (this.___) { throw ReferenceError; }");
          }

          return substV(
              "function @f? (@params*) {" +
              "  @startStmts*;" +
              "  @refError?;" +
              "  @body*" +
              "}",
              "refError", refError,
              "f", bindings.get("f"),
              "ps", bindings.get("params"),
              "params", params,
              "startStmts", new ParseTreeNodeContainer(s2.getStartStatements()),
              "body", body);
        }
        return NONE;
      }
    },

    new Rule () {
      @Override
      @RuleDescription(
          name="foreach",
          synopsis="",
          reason="Filters out hidden properties ending in ___ in for loops",
          matches="for (@k in @o) @ss;",
          substitutes="")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = makeBindings();

        if (QuasiBuilder.match("for (var @k in @o) @ss;", node, bindings)) {
          bindings.put("k", new Reference((Identifier) bindings.get("k")));
        } else if (QuasiBuilder.match("for (@k in @o) @ss;", node, bindings)) {
          ExpressionStmt es = (ExpressionStmt) bindings.get("k");
          bindings.put("k", es.getExpression());
        } else {
          return NONE;
        }

        Identifier kTemp = scope.declareStartOfScopeTempVariable();
        ParseTreeNode kAssignment = substV(
            "@k = @kTempRef;",
            "k", bindings.get("k"),
            "kTempRef", new Reference(kTemp));
        kAssignment = expandAll(kAssignment, scope, mq);
        kAssignment = new ExpressionStmt((Expression) kAssignment);

        return substV(
            "for (@kTempStmt in @o) { " +
            "  if (@kTempRef.match(/___$/)) { " +
            "    continue; " +
            "  } " +
            "  @kAssignment;" +
            "  @ss;" +
            "}",
            "kTempStmt", new ExpressionStmt(new Reference(kTemp)),
            "kTempRef", new Reference(kTemp),
            "o", bindings.get("o"),
            "kAssignment", kAssignment,
            "ss", expandAll(bindings.get("ss"), scope, mq));
      }
    },

    new Rule () {
      @Override
      @RuleDescription(
          name="recurse",
          synopsis="Automatically recurse into some structures",
          reason="")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        return expandAll(node, scope, mq);
      }
    }
  };

  public InnocentCodeRewriter(boolean logging) {
    super(logging);
    addRules(innocentRules);
  }
}
