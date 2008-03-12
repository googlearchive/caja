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

import com.google.caja.lexer.ExternalReference;
import com.google.caja.plugin.Config;
import com.google.caja.reporting.BuildInfo;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.SimpleMessageQueue;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * @author benl@google.com (Ben Laurie)
 */
public class GadgetRewriterMain {
  private Config config;

  private GadgetRewriterMain() {
    config = new Config(
        getClass(), System.err, "Cajole an OpenSocial gadget spec's Content");
  }

  public static void main(String[] argv) throws GadgetRewriteException,
      IOException, UriCallbackException, URISyntaxException {
    System.exit(new GadgetRewriterMain().run(argv));
  }

  class Callback implements UriCallback {
    public UriCallbackOption getOption(ExternalReference extref,
                                       String mimeType) {
      return UriCallbackOption.RETRIEVE;
    }

    public Reader retrieve(final ExternalReference extref,
                           final String mimeType) throws UriCallbackException {
      System.err.println("Retrieving " + extref);
      InputStream content;
      try {
        content = (InputStream)extref.getUri().toURL().getContent();
      } catch (IOException e) {
        throw new UriCallbackException(extref, e);
      }
      return new InputStreamReader(content);
    }

    public URI rewrite(ExternalReference extref, String mimeType) {
      return extref.getUri();
    }
  }
  
  private int run(String[] argv)
      throws UriCallbackException, GadgetRewriteException, IOException {
    if (!config.processArguments(argv)) {
      return -1;
    }

    MessageQueue mq = new SimpleMessageQueue();
    MessageContext mc = new MessageContext();
    DefaultGadgetRewriter rewriter = new DefaultGadgetRewriter(mq);
    rewriter.setCssSchema(config.getCssSchema(mq));
    rewriter.setHtmlSchema(config.getHtmlSchema(mq));

    Writer w = new BufferedWriter(new FileWriter(config.getOutputBase()));
    try {
      Callback cb = new Callback();
      URI uri = config.getBaseUri();
      Reader r = cb.retrieve(new ExternalReference(uri, null), null);
      try {
        rewriter.rewrite(uri, r, cb, config.getGadgetView(), w);
      } finally {
        r.close();
      }
    } finally {
      w.close();
    }
    
    return 0;
  }
}
