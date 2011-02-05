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

package com.google.caja.plugin.templates;

import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.CajoledModule;
import com.google.caja.plugin.JobEnvelope;

/**
 * A chunk of script that can be processed to appear safely as part of active
 * HTML content.
 *
 * @author Mike Samuel <mikesamuel@gmail.com>
 */
public final class SafeJsChunk {
  public final JobEnvelope source;
  /**
   * Either a {@link Block} if further processing is required, or a
   * {@link CajoledModule} if the body has been fully processed i.e.&#32;if the
   * body came from the cache.
   */
  public final ParseTreeNode body;

  SafeJsChunk(JobEnvelope source, Block body) {
    this.source = source;
    this.body = body;
  }

  SafeJsChunk(JobEnvelope source, CajoledModule body) {
    this.source = source;
    this.body = body;
  }
}
