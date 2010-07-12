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

import com.google.caja.parser.js.CajoledModule;
import com.google.caja.parser.quasiliteral.CajitaModuleRewriter;
import com.google.caja.parser.quasiliteral.ModuleManager;
import com.google.caja.plugin.Job;
import com.google.caja.plugin.Jobs;
import com.google.caja.util.ContentType;
import com.google.caja.util.Lists;
import com.google.caja.util.Pipeline;

import java.util.List;

/**
 * Put all the top level javascript code into an initializer block
 * that will set up the plugin.
 *
 * @author mikesamuel@gmail.com
 */
public final class ConsolidateCodeStage implements Pipeline.Stage<Jobs> {
  private final ModuleManager mgr;

  public ConsolidateCodeStage(ModuleManager mgr) { this.mgr = mgr; }

  public boolean apply(Jobs jobs) {
    List<Job> jsJobs = jobs.getJobsByType(ContentType.JS);
    List<CajoledModule> modules = Lists.newArrayList();
    for (Job job : jsJobs) {
      CajoledModule module = (CajoledModule) job.getRoot();
      if (module.getSrc() == null) {
        // Is top level.  Not a loaded module from ValidateJavaScriptStage.
        modules.add(module);
      }
    }
    jobs.getJobs().removeAll(jsJobs);
    CajitaModuleRewriter rw = new CajitaModuleRewriter(mgr);
    jobs.getJobs().add(Job.cajoledJob(null, rw.rewrite(modules)));
    return jobs.hasNoFatalErrors();
  }
}
