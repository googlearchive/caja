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

package com.google.caja.plugin;

import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.util.ContentType;
import com.google.common.collect.Lists;

import java.util.EnumSet;
import java.util.List;

/**
 * A group of parse trees.  Rewriting starts with one or more input parse trees,
 * and operates on those parse trees by either extracting bits, rewriting, &|
 * combining parse trees.
 *
 * @author mikesamuel@gmail.com
 */
public class Jobs {
  private MessageContext mc;
  private final MessageQueue mq;
  private final PluginMeta meta;
  private final List<JobEnvelope> jobs = Lists.newArrayList();

  public Jobs(MessageContext mc, MessageQueue mq, PluginMeta meta) {
    if (mc == null) { throw new NullPointerException(); }
    if (mq == null) { throw new NullPointerException(); }
    if (meta == null) { throw new NullPointerException(); }
    this.mc = mc;
    this.mq = mq;
    this.meta = meta;
  }

  public void setMessageContext(MessageContext newMc) { this.mc = newMc; }

  public MessageContext getMessageContext() { return mc; }

  public MessageQueue getMessageQueue() { return mq; }

  public PluginMeta getPluginMeta() { return meta; }

  /** May be mutated in place. */
  public List<JobEnvelope> getJobs() { return jobs; }

  public List<JobEnvelope> getJobsByType(
      ContentType type, ContentType... others) {
    List<JobEnvelope> matches = Lists.newArrayList();
    EnumSet<ContentType> types = EnumSet.of(type, others);
    for (JobEnvelope env : jobs) {
      if (types.contains(env.job.getType())) { matches.add(env); }
    }
    return matches;
  }

  public boolean hasNoFatalErrors() {
    return hasNoMessagesOfLevel(MessageLevel.FATAL_ERROR);
  }

  public boolean hasNoErrors() {
    return hasNoMessagesOfLevel(MessageLevel.ERROR);
  }

  public boolean hasNoMessagesOfLevel(MessageLevel level) {
    for (Message m : getMessageQueue().getMessages()) {
      if (level.compareTo(m.getMessageLevel()) <= 0) {
        return false;
      }
    }
    return true;
  }

  @Override
  public String toString() {
    return jobs.toString();  // For debugging.
  }
}
