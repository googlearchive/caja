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

import com.google.caja.lang.css.CssSchema;
import com.google.caja.parser.AncestorChain;
import com.google.caja.parser.js.Expression;
import com.google.caja.parser.js.FunctionConstructor;
import com.google.caja.parser.js.FunctionDeclaration;
import com.google.caja.plugin.CssTemplate;
import com.google.caja.plugin.GxpCompiler.BadContentException;
import com.google.caja.plugin.Job;
import com.google.caja.plugin.Jobs;
import com.google.caja.util.Pipeline;

import java.util.ListIterator;

/**
 * Takes CSS templates and turns them into functions.
 *
 * @author mikesamuel@gmail.com
 */
public final class CompileCssTemplatesStage implements Pipeline.Stage<Jobs> {
  private final CssSchema cssSchema;

  public CompileCssTemplatesStage(CssSchema cssSchema) {
    if (cssSchema == null) { throw new NullPointerException(); }
    this.cssSchema = cssSchema;
  }

  public boolean apply(Jobs jobs) {
    ListIterator<Job> it = jobs.getJobs().listIterator();
    while (it.hasNext()) {
      Job job = it.next();
      if (job.getType() != Job.JobType.CSS_TEMPLATE) { continue; }
      CssTemplate t = (CssTemplate) job.getRoot().node;
      Job replacement;
      if (t.getTemplateNameIdentifier().getName() != null) {
        // Compile to a function and add as a global definition.
        FunctionConstructor function;
        try {
          function = t.toFunction(cssSchema, jobs.getMessageQueue());
        } catch (BadContentException ex) {
          ex.toMessageQueue(jobs.getMessageQueue());
          continue;
        }

        FunctionDeclaration decl = new FunctionDeclaration(
            function.getIdentifier(), function);
        replacement = new Job(new AncestorChain<FunctionDeclaration>(decl));
      } else {
        // Compile to an expression that can be spliced back where it came from.
        // These kinds of templates are extracted via the OpenTemplateStage.
        Expression expr;
        try {
          expr = t.toPropertyValueList(cssSchema, jobs.getMessageQueue());
        } catch (BadContentException ex) {
          ex.toMessageQueue(jobs.getMessageQueue());
          continue;
        }

        replacement = new Job(
            new AncestorChain<Expression>(expr), job.getTarget());
      }
      it.remove();
      it.add(replacement);
    }
    return jobs.hasNoFatalErrors();
  }
}
