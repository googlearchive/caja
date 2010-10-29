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
import com.google.caja.util.SyntheticAttributeKey;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Caches the result of expensive pipeline stages.
 *
 * @author mikesamuel@gmail.com
 */
public abstract class JobCache {
  /** Given the parts of a {@link Job}, generates a cache key for that job. */
  public abstract Key forJob(ContentType type, ParseTreeNode node);
  /**
   * @return null to indicate a cache miss, as distinct from the empty list.
   */
  public abstract List<? extends Job> fetch(Key k);
  public abstract void store(Key k, List<? extends Job> derivatives);

  public interface Key {
    /** A {@link Keys} instance whose iterator produces only {@code this}. */
    public Keys asSingleton();
    public boolean equals(Object o);
    public int hashCode();
  }

  public interface Keys extends Iterable<Key> {
    /**
     * An instance that iterates over all the keys in {@code this} and all the
     * keys in other.
     * <p>
     * Implementation note: implementations may elect to raise a runtime
     * exception if other was not produced by the same {@code JobCache}
     * instance <b>and</b> other is not {@link JobCache#none none} but must
     * support none by returning {@code this} or an equal instance.
     */
    Keys union(Keys other);
    public boolean equals(Object o);
    public int hashCode();
  }

  /** A nullish instance such that {@link Keys#iterator} is empty. */
  public static Keys none() {
    return NONE;
  }

  private static final Keys NONE = new Keys() {
      public Iterator<Key> iterator() {
        return Collections.<Key>emptySet().iterator();
      }
      public Keys union(Keys other) { return other; }
      public @Override String toString() { return "(no-keys)"; }
    };

  public static final SyntheticAttributeKey<Boolean> NO_CACHE
      = new SyntheticAttributeKey<Boolean>(Boolean.class, "noCache");
}
