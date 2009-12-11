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
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.TokenConsumer;
import com.google.caja.parser.AbstractParseTreeNode;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.ParseTreeNodeContainer;
import com.google.caja.parser.js.BreakStmt;
import com.google.caja.parser.js.ContinueStmt;
import com.google.caja.parser.js.Declaration;
import com.google.caja.parser.js.Identifier;
import com.google.caja.parser.js.Literal;
import com.google.caja.parser.js.Reference;
import com.google.caja.parser.js.SyntheticNodes;
import com.google.caja.render.Concatenator;
import com.google.caja.render.JsPrettyPrinter;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.RenderContext;
import com.google.caja.util.SyntheticAttributes;

import java.util.HashSet;
import java.util.Set;

/**
 * Rewrites a JavaScript parse tree.
 *
 * @author ihab.awad@gmail.com (Ihab Awad)
 */
public abstract class Rewriter {

  /**
   * Annotations on {@code rules} in subclasses of {@code Rewriter} are
   * are collated and documented by {@code RulesDoclet}
   */
  private final RuleChain rules = new RuleChain();
  private final Set<String> ruleNames = new HashSet<String>();
  final MessageQueue mq;
  private final boolean taintChecking;
  private final boolean logging;

  /**
   * Creates a new Rewriter.
   *
   * @param logging whether this Rewriter should log the details of
   *     rule firings to standard error.
   */
  public Rewriter(MessageQueue mq, boolean taintChecking, boolean logging) {
    assert mq != null;
    this.mq = mq;
    this.taintChecking = taintChecking;
    this.logging = logging;
  }

  /**
   * Returns the rules of this rewriter
   */
  public Iterable<? extends Rule> getRules() {
    return rules.getAllRules();
  }

  /**
   * Alternate form of {@link #expand(ParseTreeNode)}.
   *
   * @param node a parse tree node to expand.
   * @param scope the scope in which 'node' is defined.
   * @return the expanded parse tree node.
   */
  protected final ParseTreeNode expand(ParseTreeNode node, Scope scope) {
    boolean debug = false;
    Iterable<Rule> run = debug ? rules.getAllRules() : rules.applicableTo(node);
    for (Rule rule : run) {
      try {
        ParseTreeNode result = rule.fire(node, scope);
        if (result != Rule.NONE) {
          if (debug && !rules.applicableTo(node).contains(rule)) {
            throw new SomethingWidgyHappenedError(
                rule.getName() + " should be applicable to " + node);
          }
          FilePosition resultPos = result.getFilePosition();
          if (result instanceof AbstractParseTreeNode
              && InputSource.UNKNOWN.equals(resultPos.source())) {
            ((AbstractParseTreeNode) result)
                .setFilePosition(node.getFilePosition());
          }
          if (logging) { logResults(rule, node, result, null); }
          return result;
        }
      } catch (RuntimeException ex) {
        if (logging) { logResults(rule, node, null, ex); }
        throw ex;
      }
    }

    mq.addMessage(
        RewriterMessageType.UNMATCHED_NODE_LEFT_OVER,
        node.getFilePosition(), node);
    return node;
  }

  /**
   * Adds a rule to this rewriter. Rules are evaluated in the order in
   * which they have been added to the rewriter via this method. Rules
   * may not be removed from the rewriter.  No two rules added to the
   * rewriter may have the same {@link Rule#getName() name}.
   *
   * @param rule a rewriting rule.
   * @exception IllegalArgumentException if a rule with a duplicate name is
   *     added.
   */
  public void addRule(Rule rule) {
    // We keep 'ruleNames' as a guard against programming errors
    if (!ruleNames.add(rule.getName())) {
      throw new IllegalArgumentException(
          "Duplicate rule name: " + rule.getName());
    }
    rules.add(rule);
    rule.setRewriter(this);
  }

  /**
   * Adds a list of rules in order to this rewriter.
   *
   * @param rules list of rewriting rules
   * @throws IllegalArgumentException if a rule with a duplicate name is added.
   */
  public void addRules(Rule[] rules) {
    for (Rule r : rules) { addRule(r); }
  }

  private void logResults(
      Rule rule,
      ParseTreeNode input,
      ParseTreeNode result,
      Exception exception) {
    StringBuilder s = new StringBuilder();
    s.append("-----------------------------------------------------------------------\n");
    if (rule != null) {
      s.append("rule: ").append(rule.getName()).append("\n");
    }
    if (input != null) {
      s.append("input: (")
          .append(input.getClass().getSimpleName())
          .append(") ")
          .append(render(input))
          .append("\n");
    }
    if (result != null) {
      s.append("result: (")
          .append(result.getClass().getSimpleName())
          .append(") ")
          .append(render(result))
          .append("\n");
    }
    if (exception != null) {
      s.append("error: ")
          .append(exception.toString())
          .append("\n");
    }
    System.err.println(s.toString());
  }

  public static String render(ParseTreeNode n) {
    StringBuilder output = new StringBuilder();
    TokenConsumer renderer = new JsPrettyPrinter(new Concatenator(output));
    n.render(new RenderContext(renderer));
    renderer.noMoreTokens();
    return output.toString();
  }

  /**
   * Expands a parse tree node according to the rules of this
   * rewriter, returning the expanded result.
   *
   * @param node a top-level parse tree node to expand.
   * @return the expanded parse tree node.
   */
  public final ParseTreeNode expand(ParseTreeNode node) {
    if (taintChecking) {
      flagTainted(node, mq);
      ParseTreeNode result = expand(node, null);
      checkTainted(result, mq);
      return result;
    }
    return expand(node, null);
  }

  private static void flagTainted(ParseTreeNode node, MessageQueue mq) {
    if (node.getAttributes().is(ParseTreeNode.TAINTED)) {
      mq.addMessage(
          RewriterMessageType.MULTIPLY_TAINTED, node, node.getFilePosition());
    }
    node.getAttributes().set(ParseTreeNode.TAINTED, true);
    for (ParseTreeNode n : node.children()) {
      flagTainted(n, mq);
    }
  }

  private static void checkTainted(ParseTreeNode node, MessageQueue mq) {
    // If we've already got errors, then issuing new ones on the same
    // nodes won't help.
    if (mq.hasMessageAtLevel(MessageLevel.ERROR)) {
      return;
    }
    SyntheticAttributes attrs = node.getAttributes();
    if (attrs.is(ParseTreeNode.TAINTED)) {
      if (!attrs.is(SyntheticNodes.SYNTHETIC)) {
        mq.addMessage(
            RewriterMessageType.UNSEEN_NODE_LEFT_OVER, node, node.getFilePosition());
      }
    }
    for (ParseTreeNode n : node.children()) {
      checkTainted(n, mq);
    }
  }

  /**
   * Guard access to this to go through the noexpand() overloadings below.
   */
  private <T extends ParseTreeNode> T removeTaint(T node) {
    if (taintChecking) {
      // TODO(erights): consider returning a defensive copy rather than
      // side effecting in place. If we do, we also need to revisit all
      // calls to removeTaint and noexpand().
      node.getAttributes().remove(ParseTreeNode.TAINTED);
    }
    return node;
  }

  /**
   * Returns its argument, but declares that we are avoiding passing it
   * through the expander on purpose.
   * <p>
   * We are using taint to check that all nodes emerging from this translator
   * are expanded unless stated otherwise, so {@code noexpand()}
   * removes this taint in order to state otherwise.
   */
  protected Reference noexpand(Reference node) {
    removeTaint(node.getIdentifier());
    return removeTaint(node);
  }

  protected Declaration noexpand(Declaration node) {
    if (node.getInitializer() != null) {
      mq.addMessage(
          RewriterMessageType.NOEXPAND_BINARY_DECL,
          node.getFilePosition(), node);
      return node;
    }
    removeTaint(node.getIdentifier());
    return removeTaint(node);
  }

  protected Identifier noexpand(Identifier node) {
    return removeTaint(node);
  }

  protected Literal noexpand(Literal node) {
    return removeTaint(node);
  }

  protected BreakStmt noexpand(BreakStmt node) {
    return removeTaint(node);
  }

  protected ContinueStmt noexpand(ContinueStmt node) {
    return removeTaint(node);
  }

  protected ParseTreeNodeContainer noexpandParams(ParseTreeNodeContainer node) {
    if (taintChecking) {
      for (ParseTreeNode child : node.children()) {
        noexpand((Declaration) child);
      }
      return removeTaint(node);
    }
    return node;
  }
}
