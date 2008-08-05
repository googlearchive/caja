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

package com.google.caja.opensocial.applet;

import com.google.caja.parser.AncestorChain;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.Conditional;
import com.google.caja.parser.js.ExpressionStmt;
import com.google.caja.parser.js.Statement;
import com.google.caja.parser.js.SyntheticNodes;
import com.google.caja.parser.js.TryStmt;
import com.google.caja.parser.quasiliteral.QuasiBuilder;
import com.google.caja.plugin.Job;
import com.google.caja.plugin.Jobs;
import com.google.caja.util.Pipeline;
import java.util.List;

/**
 * A pipeline stage that adds expression language semantics to a caja program.
 * The last statement in the program is treated as the "result" of the program,
 * so a program like <code>{ var a = 2; a + a }</code> when cajoled and
 * evaluated will yield the value 4.
 *
 * <p>This stage analyzes the program using the following rules:
 * <ul>
 *   <li>If it is a {@link Block}, recurse on the last non-synthetic statement.
 *   <li>If it is an {@link ExpressionStmt} e, rewrite to {@code ___.yield(e)}.
 *   <li>If it is a {@link Conditional}, recurse to each branch.
 *   <li>If it is a {@link TryStmt}, recurse to the body but not the
 *       catch/finally.
 *   <li>Else do nothing.
 * </ul>
 * We do not try to modify loops or switches.
 *
 * @author mikesamuel@gmail.com
 */
final class ExpressionLanguageStage implements Pipeline.Stage<Jobs> {
  public boolean apply(Jobs jobs) {
    for (Job job : jobs.getJobsByType(Job.JobType.JAVASCRIPT)) {
      apply(job.getRoot().cast(Statement.class));
    }
    return true;
  }

  public static void apply(AncestorChain<? extends Statement> root) {
    Statement rootNode = root.node;
    if (rootNode instanceof Block) {
      for (Statement s : ((Block) rootNode).children()) {
        // Rewrite each extracted block of javascript independently.
        // We skip synthetic nodes such as extracted event handlers, and
        // compiled CSS and HTML.
        if (!s.getAttributes().is(SyntheticNodes.SYNTHETIC)) {
          rewrite(AncestorChain.instance(root, s));
        }
      }
    }
  }

  private static void rewrite(AncestorChain<?> ac) {
    ParseTreeNode node = ac.node;
    if (node instanceof ExpressionStmt) {
      ExpressionStmt es = (ExpressionStmt) node;
      es.replaceChild(
          QuasiBuilder.substV("IMPORTS___.yield(@e)", "e", es.getExpression()),
          es.getExpression());
    } else if (node instanceof Block) {
      Block b = (Block) node;
      if (!b.children().isEmpty()) {
        rewrite(new AncestorChain<Statement>(
                    ac, b.children().get(b.children().size() - 1)));
      }
    } else if (node instanceof Conditional) {
      List<? extends ParseTreeNode> children = ((Conditional) node).children();
      int n = children.size();
      for (int i = 1; i < n; i += 2) {  // Even are conditions.
        rewrite(new AncestorChain<ParseTreeNode>(ac, children.get(i)));
      }
      if ((n & 1) == 1) {  // else clause
        rewrite(new AncestorChain<ParseTreeNode>(ac, children.get(n - 1)));
      }
    } else if (node instanceof TryStmt) {
      rewrite(new AncestorChain<Statement>(ac, ((TryStmt) node).getBody()));
    }
  }
}
