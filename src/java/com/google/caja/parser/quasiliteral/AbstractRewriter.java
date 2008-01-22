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

import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.ParseException;
import com.google.caja.parser.AbstractParseTreeNode;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.ParseTreeNodes;
import com.google.caja.parser.js.Block;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.RenderContext;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Rewrites a parse tree.
 *
 * <p>TODO(ihab.awad): Refactor to a more general quasiliteral library for
 * {@code ParseTreeNode}s and a set of specific implementations for various
 * languages (JS, HTML, CSS, ...). At the moment, our problem is the fact
 * that the {@code Scope} implementation is JavaScript specific.
 *
 * @author ihab.awad@gmail.com (Ihab Awad)
 */
public abstract class AbstractRewriter {
  
  /**
   * The special return value from a {@link Rule} that indicates the rule
   * does not apply to the supplied input.
   */
  protected static final ParseTreeNode NONE =
      new AbstractParseTreeNode<ParseTreeNode>() {
        @Override public Object getValue() { return null; }
        public void render(RenderContext r) {
          throw new UnsupportedOperationException();
        }
      };

  /**
   * A rewriting rule supplied by a subclass.
   */
  protected static abstract class Rule implements MessagePart {

    private final String name;

    /**
     * Create a new {@code Rule}.
     * @param name the unique name of this rule.
     */
    public Rule(String name) {
      this.name = name;
    }

    /**
     * @return the name of this {@code Rule}.
     */
    public String getName() { return name; }
    
    /**
     * Process the given input, returning a rewritten node.
     *
     * @param node an input node.
     * @param scope the current scope.
     * @param mq a {@code MessageQueue} for error reporting.
     * @return the rewritten node, or {@link AbstractRewriter#NONE} to indicate
     * that this rule does not apply to the given input. 
     */
    public abstract ParseTreeNode fire(
        ParseTreeNode node,
        Scope scope,
        MessageQueue mq);

    /**
     * @see MessagePart#format(MessageContext,Appendable)
     */
    public void format(MessageContext mc, Appendable out) throws IOException {
      out.append("Rule \"" + name + "\"");
    }
  }

  private final Map<String, QuasiNode> patternCache = new HashMap<String, QuasiNode>();      
  private final List<Rule> rules = new ArrayList<Rule>();
  private final Set<String> ruleNames = new HashSet<String>();
  private final boolean logging;

  protected AbstractRewriter(boolean logging) {
    this.logging = logging;
  }

  public final ParseTreeNode expand(ParseTreeNode module, MessageQueue mq) {
    return expand(module, new Scope((Block)module), mq);
  }

  protected final ParseTreeNode expand(ParseTreeNode node, Scope scope, MessageQueue mq) {
    for (Rule rule : rules) {

      ParseTreeNode result = null;
      RuntimeException ex = null;
      
      try {
        result = rule.fire(node, scope, mq);
      } catch (RuntimeException e) {
        ex = e;
      }
      
      if (result != NONE || ex != null) {
        if (logging) logResults(rule, node, result, ex);
        if (ex != null) throw ex;
        return result;
      }
    }

    throw new RuntimeException("Unrecognized node: " + node);
  }

  private void logResults(
      Rule rule,
      ParseTreeNode input,
      ParseTreeNode result,
      Exception exception) {
    StringBuilder s = new StringBuilder();
    s.append("-----------------------------------------------------------------------\n");
    if (rule != null) {
      s.append("  rule: " + rule.getName() + "\n");
    }
    if (input != null) {
      s.append(" input: (" + input.getClass().getSimpleName() + ") " + format(input) + "\n");
    }
    if (result != null) {
      s.append("result: (" + result.getClass().getSimpleName() + ") " + format(result) + "\n");
    }
    if (exception != null) {
      s.append(" error: ").append(exception.toString()).append("\n");
    }
    System.err.println(s.toString());
  }

  protected void addRule(Rule rule) {
    // We keep 'ruleNames' as a guard against programming errors
    assert(!ruleNames.contains(rule.getName()));
    rules.add(rule);
    ruleNames.add(rule.getName());
  }

  protected final boolean match(
      QuasiNode pattern,
      ParseTreeNode node) {
    return match(pattern, node, new HashMap<String, ParseTreeNode>());
  }

  protected final boolean match(
      QuasiNode pattern,
      ParseTreeNode node,
      Map<String, ParseTreeNode> bindings) {
    Map<String, ParseTreeNode> tempBindings = pattern.matchHere(node);

    if (tempBindings != null) {
      bindings.putAll(tempBindings);
      return true;
    }
    return false;
  }

  protected final boolean match(
      String patternText,
      ParseTreeNode node) {
    return match(getPatternNode(patternText), node);
  }

  protected final boolean match(
      String patternText,
      ParseTreeNode node,
      Map<String, ParseTreeNode> bindings) {
    return match(getPatternNode(patternText), node, bindings);
  }

  protected final ParseTreeNode subst(
      String patternText,
      Map<String, ParseTreeNode> bindings) {
    return subst(getPatternNode(patternText), bindings);
  }

  protected final ParseTreeNode subst(
      QuasiNode pattern,
      Map<String, ParseTreeNode> bindings) {
    ParseTreeNode result = pattern.substituteHere(bindings);

    if (result == null) {
      // Pattern programming error
      // TODO(ihab.awad): Provide a detailed dump of the bindings in the exception
      throw new RuntimeException("Failed to substitute into: \"" + pattern + "\"");
    }

    return result;
  }

  protected final ParseTreeNode substV(Object... args) {
    if (args.length %2 == 0) throw new RuntimeException("Wrong # of args for subst()");
    Map<String, ParseTreeNode> bindings = new HashMap<String, ParseTreeNode>();
    for (int i = 1; i < args.length; ) {
      bindings.put(
          (String)args[i++],
          (ParseTreeNode)args[i++]);
    }
    return subst((String)args[0], bindings);
  }

  protected final void expandEntry(
      Map<String, ParseTreeNode> bindings,
      String key,
      Scope scope,
      MessageQueue mq) {
    bindings.put(key, expand(bindings.get(key), scope, mq));
  }

  protected final void expandEntries(
      Map<String, ParseTreeNode> bindings,
      Scope scope,
      MessageQueue mq) {
    for (String key : bindings.keySet()) {
      expandEntry(bindings, key, scope, mq);
    }
  }

  protected final ParseTreeNode expandAll(ParseTreeNode node, Scope scope, MessageQueue mq)
      {
    List<ParseTreeNode> rewrittenChildren = new ArrayList<ParseTreeNode>();
    for (ParseTreeNode child : node.children()) {
      rewrittenChildren.add(expand(child, scope, mq));
    }

    return ParseTreeNodes.newNodeInstance(
        node.getClass(),
        node.getValue(),
        rewrittenChildren);
  }

  protected final QuasiNode getPatternNode(String patternText) {
    if (!patternCache.containsKey(patternText)) {
      try {
        patternCache.put(
            patternText,
            QuasiBuilder.parseQuasiNode(
                new InputSource(URI.create("built-in:///js-quasi-literals")),
                patternText));
      } catch (ParseException e) {
        // Pattern programming error
        throw new RuntimeException(e);
      }
    }
    return patternCache.get(patternText);
  }

  protected final String stringJoin(String delim, String[] values) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < values.length; i++) {
      sb.append(values[i]);
      if (i < values.length - 1) sb.append(delim);
    }
    return sb.toString();
  }

  protected String format(ParseTreeNode n) {
    try {
      StringBuilder output = new StringBuilder();
      n.render(new RenderContext(new MessageContext(), output));
      return output.toString();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
