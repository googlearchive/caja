// Copyright (C) 2013 Google Inc.
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

package com.google.caja.tracing;

import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.TokenConsumer;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.ParseTreeNodeContainer;
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.Expression;
import com.google.caja.parser.js.FormalParam;
import com.google.caja.parser.js.FunctionConstructor;
import com.google.caja.parser.js.FunctionDeclaration;
import com.google.caja.parser.js.Identifier;
import com.google.caja.parser.js.Noop;
import com.google.caja.parser.js.PlainModule;
import com.google.caja.parser.js.Reference;
import com.google.caja.parser.js.StringLiteral;
import com.google.caja.parser.js.UncajoledModule;
import com.google.caja.parser.quasiliteral.Rewriter;
import com.google.caja.parser.quasiliteral.RewriterMessageType;
import com.google.caja.parser.quasiliteral.Rule;
import com.google.caja.parser.quasiliteral.RuleDescription;
import com.google.caja.parser.quasiliteral.Scope;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.RenderContext;
import com.google.caja.util.Callback;
import com.google.common.collect.Lists;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Rewriter for adding tracing information to arbitrary JavaScript files. Not
 * part of the Caja cajoling pipeline.
 *
 * @author ihab.awad@gmail.com
 */
public class TracingRewriter extends Rewriter {

  private static StringLiteral nodeEqualsTo(ParseTreeNode n) {
    StringBuilder sb = new StringBuilder();
    TokenConsumer renderer = n.makeRenderer(sb, new Callback<IOException> () {
      @Override public void handle(IOException e) {
        throw new RuntimeException(e);
      }
    });
    n.render(new RenderContext(renderer));
    renderer.noMoreTokens();
    return new StringLiteral(
        FilePosition.UNKNOWN,
        sb.toString() + " = ");
  }

  private static Identifier paramName(int i) {
    return new Identifier(
        FilePosition.UNKNOWN,
        PARAM_PREFIX + i);
  }

  private static ParseTreeNodeContainer formals(int howMany) {
    List<ParseTreeNode> l = Lists.newArrayList();
    for (int i = 0; i < howMany; i++) {
      l.add(new FormalParam(paramName(i)));
    }
    return new ParseTreeNodeContainer(l);
  }

  private static ParseTreeNodeContainer actuals(int howMany) {
    List<ParseTreeNode> l = Lists.newArrayList();
    for (int i = 0; i < howMany; i++) {
      l.add(new Reference(paramName(i)));
    }
    return new ParseTreeNodeContainer(l);
  }

  private static StringLiteral source(ParseTreeNode node) {
    return new StringLiteral(
        FilePosition.UNKNOWN,
        node.getFilePosition().source().toString());
  }

  private static StringLiteral pos(FilePosition pos) {
    return new StringLiteral(FilePosition.UNKNOWN, pos.toString());
  }

  private static StringLiteral pos(ParseTreeNode n) {
    return pos(n.getFilePosition());
  }

  private static StringLiteral startPos(ParseTreeNode n) {
    return pos(FilePosition.startOf(n.getFilePosition()));
  }

  private static StringLiteral endPos(ParseTreeNode n) {
    return pos(FilePosition.endOf(n.getFilePosition()));
  }

  private static StringLiteral funcStartPos(FunctionConstructor fc) {
    return startPos(fc.getBody());
  }

  private static StringLiteral funcEndPos(FunctionConstructor fc) {
    return endPos(fc.getBody());
  }

  private static StringLiteral funcCallsiteName(Expression expr) {
    return new StringLiteral(
        FilePosition.UNKNOWN,
        expr instanceof Reference
            ? ((Reference) expr).getIdentifierName()
            : "");
  }

  private static StringLiteral funcName(FunctionConstructor fc) {
    return new StringLiteral(
        FilePosition.UNKNOWN,
        fc.getIdentifierName() != null
            ? fc.getIdentifierName()
            : "");
  }

  private static StringLiteral identifierName(Reference r) {
    return new StringLiteral(
        FilePosition.UNKNOWN,
        r.getIdentifierName());
  }

  private static Reference ref(String name) {
    return new Reference(
        new Identifier(
            FilePosition.UNKNOWN,
            name));
  }

  private static final String TRACING = "TRACING";
  private static final String PARAM_PREFIX = "p";
  private static final Reference PUSH_FRAME = ref("pushFrame");
  private static final Reference POP_FRAME = ref("popFrame");
  private static final Reference PUSH_CALLSITE = ref("pushCallsite");
  private static final Reference POP_CALLSITE = ref("popCallsite");
  private static final Reference LOG = ref("log");

  private final Rule[] rules = {

    ////////////////////////////////////////////////////////////////////////////
    // Structural rearrangement rules

    new Rule() {
      @Override
      @RuleDescription(
          name="moduleEnvelope",
          synopsis="Process an UncajoledModule into a PlainModule.",
          reason="",
          matches="<an UncajoledModule>",
          matchNode=UncajoledModule.class,
          substitutes="")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        if (node instanceof UncajoledModule) {
          return new PlainModule((Block) expand(node.children().get(0), scope));
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="module",
          synopsis="Top-level block expansion.",
          reason="",
          matches="{ @statements*; }",
          substitutes=""
            + "@funcDecls*;"
            + "TRACING.@pushFrame(@startPos, @nameString);"
            + "try {"
            + "  @expanded*;"
            + "} finally {"
            + "  TRACING.@popFrame(@endPos);"
            + "}")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        if (node instanceof Block && scope == null) {
          Scope s2 = Scope.fromProgram((Block) node, TracingRewriter.this);
          List<ParseTreeNode> expanded = Lists.newArrayList();
          for (ParseTreeNode c : node.children()) {
            ParseTreeNode expandedC = expand(c, s2);
            if (expandedC instanceof Noop) { continue; }
            expanded.add(expandedC);
          }
          return substV(
              "funcDecls", new ParseTreeNodeContainer(s2.getStartStatements()),
              "expanded", new ParseTreeNodeContainer(expanded),
              "startPos", startPos(node),
              "endPos", endPos(node),
              "nameString", source(node),
              "pushFrame", PUSH_FRAME,
              "popFrame", POP_FRAME);
        }
        return NONE;
      }
    },

    ////////////////////////////////////////////////////////////////////////////
    // Function body rewriting

    new Rule() {
      @Override
      @RuleDescription(
          name="funcNamedDecl",
          synopsis="",
          reason="",
          matches="function @fname(@ps*) { @bs*; }",
          substitutes="")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        if (node instanceof FunctionDeclaration) {
          FunctionConstructor initializer =
              ((FunctionDeclaration) node).getInitializer();
          Map<String, ParseTreeNode> bindings = match(initializer);
          if (bindings != null) {
            scope.addStartStatement(
                new FunctionDeclaration(
                    (FunctionConstructor) expand(
                        initializer,
                        scope)));
            return new Noop(FilePosition.UNKNOWN);
          }
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="functionCtor",
          synopsis="",
          reason="",
          matches="function @fname?(@ps*) { @bs*; }",
          substitutes=""
              + "function @fname?(@ps*) {"
              + "  @funcDecls*;"
              + "  TRACING.@pushFrame(@startPos, @nameString);"
              + "  try {"
              + "    @bs*;"
              + "  } finally {"
              + "    TRACING.@popFrame(@endPos);"
              + "  }"
              + "}")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = match(node);
        if (bindings != null) {
          Scope s2 = Scope.fromFunctionConstructor(
              scope,
              (FunctionConstructor) node);
          ParseTreeNodeContainer bodyStatements = (ParseTreeNodeContainer)
              expandAll(bindings.get("bs"), s2);
          return substV(
              "fname", bindings.get("fname"),
              "funcDecls", new ParseTreeNodeContainer(s2.getStartStatements()),
              "ps", expandAll(bindings.get("ps"), scope),
              "bs", bodyStatements,
              "nameString", funcName((FunctionConstructor) node),
              "startPos", funcStartPos((FunctionConstructor) node),
              "endPos", funcEndPos((FunctionConstructor) node),
              "pushFrame", PUSH_FRAME,
              "popFrame", POP_FRAME);
        }
        return NONE;
      }
    },

    ////////////////////////////////////////////////////////////////////////////
    // Function callsite rewriting

    new Rule() {
      @Override
      @RuleDescription(
          name="tracingLog",
          synopsis="Rewrite calls to log an expression",
          reason="",
          matches="TRACING.log(@expr)",
          substitutes="TRACING.@log(@pos, @lit + @expr)")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = match(node);
        if (bindings != null) {
          return substV(
              "pos", pos(node),
              "lit", nodeEqualsTo(bindings.get("expr")),
              "expr", expand(bindings.get("expr"), scope),
              "log", LOG);
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="tracingOther",
          synopsis="Do not expand TRACING calls",
          reason="",
          matches="TRACING.@m(@args*)",
          substitutes="TRACING.@m(@args*)")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = match(node);
        if (bindings != null) {
          return substV(
              "m", bindings.get("m"),
              "args", expandAll(bindings.get("args"), scope));
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="newExpr",
          synopsis="Rewrite constructor invocations",
          reason="",
          matches="new @ctor(@args*)",
          substitutes=""
              + "(function(ctor, @formals*) {"
              + "  TRACING.@pushCallsite('construct', @pos, @name);"
              + "  try {"
              + "    return TRACING.@popCallsite("
              + "        'return',"
              + "        new ctor(@actuals*));"
              + "  } catch (e) {"
              + "    TRACING.@popCallsite('exception');"
              + "    throw e;"
              + "  }"
              + "})(@ctor, @args*)")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = match(node);
        if (bindings != null) {
          return substV(
              "pos", pos(bindings.get("ctor")),
              "ctor", expand(bindings.get("ctor"), scope),
              "name", funcCallsiteName((Expression) bindings.get("ctor")),
              "args", expandAll(bindings.get("args"), scope),
              "formals", formals(bindings.get("args").children().size()),
              "actuals", actuals(bindings.get("args").children().size()),
              "pushCallsite", PUSH_CALLSITE,
              "popCallsite", POP_CALLSITE);
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="invokeMethodBracket",
          synopsis="Rewrite method callsites",
          reason="",
          matches="@lhs[@m](@args*)",
          substitutes=""
              + "(function(lhs, m, @formals*) {"
              + "  TRACING.@pushCallsite('method', @pos, @name);"
              + "  try {"
              + "    return TRACING.@popCallsite('return', lhs[m](@actuals*));"
              + "  } catch (e) {"
              + "    TRACING.@popCallsite('exception');"
              + "    throw e;"
              + "  }"
              + "})(@lhs, @m, @args*)")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = match(node);
        if (bindings != null) {
          ParseTreeNode m = bindings.get("m");
          ParseTreeNode name = (m instanceof StringLiteral)
              ? m : new StringLiteral(FilePosition.UNKNOWN, "");
          return substV(
              "name", name,
              "pos", pos(m),
              "lhs", expand(bindings.get("lhs"), scope),
              "m", expand(bindings.get("m"), scope),
              "args", expandAll(bindings.get("args"), scope),
              "formals", formals(bindings.get("args").children().size()),
              "actuals", actuals(bindings.get("args").children().size()),
              "pushCallsite", PUSH_CALLSITE,
              "popCallsite", POP_CALLSITE);
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="invokeMethodDot",
          synopsis="Rewrite method callsites",
          reason="",
          matches="@lhs.@m(@args*)",
          substitutes=""
              + "(function(lhs, m, @formals*) {"
              + "  TRACING.@pushCallsite('method', @pos, @name);"
              + "  try {"
              + "    return TRACING.@popCallsite('return', lhs[m](@actuals*));"
              + "  } catch (e) {"
              + "    TRACING.@popCallsite('exception');"
              + "    throw e;"
              + "  }"
              + "})(@lhs, @m, @args*)")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = match(node);
        if (bindings != null) {
          return substV(
              "name", identifierName((Reference) bindings.get("m")),
              "pos", pos(bindings.get("m")),
              "lhs", expand(bindings.get("lhs"), scope),
              "m", toStringLiteral(bindings.get("m")),
              "args", expandAll(bindings.get("args"), scope),
              "formals", formals(bindings.get("args").children().size()),
              "actuals", actuals(bindings.get("args").children().size()),
              "pushCallsite", PUSH_CALLSITE,
              "popCallsite", POP_CALLSITE);
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="callFunction",
          synopsis="Rewrite function callsites",
          reason="",
          matches="@f(@args*)",
          substitutes=""
              + "(function(f, @formals*) {"
              + "  TRACING.@pushCallsite('function', @pos, @name);"
              + "  try {"
              + "    return TRACING.@popCallsite('return', f(@actuals*));"
              + "  } catch (e) {"
              + "    TRACING.@popCallsite('exception');"
              + "    throw e;"
              + "  }"
              + "})(@f, @args*)")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = match(node);
        if (bindings != null) {
          return substV(
              "pos", pos(bindings.get("f")),
              "f", expand(bindings.get("f"), scope),
              "name", funcCallsiteName((Expression) bindings.get("f")),
              "args", expandAll(bindings.get("args"), scope),
              "formals", formals(bindings.get("args").children().size()),
              "actuals", actuals(bindings.get("args").children().size()),
              "pushCallsite", PUSH_CALLSITE,
              "popCallsite", POP_CALLSITE);
        }
        return NONE;
      }
    },

    ////////////////////////////////////////////////////////////////////////////
    // Generic handling of otherwise unmatched nodes

    new Rule() {
      @Override
      @RuleDescription(
          name="doNotShadowTracing",
          synopsis="Cannot shadow the name TRACING in any scope.",
          reason="",
          matches="",
          substitutes="")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        boolean illegal =
            (node instanceof Reference &&
                ((Reference) node).getIdentifierName().equals(TRACING)) ||
            (node instanceof Identifier &&
                ((Identifier) node).getName().equals(TRACING));
        if (illegal) {
          mq.addMessage(
              RewriterMessageType.CANNOT_ASSIGN_TO_IDENTIFIER,
              MessageLevel.FATAL_ERROR,
              node.getFilePosition(), MessagePart.Factory.valueOf(TRACING));
        }
        return NONE;
      }
    },

    new Rule() {
      @Override
      @RuleDescription(
          name="recurse",
          synopsis="Automatically recurse into unmatched",
          reason="",
          matches="<many>")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        return expandAll(node, scope);
      }
    },
  };

  public TracingRewriter(MessageQueue mq) {
    super(mq, false, false);
    addRules(rules);
  }
}