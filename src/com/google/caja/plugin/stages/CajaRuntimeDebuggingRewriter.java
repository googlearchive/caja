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

package com.google.caja.plugin.stages;

import com.google.caja.lexer.FilePosition;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.SyntheticNodes;
import com.google.caja.parser.js.BooleanLiteral;
import com.google.caja.parser.js.IntegerLiteral;
import com.google.caja.parser.quasiliteral.Rewriter;
import com.google.caja.parser.quasiliteral.Rule;
import com.google.caja.parser.quasiliteral.RuleDescription;
import com.google.caja.parser.quasiliteral.Scope;
import com.google.caja.reporting.MessageQueue;

import java.util.Map;


/**
 * Modifies cajoled code to remove fasttrack branches, and add debugging info
 * to the slow branches.
 *
 * @author mikesamuel@gmail.com
 */
final class CajaRuntimeDebuggingRewriter extends Rewriter {
  private final DebuggingSymbols symbols;

  CajaRuntimeDebuggingRewriter(DebuggingSymbols symbols) {
    super(false);
    this.symbols = symbols;
  }

  /**
   * Rewrites a {@code ___} method call to add an extra parameter which is an
   * index into {@link DebuggingSymbols}.
   * The position is drawn from the <code>@posNode</code> binding if one is
   * present, or is otherwise drawn from the match as a whole.
   */
  abstract class AddPositionParamRule extends Rule {
    @Override
    public ParseTreeNode fire(
        ParseTreeNode node, Scope scope, MessageQueue mq) {
      Map<String, ParseTreeNode> bindings = match(node);
      if (bindings != null) {
        ParseTreeNode posNode = bindings.get("posNode");
        if (posNode == null) { posNode = node; }
        FilePosition pos = spanningPos(posNode);
        if (pos == null) { pos = FilePosition.UNKNOWN; }

        int index = symbols.indexForPosition(pos);
        rebind(bindings, scope, mq);
        bindings.put("debug", new IntegerLiteral(index));
        return subst(bindings);
      }
      return NONE;
    }

    protected void rebind(Map<String, ParseTreeNode> bindings,
                          Scope scope, MessageQueue mq) {
      expandEntries(bindings, scope, mq);
    }
  }

  /**
   * Simplifies cajoled code by stripping out the fasttrack check and
   * fasttrack path leaving only the slow, instrumented path.
   */
  abstract class SimplifyingRule extends Rule {
    @Override
    public ParseTreeNode fire(
        ParseTreeNode node, Scope scope, MessageQueue mq) {
      Map<String, ParseTreeNode> bindings = match(node);
      if (bindings != null) {
        expandEntries(bindings, scope, mq);
        return subst(bindings);
      }
      return NONE;
    }
  }

  {
    addRule(new AddPositionParamRule() {
          @Override
          @RuleDescription(
              name="callProp",
              synopsis="adds debug info to ___.callProp calls",
              reason="",
              matches="___.callProp(@obj, @name, @args)",
              substitutes="___.callProp(@obj, @name, @args, @debug)")
          public ParseTreeNode fire(ParseTreeNode n, Scope s, MessageQueue mq) {
            return super.fire(n, s, mq);
          }
        });
    addRule(new AddPositionParamRule() {
          @Override
          @RuleDescription(
              name="callPub",
              synopsis="adds debug info to ___.callPub calls",
              reason="",
              matches="___.callPub(@obj, @name, @args)",
              substitutes="___.callPub(@obj, @name, @args, @debug)")
          public ParseTreeNode fire(ParseTreeNode n, Scope s, MessageQueue mq) {
            return super.fire(n, s, mq);
          }
        });
    addRule(new AddPositionParamRule() {
          @Override
          @RuleDescription(
              name="deleteProp",
              synopsis="adds debug info to ___.deleteProp calls",
              reason="",
              matches="___.deleteProp(@obj, @name)",
              substitutes="___.deleteProp(@obj, @name, @debug)")
          public ParseTreeNode fire(ParseTreeNode n, Scope s, MessageQueue mq) {
            return super.fire(n, s, mq);
          }
        });
    addRule(new AddPositionParamRule() {
          @Override
          @RuleDescription(
              name="deletePub",
              synopsis="adds debug info to ___.deletePub calls",
              reason="",
              matches="___.deletePub(@obj, @name)",
              substitutes="___.deletePub(@obj, @name, @debug)")
          public ParseTreeNode fire(ParseTreeNode n, Scope s, MessageQueue mq) {
            return super.fire(n, s, mq);
          }
        });
    addRule(new AddPositionParamRule() {
          @Override
          @RuleDescription(
              name="readProp",
              synopsis="adds debug info to ___.readProp calls",
              reason="",
              matches="___.readProp(@obj, @name, @opt_shouldThrow?)",
              substitutes="___.readProp(@obj, @name, @opt_shouldThrow, @debug)")
          public ParseTreeNode fire(ParseTreeNode n, Scope s, MessageQueue mq) {
            return super.fire(n, s, mq);
          }
          @Override
          protected void rebind(Map<String, ParseTreeNode> bindings,
                                Scope scope, MessageQueue mq) {
            if (bindings.get("opt_shouldThrow") == null) {
              bindings.put("opt_shouldThrow", new BooleanLiteral(false));
            }
          }
        });
    addRule(new AddPositionParamRule() {
          @Override
          @RuleDescription(
              name="readPub",
              synopsis="adds debug info to ___.readPub calls",
              reason="",
              matches="___.readPub(@obj, @name, @opt_shouldThrow?)",
              substitutes="___.readPub(@obj, @name, @opt_shouldThrow, @debug)")
          public ParseTreeNode fire(ParseTreeNode n, Scope s, MessageQueue mq) {
            return super.fire(n, s, mq);
          }
          @Override
          protected void rebind(Map<String, ParseTreeNode> bindings,
                                Scope scope, MessageQueue mq) {
            if (bindings.get("opt_shouldThrow") == null) {
              bindings.put("opt_shouldThrow", new BooleanLiteral(false));
            }
          }
        });
    addRule(new AddPositionParamRule() {
          @Override
          @RuleDescription(
              name="setProp",
              synopsis="adds debug info to ___.setProp calls",
              reason="",
              matches="___.setProp(@obj, @name, @val)",
              substitutes="___.setProp(@obj, @name, @val, @debug)")
          public ParseTreeNode fire(ParseTreeNode n, Scope s, MessageQueue mq) {
            return super.fire(n, s, mq);
          }
        });
    addRule(new AddPositionParamRule() {
          @Override
          @RuleDescription(
              name="setPub",
              synopsis="adds debug info to ___.setPub calls",
              reason="",
              matches="___.setPub(@obj, @name, @val)",
              substitutes="___.setPub(@obj, @name, @val, @debug)")
          public ParseTreeNode fire(ParseTreeNode n, Scope s, MessageQueue mq) {
            return super.fire(n, s, mq);
          }
        });
    addRule(new AddPositionParamRule() {
          @Override
          @RuleDescription(
              name="asSimpleFunc",
              synopsis="adds debug info to ___.asSimpleFunc calls",
              reason="",
              matches="___.asSimpleFunc(@fun)",
              substitutes="___.asSimpleFunc(@fun, @debug)")
          public ParseTreeNode fire(ParseTreeNode n, Scope s, MessageQueue mq) {
            return super.fire(n, s, mq);
          }
        });
    addRule(new AddPositionParamRule() {
      @Override
      @RuleDescription(
          name="asCtor",
          synopsis="adds debug info to ___.asCtor calls",
          reason="",
          matches="___.asCtor(@fun)",
          substitutes="___.asCtor(@fun, @debug)")
      public ParseTreeNode fire(ParseTreeNode n, Scope s, MessageQueue mq) {
        return super.fire(n, s, mq);
      }
    });
    addRule(new SimplifyingRule() {
          @Override
          @RuleDescription(
              name="simplifyCall",
              synopsis="remove fasttrack so it can't dereference null",
              reason=("fasttrack useless in debug mode but it can still cause"
                      + "errors outside our stack"),
              matches="@obj.@x_canCall___ ? (@obj.@x(@actuals*)) : @operation",
              substitutes="@operation")
          public ParseTreeNode fire(ParseTreeNode n, Scope s, MessageQueue mq) {
            return super.fire(n, s, mq);
          }
        });
    addRule(new SimplifyingRule() {
          @Override
          @RuleDescription(
              name="simplifyRead",
              synopsis="remove fasttrack so it can't dereference null",
              reason=("fasttrack useless in debug mode but it can still cause"
                      + "errors outside our stack"),
              matches="@obj.@key_canRead___ ? (@obj.@key) : @operation",
              substitutes="@operation")
          public ParseTreeNode fire(ParseTreeNode n, Scope s, MessageQueue mq) {
            return super.fire(n, s, mq);
          }
        });
    addRule(new SimplifyingRule() {
          @Override
          @RuleDescription(
              name="simplifySet",
              synopsis="remove fasttrack so it can't dereference null",
              reason=("fasttrack useless in debug mode but it can still cause"
                      + "errors outside our stack"),
              matches="@obj.@key_canSet___ ? (@obj.@key = @y) : @operation",
              substitutes="@operation")
          public ParseTreeNode fire(ParseTreeNode n, Scope s, MessageQueue mq) {
            return super.fire(n, s, mq);
          }
        });
    addRule(new AddPositionParamRule() {
          @Override
          @RuleDescription(
              name="forInLoop",
              synopsis="Add null check to forInLoop",
              reason="to identify the source of null pointer exceptions",
              matches=("{ @tmp___ = @posNode;"
                       + "for (@k___ in @tmp___) @body; }"),
              substitutes=("{ @tmp___ = ___.requireObject(@posNode, @debug);"
                           + "for (@k___ in @tmp___) @body; }"))
          public ParseTreeNode fire(ParseTreeNode n, Scope s, MessageQueue mq) {
            return super.fire(n, s, mq);
          }
        });
    addRule(new AddPositionParamRule() {
          @Override
          @RuleDescription(
              name="inOperator",
              synopsis="Add null check to rhs of 'in' operator",
              reason="to identify the source of null pointer exceptions",
              matches=("(@key in @posNode)"),
              substitutes=("(@key in ___.requireObject(@posNode, @debug))"))
          public ParseTreeNode fire(ParseTreeNode n, Scope s, MessageQueue mq) {
            return super.fire(n, s, mq);
          }
        });
    addRule(new Rule() {
          @Override
          @RuleDescription(
              name="fallback",
              synopsis="handles cases not recognized by ",
              reason="because not everything is a runtime check")
          public ParseTreeNode fire(ParseTreeNode n, Scope s, MessageQueue mq) {
            return expandAll(n, s, mq);
          }
        });
  }

  private FilePosition spanningPos(ParseTreeNode node) {
    FilePosition pos = node.getFilePosition();
    if (!node.getAttributes().is(SyntheticNodes.SYNTHETIC)
        && !FilePosition.UNKNOWN.equals(pos)) {
      return pos;
    }

    FilePosition start = null, end = null;
    for (ParseTreeNode child : node.children()) {
      FilePosition childPos = spanningPos(child);
      if (childPos != null) {
        if (start == null) {
          start = end = childPos;
        } else if (start.source().equals(childPos.source())) {
          if (childPos.startCharInFile() < start.startCharInFile()) {
            start = childPos;
          }
          if (childPos.endCharInFile() > end.endCharInFile()) {
            end = childPos;
          }
        }
      }
    }
    if (start != null) { return FilePosition.span(start, end); }
    return null;
  }
}
