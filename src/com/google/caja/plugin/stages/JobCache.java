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

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Caches the result of expensive pipeline stages.
 *
 * @author mikesamuel@gmail.com
 */
public abstract class JobCache {
  public abstract Key forJob(Job j);
  /**
   * @return null to indicate nothing in cache which is distinct from the empty
   *     list.
   */
  public abstract List<Job> fetch(Key k);
  public abstract void store(Key k, List<Job> derivatives);

  public interface Key {
    public Keys asSingleton();
  }

  public interface Keys extends Iterable<Key> {
    Keys union(Keys other);
  }

  public static Keys none() {
    return new Keys() {
      public Iterator<Key> iterator() {
        return Collections.<Key>emptySet().iterator();
      }
      public Keys union(Keys other) { return other; }
    };
  }
}
