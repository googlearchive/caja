// Copyright (C) 2011 Google Inc.
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

package com.google.caja.demos.playground.server;

import java.util.List;
import java.util.Collections;

import net.sf.jsr107cache.Cache;
import net.sf.jsr107cache.CacheManager;

import com.google.caja.parser.ParseTreeNode;
import com.google.caja.plugin.Job;
import com.google.caja.plugin.stages.JobCache;
import com.google.caja.util.ContentType;
import com.google.caja.util.Lists;

/**
 * JobCache that uses appengine's cache to hold intermediate cajoling results.

 * @author jasvir@gmail.com (Jasvir Nagra)
 */
final class AppEngineJobCache extends JobCache {
  private Cache l1cache;

  AppEngineJobCache() {
    try {
      this.l1cache = CacheManager.getInstance().getCacheFactory()
          .createCache(Collections.emptyMap());
    } catch (Exception e) {
      this.l1cache = null;
    }
  }

  @Override
  public AppEngineJobCacheKey forJob(ContentType type, ParseTreeNode node) {
    return new AppEngineJobCacheKey(type, node);
  }

  @SuppressWarnings("unchecked")
  @Override
  public List<? extends Job> fetch(JobCache.Key k) {
    if (null == l1cache) { return null; }
    if (!(k instanceof AppEngineJobCacheKey)) { return null; }
    List<Job> cachedJobs = (List<Job>) l1cache.get(k);
    if (cachedJobs == null) { return null; }
    if (cachedJobs.isEmpty()) {
      return cachedJobs;
    }
    return cloneJobs(cachedJobs);
  }

  @Override
  public void store(Key k, List<? extends Job> derivatives) {
    if (!(k instanceof AppEngineJobCacheKey)) {
      throw new IllegalArgumentException(k.getClass().getName());
    }
    if (null != l1cache) {
      l1cache.put(k, cloneJobs(derivatives));
    }
  }

  private static List<Job> cloneJobs(Iterable<? extends Job> jobs) {
    List<Job> clones = Lists.newArrayList();
    for (Job job : jobs) {
      clones.add(job.clone());
    }
    return clones;
  }
}
