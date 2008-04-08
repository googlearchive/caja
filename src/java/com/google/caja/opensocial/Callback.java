package com.google.caja.opensocial;

import com.google.caja.lexer.ExternalReference;
import com.google.caja.lexer.InputSource;
import com.google.caja.plugin.Config;
import com.google.caja.reporting.MessageContext;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.util.Map;

public class Callback implements UriCallback {
  
  private Config config;
  private MessageContext mc;
  private Map<InputSource, CharSequence> originalSources;

  public Callback ( Config config, MessageContext mc,
                    Map<InputSource, CharSequence> originalSources ) {
    this.config = config;
    this.mc = mc;
    this.originalSources = originalSources;
  }
  
  public UriCallbackOption getOption(ExternalReference extref,
                                     String mimeType) {
    return UriCallbackOption.RETRIEVE;
  }

  public Reader retrieve(ExternalReference extref, String mimeType)
      throws UriCallbackException {
    final Reader in;
    URI uri;
    try {
      uri = config.getBaseUri().resolve(extref.getUri());
      in = new InputStreamReader(uri.toURL().openStream(), "UTF-8");
    } catch (IOException e) {
      throw new UriCallbackException(extref, e);
    }
    
    final StringBuilder originalSource = new StringBuilder();
    InputSource is = new InputSource(uri);
    originalSources.put(is, originalSource);
    mc.inputSources.add(is);

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
