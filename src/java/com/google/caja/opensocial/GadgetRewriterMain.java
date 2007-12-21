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
import com.google.caja.plugin.PluginMeta;
import com.google.caja.plugin.PluginMeta.TranslationScheme;
import com.google.caja.reporting.EchoingMessageQueue;
import com.google.caja.reporting.MessageContext;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

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
 *
 */
public class GadgetRewriterMain {
  private static final Option INPUT =
    new Option("i", "input", true, "Input Gadget URL");
  private static final Option OUTPUT =
    new Option("o", "output", true, "Output Gadget File");
  private static final Option TIME =
    new Option("t", "time", true, "Repeat n times and print timing info");
  private static final Option SCHEME =
    new Option("s", "scheme", true, "Rewriting rules: Aaja, Baja or Caja?");
  
  private String gadgetUrl;
  private String outputFile;
  private int repeatCount;
  private TranslationScheme scheme;

  private static final Options options = new Options();
  
  static {
    options.addOption(INPUT);
    options.addOption(OUTPUT);
    options.addOption(TIME);
  }
  
  private GadgetRewriterMain() {
    repeatCount = 0;
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
      System.out.println("Retrieving " + extref);
      InputStream content;
      try {
        content = (InputStream)extref.getUri().toURL().getContent();
      } catch (IOException e) {
        throw new UriCallbackException(extref, e);
      }
      return new InputStreamReader(content);
    }

    public URI rewrite(ExternalReference extref, String mimeType) {
      throw new UnsupportedOperationException();
    }
  }
  
  private int run(String[] argv) throws UriCallbackException,
      GadgetRewriteException, IOException, URISyntaxException {
    int rc = processArguments(argv);
    if (rc != 0) return rc;

    if (repeatCount == 0) {
      runOnce();
    } else {
      long now = System.currentTimeMillis();
      for (int n = 0; n < repeatCount; ++n)
        runOnce();
      System.out.println("Average time per rewrite "
              + (System.currentTimeMillis() - now)/(float)repeatCount + "ms");
    }
    return 0;
  }

  private void runOnce() throws IOException, UriCallbackException,
      GadgetRewriteException, MalformedURLException, URISyntaxException {
    DefaultGadgetRewriter rewriter =
      new DefaultGadgetRewriter(new EchoingMessageQueue(
          new PrintWriter(System.err), new MessageContext(), false));
    rewriter.setTranslationScheme(scheme);
    Writer w = new BufferedWriter(new FileWriter(outputFile));
    Callback cb = new Callback();
    URI uri = new URI(gadgetUrl);
    Reader r = cb.retrieve(new ExternalReference(uri, null), null);
    rewriter.rewrite(uri, r, cb, w);
    w.flush();
    w.close();
  }

  private int processArguments(String[] argv) {
    CommandLine cl;
    try {
      cl = new BasicParser().parse(options, argv);
    } catch (org.apache.commons.cli.ParseException e) {
      throw new RuntimeException(e);
    }

    gadgetUrl = cl.getOptionValue(INPUT.getOpt());
    if (gadgetUrl == null)
      return usage("Option \"" + INPUT.getLongOpt() + "\" missing");
    
    outputFile = cl.getOptionValue(OUTPUT.getOpt());
    if (outputFile == null)
      return usage("Option \"" + OUTPUT.getLongOpt() + "\" missing");

    String t = cl.getOptionValue(TIME.getOpt());
    if (t != null)
      repeatCount = Integer.decode(t).intValue();
    
    scheme = PluginMeta.TranslationScheme.CAJA;
    t = cl.getOptionValue(SCHEME.getOpt());
    if (t.equalsIgnoreCase("aaja"))
      scheme = PluginMeta.TranslationScheme.AAJA;
    else if (t.equalsIgnoreCase("baja"))
      scheme = PluginMeta.TranslationScheme.BAJA;
    else if (t.equalsIgnoreCase("caja"))
      scheme = PluginMeta.TranslationScheme.CAJA;
    else
      return usage("Bad scheme: " + t);
    return 0;
  }
  
  private int usage(String msg) {
    System.out.println(msg);
    new HelpFormatter().printHelp(getClass().getName(), options);
    return -1;
  }
}
