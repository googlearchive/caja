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

package com.google.caja.opensocial;

import com.google.caja.lexer.ExternalReference;
import com.google.caja.lexer.InputSource;
import com.google.caja.reporting.MessageContext;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.util.Map;

public class Callback implements UriCallback {
  private MessageContext mc;
  private Map<InputSource, CharSequence> originalSources;

  public Callback(
      MessageContext mc, Map<InputSource, CharSequence> originalSources) {
    this.mc = mc;
    this.originalSources = originalSources;
  }

  public Reader retrieve(ExternalReference extref, String mimeType)
      throws UriCallbackException {
    final Reader in;
    URI uri;
    try {
      uri = extref.getUri();
      in = new InputStreamReader(uri.toURL().openStream(), "UTF-8");
    } catch (IOException e) {
      throw new UriCallbackException(extref, e);
    }

    final StringBuilder originalSource = new StringBuilder();
    InputSource is = new InputSource(uri);
    originalSources.put(is, originalSource);
    mc.addInputSource(is);

    // Tee the content out to a buffer so that we can keep track of the
    // original content so we can show error message snippets later.
    return new Reader() {
        @Override
        public void close() throws IOException { in.close(); }
        @Override
        public int read(char[] cbuf, int off, int len) throws IOException {
          int n = in.read(cbuf, off, len);
          if (n > 0) { originalSource.append(cbuf, off, n); }
          return n;
        }
        @Override
        public int read() throws IOException {
          int ch = in.read();
          if (ch >= 0) { originalSource.append((char) ch); }
          return ch;
        }
      };
  }

  public URI rewrite(ExternalReference extref, String mimeType) {
    return extref.getUri();
  }
}
