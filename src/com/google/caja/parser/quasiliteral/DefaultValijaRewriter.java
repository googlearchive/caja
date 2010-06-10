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

import com.google.caja.lexer.FilePosition;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.ParseTreeNodeContainer;
import com.google.caja.parser.js.AssignOperation;
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.Declaration;
import com.google.caja.parser.js.Expression;
import com.google.caja.parser.js.ExpressionStmt;
import com.google.caja.parser.js.FunctionConstructor;
import com.google.caja.parser.js.FunctionDeclaration;
import com.google.caja.parser.js.Identifier;
import com.google.caja.parser.js.MultiDeclaration;
import com.google.caja.parser.js.Noop;
import com.google.caja.parser.js.ObjProperty;
import com.google.caja.parser.js.ObjectConstructor;
import com.google.caja.parser.js.Operation;
import com.google.caja.parser.js.Operator;
import com.google.caja.parser.js.QuotedExpression;
import com.google.caja.parser.js.Reference;
import com.google.caja.parser.js.RegexpLiteral;
import com.google.caja.parser.js.Statement;
import com.google.caja.parser.js.StringLiteral;
import com.google.caja.parser.js.SyntheticNodes;
import com.google.caja.parser.js.TryStmt;
import com.google.caja.parser.js.UncajoledModule;
import com.google.caja.parser.js.ValueProperty;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.util.Lists;

import java.util.List;
import java.util.Map;

/**
 * Rewrites a JavaScript parse tree to comply with default Valija rules.
 *
 * @author metaweta@gmail.com (Mike Stay)
 */
@RulesetDescription(
    name="Valija-to-Cajita Transformation Rules",
    synopsis="Default set of transformations used by Valija"
  )
public class DefaultValijaRewriter extends Rewriter {
  private int tempVarCount = 1;
  private final String tempVarPrefix = "$caja$";

  Reference newTempVar(Scope scope) {
    Identifier t = new Identifier(
        FilePosition.UNKNOWN, tempVarPrefix + tempVarCount++);
    scope.declareStartOfScopeVariable(t);
    return new Reference(t);
  }

  protected ParseTreeNode noexpandAll(ParseTreeNode node) {
    // TODO(erights): If we ever turn on taint checking for
    // DefaultValijaRewriter this needs to return a node (perhaps a defensive
    // copy) in which all taint has been removed.
    return node;
  }


  private final Rule[] valijaRules = {

    // See also rules in SyntheticRuleSet

    ////////////////////////////////////////////////////////////////////////
    // 'use strict,*'; pragmas
    ////////////////////////////////////////////////////////////////////////

    new Rule() {
      @Override
      @RuleDescription(
          name="cajitaUseSubset",
          synopsis="Skip subtrees with a 'use strict,cajita' declaration",
          reason="Valija rules should not be applied to embedded cajita code",
          // TODO(mikesamuel): check after Kona meeting
          matches="'use cajita'; @stmt*",
          substitutes="{ @stmt* }")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        if (node instanceof Block) {
          Map<String, ParseTreeNode> bindings = this.match(node);
          if (bindings != null) {
            // Do not descend into children.  Cajita nodes are exempt
            // from the Valija -> Cajita translation since they
            // presumably already contain Cajita.  If they do not
            // contain valid Cajita code, the CajitaRewriter will
            // complain.
            return substV("stmt", noexpandAll(bindings.get("stmt")));
          }
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="cajitaUseSubsetFnDecl",
          synopsis="Skip functions with a 'use strict,cajita' declaration",
          reason="Valija rules should not be applied to embedded cajita code",
          matches="/*outer*/function @name(@actuals*) { 'use cajita'; @body* }",
          substitutes="$v.so('@name', function @name(@actuals*) { @body* })")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        if (node instanceof FunctionDeclaration) {
          Map<String, ParseTreeNode> bindings = this.match(
              ((FunctionDeclaration) node).getInitializer());
          if (bindings != null) {
            // Do not expand children. See discussion in cajitaUseSubset above.
            // But we do want to make the name visible on outers, so expand the
            // declaration to an assignment to outers and hoist it to the top
            // of the block.
            Identifier fname = (Identifier) bindings.get("name");
            if (scope.isOuter((fname).getName())) {
              Expression initScope = (Expression) QuasiBuilder.substV(
                  "$v.initOuter('@name');", "name", fname);
              ParseTreeNodeContainer actuals = (ParseTreeNodeContainer) bindings.get("actuals");
              Expression initBlock = (Expression) substV(
                  "name", fname,
                  "actuals", noexpandParams(actuals),
                  "body", noexpandAll(bindings.get("body"))
              );
              scope.addStartOfScopeStatement(newExprStmt(initScope));
              scope.addStartStatement(newExprStmt(initBlock));
              return new Noop(node.getFilePosition());
            }
          }
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="cajitaUseSubsetFn",
          synopsis="Skip functions with a 'use strict,cajita' declaration",
          reason="Valija rules should not be applied to embedded cajita code",
          matches="function @i?(@actuals*) { 'use cajita'; @stmt* }",
          substitutes="function @i?(@actuals*) { @stmt* }")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        FunctionConstructor ctor;
        if (node instanceof FunctionDeclaration) {
          ctor = ((FunctionDeclaration) node).getInitializer();
        } else if (node instanceof FunctionConstructor) {
          ctor = (FunctionConstructor) node;
        } else {
          return NONE;
        }
        Map<String, ParseTreeNode> bindings = this.match(ctor);
        if (bindings != null) {
          // Do not expand children. See discussion in cajitaUseSubset above.
          Identifier iOpt = (Identifier) bindings.get("i");
          ParseTreeNodeContainer actuals = (ParseTreeNodeContainer) bindings.get("actuals");
          FunctionConstructor newCtor = (FunctionConstructor) substV(
              "i", null == iOpt ? null : noexpand(iOpt),
              "actuals", noexpandParams(actuals),
              "stmt", noexpandAll(bindings.get("stmt")));
          if (node instanceof FunctionDeclaration) {
            return new FunctionDeclaration(newCtor);
          } else {
            return newCtor;
          }
        }
        return NONE;
      }
    },

    // static module loading

    new Rule() {
      @Override
      @RuleDescription(
          name="staticModuleIncluding",
          synopsis="Replaced with the Cajita module loading",
          reason="",
          matches="includeScript(@arg)",
          substitutes="load(@arg)({$v: $v})")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = match(node);
        if (bindings != null && scope.isOuter("includeScript")) {
          ParseTreeNode arg = bindings.get("arg");
          if (arg instanceof StringLiteral) {
            return substV("arg", noexpand((StringLiteral) arg));
          } else {
            mq.addMessage(
                RewriterMessageType.CANNOT_LOAD_A_DYNAMIC_VALIJA_MODULE,
                node.getFilePosition());
            return node;
          }
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="staticCommonJsModuleLoading",
          synopsis="based on the Cajita module loading",
          reason="",
          matches="require(@arg)",
          substitutes="$v.cf($v.ro('require'), [load(@arg)])")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = match(node);
        if (bindings != null && scope.isOuter("includeScript")) {
          ParseTreeNode arg = bindings.get("arg");
          if (arg instanceof StringLiteral) {
            return substV("arg", noexpand((StringLiteral) arg));
          } else {
            mq.addMessage(
                RewriterMessageType.CANNOT_LOAD_A_DYNAMIC_SERVERJS_MODULE,
                node.getFilePosition());
            return node;
          }
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="dynamicCommonJsModuleLoading",
          synopsis="based on the Cajita module loading",
          reason="",
          matches="require.async(@arg)",
          substitutes="$v.cm($v.ro('require'), 'async', [load.async(@arg)])")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = match(node);
        if (bindings != null && scope.isOuter("includeScript")) {
          ParseTreeNode arg = bindings.get("arg");
          if (arg instanceof StringLiteral) {
            return substV("arg", noexpand((StringLiteral) arg));
          } else {
            mq.addMessage(
                RewriterMessageType.CANNOT_LOAD_A_DYNAMIC_SERVERJS_MODULE,
                node.getFilePosition());
            return node;
          }
        }
        return NONE;
      }
    },

    ////////////////////////////////////////////////////////////////////////
    // Module envelope
    ////////////////////////////////////////////////////////////////////////

    new Rule() {
      @Override
      @RuleDescription(
          name="moduleEnvelope",
          synopsis="Expand to a Caja module using an isolated scope.",
          reason="The 'module' rule should fire on the body of the module.",
          matches="<a Valija UncajoledModule>",
          substitutes="<a Caja UncajoledModule>")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        if (node instanceof UncajoledModule) {
          return expandAll(node, null);
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="module",
          synopsis="Assume an imported \"$v\" that knows our shared outers. " +
            "Name it $dis so top level uses of \"this\" in Valija work.",
          reason="",
          matches="@ss*;",
          substitutes="@startStmts*; @ss*;")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        if (node instanceof Block && scope == null) {
          Scope s2 = Scope.fromProgram((Block) node, mq);
          s2.addStartStatement(
              (Declaration) QuasiBuilder.substV("var $dis = $v.getOuters();"));
          s2.addStartStatement(newExprStmt(
              (Expression) QuasiBuilder.substV("$v.initOuter('onerror');")));

          List<ParseTreeNode> expanded = Lists.newArrayList();
          for (ParseTreeNode c : node.children()) {
            expanded.add(expand(c, s2));
          }
          return substV(
              "startStmts", new ParseTreeNodeContainer(s2.getStartStatements()),
              "ss", new ParseTreeNodeContainer(expanded));
        }
        return NONE;
      }
    },

    ////////////////////////////////////////////////////////////////////////
    // Support hoisting of functions to the top of their containing block
    ////////////////////////////////////////////////////////////////////////

    new Rule() {
      @Override
      @RuleDescription(
          name="block",
          synopsis="Initialize named functions at the beginning of their enclosing block.",
          reason="Nested named function declarations are illegal in ES3 but are universally " +
            "supported by all JavaScript implementations, though in different ways. " +
            "The compromise semantics currently supported by Caja is to hoist the " +
            "declaration of a variable with the function's name to the beginning of " +
            "the enclosing function body or module top level, and to initialize " +
            "this variable to a new anonymous function every time control re-enters " +
            "the enclosing block." +
            "\n" +
            "Note that ES-Harmony will specify a better and safer semantics -- block " +
            "level lexical scoping -- that we'd like to adopt into Caja eventually. " +
            "However, it so challenging to implement this semantics by " +
            "translation to currently-implemented JavaScript that we provide " +
            "something quicker and dirtier for now.",
          matches="{@ss*;}",
          substitutes="@startStmts*; @ss*;")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        if (node instanceof Block) {
          List<ParseTreeNode> expanded = Lists.newArrayList();
          Scope s2 = Scope.fromPlainBlock(scope);
          for (ParseTreeNode c : node.children()) {
            expanded.add(expand(c, s2));
          }
          return substV(
              "startStmts", new ParseTreeNodeContainer(s2.getStartStatements()),
              "ss", new ParseTreeNodeContainer(expanded));
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
          name="foreachExpr",
          synopsis="Get the keys, then iterate over them.",
          reason="",
          matches="for (@k in @o) @ss;",
          substitutes="@t1 = $v.keys(@o);\n" +
                      "for (@t2 = 0; @t2 < @t1.length; ++@t2) {\n" +
                      "  @assign;\n" +
                      "  @ss;\n" +
                      "}\n" +
                      "/* where @assign is the expansion of\n" +
                      "@k = QuotedExpression( @t1[@t2] ); */")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = this.match(node);
        if (bindings != null &&
            bindings.get("k") instanceof ExpressionStmt) {
          ExpressionStmt es = (ExpressionStmt) bindings.get("k");
          bindings.put("k", es.getExpression());

          Reference rt1 = newTempVar(scope);
          Reference rt2 = newTempVar(scope);

          ParseTreeNode assignment = QuasiBuilder.substV(
              "@k = @t3;",
              "k", bindings.get("k"),
              "t3", new QuotedExpression((Expression) QuasiBuilder.substV(
                  "@t1[@t2]",
                  "t1", rt1,
                  "t2", rt2)));
          assignment.getAttributes().set(ParseTreeNode.TAINTED, true);

          Expression assign = (Expression) expand(assignment, scope);
          return substV(
              "t1", rt1,
              "o", expand(bindings.get("o"), scope),
              "t2", rt2,
              "assign", SyntheticNodes.s(newExprStmt(assign)),
              "ss", expand(bindings.get("ss"), scope));
        } else {
          return NONE;
        }
      }
    },

    new Rule () {
      @Override
      @RuleDescription(
          name="foreach",
          synopsis="Get the keys, then iterate over them.",
          reason="",
          matches="for (var @k in @o) @ss;",
          substitutes="@t1 = $v.keys(@o);\n" +
                      "for (@t2 = 0; @t2 < @t1.length; ++@t2) {\n" +
                      "  @assign\n" +
                      "  @ss;\n" +
                      "}\n" +
                      "/* where @assign is the expansion of\n" +
                      "var @k = QuotedExpression( @t1[@t2] ); */")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = this.match(node);
        if (bindings != null) {
          Reference rt1 = newTempVar(scope);
          Reference rt2 = newTempVar(scope);

          ParseTreeNode assignment = QuasiBuilder.substV(
              "var @k = @t3;",
              "k", bindings.get("k"),
              "t3", new QuotedExpression((Expression) QuasiBuilder.substV(
                  "@t1[@t2]",
                  "t1", rt1,
                  "t2", rt2)));

          assignment.getAttributes().set(ParseTreeNode.TAINTED, true);

          return substV(
              "t1", rt1,
              "o", expand(bindings.get("o"), scope),
              "t2", rt2,
              "assign", SyntheticNodes.s(expand(assignment, scope)),
              "ss", expand(bindings.get("ss"), scope));
        } else {
          return NONE;
        }
      }
    },

    ////////////////////////////////////////////////////////////////////////
    // try - try/catch/finally constructs
    ////////////////////////////////////////////////////////////////////////

    new Rule() {
      @Override
      @RuleDescription(
          name="tryCatch",
          synopsis="",
          reason="",
          matches="try { @s0*; } catch (@x) { @s1*; }",
          substitutes="try { @s0*; } catch (@x) { @rx = $v.tr(@rx); @s1*; }")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = match(node);
        if (bindings != null) {
          TryStmt t = (TryStmt) node;
          Identifier x = (Identifier) bindings.get("x");
          return substV(
              "s0", expandAll(bindings.get("s0"), scope),
              "x", noexpand(x),
              "rx", new Reference(x),
              "s1", expandAll(bindings.get("s1"),
                              Scope.fromCatchStmt(scope, t.getCatchClause())));
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="tryCatchFinally",
          synopsis="",
          reason="",
          matches="try { @s0*; } catch (@x) { @s1*; } finally { @s2*; }",
          substitutes="try { @s0*; } catch (@x) { @rx = $v.tr(@rx); @s1*; } finally { @s2*; }")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = match(node);
        if (bindings != null) {
          TryStmt t = (TryStmt) node;
          Identifier x = (Identifier) bindings.get("x");
          return substV(
              "s0", expandAll(bindings.get("s0"), scope),
              "x", noexpand(x),
              "rx", new Reference(x),
              "s1", expandAll(bindings.get("s1"),
                              Scope.fromCatchStmt(scope, t.getCatchClause())),
              "s2", expandAll(bindings.get("s2"), scope));
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="tryFinally",
          synopsis="",
          reason="",
          matches="try { @s0*; } finally { @s1*; }",
          substitutes="try { @s0*; } finally { @s1*; }")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = match(node);
        if (bindings != null) {
          return substV(
            "s0", expandAll(bindings.get("s0"), scope),
            "s1", expandAll(bindings.get("s1"), scope));
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="throw",
          synopsis="",
          reason="",
          matches="throw @ex",
          substitutes="throw $v.ts(@ex)")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        return transform(node, scope);
      }
    },

    ////////////////////////////////////////////////////////////////////////
    // variable - variable name handling
    ////////////////////////////////////////////////////////////////////////

    new Rule() {
      @Override
      @RuleDescription(
          name="this",
          synopsis="Replace all occurrences of \"this\" with $dis.",
          reason="",
          matches="this",
          substitutes="$dis")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = this.match(node);
        if (bindings != null) {
          return newReference(node.getFilePosition(), ReservedNames.DIS);
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="initGlobalVar",
          synopsis="",
          reason="",
          matches="/* in outer scope */ var @v = @r",
          substitutes="$v.so('@v', @r)")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = this.match(node);
        if (bindings != null) {
          Identifier v = (Identifier) bindings.get("v");
          String vname = v.getName();
          if (scope.isOuter(vname)) {
            ParseTreeNode r = bindings.get("r");
            return newExprStmt((Expression) substV(
                "v", v,
                "r", expand(nymize(r, vname, "var"), scope)));
          }
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="setGlobalVar",
          synopsis="",
          reason="",
          matches="/* declared in outer scope */ @v = @r",
          substitutes="$v.so('@v', @r)")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = this.match(node);
        if (bindings != null) {
          ParseTreeNode v = bindings.get("v");
          if (v instanceof Reference) {
            String vname = ((Reference) v).getIdentifierName();
            if (scope.isOuter(vname)) {
              ParseTreeNode r = bindings.get("r");
              return substV(
                  "v", v,
                  "r", expand(nymize(r, vname, "var"), scope));
            }
          }
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="declGlobalVar",
          synopsis="",
          reason="",
          matches="/* in outer scope */ var @v",
          substitutes="$v.initOuter('@v')")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = this.match(node);
        if (bindings != null &&
            bindings.get("v") instanceof Identifier &&
            scope.isOuter(((Identifier) bindings.get("v")).getName())) {
          ExpressionStmt es = newExprStmt(
              (Expression) substV("v", bindings.get("v")));
          es.getAttributes().set(CajitaRewriter.FOR_SIDE_EFFECT, true);
          return es;
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="readArguments",
          synopsis="Translate reference to 'arguments' unmodified",
          reason="",
          matches="arguments",
          substitutes="Array.slice(arguments,1)")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = this.match(node);
        if (bindings != null) {
          return substV();
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="readGlobalVar",
          synopsis="",
          reason="",
          matches="/* declared in outer scope */ @v",
          substitutes="$v.ro('@v')")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = this.match(node);
        if (bindings != null && bindings.get("v") instanceof Reference) {
          Reference v = (Reference) bindings.get("v");
          if (scope.isOuter(v.getIdentifierName())) {
            return substV("v", noexpand(v));
          }
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="initLocalVar",
          synopsis="",
          reason="",
          matches="/* not in outer scope */ var @v = @r",
          substitutes="var @v = @r")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = this.match(node);
        if (bindings != null) {
          Identifier v = (Identifier) bindings.get("v");
          String vname = v.getName();
          if (!scope.isOuter(vname)) {
            ParseTreeNode r = bindings.get("r");
            return substV(
                "v", v,
                "r", expand(nymize(r, vname, "var"), scope));
          }
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="setLocalVar",
          synopsis="",
          reason="",
          matches="/* not in outer scope */ @v = @r",
          substitutes="@v = @r")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = this.match(node);
        if (bindings != null) {
          ParseTreeNode v = bindings.get("v");
          if (v instanceof Reference) {
            String vname = ((Reference) v).getIdentifierName();
            if (!scope.isOuter(vname)) {
              ParseTreeNode r = bindings.get("r");
              return substV(
                  "v", v,
                  "r", expand(nymize(r, vname, "var"), scope));
            }
          }
        }
        return NONE;
      }
    },

    ////////////////////////////////////////////////////////////////////////
    // read - reading properties
    ////////////////////////////////////////////////////////////////////////

    new Rule() {
      @Override
      @RuleDescription(
          name="readPublic",
          synopsis="Read @'p' from @o or @o's POE table",
          reason="",
          matches="@o.@p",
          substitutes="$v.r(@o, '@p')")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = this.match(node);
        if (bindings != null) {
          Reference p = (Reference) bindings.get("p");
          return substV(
              "o", expand(bindings.get("o"), scope),
              "p", p);
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="readIndexPublic",
          synopsis="Read @p from @o or @o's POE table",
          reason="",
          matches="@o[@p]",
          substitutes="$v.r(@o, @p)")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = this.match(node);
        if (bindings != null) {
          return substV(
              "o", expand(bindings.get("o"), scope),
              "p", expand(bindings.get("p"), scope));
        }
        return NONE;
      }
    },

    ////////////////////////////////////////////////////////////////////////
    // set - assignments
    ////////////////////////////////////////////////////////////////////////

    new Rule() {
      @Override
      @RuleDescription(
          name="setPublic",
          synopsis="Set @'p' on @o or @o's POE table",
          reason="",
          matches="@o.@p = @r",
          substitutes="$v.s(@o, '@p', @r)")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = this.match(node);
        if (bindings != null) {
          ParseTreeNode o = bindings.get("o");
          Reference p = (Reference) bindings.get("p");
          ParseTreeNode r = bindings.get("r");
          return substV(
              "o", expand(o, scope),
              "p", p,
              "r", expand(nymize(r, p.getIdentifierName(), "meth"), scope));
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="setIndexPublic",
          synopsis="Set @p on @o or @o's POE table",
          reason="",
          matches="@o[@p] = @r",
          substitutes="$v.s(@o, @p, @r)")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = this.match(node);
        if (bindings != null) {
          return substV(
              "o", expand(bindings.get("o"), scope),
              "p", expand(bindings.get("p"), scope),
              "r", expand(bindings.get("r"), scope));
        }
        return NONE;
      }
    },

    // TODO(erights): Need a general way to expand readModifyWrite lValues.
    // For now, we're just picking off a few common special cases as they
    // come up.

    new Rule() {
      @Override
      @RuleDescription(
          name="setReadModifyWriteLocalVar",
          synopsis="",
          reason="",
          matches="<approx> @x @op= @y",  // TODO(mikesamuel): better lower limit
          substitutes="<approx> @x = @x @op @y")
      // Handle x += 3 and similar ops by rewriting them using the assignment
      // delegate, "x += y" => "x = x + y", with deconstructReadAssignOperand
      // assuring that x is evaluated at most once where that matters.
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        if (node instanceof AssignOperation) {
          AssignOperation aNode = (AssignOperation) node;
          Operator op = aNode.getOperator();
          if (op.getAssignmentDelegate() == null) { return NONE; }

          ReadAssignOperands ops = deconstructReadAssignOperand(
              aNode.children().get(0), scope, false);
          if (ops == null) { return node; }  // Error deconstructing

          // For x += 3, rhs is (x + 3)
          Operation rhs = Operation.create(
              aNode.children().get(0).getFilePosition(),
              op.getAssignmentDelegate(),
              ops.getUncajoledLValue(), aNode.children().get(1));
          Operation assignment = ops.makeAssignment(rhs);
          if (ops.getTemporaries().isEmpty()) {
            return expand(assignment, scope);
          } else {
            return QuasiBuilder.substV(
                "@tmps, @assignment",
                "tmps", newCommaOperation(ops.getTemporaries()),
                "assignment", expand(assignment, scope));
          }
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="setIncrDecr",
          synopsis="Handle pre and post ++ and --.",
          // TODO(mikesamuel): better lower bound
          matches="<approx> ++@x but any {pre,post}{in,de}crement will do",
          reason="")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        if (!(node instanceof AssignOperation)) { return NONE; }
        AssignOperation op = (AssignOperation) node;
        Expression v = op.children().get(0);
        ReadAssignOperands ops = deconstructReadAssignOperand(v, scope, false);
        if (ops == null) { return node; }

        // TODO(mikesamuel): Figure out when post increments are being
        // used without use of the resulting value and switch them to
        // pre-increments.
        switch (op.getOperator()) {
          case POST_INCREMENT:
            if (ops.isSimpleLValue()) {
              return QuasiBuilder.substV("@v ++", "v", ops.getCajoledLValue());
            } else {
              Reference tmpVal = new Reference(
                  scope.declareStartOfScopeTempVariable());
              Expression assign = (Expression) expand(
                  ops.makeAssignment((Expression) QuasiBuilder.substV(
                      "@tmpVal + 1", "tmpVal", tmpVal)),
                  scope);
              return QuasiBuilder.substV(
                  "  @tmps,"
                  + "@tmpVal = +@rvalue,"  // Coerce to a number.
                  + "@assign,"  // Assign value.
                  + "@tmpVal",
                  "tmps", newCommaOperation(ops.getTemporaries()),
                  "tmpVal", tmpVal,
                  "rvalue", ops.getCajoledLValue(),
                  "assign", assign);
            }
          case PRE_INCREMENT:
            // We subtract -1 instead of adding 1 since the - operator coerces
            // to a number in the same way the ++ operator does.
            if (ops.isSimpleLValue()) {
              return QuasiBuilder.substV("++@v", "v", ops.getCajoledLValue());
            } else if (ops.getTemporaries().isEmpty()) {
              return expand(
                  ops.makeAssignment((Expression) QuasiBuilder.substV(
                      "@rvalue - -1",
                      "rvalue", ops.getUncajoledLValue())),
                  scope);
            } else {
              return QuasiBuilder.substV(
                  "  @tmps,"
                  + "@assign",
                  "tmps", newCommaOperation(ops.getTemporaries()),
                  "assign", expand(
                      ops.makeAssignment((Expression) QuasiBuilder.substV(
                          "@rvalue - -1",
                          "rvalue", ops.getUncajoledLValue())),
                      scope));
            }
          case POST_DECREMENT:
            if (ops.isSimpleLValue()) {
              return QuasiBuilder.substV("@v--", "v", ops.getCajoledLValue());
            } else {
              Reference tmpVal = new Reference(
                  scope.declareStartOfScopeTempVariable());
              Expression assign = (Expression) expand(
                  ops.makeAssignment((Expression) QuasiBuilder.substV(
                      "@tmpVal - 1", "tmpVal", tmpVal)),
                  scope);
              return QuasiBuilder.substV(
                  "  @tmps,"
                  + "@tmpVal = +@rvalue,"  // Coerce to a number.
                  + "@assign,"  // Assign value.
                  + "@tmpVal;",
                  "tmps", newCommaOperation(ops.getTemporaries()),
                  "tmpVal", tmpVal,
                  "rvalue", ops.getCajoledLValue(),
                  "assign", assign);
            }
          case PRE_DECREMENT:
            if (ops.isSimpleLValue()) {
              return QuasiBuilder.substV("--@v", "v", ops.getCajoledLValue());
            } else if (ops.getTemporaries().isEmpty()) {
              return expand(
                  ops.makeAssignment((Expression) QuasiBuilder.substV(
                      "@rvalue - 1", "rvalue",
                      ops.getUncajoledLValue())),
                  scope);
            } else {
              return QuasiBuilder.substV(
                  "  @tmps,"
                  + "@assign",
                  "tmps", newCommaOperation(ops.getTemporaries()),
                  "assign", expand(
                      ops.makeAssignment((Expression) QuasiBuilder.substV(
                          "@rvalue - 1",
                          "rvalue", ops.getUncajoledLValue())),
                      scope));
            }
          default:
            return NONE;
        }
      }
    },

    ////////////////////////////////////////////////////////////////////////
    // new - new object creation
    ////////////////////////////////////////////////////////////////////////

    new Rule() {
      @Override
      @RuleDescription(
          name="constructNoArgs",
          synopsis="Construct a new object and supply the missing empty argument list.",
          reason="",
          matches="new @c",
          substitutes="$v.construct(@c, [])")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = this.match(node);
        if (bindings != null) {
          return substV("c", expand(bindings.get("c"), scope));
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="construct",
          synopsis="Construct a new object.",
          reason="",
          matches="new @c(@as*)",
          substitutes="$v.construct(@c, [@as*])")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = this.match(node);
        if (bindings != null) {
          return substV(
              "c", expand(bindings.get("c"), scope),
              "as", expand(bindings.get("as"), scope));
        }
        return NONE;
      }
    },

    ////////////////////////////////////////////////////////////////////////
    // delete - property deletion
    ////////////////////////////////////////////////////////////////////////

    new Rule() {
      @Override
      @RuleDescription(
          name="deletePublic",
          synopsis="Delete a statically known property of an object.",
          reason="",
          matches="delete @o.@p",
          substitutes="$v.remove(@o, '@p')")
          public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = this.match(node);
        if (bindings != null) {
          Reference p = (Reference) bindings.get("p");
          return substV(
              "o", expand(bindings.get("o"), scope),
              "p", p);
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="deleteIndexPublic",
          synopsis="Delete a dynamically chosen property of an object.",
          reason="",
          matches="delete @o[@p]",
          substitutes="$v.remove(@o, @p)")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = this.match(node);
        if (bindings != null) {
          return substV(
              "o", expand(bindings.get("o"), scope),
              "p", expand(bindings.get("p"), scope));
        }
        return NONE;
      }
    },

    ////////////////////////////////////////////////////////////////////////
    // call - function calls
    ////////////////////////////////////////////////////////////////////////

    new Rule() {
      @Override
      @RuleDescription(
          name="callNamed",
          synopsis="Call a property with a statically known name.",
          reason="",
          matches="@o.@p(@as*)",
          substitutes="$v.cm(@o, '@p', [@as*])")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = this.match(node);
        if (bindings != null) {
          Reference p = (Reference) bindings.get("p");
          List<ParseTreeNode> expanded = Lists.newArrayList();
          ParseTreeNodeContainer args = (ParseTreeNodeContainer)bindings.get("as");
          for (ParseTreeNode c : args.children()) {
            expanded.add(expand(c, scope));
          }
          return substV(
              "o", expand(bindings.get("o"), scope),
              "p", p,
              "as", new ParseTreeNodeContainer(expanded));
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="callMethod",
          synopsis="Call a property with a computed name.",
          reason="",
          matches="@o[@p](@as*)",
          substitutes="$v.cm(@o, @p, [@as*])")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = this.match(node);
        if (bindings != null) {
          List<ParseTreeNode> expanded = Lists.newArrayList();
          ParseTreeNodeContainer args = (ParseTreeNodeContainer)bindings.get("as");
          for (ParseTreeNode c : args.children()) {
            expanded.add(expand(c, scope));
          }
          return substV(
              "o", expand(bindings.get("o"), scope),
              "p", expand(bindings.get("p"), scope),
              "as", new ParseTreeNodeContainer(expanded));
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="callFunc",
          synopsis="Call a function.",
          reason="",
          matches="@f(@as*)",
          substitutes="$v.cf(@f, [@as*])")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = this.match(node);
        if (bindings != null) {
          List<ParseTreeNode> expanded = Lists.newArrayList();
          ParseTreeNodeContainer args = (ParseTreeNodeContainer)bindings.get("as");
          for (ParseTreeNode c : args.children()) {
            expanded.add(expand(c, scope));
          }
          return substV(
              "f", expand(bindings.get("f"), scope),
              "as", new ParseTreeNodeContainer(expanded));
        }
        return NONE;
      }
    },

    ////////////////////////////////////////////////////////////////////////
    // function - function definitions
    ////////////////////////////////////////////////////////////////////////

    new Rule() {
      @Override
      @RuleDescription(
          name="disfuncAnon",
          synopsis="Transmutes functions into disfunctions.",
          reason="",
          matches="function (@ps*) {@bs*;}",
          substitutes="$v.dis(function ($dis, @ps*) {@stmts*; @bs*;})")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = this.match(node);
        if (bindings != null) {
          Scope s2 = Scope.fromFunctionConstructor(scope, (FunctionConstructor)node);
          return substV(
              "ps", bindings.get("ps"),
              // It's important to expand bs before computing stmts.
              "bs", expand(bindings.get("bs"), s2),
              "stmts", new ParseTreeNodeContainer(s2.getStartStatements()));
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="disfuncNamedGlobalDecl",
          synopsis="Transmutes functions into disfunctions and hoists them.",
          reason="",
          matches="/* at top level */ function @f(@ps*) {@bs*;}",
          substitutes=(
              "$v.so('@f', (function () {" +
              "  var @f;" +
              "  function @fcaller($dis, @ps*) {" +
              "    @stmts*;" +
              "    @bs*;" +
              "  }" +
              "  @rf = $v.dis(@rfcaller, '@f');" +
              "  return @rf;" +
              "})());"))
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        // Named simple function declaration
        if (node instanceof FunctionDeclaration) {
          FunctionConstructor c = ((FunctionDeclaration) node).getInitializer();
          Map<String, ParseTreeNode> bindings = match(c);
          if (bindings != null
              && scope.isOuter(((Identifier) bindings.get("f")).getName())) {
            Scope s2 = Scope.fromFunctionConstructor(scope, c);
            checkFormals(bindings.get("ps"));
            Identifier f = (Identifier) bindings.get("f");
            Identifier fcaller = new Identifier(
                f.getFilePosition(), nym(node, f.getName(), "caller"));
            Expression expr = (Expression) substV(
                "f", f,
                "rf", new Reference(f),
                "fcaller", fcaller,
                "rfcaller", new Reference(fcaller),
                "ps", bindings.get("ps"),
                // It's important to expand bs before computing stmts.
                "bs", expand(bindings.get("bs"), s2),
                "stmts", new ParseTreeNodeContainer(s2.getStartStatements()));
            scope.addStartStatement(newExprStmt(expr));
            return QuasiBuilder.substV(";");
          }
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="disfuncNamedDecl",
          synopsis="Transmutes functions into disfunctions.",
          reason="",
          matches="function @fname(@ps*) {@bs*;}",
          substitutes=(
              "function @fcaller($dis, @ps*) {" +
              "    @stmts*;" +
              "    @bs*;" +
              "}" +
              "@fname = $v.dis(@rfcaller, '@fname');"))
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = makeBindings();
        // Named simple function declaration
        if (node instanceof FunctionDeclaration &&
            QuasiBuilder.match(
                "function @fname(@ps*) { @bs*; }",
                ((FunctionDeclaration) node).getInitializer(), bindings)) {
          Scope s2 = Scope.fromFunctionConstructor(
              scope,
              (FunctionConstructor) node.children().get(1));
          checkFormals(bindings.get("ps"));
          Identifier fname = (Identifier) bindings.get("fname");
          Identifier fcaller = new Identifier(
              FilePosition.UNKNOWN, nym(node, fname.getName(), "caller"));
          scope.declareStartOfScopeVariable(fname);
          Block block = (Block) substV(
              "fname", new Reference(fname),
              "fcaller", fcaller,
              "rfcaller", new Reference(fcaller),
              "ps", bindings.get("ps"),
              // It's important to expand bs before computing stmts.
              "bs", expand(bindings.get("bs"), s2),
              "stmts", new ParseTreeNodeContainer(s2.getStartStatements()));
          for (Statement stat : block.children()) {
            scope.addStartStatement(stat);
          }
          return QuasiBuilder.substV(";");
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="disfuncNamedValue",
          synopsis="",
          reason="",
          matches="function @fname(@ps*) { @bs*; }",
          substitutes=(
              "(function() {" +
              "  function @fcaller($dis, @ps*) {" +
              "    @stmts*;" +
              "    @bs*;" +
              "  }" +
              "  var @fname = $v.dis(@rfcaller, '@fname');" +
              "  return @fRef;" +
              "})();"))
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        // Named simple function expression
        Map<String, ParseTreeNode> bindings = this.match(node);
        if (bindings != null) {
          Scope s2 = Scope.fromFunctionConstructor(
              scope,
              (FunctionConstructor)node);
          checkFormals(bindings.get("ps"));
          Identifier fname = (Identifier) bindings.get("fname");
          Identifier fcaller = new Identifier(
              FilePosition.UNKNOWN, nym(node, fname.getName(), "caller"));
          return substV(
              "fname", fname,
              "fRef", new Reference(fname),
              "fcaller", fcaller,
              "rfcaller", new Reference(fcaller),
              "ps", bindings.get("ps"),
              // It's important to expand bs before computing stmts.
              "bs", expand(bindings.get("bs"), s2),
              "stmts", new ParseTreeNodeContainer(s2.getStartStatements()));
        }
        return NONE;
      }
    },

    ////////////////////////////////////////////////////////////////////////
    // multiDeclaration - multiple declarations
    ////////////////////////////////////////////////////////////////////////

    new Rule() {
      @Override
      @RuleDescription(
          name="multiDeclaration",
          synopsis="Convert a MultiDeclaration into a comma expression",
          reason="")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        if (node instanceof MultiDeclaration
            && scope.isOuter()) {
          List <Expression> newChildren = Lists.newArrayList();
          for (int i = 0, len = node.children().size(); i < len; i++) {
            ExpressionStmt result = (ExpressionStmt)
                expand(node.children().get(i), scope);
            newChildren.add(i, result.getExpression());
          }
          return newExprStmt(newCommaOperation(newChildren));
        }
        return NONE;
      }
    },

    ////////////////////////////////////////////////////////////////////////
    // map - object literals
    ////////////////////////////////////////////////////////////////////////

    new Rule() {
      @Override
      @RuleDescription(
          name="oneprop",
          synopsis="",
          reason="",
          matches="\"@key\": @val",
          substitutes="\"@key\": @val")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        if (node instanceof ObjProperty) {
          if (node instanceof ValueProperty) {
            ValueProperty prop = (ValueProperty) node;
            StringLiteral key = prop.getPropertyNameNode();
            Expression val = prop.getValueExpr();
            return new ValueProperty(
                noexpand(key),
                (Expression) expand(
                    nymize(val, key.getUnquotedValue(), "lit"), scope));
          } else {
            mq.addMessage(
                RewriterMessageType.GETTERS_SETTERS_NOT_SUPPORTED,
                node.getFilePosition(), this);
            return node;
          }
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="map",
          synopsis="",
          reason="",
          matches="({@keys*: @val*})",
          substitutes="({@keys*: @vals*})")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        if (node instanceof ObjectConstructor) {
          List<? extends ObjProperty> props = ((ObjectConstructor) node)
              .children();
          List<ObjProperty> outProps = Lists.newArrayList();
          for (ObjProperty prop : props) {
            ObjProperty expandedProp = (ObjProperty) expand(prop, scope);
            outProps.add(expandedProp);
          }
          return new ObjectConstructor(FilePosition.UNKNOWN, outProps);
        }
        return NONE;
      }
    },

    ////////////////////////////////////////////////////////////////////////
    // other - things not otherwise covered
    ////////////////////////////////////////////////////////////////////////

    new Rule() {
      @Override
      @RuleDescription(
          name="outerTypeof",
          synopsis="typeof of a global reference.",
          reason="Typeof should not throw an error for undefined outers",
          matches="typeof /* global reference */ @f",
          substitutes="$v.typeOf($v.ros('@f'))")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = this.match(node);
        if (bindings != null) {
          ParseTreeNode f = bindings.get("f");
          if (f instanceof Reference) {
            Reference fRef = (Reference) f;
            if (scope.isOuter(fRef.getIdentifierName())) {
              return substV("f", noexpand(fRef));
            }
          }
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="otherTypeof",
          synopsis="Rewrites typeof.",
          reason="Both typeof function and typeof disfunction need to return \"function\".",
          matches="typeof @f",
          substitutes="$v.typeOf(@f)")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = this.match(node);
        if (bindings != null) {
          return substV("f", expand(bindings.get("f"), scope));
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="otherInstanceof",
          synopsis="Rewrites instanceof.",
          reason="Need to check both the shadow prototype chain and the real one.",
          matches="@o instanceof @f",
          substitutes="$v.instanceOf(@o, @f)")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = this.match(node);
        if (bindings != null) {
          return substV(
              "o", expand(bindings.get("o"), scope),
              "f", expand(bindings.get("f"), scope));
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="inPublic",
          synopsis="",
          reason="",
          matches="@i in @o",
          substitutes="$v.canReadRev(@i, @o)")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = this.match(node);
        if (bindings != null) {
          return substV(
              "i", expand(bindings.get("i"), scope),
              "o", expand(bindings.get("o"), scope));
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="regexLiteral",
          synopsis="Use the regular expression constructor",
          reason="So that every use of a regex literal creates a new instance"
               + " to prevent state from leaking via interned literals.  This"
               + " is consistent with the way ES4 treates regex literals.",
          substitutes="$v.construct(RegExp, [@pattern, @modifiers?])")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        if (node instanceof RegexpLiteral) {
          RegexpLiteral.RegexpWrapper re = ((RegexpLiteral) node).getValue();
          FilePosition pos = node.getFilePosition();
          StringLiteral pattern = StringLiteral.valueOf(pos, re.getMatchText());
          StringLiteral modifiers = !"".equals(re.getModifiers())
              ? StringLiteral.valueOf(pos, re.getModifiers()) : null;
          return substV(
              "pattern", pattern,
              "modifiers", modifiers);
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="unquote",
          synopsis="Removes a QuotedExpression wrapper.",
          reason="")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
         if (node instanceof QuotedExpression) {
           return ((QuotedExpression) node).unquote();
         }
         return NONE;
      }
    },

    ////////////////////////////////////////////////////////////////////////
    // recurse - automatically recurse into remaining structures
    ////////////////////////////////////////////////////////////////////////

    new Rule() {
      @Override
      @RuleDescription(
          name="recurse",
          synopsis="Automatically recurse into any remaining structures",
          reason="")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        return expandAll(node, scope);
      }
    }
  };

  /**
   * Creates a default valija rewriter with logging off.
   */
  public DefaultValijaRewriter(MessageQueue mq) {
    this(mq, false);
  }

  public DefaultValijaRewriter(MessageQueue mq, boolean logging) {
    super(mq, false, logging);
    addRules(SyntheticRuleSet.syntheticRules(this));
    addRules(valijaRules);
  }

}
