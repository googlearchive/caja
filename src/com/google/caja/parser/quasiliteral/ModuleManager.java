// Copyright (C) 2009 Google Inc.
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

package com.google.caja.parser.quasiliteral;

import com.google.caja.plugin.PluginMeta;
import com.google.caja.plugin.UriFetcher;
import com.google.caja.reporting.BuildInfo;
import com.google.caja.reporting.MessageQueue;

// TODO(kpreid): Review whether this may be deleted
/**
 * This used to fetch and hold modules for the load() statement implemented
 * in the cajoler.  That's gone now.
 * <p>
 * This class still exists because it bundles together a number of parameters
 * needed at various stages of the cajoler pipeline, and it's nontrivial to
 * fix all the public APIs to eliminate this class.
 *
 * @author maoziqing@gmail.com
 */
public class ModuleManager {
  private final PluginMeta meta;
  private final BuildInfo buildInfo;
  private final UriFetcher uriFetcher;
  private final MessageQueue mq;

  public ModuleManager(
      PluginMeta meta, BuildInfo buildInfo, UriFetcher uriFetcher,
      MessageQueue mq) {
    assert uriFetcher != null;
    this.meta = meta;
    this.buildInfo = buildInfo;
    this.uriFetcher = uriFetcher;
    this.mq = mq;
  }

  public PluginMeta getPluginMeta() { return meta; }

  public BuildInfo getBuildInfo() { return buildInfo; }

  public UriFetcher getUriFetcher() { return uriFetcher; }

  public MessageQueue getMessageQueue() { return mq; }
}
