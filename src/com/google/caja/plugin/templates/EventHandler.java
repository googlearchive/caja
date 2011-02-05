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

import com.google.caja.parser.js.Statement;
import com.google.caja.plugin.JobEnvelope;

/**
 * A bit of unsanitized JS that was extracted from an inline HTML event handler
 * or side effecting URL.
 * E.g. the value of an HTML <code>onclick</code> attribute.
 *
 * @author Mike Samuel <mikesamuel@gmail.com>
 */
public final class EventHandler {
  public final JobEnvelope source;
  public final Statement handler;

  public EventHandler(JobEnvelope source, Statement handler) {
    this.source = source;
    this.handler = handler;
  }
}
