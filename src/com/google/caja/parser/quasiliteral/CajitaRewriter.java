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

import com.google.caja.parser.AbstractParseTreeNode;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.ParseTreeNodeContainer;
import com.google.caja.parser.ParseTreeNodes;
import com.google.caja.parser.js.ArrayConstructor;
import com.google.caja.parser.js.AssignOperation;
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.BreakStmt;
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
import com.google.caja.parser.js.LabeledStmtWrapper;
import com.google.caja.parser.js.Literal;
import com.google.caja.parser.js.Loop;
import com.google.caja.parser.js.ModuleEnvelope;
import com.google.caja.parser.js.MultiDeclaration;
import com.google.caja.parser.js.Noop;
import com.google.caja.parser.js.Operation;
import com.google.caja.parser.js.Operator;
import com.google.caja.parser.js.Reference;
import com.google.caja.parser.js.RegexpLiteral;
import com.google.caja.parser.js.ReturnStmt;
import com.google.caja.parser.js.SimpleOperation;
import com.google.caja.parser.js.Statement;
import com.google.caja.parser.js.SwitchStmt;
import com.google.caja.parser.js.ThrowStmt;
import com.google.caja.parser.js.TranslatedCode;
import com.google.caja.parser.js.TryStmt;
import com.google.caja.parser.js.UseSubsetDirective;
import com.google.caja.util.Pair;
import com.google.caja.util.SyntheticAttributeKey;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageQueue;

import static com.google.caja.parser.js.SyntheticNodes.s;

import java.util.ArrayList;
import java.util.Collection;
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
   * emit it's value to a variable "moduleResult___" so that it can used as
   * the result of module loading.
   */
  @SuppressWarnings("unchecked")
  public static ParseTreeNode returnLast(ParseTreeNode node) {
    ParseTreeNode result = null;
    // Code translated from another language should not be used as the module
    // result.
    if (node.getAttributes().is(TRANSLATED)) { return node; }
    if (node instanceof ExpressionStmt) {
      result = new ExpressionStmt((Expression) QuasiBuilder.substV(
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
        result = new Block(stats);
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
      result = new Conditional(null, nodes);
    } else if (node instanceof TryStmt) {
      TryStmt tryer = (TryStmt) node;
      result = new TryStmt((Statement) returnLast(tryer.getBody()),
          tryer.getCatchClause(),
          tryer.getFinallyClause());
    }
    if (null == result) { return node; }
    result.getAttributes().putAll(node.getAttributes());
    if (result instanceof AbstractParseTreeNode) {
      ((AbstractParseTreeNode<?>) result)
          .setFilePosition(node.getFilePosition());
    }
    return result;
  }

  // A NOTE ABOUT MATCHING MEMBER ACCESS EXPRESSIONS
  // When we match the pattern like '@x.@y' or '@x.@y()' against a specimen,
  // the result is that 'y' is bound to the rightmost component, and 'x' is
  // the remaining sub-expression on the left. Thus the result of matching
  //     @x.@y, @x.@y(), @x.@y(arg), @x.@y(args*), ...
  // is that 'y' is always bound to a Reference.

  final public Rule[] cajaRules = {
    new Rule() {
      @Override
      @RuleDescription(
          name="moduleEnvelope",
          synopsis="Cajole a ModuleEnvelope into a call to ___.loadModule.",
          reason="So that the module loader can be invoked to load a module.",
          matches="<a ModuleEnvelope>",
          substitutes=(
              "{"
              + "  ___./*@synthetic*/loadModule("
              + "      /*@synthetic*/function (___, IMPORTS___) {"
              + "        var moduleResult___ = ___.NO_RESULT;"
              + "        @body*;"
              + "        return moduleResult___;"
              + "      });"
              + "}"))
      public ParseTreeNode fire(
          ParseTreeNode node, Scope scope, MessageQueue mq) {
        if (node instanceof ModuleEnvelope) {
          // TODO(erights): Pull manifest up into module record.
          ModuleEnvelope rewritten = (ModuleEnvelope) expandAll(node, null, mq);
          ParseTreeNodeContainer moduleStmts = new ParseTreeNodeContainer(
              rewritten.getModuleBody().children());
          return substV("body", returnLast(moduleStmts));
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="translatedCode",
          synopsis="Allow code received from a *->JS translator",
          reason="Translated code should not be treated as user supplied JS.",
          matches="<TranslatedCode>")
      public ParseTreeNode fire(
          ParseTreeNode node, Scope scope, MessageQueue mq) {
        if (node instanceof TranslatedCode) {
          Statement rewritten
              = ((TranslatedCode) expandAll(node, scope, mq)).getTranslation();
          markTranslated(rewritten);
          return rewritten;
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
      public ParseTreeNode fire(
          ParseTreeNode node, Scope scope, MessageQueue mq) {
        if (node instanceof Block && scope == null) {
          Scope s2 = Scope.fromProgram((Block) node, mq);
          List<ParseTreeNode> expanded = new ArrayList<ParseTreeNode>();
          for (ParseTreeNode c : node.children()) {
            expanded.add(expand(c, s2, mq));
          }
          List<ParseTreeNode> importedVars = new ArrayList<ParseTreeNode>();

          Set<String> importNames = s2.getImportedVariables();
          // Order imports so that Array and Object appear first, and so that
          // they appear before any use of the [] and {} shorthand syntaxes
          // since those are specified in ES262 by looking up the identifiers
          // "Array" and "Object" in the local scope.
          // SpiderMonkey actually implements this behavior, though it is fixed
          // in FF3, and ES3.1 is specifying the behavior of [] and {} in terms
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
            Identifier kid = new Identifier(k);
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
    // Do nothing if the node is already the result of some translation
    ////////////////////////////////////////////////////////////////////////

    new Rule() {
      @Override
      @RuleDescription(
          name="syntheticReference",
          synopsis="Pass through synthetic references.",
          reason="A variable may not be mentionable otherwise.",
          matches="/* synthetic */ @ref",
          substitutes="<expanded>")
      public ParseTreeNode fire(
          ParseTreeNode node, Scope scope, MessageQueue mq) {
        if (node instanceof Reference) {
          Reference ref = (Reference) node;
          if (isSynthetic(ref.getIdentifier())) {
            return node;
          }
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="syntheticCalls",
          synopsis="Pass through calls where the method name is synthetic.",
          reason="A synthetic method may not be marked callable.",
          matches="/* synthetic */ @o.@m(@as*)",
          substitutes="<expanded>")
      public ParseTreeNode fire(
          ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = this.match(node);
        if (bindings != null && isSynthetic((Reference) bindings.get("m"))) {
          return expandAll(node, scope, mq);
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
      public ParseTreeNode fire(
          ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = this.match(node);
        if (bindings != null && isSynthetic((Reference) bindings.get("m"))) {
          return expandAll(node, scope, mq);
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
      public ParseTreeNode fire(
          ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = this.match(node);
        if (bindings != null && isSynthetic((Reference) bindings.get("m"))) {
          return expandAll(node, scope, mq);
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
      public ParseTreeNode fire(
          ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = this.match(node);
        if (bindings != null && isSynthetic((Reference) bindings.get("m"))) {
          return expandAll(node, scope, mq);
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
      public ParseTreeNode fire(
          ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = this.match(node);
        if (bindings != null && bindings.get("lhs") instanceof Reference) {
          if (isSynthetic((Reference)bindings.get("lhs"))) {
            return expandAll(node, scope, mq);
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
      public ParseTreeNode fire(
          ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = this.match(node);
        if (bindings != null && isSynthetic((Identifier) bindings.get("v"))) {
          return expandAll(node, scope, mq);
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
      public ParseTreeNode fire(
          ParseTreeNode node, Scope scope, MessageQueue mq) {
        FunctionConstructor ctor = node instanceof FunctionDeclaration
            ? ((FunctionDeclaration) node).getInitializer()
            : (FunctionConstructor) node;
        if (isSynthetic(ctor)) {
          return expandAll(node, scope, mq);
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
      public ParseTreeNode fire(
          ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = this.match(node);
        if (bindings != null) {
          Identifier ex = (Identifier) bindings.get("ex");
          if (isSynthetic(ex)) {
            expandEntries(bindings, scope, mq);
            return subst(bindings);
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
      public ParseTreeNode fire(
          ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = this.match(node);
        if (bindings != null) {
          Identifier ex = (Identifier) bindings.get("ex");
          if (isSynthetic(ex)) {
            expandEntries(bindings, scope, mq);
            return subst(bindings);
          }
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
              + "re-enters the enclosing block.\n"
              + "Note that ES3.1 and ES4 specify a better and safer semantics "
              + "-- block level lexical scoping -- that we'd like to adopt into "
              + "Cajita eventually. However, it so challenging to implement this "
              + "semantics by translation to currently-implemented JavaScript "
              + "that we provide something quicker and dirtier for now.",
          matches="{@ss*;}",
          substitutes="@startStmts*; @ss*;")
      public ParseTreeNode fire(
          ParseTreeNode node, Scope scope, MessageQueue mq) {
        if (node instanceof Block) {
          List<ParseTreeNode> expanded = new ArrayList<ParseTreeNode>();
          Scope s2 = Scope.fromPlainBlock(scope);
          for (ParseTreeNode c : node.children()) {
            expanded.add(expand(c, s2, mq));
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
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
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
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
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
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
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
            "s0",  expandAll(bindings.get("s0"), scope, mq),
            "s1",  expandAll(bindings.get("s1"),
                             Scope.fromCatchStmt(scope, t.getCatchClause()), mq),
            "x", bindings.get("x"));
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
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
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
            "s0",  expandAll(bindings.get("s0"), scope, mq),
            "s1",  expandAll(bindings.get("s1"),
                             Scope.fromCatchStmt(scope, t.getCatchClause()), mq),
            "s2",  expandAll(bindings.get("s2"), scope, mq),
            "x", bindings.get("x"));
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
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = match(node);
        if (bindings != null) {
          return substV(
            "s0",  expandAll(bindings.get("s0"), scope, mq),
            "s1",  expandAll(bindings.get("s1"), scope, mq));
        }
        return NONE;
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
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = match(node);
        if (bindings != null) {
          return subst(bindings);
        }
        return NONE;
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
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
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
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
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
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
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
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = match(node);
        if (bindings != null
            && scope.isDeclaredFunctionReference(bindings.get("fname"))) {
          return subst(bindings);
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
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = match(node);
        if (bindings != null && bindings.get("v") instanceof Reference) {
          return bindings.get("v");
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
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
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
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
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
          name="readPublic",
          synopsis="",
          reason="",
          matches="@o.@p",
          substitutes="<approx> ___.readPub(@o, @'p')")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = match(node);
        if (bindings != null) {
          Reference p = (Reference) bindings.get("p");
          String propertyName = p.getIdentifierName();
          return QuasiBuilder.substV(
              "@ref = @o, ("
              + "    @ref.@fp"
              + "    ? @ref.@p"
              + "    : ___.readPub(@ref, @rp))",
              "ref", new Reference(scope.declareStartOfScopeTempVariable()),
              "o", expand(bindings.get("o"), scope, mq),
              "p",  p,
              "fp", newReference(propertyName + "_canRead___"),
              "rp", toStringLiteral(p));
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="readNumPublic",
          synopsis="Recognize that numeric indexing is inherently safe.",
          reason="When the developer knows that their index expression is numeric, " +
              "they can indicate this with the unary plus operator -- which " +
              "coerces to a number. Since numeric properties are necessarily " +
              "readable, we can pass these through directly to JavaScript.",
          matches="@o[+@s]",
          substitutes="@o[+@s]")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = match(node);
        if (bindings != null) {
          return substV(
              "o", expand(bindings.get("o"), scope, mq),
              "s", expand(bindings.get("s"), scope, mq));
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
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = match(node);
        if (bindings != null) {
          return substV(
              "o", expand(bindings.get("o"), scope, mq),
              "s", expand(bindings.get("s"), scope, mq));
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
          name="setBadAssignToFunctionName",
          synopsis="Statically reject if an assignment expression assigns to a "
              + "function name.",
          reason="",
          matches="<approx> @fname @op?= @x", // TODO(mikesamuel): Limit further
          substitutes="<reject>")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
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
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
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
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
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
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
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
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
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
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
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
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = match(node);
        if (bindings != null && bindings.get("fname") instanceof Reference) {
          Reference fname = (Reference) bindings.get("fname");
          Reference p = (Reference) bindings.get("p");
          if (scope.isDeclaredFunction(getReferenceName(fname))) {
            return QuasiBuilder.substV(
                "___.setStatic(@fname, @rp, @r)",
                "fname", fname,
                "rp", toStringLiteral(p),
                "r", expand(bindings.get("r"), scope, mq));
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
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = match(node);
        if (bindings != null) {
          Reference p = (Reference) bindings.get("p");
          String propertyName = p.getIdentifierName();
          return QuasiBuilder.substV(
              "@tmpO = @expandO," +
              "@tmpR = @expandR," +
              "@tmpO.@pCanSet ?" +
              "    (@tmpO.@p = @tmpR) :" +
              "    ___.setPub(@tmpO, @pName, @tmpR);",
              "tmpO", new Reference(scope.declareStartOfScopeTempVariable()),
              "tmpR", new Reference(scope.declareStartOfScopeTempVariable()),
              "expandO", expand(bindings.get("o"), scope, mq),
              "expandR", expand(bindings.get("r"), scope, mq),
              "pCanSet", newReference(propertyName + "_canSet___"),
              "p", p,
              "pName", toStringLiteral(p));
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
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = match(node);
        if (bindings != null) {
          return substV(
              "o", expand(bindings.get("o"), scope, mq),
              "s", expand(bindings.get("s"), scope, mq),
              "r", expand(bindings.get("r"), scope, mq));
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
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
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
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = match(node);
        if (bindings != null
            && !scope.isFunction(getIdentifierName(bindings.get("v")))) {
          return substV(
              "v", bindings.get("v"),
              "r", expand(bindings.get("r"), scope, mq));
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
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
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
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = match(node);
        if (bindings != null
            && !scope.isFunction(getIdentifierName(bindings.get("v")))) {
          return node;
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
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
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
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = match(node);
        if (bindings != null) {
          ParseTreeNode v = bindings.get("v");
          if (v instanceof Reference) {
            if (!scope.isFunction(getReferenceName(v))) {
              return substV(
                  "v", v,
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
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        if (node instanceof AssignOperation) {
          AssignOperation aNode = (AssignOperation) node;
          Operator op = aNode.getOperator();
          if (op.getAssignmentDelegate() == null) { return NONE; }

          ReadAssignOperands ops = deconstructReadAssignOperand(
              aNode.children().get(0), scope, mq);
          if (ops == null) { return node; }  // Error deconstructing

          // For x += 3, rhs is (x + 3)
          Operation rhs = Operation.create(
              op.getAssignmentDelegate(),
              ops.getUncajoledLValue(), aNode.children().get(1));
          rhs.setFilePosition(aNode.children().get(0).getFilePosition());
          Operation assignment = ops.makeAssignment(rhs);
          assignment.setFilePosition(aNode.getFilePosition());
          if (ops.getTemporaries().isEmpty()) {
            return expand(assignment, scope, mq);
          } else {
            return QuasiBuilder.substV(
                "@tmps, @assignment",
                "tmps", newCommaOperation(ops.getTemporaries()),
                "assignment", expand(assignment, scope, mq));
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
              return QuasiBuilder.substV("@v ++", "v", ops.getCajoledLValue());
            } else {
              Reference tmpVal = new Reference(
                  scope.declareStartOfScopeTempVariable());
              Expression assign = (Expression) expand(
                  ops.makeAssignment((Expression) QuasiBuilder.substV(
                      "@tmpVal + 1", "tmpVal", tmpVal)),
                  scope, mq);
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
                  scope, mq);
            } else {
              return QuasiBuilder.substV(
                  "  @tmps,"
                  + "@assign",
                  "tmps", newCommaOperation(ops.getTemporaries()),
                  "assign", expand(
                      ops.makeAssignment((Expression) QuasiBuilder.substV(
                          "@rvalue - -1", "rvalue", ops.getUncajoledLValue())),
                      scope, mq));
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
                  scope, mq);
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
                          "@rvalue - 1", "rvalue",
                          ops.getUncajoledLValue())),
                  scope, mq);
            } else {
              return QuasiBuilder.substV(
                  "  @tmps,"
                  + "@assign",
                  "tmps", newCommaOperation(ops.getTemporaries()),
                  "assign", expand(
                      ops.makeAssignment((Expression) QuasiBuilder.substV(
                          "@rvalue - 1", "rvalue", ops.getUncajoledLValue())),
                      scope, mq));
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
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = match(node);
        if (bindings != null) {
          return subst(bindings);
        }
        return NONE;
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
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = match(node);
        if (bindings != null) {
          return subst(bindings);
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
          name="deleteBadValueOf",
          synopsis="Prohibit deletion of valueOf.",
          reason="Although a non-existent valueOf should behave the same way "
              + "asthe default one as regards [[DefaultValue]], for simplicity "
              + "weonly want to have to consider one of those cases.",
          matches="delete @o.valueOf",
          substitutes="<reject>")
      public ParseTreeNode fire(
          ParseTreeNode node, Scope scope, MessageQueue mq) {
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
      public ParseTreeNode fire(
          ParseTreeNode node, Scope scope, MessageQueue mq) {
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
      public ParseTreeNode fire(
          ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = match(node);
        if (bindings != null) {
          Reference p = (Reference) bindings.get("p");
          return QuasiBuilder.substV(
              "___.deletePub(@o, @pname)",
              "o", expand(bindings.get("o"), scope, mq),
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
      public ParseTreeNode fire(
          ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = match(node);
        if (bindings != null) {
          return substV(
              "o", expand(bindings.get("o"), scope, mq),
              "s", expand(bindings.get("s"), scope, mq));
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
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
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
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
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
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = match(node);
        if (bindings != null) {
          ParseTreeNode o = bindings.get("o");
          Permit oPermit = scope.permitRead(o);
          if (null != oPermit) {
            ParseTreeNode m = bindings.get("m");
            if (null != oPermit.canCall(m)) {
              return substV(
                  "o",  expand(o, scope, mq),
                  "m",  m,
                  "as", expandAll(bindings.get("as"), scope, mq));
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
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = match(node);
        if (bindings != null) {
          Pair<ParseTreeNode, ParseTreeNode> aliases =
              reuseAll(bindings.get("as"), scope, mq);
          Reference m = (Reference) bindings.get("m");
          String methodName = m.getIdentifierName();
          return QuasiBuilder.substV(
              "@oTmp = @o," +
              "@as," +
              "@oTmp.@fm ? @oTmp.@m(@vs*) : ___.callPub(@oTmp, @rm, [@vs*]);",
              "oTmp", new Reference(scope.declareStartOfScopeTempVariable()),
              "o",  expand(bindings.get("o"), scope, mq),
              "as", newCommaOperation(aliases.b.children()),
              "vs", aliases.a,
              "m",  m,
              "fm", newReference(methodName + "_canCall___"),
              "rm", toStringLiteral(m));
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
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = match(node);
        if (bindings != null) {
          expandEntries(bindings, scope, mq);
          return subst(bindings);
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="callDeclaredFunc",
          synopsis="When calling a declared function name, leave the freezing to asSimpleFunc.",
          reason="If @fname is a declared function name, an escaping use as " +
              "here would normally generate a call to primFreeze it, so that " +
              "it's frozen on first use. However, since asSimpleFunc() now " +
              "freezes its argument, if @fname is a declared function name, " +
              "we avoid expanding it.",
          matches="@fname(@as*)",
          substitutes="___.asSimpleFunc(@fname)(@as*)")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = match(node);
        if (bindings != null &&
            scope.isDeclaredFunctionReference(bindings.get("fname"))) {
          return substV(
              "fname", bindings.get("fname"),
              "as", expandAll(bindings.get("as"), scope, mq));
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
          substitutes="___.asSimpleFunc(@f)(@as*)")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = match(node);
        if (bindings != null) {
          return substV(
              "f", expand(bindings.get("f"), scope, mq),
              "as", expandAll(bindings.get("as"), scope, mq));
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
          name="funcAnonSimple",
          synopsis="",
          reason="",
          matches="function (@ps*) { @bs*; }",
          substitutes="___.simpleFrozenFunc(\n"
              + "  function (@ps*) {\n"
              + "    @fh*;\n"
              + "    @stmts*;\n"
              + "    @bs*;\n"
              + "})")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = match(node);
        // Anonymous simple function constructor
        if (bindings != null) {
          Scope s2 = Scope.fromFunctionConstructor(scope, (FunctionConstructor) node);
          checkFormals(bindings.get("ps"), mq);
          return substV(
              "ps", bindings.get("ps"),
              // It's important to expand bs before computing fh and stmts.
              "bs", expand(bindings.get("bs"), s2, mq),
              "fh", getFunctionHeadDeclarations(s2),
              "stmts", new ParseTreeNodeContainer(s2.getStartStatements()));
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="funcNamedSimpleDecl",
          synopsis="",
          reason="",
          matches="function @fname(@ps*) { @bs*; }",
          substitutes="@fname = ___.simpleFunc(\n"
              + "  function(@ps*) {\n"
              + "    @fh*;\n"
              + "    @stmts*;\n"
              + "    @bs*;\n"
              + "}, @'fname');")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = (
            node instanceof FunctionDeclaration)
            ? match(((FunctionDeclaration) node).getInitializer())
            : null;
        // Named simple function declaration
        if (bindings != null) {
          Scope s2 = Scope.fromFunctionConstructor(
              scope,
              ((FunctionDeclaration) node).getInitializer());
          checkFormals(bindings.get("ps"), mq);
          Identifier fname = (Identifier) bindings.get("fname");
          scope.declareStartOfScopeVariable(fname);
          Expression expr = (Expression) QuasiBuilder.substV(
              "@fname = ___.simpleFunc(" +
              "  function(@ps*) {" +
              "    @fh*;" +
              "    @stmts*;" +
              "    @bs*;" +
              "}, @rf);",
              "fname", new Reference(fname),
              "rf", toStringLiteral(fname),
              "ps", bindings.get("ps"),
              // It's important to expand bs before computing fh and stmts.
              "bs", expand(bindings.get("bs"), s2, mq),
              "fh", getFunctionHeadDeclarations(s2),
              "stmts", new ParseTreeNodeContainer(s2.getStartStatements()));
          scope.addStartOfBlockStatement(new ExpressionStmt(expr));
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
              + "  return ___.simpleFrozenFunc(@fname, @'fname');\n"
              + "})();")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = match(node);
        // Named simple function expression
        if (bindings != null) {
          Scope s2 = Scope.fromFunctionConstructor(
              scope,
              (FunctionConstructor) node);
          checkFormals(bindings.get("ps"), mq);
          Identifier fname = (Identifier) bindings.get("fname");
          Reference fRef = new Reference(fname);
          return QuasiBuilder.substV(
              "(function() {\n"
              + "  function @fname(@ps*) {\n"
              + "    @fh*;\n"
              + "    @stmts*;\n"
              + "    @bs*;\n"
              + "  }\n"
              + "  return ___.simpleFrozenFunc(@fRef, @rf);\n"
              + "})();",
              "fname", fname,
              "fRef", fRef,
              "rf", toStringLiteral(fname),
              "ps", bindings.get("ps"),
              // It's important to expand bs before computing fh and stmts.
              "bs", expand(bindings.get("bs"), s2, mq),
              "fh", getFunctionHeadDeclarations(s2),
              "stmts", new ParseTreeNodeContainer(s2.getStartStatements()));
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
          matches="({@keys*: @vals*})",
          substitutes="<reject>")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = match(node);
        if (bindings != null
            && literalsContain(bindings.get("keys"), "valueOf")) {
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
          name="mapBadKeySuffix",
          synopsis="Statically reject if a key with `__` suffix is found",
          reason="",
          matches="({@keys*: @vals*})",
          substitutes="<reject>")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = match(node);
        if (bindings != null && literalsEndWith(bindings.get("keys"), "__")) {
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
          name="mapNonEmpty",
          synopsis="Turns an object literal into an explicit initialization.",
          reason="To avoid creating even a temporary possibly unsafe object " +
              "(such as one with a bad 'toString' method), pass an " +
              "array of @items, which are interleaved @keys and @vals.",
          matches="({@keys*: @vals*})",
          substitutes="___.initializeMap([@items*])")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = match(node);
        if (bindings != null) {
          List<ParseTreeNode> items = new ArrayList<ParseTreeNode>();
          List<? extends ParseTreeNode> keys = bindings.get("keys").children();
          List<? extends ParseTreeNode> vals = expand(bindings.get("vals"), scope, mq).children();
          for (int i = 0, n = keys.size(); i < n; ++i) {
            items.add(keys.get(i));
            items.add(vals.get(i));
          }
          return substV(
              "items", new ParseTreeNodeContainer(items));
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
            if (declarations.isEmpty()) {
              return new ExpressionStmt(newCommaOperation(initializers));
            } else {
              return substV(
                  "decl", new MultiDeclaration(declarations),
                  "init", new ExpressionStmt(newCommaOperation(initializers)));
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
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = match(node);
        if (bindings != null) {
          return substV(
              "f", expand(bindings.get("f"), scope, mq));
        }
        return NONE;
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
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = match(node);
        if (bindings != null) {
          return substV(
              "i", expand(bindings.get("i"), scope, mq),
              "o", expand(bindings.get("o"), scope, mq));
        }
        return NONE;
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
      public ParseTreeNode fire(
          ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = match(node);
        if (bindings != null) { return expandAll(node, scope, mq); }
        return NONE;
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
      public ParseTreeNode fire(
          ParseTreeNode node, Scope scope, MessageQueue mq) {
        Map<String, ParseTreeNode> bindings = match(node);
        if (bindings != null) { return expandAll(node, scope, mq); }
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

    new Rule() {
      @Override
      @RuleDescription(
          name="regexLiteral",
          synopsis="Cajita requires use of the regular expression constructor",
          reason="Because JavaScript regex literal instances are "
              + "shared by default, and regex literal lexing is difficult.",
          matches="/foo/",
          substitutes="<reject>")
      public ParseTreeNode fire(
          ParseTreeNode node, Scope scope, MessageQueue mq) {
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
      public ParseTreeNode fire(ParseTreeNode node, Scope s, MessageQueue mq) {
        if (node instanceof UseSubsetDirective) {
          return new Noop();
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
      public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
        if (node instanceof ParseTreeNodeContainer ||
            node instanceof ArrayConstructor ||
            // TODO(mikesamuel): break/continue with unmentionable label
            node instanceof BreakStmt ||
            node instanceof CaseStmt ||
            node instanceof Conditional ||
            node instanceof ContinueStmt ||
            node instanceof DebuggerStmt ||
            node instanceof DefaultCaseStmt ||
            node instanceof ExpressionStmt ||
            node instanceof FormalParam ||
            node instanceof Identifier ||
            node instanceof Literal ||
            node instanceof Loop ||
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

  /**
   * Creates a default caja rewriter
   */
  public CajitaRewriter() {
    this(false);
  }

  public CajitaRewriter(boolean logging) {
    super(logging);
    addRules(cajaRules);
  }
}
