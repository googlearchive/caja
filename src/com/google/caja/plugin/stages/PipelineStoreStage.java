// Copyright (C) 2010 Google Inc.
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

import com.google.caja.plugin.Job;
import com.google.caja.plugin.JobEnvelope;
import com.google.caja.plugin.Jobs;
import com.google.caja.util.Multimap;
import com.google.caja.util.Multimaps;
import com.google.caja.util.Pipeline;
import com.google.common.collect.Lists;

import java.util.ListIterator;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Responsible for updating the job cache with the results of processing
 * jobs that did not come {@link JobEnvelope#fromCache from the cache}.
 *
 * @author Mike Samuel <mikesamuel@gmail.com>
 * @see PipelineFetchStage
 */
@ParametersAreNonnullByDefault
public final class PipelineStoreStage implements Pipeline.Stage<Jobs> {
  private final JobCache cache;

  public PipelineStoreStage(JobCache cache) {
    this.cache = cache;
  }

  public boolean apply(Jobs jobs) {
    Multimap<JobCache.Key, Job> byKey = Multimaps.newListHashMultimap();
    for (ListIterator<JobEnvelope> it = jobs.getJobs().listIterator();
         it.hasNext();) {
      JobEnvelope env = it.next();
      if (!env.fromCache) {
        for (JobCache.Key key : env.cacheKeys) {
          byKey.put(key, env.job);
        }
      }
      // Reset the fromCache marker, since any job that came from the cache is
      // now at the same level of processing as any jobs that did not come from
      // the cache; and release any cache keys for GC.
      if (env.fromCache || env.cacheKeys.iterator().hasNext()) {
        it.set(new JobEnvelope(
            env.placeholderId, JobCache.none(), env.sourceType, false,
            env.job));
      }
    }
    for (JobCache.Key key : byKey.keySet()) {
      cache.store(key, Lists.newArrayList(byKey.get(key)));
    }
    return jobs.hasNoFatalErrors();
  }
}
