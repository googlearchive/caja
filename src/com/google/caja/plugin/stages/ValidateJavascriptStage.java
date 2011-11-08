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

import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.js.ArrayConstructor;
import com.google.caja.parser.js.CajoledModule;
import com.google.caja.parser.js.Expression;
import com.google.caja.parser.js.Statement;
import com.google.caja.parser.js.StringLiteral;
import com.google.caja.parser.js.UncajoledModule;
import com.google.caja.parser.quasiliteral.ModuleManager;
import com.google.caja.plugin.ExpressionSanitizerCaja;
import com.google.caja.plugin.Job;
import com.google.caja.plugin.JobEnvelope;
import com.google.caja.plugin.Jobs;
import com.google.caja.util.ContentType;
import com.google.caja.util.Maps;
import com.google.caja.util.Pipeline;

import java.net.URI;
import java.util.ListIterator;
import java.util.Map;

/**
 * Rewrite the javascript to prevent runtime sandbox violations.
 *
 * @author mikesamuel@gmail.com
 */
public final class ValidateJavascriptStage implements Pipeline.Stage<Jobs> {
  private final ModuleManager mgr;

  public ValidateJavascriptStage(ModuleManager mgr) {
    this.mgr = mgr;
  }

  public boolean apply(Jobs jobs) {
    Map<String, JobCache.Keys> keys = Maps.newHashMap();
    for (ListIterator<JobEnvelope> it = jobs.getJobs().listIterator();
         it.hasNext();) {
      JobEnvelope env = it.next();

      if (env.fromCache) { continue; }
      Job job = env.job;
      if (job.getType() != ContentType.JS) { continue; }

      URI baseUri = job.getBaseUri();
      Statement s = (Statement) job.getRoot();
      ParseTreeNode result = new ExpressionSanitizerCaja(mgr, baseUri)
          .sanitize(UncajoledModule.of(s));
      if (!(result instanceof CajoledModule)) {
        // Rewriter failed to rewrite so returned its input.
        // There should be details on the message queue.
        it.remove();
        continue;
      }
      CajoledModule validated = (CajoledModule) result;
      it.set(env.withJob(Job.cajoledJob(validated)));

      if (env.cacheKeys.iterator().hasNext()) {
        ArrayConstructor deps = validated.getInlinedModules();
        if (deps != null) {
          for (Expression moduleName : deps.children()) {
            String moduleUri = ((StringLiteral) moduleName).getUnquotedValue();
            JobCache.Keys forUri = keys.get(moduleUri);
            if (forUri == null) {
              forUri = env.cacheKeys;
            } else {
              forUri = forUri.union(env.cacheKeys);
            }
            keys.put(moduleUri, env.cacheKeys);
          }
        }
      }
    }

    // Unpack any loaded modules onto the job queue so we can make sure they
    // show up in the appropriate caches.
    for (CajoledModule module : mgr.getModuleMap()) {
      String src = module.getSrc();
      JobCache.Keys keysForModule = keys.get(src);
      if (keysForModule == null) {
        keysForModule = JobCache.none();
      }
      jobs.getJobs().add(new JobEnvelope(
          null, keysForModule, ContentType.JS, false, Job.cajoledJob(module)));
    }

    return jobs.hasNoFatalErrors();
  }
}
