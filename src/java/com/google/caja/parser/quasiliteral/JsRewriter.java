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

import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.js.Expression;
import com.google.caja.parser.js.ExpressionStmt;
import com.google.caja.parser.js.FunctionConstructor;
import com.google.caja.parser.js.Identifier;
import com.google.caja.parser.js.Reference;
import com.google.caja.parser.js.StringLiteral;
import com.google.caja.plugin.ExpressionSanitizer;
import com.google.caja.plugin.ReservedNames;
import com.google.caja.plugin.SyntheticNodes;
import com.google.caja.util.Pair;
import com.google.caja.reporting.MessageQueue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * This class sets up scoping rules and provides some basic definitions
 * for JavaScript rewriting.
 *
 * @author ihab.awad@gmail.com (Ihab Awad)
 */
public abstract class JsRewriter extends AbstractRewriter {
  protected JsRewriter(boolean logging) {
    super(logging);
  }

  protected ParseTreeNode getFunctionHeadDeclarations(
      Rule rule,
      Scope scope,
      MessageQueue mq) {
    List<ParseTreeNode> stmts = new ArrayList<ParseTreeNode>();

    if (scope.hasFreeArguments()) {
      stmts.add(substV(
          "var @la = ___.args(@ga);",
          "la", new Identifier(ReservedNames.LOCAL_ARGUMENTS),
          "ga", new Reference(new Identifier(ReservedNames.ARGUMENTS))));
    }
    if (scope.hasFreeThis()) {
      stmts.add(substV(
          "var @lt = @gt;",
          "lt", new Identifier(ReservedNames.LOCAL_THIS),
          "gt", new Reference(new Identifier(ReservedNames.THIS))));
    }

    return new ParseTreeNodeContainer(stmts);
  }

  protected Pair<ParseTreeNode, ParseTreeNode> reuse(
      String variableName,
      ParseTreeNode value,
      Rule rule,
      Scope scope,
      MessageQueue mq) {
    return new Pair<ParseTreeNode, ParseTreeNode>(
        new Reference(new Identifier(variableName)),
        substV(
            "var @ref = @rhs;",
            "ref", new Identifier(variableName),
            "rhs", expand(value, scope, mq)));
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
          "x" + i + "___",
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

  protected ParseTreeNode expandDef(
      ParseTreeNode symbol,
      ParseTreeNode value,
      Rule rule,
      Scope scope,
      MessageQueue mq) {
    if (!(symbol instanceof Reference)) {
      throw new RuntimeException("expandDef on non-Reference: " + symbol);
    }
    String name = getReferenceName(symbol);
    return scope.isGlobal(name) || !scope.isDefined(name) ?
        new ExpressionStmt((Expression)substV(
            "___OUTERS___.@s = @v",
            "s", symbol,
            "v", value)) :
        substV(
            "var @s = @v",
            "s", symbol.children().get(0),
            "v", value);
  }

  protected ParseTreeNode expandMember(
      ParseTreeNode fname,
      ParseTreeNode member,
      Rule rule,
      Scope scope,
      MessageQueue mq) {
    if (!scope.isDeclaredFunction(getReferenceName(fname))) {
      throw new RuntimeException("Internal: not statically a function name: " + fname);
    }

    Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();

    if (match("function(@ps*) { @bs*; }", member, bindings)) {
      Scope s2 = new Scope(scope, (FunctionConstructor)member);
      if (s2.hasFreeThis()) {
        return substV(
            "___.method(@fname, function(@ps*) {" +
            "  @fh*;" +
            "  @bs*;" +
            "});",
            "fname", fname,
            "ps",    bindings.get("ps"),
            "bs",    expand(bindings.get("bs"), s2, mq),
            "fh",    getFunctionHeadDeclarations(rule, s2, mq));
      }
    }

    return expand(member, scope, mq);
  }

  protected ParseTreeNode expandAllMembers(
      ParseTreeNode fname,
      ParseTreeNode members,
      Rule rule,
      Scope scope,
      MessageQueue mq) {
    List<ParseTreeNode> results = new ArrayList<ParseTreeNode>();
    for (ParseTreeNode member : members.children()) {
      results.add(expandMember(fname, member, rule, scope, mq));
    }
    return new ParseTreeNodeContainer(results);
  }

  protected ParseTreeNode expandMemberMap(
      ParseTreeNode fname,
      ParseTreeNode memberMap,
      Rule rule,
      Scope scope,
      MessageQueue mq) {
    if (!scope.isDeclaredFunction(getReferenceName(fname))) {
      throw new RuntimeException("Internal: not statically a function name: " + fname);
    }

    Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();

    if (match("({@keys*: @vals*})", memberMap, bindings)) {
      if (literalsEndWith(bindings.get("keys"), "__")) {
        mq.addMessage(
            JsRewriterMessageType.MEMBER_KEY_MAY_NOT_END_IN_DOUBLE_UNDERSCORE,
            rule, memberMap);
        return memberMap;
      }

      return substV(
          "({@keys*: @vals*})",
          "keys", bindings.get("keys"),
          "vals", expandAllMembers(fname, bindings.get("vals"), rule, scope, mq));
    }
    
    mq.addMessage(JsRewriterMessageType.MAP_EXPRESSION_EXPECTED,
        rule, memberMap);
    return memberMap;
  }

  protected boolean checkMapExpression(
      ParseTreeNode node,
      Rule rule,
      Scope scope,
      MessageQueue mq) {
    Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
    if (!match("({@keys*: @vals*})", node, bindings)) {
      mq.addMessage(
          JsRewriterMessageType.MAP_EXPRESSION_EXPECTED,
          rule, node);
      return false;
    } else if (literalsEndWith(bindings.get("keys"), "_")) {
      mq.addMessage(
          JsRewriterMessageType.KEY_MAY_NOT_END_IN_UNDERSCORE,
          rule, node);
      return false;
    }
    return true;
  }

  protected boolean isSynthetic(ParseTreeNode node) {
    return node.getAttributes().is(SyntheticNodes.SYNTHETIC);
  }
  
  protected String getReferenceName(ParseTreeNode ref) {
    return ((Reference)ref).getIdentifierName();
  }

  protected String getIdentifierName(ParseTreeNode id) {
    return ((Identifier)id).getValue();
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
}
