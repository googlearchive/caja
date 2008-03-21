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
import com.google.caja.parser.js.Literal;
import com.google.caja.parser.js.Loop;
import com.google.caja.parser.js.MultiDeclaration;
import com.google.caja.parser.js.Noop;
import com.google.caja.parser.js.Operator;
import com.google.caja.parser.js.Reference;
import com.google.caja.parser.js.ReturnStmt;
import com.google.caja.parser.js.SimpleOperation;
import com.google.caja.parser.js.SpecialOperation;
import com.google.caja.parser.js.SwitchStmt;
import com.google.caja.parser.js.StringLiteral;
import com.google.caja.parser.js.ThrowStmt;
import com.google.caja.parser.js.TryStmt;
import com.google.caja.parser.js.Statement;
import com.google.caja.parser.js.UndefinedLiteral;
import com.google.caja.parser.js.ArrayConstructor;
import com.google.caja.plugin.ReservedNames;
import com.google.caja.plugin.SyntheticNodes;
import static com.google.caja.plugin.SyntheticNodes.s;
import com.google.caja.util.Pair;
import com.google.caja.util.SyntheticAttributes;
import com.google.caja.reporting.MessageQueue;

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
public class DefaultCajaRewriter extends Rewriter {
  public DefaultCajaRewriter() {
    this(true);
  }

  public DefaultCajaRewriter(boolean logging) {
    super(logging);

    ////////////////////////////////////////////////////////////////////////
    // Do nothing if the node is already the result of some translation
    ////////////////////////////////////////////////////////////////////////

    addRule(new Rule("synthetic0", this) {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        if (isSynthetic(node)) {
          if (node instanceof FunctionConstructor) {
            scope = Scope.fromFunctionConstructor(scope, (FunctionConstructor)node);
          }
          return expandAll(node, scope, mq);
        }
        return NONE;
      }
    });

    ////////////////////////////////////////////////////////////////////////
    // with - disallow the 'with' construct
    ////////////////////////////////////////////////////////////////////////

    addRule(new Rule("with", this) {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        // Our parser does not recognize "with" at all.
        return NONE;
      }
    });

    ////////////////////////////////////////////////////////////////////////
    // foreach - "for ... in" loops
    ////////////////////////////////////////////////////////////////////////

    addRule(new Rule("foreach", this) {
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

        Pair<ParseTreeNode, ParseTreeNode> oTemp = reuse(
            scope.newTempVariable(),
            bindings.get("o"),
            scope.isGlobal(),
            this,
            scope,
            mq);

        Pair<ParseTreeNode, ParseTreeNode> kTemp = reuse(
            scope.newTempVariable(),
            s(new UndefinedLiteral()),
            scope.isGlobal(),
            this,
            scope,
            mq);

        List<Statement> declsList = new ArrayList<Statement>();
        declsList.add((Statement)oTemp.b);
        declsList.add((Statement)kTemp.b);

        if (isDecl) {
          Pair<ParseTreeNode, ParseTreeNode> kDecl = reuseEmpty(
              ((Reference)bindings.get("k")).getIdentifierName(),
              scope.isGlobal(),
              this,
              scope,
              mq);
          declsList.add((Statement)kDecl.b);
        }

        ParseTreeNode kAssignment = substV(
            "@k = @kTempRef;",
            "k", bindings.get("k"),
            "kTempRef", kTemp.a);
        kAssignment.getAttributes().remove(SyntheticNodes.SYNTHETIC);
        kAssignment = expand(kAssignment, scope, mq);
        kAssignment = s(new ExpressionStmt((Expression)kAssignment));

        // Note that we use 'canEnumProp' even if 'this' is actually the global object (in
        // which case 'this' will get rewritten to '___OUTERS___'. Statements in the global
        // scope *are* effectively executing with 'this === ___OUTERS___'.

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
            "oTempRef", oTemp.a,
            "kTempRef", kTemp.a,
            "kTempStmt", s(new ExpressionStmt((Expression)kTemp.a)),
            "kAssignment", kAssignment,
            "ss", expand(bindings.get("ss"), scope, mq));
      }
    });

    ////////////////////////////////////////////////////////////////////////
    // try - try/catch/finally constructs
    ////////////////////////////////////////////////////////////////////////

    addRule(new Rule("tryCatch", this) {
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
    });

    addRule(new Rule("tryCatchFinally", this) {
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
    });

    addRule(new Rule("tryFinally", this) {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("try { @s0*; } finally { @s1*; }", node, bindings)) {
          TryStmt t = (TryStmt)node;
          return substV(
            "try { @s0*; } finally { @s1*; }",
            "s0",  expandAll(bindings.get("s0"), scope, mq),
            "s1",  expandAll(bindings.get("s1"), scope, mq));
        }
        return NONE;
      }
    });

    ////////////////////////////////////////////////////////////////////////
    // variable - variable name handling
    ////////////////////////////////////////////////////////////////////////

    addRule(new Rule("varArgs", this) {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match(ReservedNames.ARGUMENTS, node, bindings)) {
          return subst(ReservedNames.LOCAL_ARGUMENTS, bindings);
        }
        return NONE;
      }
    });

    addRule(new Rule("varThis", this) {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match(ReservedNames.THIS, node, bindings)) {
          return scope.isGlobal() ?
              subst("___OUTERS___", bindings) :
              subst(ReservedNames.LOCAL_THIS, bindings);
        }
        return NONE;
      }
    });

    addRule(new Rule("varBadSuffix", this) {
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
    });

    addRule(new Rule("varBadSuffixDeclaration", this) {
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
    });

    addRule(new Rule("varBadGlobalSuffix", this) {
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
    });

    addRule(new Rule("varBadCtorLeak", this) {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("@x", node, bindings) &&
            bindings.get("x") instanceof Reference &&
            scope.isConstructor(getReferenceName(bindings.get("x")))) {
          mq.addMessage(
              RewriterMessageType.CONSTRUCTORS_ARE_NOT_FIRST_CLASS,
              node.getFilePosition(), this, node);
          return node;
        }
        return NONE;
      }
    });

    addRule(new Rule("varFuncFreeze", this) {
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
    });

    addRule(new Rule("varGlobal", this) {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("@x", node, bindings) &&
            bindings.get("x") instanceof Reference) {
          return expandReferenceToOuters(bindings.get("x"), scope, mq);
        }
        return NONE;
      }
    });

    addRule(new Rule("varDefault", this) {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("@x", node, bindings) &&
            bindings.get("x") instanceof Reference) {
          return bindings.get("x");
        }
        return NONE;
      }
    });

    ////////////////////////////////////////////////////////////////////////
    // read - reading values
    ////////////////////////////////////////////////////////////////////////

    addRule(new Rule("readBadSuffix", this) {
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
    });

    addRule(new Rule("readGlobalViaThis", this) {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("this.@p", node, bindings) && scope.isGlobal()) {
          return expandReferenceToOuters(bindings.get("p"), scope, mq);
        }
        return NONE;
      }
    });

    addRule(new Rule("readInternal", this) {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("this.@p", node, bindings)) {
          String propertyName = ((Reference)bindings.get("p")).getIdentifierName();
          return substV(
            "t___.@fp ? t___.@p : ___.readProp(t___, @rp)",
            "p",  bindings.get("p"),
            "fp", new Reference(new Identifier(propertyName + "_canRead___")),
            "rp", new StringLiteral("'" + propertyName + "'"));
        }
        return NONE;
      }
    });

    addRule(new Rule("readBadInternal", this) {
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
    });

    addRule(new Rule("readPublic", this) {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("@o.@p", node, bindings)) {
          String propertyName = ((Reference)bindings.get("p")).getIdentifierName();
          return substV(
              "(function() {" +
              "  var x___ = @o;" +
              "  return x___.@fp ? x___.@p : ___.readPub(x___, @rp);" +
              "})()",
              "o",  expand(bindings.get("o"), scope, mq),
              "p",  bindings.get("p"),
              "fp", new Reference(new Identifier(propertyName + "_canRead___")),
              "rp", new StringLiteral("'" + propertyName + "'"));
        }
        return NONE;
      }
    });

    addRule(new Rule("readIndexGlobal", this) {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("this[@s]", node, bindings) && scope.isGlobal()) {
          return substV(
              "___.readPub(___OUTERS___, @s)",
              "s", expand(bindings.get("s"), scope, mq));
        }
        return NONE;
      }
    });

    addRule(new Rule("readIndexInternal", this) {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("this[@s]", node, bindings)) {
          return substV(
              "___.readProp(t___, @s)",
              "s", expand(bindings.get("s"), scope, mq));
        }
        return NONE;
      }
    });

    addRule(new Rule("readIndexPublic", this) {
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
    });

    ////////////////////////////////////////////////////////////////////////
    // set - assignments
    ////////////////////////////////////////////////////////////////////////

    addRule(new Rule("setGlobal", this) {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("@p = @r", node, bindings) &&
            bindings.get("p") instanceof Reference) {
          String propertyName = getReferenceName(bindings.get("p"));
          if (scope.isGlobal(propertyName) && !ReservedNames.THIS.equals(propertyName)) {
            return substV(
                "(function() {" +
                "  var x___ = @r;" +
                "  return ___OUTERS___.@fp ?" +
                "      (___OUTERS___.@p = x___) :" +
                "      ___.setPub(___OUTERS___, @rp, x___);" +
                "})()",
                "r",  expand(bindings.get("r"), scope, mq),
                "p",  bindings.get("p"),
                "fp", new Reference(new Identifier(propertyName + "_canSet___")),
                "rp", new StringLiteral(StringLiteral.toQuotedValue(propertyName)));
          }
        }
        return NONE;
      }
    });

    addRule(new Rule("setBadThis", this) {
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
    });

    addRule(new Rule("setBadSuffix", this) {
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
    });

    addRule(new Rule("setGlobalViaThis", this) {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("this.@p = @r", node, bindings) && scope.isGlobal()) {
          String propertyName = ((Reference)bindings.get("p")).getIdentifierName();
          return substV(
              "(function() {" +
              "  var x___ = @r;" +
              "  return ___OUTERS___.@fp ?" +
              "      (___OUTERS___.@p = x___) :" +
              "      ___.setPub(___OUTERS___, @rp, x___);" +
              "})()",
              "r",  expand(bindings.get("r"), scope, mq),
              "p",  bindings.get("p"),
              "fp", new Reference(new Identifier(propertyName + "_canSet___")),
              "rp", new StringLiteral(StringLiteral.toQuotedValue(propertyName)));
        }
        return NONE;
      }
    });

    addRule(new Rule("setInternal", this) {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("this.@p = @r", node, bindings)) {
          String propertyName = ((Reference)bindings.get("p")).getIdentifierName();
          Reference target = new Reference(new Identifier(
              scope.isGlobal() ? ReservedNames.OUTERS : ReservedNames.LOCAL_THIS));
          return substV(
              "(function() {" +
              "  var x___ = @r;" +
              "  return t___.@fp ? (t___.@p = x___) : ___.setProp(t___, @rp, x___);" +
              "})()",
              "r",  expand(bindings.get("r"), scope, mq),
              "p",  bindings.get("p"),
              "fp", new Reference(new Identifier(propertyName + "_canSet___")),
              "rp", new StringLiteral(StringLiteral.toQuotedValue(propertyName)),
              "target", target);
        }
        return NONE;
      }
    });

    addRule(new Rule("setMember", this) {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();

        // BUG TODO(erights,ihab): We must only recognize (and thus allow) this
        // expression when it is evaluated for effects only, not for value.
        // Currently, since we have no such test, the translated expression will
        // safely evaluate to <tt>undefined</tt>, but this behavior is not within
        // a fail-stop subset of JavaScript.
        if (match("@fname.prototype.@p = @m;", node, bindings)) {
          String fname = getReferenceName(bindings.get("fname"));
          if (scope.isDeclaredFunction(fname)) {
            String propertyName = getReferenceName(bindings.get("p"));
            if (!"constructor".equals(propertyName)) {
              return substV(
                  "(function() {" +
                  "  var x___ = @m;" +
                  "  return ___.setMember(@fname, @rp, x___);" +
                  "})();",
                  "fname", bindings.get("fname"),
                  "m", expandMember(bindings.get("fname"), bindings.get("m"), this, scope, mq),
                  "rp", new StringLiteral("'" + propertyName + "'"));
            }
          }
        }
        return NONE;
      }
    });

    addRule(new Rule("setBadInternal", this) {
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
    });

    addRule(new Rule("setStatic", this) {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("@fname.@p = @r", node, bindings) &&
            bindings.get("fname") instanceof Reference &&
            scope.isFunction(getReferenceName(bindings.get("fname")))) {
          String propertyName = ((Reference)bindings.get("p")).getIdentifierName();
          if (!"Super".equals(propertyName)) {
            return substV(
                "___.setPub(@fname, @rp, @r)",
                "fname", bindings.get("fname"),
                "rp", new StringLiteral("'" + propertyName + "'"),
                "r", expand(bindings.get("r"), scope, mq));
          }
        }
        return NONE;
      }
    });

    addRule(new Rule("setPublic", this) {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("@o.@p = @r", node, bindings)) {
          String propertyName = ((Reference)bindings.get("p")).getIdentifierName();
          Pair<ParseTreeNode, ParseTreeNode> po =
              reuse("x___", bindings.get("o"), false, this, scope, mq);
          Pair<ParseTreeNode, ParseTreeNode> pr =
              reuse("x0___", bindings.get("r"), false, this, scope, mq);
          return substV(
              "(function() {" +
              "  @pob;" +
              "  @prb;" +
              "  return @poa.@pCanSet ? (@poa.@p = @pra) : " +
              "                         ___.setPub(@poa, @pName, @pra);" +
              "})();",
              "pName", new StringLiteral("'" + propertyName + "'"),
              "p", bindings.get("p"),
              "pCanSet", new Reference(new Identifier(propertyName + "_canSet___")),
              "poa", po.a,
              "pob", po.b,
              "pra", pr.a,
              "prb", pr.b);
        }
        return NONE;
      }
    });

    addRule(new Rule("setIndexInternal", this) {
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
    });

    addRule(new Rule("setIndexPublic", this) {
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
    });

    addRule(new Rule("setBadInitialize", this) {
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
    });

    addRule(new Rule("setInitialize", this) {
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
    });

    addRule(new Rule("setBadDeclare", this) {
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
    });

    addRule(new Rule("setDeclare", this) {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("var @v", node, bindings) &&
            !scope.isFunction(getIdentifierName(bindings.get("v")))) {
          if (!scope.isGlobal()) {
            return node;
          } else {
            ParseTreeNode v = bindings.get("v");
            String vName = getIdentifierName(v);
            ParseTreeNode expr = substV(
                "___.setPub(___OUTERS___, @vName, ___.readPub(___OUTERS___, @vName));",
                "vName", new StringLiteral("'" + vName + "'"));
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
    });

    // TODO(erights): Need a general way to expand lValues
    addRule(new Rule("setVar", this) {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("@v = @r", node, bindings)) {
          ParseTreeNode v = bindings.get("v");
          ParseTreeNode r = bindings.get("r");
          if (v instanceof Reference) {
            String vName = getReferenceName(v);
            if (!scope.isFunction(vName)) {
              if (scope.isGlobal(vName)) {
                Pair<ParseTreeNode, ParseTreeNode> pr =
                    reuse("x___", r, true, this, scope, mq);
                return substV(
                    "(function() {" +
                    "  @prb;" +
                    "  return ___OUTERS___.@vCanSet ? (___OUTERS___.@v = @pra) :" +
                    "                                 ___.setPub(___OUTERS___, @vName, @pra);" +
                    "})();",
                    "v", v,
                    "vCanSet", new Reference(new Identifier(vName + "_canSet___")),
                    "vName", new StringLiteral(StringLiteral.toQuotedValue(vName)),
                    "pra", pr.a,
                    "prb", pr.b);
              } else {
                return substV(
                    "@v = @r",
                    "v", v,
                    "r", expand(r, scope, mq));
              }
            }
          }
        }
        return NONE;
      }
    });

    // TODO(erights): Need a general way to expand readModifyWrite lValues.
    // For now, we're just picking off a few common special cases as they
    // come up.

    addRule(new Rule("setReadModifyWriteLocalVar", this) {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        if (node instanceof AssignOperation) {
          AssignOperation aNode = (AssignOperation)node;
          Operator op = aNode.getOperator();
          if (op != Operator.ASSIGN) {
            Expression lValue = aNode.children().get(0);
            if (lValue instanceof Reference) {
              String lStr = getReferenceName(lValue);
              if (!scope.isGlobal(lStr)) {
                return expandAll(node, scope, mq);
              }
            }
          }
        }
        return NONE;
      }
    });

    addRule(new Rule("setPostIncrGlobal", this) {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("@v ++", node, bindings)) {
          ParseTreeNode v = bindings.get("v");
          if (v instanceof Reference) {
            if (scope.isGlobal(getReferenceName(v))) {
              return substV(
                  "(function() {" +
                  "  var x___ = Number(___.readPub(___OUTERS___, @vName, true));" +
                  "  ___.setPub(___OUTERS___, @vName, x___ + 1);" +
                  "  return x___;" +
                  "})()",
                  "vName", new StringLiteral(StringLiteral.toQuotedValue(getReferenceName(v))));
            }
          }
        }
        return NONE;
      }
    });

    ////////////////////////////////////////////////////////////////////////
    // new - new object creation
    ////////////////////////////////////////////////////////////////////////

    addRule(new Rule("newCalllessCtor", this) {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("new @ctor", node, bindings) &&
            bindings.get("ctor") instanceof Reference &&
            scope.isDeclaredFunction(getReferenceName(bindings.get("ctor")))) {
          return substV(
              "new (___.asCtor(@ctor))()",
              "ctor", expandReferenceToOuters(bindings.get("ctor"), scope, mq));
        }
        return NONE;
      }
    });

    addRule(new Rule("newCtor", this) {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("new @ctor(@as*)", node, bindings) &&
            bindings.get("ctor") instanceof Reference &&
            scope.isDeclaredFunction(getReferenceName(bindings.get("ctor")))) {
          return substV(
              "new (___.asCtor(@ctor))(@as*)",
              "ctor", expandReferenceToOuters(bindings.get("ctor"), scope, mq),
              "as", expandAll(bindings.get("as"), scope, mq));
        }
        return NONE;
      }
    });

    addRule(new Rule("newFunc", this) {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("new @f(@as*)", node, bindings) &&
            bindings.get("f") instanceof Reference) {
          return substV(
              "new (___.asCtor(@f))(@as*)",
              "f", expandReferenceToOuters(bindings.get("f"), scope, mq),
              "as", expandAll(bindings.get("as"), scope, mq));
        }
        return NONE;
      }
    });

    addRule(new Rule("newBadCtor", this) {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("new @ctor(@as*)", node, bindings)) {
          mq.addMessage(
              RewriterMessageType.NEW_ON_ARBITRARY_EXPRESSION_DISALLOWED,
              node.getFilePosition(), this, node);
          return node;
        }
        return NONE;
      }
    });

    ////////////////////////////////////////////////////////////////////////
    // call - function calls
    ////////////////////////////////////////////////////////////////////////

    addRule(new Rule("callBadSuffix", this) {
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
    });

    addRule(new Rule("callGlobalViaThis", this) {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("this.@m(@as*)", node, bindings) && scope.isGlobal()) {
          Pair<ParseTreeNode, ParseTreeNode> aliases =
              reuseAll(bindings.get("as"), false, this, scope, mq);
          String methodName = ((Reference)bindings.get("m")).getIdentifierName();
          return substV(
              "(function() {" +
              "  @as*;" +
              "  return ___OUTERS___.@fm ?" +
              "      ___OUTERS___.@m(@vs*) :" +
              "      ___.callPub(___OUTERS___, @rm, [@vs*]);" +
              "})()",
              "as", aliases.b,
              "vs", aliases.a,
              "m",  bindings.get("m"),
              "fm", new Reference(new Identifier(methodName + "_canCall___")),
              "rm", new StringLiteral(StringLiteral.toQuotedValue(methodName)));
        }
        return NONE;
      }
    });

    addRule(new Rule("callInternal", this) {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("this.@m(@as*)", node, bindings)) {
          Pair<ParseTreeNode, ParseTreeNode> aliases =
              reuseAll(bindings.get("as"), false, this, scope, mq);
          String methodName = ((Reference)bindings.get("m")).getIdentifierName();
          return substV(
              "(function() {" +
              "  @as*;" +
              "  return t___.@fm ? t___.@m(@vs*) : ___.callProp(t___, @rm, [@vs*]);" +
              "})()",
              "as", aliases.b,
              "vs", aliases.a,
              "m",  bindings.get("m"),
              "fm", new Reference(new Identifier(methodName + "_canCall___")),
              "rm", new StringLiteral(StringLiteral.toQuotedValue(methodName)));
        }
        return NONE;
      }
    });

    addRule(new Rule("callBadInternal", this) {
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
    });

    addRule(new Rule("callCajaDef2", this) {
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
    });

    addRule(new Rule("callCajaDef2Bad", this) {
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
    });

    addRule(new Rule("callCajaDef3Plus", this) {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("caja.def(@fname, @base, @mm, @ss?)", node, bindings) &&
            scope.isFunction(getReferenceName(bindings.get("fname"))) &&
            scope.isFunction(getReferenceName(bindings.get("base")))) {
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
    });

    addRule(new Rule("callCajaDef3PlusBad", this) {
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
    });

    addRule(new Rule("callPublic", this) {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("@o.@m(@as*)", node, bindings)) {
          Pair<ParseTreeNode, ParseTreeNode> aliases =
              reuseAll(bindings.get("as"), false, this, scope, mq);
          String methodName = ((Reference)bindings.get("m")).getIdentifierName();
          return substV(
              "(function() {" +
              "  var x___ = @o;" +
              "  @as*;" +
              "  return x___.@fm ? x___.@m(@vs*) : ___.callPub(x___, @rm, [@vs*]);" +
              "})()",
              "o",  expand(bindings.get("o"), scope, mq),
              "as", aliases.b,
              "vs", aliases.a,
              "m",  bindings.get("m"),
              "fm", new Reference(new Identifier(methodName + "_canCall___")),
              "rm", new StringLiteral("'" + methodName + "'"));
        }
        return NONE;
      }
    });

    addRule(new Rule("callIndexInternal", this) {
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
    });

    addRule(new Rule("callIndexPublic", this) {
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
    });

    addRule(new Rule("callFunc", this) {
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
    });

    ////////////////////////////////////////////////////////////////////////
    // function - function definitions
    ////////////////////////////////////////////////////////////////////////

    addRule(new Rule("funcAnonSimple", this) {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        // Anonymous simple function constructor
        if (match("function(@ps*) { @bs*; }", node, bindings)) {
          Scope s2 = Scope.fromFunctionConstructor(scope, (FunctionConstructor)node);
          if (!s2.hasFreeThis()) {
            return substV(
                "___.primFreeze(" +
                "  ___.simpleFunc(" +
                "    function(@ps*) {" +
                "      @fh*;" +
                "      @bs*;" +
                "}))",
                "ps", bindings.get("ps"),
                "bs", expand(bindings.get("bs"), s2, mq),
                "fh", getFunctionHeadDeclarations(this, s2, mq));
          }
        }
        return NONE;
      }
    });

    addRule(new Rule("funcNamedSimpleDecl", this) {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        // Named simple function declaration
        if (node.getClass() == FunctionDeclaration.class &&
            match("function @f(@ps*) { @bs*; }", node.children().get(1), bindings)) {
          Scope s2 = Scope.fromFunctionConstructor(
              scope,
              (FunctionConstructor)node.children().get(1));
          if (!s2.hasFreeThis()) {
            return expandDef(
                new Reference((Identifier)bindings.get("f")),
                substV(
                    "___.simpleFunc(" +
                    "  function @f(@ps*) {" +
                    "    @fh*;" +
                    "    @bs*;" +
                    "});",
                    "f", bindings.get("f"),
                    "ps", bindings.get("ps"),
                    "bs", expand(bindings.get("bs"), s2, mq),
                    "fh", getFunctionHeadDeclarations(this, s2, mq)),
                this,
                scope,
                mq);
          }
        }
        return NONE;
      }
    });

    addRule(new Rule("funcNamedSimpleValue", this) {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        // Named simple function constructor
        if (match(getPatternNode("function @f(@ps*) { @bs* }"), node, bindings)) {
          Scope s2 = Scope.fromFunctionConstructor(scope, (FunctionConstructor)node);
          if (!s2.hasFreeThis()) {
            return substV(
                "___.primFreeze(" +
                "  ___.simpleFunc(" +
                "    function @f(@ps*) {" +
                "      @fh*;" +
                "      @bs*;" +
                "}));",
                "ps", bindings.get("ps"),
                "fh", getFunctionHeadDeclarations(this, s2, mq),
                "bs", expand(bindings.get("bs"), s2, mq),
                "f",  bindings.get("f"));
          }
        }
        return NONE;
      }
    });

    addRule(new Rule("funcBadMethod", this) {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("function(@ps*) { @bs*; }", node, bindings)) {
          Scope s2 = Scope.fromFunctionConstructor(scope, (FunctionConstructor)node);
          if (s2.hasFreeThis()) {
            mq.addMessage(
                RewriterMessageType. METHOD_IN_NON_METHOD_CONTEXT,
                node.getFilePosition(), this, node);
            return node;
          }
        }
        return NONE;
      }
    });

    addRule(new Rule("funcBadCtor", this) {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        // This catches a case where a named function is *not* part of a declaration.
        if (match("function @f(@ps*) { @bs*; }", node, bindings)) {
          Scope s2 = Scope.fromFunctionConstructor(scope, (FunctionConstructor)node);
          if (s2.hasFreeThis()) {
            mq.addMessage(
                RewriterMessageType.CONSTRUCTOR_CANNOT_ESCAPE,
                node.getFilePosition(), this, node);
            return node;
          }
        }
        return NONE;
      }
    });

    addRule(new Rule("funcDerivedCtorDecl", this) {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (node instanceof FunctionDeclaration &&
            match(
                "function @f(@ps*) { @sf.Super.call(this, @as*); @bs*; }",
                node.children().get(1), bindings)) {
          // The following test checks that Reference "@sf" has the same name as Identifier "@f":
          Object fName = bindings.get("f").getValue();
          Object sfName = bindings.get("sf").children().get(0).getValue();
          if (fName.equals(sfName)) {
            Scope argsScope = Scope.fromParseTreeNodeContainer(
                scope,
            (ParseTreeNodeContainer)bindings.get("as"));
            if (!argsScope.hasFreeThis()) {
              Scope s2 = Scope.fromFunctionConstructor(
                  scope,
                  (FunctionConstructor)node.children().get(1));
              return expandDef(
                  new Reference((Identifier)bindings.get("f")),
                  substV(
                      "___.ctor(function @f(@ps*) {" +
                      "  @fh*;" +
                      "  @sf.Super.call(@th, @as*);" +
                      "  @bs*;" +
                      "});",
                      "f", bindings.get("f"),
                      "ps", bindings.get("ps"),
                      "fh", getFunctionHeadDeclarations(this, s2, mq),
                      "sf", bindings.get("sf"),
                      "as", expand(bindings.get("as"), s2, mq),
                      "bs", expand(bindings.get("bs"), s2, mq),
                      "th", new Reference(new Identifier(ReservedNames.LOCAL_THIS))),
                  this,
                  scope,
                  mq);
            }
          }
        }
        return NONE;
      }
    });

    addRule(new Rule("funcCtorDecl", this) {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (node instanceof FunctionDeclaration &&
            match("function @f(@ps*) { @bs*; }", node.children().get(1), bindings)) {
          Scope s2 = Scope.fromFunctionConstructor(
              scope,
              (FunctionConstructor)node.children().get(1));
          if (s2.hasFreeThis()) {
            return expandDef(
                new Reference((Identifier)bindings.get("f")),
                substV(
                    "___.ctor(function @f(@ps*) {" +
                    "  @fh*;" +
                    "  @bs*;" +
                    "});",
                    "f", bindings.get("f"),
                    "ps", bindings.get("ps"),
                    "fh", getFunctionHeadDeclarations(this, s2, mq),
                    "bs", expand(bindings.get("bs"), s2, mq)),
                this,
                scope,
                mq);
          }
        }
        return NONE;
      }
    });

    ////////////////////////////////////////////////////////////////////////
    // map - object literals
    ////////////////////////////////////////////////////////////////////////

    addRule(new Rule("mapEmpty", this) {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("({})", node, bindings)) {
          return node.clone();
        }
        return NONE;
      }
    });

    addRule(new Rule("mapBadKeySuffix", this) {
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
    });

    addRule(new Rule("mapNonEmpty", this) {
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
    });

    ////////////////////////////////////////////////////////////////////////
    // multiDeclaration - multiple declarations
    ////////////////////////////////////////////////////////////////////////

    // TODO(ihab.awad): The 'multiDeclaration' implementation is hard
    // to follow or maintain. Refactor asap.
    addRule(new Rule("multiDeclaration", this) {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        if (node instanceof MultiDeclaration) {
          boolean isDeclaration = false;
          List<ParseTreeNode> results = new ArrayList<ParseTreeNode>();

          for (ParseTreeNode child : node.children()) {
            ParseTreeNode result = expand(child, scope, mq);
            if (result.getClass() == Expression.class) {
              results.add(result);
            } else if (result.getClass() == ExpressionStmt.class) {
              results.add(result.children().get(0));
            } else if (result.getClass() == Declaration.class) {
              results.add(result);
              isDeclaration = true;
            } else {
              throw new RuntimeException("Unexpected result class: " + result.getClass());
            }
          }

          if (!isDeclaration) {
            ParseTreeNode output = results.get(0);
            for (int i = 1; i < results.size(); i++) {
              List<ParseTreeNode> children = Arrays.asList(new ParseTreeNode[] {
                  output,
                  results.get(i),
              });
              output = ParseTreeNodes.newNodeInstance(
                  SpecialOperation.class,
                  Operator.COMMA,
                  children);
            }
            return ParseTreeNodes.newNodeInstance(
                ExpressionStmt.class,
                null,
                Arrays.asList(new ParseTreeNode[] { output }));
          } else {
            return ParseTreeNodes.newNodeInstance(
                MultiDeclaration.class,
                null,
                results);
          }
        }
        return NONE;
      }
    });

    ////////////////////////////////////////////////////////////////////////
    // other - things not otherwise covered
    ////////////////////////////////////////////////////////////////////////

    addRule(new Rule("otherInstanceof", this) {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("@o instanceof @f", node, bindings)) {
          if (scope.isFunction(getReferenceName(bindings.get("f")))) {
            return substV(
                "@o instanceof @f",
                "o", expand(bindings.get("o"), scope, mq),
                "f", expand(bindings.get("f"), scope, mq));
          }
        }
        return NONE;
      }
    });

    addRule(new Rule("otherTypeof", this) {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("typeof @f", node, bindings)) {
          return substV(
              "typeof @f",
              "f", expand(bindings.get("f"), scope, mq));
        }
        return NONE;
      }
    });

    addRule(new Rule("otherBadInstanceof", this) {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("@o instanceof @f", node, bindings)) {
          mq.addMessage(
              RewriterMessageType.INVOKED_INSTANCEOF_ON_NON_FUNCTION,
              node.getFilePosition(), this, node);
          return node;
        }
        return NONE;
      }
    });

    ////////////////////////////////////////////////////////////////////////
    // recurse - automatically recurse into some structures
    ////////////////////////////////////////////////////////////////////////

    addRule(new Rule("recurse", this) {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        if (node instanceof ParseTreeNodeContainer ||
            node instanceof ArrayConstructor ||
            node instanceof BreakStmt ||
            node instanceof Block ||
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
    });
  }
}
