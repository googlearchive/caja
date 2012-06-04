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

import static com.google.caja.parser.js.SyntheticNodes.s;

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
import com.google.caja.parser.js.DirectivePrologue;
import com.google.caja.parser.js.Elision;
import com.google.caja.parser.js.Expression;
import com.google.caja.parser.js.ExpressionStmt;
import com.google.caja.parser.js.FormalParam;
import com.google.caja.parser.js.FunctionConstructor;
import com.google.caja.parser.js.FunctionDeclaration;
import com.google.caja.parser.js.GetterProperty;
import com.google.caja.parser.js.Identifier;
import com.google.caja.parser.js.IntegerLiteral;
import com.google.caja.parser.js.LabeledStatement;
import com.google.caja.parser.js.LabeledStmtWrapper;
import com.google.caja.parser.js.Literal;
import com.google.caja.parser.js.Loop;
import com.google.caja.parser.js.MultiDeclaration;
import com.google.caja.parser.js.Noop;
import com.google.caja.parser.js.NumberLiteral;
import com.google.caja.parser.js.ObjProperty;
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
import com.google.caja.parser.js.ValueProperty;
import com.google.caja.reporting.BuildInfo;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.util.Lists;
import com.google.caja.util.Sets;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Rewrites a JavaScript parse tree to comply with default Caja rules.
 *
 * <p>By design, a rewriter is a "one-shot" object to be used for rewriting
 * one module then discarded.
 *
 * @author ihab.awad@gmail.com (Ihab Awad)
 */
@RulesetDescription(
    name="ES5/3 Transformation Rules",
    synopsis="Default set of transformations used by ES5/3"
  )
public class ES53Rewriter extends Rewriter {
  private final BuildInfo buildInfo;
  private final Set<StringLiteral> includedModules = Sets.newTreeSet(
      new Comparator<StringLiteral>() {
    @Override
    public int compare(StringLiteral o1, StringLiteral o2) {
      return o1.getUnquotedValue().compareTo(o2.getUnquotedValue());
    }
  });
  private final Set<StringLiteral> inlinedModules = Sets.newTreeSet(
      new Comparator<StringLiteral>() {
    @Override
    public int compare(StringLiteral o1, StringLiteral o2) {
      return o1.getUnquotedValue().compareTo(o2.getUnquotedValue());
    }
  });

  /** Index of the last node that wasn't translated from another language. */
  private int lastRealJavascriptChild(
      List<? extends ParseTreeNode> nodes) {
    int lasti = nodes.size();
    while (--lasti >= 0) {
      if (!isForSideEffect(nodes.get(lasti))) {
        break;
      }
    }
    return lasti;
  }

  /**
   * Generate the header that should be placed at the beginning of the body
   * of the translation of an ES5/3 function body.
   *
   * @param scope The scope that results from expanding (cajoling) the ES5/3
   *              function body.
   * @return If the function body contains a free use of <tt>arguments</tt>,
   *         translate to an initialization of cajoled arguments based on
   *         an entry snapshot of the real ones. If the function body
   *         contains a free use of <tt>this</tt>, translate to an
   *         initialization of <tt>dis___</tt> to a sanitized this, by
   *         replacing the global object with <tt>void 0</tt>.
   */
  public static ParseTreeNode getFunctionHeadDeclarations(Scope scope) {
    List<ParseTreeNode> stmts = Lists.newArrayList();

    if (scope.hasFreeArguments()) {
      stmts.add(QuasiBuilder.substV(
          "___.deodorize(@ga, -6);" +
          "var @la = ___.args(@ga);",
          "la", s(new Identifier(
              FilePosition.UNKNOWN, ReservedNames.LOCAL_ARGUMENTS)),
          "ga", Rule.newReference(FilePosition.UNKNOWN,
                                  ReservedNames.ARGUMENTS)));
    }
    if (scope.hasFreeThis()) {
      stmts.add(QuasiBuilder.substV(
          "var dis___ = (this && this.___) ? void 0 : this;"));
    }
    return new ParseTreeNodeContainer(stmts);
  }

  /**
   * Find the last expression statement executed in a block of code and
   * emit its value to a variable "moduleResult___" so that it can used as
   * the result of module loading.
   */
  @SuppressWarnings("unchecked")
  public ParseTreeNode returnLast(ParseTreeNode node) {
    ParseTreeNode result = null;
    // Code translated from another language should not be used as the module
    // result.
    if (isForSideEffect(node)) { return node; }
    if (node instanceof ExpressionStmt) {
      result = new ExpressionStmt(
          node.getFilePosition(),
          (Expression) QuasiBuilder.substV(
              "moduleResult___ = @result;",
              "result", ((ExpressionStmt) node).getExpression()));
    } else if (node instanceof ParseTreeNodeContainer) {
      List<ParseTreeNode> nodes = Lists.newArrayList(node.children());
      int lasti = lastRealJavascriptChild(nodes);
      if (lasti >= 0) {
        nodes.set(lasti, returnLast(nodes.get(lasti)));
        result = new ParseTreeNodeContainer(nodes);
      }
    } else if (node instanceof Block) {
      List<Statement> stats = Lists.newArrayList();
      stats.addAll((Collection<? extends Statement>) node.children());
      int lasti = lastRealJavascriptChild(stats);
      if (lasti >= 0) {
        stats.set(lasti, (Statement) returnLast(stats.get(lasti)));
        result = new Block(node.getFilePosition(), stats);
      }
    } else if (node instanceof Conditional) {
      List<ParseTreeNode> nodes = Lists.newArrayList();
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

  private static final FilePosition UNK = FilePosition.UNKNOWN;

  // A NOTE ABOUT MATCHING MEMBER ACCESS EXPRESSIONS
  // When we match the pattern like '@x.@y' or '@x.@y()' against a specimen,
  // the result is that 'y' is bound to the rightmost component, and 'x' is
  // the remaining sub-expression on the left. Thus the result of matching
  //     @x.@y, @x.@y(), @x.@y(arg), @x.@y(args*), ...
  // is that 'y' is always bound to a Reference.

  private final Rule[] cajaRules = {

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
          matches="<TranslatedCode>",
          matchNode=TranslatedCode.class)
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        if (node instanceof TranslatedCode) {
          Statement rewritten
              = ((TranslatedCode) expandAll(node, scope)).getTranslation();
          markTreeForSideEffect(rewritten);
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
          matchNode=UncajoledModule.class,
          substitutes=(
              ""
              + "(/*@synthetic*/{"
              + "  instantiate: /*@synthetic*/function (___, IMPORTS___) {"
              + "    /*var moduleResult___ = ___.NO_RESULT;*/"
              + "    @rewrittenModuleStmts*;"
              + "    /*return moduleResult___;*/"
              + "  },"
              + "  @metaKeys*: @metaValues*,"
              + "  cajolerName: @cajolerName,"
              + "  cajolerVersion: @cajolerVersion,"
              + "  cajoledDate: @cajoledDate"
              + "})"))
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        if (node instanceof UncajoledModule) {
          Statement returnStmt = (Statement) QuasiBuilder.substV(
              "return moduleResult___;");
          markTreeForSideEffect(returnStmt);
          Block inputModuleStmts = (Block) QuasiBuilder.substV(
              ""
              + "var moduleResult___ = ___./*@synthetic*/NO_RESULT;"
              + "@moduleBody*;"
              + "@returnStmt",
              "moduleBody", new ParseTreeNodeContainer(
                  ((UncajoledModule) node).getModuleBody().children()),
              "returnStmt", returnStmt);
          Block rewrittenModuleStmts = (Block) expand(inputModuleStmts, null);
          ParseTreeNodeContainer metaKeys = new ParseTreeNodeContainer();
          ParseTreeNodeContainer metaValues = new ParseTreeNodeContainer();
          if (!includedModules.isEmpty()) {
            metaKeys.appendChild(StringLiteral.valueOf(UNK, "includedModules"));
            metaValues.appendChild(
                new ArrayConstructor(UNK, Lists.newArrayList(includedModules)));
          }
          if (!inlinedModules.isEmpty()) {
            metaKeys.appendChild(StringLiteral.valueOf(UNK, "inlinedModules"));
            metaValues.appendChild(
                new ArrayConstructor(UNK, Lists.newArrayList(inlinedModules)));
          }

          ObjectConstructor moduleObjectLiteral = (ObjectConstructor) substV(
              "rewrittenModuleStmts", returnLast(rewrittenModuleStmts),
              "metaKeys", metaKeys,
              "metaValues", metaValues,
              "cajolerName", new StringLiteral(UNK, "com.google.caja"),
              "cajolerVersion", new StringLiteral(
                  UNK, buildInfo.getBuildVersion()),
              "cajoledDate", new IntegerLiteral(
                  UNK, buildInfo.getCurrentTime()));
          return new CajoledModule(moduleObjectLiteral);
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="module",
          synopsis="Return last expr-statement",
          reason="Builds the module body encapsulation around the ES5/3 "
              + "code block.",
          matches="{@ss*;}",
          substitutes="var dis___ = IMPORTS___; @startStmts*; @expanded*;")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        if (node instanceof Block && scope == null) {
          Scope s2 = Scope.fromProgram((Block) node, ES53Rewriter.this);
          List<ParseTreeNode> expanded = Lists.newArrayList();
          for (ParseTreeNode c : node.children()) {
            ParseTreeNode expandedC = expand(c, s2);
            if (expandedC instanceof Noop) { continue; }
            expanded.add(expandedC);
          }
          return substV(
              "startStmts", new ParseTreeNodeContainer(s2.getStartStatements()),
              "expanded", new ParseTreeNodeContainer(expanded));
        }
        return NONE;
      }
    },

    ////////////////////////////////////////////////////////////////////////
    // Support hoisting of functions to the top of their containing block
    ////////////////////////////////////////////////////////////////////////

    // TODO: (metaweta) Implement block scoping.
    new Rule() {
      @Override
      @RuleDescription(
          name="block",
          synopsis="Initialize named functions at the beginning of their "
              + "enclosing block.",
          reason="Nested named function declarations are illegal in ES3 but are "
              + "universally supported by all JavaScript implementations, "
              + "though in different ways. The compromise semantics currently "
              + "supported by ES5/3 is to hoist the declaration of a variable "
              + "with the function's name to the beginning of the enclosing "
              + "function body or module top level, and to initialize this "
              + "variable to a new anonymous function every time control "
              + "re-enters the enclosing block."
              + "\n"
              + "Note that ES-Harmony will specify a better and safer semantics "
              + "-- block level lexical scoping -- that we'd like to adopt into "
              + "ES5/3 eventually. However, it is so challenging to implement "
              + "this semantics by translation to currently-implemented "
              + "JavaScript that we provide something quicker and dirtier "
              + "for now.",
          matches="{@ss*;}",
          substitutes="@startStmts*; @ss*;")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        if (node instanceof Block) {
          List<Statement> expanded = Lists.newArrayList();
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
              + "reviewers: matching of references with declarations can only "
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

    new Rule () {
      @Override
      @RuleDescription(
          name="foreachExpr",
          synopsis="Filter nonenumerable keys.",
          reason="",
          matches="for (@k in @o) @ss;",
          substitutes=(
              "@ot = Object(@o).e___();" +
              "for (@kts in @ot) {" +
              "  if (typeof @kt === 'number' || ('' + (+@kt)) === @kt) {" +
              "    @assign1; /* k = kt; */" +
              "  } else {" +
              "    if (/^NUM___/.test(@kt) && /__$/.test(@kt)) { continue; }" +
              "    @m = @kt.match(/([\\s\\S]*)_e___$/);" +
              "    if (!@m || !@ot[@kt]) { continue; }" +
              "    @assign2; /* k = @m[1]; */" +
              "  }" +
              "  @ss;" +
              "}"))
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = this.match(node);
        if (bindings != null) {
          Expression k;
          Statement ks = (Statement) bindings.get("k");
          if (ks instanceof ExpressionStmt) {
            k = ((ExpressionStmt) ks).getExpression();
          } else {
            Declaration d = (Declaration) ks;
            if (d.getInitializer() != null
                || d.getIdentifierName().endsWith("__")) {
              return NONE;
            }
            k = new Reference(d.getIdentifier());
            setTaint(k);
            // TODO(mikesamuel): once decls consolidated, no need to add to
            // start of scope.
            scope.addStartOfScopeStatement((Statement) expand(d, scope));
          }
          Reference m = scope.declareStartOfScopeTemp();
          Reference kt = scope.declareStartOfScopeTemp();
          Reference ot = scope.declareStartOfScopeTemp();

          FilePosition unk = FilePosition.UNKNOWN;
          Expression assign1 = Operation.create(unk, Operator.ASSIGN, k, kt);
          setTaint(assign1);
          Expression assign2 = Operation.create(unk, Operator.ASSIGN, k,
              Operation.create(unk, Operator.SQUARE_BRACKET, m,
              new IntegerLiteral(unk, 1)));
          setTaint(assign2);

          ParseTreeNode result = substV(
              "m", m,
              "kt", kt,
              "kts", newExprStmt(kt),
              "ot", ot,
              "o", expand(bindings.get("o"), scope),
              "assign1", newExprStmt((Expression) expand(assign1, scope)),
              "assign2", newExprStmt((Expression) expand(assign2, scope)),
              "ss", expand(bindings.get("ss"), scope));
          return result;
        } else {
          return NONE;
        }
      }
    },

    ////////////////////////////////////////////////////////////////////////
    // try - try/catch/finally constructs
    ////////////////////////////////////////////////////////////////////////

    // TODO: (metaweta) Implement immutability auditor and then only allow
    // throwing immutable objects.
    new Rule() {
      @Override
      @RuleDescription(
          name="tryCatch",
          synopsis="Expand the innards of a try/catch.",
          reason="",
          matches="try { @s0*; } catch (@x) { @s1*; }",
          substitutes="try { @s0*; } catch (@x) { @s1*; }")
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
            "s0",  withoutNoops(expandAll(bindings.get("s0"), scope)),
            "x",   noexpand((Identifier) bindings.get("x")),
            "s1",  withoutNoops(
                        expandAll(bindings.get("s1"),
                        Scope.fromCatchStmt(scope, t.getCatchClause()))));
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="tryCatchFinally",
          synopsis="Expand the innards of a try/catch/finally.",
          reason="",
          matches="try { @s0*; } catch (@x) { @s1*; } finally { @s2*; }",
          substitutes="try { @s0*; } catch (@x) { @s1*; } finally { @s2*; }")
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
            "s0",  withoutNoops(expandAll(bindings.get("s0"), scope)),
            "x",   noexpand((Identifier) bindings.get("x")),
            "s1",  withoutNoops(expandAll(
                       bindings.get("s1"),
                       Scope.fromCatchStmt(scope, t.getCatchClause()))),
            "s2",  withoutNoops(expandAll(bindings.get("s2"), scope)));
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="tryFinally",
          synopsis="Expand the innards of a try/finally.",
          reason="",
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
              + "into references to a fake arguments object",
          reason="ES3 specifies that the magic \"arguments\" variable is a "
              + "dynamic (\"joined\") mutable array-like reflection of the "
              + "values of the parameter variables. However, the typical usage "
              + "is to pass it to provide access to one's original arguments, "
              + "without the intention of providing the ability to mutate the "
              + "caller's parameter variables. By making a fake arguments "
              + "object with no \"callee\" property, we provide the least "
              + "authority assumed by this typical use.\n"
              + "The fake is made with a \"var a___ = "
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
          name="varThis",
          synopsis="Replace \"this\" with \"dis___\".",
          reason="The rules for binding of \"this\" in "
              + "JavaScript are dangerous.",
          matches="this",
          substitutes="dis___")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        return transform(node, scope);
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
          matches="<approx> var @v__ ...",
          matchNode=Declaration.class,
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


    // This rule is separate from varBadSuffixDeclaration, because
    // although FunctionDeclaration is a subclass of Declaration,
    // it fuzzes as a FunctionConstructor.
    new Rule() {
      @Override
      @RuleDescription(
          name="functionBadSuffixDeclaration",
          synopsis="Statically reject if a variable with `__` suffix is found.",
          reason="Caja reserves the `__` suffix for internal use.",
          matches="<approx> function @v__ ...",
          matchNode=FunctionDeclaration.class,
          substitutes="<reject>")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        if (node instanceof FunctionDeclaration) {
          Identifier name = ((FunctionDeclaration) node).getIdentifier();
          String strName = name.getValue();
          if (strName != null && strName.endsWith("__")) {
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
          name="functionBadSuffixConstructor",
          synopsis="Statically reject if a variable with `__` suffix is found.",
          reason="Caja reserves the `__` suffix for internal use.",
          matches="<approx> function @v__ ...",
          matchNode=FunctionConstructor.class,
          substitutes="<reject>")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        if (node instanceof FunctionConstructor) {
          Identifier name = ((FunctionConstructor) node).getIdentifier();
          String strName = name.getValue();
          if (strName != null && strName.endsWith("__")) {
            mq.addMessage(
                RewriterMessageType.VARIABLES_CANNOT_END_IN_DOUBLE_UNDERSCORE,
                node.getFilePosition(), this, node);
            return node;
          }
        }
        return NONE;
      }
    },

    // TODO(metaweta): Use fastpath
    new Rule() {
      @Override
      @RuleDescription(
          name="varGlobal",
          synopsis="Global vars are rewritten to be properties of IMPORTS___.",
          reason="",
          matches="@v",
          matchNode=Reference.class,
          substitutes="IMPORTS___.@fp ?" +
              "IMPORTS___.@v :" +
              "___.ri(IMPORTS___, @vname)")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = match(node);
        if (bindings != null) {
          ParseTreeNode v = bindings.get("v");
          if (v instanceof Reference) {
            Reference vRef = (Reference) v;
            if (scope.isOuter(vRef.getIdentifierName())) {
              return substV(
                  "fp", newReference(
                      vRef.getFilePosition(),
                      vRef.getIdentifierName() + "_v___"),
                  "v", noexpand(vRef),
                  "vname", toStringLiteral(v));
            }
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
          matches="@v",
          matchNode=Reference.class,
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
          name="getLength",
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
          name="get",
          synopsis="",
          reason="",
          matches="@o.@p",
          substitutes="@oRef.@fp ? @oRef.@p : @oRef.v___('@p')")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = match(node);
        if (bindings != null) {
          Reference p = (Reference) bindings.get("p");
          String propertyName = p.getIdentifierName();
          Reusable ru = new Reusable(scope, bindings.get("o"));
          ru.generate();
          return commas(ru.init(), (Expression) substV(
              "oRef", ru.ref(0),
              "p",    noexpand(p),
              "fp",   newReference(p.getFilePosition(),
                                   propertyName + "_v___"),
              "rp",   toStringLiteral(p)));
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="readNum",
          synopsis="Recognize that array indexing is inherently safe.",
          reason="When the developer knows that their index expression is" +
              " an array index, they can indicate this with the" +
              " 'known-numeric operator'. Since these" +
              " properties are necessarily readable, we can pass them " +
              " through directly to JavaScript. We don't support Firefox 2," +
              " which exposes authority on negative indices of some objects.",
          matches="@o[+@s]",
          substitutes="@o[+@s]")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        return transform(node, scope);
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="readNumWithConstantIndex",
          synopsis="Recognize that array indexing is inherently safe.",
          reason="Numeric properties are always readable;" +
              " we can pass these through directly to JavaScript." +
              " We don't support Firefox 2 or 3," +
              " which expose authority on negative indices of some objects.",
          matches="@o[@numLiteral]",
          substitutes="@o[@numLiteral]")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = match(node);
        if (bindings != null) {
          ParseTreeNode index = bindings.get("numLiteral");
          if (index instanceof NumberLiteral) {
            return substV(
                "o", expand(bindings.get("o"), scope),
                "numLiteral", expand(index, scope));
          }
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="readIndex",
          synopsis="",
          reason="",
          matches="@o[@s]",
          substitutes="@o.v___(@s)")
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
          name="setBadVariable",
          synopsis="Statically reject if an expression assigns to an "
              + "unmaskable variable.",
          reason="arguments and eval are not allowed to be written to.",
          matches="@import = @y",
          substitutes="<reject>")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = match(node);
        if (bindings != null && bindings.get("import") instanceof Reference) {
          String name =
              ((Reference) bindings.get("import")).getIdentifierName();
          if (Scope.UNMASKABLE_IDENTIFIERS.contains(name)) {
            mq.addMessage(
                RewriterMessageType.CANNOT_ASSIGN_TO_IDENTIFIER,
                node.getFilePosition(), MessagePart.Factory.valueOf(name));
            return node;
          }
        }
        return NONE;
      }
    },

    // TODO (metaweta): Use fastpath.
    new Rule() {
      @Override
      @RuleDescription(
          name="initGlobalVar",
          synopsis="",
          reason="",
          matches="/* in outer scope */ var @v = @r",
          substitutes="IMPORTS___.w___('@v', @r)")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = this.match(node);
        if (bindings != null) {
          Identifier v = (Identifier) bindings.get("v");
          String vname = v.getName();
          if (scope.isOuter(vname)) {
            ParseTreeNode r = bindings.get("r");
            mq.addMessage(
                RewriterMessageType.TOP_LEVEL_VAR_INCOMPATIBLE_WITH_CAJA,
                node.getFilePosition(),
                MessagePart.Factory.valueOf(
                    render(QuasiBuilder.substV("window['@v']", "v", v))
                    + " = ..."));
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
          name="setBadGlobalVar",
          synopsis="Statically reject if a global with `__` suffix is found.",
          reason="Caja reserves the `__` suffix for internal use.",
          matches="/* declared in outer scope */ @v__ = @r",
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

    // TODO (metaweta): Use fastpath.
    new Rule() {
      @Override
      @RuleDescription(
          name="setGlobalVar",
          synopsis="",
          reason="",
          matches="/* declared in outer scope */ @v = @r",
          substitutes="IMPORTS___.@fp ?" +
              "IMPORTS___.@v = @r :" +
              "___.wi(IMPORTS___, '@v', @r)")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = this.match(node);
        if (bindings != null) {
          ParseTreeNode v = bindings.get("v");
          if (v instanceof Reference) {
            Reference vRef = (Reference) v;
            String vname = vRef.getIdentifierName();
            if (scope.isOuter(vname)) {
              ParseTreeNode r = bindings.get("r");
              return substV(
                  "v", noexpand(vRef),
                  "fp", newReference(
                      vRef.getFilePosition(),
                      vRef.getIdentifierName() + "_w___"),
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
          substitutes="___.di(IMPORTS___, '@v')")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = this.match(node);
        if (bindings != null &&
            bindings.get("v") instanceof Identifier &&
            scope.isOuter(((Identifier) bindings.get("v")).getName())) {
          mq.addMessage(
              RewriterMessageType.TOP_LEVEL_VAR_INCOMPATIBLE_WITH_CAJA,
              node.getFilePosition(),
              MessagePart.Factory.valueOf(
                  render(QuasiBuilder.substV("window['@v'] = undefined",
                      "v", bindings.get("v")))));
          ExpressionStmt es = newExprStmt(
              (Expression) substV("v", bindings.get("v")));
          markTreeForSideEffect(es);
          return es;
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
          name="set",
          synopsis="Set a property.",
          reason="",
          matches="@o.@p = @r",
          substitutes="<approx> @o.w___(@'p', @r);")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = match(node);
        if (bindings != null) {
          Reference p = (Reference) bindings.get("p");
          String propertyName = p.getIdentifierName();
          ParseTreeNode r = nymize(bindings.get("r"), propertyName, "meth");
          Reusable ru = new Reusable(scope, bindings.get("o"), r);
          ru.generate();
          return commas(ru.init(), (Expression) QuasiBuilder.substV(
              "@oRef.@pWritable === @oRef ? (@oRef.@p = @rRef) : " +
              "                           @oRef.w___(@pName, @rRef);",
              "oRef", ru.ref(0),
              "rRef", ru.ref(1),
              "pWritable", newReference(UNK, propertyName + "_w___"),
              "p", noexpand(p),
              "pName", toStringLiteral(p)));
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="setNumericIndex",
          synopsis="Set a property marked as numeric.",
          reason="",
          matches="@o[+@p] = @r",
          substitutes="<approx> @o.w___(+@p, @r);")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = match(node);
        if (bindings != null) {
          Expression p = (Expression) bindings.get("p");
          ParseTreeNode r = nymize(bindings.get("r"), "", "meth");
          Reusable ru = new Reusable(scope, bindings.get("o"), r);
          ru.generate();
          return commas(ru.init(), (Expression) QuasiBuilder.substV(
              "@oRef.NUM____w___ === @oRef ? (@oRef[+@p] = @rRef) : " +
              "                           @oRef.w___(+@p, @rRef);",
              "oRef", ru.ref(0),
              "rRef", ru.ref(1),
              "p", expand(p, scope)));
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="setNumericLiteralIndex",
          synopsis="Set a numeric literal property.",
          reason="",
          matches="@o[@numLiteral] = @r",
          substitutes="<approx> @o.w___(@numLiteral, @r);")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = match(node);
        if (bindings != null) {
          ParseTreeNode index = bindings.get("numLiteral");
          if (index instanceof NumberLiteral) {
            ParseTreeNode r = nymize(
                bindings.get("r"), index.toString(), "meth");
            Reusable ru = new Reusable(scope, bindings.get("o"), r);
            ru.generate();
            return commas(ru.init(), (Expression) QuasiBuilder.substV(
                "(@oRef.NUM____w___ === @oRef) ? " +
                "    (@oRef[@numLiteral] = @rRef) : " +
                "    @oRef.w___(@numLiteral, @rRef);",
                "oRef", ru.ref(0),
                "rRef", ru.ref(1),
                "numLiteral", noexpand((NumberLiteral)index)));
          }
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="setIndex",
          synopsis="",
          reason="",
          matches="@o[@s] = @r",
          substitutes="@o.w___(@s, @r)")
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
          reason="vars and functions have different scoping.",
          matches="var @v = @r",
          substitutes="@v = @r")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = match(node);
        if (bindings != null) {
          Identifier v = (Identifier) bindings.get("v");
          if (!scope.isFunction(v.getName())) {
            ParseTreeNode r = bindings.get("r");
            scope.addStartOfScopeStatement(
                new Declaration(v.getFilePosition(), v, null));
            ExpressionStmt init = new ExpressionStmt(
                node.getFilePosition(), (Expression) substV(
                    "v", new Reference(noexpand(v)),
                    "r", expand(nymize(r, v.getName(), "var"), scope)));
            // The result of the initializer of a declaration is not relevant to
            // the module result.
            markTreeForSideEffect(init);
            return init;
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
          reason="vars and functions have different scoping.",
          matches="var @v",
          substitutes="var @v")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = match(node);
        if (bindings != null) {
          Identifier id = (Identifier) bindings.get("v");
          if (!scope.isFunction(id.getName())) {
            scope.addStartOfScopeStatement((Declaration) substV(
                "v", noexpand((Identifier) bindings.get("v"))));
            return new Noop(node.getFilePosition());
          }
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
          synopsis="Plain old assignment.",
          reason="",
          matches="@v = @r",
          substitutes="@v = @r")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = match(node);
        if (bindings != null) {
          ParseTreeNode v = bindings.get("v");
          if (v instanceof Reference) {
            String vname = getReferenceName(v);
            ParseTreeNode r = bindings.get("r");
            return substV(
                "v", noexpand((Reference) v),
                "r", expand(nymize(r, vname, "var"), scope));
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
          matches="<approx> @x @op= @y",
          matchNode=AssignOperation.class,
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
          matches="<approx> ++@x but any {pre,post}{in,de}crement will do",
          matchNode=AssignOperation.class,
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
              Reference tmpVal = scope.declareStartOfScopeTemp();
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
          substitutes="new @ctor.new___()")
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
          substitutes="new @ctor.new___(@as*)")
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
          name="delete",
          synopsis="",
          reason="",
          matches="delete @o.@p",
          substitutes="@o.c___(@'p')")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = match(node);
        if (bindings != null) {
          Reference p = (Reference) bindings.get("p");
          return QuasiBuilder.substV(
              "@o.c___(@pname)",
              "o", expand(bindings.get("o"), scope),
              "pname", toStringLiteral(p));
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="deleteIndex",
          synopsis="",
          reason="",
          matches="delete @o[@s]",
          substitutes="@o.c___(@s)")
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
          name="callMethod",
          synopsis="",
          reason="",
          matches="@o.@m(@as*)",
          substitutes="<approx> @o.m___(@'m', [@as*])")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = match(node);
        if (bindings != null) {
          Reference m = (Reference) bindings.get("m");
          String methodName = m.getIdentifierName();
          Reusable ru = new Reusable(scope, bindings.get("o"));
          ru.addChildren(bindings.get("as"));
          ru.generate();
          return commas(ru.init(), (Expression) QuasiBuilder.substV(
              "@oRef.@fm ? @oRef.@m(@argRefs*) : @oRef.m___(@rm, [@argRefs*]);",
              "oRef",    ru.ref(0),
              "argRefs", ru.refListFrom(1),
              "m",       noexpand(m),
              "fm",      newReference(UNK, methodName + "_m___"),
              "rm",      toStringLiteral(m)));
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="callIndexedMethod",
          synopsis="",
          reason="",
          matches="@o[@s](@as*)",
          substitutes="@o.m___(@s, [@as*])")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        return transform(node, scope);
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="callFunc",
          synopsis="",
          reason="",
          matches="@f(@as*)",
          substitutes="@f.i___(@as*)")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        return transform(node, scope);
      }
    },

    ////////////////////////////////////////////////////////////////////////
    // function - function definitions
    ////////////////////////////////////////////////////////////////////////

    // TODO(metaweta): Do a lighter-weight wrapping when the function
    // does not use {@code this}.

    new Rule() {
      @Override
      @RuleDescription(
          name="funcAnonSimple",
          synopsis="",
          reason="",
          matches="function (@ps*) { @bs*; }",
          substitutes=""
              + "___.f(function (@ps*) {\n"
              + "  @fh*;\n"
              + "  @stmts*;\n"
              + "  @bs*;\n"
              + "})")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = match(node);
        // Anonymous simple function constructor
        if (bindings != null) {
          Scope s2 = Scope.fromFunctionConstructor(
              scope,
              (FunctionConstructor) node);
          ParseTreeNodeContainer ps =
              (ParseTreeNodeContainer) bindings.get("ps");
          checkFormals(ps);
          return substV(
              "ps", noexpandParams(ps),
              // It's important to expand bs before computing fh and stmts.
              "bs", withoutNoops(expand(bindings.get("bs"), s2)),
              "fh", getFunctionHeadDeclarations(s2),
              "stmts", new ParseTreeNodeContainer(s2.getStartStatements()));
        }
        return NONE;
      }
    },

    // References to {@code fname} in the body should refer to the result of
    // {@code wrap}.
    //
    // While under FF3.6/Firebug 1.5 there don't seem to be any issues,
    // this translation has triggered the "cannot access optimized closure"
    // bug on other version combinations.
    // https://bugzilla.mozilla.org/show_bug.cgi?id=505001
    // Using a Y combinator can fix that, but is harder to understand.
    // TODO(metaweta): Only use closure if it's really recursive.
    new Rule() {
      @Override
      @RuleDescription(
          name="funcNamedDecl",
          synopsis="",
          reason="",
          matches="function @fname(@ps*) { @bs*; }",
          substitutes=""
            + "function @fname(@ps*) {\n"
            + "  @fh*;\n"
            + "  @stmts*;\n"
            + "  @bs*;\n"
            + "}\n"
            + "___.f(@fRef, '@fname');")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        if (node instanceof FunctionDeclaration && !scope.isOuter()) {
          Map<String, ParseTreeNode> bindings = match(
              ((FunctionDeclaration) node).getInitializer());
          // Named simple function declaration
          if (bindings != null) {
            Scope s2 = Scope.fromFunctionConstructor(
                scope, ((FunctionDeclaration) node).getInitializer());
            ParseTreeNodeContainer ps =
                (ParseTreeNodeContainer) bindings.get("ps");
            checkFormals(ps);
            Identifier fname = noexpand((Identifier) bindings.get("fname"));
            scope.declareStartOfScopeVariable(fname);
            Statement stmt = (Statement) substV(
                "fname", fname,
                "fRef", new Reference(fname),
                "ps", noexpandParams(ps),
                // It's important to expand bs before computing fh and stmts.
                "bs", withoutNoops(expand(bindings.get("bs"), s2)),
                "fh", getFunctionHeadDeclarations(s2),
                "stmts", new ParseTreeNodeContainer(s2.getStartStatements()));
            scope.addStartStatement(stmt);
            return new Noop(FilePosition.UNKNOWN);
          }
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="funcNamedTopDecl",
          synopsis="",
          reason="",
          matches="function @fname(@ps*) { @bs*; }",
          substitutes=""
            + "function @fname(@ps*) {\n"
            + "  @fh*;\n"
            + "  @stmts*;\n"
            + "  @bs*;\n"
            + "}\n"
            + "IMPORTS___.w___('@fname', ___.f(@fRef, '@fname'));")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        if (node instanceof FunctionDeclaration && scope.isOuter()) {
          Map<String, ParseTreeNode> bindings = match(
              ((FunctionDeclaration) node).getInitializer());
          // Named simple function declaration
          if (bindings != null) {
            Scope s2 = Scope.fromFunctionConstructor(
                scope, ((FunctionDeclaration) node).getInitializer());
            ParseTreeNodeContainer ps =
                (ParseTreeNodeContainer) bindings.get("ps");
            checkFormals(ps);
            Identifier fname = noexpand((Identifier) bindings.get("fname"));
            mq.addMessage(
                RewriterMessageType.TOP_LEVEL_FUNC_INCOMPATIBLE_WITH_CAJA,
                node.getFilePosition(),
                MessagePart.Factory.valueOf(
                    render(QuasiBuilder.substV(
                        "window['@fname'] = function (@ps*) { /* ... */ }",
                        "fname", fname,
                        "ps", bindings.get("ps")))));
            Statement stmt = (Statement) substV(
                "fname", fname,
                "fRef", new Reference(fname),
                "ps", noexpandParams(ps),
                // It's important to expand bs before computing fh and stmts.
                "bs", withoutNoops(expand(bindings.get("bs"), s2)),
                "fh", getFunctionHeadDeclarations(s2),
                "stmts", new ParseTreeNodeContainer(s2.getStartStatements()));
            scope.addStartStatement(stmt);
            return new Noop(FilePosition.UNKNOWN);
          }
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="funcNamedValue",
          synopsis="",
          reason="",
          matches="function @fname(@ps*) { @bs*; }",
          substitutes=""
              + "(function () {\n"
              + "  function @fname(@ps*) {\n"
              + "    @fh*;\n"
              + "    @stmts*;\n"
              + "    @bs*;\n"
              + "  }\n"
              + "  return ___.f(@fRef, '@fname');"
              + "})()")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = match(node);
        // Named simple function expression
        if (bindings != null) {
          Scope s2 = Scope.fromFunctionConstructor(
              scope, (FunctionConstructor) node);
          ParseTreeNodeContainer ps =
              (ParseTreeNodeContainer) bindings.get("ps");
          checkFormals(ps);
          Identifier fname = noexpand((Identifier) bindings.get("fname"));
          return substV(
              "fname", fname,
              "fRef", new Reference(fname),
              "ps", noexpandParams(ps),
              // It's important to expand bs before computing fh and stmts.
              "bs", withoutNoops(expand(bindings.get("bs"), s2)),
              "fh", getFunctionHeadDeclarations(s2),
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
          synopsis="Consider declarations separately from initializers",
          reason="",
          matches="var @a=@b?, @c=@d*",
          substitutes="{ @decl; @init; }")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        if (node instanceof MultiDeclaration) {
          Expression initializer = null;
          for (Declaration d : ((MultiDeclaration) node).children()) {
            Statement s = (Statement) expand(d, scope);
            if (s instanceof Noop) { continue; }
            if (s instanceof ExpressionStmt) {
              Expression init = ((ExpressionStmt) s).getExpression();
              initializer = initializer == null
                  ? init
                  : Operation.createInfix(Operator.COMMA, initializer, init);
            } else {
              requireErrors(mq, s);
              return node;
            }
          }
          if (initializer == null) {
            return new Noop(node.getFilePosition());
          } else {
            ExpressionStmt es = new ExpressionStmt(
                node.getFilePosition(), initializer);
            // The value of a declaration is not the value of the initializer.
            markTreeForSideEffect(es);
            return es;
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
          name="mapBadKeySuffix",
          synopsis="Statically reject a property whose name ends with `__`",
          reason="",
          matches="<approx> \"@k__\": @v",
          matchNode=ObjProperty.class,
          substitutes="<reject>")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        if (node instanceof ObjProperty) {
          ObjProperty prop = (ObjProperty) node;
          StringLiteral key = prop.getPropertyNameNode();
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
          name="objectProperty",
          synopsis="nymize object properties",
          reason="",
          matches="<approx> \"@k\": @v",
          matchNode=ObjProperty.class,
          substitutes="<nymized>")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        if (node instanceof ObjProperty) {
          if (node instanceof ValueProperty) {
            ValueProperty prop = (ValueProperty) node;
            return new ParseTreeNodeContainer(Arrays.asList(
                 noexpand(prop.getPropertyNameNode()),
                 expand(
                     nymize(prop.getValueExpr(), prop.getPropertyName(), "lit"),
                     scope)));
          } else {
            StringLiteral k = (StringLiteral) node.children().get(0);
            String kType = (node instanceof GetterProperty ? "get" : "set");
            String kName = k.getUnquotedValue() +
                (node instanceof GetterProperty ? "_g___" : "_s___");
            Expression v = (Expression) node.children().get(1);
            return new ParseTreeNodeContainer(Arrays.asList(
                QuasiBuilder.substV("[@k, '" + kType + "']", "k", noexpand(k)),
                expand(nymize(v, kName, "lit"), scope)));
          }
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="map",
          synopsis="Turns an object literal into an explicit initialization.",
          reason="",
          matches="({@key*: @val*})",
          substitutes="___.iM([@parts*])")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        if (node instanceof ObjectConstructor) {
          ObjectConstructor obj = (ObjectConstructor) node;
          List<? extends ObjProperty> props = obj.children();
          List<Expression> expanded = Lists.newArrayList();
          for (ObjProperty prop : props) {
            ParseTreeNode nymized = expand(prop, scope);
            if (nymized instanceof ParseTreeNodeContainer) {
              // Non error property handling case above.
              for (ParseTreeNode child : nymized.children()) {
                expanded.add((Expression) child);
              }
            }
          }
          return substV("parts", new ParseTreeNodeContainer(expanded));
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
          name="typeofGlobal",
          synopsis="Don't throw a ReferenceError",
          reason="",
          matches="typeof @v",
          substitutes="typeof @v")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = match(node);
        if (bindings != null) {
          ParseTreeNode v = bindings.get("v");
          if (v instanceof Reference) {
            Reference vRef = (Reference) v;
            if (scope.isOuter(vRef.getIdentifierName())) {
              return QuasiBuilder.substV(
                  "typeof IMPORTS___.v___(@vname)",
                  "vname", toStringLiteral(v));
            }
          }
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="typeof",
          synopsis="Typeof translates simply",
          reason="",
          matches="typeof @v",
          substitutes="typeof @v")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        return transform(node, scope);
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="in",
          synopsis="Is a property present on the object?",
          reason="",
          matches="@i in @o",
          substitutes="___.i('' + @i, @o)")
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
          synopsis="Use the regular expression constructor",
          reason="So that every use of a regex literal creates a new instance"
              + " to prevent state from leaking via interned literals. This"
              + " is consistent with the way ES4 treates regex literals.",
          matchNode=RegexpLiteral.class,
          substitutes="new RegExp.new___(@pattern, @modifiers?)")
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

    // TODO: use a whitelist, deactivate the rest by replacing with
    // {@code '' + @directive}
    new Rule() {
      @Override
      @RuleDescription(
          name="useSubsetDirective",
          synopsis="replace use subset directives with noops",
          reason="rewriting changes the block structure of the input, which"
              + " could lead to a directive appearing in an illegal position"
              + " since directives must appear at the beginning of a program"
              + " or function body, not in an arbitrary block",
          matches="<approx> 'use';",
          matchNode=DirectivePrologue.class,
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
            node instanceof Elision ||
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

  public ES53Rewriter(
      URI baseUri, ModuleManager moduleManager, boolean logging) {
    super(null == moduleManager ? null : moduleManager.getMessageQueue(),
        true, logging);
    this.buildInfo =
      null == moduleManager ? null : moduleManager.getBuildInfo();
    initRules();
  }

  public ES53Rewriter(BuildInfo buildInfo, MessageQueue mq, boolean logging) {
    super(mq, true, logging);
    this.buildInfo = buildInfo;
    initRules();
  }

  private void initRules() {
    addRules(SyntheticRuleSet.syntheticRules(this));
    addRules(cajaRules);
  }

  private static void requireErrors(MessageQueue mq, ParseTreeNode n) {
    // Make sure a sub-rule has put an error because the rule got an unexpected
    // result from a recursive call to expand.
    if (!mq.hasMessageAtLevel(MessageLevel.ERROR)) {
      mq.addMessage(
          RewriterMessageType.BAD_RESULT_FROM_RECURSIVE_CALL,
          n.getFilePosition(), n);
    }
  }
}
