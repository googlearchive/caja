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

import com.google.caja.parser.ParseTreeNode;
import com.google.caja.plugin.Job;
import com.google.caja.util.ContentType;
import com.google.caja.util.Lists;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class StubJobCache extends JobCache {
  @Override
  public List<? extends Job> fetch(JobCache.Key k) { return null; }

  @Override
  public Key forJob(ContentType type, ParseTreeNode node) {
    return new TrivialJobCacheKey(node.getFilePosition().toString());
  }

  @Override
  public void store(JobCache.Key k, List<? extends Job> derivatives) {
    // Do nothing
  }

  private static final class TrivialJobCacheKey implements JobCache.Key {
    private final String pos;

    public TrivialJobCacheKey(String pos) { this.pos = pos; }

    @Override
    public JobCache.Keys asSingleton() {
      return new TrivialJobCacheKeys(
          Collections.<JobCache.Key>singletonList(this));
    }

    @Override public String toString() { return "[JobCacheKey @ " + pos + "]"; }
  }

  private static final class TrivialJobCacheKeys implements JobCache.Keys {
    private final List<JobCache.Key> keys;

    TrivialJobCacheKeys(List<JobCache.Key> keys) { this.keys = keys; }

    @Override
    public JobCache.Keys union(JobCache.Keys other) {
      if (!other.iterator().hasNext()) { return this; }
      List<JobCache.Key> keys = Lists.newArrayList();
      keys.addAll(this.keys);
      keys.addAll(((TrivialJobCacheKeys) other).keys);
      return new TrivialJobCacheKeys(Collections.unmodifiableList(keys));
    }

    @Override public Iterator<JobCache.Key> iterator() {
      return keys.iterator();
    }

    @Override public String toString() { return keys.toString(); }
  }
}
