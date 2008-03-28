// Copyright 2007 Google Inc. All Rights Reserved.
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

import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.ExternalReference;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.ParseException;
import com.google.caja.plugin.Config;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.SimpleMessageQueue;
import com.google.caja.reporting.SnippetProducer;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * @author benl@google.com (Ben Laurie)
 */
public class GadgetRewriterMain {
  private Config config;
  private MessageContext mc = new MessageContext();
  private Map<InputSource, CharSequence> originalSources
      = new HashMap<InputSource, CharSequence>();

  private GadgetRewriterMain() {
    config = new Config(
        getClass(), System.err, "Cajole an OpenSocial gadget spec's Content");
    mc.inputSources = new ArrayList<InputSource>();
  }

  public static void main(String[] argv)
      throws GadgetRewriteException, IOException, ParseException,
          UriCallbackException {
    System.exit(new GadgetRewriterMain().run(argv));
  }

  class Callback implements UriCallback {
    public UriCallbackOption getOption(ExternalReference extref,
                                       String mimeType) {
      return UriCallbackOption.RETRIEVE;
    }

    public Reader retrieve(ExternalReference extref, String mimeType)
        throws UriCallbackException {
      System.err.println("Retrieving " + extref);
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
  
  private int run(String[] argv)
      throws GadgetRewriteException, IOException, ParseException,
          UriCallbackException {
    if (!config.processArguments(argv)) {
      return -1;
    }

    MessageQueue mq = new SimpleMessageQueue();
    DefaultGadgetRewriter rewriter = new DefaultGadgetRewriter(mq);
    rewriter.setCssSchema(config.getCssSchema(mq));
    rewriter.setHtmlSchema(config.getHtmlSchema(mq));

    Writer w = new BufferedWriter(new FileWriter(config.getOutputBase()));
    try {
      Callback cb = new Callback();
      URI baseUri = config.getBaseUri();
      for (URI input : config.getInputUris()) {
        Reader r = cb.retrieve(new ExternalReference(input, null), null);
        CharProducer p = CharProducer.Factory.create(r, new InputSource(input));
        try {
          rewriter.rewrite(baseUri, p, cb, config.getGadgetView(), w);
        } finally {
          SnippetProducer sp = new SnippetProducer(originalSources, mc);
          for (Message msg : mq.getMessages()) {
            System.err.println(
                msg.getMessageLevel().name() + ": " + msg.format(mc));
            System.err.println(sp.getSnippet(msg));
            System.err.println();
          }
          r.close();
        }
      }
    } finally {
      w.close();
    }
    
    return 0;
  }
}
