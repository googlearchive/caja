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

import java.net.URI;

import org.w3c.dom.Element;

/**
 * A {@link ValidatedStylesheet} that has been converted to HTML or JS.
 *
 * @author Mike Samuel <mikesamuel@gmail.com>
 */
final class SafeStylesheet {
  public final JobEnvelope source;
  public final Element htmlVersion;
  public final Statement jsVersion;
  public final URI baseUri;

  SafeStylesheet(JobEnvelope source, Element htmlVersion, URI baseUri) {
    this.source = source;
    this.htmlVersion = htmlVersion;
    this.jsVersion = null;
    this.baseUri = baseUri;
  }

  SafeStylesheet(JobEnvelope source, Statement jsVersion, URI baseUri) {
    this.source = source;
    this.htmlVersion = null;
    this.jsVersion = jsVersion;
    this.baseUri = baseUri;
  }
}
