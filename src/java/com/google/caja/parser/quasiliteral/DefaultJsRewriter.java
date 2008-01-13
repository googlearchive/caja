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
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.CatchStmt;
import com.google.caja.parser.js.Conditional;
import com.google.caja.parser.js.ExpressionStmt;
import com.google.caja.parser.js.FunctionConstructor;
import com.google.caja.parser.js.FunctionDeclaration;
import com.google.caja.parser.js.Identifier;
import com.google.caja.parser.js.Literal;
import com.google.caja.parser.js.Loop;
import com.google.caja.parser.js.Noop;
import com.google.caja.parser.js.Operation;
import com.google.caja.parser.js.Reference;
import com.google.caja.parser.js.ReturnStmt;
import com.google.caja.parser.js.StringLiteral;
import com.google.caja.parser.js.ThrowStmt;
import com.google.caja.parser.js.TryStmt;
import com.google.caja.parser.js.UndefinedLiteral;
import com.google.caja.plugin.ReservedNames;
import com.google.caja.util.Pair;
import com.google.caja.reporting.MessageQueue;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Rewrites a JavaScript parse tree to comply with default Caja rules.
 *
 * @author ihab.awad@gmail.com (Ihab Awad)
 */
public class DefaultJsRewriter extends JsRewriter {
  public DefaultJsRewriter() {
    this(true);
  }
  
  public DefaultJsRewriter(boolean logging) {
    super(logging);

    ////////////////////////////////////////////////////////////////////////
    // Do nothing if the node is already the result of some translation
    ////////////////////////////////////////////////////////////////////////

    addRule(new Rule("synthetic0") {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        if (isSynthetic(node)) {
          if (node instanceof FunctionConstructor) {
            scope = new Scope(scope, (FunctionConstructor)node);
          }
          return expandAll(node, scope, mq);
        }
        return NONE;
      }
    });

    ////////////////////////////////////////////////////////////////////////
    // with - disallow the 'with' construct
    ////////////////////////////////////////////////////////////////////////

    addRule(new Rule("with") {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        // Our parser does not recognize "with" at all.
        return NONE;
      }
    });

    ////////////////////////////////////////////////////////////////////////
    // try - try/catch/finally constructs
    ////////////////////////////////////////////////////////////////////////

    addRule(new Rule("tryCatch") {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("try { @s0*; } catch (@x) { @s1*; }", node, bindings)) {
          return substV(
            "try { @s0*; } catch (@x) { @s1*; }",
            "s0",  expandAll(bindings.get("s0"), scope, mq),
            "s1",  expandAll(bindings.get("s1"), scope, mq),
            "x", bindings.get("x"));
        }
        return NONE;
      }
    });

    addRule(new Rule("tryCatchFinally") {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("try { @s0*; } catch (@x) { @s1*; } finally { @s2*; }", node, bindings)) {
          return substV(
            "try { @s0*; } catch (@x) { @s1*; }",
            "s0",  expandAll(bindings.get("s0"), scope, mq),
            "s1",  expandAll(bindings.get("s1"), scope, mq),
            "s2",  expandAll(bindings.get("s2"), scope, mq),              
            "x", bindings.get("x"));
        }
        return NONE;
      }
    });

    ////////////////////////////////////////////////////////////////////////
    // variable - variable name handling
    ////////////////////////////////////////////////////////////////////////

    addRule(new Rule("varArgs") {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match(ReservedNames.ARGUMENTS, node, bindings)) {
          return subst(ReservedNames.LOCAL_ARGUMENTS, bindings);
        }
        return NONE;
      }
    });

    addRule(new Rule("varThis") {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match(ReservedNames.THIS, node, bindings)) {
          return scope.getParent() == null ?
              subst("___OUTERS___", bindings) :
              subst(ReservedNames.LOCAL_THIS, bindings);
        }
        return NONE;
      }
    });

    addRule(new Rule("varBadSuffix") {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("@x__", node, bindings)) {
          mq.addMessage(
              JsRewriterMessageType.VARIABLES_CANNOT_END_IN_DOUBLE_UNDERSCORE,
              this, node);
          return node;
        }
        return NONE;
      }
    });

    addRule(new Rule("varBadGlobalSuffix") {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("@x_", node, bindings)) {
          String symbol = ((Identifier)bindings.get("x")).getValue() + "_";
          if (scope.isGlobal(symbol)) {
            mq.addMessage(
                JsRewriterMessageType.GLOBALS_CANNOT_END_IN_UNDERSCORE,
                this, node); 
            return node;
          }
        }
        return NONE;
      }
    });

    addRule(new Rule("varBadCtorLeak") {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("@x", node, bindings) &&
            bindings.get("x") instanceof Reference &&
            scope.isConstructor(getReferenceName(bindings.get("x")))) {
          mq.addMessage(
              JsRewriterMessageType.CONSTRUCTORS_ARE_NOT_FIRST_CLASS,
              this, node);
          return node;
        }
        return NONE;
      }
    });

    addRule(new Rule("varFuncFreeze") {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("@x", node, bindings) &&
            bindings.get("x") instanceof Reference) {
          String name = getReferenceName(bindings.get("x"));
          if (scope.isFunction(name)) {
            // TODO(ihab.awad): Figure out how to implement this without repeating varGlobal
            return (scope.isGlobal(name) || !scope.isDefined(name))  ?
                substV(
                  "___.primFreeze(___OUTERS___.@x)",
                  "x", bindings.get("x")) :
                substV(
                    "___.primFreeze(@x)",
                    "x", bindings.get("x"));
          }
        }
        return NONE;
      }
    });

    addRule(new Rule("varGlobal") {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("@x", node, bindings) &&
            bindings.get("x") instanceof Reference) {
          String name = getReferenceName(bindings.get("x"));
          if (scope.isGlobal(name) || !scope.isDefined(name)) {
            return subst(
                "___OUTERS___.@x", bindings
            );
          }
        }
        return NONE;
      }
    });

    addRule(new Rule("varDefault") {
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

    addRule(new Rule("readBadSuffix") {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("@x.@y__", node, bindings)) {
          mq.addMessage(
              JsRewriterMessageType.PROPERTIES_CANNOT_END_IN_DOUBLE_UNDERSCORE,
              this, node);
          return node;
        }
        return NONE;
      }
    });

    addRule(new Rule("readInternal") {
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

    addRule(new Rule("readBadInternal") {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("@x.@y_", node, bindings)) {
          mq.addMessage(
              JsRewriterMessageType.PUBLIC_PROPERTIES_CANNOT_END_IN_UNDERSCORE,
              this, node);
          return node;
        }
        return NONE;
      }
    });

    addRule(new Rule("readPublic") {
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

    addRule(new Rule("readIndexInternal") {
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

    addRule(new Rule("readIndexPublic") {
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

    addRule(new Rule("setBadSuffix") {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("@x.@y__ = @z", node, bindings)) {
          mq.addMessage(
              JsRewriterMessageType.PROPERTIES_CANNOT_END_IN_DOUBLE_UNDERSCORE,
              this, node);
          return node;
        }
        return NONE;
      }
    });

    addRule(new Rule("setInternal") {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("this.@p = @r", node, bindings)) {
          String propertyName = ((Reference)bindings.get("p")).getIdentifierName();
          return substV(
              "(function() {" +
              "  var x___ = @r;" +
              "  t___.@fp ? (t___.@p = x___) : ___.setProp(t___, @rp, x___);" +
              "})()",
              "r",  expand(bindings.get("r"), scope, mq),
              "p",  bindings.get("p"),
              "fp", new Reference(new Identifier(propertyName + "_canSet___")),
              "rp", new StringLiteral("'" + propertyName + "'"));
        }
        return NONE;
      }
    });

    addRule(new Rule("setMember") {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("@fname.prototype.@p = @m;", node, bindings)) {
          String fname = getReferenceName(bindings.get("fname"));
          if (scope.isDeclaredFunction(fname)) {
            String propertyName = getReferenceName(bindings.get("p"));
            if (!"constructor".equals(propertyName)) {
              return substV(
                  "(function() {" +
                  "  var x___ = @m;" +
                  "  ___.setMember(@fname, @rp, x___);" +
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

    addRule(new Rule("setBadInternal") {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("@x.@y_ = @z", node, bindings)) {
          mq.addMessage(
              JsRewriterMessageType.PUBLIC_PROPERTIES_CANNOT_END_IN_UNDERSCORE,
              this, node);
          return node;
        }
        return NONE;
      }
    });

    addRule(new Rule("setMemberMap") {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("@fname.prototype = @mm", node, bindings) &&
            scope.isFunction(getReferenceName(bindings.get("fname")))) {
          return substV(
              "___.setMemberMap(@fname, @mm)",
              "fname", expand(bindings.get("fname"), scope, mq),
              "mm", expandMemberMap(bindings.get("fname"), bindings.get("mm"), this, scope, mq));
        }
        return NONE;
      }
    });

    addRule(new Rule("setStatic") {
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

    addRule(new Rule("setPublic") {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("@o.@p = @r", node, bindings)) {
          String propertyName = ((Reference)bindings.get("p")).getIdentifierName();
          Pair<ParseTreeNode, ParseTreeNode> po =
              reuse("x___", bindings.get("o"), this, scope, mq);
          Pair<ParseTreeNode, ParseTreeNode> pr =
              reuse("x0___", bindings.get("r"), this, scope, mq);
          return substV(
              "(function() {" +
              "  @pob;" +
              "  @prb;" +
              "  @poa.@pCanSet ? (@poa.@p = @pra) : ___.setPub(@poa, @pName, @pra);" +
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

    addRule(new Rule("setIndexInternal") {
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

    addRule(new Rule("setIndexPublic") {
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

    addRule(new Rule("setInitialize") {
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

    addRule(new Rule("setDeclare") {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("var @v", node, bindings) &&
            !scope.isFunction(getIdentifierName(bindings.get("v")))) {
          return node;
        }
        return NONE;
      }
    });

    ////////////////////////////////////////////////////////////////////////
    // new - new object creation
    ////////////////////////////////////////////////////////////////////////

    addRule(new Rule("newCtor") {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("new @ctor(@as*)", node, bindings) &&
            scope.isDeclaredFunction(getReferenceName(bindings.get("ctor")))) {
          return substV(
              "new (___.asCtor(@ctor))(@as*)",
              "ctor", bindings.get("ctor"),
              "as", expandAll(bindings.get("as"), scope, mq));
        }
        return NONE;
      }
    });

    addRule(new Rule("newFunc") {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("new @f(@as*)", node, bindings)) {
          return substV(
              "new (___.asCtor(@f))(@as*)",
              "f", expand(bindings.get("f"), scope, mq),
              "as", expandAll(bindings.get("as"), scope, mq));
        }
        return NONE;
      }
    });

    ////////////////////////////////////////////////////////////////////////
    // call - function calls
    ////////////////////////////////////////////////////////////////////////

    addRule(new Rule("callBadSuffix") {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("@o.@s__(@as*)", node, bindings)) {
          mq.addMessage(
              JsRewriterMessageType.SELECTORS_CANNOT_END_IN_DOUBLE_UNDERSCORE,
              this, node);
          return node;
        }
        return NONE;
      }
    });

    addRule(new Rule("callInternal") {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("this.@m(@as*)", node, bindings)) {
          Pair<ParseTreeNode, ParseTreeNode> aliases =
              reuseAll(bindings.get("as"), this, scope, mq);
          String methodName = ((Reference)bindings.get("m")).getIdentifierName();
          return substV(
              "(function() {" +
              "  @as*;" +
              "  t___.@fm ? this.@m(@vs*) : ___.callProp(t___, @rm, [@vs*]);" +
              "})()",
              "as", aliases.b,
              "vs", aliases.a,
              "m",  bindings.get("m"),
              "fm", new Reference(new Identifier(methodName + "_canCall___")),
              "rm", new StringLiteral("'" + methodName + "'"));
        }
        return NONE;
      }
    });

    addRule(new Rule("callBadInternal") {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("@o.@s_(@as*)", node, bindings)) {
          mq.addMessage(
              JsRewriterMessageType.PUBLIC_SELECTORS_CANNOT_END_IN_UNDERSCORE,
              this, node);
          return node;
        }
        return NONE;
      }
    });

    addRule(new Rule("callCajaDef2") {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("caja.def(@fname, @base)", node, bindings) &&
            scope.isFunction(getReferenceName(bindings.get("fname"))) &&
            scope.isFunction(getReferenceName(bindings.get("base")))) {
          return subst(
              "caja.def(@fname, @base)", bindings
          );
        }
        return NONE;
      }
    });

    addRule(new Rule("callCajaDef3Plus") {
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
              expandMemberMap(bindings.get("fname"), bindings.get("ss"), this, scope, mq);
          return substV(
              "caja.def(@fname, @base, @mm, @ss?)",
              "fname", bindings.get("fname"),
              "base", bindings.get("base"),
              "mm", expandMemberMap(bindings.get("fname"), bindings.get("mm"), this, scope, mq),
              "ss", ss);
        }
        return NONE;
      }
    });

    addRule(new Rule("callPublic") {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("@o.@m(@as*)", node, bindings)) {
          Pair<ParseTreeNode, ParseTreeNode> aliases =
              reuseAll(bindings.get("as"), this, scope, mq);
          String methodName = ((Reference)bindings.get("m")).getIdentifierName();
          return substV(
              "(function() {" +
              "  var x___ = @o;" +
              "  @as*;" +
              "  x___.@fm ? x___.@m(@vs*) : ___.callPub(x___, @rm, [@vs*]);" +
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

    addRule(new Rule("callIndexInternal") {
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

    addRule(new Rule("callIndexPublic") {
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

    addRule(new Rule("callFunc") {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("@f(@as*)", node, bindings)) {
          expandEntries(bindings, scope, mq);
          return subst(
              "___.asSimpleFunc(@f)(@as*)", bindings
          );
        }
        return NONE;
      }
    });

    ////////////////////////////////////////////////////////////////////////
    // function - function definitions
    ////////////////////////////////////////////////////////////////////////

    addRule(new Rule("funcAnonSimple") {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        // Anonymous simple function constructor
        if (match("function(@ps*) { @bs*; }", node, bindings)) {
          Scope s2 = new Scope(scope, (FunctionConstructor)node);
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

    addRule(new Rule("funcNamedSimpleDecl") {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        // Named simple function declaration
        if (node.getClass() == FunctionDeclaration.class &&
            match("function @f(@ps*) { @bs*; }", node.children().get(1), bindings)) {
          Scope s2 = new Scope(scope, (FunctionConstructor)node.children().get(1));
          if (!s2.hasFreeThis()) {
            return expandDef(
                new Reference((Identifier)bindings.get("f")),
                substV(
                    "___.simpleFunc(" +
                    "  function(@ps*) {" +
                    "    @fh*;" +
                    "    @bs*;" +
                    "});",
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

    addRule(new Rule("funcNamedSimpleValue") {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        // Named simple function constructor
        if (match(getPatternNode("function @f(@ps*) { @bs* }"), node, bindings)) {
          Scope s2 = new Scope(scope, (FunctionConstructor)node);
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

    addRule(new Rule("funcBadMethod") {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("function(@ps*) { @bs*; }", node, bindings)) {
          Scope s2 = new Scope(scope, (FunctionConstructor)node);
          if (s2.hasFreeThis()) {
            mq.addMessage(
                JsRewriterMessageType. METHOD_IN_NON_METHOD_CONTEXT,
                this, node);
            return node;
          }
        }
        return NONE;
      }
    });

    addRule(new Rule("funcBadCtor") {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        // This catches a case where a named function is *not* part of a declaration.
        if (match("function @f(@ps*) { @bs*; }", node, bindings)) {
          Scope s2 = new Scope(scope, (FunctionConstructor)node);
          if (s2.hasFreeThis()) {
            mq.addMessage(
                JsRewriterMessageType.CONSTRUCTOR_CANNOT_ESCAPE,
                this, node);
            return node;
          }
        }
        return NONE;
      }
    });

    addRule(new Rule("funcDerivedCtorDecl") {
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
            if (!new Scope(scope, bindings.get("as")).hasFreeThis()) {
              Scope s2 = new Scope(scope, (FunctionConstructor)node.children().get(1));
              return expandDef(
                  new Reference((Identifier)bindings.get("f")),
                  substV(
                      "___.ctor(function(@ps*) {" +
                      "  @fh*;" +
                      "  @sf.Super.call(@th, @as*);" +
                      "  @bs*;" +
                      "});",
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

    addRule(new Rule("funcCtorDecl") {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (node instanceof FunctionDeclaration &&
            match("function @f(@ps*) { @bs*; }", node.children().get(1), bindings)) {
          Scope s2 = new Scope(scope, (FunctionConstructor)node.children().get(1));
          if (s2.hasFreeThis()) {
            return expandDef(
                new Reference((Identifier)bindings.get("f")),
                substV(
                    "___.ctor(function(@ps*) {" +
                    "  @fh*;" +
                    "  @bs*;" +
                    "});",
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

    addRule(new Rule("mapEmpty") {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("({})", node, bindings)) {
          return node.clone();
        }
        return NONE;
      }
    });

    addRule(new Rule("mapBadKeySuffix") {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("({@keys*: @vals*})", node, bindings) &&
            literalsEndWithUnderscores(bindings.get("keys"))) {
          mq.addMessage(
              JsRewriterMessageType.KEY_MAY_NOT_END_IN_UNDERSCORE,
              this, node);
          return node;
        }
        return NONE;
      }
    });

    addRule(new Rule("mapNonEmpty") {
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
    // other - things not otherwise covered
    ////////////////////////////////////////////////////////////////////////

    addRule(new Rule("otherInstanceof") {
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

    addRule(new Rule("otherBadInstanceof") {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match("@o instanceof @f", node, bindings)) {
          mq.addMessage(
              JsRewriterMessageType.INVOKED_INSTANCEOF_ON_NON_FUNCTION,
              this, node);
          return node;
        }
        return NONE;
      }
    });

    ////////////////////////////////////////////////////////////////////////
    // Automatically recurse into some structures
    ////////////////////////////////////////////////////////////////////////

    addRule(new Rule("recurse") {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        if (node instanceof ParseTreeNodeContainer ||
            node instanceof Block ||
            node instanceof CatchStmt ||
            node instanceof Conditional ||
            node instanceof ExpressionStmt ||
            node instanceof Identifier ||
            node instanceof Literal ||
            node instanceof Loop ||
            node instanceof Noop ||
            node instanceof Operation ||
            node instanceof ReturnStmt ||
            node instanceof ThrowStmt ||
            node instanceof TryStmt) {
          return expandAll(node, scope, mq);
        }
        return NONE;
      }
    });
  }
}
