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

import com.google.caja.parser.css.CssTree;
import com.google.caja.plugin.CssDynamicExpressionRewriter;
import com.google.caja.plugin.Job;
import com.google.caja.plugin.JobEnvelope;
import com.google.caja.plugin.Jobs;
import com.google.caja.util.ContentType;
import com.google.caja.util.Pipeline;

/**
 * Compiles CSS style-sheets to JavaScript which outputs the same CSS, but with
 * rules only affecting nodes that are children of a class whose name contains
 * the gadget id.
 *
 * @author mikesamuel@gmail.com
 */
public final class RewriteCssStage implements Pipeline.Stage<Jobs> {
  public boolean apply(Jobs jobs) {
    for (JobEnvelope env : jobs.getJobsByType(ContentType.CSS)) {
      if (env.fromCache) { continue; }
      Job job = env.job;

      new CssDynamicExpressionRewriter(jobs.getPluginMeta()).rewriteCss(
          (CssTree.StyleSheet) job.getRoot());
    }
    return jobs.hasNoFatalErrors();
  }
}
