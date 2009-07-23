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
import com.google.caja.util.Callback;

import java.io.IOException;
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
  private class WrapperRenderer implements TokenConsumer {
    private final Appendable out;
    private final Callback<IOException> exHandler;
    private final SourceSpansRenderer delegate;

    public WrapperRenderer(Appendable out, Callback<IOException> exHandler) {
      this.out = out;
      this.exHandler = exHandler;

      delegate = new SourceSpansRenderer(
          exHandler,
          new InputSource(URI.create("file://foo/bar.js")));
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
      try {
        out.append(delegate.getProgramText());
        for (String slmLine : delegate.getSourceLocationMap()) {
          out.append(slmLine).append("\n");
        }
      } catch (IOException e) {
        exHandler.handle(e);
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
      MessageContext mc, Appendable out, Callback<IOException> exHandler) {
    return new WrapperRenderer(out, exHandler);
  }
}
