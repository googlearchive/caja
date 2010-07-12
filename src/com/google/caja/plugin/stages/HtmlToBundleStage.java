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

package com.google.caja.plugin.stages;

import com.google.caja.lang.css.CssSchema;
import com.google.caja.lang.html.HtmlSchema;
import com.google.caja.parser.html.Dom;
import com.google.caja.plugin.Job;

import java.net.URI;

import org.w3c.dom.Node;

/**
 * Separates HTML into a fragment of safe, static HTML, and a block of dynamic
 * JavaScript.
 *
 * @author mikesamuel@gmail.com
 */
public final class HtmlToBundleStage extends CompileHtmlStage {
  public HtmlToBundleStage(CssSchema cssSchema, HtmlSchema htmlSchema) {
    super(cssSchema, htmlSchema);
  }

  @Override
  Job makeJobFromHtml(JobCache.Keys cacheKeys, Node html, URI baseUri) {
    return Job.domJob(cacheKeys, new Dom(html), baseUri);
  }
}
