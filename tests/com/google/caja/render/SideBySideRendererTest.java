// Copyright (C) 2008 Google Inc.
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

package com.google.caja.render;

import com.google.caja.lexer.TokenConsumer;
import com.google.caja.lexer.InputSource;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.RenderContext;

import java.util.Map;

/**
 * @author ihab.awad@gmail.com
 */
public class SideBySideRendererTest extends OrigSourceRendererTestCase {
  public final void testRendering() throws Exception {
    runTest(
        "sbs-golden.js", "sbs-rewritten-tokens.txt",
        "sbs-test-input1.js", "sbs-test-input2.html", "sbs-test-input3.css");
  }

  @Override
  protected TokenConsumer createRenderer(
      Map<InputSource, ? extends CharSequence> originalSource,
      MessageContext mc, RenderContext rc) {
    return new TabularSideBySideRenderer(originalSource, mc, rc);
  }
}
