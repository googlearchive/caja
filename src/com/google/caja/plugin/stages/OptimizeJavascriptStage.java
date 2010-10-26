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

import com.google.caja.parser.quasiliteral.opt.ArrayIndexOptimization;
import com.google.caja.plugin.Job;
import com.google.caja.plugin.Jobs;
import com.google.caja.util.ContentType;
import com.google.caja.util.Pipeline.Stage;

public final class OptimizeJavascriptStage implements Stage<Jobs> {
  public boolean apply(Jobs jobs) {
    if (jobs.getPluginMeta().getEnableES53()) {
      for (Job job : jobs.getJobsByType(ContentType.JS)) {
        ArrayIndexOptimization.optimize(job.getRoot());
      }
    }
    return jobs.hasNoFatalErrors();
  }
}
