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
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.CajoledModule;
import com.google.caja.parser.js.Expression;
import com.google.caja.parser.js.ExpressionStmt;
import com.google.caja.parser.js.Statement;
import com.google.caja.parser.js.StringLiteral;
import com.google.caja.parser.js.UncajoledModule;
import com.google.caja.parser.quasiliteral.ModuleManager;
import com.google.caja.plugin.ExpressionSanitizerCaja;
import com.google.caja.plugin.Job;
import com.google.caja.plugin.Jobs;
import com.google.caja.util.ContentType;
import com.google.caja.util.Maps;
import com.google.caja.util.Pipeline;

import java.net.URI;
import java.util.Collections;
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
    for (ListIterator<Job> it = jobs.getJobs().listIterator(); it.hasNext();) {
      Job job = it.next();
      if (job.getType() != ContentType.JS) { continue; }
      JobCache.Keys cacheKeys = job.getCacheKeys();

      URI baseUri = job.getBaseUri();
      Statement s = (Statement) job.getRoot();
      ParseTreeNode result = new ExpressionSanitizerCaja(mgr, baseUri)
          .sanitize(uncajoledModule(s), jobs.getPluginMeta().getEnableES53());
      if (!(result instanceof CajoledModule)) {
        // Rewriter failed to rewrite so returned its input.
        // There should be details on the message queue.
        it.remove();
        continue;
      }
      CajoledModule validated = (CajoledModule) result;
      it.set(Job.cajoledJob(cacheKeys, validated));

      if (cacheKeys.iterator().hasNext()) {
        ArrayConstructor deps = validated.getInlinedModules();
        for (Expression moduleName : deps.children()) {
          String moduleUri = ((StringLiteral) moduleName).getUnquotedValue();
          JobCache.Keys forUri = keys.get(moduleUri);
          if (forUri == null) {
            forUri = cacheKeys;
          } else {
            forUri = forUri.union(cacheKeys);
          }
          keys.put(moduleUri, cacheKeys);
        }
      }
    }

    // Unpack any loaded modules onto the job queue so we can make sure they
    // show up in the appropriate caches.
    for (CajoledModule module : mgr.getModuleMap()) {
      String src = module.getSrc();
      jobs.getJobs().add(Job.cajoledJob(keys.get(src), module));
    }

    return jobs.hasNoFatalErrors();
  }

  private static UncajoledModule uncajoledModule(ParseTreeNode node) {
    Block body;
    if (node instanceof Block) {
      body = (Block) node;
    } else {
      if (node instanceof Expression) {
        node = new ExpressionStmt((Expression) node);
      }
      body = new Block(
          node.getFilePosition(), Collections.singletonList((Statement) node));
    }

    return new UncajoledModule(body);
  }
}
