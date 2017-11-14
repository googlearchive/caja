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

import java.net.URI;

import com.google.caja.parser.css.CssTree.StyleSheet;
import com.google.caja.plugin.JobEnvelope;

/**
 * A chunk of CSS that has had a schema applied and had unsafe rules rewritten.
 *
 * @author Mike Samuel <mikesamuel@gmail.com>
 */
public class ValidatedStylesheet {
  public final JobEnvelope source;
  public final StyleSheet ss;
  public final URI baseUri;

  public ValidatedStylesheet(JobEnvelope source, StyleSheet ss, URI baseUri) {
    this.source = source;
    this.ss = ss;
    this.baseUri = baseUri;
  }
}
