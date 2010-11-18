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

package com.google.caja.render;

import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.TokenConsumer;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.RenderContext;

import java.net.URI;
import java.util.Map;

/**
 * @author ihab.awad@gmail.com
 */
public class SourceSpansRendererTest extends OrigSourceRendererTestCase {
  // The SourceSpansRenderer does not *just* output text; it produces a
  // bunch of file position mapping data structures as well. We wrap it here
  // with something that generates a single piece of plain text out of the lot,
  // so we can test it using the superclass's framework.
  private static final class WrapperRenderer implements TokenConsumer {
    private final RenderContext rc;
    private final SourceSpansRenderer delegate;

    public WrapperRenderer(RenderContext rc) {
      this.rc = rc;
      delegate = new SourceSpansRenderer(
          new InputSource(URI.create("file://foo/bar.js")), rc);
    }

    public void mark(FilePosition pos) {
      delegate.mark(pos);
    }

    public void consume(String text) {
      delegate.consume(text);
    }

    public void noMoreTokens() {
      delegate.noMoreTokens();
      dumpResults();
    }

    private void dumpResults() {
      Concatenator out = (Concatenator) rc.getOut();
      out.consume(delegate.getProgramText());
      for (String slmLine : delegate.getSourceLocationMap()) {
        out.consume(slmLine);
        out.consume("\n");
      }
    }
  }

  public final void testRendering() throws Exception {
    runTest(
        "ssp-golden.txt", "ssp-rewritten-tokens.txt",
        "ssp-test-input.js");
  }

  @Override
  protected TokenConsumer createRenderer(
      Map<InputSource, ? extends CharSequence> originalSource,
      MessageContext mc, RenderContext rc) {
    return new WrapperRenderer(rc);
  }
}
