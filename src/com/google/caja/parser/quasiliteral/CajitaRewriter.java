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

import com.google.caja.SomethingWidgyHappenedError;
import com.google.caja.lexer.FilePosition;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.ParseTreeNodeContainer;
import com.google.caja.parser.js.ArrayConstructor;
import com.google.caja.parser.js.AssignOperation;
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.BreakStmt;
import com.google.caja.parser.js.CajoledModule;
import com.google.caja.parser.js.CaseStmt;
import com.google.caja.parser.js.Conditional;
import com.google.caja.parser.js.ContinueStmt;
import com.google.caja.parser.js.ControlOperation;
import com.google.caja.parser.js.DebuggerStmt;
import com.google.caja.parser.js.Declaration;
import com.google.caja.parser.js.DefaultCaseStmt;
import com.google.caja.parser.js.Expression;
import com.google.caja.parser.js.ExpressionStmt;
import com.google.caja.parser.js.FormalParam;
import com.google.caja.parser.js.FunctionConstructor;
import com.google.caja.parser.js.FunctionDeclaration;
import com.google.caja.parser.js.Identifier;
import com.google.caja.parser.js.IntegerLiteral;
import com.google.caja.parser.js.LabeledStatement;
import com.google.caja.parser.js.LabeledStmtWrapper;
import com.google.caja.parser.js.Literal;
import com.google.caja.parser.js.Loop;
import com.google.caja.parser.js.MultiDeclaration;
import com.google.caja.parser.js.Noop;
import com.google.caja.parser.js.NumberLiteral;
import com.google.caja.parser.js.ObjectConstructor;
import com.google.caja.parser.js.Operation;
import com.google.caja.parser.js.Operator;
import com.google.caja.parser.js.Reference;
import com.google.caja.parser.js.RegexpLiteral;
import com.google.caja.parser.js.ReturnStmt;
import com.google.caja.parser.js.SimpleOperation;
import com.google.caja.parser.js.Statement;
import com.google.caja.parser.js.StringLiteral;
import com.google.caja.parser.js.SwitchStmt;
import com.google.caja.parser.js.ThrowStmt;
import com.google.caja.parser.js.TranslatedCode;
import com.google.caja.parser.js.TryStmt;
import com.google.caja.parser.js.UncajoledModule;
import com.google.caja.parser.js.DirectivePrologue;
import com.google.caja.reporting.BuildInfo;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.util.Pair;
import com.google.caja.util.SyntheticAttributeKey;

import static com.google.caja.parser.js.SyntheticNodes.s;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Rewrites a JavaScript parse tree to comply with default Caja rules.
 *
 * @author ihab.awad@gmail.com (Ihab Awad)
 */
@RulesetDescription(
    name="Caja Transformation Rules",
    synopsis="Default set of transformations used by Caja"
  )
public class CajitaRewriter extends Rewriter {
  private static final SyntheticAttributeKey<Boolean> TRANSLATED
      = new SyntheticAttributeKey<Boolean>(Boolean.class, "translatedCode");

  private final BuildInfo buildInfo;
  private final ModuleManager moduleManager;
  // TODO: move this into scope if we use a single CajitaRewriter to rewrite
  // multiple modules
  private final Set<StringLiteral> moduleNameList
      = new HashSet<StringLiteral>();

  /** Mark a tree as having been translated from another language. */
  private static void markTranslated(ParseTreeNode node) {
    if (node instanceof Statement) {
      node.getAttributes().set(TRANSLATED, true);
      for (ParseTreeNode child : node.children()) {
        markTranslated(child);
      }
    }
  }
  /** Index of the last node that wasn't translated from another language. */
  private static int lastRealJavascriptChild(
      List<? extends ParseTreeNode> nodes) {
    int lasti = nodes.size();
    while (--lasti >= 0) {
      if (!nodes.get(lasti).getAttributes().is(TRANSLATED)) { break; }
    }
    return lasti;
  }

  /**
   * Find the last expression statetement executed in a block of code and
   * emit its value to a variable "moduleResult___" so that it can used as
   * the result of module loading.
   */
  @SuppressWarnings("unchecked")
  public static ParseTreeNode returnLast(ParseTreeNode node) {
    ParseTreeNode result = null;
    // Code translated from another language should not be used as the module
    // result.
    if (node.getAttributes().is(TRANSLATED)) { return node; }
    if (node instanceof ExpressionStmt) {
      result = new ExpressionStmt(
          node.getFilePosition(),
          (Expression) QuasiBuilder.substV(
              "moduleResult___ = @result;",
              "result", ((ExpressionStmt) node).getExpression()));
    } else if (node instanceof ParseTreeNodeContainer) {
      List<ParseTreeNode> nodes = new ArrayList<ParseTreeNode>(node.children());
      int lasti = lastRealJavascriptChild(nodes);
      if (lasti >= 0) {
        nodes.set(lasti, returnLast(nodes.get(lasti)));
        result = new ParseTreeNodeContainer(nodes);
      }
    } else if (node instanceof Block) {
      List<Statement> stats = new ArrayList<Statement>();
      stats.addAll((Collection<? extends Statement>) node.children());
      int lasti = lastRealJavascriptChild(stats);
      if (lasti >= 0) {
        stats.set(lasti, (Statement) returnLast(stats.get(lasti)));
        result = new Block(node.getFilePosition(), stats);
      }
    } else if (node instanceof Conditional) {
      List<ParseTreeNode> nodes = new ArrayList<ParseTreeNode>();
      nodes.addAll(node.children());
      int lasti = nodes.size() - 1;
      for (int i = 1; i <= lasti; i += 2) {  // Even are conditions.
        nodes.set(i, returnLast(nodes.get(i)));
      }
      if ((lasti & 1) == 0) {  // else clause
        nodes.set(lasti, returnLast(nodes.get(lasti)));
      }
      result = new Conditional(node.getFilePosition(), null, nodes);
    } else if (node instanceof TryStmt) {
      TryStmt tryer = (TryStmt) node;
      result = new TryStmt(
          node.getFilePosition(),
          (Block) returnLast(tryer.getBody()),
          tryer.getCatchClause(),
          tryer.getFinallyClause());
    }
    if (null == result) { return node; }
    result.getAttributes().putAll(node.getAttributes());
    return result;
  }

  // A NOTE ABOUT MATCHING MEMBER ACCESS EXPRESSIONS
  // When we match the pattern like '@x.@y' or '@x.@y()' against a specimen,
  // the result is that 'y' is bound to the rightmost component, and 'x' is
  // the remaining sub-expression on the left. Thus the result of matching
  //     @x.@y, @x.@y(), @x.@y(arg), @x.@y(args*), ...
  // is that 'y' is always bound to a Reference.

  final public Rule[] cajaRules = {

    ////////////////////////////////////////////////////////////////////////
    // Do nothing if the node is already the result of some translation
    ////////////////////////////////////////////////////////////////////////

    // See also rules in SyntheticRuleSet.

    new Rule() {
      @Override
      @RuleDescription(
          name="translatedCode",
          synopsis="Allow code received from a *->JS translator",
          reason="Translated code should not be treated as user supplied JS.",
          matches="<TranslatedCode>")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        if (node instanceof TranslatedCode) {
          Statement rewritten
              = ((TranslatedCode) expandAll(node, scope)).getTranslation();
          markTranslated(rewritten);
          return rewritten;
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="labeledStatement",
          synopsis="Statically reject if a label with `__` suffix is found",
          reason="Caja reserves the `__` suffix for internal use",
          matches="@lbl: @stmt;",
          substitutes="@lbl: @stmt;")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        if (node instanceof LabeledStatement) {
          String label = ((LabeledStatement) node).getLabel();
          if (label.endsWith("__")) {
            mq.addMessage(
                RewriterMessageType.LABELS_CANNOT_END_IN_DOUBLE_UNDERSCORE,
                node.getFilePosition(),
                MessagePart.Factory.valueOf(label));
          }
        }
        return NONE;
      }
    },

    // Loading a static module

    new Rule() {
      @Override
      @RuleDescription(
          name="loadmodule",
          synopsis="rewrites the load function.",
          reason="",
          matches="load(@arg)",
          substitutes="<depending on whether bundle the modules")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = match(node);
        if (bindings != null && scope.isImported("load")) {
          ParseTreeNode arg = bindings.get("arg");
          if (arg instanceof StringLiteral) {
            if (moduleManager != null) {
              int index = moduleManager.getModule((StringLiteral) arg);
              if (index != -1) {
                return QuasiBuilder.substV(
                    "moduleMap___[@moduleIndex]",
                    "moduleIndex",
                    new IntegerLiteral(FilePosition.UNKNOWN, index));
              } else {
                // error messages were logged in the function getModule
                return node;
              }
            } else {
              String name = ((StringLiteral) arg).getUnquotedValue();
              moduleNameList.add(new StringLiteral(
                  FilePosition.UNKNOWN, name));
              return QuasiBuilder.substV(
                  "load(@name)",
                  "name",
                  new StringLiteral(FilePosition.UNKNOWN, name));
            }
          } else {
            mq.addMessage(
                RewriterMessageType.CANNOT_LOAD_A_DYNAMIC_CAJITA_MODULE,
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
          synopsis="Cajole an UncajoledModule into a CajoledModule. Note "
              + "that the ouptut is a CajoledModule wrapper *around* the"
              + "contents of the 'substitutes' of this rule.",
          reason="So that the module loader can be invoked to load a module.",
          matches="<an UncajoledModule>",
          substitutes=(
              ""
              + "(/*@synthetic*/{"
              + "  instantiate: /*@synthetic*/function (___, IMPORTS___) {"
              + "    var moduleResult___ = ___.NO_RESULT;"
              + "    @rewrittenModuleStmts*;"
              + "    return moduleResult___;"
              + "  },"
              + "  includedModules: @moduleNames,"
              + "  cajolerName: @cajolerName,"
              + "  cajolerVersion: @cajolerVersion,"
              + "  cajoledDate: @cajoledDate"
              // TODO(ihab.awad): originalSource
              // TODO(ihab.awad): sourceLocationMap
              // TODO(ihab.awad): imports
              // TODO(ihab.awad): manifest
              + "})"))
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        if (node instanceof UncajoledModule) {
          Block inputModuleStmts = ((UncajoledModule) node).getModuleBody();
          Block rewrittenModuleStmts = (Block) expand(inputModuleStmts, null);
          ObjectConstructor moduleObjectLiteral = (ObjectConstructor) substV(
              "rewrittenModuleStmts", returnLast(rewrittenModuleStmts),
              "moduleNames", new ArrayConstructor(
                  FilePosition.UNKNOWN,
                  new ArrayList<StringLiteral>(moduleNameList)),
              "cajolerName", new StringLiteral(
                  FilePosition.UNKNOWN, "com.google.caja"),
              "cajolerVersion", new StringLiteral(
                  FilePosition.UNKNOWN,
                  buildInfo.getBuildVersion()),
              "cajoledDate", new IntegerLiteral(
                  FilePosition.UNKNOWN,
                  buildInfo.getCurrentTime()));
          return new CajoledModule(moduleObjectLiteral);
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="module",
          synopsis="Import free vars. Return last expr-statement",
          reason="Builds the module body encapsulation around the Cajita "
              + "code block.",
          matches="{@ss*;}",
          substitutes="@importedvars*; @startStmts*; @expanded*;")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        if (node instanceof Block && scope == null) {
          Scope s2 = Scope.fromProgram((Block) node, mq);
          List<ParseTreeNode> expanded = new ArrayList<ParseTreeNode>();
          for (ParseTreeNode c : node.children()) {
            expanded.add(expand(c, s2));
          }
          List<ParseTreeNode> importedVars = new ArrayList<ParseTreeNode>();

          Set<String> importNames = s2.getImportedVariables();
          // Order imports so that Array and Object appear first, and so that
          // they appear before any use of the [] and {} shorthand syntaxes
          // since those are specified in ES3 by looking up the identifiers
          // "Array" and "Object" in the local scope.
          // SpiderMonkey actually implements this behavior, though it is fixed
          // in FF3, and ES5 specifies the behavior of [] and {} in terms
          // of the original Array and Object constructors for that context.
          Set<String> orderedImportNames = new LinkedHashSet<String>();
          if (importNames.contains("Array")) {
            orderedImportNames.add("Array");
          }
          if (importNames.contains("Object")) {
            orderedImportNames.add("Object");
          }
          orderedImportNames.addAll(importNames);

          for (String k : orderedImportNames) {
            Identifier kid = new Identifier(FilePosition.UNKNOWN, k);
            Expression permitsUsed = s2.getPermitsUsed(kid);
            if (null == permitsUsed
                || "Array".equals(k) || "Object".equals(k)) {
              importedVars.add(
                  QuasiBuilder.substV(
                      "var @vIdent = ___.readImport(IMPORTS___, @vName);",
                      "vIdent", s(kid),
                      "vName", toStringLiteral(kid)));
            } else {
              importedVars.add(
                  QuasiBuilder.substV(
                      "var @vIdent = ___.readImport(IMPORTS___, @vName, @permits);",
                      "vIdent", s(kid),
                      "vName", toStringLiteral(kid),
                      "permits", permitsUsed));
            }
          }

          return substV(
              "importedvars", new ParseTreeNodeContainer(importedVars),
              "startStmts", new ParseTreeNodeContainer(s2.getStartStatements()),
              "expanded", new ParseTreeNodeContainer(expanded));
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
          synopsis="Initialize named functions at the beginning of their "
              + "enclosing block.",
          reason="Nested named function declarations are illegal in ES3 but are "
              + "universally supported by all JavaScript implementations, "
              + "though in different ways. The compromise semantics currently "
              + "supported by Cajita is to hoist the declaration of a variable "
              + "with the function's name to the beginning of the enclosing "
              + "function body or module top level, and to initialize this "
              + "variable to a new anonymous function every time control "
              + "re-enters the enclosing block."
              + "\n"
              + "Note that ES-Harmony will specify a better and safer semantics "
              + "-- block level lexical scoping -- that we'd like to adopt into "
              + "Cajita eventually. However, it is so challenging to implement this "
              + "semantics by translation to currently-implemented JavaScript "
              + "that we provide something quicker and dirtier for now.",
          matches="{@ss*;}",
          substitutes="@startStmts*; @ss*;")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        if (node instanceof Block) {
          List<Statement> expanded = new ArrayList<Statement>();
          Scope s2 = Scope.fromPlainBlock(scope);
          for (Statement c : ((Block) node).children()) {
            ParseTreeNode rewritten = expand(c, s2);
            if (rewritten.getClass() == Block.class) {
              expanded.addAll(((Block) rewritten).children());
            } else if (!(rewritten instanceof Noop)) {
              expanded.add((Statement) rewritten);
            }
          }
          return substV(
              "startStmts", new ParseTreeNodeContainer(s2.getStartStatements()),
              "ss", new ParseTreeNodeContainer(expanded));
        }
        return NONE;
      }
    },

    ////////////////////////////////////////////////////////////////////////
    // with - disallow the 'with' construct
    ////////////////////////////////////////////////////////////////////////

    new Rule() {
      @Override
      @RuleDescription(
          name="with",
          synopsis="Statically reject if a `with` block is found.",
          reason="`with` violates the assumptions made by Scope, and makes it "
              + "very hard to write a Scope that works. "
              + "http://yuiblog.com/blog/2006/04/11/with-statement-considered-harmful/ "
              + "briefly touches on why `with` is bad for programmers. For "
              + "reviewers -- matching of references with declarations can only "
              + "be done at runtime. All other secure JS subsets that we know "
              + "of (ADSafe, Jacaranda, & FBJS) also disallow `with`.",
          matches="with (@scope) @body;",
          substitutes="<reject>")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = this.match(node);
        if (bindings != null) {
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

    new Rule() {
      @Override
      @RuleDescription(
          name="forInBad",
          synopsis="Do not allow a for-in loop.",
          reason="Use Cajita for-in construct instead.",
          matches="for (@k in @o) @ss;",
          substitutes="")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = match(node);
        if (bindings != null) {
          mq.addMessage(
              RewriterMessageType.FOR_IN_NOT_IN_CAJITA,
              node.getFilePosition(), this, node);
          return node;
        }
        return NONE;
      }
    },

    ////////////////////////////////////////////////////////////////////////
    // try - try/catch/finally constructs
    ////////////////////////////////////////////////////////////////////////

    new Rule() {
      @Override
      @RuleDescription(
          name="tryCatch",
          synopsis="Ensure that only immutable data is thrown, and repair scope "
              + "confusion in existing JavaScript implementations of "
              + "try/catch.",
          reason="When manually reviewing code for vulnerability, experience "
              + "shows that reviewers cannot pay adequate attention to the "
              + "pervasive possibility of thrown exceptions. These lead to four "
              + "dangers: 1) leaking an authority-bearing object, endangering "
              + "integrity, 2) leaking a secret, endangering secrecy, and 3) "
              + "aborting a partially completed state update, leaving the state "
              + "malformed, endangering integrity, and 4) preventing an "
              + "operation that was needed, endangering availability. Caja only "
              + "seeks to make strong claims about integrity. By ensuring that "
              + "only immutable (transitively frozen) data is thrown, we "
              + "prevent problem #1. For the others, programmer vigilance is "
              + "still needed. \n"
              + "Current JavaScript implementations fail, in different ways, to "
              + "implement the scoping of the catch variable specified in ES3. "
              + "We translate Caja to JavaScript so as to implement the ES3 "
              + "specified scoping on current JavaScript implementations.",
          matches="try { @s0*; } catch (@x) { @s1*; }",
          substitutes="try {\n"
              + "  @s0*;\n"
              + "} catch (ex___) {\n"
              + "  try {\n"
              + "    throw ___.tameException(ex___); \n"
              + "  } catch (@x) {\n"
              + "    @s1*;\n"
              + "  }\n"
              + "}")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = match(node);
        if (bindings != null) {
          TryStmt t = (TryStmt) node;
          Identifier exceptionName = t.getCatchClause().getException()
              .getIdentifier();
          if (exceptionName.getName().endsWith("__")) {
            mq.addMessage(
                RewriterMessageType.VARIABLES_CANNOT_END_IN_DOUBLE_UNDERSCORE,
                node.getFilePosition(), this, node);
            return node;
          }
          return substV(
            "s0",  expandAll(bindings.get("s0"), scope),
            "x",   noexpand((Identifier) bindings.get("x")),
            "s1",  expandAll(bindings.get("s1"),
                             Scope.fromCatchStmt(scope, t.getCatchClause())));
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="tryCatchFinally",
          synopsis="Finally adds no special issues beyond those explained in "
              + "try/catch.",
          reason="Caja is not attempting to impose determinism, so the reasons "
              + "for Joe-E to avoid finally do not apply.",
          matches="try { @s0*; } catch (@x) { @s1*; } finally { @s2*; }",
          substitutes="try {\n"
              + "  @s0*;\n"
              + "} catch (ex___) {\n"
              + "  try {\n"
              + "    throw ___.tameException(ex___);\n"
              + "  } catch (@x) {\n"
              + "    @s1*;\n"
              + "  }\n"
              + "} finally {\n"
              + "  @s2*;\n"
              + "}")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = match(node);
        if (bindings != null) {
          TryStmt t = (TryStmt) node;
          Identifier exceptionName = t.getCatchClause().getException()
              .getIdentifier();
          if (exceptionName.getName().endsWith("__")) {
            mq.addMessage(
                RewriterMessageType.VARIABLES_CANNOT_END_IN_DOUBLE_UNDERSCORE,
                node.getFilePosition(), this, node);
            return node;
          }
          return substV(
            "s0",  expandAll(bindings.get("s0"), scope),
            "x",   noexpand((Identifier) bindings.get("x")),
            "s1",  expandAll(bindings.get("s1"),
                             Scope.fromCatchStmt(scope, t.getCatchClause())),
            "s2",  expandAll(bindings.get("s2"), scope));
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="tryFinally",
          synopsis="See bug 383. Otherwise, it's just the trivial translation.",
          reason="try/finally actually seems to work as needed by current "
              + "JavaScript implementations.",
          matches="try { @s0*; } finally { @s1*; }",
          substitutes="try { @s0*; } finally { @s1*; }")
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
          name="varArgs",
          synopsis="Make all references to the magic \"arguments\" variable "
              + "into references to a frozen array containing a snapshot of the "
              + "actual arguments taken when the function was first entered.",
          reason="ES3 specifies that the magic \"arguments\" variable is a "
              + "dynamic (\"joined\") mutable array-like reflection of the "
              + "values of the parameter variables. However, te typical usage "
              + "is to pass it to provide access to one's original arguments -- "
              + "without the intention of providing the ability to mutate the "
              + "caller's parameter variables. By making a frozen array "
              + "snapshot with no \"callee\" property, we provide the least "
              + "authority assumed by this typical use.\n"
              + "The snapshot is made with a \"var a___ = "
              + "___.args(arguments);\" generated at the beginning of the "
              + "function body.",
          matches="arguments",
          substitutes="a___")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        return transform(node, scope);
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="varThisBad",
          synopsis="The \"this\" keyword is not permitted in Cajita.",
          reason="The rules for binding of \"this\" in JavaScript are dangerous.",
          matches="this",
          substitutes="<rejected>")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = match(node);
        if (bindings != null) {
          mq.addMessage(
              RewriterMessageType.THIS_NOT_IN_CAJITA,
              node.getFilePosition(), this, node);
          return node;
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="varBadSuffix",
          synopsis="Statically reject if a variable with `__` suffix is found.",
          reason="Caja reserves the `__` suffix for internal use.",
          matches="@v__",
          substitutes="<reject>")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = match(node);
        if (bindings != null) {
          mq.addMessage(
              RewriterMessageType.VARIABLES_CANNOT_END_IN_DOUBLE_UNDERSCORE,
              node.getFilePosition(), this, node);
          return node;
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="varBadSuffixDeclaration",
          synopsis="Statically reject if a variable with `__` suffix is found.",
          reason="Caja reserves the `__` suffix for internal use.",
          matches="<approx>(var|function) @v__ ...",  // TODO(mikesamuel): limit
          substitutes="<reject>")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        if (node instanceof Declaration) {
          Identifier name = ((Declaration) node).getIdentifier();
          if (name.getValue().endsWith("__")) {
            mq.addMessage(
                RewriterMessageType.VARIABLES_CANNOT_END_IN_DOUBLE_UNDERSCORE,
                node.getFilePosition(), this, node);
            return node;
          }
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="varFuncFreeze",
          synopsis="An escaping occurence of a function name freezes the "
              + "function.",
          reason="",
          matches="@fname",
          substitutes="___.primFreeze(@fname)")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = match(node);
        if (bindings != null) {
          ParseTreeNode fname = bindings.get("fname");
          if (scope.isDeclaredFunctionReference(fname)) {
            return substV("fname", noexpand((Reference) fname));
          }
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="varDefault",
          synopsis="Any remaining uses of a variable name are preserved.",
          reason="",
          matches="@v",  // TODO(mikesamuel): limit further
          substitutes="@v")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = match(node);
        if (bindings != null) {
          ParseTreeNode v = bindings.get("v");
          if (v instanceof Reference) {
            return noexpand((Reference) v);
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
          name="readBadSuffix",
          synopsis="Statically reject if a property has `__` suffix is found.",
          reason="Caja reserves the `__` suffix for internal use.",
          matches="@x.@p__",
          substitutes="<reject>")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = match(node);
        if (bindings != null) {
          mq.addMessage(
              RewriterMessageType.PROPERTIES_CANNOT_END_IN_DOUBLE_UNDERSCORE,
              node.getFilePosition(), this, node);
          return node;
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="readBadPrototype",
          synopsis="Warn that reading the 'prototype' property of a function "
              + "is useless in Cajita.",
          reason="A programmer reading the 'prototype' property of a function "
              + "is most likely attempting classic JavaScript prototypical "
              + "inheritance, which is not supported in Cajita.",
          matches="@f.prototype",
          substitutes="<warning>")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = match(node);
        if (bindings != null &&
            bindings.get("f") instanceof Reference &&
            scope.isFunction(getReferenceName(bindings.get("f")))) {
          mq.addMessage(
              RewriterMessageType.PROTOTYPICAL_INHERITANCE_NOT_IN_CAJITA,
              node.getFilePosition(), this, node);
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="permittedRead",
          synopsis="When @o.@m is a statically permitted read, translate directly.",
          reason="The static permissions check is recorded so that, when the base of " +
                 "@o is imported, we check that this static permission was actually " +
                 "safe to assume.",
          matches="@o.@m",
          substitutes="@o.@m")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = match(node);
        if (bindings != null) {
          ParseTreeNode o = bindings.get("o");
          Permit oPermit = scope.permitRead(o);
          if (null != oPermit) {
            Reference m = (Reference) bindings.get("m");
            if (null != oPermit.canRead(m)) {
              return substV(
                  "o",  expand(o, scope),
                  "m",  noexpand(m));
            }
          }
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="readPublicLength",
          synopsis="",
          reason="Length is whitelisted on Object.prototype",
          matches="@o.length",
          substitutes="@o.length")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        return transform(node, scope);
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="readPublic",
          synopsis="",
          reason="",
          matches="@o.@p",
          substitutes="@oRef.@fp ? @oRef.@p : ___.readPub(@oRef, '@p')")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = match(node);
        if (bindings != null) {
          Pair<Expression, Expression> oPair = reuse(bindings.get("o"), scope);
          Reference p = (Reference) bindings.get("p");
          String propertyName = p.getIdentifierName();
          return commas(oPair.b, (Expression) substV(
              "oRef", oPair.a,
              "p",    noexpand(p),
              "fp",   newReference(p.getFilePosition(),
                                   propertyName + "_canRead___"),
              "rp",   toStringLiteral(p)));
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="readNumPublic",
          synopsis="Recognize that array indexing is inherently safe.",
          reason="When the developer knows that their index expression is" +
              " an array index, they can indicate this with the" +
              " 'absolute value operator', really an expression which" +
              " coerces to a nonnegative 32-bit integer. Since these" +
              " properties are necessarily readable, we can pass them " +
              " through directly to JavaScript.",
          matches="@o[@s&(-1>>>1)]",
          substitutes="@o[@s&(-1>>>1)]")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        return transform(node, scope);
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="readNumWithConstantIndex",
          synopsis="Recognize that array indexing is inherently safe.",
          reason="Nonnegative integer properties are always readable;" +
              " we can pass these through directly to JavaScript.",
          matches="@o[@numLiteral]",
          substitutes="@o[@numLiteral]")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = match(node);
        if (bindings != null) {
          ParseTreeNode index = bindings.get("numLiteral");
          if (index instanceof NumberLiteral) {
            double indexValue =
                ((NumberLiteral) index).getValue().doubleValue();
            if (indexValue >= 0 &&
                indexValue == Math.floor(indexValue)) {
              return substV(
                  "o", expand(bindings.get("o"), scope),
                  "numLiteral", expand(index, scope));
            }
          }
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="readIndexPublic",
          synopsis="",
          reason="",
          matches="@o[@s]",
          substitutes="___.readPub(@o, @s)")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        return transform(node, scope);
      }
    },

    ////////////////////////////////////////////////////////////////////////
    // set - assignments
    ////////////////////////////////////////////////////////////////////////

    new Rule() {
      @Override
      @RuleDescription(
          name="setBadAssignToFunctionName",
          synopsis="Statically reject if an assignment expression assigns to a "
              + "function name.",
          reason="",
          matches="<approx> @fname @op?= @x", // TODO(mikesamuel): Limit further
          substitutes="<reject>")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        if (node instanceof AssignOperation
            && node.children().get(0) instanceof Reference
            && scope.isFunction(getReferenceName(node.children().get(0)))) {
          mq.addMessage(
              RewriterMessageType.CANNOT_ASSIGN_TO_FUNCTION_NAME,
              node.getFilePosition(), this, node);
          return node;
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="setBadThis",
          synopsis="The \"this\" keyword is not permitted in Cajita.",
          reason="The rules for binding of \"this\" in JavaScript are dangerous.",
          matches="this = @z",
          substitutes="<reject>")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = match(node);
        if (bindings != null) {
          mq.addMessage(
              RewriterMessageType.THIS_NOT_IN_CAJITA,
              node.getFilePosition(), this, node);
          return node;
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="setBadFreeVariable",
          synopsis="Statically reject if an expression assigns to a free "
              + "variable.",
          reason="This is still controversial (see bug 375). However, the "
              + "rationale is to prevent code that's nested lexically within a "
              + "module to from introducing mutable state outside its local "
              + "function-body scope. Without this rule, two nested blocks "
              + "within the same module could communicate via a pseudo-imported "
              + "variable that is not declared or used at the outer scope of "
              + "the module body.",
          matches="@import = @y",
          substitutes="<reject>")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = match(node);
        if (bindings != null && bindings.get("import") instanceof Reference) {
          String name = ((Reference) bindings.get("import")).getIdentifierName();
          if (Scope.UNMASKABLE_IDENTIFIERS.contains(name)) {
            mq.addMessage(
                RewriterMessageType.CANNOT_MASK_IDENTIFIER,
                node.getFilePosition(), MessagePart.Factory.valueOf(name));
          } else if (scope.isImported(name)) {
            mq.addMessage(
                RewriterMessageType.CANNOT_ASSIGN_TO_FREE_VARIABLE,
                node.getFilePosition(), this, node);
            return node;
          }
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="setBadValueOf",
          synopsis="Statically reject if assigning to valueOf.",
          reason="We depend on valueOf returning consistent results.",
          matches="@x.valueOf = @z",
          substitutes="<reject>")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = match(node);
        if (bindings != null) {
          mq.addMessage(
              RewriterMessageType.VALUEOF_PROPERTY_MUST_NOT_BE_SET,
              node.getFilePosition(), this, node);
          return node;
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="setBadSuffix",
          synopsis="Statically reject if a property with `__` suffix is found.",
          reason="Caja reserves the `__` suffix for internal use.",
          matches="@x.@p__ = @z",
          substitutes="<reject>")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = match(node);
        if (bindings != null) {
          mq.addMessage(
              RewriterMessageType.PROPERTIES_CANNOT_END_IN_DOUBLE_UNDERSCORE,
              node.getFilePosition(), this, node);
          return node;
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="setBadPrototype",
          synopsis="Warn that setting the 'prototype' property of a function "
              + "is useless in Cajita.",
          reason="A programmer setting the 'prototype' property of a function "
              + "is most likely attempting classic JavaScript prototypical "
              + "inheritance, which is not supported in Cajita.",
          matches="@f.prototype = @rhs",
          substitutes="<warning>")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = match(node);
        if (bindings != null &&
            bindings.get("f") instanceof Reference &&
            scope.isFunction(getReferenceName(bindings.get("f")))) {
          mq.addMessage(
              RewriterMessageType.PROTOTYPICAL_INHERITANCE_NOT_IN_CAJITA,
              node.getFilePosition(), this, node);
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="setStatic",
          synopsis="Initialize the direct properties (static members) of a "
              + "potentially-mutable named function.",
          reason="",
          matches="@fname.@p = @r",
          substitutes="___.setStatic(@fname, @'p', @r)")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = match(node);
        if (bindings != null && bindings.get("fname") instanceof Reference) {
          Reference fname = (Reference) bindings.get("fname");
          Reference p = (Reference) bindings.get("p");
          if (scope.isDeclaredFunction(getReferenceName(fname))) {
            ParseTreeNode r = bindings.get("r");
            return QuasiBuilder.substV(
                "___.setStatic(@fname, @rp, @r)",
                "fname", noexpand(fname),
                "rp", toStringLiteral(p),
                "r", expand(nymize(r, p.getIdentifierName(), "static"), scope));
          }
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="setIndexStatic",
          synopsis="Initialize the computed direct properties (static members) of a "
              + "potentially-mutable named function.",
          reason="",
          matches="@fname[@s] = @r",
          substitutes="___.setStatic(@fname, @s, @r)")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = match(node);
        if (bindings != null && bindings.get("fname") instanceof Reference) {
          Reference fname = (Reference) bindings.get("fname");
          if (scope.isDeclaredFunction(getReferenceName(fname))) {
            return substV(
                "fname", noexpand(fname),
                "s", expand(bindings.get("s"), scope),
                "r", expand(bindings.get("r"), scope));
          }
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="setPublic",
          synopsis="Set a public property.",
          reason="If the object is an unfrozen JSONContainer (a record or "
              + "array), then this will create the own property if needed. If "
              + "it is an unfrozen constructed object, then clients can assign "
              + "to existing public own properties, but cannot directly create "
              + "such properties.",
          matches="@o.@p = @r",
          substitutes="<approx> ___.setPub(@o, @'p', @r);")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = match(node);
        if (bindings != null) {
          Pair<Expression, Expression> oPair = reuse(bindings.get("o"), scope);
          Reference p = (Reference) bindings.get("p");
          String propertyName = p.getIdentifierName();
          ParseTreeNode r = bindings.get("r");
          Pair<Expression, Expression> rPair = reuse(nymize(r, propertyName, "meth"), scope);
          return commas(oPair.b, rPair.b, (Expression) QuasiBuilder.substV(
              "@oRef.@pCanSet === @oRef ? (@oRef.@p = @rRef) : " +
              "                           ___.setPub(@oRef, @pName, @rRef);",
              "oRef", oPair.a,
              "rRef", rPair.a,
              "pCanSet", newReference(
                  FilePosition.UNKNOWN, propertyName + "_canSet___"),
              "p", noexpand(p),
              "pName", toStringLiteral(p)));
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="setIndexPublic",
          synopsis="",
          reason="",
          matches="@o[@s] = @r",
          substitutes="___.setPub(@o, @s, @r)")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = match(node);
        if (bindings != null) {
          return substV(
              "o", expand(bindings.get("o"), scope),
              "s", expand(bindings.get("s"), scope),
              "r", expand(bindings.get("r"), scope));
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="setBadInitialize",
          synopsis="Statically reject if a variable with `__` suffix is found.",
          reason="Caja reserves the `__` suffix for internal use.",
          matches="var @v__ = @r",
          substitutes="<reject>")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = match(node);
        if (bindings != null) {
          mq.addMessage(
              RewriterMessageType.VARIABLES_CANNOT_END_IN_DOUBLE_UNDERSCORE,
              node.getFilePosition(), this, node);
          return node;
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="setInitialize",
          synopsis="Ensure v is not a function name. Expand the right side.",
          reason="",
          matches="var @v = @r",
          substitutes="var @v = @r")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = match(node);
        if (bindings != null) {
          Identifier v = (Identifier) bindings.get("v");
          if (!scope.isFunction(v.getName())) {
            ParseTreeNode r = bindings.get("r");
            return substV(
                "v", noexpand(v),
                "r", expand(nymize(r, v.getName(), "var"), scope));
          }
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="setBadDeclare",
          synopsis="Statically reject if a variable with `__` suffix is found.",
          reason="Caja reserves the `__` suffix for internal use.",
          matches="var @v__",
          substitutes="<reject>")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = match(node);
        if (bindings != null) {
          mq.addMessage(
              RewriterMessageType.VARIABLES_CANNOT_END_IN_DOUBLE_UNDERSCORE,
              node.getFilePosition(), this, node);
          return node;
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="setDeclare",
          synopsis="Ensure that v isn't a function name.",
          reason="",
          matches="var @v",
          substitutes="var @v")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = match(node);
        if (bindings != null
            && !scope.isFunction(getIdentifierName(bindings.get("v")))) {
          return substV("v", noexpand((Identifier) bindings.get("v")));
        }
        return NONE;
      }
    },

    // TODO(erights): Need a general way to expand lValues
    new Rule() {
      @Override
      @RuleDescription(
          name="setBadVar",
          synopsis="Statically reject if a variable with `__` suffix is found.",
          reason="Caja reserves the `__` suffix for internal use.",
          matches="@v__ = @r",
          substitutes="<reject>")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = match(node);
        if (bindings != null) {
          mq.addMessage(
              RewriterMessageType.VARIABLES_CANNOT_END_IN_DOUBLE_UNDERSCORE,
              node.getFilePosition(), this, node);
          return node;
        }
        return NONE;
      }
    },

    // TODO(erights): Need a general way to expand lValues
    new Rule() {
      @Override
      @RuleDescription(
          name="setVar",
          synopsis="Only if v isn't a function name.",
          reason="",
          matches="@v = @r",
          substitutes="@v = @r")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = match(node);
        if (bindings != null) {
          ParseTreeNode v = bindings.get("v");
          if (v instanceof Reference) {
            String vname = getReferenceName(v);
            if (!scope.isFunction(vname)) {
              ParseTreeNode r = bindings.get("r");
              return substV(
                  "v", noexpand((Reference) v),
                  "r", expand(nymize(r, vname, "var"), scope));
            }
          }
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
              aNode.children().get(0), scope);
          if (ops == null) { return node; }  // Error deconstructing

          // For x += 3, rhs is (x + 3)
          Operation rhs = Operation.create(
              aNode.children().get(0).getFilePosition(),
              op.getAssignmentDelegate(),
              ops.getUncajoledLValue(), aNode.children().get(1));
          Operation assignment = ops.makeAssignment(rhs);
          return commas(newCommaOperation(ops.getTemporaries()),
                        (Expression) expand(assignment, scope));
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
        ReadAssignOperands ops = deconstructReadAssignOperand(v, scope);
        if (ops == null) { return node; }  // Error deconstructing

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
                          "@rvalue - -1", "rvalue", ops.getUncajoledLValue())),
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
                  ops.makeAssignment(
                      (Expression) QuasiBuilder.substV(
                          "@rvalue - 1",
                          "rvalue", ops.getUncajoledLValue())),
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
          name="newCalllessCtor",
          synopsis="Add missing empty argument list.",
          reason="JavaScript syntax allows constructor calls without \"()\".",
          matches="new @ctor",
          substitutes="___.construct(@ctor, [])")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        return transform(node, scope);
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="newCtor",
          synopsis="",
          reason="",
          matches="new @ctor(@as*)",
          substitutes="___.construct(@ctor, [@as*])")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        return transform(node, scope);
      }
    },

    ////////////////////////////////////////////////////////////////////////
    // delete - property deletion
    ////////////////////////////////////////////////////////////////////////

    new Rule() {
      @Override
      @RuleDescription(
          name="deleteBadValueOf",
          synopsis="Prohibit deletion of valueOf.",
          reason="Although a non-existent valueOf should behave the same way "
              + "asthe default one as regards [[DefaultValue]], for simplicity "
              + "weonly want to have to consider one of those cases.",
          matches="delete @o.valueOf",
          substitutes="<reject>")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = match(node);
        if (bindings != null) {
          mq.addMessage(
              RewriterMessageType.VALUEOF_PROPERTY_MUST_NOT_BE_DELETED,
              node.getFilePosition(), this, node);
          return node;
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="deleteBadSuffix",
          synopsis="",
          reason="",
          matches="delete @o.@p__",
          substitutes="<reject>")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = match(node);
        if (bindings != null) {
          mq.addMessage(
              RewriterMessageType.PROPERTIES_CANNOT_END_IN_DOUBLE_UNDERSCORE,
              node.getFilePosition(), this, node);
          return node;
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="deletePublic",
          synopsis="",
          reason="",
          matches="delete @o.@p",
          substitutes="___.deletePub(@o, @'p')")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = match(node);
        if (bindings != null) {
          Reference p = (Reference) bindings.get("p");
          return QuasiBuilder.substV(
              "___.deletePub(@o, @pname)",
              "o", expand(bindings.get("o"), scope),
              "pname", toStringLiteral(p));
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="deleteIndexPublic",
          synopsis="",
          reason="",
          matches="delete @o[@s]",
          substitutes="___.deletePub(@o, @s)")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = match(node);
        if (bindings != null) {
          return substV(
              "o", expand(bindings.get("o"), scope),
              "s", expand(bindings.get("s"), scope));
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="deleteNonProperty",
          synopsis="",
          reason="",
          matches="delete @v",
          substitutes="<reject>")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = match(node);
        if (bindings != null) {
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

    new Rule() {
      @Override
      @RuleDescription(
          name="callBadSuffix",
          synopsis="Statically reject if a selector with `__` suffix is found.",
          reason="Caja reserves the `__` suffix for internal use.",
          matches="@o.@p__(@as*)",
          substitutes="<reject>")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = match(node);
        if (bindings != null) {
          mq.addMessage(
              RewriterMessageType.SELECTORS_CANNOT_END_IN_DOUBLE_UNDERSCORE,
              node.getFilePosition(), this, node);
          return node;
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="permittedCall",
          synopsis="When @o.@m is a statically permitted call, translate directly.",
          reason="The static permissions check is recorded so that, when the " +
              "base of @o is imported, we check that this static permission " +
              "was actually safe to assume.",
          matches="@o.@m(@as*)",
          substitutes="@o.@m(@as*)")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = match(node);
        if (bindings != null) {
          ParseTreeNode o = bindings.get("o");
          Permit oPermit = scope.permitRead(o);
          if (null != oPermit) {
            Reference m = (Reference) bindings.get("m");
            if (null != oPermit.canCall(m)) {
              return substV(
                  "o",  expand(o, scope),
                  "m",  noexpand(m),
                  "as", expandAll(bindings.get("as"), scope));
            }
          }
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="callPublic",
          synopsis="",
          reason="",
          matches="@o.@m(@as*)",
          substitutes="<approx> ___.callPub(@o, @'m', [@as*])")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = match(node);
        if (bindings != null) {
          Pair<Expression, Expression> oPair = reuse(bindings.get("o"), scope);
          Reference m = (Reference) bindings.get("m");
          Pair<ParseTreeNodeContainer, Expression> argsPair =
              reuseAll(bindings.get("as"), scope);
          String methodName = m.getIdentifierName();
          return commas(oPair.b, argsPair.b, (Expression) QuasiBuilder.substV(
              "@oRef.@fm ? @oRef.@m(@argRefs*) : ___.callPub(@oRef, @rm, [@argRefs*]);",
              "oRef",    oPair.a,
              "argRefs", argsPair.a,
              "m",       noexpand(m),
              "fm",      newReference(
                             FilePosition.UNKNOWN, methodName + "_canCall___"),
              "rm",      toStringLiteral(m)));
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="callIndexPublic",
          synopsis="",
          reason="",
          matches="@o[@s](@as*)",
          substitutes="___.callPub(@o, @s, [@as*])")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        return transform(node, scope);
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="callDeclaredFunc",
          synopsis="When calling a declared function name, leave the freezing to CALL___.",
          reason="If @fname is a declared function name, an escaping use as here would " +
              "normally generate a call to primFreeze it, so that it's frozen on " +
              "first use. However, since the default CALL___ method now freezes  " +
              "the function it's called on, " +
              "if @fname is a declared function name, we avoid expanding it.",
          matches="@fname(@as*)",
          substitutes="@fname.CALL___(@as*)")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = match(node);
        if (bindings != null) {
          ParseTreeNode fname = bindings.get("fname");
          if (scope.isDeclaredFunctionReference(fname)) {
            return substV(
              "fname", noexpand((Reference) fname),
              "as",    expandAll(bindings.get("as"), scope));
          }
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="callFunc",
          synopsis="",
          reason="",
          matches="@f(@as*)",
          substitutes="@f.CALL___(@as*)")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        return transform(node, scope);
      }
    },

    ////////////////////////////////////////////////////////////////////////
    // function - function definitions
    ////////////////////////////////////////////////////////////////////////

    new Rule() {
      @Override
      @RuleDescription(
          name="funcAnonSimple",
          synopsis="",
          reason="",
          matches="function (@ps*) { @bs*; }",
          substitutes="___.markFuncFreeze(\n"
              + "  function (@ps*) {\n"
              + "    @fh*;\n"
              + "    @stmts*;\n"
              + "    @bs*;\n"
              + "})")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = match(node);
        // Anonymous simple function constructor
        if (bindings != null) {
          Scope s2 = Scope.fromFunctionConstructor(scope, (FunctionConstructor) node);
          ParseTreeNodeContainer ps = (ParseTreeNodeContainer) bindings.get("ps");
          checkFormals(ps);
          return substV(
              "ps", noexpandParams(ps),
              // It's important to expand bs before computing fh and stmts.
              "bs", expand(bindings.get("bs"), s2),
              "fh", getFunctionHeadDeclarations(s2),
              "stmts", new ParseTreeNodeContainer(s2.getStartStatements()));
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="funcNamedTopDecl",
          synopsis="A non-nested named function doesn't need a maker",
          reason="",
          matches="function @fname(@ps*) { @bs*; }",
          substitutes="function @fname(@ps*) {\n"
            + "  @fh*;\n"
            + "  @stmts*;\n"
            + "  @bs*;\n"
            + "}\n"
            + "@fname.FUNC___ = @'fname';\n")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        if (node instanceof FunctionDeclaration &&
            scope == scope.getClosestDeclarationContainer()) {

          Map<String, ParseTreeNode> bindings =
            match(((FunctionDeclaration) node).getInitializer());
          // Named simple function declaration
          if (bindings != null) {
            Scope s2 = Scope.fromFunctionConstructor(
                scope,
                ((FunctionDeclaration) node).getInitializer());
            ParseTreeNodeContainer ps = (ParseTreeNodeContainer) bindings.get("ps");
            checkFormals(ps);
            Identifier fname = noexpand((Identifier) bindings.get("fname"));
            Block block = (Block) QuasiBuilder.substV(
                "function @fname(@ps*) {\n"
                + "  @fh*;\n"
                + "  @stmts*;\n"
                + "  @bs*;\n"
                + "}\n"
                + "@fRef.FUNC___ = @rf;\n",
                "fname", fname,
                "fRef", new Reference(fname),
                "rf", toStringLiteral(fname),
                "ps", noexpandParams(ps),
                // It's important to expand bs before computing fh and stmts.
                "bs", expand(bindings.get("bs"), s2),
                "fh", getFunctionHeadDeclarations(s2),
                "stmts", new ParseTreeNodeContainer(s2.getStartStatements()));
            for (Statement stat : block.children()) {
              scope.addStartOfBlockStatement(stat);
            }
            return QuasiBuilder.substV(";");
          }
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="funcNamedSimpleDecl",
          synopsis="Simulate a nested named function declaration with a top "
                   + "level named function declaration inside an anon "
                   + "function expression.",
          reason="Current (pre-ES5) browsers have wacky scoping semantics "
            + "for nested named function declarations.",
          matches="function @fname(@ps*) { @bs*; }",
          substitutes="@fname = (function() {\n"
            + "  function @fself(@ps*) {\n"
            + "    @fh*;\n"
            + "    @stmts*;\n"
            + "    @bs*;\n"
            + "  }\n"
            + "  @fself.FUNC___ = @'fname';\n"
            + "  return @fself;\n"
            + "})();")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = (
            node instanceof FunctionDeclaration)
            ? match(((FunctionDeclaration) node).getInitializer())
            : null;
        // Named simple function declaration
        if (bindings != null) {
          Scope s2 = Scope.fromFunctionConstructor(
              scope,
              ((FunctionDeclaration) node).getInitializer());
          ParseTreeNodeContainer ps = (ParseTreeNodeContainer) bindings.get("ps");
          checkFormals(ps);
          Identifier fname = noexpand((Identifier) bindings.get("fname"));
          Identifier fself = new Identifier(
              FilePosition.UNKNOWN, nym(node, fname.getName(), "self"));
          scope.declareStartOfScopeVariable(fname);
          Expression expr = (Expression) QuasiBuilder.substV(
              "@fRef = (function() {\n"
              + "  function @fself(@ps*) {\n"
              + "    @fh*;\n"
              + "    @stmts*;\n"
              + "    @bs*;\n"
              + "  }\n"
              + "  @rfself.FUNC___ = @rf;\n"
              + "  return @rfself;\n"
              + "})();",
              "fname", fname,
              "fRef", new Reference(fname),
              "fself", fself,
              "rfself", new Reference(fself),
              "rf", toStringLiteral(fname),
              "ps", noexpandParams(ps),
              // It's important to expand bs before computing fh and stmts.
              "bs", expand(bindings.get("bs"), s2),
              "fh", getFunctionHeadDeclarations(s2),
              "stmts", new ParseTreeNodeContainer(s2.getStartStatements()));
          scope.addStartOfBlockStatement(
              new ExpressionStmt(node.getFilePosition(), expr));
          return QuasiBuilder.substV(";");
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="funcNamedSimpleValue",
          synopsis="",
          reason="",
          matches="function @fname(@ps*) { @bs*; }",
          substitutes="(function() {\n"
              + "  function @fname(@ps*) {\n"
              + "    @fh*;\n"
              + "    @stmts*;\n"
              + "    @bs*;\n"
              + "  }\n"
              + "  return ___.markFuncFreeze(@fname, @'fname');\n"
              + "})();")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = match(node);
        // Named simple function expression
        if (bindings != null) {
          Scope s2 = Scope.fromFunctionConstructor(
              scope, (FunctionConstructor) node);
          ParseTreeNodeContainer ps = (ParseTreeNodeContainer) bindings.get("ps");
          checkFormals(ps);
          Identifier fname = noexpand((Identifier) bindings.get("fname"));
          return QuasiBuilder.substV(
              "(function() {\n"
              + "  function @fname(@ps*) {\n"
              + "    @fh*;\n"
              + "    @stmts*;\n"
              + "    @bs*;\n"
              + "  }\n"
              + "  return ___.markFuncFreeze(@fRef, @rf);\n"
              + "})();",
              "fname", fname,
              "fRef", new Reference(fname),
              "rf", toStringLiteral(fname),
              "ps", noexpandParams(ps),
              // It's important to expand bs before computing fh and stmts.
              "bs", expand(bindings.get("bs"), s2),
              "fh", getFunctionHeadDeclarations(s2),
              "stmts", new ParseTreeNodeContainer(s2.getStartStatements()));
        }
        return NONE;
      }
    },

    ////////////////////////////////////////////////////////////////////////
    // multiDeclaration - multiple declarations
    ////////////////////////////////////////////////////////////////////////

    // TODO(ihab.awad): The 'multiDeclaration' implementation is hard
    // to follow or maintain. Refactor asap.
    new Rule() {
      @Override
      @RuleDescription(
          name="multiDeclaration",
          synopsis="Consider declarations separately from initializers",
          reason="",
          matches="var @a=@b?, @c=@d*",
          substitutes="{ @decl; @init; }")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        if (node instanceof MultiDeclaration) {
          boolean allDeclarations = true;
          List<ParseTreeNode> expanded = new ArrayList<ParseTreeNode>();

          // Expand each declaration individually, and keep track of whether
          // the result is a declaration or whether we can just run the
          // initializers separately.
          for (ParseTreeNode child : node.children()) {
            ParseTreeNode result = expand(child, scope);
            if (result instanceof ExpressionStmt) {
              result = result.children().get(0);
            } else if (!(result instanceof Expression
                         || result instanceof Declaration)) {
              throw new SomethingWidgyHappenedError(
                  "Unexpected result class: " + result.getClass());
            }
            expanded.add(result);
            allDeclarations &= result instanceof Declaration;
          }

          // If they're not all declarations, then split the initializers out
          // so that we can run them in order.
          List<Declaration> declarations = new ArrayList<Declaration>();
          List<Expression> initializers = new ArrayList<Expression>();
          if (allDeclarations) {
            for (ParseTreeNode n : expanded) {
              declarations.add((Declaration) n);
            }
          } else {
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
          }
          if (initializers.isEmpty()) {
            return new MultiDeclaration(node.getFilePosition(), declarations);
          } else if (declarations.isEmpty()) {
            return newExprStmt(newCommaOperation(initializers));
          } else {
            Expression init = newCommaOperation(initializers);
            return substV(
                "decl", new MultiDeclaration(
                    FilePosition.UNKNOWN, declarations),
                "init", newExprStmt(init));
          }
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
          name="mapBadKeyValueOf",
          synopsis="Statically reject 'valueOf' as a key",
          reason="We depend on valueOf returning consistent results.",
          matches="({@key: @val})",
          substitutes="<reject>")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = matchSingleMap(node);
        if (bindings != null) {
          StringLiteral key = (StringLiteral) bindings.get("key");
          if (key.getUnquotedValue().equals("valueOf")) {
            mq.addMessage(
                RewriterMessageType.VALUEOF_PROPERTY_MUST_NOT_BE_SET,
                key.getFilePosition(), this, key);
            return node;
          }
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="mapBadKeySuffix",
          synopsis="Statically reject if a key with `__` suffix is found",
          reason="",
          matches="({@key: @val})",
          substitutes="<reject>")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = matchSingleMap(node);
        if (bindings != null) {
          StringLiteral key = (StringLiteral) bindings.get("key");
          if (key.getUnquotedValue().endsWith("__")) {
            mq.addMessage(
                RewriterMessageType.PROPERTIES_CANNOT_END_IN_DOUBLE_UNDERSCORE,
                key.getFilePosition(), this, key);
            return node;
          }
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="mapSingle",
          synopsis="Turns an object literal into an explicit initialization.",
          reason="To avoid creating even a temporary possibly unsafe object " +
              "(such as one with a bad 'toString' method), pass an " +
              "array of a @key and a @val.",
          matches="({@key: @val})",
          substitutes="___.iM([@key, @val])")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = matchSingleMap(node);
        if (bindings != null) {
          StringLiteral key = (StringLiteral) bindings.get("key");
          ParseTreeNode val = bindings.get("val");
          return substV(
              "key", noexpand(key),
              "val", expand(nymize(val, key.getUnquotedValue(), "lit"), scope));
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="mapPlural",
          synopsis="Turns an object literal into an explicit initialization.",
          reason="To avoid creating even a temporary possibly unsafe object " +
              "(such as one with a bad 'toString' method), pass an " +
              "array of @items, which are interleaved @keys and @vals.",
          matches="({@keys*: @vals*})",
          substitutes="___.iM([@items*])")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = match(node);
        if (bindings != null) {
          List<ParseTreeNode> items = new ArrayList<ParseTreeNode>();
          List<? extends ParseTreeNode> keys = bindings.get("keys").children();
          List<? extends ParseTreeNode> vals = bindings.get("vals").children();
          int len = keys.size();
          if (1 == len) {
            mq.addMessage(
                RewriterMessageType.MAP_RECURSION_FAILED,
                node.getFilePosition(), node);
          }
          for (int i = 0, n = len; i < n; ++i) {
            ParseTreeNode pairIn = substSingleMap(keys.get(i), vals.get(i));
            ParseTreeNode pairOut = expand(pairIn, scope);
            Map<String, ParseTreeNode> pairBindings = makeBindings();
            if (!QuasiBuilder.match("___.iM([@key, @val])",
                                    pairOut, pairBindings)) {
              mq.addMessage(
                  RewriterMessageType.MAP_RECURSION_FAILED,
                  node.getFilePosition(), node);
            } else {
              items.add(pairBindings.get("key"));
              items.add(pairBindings.get("val"));
            }
          }
          return substV(
              "items", new ParseTreeNodeContainer(items));
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
          name="otherTypeof",
          synopsis="Typeof translates simply",
          reason="One of Caja's deviations from JavaScript is that reading a "
              + "non-existent imported variable returns 'undefined' rather than "
              + "throwing a ReferenceError. Therefore, in Caja, 'typeof' can "
              + "always evaluate its argument.",
          matches="typeof @f",
          substitutes="___.typeOf(@f)")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        return transform(node, scope);
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="inPublic",
          synopsis="Is a public property present on the object?",
          reason="",
          matches="@i in @o",
          substitutes="___.inPub(@i, @o)")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        return transform(node, scope);
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="voidOp",
          synopsis="",
          reason="",
          matches="void @x",
          substitutes="void @x")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        return transform(node, scope);
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="commaOp",
          synopsis="",
          reason="",
          matches="(@a, @b)",
          substitutes="(@a, @b)")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        return transform(node, scope);
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="breakStmt",
          synopsis="disallow labels that end in __",
          reason="",
          matches="break @a;",
          substitutes="break @a;")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        if (node instanceof BreakStmt) {
          String label = ((BreakStmt) node).getLabel();
          if (label.endsWith("__")) {
            mq.addMessage(
                RewriterMessageType.LABELS_CANNOT_END_IN_DOUBLE_UNDERSCORE,
                node.getFilePosition(),
                MessagePart.Factory.valueOf(label));
          }
          return noexpand((BreakStmt) node);
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="continueStmt",
          synopsis="disallow labels that end in __",
          reason="",
          matches="continue @a;",
          substitutes="continue @a;")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        if (node instanceof ContinueStmt) {
          String label = ((ContinueStmt) node).getLabel();
          if (label.endsWith("__")) {
            mq.addMessage(
                RewriterMessageType.LABELS_CANNOT_END_IN_DOUBLE_UNDERSCORE,
                node.getFilePosition(),
                MessagePart.Factory.valueOf(label));
          }
          return noexpand((ContinueStmt) node);
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="regexLiteral",
          synopsis="Cajita requires use of the regular expression constructor",
          reason="Because JavaScript regex literal instances are "
              + "shared by default, and regex literal lexing is difficult.",
          matches="/foo/",
          substitutes="<reject>")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        if (node instanceof RegexpLiteral) {
          mq.addMessage(
              RewriterMessageType.REGEX_LITERALS_NOT_IN_CAJITA,
              node.getFilePosition(), this, node);
          return node;
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="useSubsetDirective",
          synopsis="replace use subset directives with noops",
          reason="rewriting changes the block structure of the input, which"
              + " could lead to a directive appearing in an illegal position"
              + " since directives must appear at the beginning of a program"
              + " or function body, not in an arbitrary block",
          matches="'use';",
          substitutes=";")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        if (node instanceof DirectivePrologue) {
          return new Noop(node.getFilePosition());
        }
        return NONE;
      }
    },

    ////////////////////////////////////////////////////////////////////////
    // recurse - automatically recurse into some structures
    ////////////////////////////////////////////////////////////////////////

    new Rule() {
      @Override
      @RuleDescription(
          name="recurse",
          synopsis="Automatically recurse into some structures",
          reason="",
          matches="<many>")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        if (node instanceof ParseTreeNodeContainer ||
            node instanceof ArrayConstructor ||
            node instanceof CaseStmt ||
            node instanceof Conditional ||
            node instanceof DebuggerStmt ||
            node instanceof DefaultCaseStmt ||
            node instanceof ExpressionStmt ||
            node instanceof FormalParam ||
            node instanceof Identifier ||
            node instanceof LabeledStmtWrapper ||
            node instanceof Literal ||
            node instanceof Loop ||
            node instanceof Noop ||
            node instanceof SimpleOperation ||
            node instanceof ControlOperation ||
            node instanceof ReturnStmt ||
            node instanceof SwitchStmt ||
            node instanceof ThrowStmt) {
          return expandAll(node, scope);
        }
        return NONE;
      }
    }
  };

  public CajitaRewriter(
      BuildInfo buildInfo, ModuleManager moduleManager, MessageQueue mq,
      boolean logging) {
    super(mq, true, logging);
    this.buildInfo = buildInfo;
    this.moduleManager = moduleManager;
    addRules(SyntheticRuleSet.syntheticRules(this));
    addRules(cajaRules);
  }

  public CajitaRewriter(BuildInfo buildInfo, MessageQueue mq, boolean logging) {
    this(buildInfo, null, mq, logging);
  }
}
