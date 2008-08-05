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

package com.google.caja.plugin.stages;

import com.google.caja.parser.AncestorChain;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.js.SyntheticNodes;
import com.google.caja.parser.js.Block;
import com.google.caja.plugin.ExpressionSanitizerCaja;
import com.google.caja.plugin.Job;
import com.google.caja.plugin.Jobs;
import com.google.caja.util.Pipeline;

/**
 * Rewrite the javascript to prevent runtime sandbox violations.
 *
 * @author mikesamuel@gmail.com
 */
public final class ValidateJavascriptStage implements Pipeline.Stage<Jobs> {
  public boolean apply(Jobs jobs) {
    boolean valid = true;
    for (Job job : jobs.getJobsByType(Job.JobType.JAVASCRIPT)) {
      // Pass in the rootmost scope that has non-synthetic children, so that
      // the Caja rules correctly identify global function declarations.
      AncestorChain<?> nonSyntheticScopeRoot
          = nonSyntheticScopeRoot(job.getRoot());

      if (nonSyntheticScopeRoot != null) {  // False for empty programs
        valid &= new ExpressionSanitizerCaja(
            jobs.getMessageQueue(), jobs.getPluginMeta())
            .sanitize(nonSyntheticScopeRoot);
      }
    }

    return valid && jobs.hasNoFatalErrors();
  }

  public AncestorChain<?> nonSyntheticScopeRoot(AncestorChain<?> js) {
    AncestorChain<?> scopeRoot = nonSyntheticRoot(js);
    if (scopeRoot == null) { return null; }
    while (scopeRoot.parent != null && !(scopeRoot.node instanceof Block)) {
      scopeRoot = scopeRoot.parent;
    }
    return scopeRoot;
  }

  public AncestorChain<?> nonSyntheticRoot(AncestorChain<?> js) {
    ParseTreeNode node = js.node;

    if (SyntheticNodes.isSynthesizable(node)
        && !node.getAttributes().is(SyntheticNodes.SYNTHETIC)) {
      return js;
    }

    // If any children are non-synthetic
    AncestorChain<?> nonSyntheticChild = null;
    for (ParseTreeNode child : node.children()) {
      AncestorChain<?> result = nonSyntheticRoot(
          new AncestorChain<ParseTreeNode>(js, child));
      if (result != null) {
        // Two children at least, so return js as the LCD
        if (nonSyntheticChild != null) { return js; }
        nonSyntheticChild = result;
      }
    }

    return nonSyntheticChild;
  }
}
