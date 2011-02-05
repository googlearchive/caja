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
import com.google.caja.util.Pipeline;

import java.util.List;
import java.util.ListIterator;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Checks the cache to find out if jobs have already been processed, replacing
 * any with the processed version from the cache, and associating cache keys
 * with any jobs that are not in the cache so that the later
 * {@link PipelineStoreStage} can put them in the cache.
 *
 * @author Mike Samuel <mikesamuel@gmail.com>
 */
@ParametersAreNonnullByDefault
public final class PipelineFetchStage implements Pipeline.Stage<Jobs> {
  private final JobCache cache;

  public PipelineFetchStage(JobCache cache) {
    this.cache = cache;
  }

  public boolean apply(Jobs jobs) {
    for (ListIterator<JobEnvelope> it = jobs.getJobs().listIterator();
         it.hasNext();) {
      JobEnvelope env = it.next();
      if (env.fromCache
          || env.job.getRoot().getAttributes().is(JobCache.NO_CACHE)) {
        continue;
      }
      Job job = env.job;
      JobCache.Key key = cache.forJob(job.getType(), job.getRoot());
      List<? extends Job> fromCache = cache.fetch(key);
      if (fromCache != null) {
        it.remove();
        for (Job cacheJob : fromCache) {
          JobEnvelope replacement = new JobEnvelope(
              // Use the placeholder from the original.
              // Placeholders are not part of the cache.
              env.placeholderId,
              // For cached jobs, we don't need a key since we're not going to
              // put them back.
              env.cacheKeys,
              // The source type from the original.
              env.sourceType,
              // It came from the cache.
              true,
              // The cache is responsible for returning a copy that we can
              // mutate.
              cacheJob);
          it.add(replacement);
        }
      } else {
        it.set(new JobEnvelope(
            env.placeholderId, key.asSingleton(), env.sourceType, false, job));
      }
    }
    return jobs.hasNoFatalErrors();
  }
}
