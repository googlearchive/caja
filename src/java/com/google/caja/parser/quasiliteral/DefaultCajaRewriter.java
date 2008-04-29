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
import com.google.caja.parser.ParseTreeNodes;
import com.google.caja.parser.AbstractParseTreeNode;
import com.google.caja.parser.js.AssignOperation;
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.BreakStmt;
import com.google.caja.parser.js.CaseStmt;
import com.google.caja.parser.js.Conditional;
import com.google.caja.parser.js.ContinueStmt;
import com.google.caja.parser.js.ControlOperation;
import com.google.caja.parser.js.Declaration;
import com.google.caja.parser.js.DefaultCaseStmt;
import com.google.caja.parser.js.Expression;
import com.google.caja.parser.js.ExpressionStmt;
import com.google.caja.parser.js.FunctionConstructor;
import com.google.caja.parser.js.FunctionDeclaration;
import com.google.caja.parser.js.Identifier;
import com.google.caja.parser.js.LabeledStmtWrapper;
import com.google.caja.parser.js.Literal;
import com.google.caja.parser.js.Loop;
import com.google.caja.parser.js.MultiDeclaration;
import com.google.caja.parser.js.Noop;
import com.google.caja.parser.js.Operation;
import com.google.caja.parser.js.Operator;
import com.google.caja.parser.js.Reference;
import com.google.caja.parser.js.ReturnStmt;
import com.google.caja.parser.js.SimpleOperation;
import com.google.caja.parser.js.SpecialOperation;
import com.google.caja.parser.js.StringLiteral;
import com.google.caja.parser.js.SwitchStmt;
import com.google.caja.parser.js.ThrowStmt;
import com.google.caja.parser.js.TryStmt;
import com.google.caja.parser.js.Statement;
import com.google.caja.parser.js.UndefinedLiteral;
import com.google.caja.parser.js.ArrayConstructor;
import com.google.caja.plugin.ReservedNames;
import com.google.caja.plugin.SyntheticNodes;
import static com.google.caja.plugin.SyntheticNodes.s;
import com.google.caja.util.Pair;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageQueue;

import static com.google.caja.parser.quasiliteral.QuasiBuilder.match;
import static com.google.caja.parser.quasiliteral.QuasiBuilder.subst;
import static com.google.caja.parser.quasiliteral.QuasiBuilder.substV;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Rewrites a JavaScript parse tree to comply with default Caja rules.
 *
 * @author ihab.awad@gmail.com (Ihab Awad)
 */
@RulesetDescription(
    name="Caja Transformation Rules",
    synopsis="Default set of transformations used by Caja"
  )
public class DefaultCajaRewriter extends Rewriter {
  final public Rule[] cajaRules = {
    new Rule () {
      @Override
      @RuleDescription(
          name="block",
          synopsis="",
          reason="")
      public ParseTreeNode fire(
          ParseTreeNode node, Scope scope, MessageQueue mq) {
        if (node instanceof Block) {
          List<ParseTreeNode> expanded = new ArrayList<ParseTreeNode>();
          Scope s2 = (scope == null) ?
              Scope.fromProgram((Block)node, mq) :
              Scope.fromPlainBlock(scope, (Block)node);
          for (ParseTreeNode c : node.children()) {
            expanded.add(expand(c, s2, mq));
          }
          // TODO(ihab.awad): Refactor to use an array of temporaries, which is set to null
          // at the bottom of the block. This would be wrapped in a try/finally to be sure
          // the array is always nulled out.
          return substV(
              "@startStmts*;" +
              "@expanded*;",
              "startStmts", new ParseTreeNodeContainer(s2.getStartStatements()),
              "expanded", new ParseTreeNodeContainer(expanded));
        }
        return NONE;
      }
    },

    ////////////////////////////////////////////////////////////////////////
    // Do nothing if the node is already the result of some translation
    ////////////////////////////////////////////////////////////////////////

    new Rule () {
      @Override
      @RuleDescription(
          name="synthetic",
          synopsis="Pass through synthetic nodes",
          reason="")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        if (isSynthetic(node)) {
          return expandAll(node, scope, mq);
        }
        return NONE;
      }
    },

    ////////////////////////////////////////////////////////////////////////
    // with - disallow the 'with' construct
    ////////////////////////////////////////////////////////////////////////

    new Rule () {
      @Override
      @RuleDescription(
          name="with",
          synopsis="Throw an error if a `with` block is found",
          reason="`with` violates the assumptions that Scope makes and makes it very"
            + "hard to write a Scope that works."
            + "http://yuiblog.com/blog/2006/04/11/with-statement-considered-harmful/"
            + "briefly touches on why `with` is bad for programmers."
            + "For reviewers -- matching of references with declarations can only"
            + "be done at runtime."
            + "All other secure JS subsets that I know of (ADSafe Jacaranda & FBJS)"
            + "also disallow `with`.",
          matches="with (@scope) @body;",
          substitutes="")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("with (@scope) @body;", node, bindings)) {
          mq.addMessage(
              RewriterMessageType.WITH_BLOCKS_NOT_ALLOWED,
              node.getFilePosition());
          return node;
        }
        return NONE;
      }
    },

    ////////////////////////////////////////////////////////////////////////
    // foreach - "for ... in" loops
    ////////////////////////////////////////////////////////////////////////

    new Rule () {
      @Override
      @RuleDescription(
          name="foreach",
          synopsis="",
          reason="",
          matches="for (var @k in @o) @ss;",
          substitutes="")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        boolean isDecl;

        if (match("for (var @k in @o) @ss;", node, bindings)) {
          isDecl = true;
          bindings.put("k", new Reference((Identifier)bindings.get("k")));
        } else if (match("for (@k in @o) @ss;", node, bindings)) {
          isDecl = false;
          ExpressionStmt es = (ExpressionStmt)bindings.get("k");
          bindings.put("k", es.getExpression());
        } else {
          return NONE;
        }

        List<Statement> declsList = new ArrayList<Statement>();

        Identifier oTemp = scope.declareStartOfScopeTempVariable();
        declsList.add(s(new ExpressionStmt((Expression)substV(
            "@oTemp = @o;",
            "oTemp", s(new Reference(oTemp)),
            "o", expand(bindings.get("o"), scope, mq)))));

        Identifier kTemp = scope.declareStartOfScopeTempVariable();

        if (isDecl) {
          String kName = ((Reference)bindings.get("k")).getIdentifierName();
          if (!scope.isGlobal(kName)) {
            scope.addStartOfScopeStatement((Statement)substV(
                "var @k;",
                "k", s(new Identifier(kName))));
          }
        }

        ParseTreeNode kAssignment = substV(
            "@k = @kTempRef;",
            "k", bindings.get("k"),
            "kTempRef", s(new Reference(kTemp)));
        kAssignment.getAttributes().remove(SyntheticNodes.SYNTHETIC);
        kAssignment = expand(kAssignment, scope, mq);
        kAssignment = s(new ExpressionStmt((Expression)kAssignment));

        boolean isThis = ReservedNames.THIS.equals(bindings.get("o").children().get(0).getValue());
        String canEnumName = isThis && !scope.isGlobal() ? "canEnumProp" : "canEnumPub";
        Reference canEnum = new Reference(new Identifier(canEnumName));

        return substV(
            "@decls*;" +
            "for (@kTempStmt in @oTempRef) {" +
            "  if (___.@canEnum(@oTempRef, @kTempRef)) {" +
            "    @kAssignment;" +
            "    @ss;" +
            "  }" +
            "}",
            "canEnum", canEnum,
            "decls", new ParseTreeNodeContainer(declsList),
            "oTempRef", s(new Reference(oTemp)),
            "kTempRef", s(new Reference(kTemp)),
            "kTempStmt", s(new ExpressionStmt(s(new Reference(kTemp)))),
            "kAssignment", kAssignment,
            "ss", expand(bindings.get("ss"), scope, mq));
      }
    },

    ////////////////////////////////////////////////////////////////////////
    // try - try/catch/finally constructs
    ////////////////////////////////////////////////////////////////////////

    new Rule () {
      @Override
      @RuleDescription(
          name="tryCatch",
          synopsis="",
          reason="",
          matches="try { @s0*; } catch (@x) { @s1*; }",
          substitutes="")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("try { @s0*; } catch (@x) { @s1*; }", node, bindings)) {
          TryStmt t = (TryStmt)node;
          return substV(
            "try {" +
            "  @s0*;" +
            "} catch (ex___) {" +
            "  try {" +
            "    throw ___.tameException(ex___); " +
            "  } catch (@x) {" +
            "    @s1*;" +
             "  }" +
            "}",
            "s0",  expandAll(bindings.get("s0"), scope, mq),
            "s1",  expandAll(bindings.get("s1"),
                             Scope.fromCatchStmt(scope, t.getCatchClause()), mq),
            "x", bindings.get("x"));
        }
        return NONE;
      }
    },

    new Rule () {
      @Override
      @RuleDescription(
          name="tryCatchFinally",
          synopsis="",
          reason="",
          matches="try { @s0*; } catch (@x) { @s1*; } finally { @s2*; }",
          substitutes="")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("try { @s0*; } catch (@x) { @s1*; } finally { @s2*; }", node, bindings)) {
          TryStmt t = (TryStmt)node;
          return substV(
            "try {" +
            "  @s0*;" +
            "} catch (ex___) {" +
            "  try {" +
            "    throw ___.tameException(ex___);" +
            "  } catch (@x) {" +
            "    @s1*;" +
            "  }" +
            "} finally {" +
            "  @s2*;" +
            "}",
            "s0",  expandAll(bindings.get("s0"), scope, mq),
            "s1",  expandAll(bindings.get("s1"),
                             Scope.fromCatchStmt(scope, t.getCatchClause()), mq),
            "s2",  expandAll(bindings.get("s2"), scope, mq),
            "x", bindings.get("x"));
        }
        return NONE;
      }
    },

    new Rule () {
      @Override
      @RuleDescription(
          name="tryFinally",
          synopsis="",
          reason="",
          matches="try { @s0*; } finally { @s1*; }",
          substitutes="")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("try { @s0*; } finally { @s1*; }", node, bindings)) {
          return substV(
            "try { @s0*; } finally { @s1*; }",
            "s0",  expandAll(bindings.get("s0"), scope, mq),
            "s1",  expandAll(bindings.get("s1"), scope, mq));
        }
        return NONE;
      }
    },

    ////////////////////////////////////////////////////////////////////////
    // variable - variable name handling
    ////////////////////////////////////////////////////////////////////////

    new Rule () {
      @Override
      @RuleDescription(
          name="varArgs",
          synopsis="",
          reason="")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match(ReservedNames.ARGUMENTS, node, bindings)) {
          return subst(ReservedNames.LOCAL_ARGUMENTS, bindings);
        }
        return NONE;
      }
    },

    new Rule () {
      @Override
      @RuleDescription(
          name="varThis",
          synopsis="",
          reason="")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match(ReservedNames.THIS, node, bindings)) {
          return scope.isGlobal() ?
              subst("___OUTERS___", bindings) :
              subst(ReservedNames.LOCAL_THIS, bindings);
        }
        return NONE;
      }
    },

    new Rule () {
      @Override
      @RuleDescription(
          name="varBadSuffix",
          synopsis="Throw an error if a variable with `__` suffix is found",
          reason="Caja reserves the `__` suffix for internal use",
          matches="@x__",
          substitutes="")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("@x__", node, bindings)) {
          mq.addMessage(
              RewriterMessageType.VARIABLES_CANNOT_END_IN_DOUBLE_UNDERSCORE,
              node.getFilePosition(), this, node);
          return node;
        }
        return NONE;
      }
    },

    new Rule () {
      @Override
      @RuleDescription(
          name="varBadSuffixDeclaration",
          synopsis="Throw an error if a variable with `__` suffix is found",
          reason="Caja reserves the `__` suffix for internal use")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        if (node instanceof Declaration &&
            ((Declaration)node).getIdentifier().getValue().endsWith("__")) {
          mq.addMessage(
              RewriterMessageType.VARIABLES_CANNOT_END_IN_DOUBLE_UNDERSCORE,
              node.getFilePosition(), this, node);
          return node;
        }
        return NONE;
      }
    },

    new Rule () {
      @Override
      @RuleDescription(
          name="varBadGlobalSuffix",
          synopsis="Throw an error if a global variable with `_` suffix is found",
          reason="Caja defines variable with a `_` ")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("@x_", node, bindings)) {
          String symbol = ((Identifier)bindings.get("x")).getValue() + "_";
          if (scope.isGlobal(symbol)) {
            mq.addMessage(
                RewriterMessageType.GLOBALS_CANNOT_END_IN_UNDERSCORE,
                node.getFilePosition(), this, node);
            return node;
          }
        }
        return NONE;
      }
    },

    new Rule () {
      @Override
      @RuleDescription(
          name="varFuncFreeze",
          synopsis="",
          reason="")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("@x", node, bindings) &&
            bindings.get("x") instanceof Reference) {
          String name = getReferenceName(bindings.get("x"));
          if (scope.isFunction(name)) {
            return substV(
                "___.primFreeze(@x)",
                "x", expandReferenceToOuters(bindings.get("x"), scope, mq));
          }
        }
        return NONE;
      }
    },

    new Rule () {
      @Override
      @RuleDescription(
          name="varGlobal",
          synopsis="",
          reason="")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("@x", node, bindings) &&
            bindings.get("x") instanceof Reference) {
          return expandReferenceToOuters(bindings.get("x"), scope, mq);
        }
        return NONE;
      }
    },

    new Rule () {
      @Override
      @RuleDescription(
          name="varDefault",
          synopsis="",
          reason="")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("@x", node, bindings) &&
            bindings.get("x") instanceof Reference) {
          return bindings.get("x");
        }
        return NONE;
      }
    },

    ////////////////////////////////////////////////////////////////////////
    // read - reading values
    ////////////////////////////////////////////////////////////////////////

    new Rule () {
      @Override
      @RuleDescription(
          name="readBadSuffix",
          synopsis="Throw an error if a property has `__` suffix is found",
          reason="Caja reserves the `__` suffix for internal use")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("@x.@y__", node, bindings)) {
          mq.addMessage(
              RewriterMessageType.PROPERTIES_CANNOT_END_IN_DOUBLE_UNDERSCORE,
              node.getFilePosition(), this, node);
          return node;
        }
        return NONE;
      }
    },

    new Rule () {
      @Override
      @RuleDescription(
          name="readGlobalViaThis",
          synopsis="",
          reason="")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("this.@p", node, bindings) && scope.isGlobal()) {
          String xName = getReferenceName(bindings.get("p"));
          return substV(
              "___OUTERS___.@xCanRead ? ___OUTERS___.@x : ___.readPub(___OUTERS___, @xName);",
              "x", bindings.get("p"),
              "xCanRead", new Reference(new Identifier(xName + "_canRead___")),
              "xName", new StringLiteral(StringLiteral.toQuotedValue(xName)));
        }
        return NONE;
      }
    },

    new Rule () {
      @Override
      @RuleDescription(
          name="readInternal",
          synopsis="",
          reason="")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("this.@p", node, bindings)) {
          Reference p = (Reference) bindings.get("p");
          String propertyName = p.getIdentifierName();
          return substV(
            "t___.@fp ? t___.@p : ___.readProp(t___, @rp)",
            "p",  p,
            "fp", new Reference(new Identifier(propertyName + "_canRead___")),
            "rp", toStringLiteral(p));
        }
        return NONE;
      }
    },

    new Rule () {
      @Override
      @RuleDescription(
          name="readBadInternal",
          synopsis="Throw an error if a global variable with `_` suffix is found",
          reason="Caja defines variable with a `_` suffix as private")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("@x.@y_", node, bindings)) {
          mq.addMessage(
              RewriterMessageType.PUBLIC_PROPERTIES_CANNOT_END_IN_UNDERSCORE,
              node.getFilePosition(), this, node);
          return node;
        }
        return NONE;
      }
    },

    new Rule () {
      @Override
      @RuleDescription(
          name="readPublic",
          synopsis="",
          reason="")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("@o.@p", node, bindings)) {
          Reference p = (Reference) bindings.get("p");
          String propertyName = p.getIdentifierName();
          return substV(
              "@ref = @o," +
              "  @ref.@fp ?" +
              "  @ref.@p :" +
              "  ___.readPub(@ref, @rp)",
              "ref", s(new Reference(scope.declareStartOfScopeTempVariable())),
              "o", expand(bindings.get("o"), scope, mq),
              "p",  p,
              "fp", new Reference(new Identifier(propertyName + "_canRead___")),
              "rp", toStringLiteral(p));
        }
        return NONE;
      }
    },

    new Rule () {
      @Override
      @RuleDescription(
          name="readIndexGlobal",
          synopsis="",
          reason="")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("this[@s]", node, bindings) && scope.isGlobal()) {
          return substV(
              "___.readPub(___OUTERS___, @s)",
              "s", expand(bindings.get("s"), scope, mq));
        }
        return NONE;
      }
    },

    new Rule () {
      @Override
      @RuleDescription(
          name="readIndexInternal",
          synopsis="",
          reason="")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("this[@s]", node, bindings)) {
          return substV(
              "___.readProp(t___, @s)",
              "s", expand(bindings.get("s"), scope, mq));
        }
        return NONE;
      }
    },

    new Rule () {
      @Override
      @RuleDescription(
          name="readIndexPublic",
          synopsis="",
          reason="")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("@o[@s]", node, bindings)) {
          return substV(
              "___.readPub(@o, @s)",
              "o", expand(bindings.get("o"), scope, mq),
              "s", expand(bindings.get("s"), scope, mq));
        }
        return NONE;
      }
    },

    ////////////////////////////////////////////////////////////////////////
    // set - assignments
    ////////////////////////////////////////////////////////////////////////

    new Rule () {
      @Override
      @RuleDescription(
          name="setGlobal",
          synopsis="",
          reason="")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("@p = @r", node, bindings) &&
            bindings.get("p") instanceof Reference) {
          Reference p = (Reference) bindings.get("p");
          String propertyName = getReferenceName(p);
          if (scope.isGlobal(propertyName) && !ReservedNames.THIS.equals(propertyName)) {
            return substV(
                "@ref = @r," +
                "___OUTERS___.@fp ?" +
                "  (___OUTERS___.@p = @ref) :" +
                "  ___.setPub(___OUTERS___, @rp, @ref);",
                "ref", s(new Reference(scope.declareStartOfScopeTempVariable())),
                "r",  expand(bindings.get("r"), scope, mq),
                "p",  p,
                "fp", new Reference(new Identifier(propertyName + "_canSet___")),
                "rp", toStringLiteral(p));
          }
        }
        return NONE;
      }
    },

    new Rule () {
      @Override
      @RuleDescription(
          name="setBadThis",
          synopsis="Throw an error if an expression assigns to `this`",
          reason="")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("this = @z", node, bindings)) {
          mq.addMessage(
              RewriterMessageType.CANNOT_ASSIGN_TO_THIS,
              node.getFilePosition(), this, node);
          return node;
        }
        return NONE;
      }
    },

    new Rule () {
      @Override
      @RuleDescription(
          name="setBadSuffix",
          synopsis="Throw an error if a property with `__` suffix is found",
          reason="Caja reserves the `__` suffix for internal use")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("@x.@y__ = @z", node, bindings)) {
          mq.addMessage(
              RewriterMessageType.PROPERTIES_CANNOT_END_IN_DOUBLE_UNDERSCORE,
              node.getFilePosition(), this, node);
          return node;
        }
        return NONE;
      }
    },

    new Rule () {
      @Override
      @RuleDescription(
          name="setGlobalViaThis",
          synopsis="",
          reason="")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("this.@p = @r", node, bindings) && scope.isGlobal()) {
          Reference p = (Reference) bindings.get("p");
          String propertyName = p.getIdentifierName();
          return substV(
              "@ref = @r," +
              "___OUTERS___.@fp ?" +
              "  (___OUTERS___.@p = @ref) :" +
              "  ___.setPub(___OUTERS___, @rp, @ref);",
              "ref", s(new Reference(scope.declareStartOfScopeTempVariable())),
              "r",  expand(bindings.get("r"), scope, mq),
              "p",  p,
              "fp", new Reference(new Identifier(propertyName + "_canSet___")),
              "rp", toStringLiteral(p));
        }
        return NONE;
      }
    },

    new Rule () {
      @Override
      @RuleDescription(
          name="setInternal",
          synopsis="",
          reason="")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("this.@p = @r", node, bindings)) {
          String propertyName = ((Reference)bindings.get("p")).getIdentifierName();
          Reference target = new Reference(new Identifier(
              scope.isGlobal() ? ReservedNames.OUTERS : ReservedNames.LOCAL_THIS));
          return substV(
              "@ref = @r," +
              "@target.@fp ?" +
              "  (@target.@p = @ref) :" +
              "  ___.setProp(@target, @rp, @ref);",
              "ref", s(new Reference(scope.declareStartOfScopeTempVariable())),
              "r",  expand(bindings.get("r"), scope, mq),
              "p",  bindings.get("p"),
              "fp", new Reference(new Identifier(propertyName + "_canSet___")),
              "rp", toStringLiteral(bindings.get("p")),
              "target", target);
        }
        return NONE;
      }
    },

    new Rule () {
      @Override
      @RuleDescription(
          name="setMember",
          synopsis="",
          reason="")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();

        // BUG TODO(erights,ihab): We must only recognize (and thus allow) this
        // expression when it is evaluated for effects only, not for value.
        // Currently, since we have no such test, the translated expression will
        // safely evaluate to <tt>undefined</tt>, but this behavior is not within
        // a fail-stop subset of JavaScript.
        if (match("@clazz.prototype.@p = @m;", node, bindings)) {
          ParseTreeNode clazz = bindings.get("clazz");
          if (clazz instanceof Reference) {
            String className = getReferenceName(clazz);
            if (scope.isDeclaredFunction(className)) {
              Reference p = (Reference) bindings.get("p");
              if (!"constructor".equals(getReferenceName(p))) {
                // Make sure @p and @clazz are mentionable.
                expand(p, scope, mq);
                expand(clazz, scope, mq);
                return substV(
                    "___.setMember(@clazz, @rp, @m);",
                    "clazz", expandReferenceToOuters(clazz, scope, mq),  // Don't expand so we don't freeze.
                    "m", expandMember(clazz, bindings.get("m"), this, scope, mq),
                    "rp", toStringLiteral(p));
              }
            }
          } else {
            // TODO(mikesamuel): make constructors first class for the purpose
            // of defining members.
          }
        }
        return NONE;
      }
    },

    new Rule () {
      @Override
      @RuleDescription(
          name="setBadInternal",
          synopsis="Throw an error if a global variable with `_` suffix is found",
          reason="Caja defines variable with a `_` suffix as private")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("@x.@y_ = @z", node, bindings)) {
          mq.addMessage(
              RewriterMessageType.PUBLIC_PROPERTIES_CANNOT_END_IN_UNDERSCORE,
              node.getFilePosition(), this, node);
          return node;
        }
        return NONE;
      }
    },

    new Rule () {
      @Override
      @RuleDescription(
          name="setStatic",
          synopsis="",
          reason="")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("@fname.@p = @r", node, bindings) &&
            bindings.get("fname") instanceof Reference &&
            scope.isFunction(getReferenceName(bindings.get("fname")))) {
          Reference p = (Reference) bindings.get("p");
          String propertyName = p.getIdentifierName();
          if (!"Super".equals(propertyName)) {
            return substV(
                "___.setPub(@fname, @rp, @r)",
                "fname", bindings.get("fname"),
                "rp", toStringLiteral(p),
                "r", expand(bindings.get("r"), scope, mq));
          }
        }
        return NONE;
      }
    },

    new Rule () {
      @Override
      @RuleDescription(
          name="setPublic",
          synopsis="",
          reason="")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("@o.@p = @r", node, bindings)) {
          String propertyName = ((Reference)bindings.get("p")).getIdentifierName();
          return substV(
              "@tmpO = @expandO," +
              "@tmpR = @expandR," +
              "@tmpO.@pCanSet ?" +
              "    (@tmpO.@p = @tmpR) :" +
              "    ___.setPub(@tmpO, @pName, @tmpR);",
              "tmpO", s(new Reference(scope.declareStartOfScopeTempVariable())),
              "tmpR", s(new Reference(scope.declareStartOfScopeTempVariable())),
              "expandO", expand(bindings.get("o"), scope, mq),
              "expandR", expand(bindings.get("r"), scope, mq),
              "pCanSet", new Reference(new Identifier(propertyName + "_canSet___")),
              "p", bindings.get("p"),
              "pName", toStringLiteral(bindings.get("p")));
        }
        return NONE;
      }
    },

    new Rule () {
      @Override
      @RuleDescription(
          name="setIndexInternal",
          synopsis="",
          reason="")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("this[@s] = @r", node, bindings)) {
          return substV(
              "___.setProp(t___, @s, @r)",
              "s", expand(bindings.get("s"), scope, mq),
              "r", expand(bindings.get("r"), scope, mq));
        }
        return NONE;
      }
    },

    new Rule () {
      @Override
      @RuleDescription(
          name="setIndexPublic",
          synopsis="",
          reason="")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("@o[@s] = @r", node, bindings)) {
          return substV(
              "___.setPub(@o, @s, @r)",
              "o", expand(bindings.get("o"), scope, mq),
              "s", expand(bindings.get("s"), scope, mq),
              "r", expand(bindings.get("r"), scope, mq));
        }
        return NONE;
      }
    },

    new Rule () {
      @Override
      @RuleDescription(
          name="setBadInitialize",
          synopsis="Throw an error if a variable with `__` suffix is found",
          reason="Caja reserves the `__` suffix for internal use")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("var @v__ = @r", node, bindings)) {
          mq.addMessage(
              RewriterMessageType.VARIABLES_CANNOT_END_IN_DOUBLE_UNDERSCORE,
              node.getFilePosition(), this, node);
          return node;
        }
        return NONE;
      }
    },

    new Rule () {
      @Override
      @RuleDescription(
          name="setInitialize",
          synopsis="",
          reason="")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("var @v = @r", node, bindings) &&
            !scope.isFunction(getIdentifierName(bindings.get("v")))) {
          return expandDef(
              new Reference((Identifier)bindings.get("v")),
              expand(bindings.get("r"), scope, mq),
              this,
              scope,
              mq);
        }
        return NONE;
      }
    },

    new Rule () {
      @Override
      @RuleDescription(
          name="setBadDeclare",
          synopsis="Throw an error if a variable with `__` suffix is found",
          reason="Caja reserves the `__` suffix for internal use")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("var @v__", node, bindings)) {
          mq.addMessage(
              RewriterMessageType.VARIABLES_CANNOT_END_IN_DOUBLE_UNDERSCORE,
              node.getFilePosition(), this, node);
          return node;
        }
        return NONE;
      }
    },

    new Rule () {
      @Override
      @RuleDescription(
          name="setDeclare",
          synopsis="",
          reason="")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("var @v", node, bindings) &&
            !scope.isFunction(getIdentifierName(bindings.get("v")))) {
          if (!scope.isGlobal()) {
            return node;
          } else {
            ParseTreeNode expr = substV(
                "___.setPub(___OUTERS___, @vName, ___.readPub(___OUTERS___, @vName));",
                "vName", toStringLiteral(bindings.get("v")));
            // Must now wrap the Expression in something Statement-like since
            // that is what the enclosing context expects:
            return ParseTreeNodes.newNodeInstance(
                ExpressionStmt.class,
                null,
                Arrays.asList(new ParseTreeNode[] { expr }));
          }
        }
        return NONE;
      }
    },

    // TODO(erights): Need a general way to expand lValues
    new Rule () {
      @Override
      @RuleDescription(
          name="setVar",
          synopsis="",
          reason="")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("@v = @r", node, bindings)) {
          if (bindings.get("v") instanceof Reference) {
            String vName = getReferenceName(bindings.get("v"));
            if (!scope.isFunction(vName)) {
              return substV(
                  "@v = @r",
                  "v", bindings.get("v"),
                  "r", expand(bindings.get("r"), scope, mq));
            }
          }
        }
        return NONE;
      }
    },

    // TODO(erights): Need a general way to expand readModifyWrite lValues.
    // For now, we're just picking off a few common special cases as they
    // come up.

    new Rule () {
      @Override
      @RuleDescription(
          name="setReadModifyWriteLocalVar",
          synopsis="",
          reason="")
      // Handle x += 3 and similar ops by rewriting them using the assignment
      // delegate, "x += y" => "x = x + y", with deconstructReadAssignOperand
      // assuring that x is evaluated at most once where that matters.
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        if (node instanceof AssignOperation) {
          AssignOperation aNode = (AssignOperation)node;
          Operator op = aNode.getOperator();
          if (op.getAssignmentDelegate() == null) { return NONE; }

          ReadAssignOperands ops = deconstructReadAssignOperand(
              aNode.children().get(0), scope, mq);
          if (ops == null) { return node; }  // Error deconstructing

          // For x += 3, rhs is (x + 3)
          Operation rhs = Operation.create(
              op.getAssignmentDelegate(), ops.getRValue(),
              (Expression) expand(aNode.children().get(1), scope, mq));
          rhs.setFilePosition(aNode.getFilePosition());
          Expression assignment = ops.makeAssignment(rhs);
          ((AbstractParseTreeNode<?>) assignment)
              .setFilePosition(aNode.getFilePosition());
          if (ops.getTemporaries().isEmpty()) {
            return assignment;
          } else {
            return substV(
                "  @tmps,"
                + "@assign",
                "tmps", newCommaOperation(ops.getTemporaries()),
                "assign", assignment);
          }
        }
        return NONE;
      }
    },

    new Rule () {
      @Override
      @RuleDescription(
          name="setIncrDecr",
          synopsis="",
          reason="")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        if (!(node instanceof AssignOperation)) { return NONE; }
        AssignOperation op = (AssignOperation) node;
        Expression v = op.children().get(0);
        ReadAssignOperands ops = deconstructReadAssignOperand(v, scope, mq);
        if (ops == null) { return node; }

        // TODO(mikesamuel): Figure out when post increments are being
        // used without use of the resulting value and switch them to
        // pre-increments.
        switch (op.getOperator()) {
          case POST_INCREMENT:
            if (ops.isSimpleLValue()) {
              return substV("@v ++", "v", ops.getRValue());
            } else {
              Reference tmpVal = s(new Reference(scope.declareStartOfScopeTempVariable()));
              Expression assign = ops.makeAssignment((Expression)substV(
                  "@tmpVal + 1",
                  "tmpVal", tmpVal));
              return substV(
                  "  @tmps,"
                  + "@tmpVal = @rvalue - 0,"  // Coerce to a number.
                  + "@assign,"  // Assign value.
                  + "@tmpVal",
                  "tmps", newCommaOperation(ops.getTemporaries()),
                  "tmpVal", tmpVal,
                  "rvalue", ops.getRValue(),
                  "assign", assign);
            }
          case PRE_INCREMENT:
            // We subtract -1 instead of adding 1 since the - operator coerces
            // to a number in the same way the ++ operator does.
            if (ops.isSimpleLValue()) {
              return substV("++@v", "v", ops.getRValue());
            } else if (ops.getTemporaries().isEmpty()) {
              return ops.makeAssignment((Expression)
                  substV("@rvalue - -1", "rvalue", ops.getRValue()));
            } else {
              return substV(
                  "  @tmps,"
                  + "@assign",
                  "tmps", newCommaOperation(ops.getTemporaries()),
                  "assign", ops.makeAssignment((Expression)
                      substV("@rvalue - -1", "rvalue", ops.getRValue())));
            }
          case POST_DECREMENT:
            if (ops.isSimpleLValue()) {
              return substV("@v--", "v", ops.getRValue());
            } else {
              Reference tmpVal = s(new Reference(scope.declareStartOfScopeTempVariable()));
              Expression assign = ops.makeAssignment((Expression)substV(
                  "@tmpVal - 1",
                  "tmpVal", tmpVal));
              return substV(
                  "  @tmps,"
                  + "@tmpVal = @rvalue - 0,"  // Coerce to a number.
                  + "@assign,"  // Assign value.
                  + "@tmpVal;",
                  "tmps", newCommaOperation(ops.getTemporaries()),
                  "tmpVal", tmpVal,
                  "rvalue", ops.getRValue(),
                  "assign", assign);
            }
          case PRE_DECREMENT:
            if (ops.isSimpleLValue()) {
              return substV("--@v", "v", ops.getRValue());
            } else if (ops.getTemporaries().isEmpty()) {
              return ops.makeAssignment((Expression)
                  substV("@rvalue - 1", "rvalue", ops.getRValue()));
            } else {
              return substV(
                  "  @tmps,"
                  + "@assign",
                  "tmps", newCommaOperation(ops.getTemporaries()),
                  "assign", ops.makeAssignment((Expression)
                      substV("@rvalue - 1", "rvalue", ops.getRValue())));
            }
          default:
            return NONE;
        }
      }
    },

    ////////////////////////////////////////////////////////////////////////
    // new - new object creation
    ////////////////////////////////////////////////////////////////////////

    new Rule () {
      @Override
      @RuleDescription(
          name="newCalllessCtor",
          synopsis="",
          reason="")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("new @ctor", node, bindings)) {
          return expand(
              Operation.create(Operator.FUNCTION_CALL, (Expression) node),
              scope, mq);
        }
        return NONE;
      }
    },

    new Rule () {
      @Override
      @RuleDescription(
          name="newCtor",
          synopsis="",
          reason="")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("new @ctor(@as*)", node, bindings)) {
          ParseTreeNode ctor = bindings.get("ctor");
          return substV(
              "new (___.asCtor(@ctor))(@as*)",
              "ctor", expand(ctor, scope, mq),
              "as", expandAll(bindings.get("as"), scope, mq));
        }
        return NONE;
      }
    },

    ////////////////////////////////////////////////////////////////////////
    // delete - property deletion
    ////////////////////////////////////////////////////////////////////////

    new Rule () {
      @Override
      @RuleDescription(
          name="deleteProp",
          synopsis="",
          reason="")
      public ParseTreeNode fire(
          ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings
            = new LinkedHashMap<String, ParseTreeNode>();
        if (match("delete this[@k]", node, bindings)) {
          ParseTreeNode thisNode = node.children().get(0).children().get(0);
          return substV(
              "___.deleteProp(@this, @k)",
              "this", expand(thisNode, scope, mq),
              "k", expand(bindings.get("k"), scope, mq)
              );
        } else if (match("delete this.@k", node, bindings)) {
          ParseTreeNode thisNode = node.children().get(0).children().get(0);
          Reference k = (Reference) bindings.get("k");
          if (k.getIdentifierName().endsWith("__")) {
            mq.addMessage(
                RewriterMessageType.PROPERTIES_CANNOT_END_IN_DOUBLE_UNDERSCORE,
                node.getFilePosition(), this, node);
          }
          return substV(
              "___.deleteProp(@this, @kname)",
              "this", expand(thisNode, scope, mq),
              "kname", toStringLiteral(k)
              );
        }
        return NONE;
      }
    },

    new Rule () {
      @Override
      @RuleDescription(
          name="deletePub",
          synopsis="",
          reason="")
      public ParseTreeNode fire(
          ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings
            = new LinkedHashMap<String, ParseTreeNode>();
        if (match("delete @o[@k]", node, bindings)) {
          return substV(
              "___.deletePub(@o, @k)",
              "o", expand(bindings.get("o"), scope, mq),
              "k", expand(bindings.get("k"), scope, mq));
        } else if (match("delete @o.@k", node, bindings)) {
          Reference k = (Reference) bindings.get("k");
          expand(k, scope, mq);
          return substV(
              "___.deletePub(@o, @ks)",
              "o", expand(bindings.get("o"), scope, mq),
              "ks", toStringLiteral(k));
        }
        return NONE;
      }
    },

    new Rule () {
      @Override
      @RuleDescription(
          name="deleteGlobal",
          synopsis="",
          reason="")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings
            = new LinkedHashMap<String, ParseTreeNode>();
        if (match("delete @v", node, bindings)) {
          ParseTreeNode v = bindings.get("v");
          if (v instanceof Reference) {
            expand(v, scope, mq);  // Make sure v is mentionable
            return substV(
                "___.deletePub(___OUTERS___, @vname)",
                "vname", toStringLiteral(v));
          }
        }
        return NONE;
      }
    },

    new Rule () {
      @Override
      @RuleDescription(
          name="deleteNonLvalue",
          synopsis="",
          reason="")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings
            = new LinkedHashMap<String, ParseTreeNode>();
        if (match("delete @v", node, bindings)) {
          mq.addMessage(
              RewriterMessageType.NOT_DELETABLE, node.getFilePosition());
          return node;
        }
        return NONE;
      }
    },

    ////////////////////////////////////////////////////////////////////////
    // call - function calls
    ////////////////////////////////////////////////////////////////////////

    new Rule () {
      @Override
      @RuleDescription(
          name="callBadSuffix",
          synopsis="Throw an error if a selector with `__` suffix is found",
          reason="Caja reserves the `__` suffix for internal use")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("@o.@s__(@as*)", node, bindings)) {
          mq.addMessage(
              RewriterMessageType.SELECTORS_CANNOT_END_IN_DOUBLE_UNDERSCORE,
              node.getFilePosition(), this, node);
          return node;
        }
        return NONE;
      }
    },

    new Rule () {
      @Override
      @RuleDescription(
          name="callGlobalViaThis",
          synopsis="",
          reason="")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("this.@m(@as*)", node, bindings) && scope.isGlobal()) {
          Pair<ParseTreeNode, ParseTreeNode> aliases =
              reuseAll(bindings.get("as"), this, scope, mq);
          Reference m = (Reference) bindings.get("m");
          String methodName = m.getIdentifierName();
          return substV(
              "@as," +
              "___OUTERS___.@fm ?" +
              "    ___OUTERS___.@m(@vs*) :" +
              "    ___.callPub(___OUTERS___, @rm, [@vs*])",
              "as", newCommaOperation(aliases.b.children()),
              "vs", aliases.a,
              "m",  m,
              "fm", new Reference(new Identifier(methodName + "_canCall___")),
              "rm", toStringLiteral(m));
        }
        return NONE;
      }
    },

    new Rule () {
      @Override
      @RuleDescription(
          name="callInternal",
          synopsis="",
          reason="")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("this.@m(@as*)", node, bindings)) {
          Pair<ParseTreeNode, ParseTreeNode> aliases =
              reuseAll(bindings.get("as"), this, scope, mq);
          Reference m = (Reference) bindings.get("m");
          String methodName = m.getIdentifierName();
          return substV(
              "@as," +
              "t___.@fm ? t___.@m(@vs*) : ___.callProp(t___, @rm, [@vs*])",
              "as", newCommaOperation(aliases.b.children()),
              "vs", aliases.a,
              "m",  bindings.get("m"),
              "fm", new Reference(new Identifier(methodName + "_canCall___")),
              "rm", toStringLiteral(m));
        }
        return NONE;
      }
    },

    new Rule () {
      @Override
      @RuleDescription(
          name="callBadInternal",
          synopsis="Throw an error if a public selector with `_` suffix is found",
          reason="Caja defines selectors with a `_` as private")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("@o.@s_(@as*)", node, bindings)) {
          mq.addMessage(
              RewriterMessageType.PUBLIC_SELECTORS_CANNOT_END_IN_UNDERSCORE,
              node.getFilePosition(), this, node);
          return node;
        }
        return NONE;
      }
    },

    new Rule () {
      @Override
      @RuleDescription(
          name="callCajaDef2",
          synopsis="",
          reason="")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("caja.def(@fname, @base)", node, bindings) &&
            scope.isFunction(getReferenceName(bindings.get("fname"))) &&
            scope.isFunction(getReferenceName(bindings.get("base")))) {
          return substV(
              "caja.def(@fname, @base)",
              "fname", expandReferenceToOuters(bindings.get("fname"), scope, mq),
              "base", expandReferenceToOuters(bindings.get("base"), scope, mq));
        }
        return NONE;
      }
    },

    new Rule () {
      @Override
      @RuleDescription(
          name="callCajaDef2Bad",
          synopsis="",
          reason="")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("caja.def(@fname, @base)", node, bindings)) {
          mq.addMessage(
              RewriterMessageType.CAJA_DEF_ON_NON_CTOR,
              node.getFilePosition(), this, node);
          return node;
        }
        return NONE;
      }
    },

    new Rule () {
      @Override
      @RuleDescription(
          name="callCajaDef3Plus",
          synopsis="",
          reason="")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("caja.def(@fname, @base, @mm, @ss?)", node, bindings) &&
            scope.isFunction(getReferenceName(bindings.get("fname"))) &&
            (bindings.get("base") instanceof UndefinedLiteral ||
             scope.isFunction(getReferenceName(bindings.get("base"))))) {
          if (!checkMapExpression(bindings.get("mm"), this, scope, mq)) {
            return node;
          }
          if (bindings.get("ss") != null &&
              !checkMapExpression(bindings.get("ss"), this, scope, mq)) {
            return node;
          }
          ParseTreeNode ss = bindings.get("ss") == null ? null :
              expandAll(bindings.get("ss"), scope, mq);
          return substV(
              "caja.def(@fname, @base, @mm, @ss?)",
              "fname", expandReferenceToOuters(bindings.get("fname"), scope, mq),
              "base", expandReferenceToOuters(bindings.get("base"), scope, mq),
              "mm", expandMemberMap(bindings.get("fname"), bindings.get("mm"), this, scope, mq),
              "ss", ss);
        }
        return NONE;
      }
    },

    new Rule () {
      @Override
      @RuleDescription(
          name="callCajaDef3PlusBad",
          synopsis="",
          reason="")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("caja.def(@fname, @base, @mm, @ss?)", node, bindings)) {
          mq.addMessage(
              RewriterMessageType.CAJA_DEF_ON_NON_CTOR,
              node.getFilePosition(), this, node);
          return node;
        }
        return NONE;
      }
    },

    new Rule () {
      @Override
      @RuleDescription(
          name="callPublic",
          synopsis="",
          reason="")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("@o.@m(@as*)", node, bindings)) {
          Pair<ParseTreeNode, ParseTreeNode> aliases =
              reuseAll(bindings.get("as"), this, scope, mq);
          Reference m = (Reference) bindings.get("m");
          String methodName = m.getIdentifierName();
          return substV(
              "@oTmp = @o," +
              "@as," +
              "@oTmp.@fm ? @oTmp.@m(@vs*) : ___.callPub(@oTmp, @rm, [@vs*]);",
              "oTmp", s(new Reference(scope.declareStartOfScopeTempVariable())),
              "o",  expand(bindings.get("o"), scope, mq),
              "as", newCommaOperation(aliases.b.children()),
              "vs", aliases.a,
              "m",  m,
              "fm", new Reference(new Identifier(methodName + "_canCall___")),
              "rm", toStringLiteral(m));
        }
        return NONE;
      }
    },

    new Rule () {
      @Override
      @RuleDescription(
          name="callIndexInternal",
          synopsis="",
          reason="")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("this[@s](@as*)", node, bindings)) {
          expandEntries(bindings, scope, mq);
          return subst(
              "___.callProp(t___, @s, [@as*])", bindings
          );
        }
        return NONE;
      }
    },

    new Rule () {
      @Override
      @RuleDescription(
          name="callIndexPublic",
          synopsis="",
          reason="")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("@o[@s](@as*)", node, bindings)) {
          expandEntries(bindings, scope, mq);
          return subst(
              "___.callPub(@o, @s, [@as*])", bindings
          );
        }
        return NONE;
      }
    },

    new Rule () {
      @Override
      @RuleDescription(
          name="callFunc",
          synopsis="",
          reason="")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("@f(@as*)", node, bindings)) {
          return substV(
              "___.asSimpleFunc(@f)(@as*)",
              "f", expand(bindings.get("f"), scope, mq),
              "as", expandAll(bindings.get("as"), scope, mq));
        }
        return NONE;
      }
    },

    ////////////////////////////////////////////////////////////////////////
    // function - function definitions
    ////////////////////////////////////////////////////////////////////////

    new Rule () {
      @Override
      @RuleDescription(
          name="funcAnonSimple",
          synopsis="",
          reason="")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        // Anonymous simple function constructor
        if (match("function(@ps*) { @bs*; }", node, bindings)) {
          Scope s2 = Scope.fromFunctionConstructor(scope, (FunctionConstructor)node);
          if (!s2.hasFreeThis()) {
            checkFormals(bindings.get("ps"), mq);
            return substV(
                "___.primFreeze(" +
                "  ___.simpleFunc(" +
                "    function(@ps*) {" +
                "      @fh*;" +
                "      @stmts*;" +
                "      @bs*;" +
                "}))",
                "ps", bindings.get("ps"),
                "bs", expand(bindings.get("bs"), s2, mq),
                "fh", getFunctionHeadDeclarations(this, s2, mq),
                "stmts", new ParseTreeNodeContainer(s2.getStartStatements()));
          }
        }
        return NONE;
      }
    },

    new Rule () {
      @Override
      @RuleDescription(
          name="funcNamedSimpleDecl",
          synopsis="",
          reason="")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        // Named simple function declaration
        if (node instanceof FunctionDeclaration &&
            match("function @f(@ps*) { @bs*; }", node.children().get(1), bindings)) {
          Scope s2 = Scope.fromFunctionConstructor(
              scope,
              (FunctionConstructor)node.children().get(1));
          if (!s2.hasFreeThis()) {
            checkFormals(bindings.get("ps"), mq);
            Identifier f = (Identifier)bindings.get("f");
            scope.declareStartOfScopeVariable(f);
            scope.addStartOfBlockStatement((Statement)expandDef(
                new Reference((Identifier)bindings.get("f")),
                substV(
                    "___.simpleFunc(" +
                    "  function @f(@ps*) {" +
                    "    @fh*;" +
                    "    @stmts*;" +
                    "    @bs*;" +
                    "});",
                    "f", f,
                    "ps", bindings.get("ps"),
                    "bs", expand(bindings.get("bs"), s2, mq),
                    "fh", getFunctionHeadDeclarations(this, s2, mq),
                    "stmts", new ParseTreeNodeContainer(s2.getStartStatements())),
                this,
                scope,
                mq));
            return substV(";");
          }
        }
        return NONE;
      }
    },

    new Rule () {
      @Override
      @RuleDescription(
          name="funcNamedSimpleValue",
          synopsis="",
          reason="")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        // Named simple function expression
        if (match("function @f(@ps*) { @bs*; }", node, bindings)) {
          Scope s2 = Scope.fromFunctionConstructor(
              scope,
              (FunctionConstructor)node);
          if (!s2.hasFreeThis()) {
            checkFormals(bindings.get("ps"), mq);
            return substV(
                "___.primFreeze(" +
                "  ___.simpleFunc(" +
                "    function @f(@ps*) {" +
                "      @fh*;" +
                "      @stmts*;" +
                "      @bs*;" +
                "  }));",
                "f", bindings.get("f"),
                "ps", bindings.get("ps"),
                "bs", expand(bindings.get("bs"), s2, mq),
                "fh", getFunctionHeadDeclarations(this, s2, mq),
                "stmts", new ParseTreeNodeContainer(s2.getStartStatements()));
          }
        }
        return NONE;
      }
    },

    new Rule () {
      @Override
      @RuleDescription(
          name="funcExophoricFunction",
          synopsis="",
          reason="")
      public ParseTreeNode fire(
          ParseTreeNode node, Scope scope, final MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("(function (@formals*) { @body*; })", node, bindings)) {
          Scope s2 = Scope.fromFunctionConstructor(
              scope, (FunctionConstructor) node);
          if (!s2.hasFreeThis()) { return NONE; }

          checkFormals(bindings.get("formals"), mq);
          ParseTreeNode rewrittenBody = bindings.get("body").clone();
          rewrittenBody.acceptPreOrder(new ExophoricFunctionRewriter(mq), null);
          return substV(
              "___.exophora(" +
              "    function (@formals*) {" +
              "      var @localThis = this;" +
              "      @stmts*;" +
              "      @body*;" +
              "})",
              "formals", bindings.get("formals"),
              "localThis", s(new Identifier(ReservedNames.LOCAL_THIS)),
              "body", expand(rewrittenBody, s2, mq),
              "stmts", new ParseTreeNodeContainer(s2.getStartStatements()));
        }
        return NONE;
      }
    },

    new Rule () {
      @Override
      @RuleDescription(
          name="funcBadMethod",
          synopsis="",
          reason="")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("function(@ps*) { @bs*; }", node, bindings)) {
          Scope s2 = Scope.fromFunctionConstructor(scope, (FunctionConstructor)node);
          if (s2.hasFreeThis()) {
            mq.addMessage(
                RewriterMessageType.ANONYMOUS_FUNCTION_REFERENCES_THIS,
                node.getFilePosition(),
                this,
                node);
            return node;
          }
        }
        return NONE;
      }
    },

    new Rule () {
      @Override
      @RuleDescription(
          name="funcCtor",
          synopsis="",
          reason="")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        boolean declaration = node instanceof FunctionDeclaration;
        ParseTreeNode constructorNode = declaration ? node.children().get(1) : node;
        if (match("function @f(@ps*) { @b; @bs*; }", constructorNode, bindings)) {
          Scope s2 = Scope.fromFunctionConstructor(scope, (FunctionConstructor)constructorNode);
          if (s2.hasFreeThis()) {
            checkFormals(bindings.get("ps"), mq);
            ParseTreeNode bNode = bindings.get("b");
            if (bNode instanceof ExpressionStmt) {
              // Rebind bNode to the Expression part of the ExpressionStmt.
              bNode = bNode.children().get(0);
            }
            Map<String, ParseTreeNode> superBindings = new LinkedHashMap<String, ParseTreeNode>();
            // To subclass, the very first line must be a call to the super constructor,
            // which must be a reference to a declared function.
            if (match("@super.call(this, @params*);", bNode, superBindings) &&
                s2.isDeclaredFunctionReference(superBindings.get("super"))){
              Scope paramScope = Scope.fromParseTreeNodeContainer(
                  s2,
                  (ParseTreeNodeContainer)superBindings.get("params"));
              // The rest of the parameters must not contain "this".
              if (paramScope.hasFreeThis()) {
                mq.addMessage(
                    RewriterMessageType.PARAMETERS_TO_SUPER_CONSTRUCTOR_MAY_NOT_CONTAIN_THIS,
                    node.getFilePosition(),
                    this,
                    bNode);
                return node;
              }
              // Expand the parameters, but not the call itself.
              bNode = new ExpressionStmt((Expression)substV(
                  "@super.call(this, @params*);",
                  "super", expandReferenceToOuters(superBindings.get("super"), s2, mq),
                  "params", expand(superBindings.get("params"), s2, mq)));
            } else {
              // If it's not a call to a constructor, expand the entire node.
              bNode = expand(bindings.get("b"), s2, mq);
            }
            Identifier f = (Identifier)bindings.get("f");
            Reference fRef = new Reference(f);
            Identifier f_init___ = new Identifier(f.getName() + "_init___");
            Reference f_init___Ref = new Reference(f_init___);
            // Add a declaration to the start of function body
            if (declaration) {
              scope.declareStartOfScopeVariable(f);
            }
            ParseTreeNode result = substV(
                "(function () {" +
                "  ___.splitCtor(@fRef, @f_init___Ref);" +
                "  function @f(var_args) { return new @fRef.make___(arguments); }" +
                "  function @f_init(@ps*) {" +
                "    @fh*;" +
                "    @stmts*;" +
                "    @b;" +
                "    @bs*;" +
                "  }" +
                "  return @fRef;" +
                "})()",
                "f", f,
                "fRef", fRef,
                "f_init", f_init___,
                "f_init___Ref", f_init___Ref,
                "ps", bindings.get("ps"),
                "fh", getFunctionHeadDeclarations(this, s2, mq),
                "b", bNode,
                "bs", expand(bindings.get("bs"), s2, mq),
                "stmts", new ParseTreeNodeContainer(s2.getStartStatements()));
            if (declaration) {
              // Add the initialization to the start of block
              scope.addStartOfBlockStatement((Statement)expandDef(
                  new Reference((Identifier)bindings.get("f")),
                  result,
                  this,
                  scope,
                  mq));
              return substV(";");
            } else {
              // If used in an expression, it's the first use, so we freeze it.
              return substV("___.primFreeze(@result);", "result", result);
            }
          }
        }
        return NONE;
      }
    },

    ////////////////////////////////////////////////////////////////////////
    // map - object literals
    ////////////////////////////////////////////////////////////////////////

    new Rule () {
      @Override
      @RuleDescription(
          name="mapEmpty",
          synopsis="",
          reason="")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("({})", node, bindings)) {
          return node.clone();
        }
        return NONE;
      }
    },

    new Rule () {
      @Override
      @RuleDescription(
          name="mapBadKeySuffix",
          synopsis="Throw an error if a key with `_` suffix is found",
          reason="")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("({@keys*: @vals*})", node, bindings) &&
            literalsEndWith(bindings.get("keys"), "_")) {
          mq.addMessage(
              RewriterMessageType.KEY_MAY_NOT_END_IN_UNDERSCORE,
              node.getFilePosition(), this, node);
          return node;
        }
        return NONE;
      }
    },

    new Rule () {
      @Override
      @RuleDescription(
          name="mapNonEmpty",
          synopsis="",
          reason="")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("({@keys*: @vals*})", node, bindings)) {
          return substV(
              "({ @keys*: @vals* })",
              "keys", bindings.get("keys"),
              "vals", expand(bindings.get("vals"), scope, mq));
        }
        return NONE;
      }
    },

    ////////////////////////////////////////////////////////////////////////
    // multiDeclaration - multiple declarations
    ////////////////////////////////////////////////////////////////////////

    // TODO(ihab.awad): The 'multiDeclaration' implementation is hard
    // to follow or maintain. Refactor asap.
    new Rule () {
      @Override
      @RuleDescription(
          name="multiDeclaration",
          synopsis="",
          reason="")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        if (node instanceof MultiDeclaration) {
          boolean allDeclarations = true;
          List<ParseTreeNode> expanded = new ArrayList<ParseTreeNode>();

          // Expand each declaration individually, and keep track of whether
          // the result is a declaration or whether we can just run the
          // initializers separately.
          for (ParseTreeNode child : node.children()) {
            ParseTreeNode result = expand(child, scope, mq);
            if (result instanceof ExpressionStmt) {
              result = result.children().get(0);
            } else if (!(result instanceof Expression
                         || result instanceof Declaration)) {
              throw new RuntimeException(
                  "Unexpected result class: " + result.getClass());
            }
            expanded.add(result);
            allDeclarations &= result instanceof Declaration;
          }

          // If they're not all declarations, then split the initializers out
          // so that we can run them in order.
          if (!allDeclarations) {
            List<Declaration> declarations = new ArrayList<Declaration>();
            List<Expression> initializers = new ArrayList<Expression>();
            for (ParseTreeNode n : expanded) {
              if (n instanceof Declaration) {
                Declaration decl = (Declaration) n;
                Expression init = decl.getInitializer();
                if (init != null) {
                  initializers.add(init);
                  decl.removeChild(init);
                }
                declarations.add(decl);
              } else {
                initializers.add((Expression) n);
              }
            }
            Expression[] initOperands = initializers.toArray(new Expression[0]);
            Expression init = (initOperands.length > 1
                               ? Operation.create(Operator.COMMA, initOperands)
                               : initOperands[0]);
            if (declarations.isEmpty()) {
              return new ExpressionStmt(init);
            } else {
              return substV(
                  "{ @decl; @init; }",
                  "decl", new MultiDeclaration(declarations),
                  "init", new ExpressionStmt(init));
            }
          } else {
            return ParseTreeNodes.newNodeInstance(
                MultiDeclaration.class, null, expanded);
          }
        }
        return NONE;
      }
    },

    ////////////////////////////////////////////////////////////////////////
    // other - things not otherwise covered
    ////////////////////////////////////////////////////////////////////////

    new Rule () {
      @Override
      @RuleDescription(
          name="otherInstanceof",
          synopsis="",
          reason="")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("@o instanceof @f", node, bindings)) {
          return substV(
              "@o instanceof @f",
              "o", expand(bindings.get("o"), scope, mq),
              "f", expand(bindings.get("f"), scope, mq));
        }
        return NONE;
      }
    },

    new Rule () {
      @Override
      @RuleDescription(
          name="otherTypeof",
          synopsis="",
          reason="")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("typeof @f", node, bindings)) {
          ParseTreeNode f = bindings.get("f");
          if (f instanceof Reference && scope.isGlobal(getReferenceName(f))) {
            // Lookup of an undefined&undeclared global for typing purposes
            // should not fail with an exception.
            expand(f, scope, mq);
            return substV(
                "typeof ___.readPub(___OUTERS___, @fname)",
                "fname", toStringLiteral(f));
          } else {
            return substV(
                "typeof @f",
                "f", expand(f, scope, mq));
          }
        }
        return NONE;
      }
    },

    new Rule () {
      @Override
      @RuleDescription(
          name="otherSpecialOp",
          synopsis="",
          reason="")
      public ParseTreeNode fire(
          ParseTreeNode node, Scope scope, MessageQueue mq) {
        if (!(node instanceof SpecialOperation)) { return NONE; }
        switch (((SpecialOperation) node).getOperator()) {
          case COMMA: case VOID:
            return expandAll(node, scope, mq);
          default:
            return NONE;
        }
      }
    },

    new Rule () {
      @Override
      @RuleDescription(
          name="labeledStatement",
          synopsis="Throw an error if a label with `__` suffix is found",
          reason="Caja reserves the `__` suffix for internal use")
      public ParseTreeNode fire(
          ParseTreeNode node, Scope scope, MessageQueue mq) {
        if (node instanceof LabeledStmtWrapper) {
          LabeledStmtWrapper lsw = (LabeledStmtWrapper) node;
          if (lsw.getLabel().endsWith("__")) {
            mq.addMessage(
                RewriterMessageType.LABELS_CANNOT_END_IN_DOUBLE_UNDERSCORE,
                node.getFilePosition(),
                MessagePart.Factory.valueOf(lsw.getLabel()));
          }
          LabeledStmtWrapper expanded = new LabeledStmtWrapper(
              lsw.getLabel(), (Statement) expand(lsw.getBody(), scope, mq));
          expanded.setFilePosition(lsw.getFilePosition());
          return expanded;
        }
        return NONE;
      }
    },

    ////////////////////////////////////////////////////////////////////////
    // recurse - automatically recurse into some structures
    ////////////////////////////////////////////////////////////////////////

    new Rule () {
      @Override
      @RuleDescription(
          name="recurse",
          synopsis="Automatically recurse into some structures",
          reason="")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        if (node instanceof ParseTreeNodeContainer ||
            node instanceof ArrayConstructor ||
            node instanceof BreakStmt ||
            node instanceof CaseStmt ||
            node instanceof Conditional ||
            node instanceof ContinueStmt ||
            node instanceof DefaultCaseStmt ||
            node instanceof ExpressionStmt ||
            node instanceof Identifier ||
            node instanceof Literal ||
            node instanceof Loop ||
            node instanceof MultiDeclaration ||
            node instanceof Noop ||
            node instanceof SimpleOperation ||
            node instanceof ControlOperation ||
            node instanceof ReturnStmt ||
            node instanceof SwitchStmt ||
            node instanceof ThrowStmt) {
          return expandAll(node, scope, mq);
        }
        return NONE;
      }
    }
  };

  public DefaultCajaRewriter() {
    this(true);
  }

  public DefaultCajaRewriter(boolean logging) {
    super(logging);
    addRules(cajaRules);
  }
}
