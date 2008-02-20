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
import com.google.caja.parser.css.CssTree;
import com.google.caja.plugin.CssRewriter;
import com.google.caja.plugin.CssTemplate;
import com.google.caja.plugin.CssValidator;
import com.google.caja.plugin.Job;
import com.google.caja.plugin.Jobs;
import com.google.caja.util.Pipeline;

/**
 * Make sure the css is well formed and prefix all rules
 * so that they don't affect nodes outside the plugin.
 */
public class ValidateCssStage implements Pipeline.Stage<Jobs> {
  /**
   * Sanitizes and namespace any css jobs.
   * @return true if the input css was safe.  False if any destructive
   *   modifications had to be made to make it safe, or if such modifications
   *   were needed but could not be made.
   */
  public boolean apply(Jobs jobs) {
    // TODO(mikesamuel): build up a list of classes and ids for use in
    // generating "no such symbol" warnings from the GXPs.
    boolean valid = true;
    CssValidator v = new CssValidator(jobs.getMessageQueue());
    CssRewriter rw = new CssRewriter(
        jobs.getPluginMeta(), jobs.getMessageQueue());
    for (Job job : jobs.getJobsByType(Job.JobType.CSS)) {
      // The parsetree node is a CssTree.StyleSheet
      AncestorChain<CssTree> cssTree = job.getRoot().cast(CssTree.class);
      valid &= v.validateCss(cssTree);
      valid &= rw.rewrite(cssTree);
    }
    for (Job job : jobs.getJobsByType(Job.JobType.CSS_TEMPLATE)) {
      AncestorChain<CssTemplate> tmpl = job.getRoot().cast(CssTemplate.class);
      AncestorChain<CssTree> cssTree
          = new AncestorChain<CssTree>(tmpl, tmpl.node.getCss());
      valid &= v.validateCss(cssTree);
      valid &= rw.rewrite(cssTree);
    }
    for (Job job : jobs.getJobsByType(Job.JobType.CSS_TEMPLATE)) {
      // The parsetree node is a CssTree.StyleSheet
      AncestorChain<CssTemplate> chain = job.getRoot().cast(CssTemplate.class);
      valid &= validate(
          v, rw, new AncestorChain<CssTree>(chain, chain.node.getCss()));
    }

    return valid && jobs.hasNoFatalErrors();
  }

  private static final boolean validate(
      CssValidator v, CssRewriter rw, AncestorChain<CssTree> css) {
    return v.validateCss(css) & rw.rewrite(css);
  }
}
