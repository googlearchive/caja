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
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
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

  public GadgetRewriterMain() {
    config = new Config(
        getClass(), System.err, "Cajole an OpenSocial gadget spec's Content");
  }

   public static void main(String[] argv)
       throws GadgetRewriteException, IOException, UriCallbackException,
          ParseException {
     GadgetRewriterMain grm = new GadgetRewriterMain();
     if (grm.init(argv)) {
       System.exit(grm.run());
     }
     System.exit(-1);
   }

  public boolean init(String[] argv) {
    return config.processArguments(argv);
  }

  public int run()
      throws GadgetRewriteException, IOException, UriCallbackException,
          ParseException {
    MessageQueue mq = new SimpleMessageQueue();
    DefaultGadgetRewriter rewriter = new DefaultGadgetRewriter(mq);
    rewriter.setCssSchema(config.getCssSchema(mq));
    rewriter.setHtmlSchema(config.getHtmlSchema(mq));
    rewriter.setDebugMode(config.debugMode());
    rewriter.setValijaMode(config.cajaMode());

    Writer w = new BufferedWriter(new FileWriter(config.getOutputBase()));
    try {
      Callback cb = new Callback(config, mc, originalSources);
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

  public Config getConfig() {
    return config;
  }
}
