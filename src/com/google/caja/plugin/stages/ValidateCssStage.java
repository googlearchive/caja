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

import com.google.caja.lang.css.CssSchema;
import com.google.caja.lang.html.HtmlSchema;
import com.google.caja.parser.AncestorChain;
import com.google.caja.parser.css.CssTree;
import com.google.caja.plugin.CssRewriter;
import com.google.caja.plugin.CssValidator;
import com.google.caja.plugin.Job;
import com.google.caja.plugin.Jobs;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.util.Pipeline;

/**
 * Make sure the css is well formed and obeys the HTML and CSS Schemas.
 *
 * @author mikesamuel@gmail.com
 */
public final class ValidateCssStage implements Pipeline.Stage<Jobs> {
  private final CssSchema cssSchema;
  private final HtmlSchema htmlSchema;

  public ValidateCssStage(CssSchema cssSchema, HtmlSchema htmlSchema) {
    if (null == cssSchema) { throw new NullPointerException(); }
    if (null == htmlSchema) { throw new NullPointerException(); }
    this.cssSchema = cssSchema;
    this.htmlSchema = htmlSchema;
  }

  /**
   * Sanitizes and namespace any css jobs.
   * @return true if the input css was safe.  False if any destructive
   *   modifications had to be made to make it safe, or if such modifications
   *   were needed but could not be made.
   */
  public boolean apply(Jobs jobs) {
    // TODO(mikesamuel): build up a list of classes and ids for use in
    // generating "no such symbol" warnings from the GXPs/HTML.
    CssValidator v = new CssValidator(
        cssSchema, htmlSchema, jobs.getMessageQueue());
    CssRewriter rw = new CssRewriter(
        jobs.getPluginMeta(), jobs.getMessageQueue());

    v.withInvalidNodeMessageLevel(MessageLevel.WARNING);
    rw.withInvalidNodeMessageLevel(MessageLevel.WARNING);
    for (Job job : jobs.getJobsByType(Job.JobType.CSS)) {
      validate(v, rw, job.getRoot().cast(CssTree.class));
    }

    v.withInvalidNodeMessageLevel(MessageLevel.ERROR);
    rw.withInvalidNodeMessageLevel(MessageLevel.ERROR);

    return jobs.hasNoFatalErrors();
  }

  private static final void validate(
      CssValidator v, CssRewriter rw, AncestorChain<CssTree> css) {
    v.validateCss(css);
    rw.rewrite(css);
  }
}
