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
 * An input to the template compiler that contains references to extracted
 * content.
 *
 * @author Mike Samuel <mikesamuel@gmail.com>
 */
public final class IhtmlRoot {
  public final JobEnvelope source;
  public final Node root;
  public final URI baseUri;

  public IhtmlRoot(JobEnvelope source, Node root, URI baseUri) {
    this.source = source;
    this.root = root;
    this.baseUri = baseUri;
  }
}
