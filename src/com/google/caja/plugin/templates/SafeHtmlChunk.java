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

import com.google.caja.plugin.JobEnvelope;
import java.net.URI;
import org.w3c.dom.Node;

/**
 * A chunk of sanitized HTML with routing info.
 *
 * @author Mike Samuel <mikesamuel@gmail.com>
 */
public final class SafeHtmlChunk {
  /** The routing info for this HTML. */
  public final JobEnvelope source;
  public final Node root;
  /** The URI against which relative URIs under root are resolved. */
  public final URI baseUri;

  SafeHtmlChunk(JobEnvelope source, Node root, URI baseUri) {
    this.source = source;
    this.root = root;
    this.baseUri = baseUri;
  }
}
