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
import com.google.caja.lexer.InputSource;
import com.google.caja.parser.AncestorChain;
import com.google.caja.plugin.Dom;
import com.google.caja.plugin.Job;

import org.w3c.dom.Node;

public final class HtmlToBundleStage extends CompileHtmlStage {
  public HtmlToBundleStage(CssSchema cssSchema, HtmlSchema htmlSchema) {
    super(cssSchema, htmlSchema);
  }

  @Override
  Job makeJobFromHtml(Node html) {
    return Job.domJob(
        AncestorChain.instance(new Dom(html)), InputSource.UNKNOWN.getUri());
  }
}
