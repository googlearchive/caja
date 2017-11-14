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

package com.google.caja.plugin;

import com.google.caja.plugin.stages.JobCache;
import com.google.caja.util.ContentType;

/**
 * A wrapper around a {@link Job job} that includes info about how it should be
 * processed, cached, and recombined with other jobs
 *
 * @author Mike Samuel <mikesamuel@gmail.com>
 */
public final class JobEnvelope {
  /** Identifies from where the job was extracted.  May be {@code null}. */
  public String placeholderId;
  /** A hash of the content from which the job was derived. */
  public JobCache.Keys cacheKeys;
  /** The type of content from which the job was derived. */
  public final ContentType sourceType;
  /**
   * True iff the content comes from a cache, and so should not be processed by
   * stages between the pipeline fetch and pipeline store stages.
   */
  public final boolean fromCache;

  public final Job job;

  /**
   * @param placeholderId May be {@code null}.
   */
  public JobEnvelope(
      String placeholderId, JobCache.Keys cacheKeys, ContentType sourceType,
      boolean fromCache, Job job) {
    this.placeholderId = placeholderId;
    this.cacheKeys = cacheKeys;
    this.sourceType = sourceType;
    this.fromCache = fromCache;
    this.job = job;
  }

  public static JobEnvelope of(Job job) {
    return new JobEnvelope(null, JobCache.none(), job.getType(), false, job);
  }

  public JobEnvelope withJob(Job job) {
    return new JobEnvelope(
        placeholderId, cacheKeys, sourceType, fromCache, job);
  }

  /** A debugging string. */
  @Override
  public String toString() {
    if (this.sourceType != job.getType() || this.placeholderId != null
        || this.fromCache) {
      StringBuilder sb = new StringBuilder();
      sb.append("(JobEnvelope");
      if (this.sourceType != job.getType()) {
        sb.append(' ').append(sourceType);
      }
      if (this.placeholderId != null) {
        sb.append(" phid=").append(placeholderId);
      }
      if (this.fromCache) {
        sb.append(" cached");
      }
      return sb.append(' ').append(job).append(')').toString();
    } else {
      return job.toString();
    }
  }

  public boolean areFromSameSource(JobEnvelope that) {
    return that != null && this.cacheKeys.equals(that.cacheKeys)
        && (this.placeholderId != null
            ? this.placeholderId.equals(that.placeholderId)
            : that.placeholderId == null);
  }
}
