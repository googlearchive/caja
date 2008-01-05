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
import java.util.List;
import java.util.ArrayList;

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
  private final List<Job> jobs = new ArrayList<Job>();

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
  public List<Job> getJobs() { return jobs; }

  public List<Job> getJobsByType(Job.JobType type) {
    List<Job> matches = new ArrayList<Job>();
    for (Job job : jobs) {
      if (job.getType() == type) { matches.add(job); }
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
}
