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

import com.google.caja.lexer.ParseException;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.js.Block;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.RenderContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Rewrites a JavaScript parse tree.
 *  
 * @author ihab.awad@gmail.com (Ihab Awad)
 */
public abstract class Rewriter {
  private final Map<String, QuasiNode> patternCache = new HashMap<String, QuasiNode>();
  private final List<Rule> rules = new ArrayList<Rule>();
  private final Set<String> ruleNames = new HashSet<String>();
  private final boolean logging;

  /**
   * Creates a new Rewriter.
   *
   * @param logging whether this Rewriter should log the details of rule firings to
   * standard error.
   */
  public Rewriter(boolean logging) {
    this.logging = logging;
  }

  /**
   * Expands a parse tree node according to the rules of this rewriter, returning
   * the expanded result.
   *
   * @param node a top-level parse tree node to expand.
   * @param mq a message queue for compiler messages.
   * @return the expanded parse tree node.
   */
  public final ParseTreeNode expand(ParseTreeNode node, MessageQueue mq) {
    return expand(node, Scope.fromRootBlock((Block)node, mq), mq);
  }

  /**
   * Alternate form of {@link #expand(ParseTreeNode, MessageQueue)}.
   *
   * @param node a parse tree node to expand.
   * @param scope the scope in which 'node' is defined.
   * @param mq a message queue for compiler messages.
   * @return the exapnded parse tree node.
   */
  public final ParseTreeNode expand(ParseTreeNode node, Scope scope, MessageQueue mq) {
    for (Rule rule : rules) {

      ParseTreeNode result = null;
      RuntimeException ex = null;
      
      try {
        result = rule.fire(node, scope, mq);
      } catch (RuntimeException e) {
        ex = e;
      }
      
      if (result != Rule.NONE || ex != null) {
        if (logging) logResults(rule, node, result, ex);
        if (ex != null) throw ex;
        return result;
      }
    }

    throw new RuntimeException("Unimplemented case involving: " + node);
  }

  /**
   * Adds a rule to this rewriter. Rules are evaluated in the order in which they have
   * been added to the rewriter via this method. Rules may not be removed from the rewriter.
   * No two rules added to the rewriter may have the same
   * {@link com.google.caja.parser.quasiliteral.Rule#getName() name}.
   *
   * @param rule a rewriting rule.
   * @exception IllegalArgumentException if a rule with a duplicate name is added.
   */
  public void addRule(Rule rule) {
    // We keep 'ruleNames' as a guard against programming errors
    if (ruleNames.contains(rule.getName()))
      throw new IllegalArgumentException("Duplicate rule name: " + rule.getName());
    rules.add(rule);
    ruleNames.add(rule.getName());
  }

  /**
   * Obtains a quasiliteral node from a text pattern. Components of the rewriter should prefer
   * this methood over calling a {@link QuasiBuilder} directly since this method matains a
   * cache of pre-compiled patterns.
   *
   * @param patternText a quasiliteral pattern.
   * @return the quasiliteral node represented by the supplied pattern.
   */
  public final QuasiNode getPatternNode(String patternText) {
    if (!patternCache.containsKey(patternText)) {
      try {
        patternCache.put(
            patternText,
            QuasiBuilder.parseQuasiNode(patternText));
      } catch (ParseException e) {
        // Pattern programming error
        throw new RuntimeException(e);
      }
    }
    return patternCache.get(patternText);
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
          .append(format(input))
          .append("\n");
    }
    if (result != null) {
      s.append("result: (")
          .append(result.getClass().getSimpleName())
          .append(") ")
          .append(format(result))
          .append("\n");
    }
    if (exception != null) {
      s.append("error: ")
          .append(exception.toString())
          .append("\n");
    }
    System.err.println(s.toString());
  }

  private String format(ParseTreeNode n) {
    try {
      StringBuilder output = new StringBuilder();
      n.render(new RenderContext(new MessageContext(), output));
      return output.toString();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
