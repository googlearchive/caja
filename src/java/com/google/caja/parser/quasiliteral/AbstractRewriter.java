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
import com.google.caja.reporting.RenderContext;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Rewrites a parse tree.
 *
 * <p>TODO(ihab.awad): Refactor to a more general quasiliteral library for
 * {@code ParseTreeNode}s and a set of specific implementations for various
 * languages (JS, HTML, CSS, ...).
 *
 * <p>TODO(ihab.awad): All exceptions must be CajaExceptions.
 *
 * @author ihab.awad@gmail.com (Ihab Awad)
 */
public abstract class AbstractRewriter {
  protected static final ParseTreeNode NONE =
      new AbstractParseTreeNode<ParseTreeNode>() {
        public Object getValue() { return null; }
        public void render(RenderContext r) throws IOException { }
      };

  protected static interface Rule {
    ParseTreeNode fire(ParseTreeNode node, Scope scope);
  }

  private final Map<String, QuasiNode> patternCache = new HashMap<String, QuasiNode>();      
  private final List<Rule> rules = new ArrayList<Rule>();
  private final Map<Rule, String> ruleNames = new HashMap<Rule, String>();

  public final ParseTreeNode expand(ParseTreeNode module) {
    return expand(module, new Scope((Block)module));
  }

  protected final ParseTreeNode expand(ParseTreeNode node, Scope scope) {
    for (Rule rule : rules) {

      ParseTreeNode result = null;
      RuntimeException ex = null;
      
      try {
        result = rule.fire(node, scope);
      } catch (RuntimeException e) {
        ex = e;
      }
      
      if (result != NONE || ex != null) {
        if (false) logResults(rule, node, result, ex);
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
      s.append("  rule: " + ruleNames.get(rule) + "\n");
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
    System.out.println(s.toString());
 }

  protected void addRule(String name, Rule rule) {
    if (ruleNames.values().contains(name)) {
      throw new RuntimeException("Duplicate rule named: " + name);
    }
    rules.add(rule);
    ruleNames.put(rule, name);
  }

  protected final boolean match(
      ParseTreeNode node,
      Map<String, ParseTreeNode> bindings,
      QuasiNode pattern) {
    Map<String, ParseTreeNode> tempBindings = pattern.matchHere(node);

    if (tempBindings != null) {
      bindings.putAll(tempBindings);
      return true;
    }
    return false;
  }

  protected final boolean match(
      ParseTreeNode node,
      Map<String, ParseTreeNode> bindings,
      String patternText) {
    return match(node, bindings, getPatternNode(patternText));
  }

  protected final ParseTreeNode subst(
      Map<String, ParseTreeNode> bindings,
      String patternText) {
    return subst(bindings, getPatternNode(patternText));
  }

  protected final ParseTreeNode subst(
      Map<String, ParseTreeNode> bindings,
      QuasiNode pattern) {
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
    for (int i = 0; i < args.length - 1; ){
      bindings.put(
          (String)args[i++],
          (ParseTreeNode)args[i++]);
    }
    return subst(bindings, (String)args[args.length - 1]);    
  }

  protected final void expandEntry(
      Map<String, ParseTreeNode> bindings,
      String key,
      Scope scope) {
    bindings.put(key, expand(bindings.get(key), scope));
  }

  protected final void expandEntries(
      Map<String, ParseTreeNode> bindings,
      Scope scope) {
    for (String key : bindings.keySet()) {
      expandEntry(bindings, key, scope);
    }
  }

  protected final ParseTreeNode expandAll(ParseTreeNode node, Scope scope) {
    List<ParseTreeNode> rewrittenChildren = new ArrayList<ParseTreeNode>();
    for (ParseTreeNode child : node.children()) {
      rewrittenChildren.add(expand(child, scope));
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