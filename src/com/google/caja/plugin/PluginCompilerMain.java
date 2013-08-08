// Copyright (C) 2006 Google Inc.
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

package com.google.caja.plugin;

import com.google.caja.SomethingWidgyHappenedError;
import com.google.caja.lexer.ExternalReference;
import com.google.caja.lexer.FetchedData;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.ParseException;
import com.google.caja.lexer.TokenConsumer;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.ParserContext;
import com.google.caja.parser.html.Nodes;
import com.google.caja.parser.js.CajoledModule;
import com.google.caja.plugin.UriFetcher.ChainingUriFetcher;
import com.google.caja.render.Concatenator;
import com.google.caja.render.JsMinimalPrinter;
import com.google.caja.reporting.BuildInfo;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.MessageType;
import com.google.caja.reporting.RenderContext;
import com.google.caja.reporting.SimpleMessageQueue;
import com.google.caja.util.Callback;
import com.google.caja.util.Charsets;
import com.google.caja.util.Maps;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.w3c.dom.Node;

/**
 * An executable that invokes the {@link PluginCompiler}.
 *
 * @author mikesamuel@gmail.com
 */
public final class PluginCompilerMain {
  private final MessageQueue mq;
  private final MessageContext mc;
  private final Map<InputSource, CharSequence> originalSources
      = Maps.newHashMap();
  private final Config config = new Config(
      getClass(), System.err, "Cajoles HTML, CSS, and JS files to JS.");
  private final Callback<IOException> exHandler = new Callback<IOException>() {
    public void handle(IOException ex) {
      mq.addMessage(
          MessageType.IO_ERROR, MessagePart.Factory.valueOf(ex.toString()));
    }
  };

  private class CachingUriFetcher extends FileSystemUriFetcher {
    public CachingUriFetcher(UriToFile u2f) { super(u2f); }

    @Override
    protected Reader newReader(File f) throws FileNotFoundException {
      return new InputStreamReader(new FileInputStream(f), Charsets.UTF_8);
    }

    @Override
    protected InputStream newInputStream(File f) throws FileNotFoundException {
      return new FileInputStream(f);
    }
  }

  private PluginCompilerMain() {
    mq = new SimpleMessageQueue();
    mc = new MessageContext();
  }

  private int run(String[] argv) {
    if (!config.processArguments(argv)) {
      return -1;
    }
    if (config.getBenchmark() == 0) {
      return runOnce(true);
    } else {
      runBench(argv, false);
      runBench(argv, true);
      pause();
      return 0;
    }
  }

  private int runOnce(boolean writeFiles) {
    boolean success = false;
    MessageContext mc = null;
    CajoledModule compiledJsOutput = null;
    Node compiledDomOutput = null;
    String compiledHtmlOutput = null;
    File fileLimitAncestor = config.getFetcherBase();

    File jsOutputDest = config.getOutputJsFile();
    File htmlOutputDest = config.getOutputHtmlFile();

    try {
      UriFetcher fetcher;
      UriPolicy policy;
      try {
        if (fileLimitAncestor != null) {
          UriToFile u2f = new UriToFile(fileLimitAncestor);
          fetcher = ChainingUriFetcher.make(
              new DataUriFetcher(),
              new CachingUriFetcher(u2f));
          policy = new FileSystemUriPolicy(u2f);
        } else {
          fetcher = new DataUriFetcher();
          policy = UriPolicy.DENY_ALL;
        }
      } catch (IOException e) {  // Could not resolve file name
        fetcher = new DataUriFetcher();
        policy = UriPolicy.DENY_ALL;
      }
      final Set<String> lUrls = config.getLinkableUris();
      if (!lUrls.isEmpty()) {
        final UriPolicy prePolicy = policy;
        policy = new UriPolicy() {
          public String rewriteUri(
              ExternalReference u, UriEffect effect,
              LoaderType loader, Map<String, ?> hints) {
            String uri = u.getUri().toString();
            if (lUrls.contains(uri)) { return uri; }
            return prePolicy.rewriteUri(u, effect, loader, hints);
          }
        };
      }
      final Set<String> fUris = config.getFetchableUris();
      final boolean fUriAll = config.hasFetchableUriAll();
      if (fUriAll || !fUris.isEmpty()) {
        fetcher = ChainingUriFetcher.make(
            fetcher,
            new UriFetcher() {
              public FetchedData fetch(ExternalReference ref, String mimeType)
                  throws UriFetchException {
                String uri = ref.getUri().toString();
                if (!fUriAll && !fUris.contains(uri)) {
                  throw new UriFetchException(ref, mimeType);
                }
                try {
                  return FetchedData.fromConnection(
                      new URL(uri).openConnection());
                } catch (IOException ex) {
                  throw new UriFetchException(ref, mimeType, ex);
                }
              }
            });
      }

      if (config.hasLinkableUriAll()) {
        policy = UriPolicy.IDENTITY;
      } else if (config.hasLinkableUriRuntime()) {
        policy = null;
      }

      PluginMeta meta = new PluginMeta(fetcher, policy);
      meta.setIdClass(config.getIdClass());
      meta.setPrecajoleMinify(
          config.renderer() == Config.SourceRenderMode.MINIFY);
      if (config.hasNoPrecajoled()) {
        meta.setPrecajoleMap(null);
      }

      PluginCompiler compiler = new PluginCompiler(
          BuildInfo.getInstance(), meta, mq);
      compiler.setPreconditions(
          config.preconditions(compiler.getPreconditions()));
      compiler.setGoals(config.goals(compiler.getGoals()));

      mc = compiler.getMessageContext();
      compiler.setCssSchema(config.getCssSchema(mq));
      compiler.setHtmlSchema(config.getHtmlSchema(mq));

      success = parseInputs(meta, config.getInputUris(), compiler)
          && compiler.run();
      if (success) {
        compiledJsOutput = compiler.getJavascript();
        compiledDomOutput = compiler.getStaticHtml();
        compiledHtmlOutput = compiledDomOutput != null ?
            Nodes.render(compiledDomOutput) : "";
      }
    } finally {
      if (mc == null) { mc = new MessageContext(); }
      MessageLevel maxMessageLevel = dumpMessages(mq, mc, System.err);
      success &= MessageLevel.ERROR.compareTo(maxMessageLevel) > 0;
    }

    if (!writeFiles) {
      return success ? 0 : -1;
    }

    if (success) {
      if (jsOutputDest != null) {
        writeFile(jsOutputDest, compiledJsOutput);
      } else {
        StringBuilder compiledJsOutputBuf = new StringBuilder();
        compiledJsOutputBuf.append("<script>");
        try {
          writeFile(compiledJsOutputBuf, compiledJsOutput);
        } catch (IOException ex) {
          throw new SomethingWidgyHappenedError(ex);
        }
        compiledJsOutputBuf.append("</script>");
        compiledHtmlOutput += compiledJsOutputBuf;
      }
      if (htmlOutputDest != null) {
        writeFile(htmlOutputDest, compiledHtmlOutput);
      }
    } else {
      // Make sure there is no previous output file from a failed run.
      if (jsOutputDest != null) { jsOutputDest.delete(); }
      if (htmlOutputDest != null) { htmlOutputDest.delete(); }
      // If it wasn't there in the first place, or is not writable, that's OK,
      // so ignore the return value.
    }

    return success ? 0 : -1;
  }

  private boolean parseInputs(PluginMeta meta, Collection<URI> inputs,
      PluginCompiler pluginc) {
    boolean parsePassed = true;
    for (URI input : inputs) {
      try {
        ParseTreeNode parseTree = new ParserContext(mq)
            .withInput(new InputSource(input))
            .withConfig(meta)
            .withConfig(mc)
            .withSourceMap(originalSources)
            .build();
        if (null != parseTree) { pluginc.addInput(parseTree, input); }
      } catch (ParseException ex) {
        ex.toMessageQueue(mq);
        parsePassed = false;
      } catch (IOException ex) {
        mq.addMessage(MessageType.IO_ERROR,
                      MessagePart.Factory.valueOf(ex.toString()));
        parsePassed = false;
      }
    }
    return parsePassed;
  }

  /** Write the given HTML to the given file. */
  private void writeFile(File outputHtmlFile, String compiledHtmlOutput) {
    try {
      OutputStreamWriter out = new OutputStreamWriter(
            new FileOutputStream(outputHtmlFile), Charsets.UTF_8);
      try {
        out.append(compiledHtmlOutput);
      } finally {
        try { out.close(); } catch (IOException e) { /* close quietly */ }
      }
    } catch (IOException ex) {
      exHandler.handle(ex);
    }
  }

  /** Write the given parse tree to the given file. */
  private void writeFile(File f, CajoledModule module) {
    if (module == null) { return; }

    Writer out = null;

    try {
      out = new OutputStreamWriter(new FileOutputStream(f), Charsets.UTF_8);
      writeFile(out, module);
    } catch (IOException ex) {
      ex.printStackTrace();
    } finally {
      if (out != null) {
        try {
          out.close();
        } catch (IOException e) {
          /* no zero-argument ctor */
        }
      }
    }
  }

  private void writeFile(Appendable out, CajoledModule module)
      throws IOException {
    TokenConsumer tc;
    switch (config.renderer()) {
      case PRETTY:
        tc = module.makeRenderer(out, exHandler);
        break;
      case MINIFY:
        tc = new JsMinimalPrinter(new Concatenator(out,  exHandler));
        break;
      default:
        throw new SomethingWidgyHappenedError(
            "Unrecognized renderer: " + config.renderer());
    }
    RenderContext rc = new RenderContext(tc);
    module.render(rc);
    tc.noMoreTokens();
    out.append('\n');
  }

  /**
   * Dumps messages to the given output stream, returning the highest message
   * level seen.
   */
  static MessageLevel dumpMessages(
      MessageQueue mq, MessageContext mc, Appendable out) {
    MessageLevel maxLevel = MessageLevel.values()[0];
    for (Message m : mq.getMessages()) {
      MessageLevel level = m.getMessageLevel();
      if (maxLevel.compareTo(level) < 0) { maxLevel = level; }
    }
    MessageLevel ignoreLevel = null;
    if (maxLevel.compareTo(MessageLevel.LINT) < 0) {
      // If there's only checkpoints, be quiet.
      ignoreLevel = MessageLevel.LOG;
    }
    try {
      for (Message m : mq.getMessages()) {
        MessageLevel level = m.getMessageLevel();
        if (ignoreLevel != null && level.compareTo(ignoreLevel) <= 0) {
          continue;
        }
        String levelName = level.name();
        out.append(levelName);
        if (levelName.length() < 7) {
          out.append("       ".substring(levelName.length()));
        }
        out.append(": ");
        m.format(mc, out);
        out.append("\n");

        if (maxLevel.compareTo(level) < 0) { maxLevel = level; }
      }
    } catch (IOException ex) {
      ex.printStackTrace();
    }
    return maxLevel;
  }

  public static void main(String[] args) {
    int exitCode;
    try {
      PluginCompilerMain main = new PluginCompilerMain();
      exitCode = main.run(args);
    } catch (Exception ex) {
      ex.printStackTrace();
      exitCode = -1;
    }
    try {
      System.exit(exitCode);
    } catch (SecurityException ex) {
      // This method may be invoked under a SecurityManager, e.g. by Ant,
      // so just suppress the security exception and return normally.
    }
  }

  private static void pause() {
    try {
      BufferedReader b = new BufferedReader(new InputStreamReader(System.in));
      System.out.println("press return...");
      b.readLine();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void runBench(String[] argv, boolean warm) {
    pause();

    int trials = Math.max(config.getBenchmark(), 1);
    long first_time = 0;
    long all_times = 0;

    for (int i = 0; i < trials; i++) {
      long t0 = System.nanoTime();
      runBenchOnce(argv);
      long dt = System.nanoTime() - t0;
      all_times += dt;
      if (i == 0) {
        first_time = dt;
      }
    }

    DecimalFormat fmt = new DecimalFormat("#.## msec");
    if (!warm) {
      System.out.println(
          fmt.format(first_time / 1e6) + ": first run");
    }
    System.out.println(
        fmt.format(all_times / (trials * 1e6)) + ": all runs");
  }

  private static void runBenchOnce(String[] argv) {
    // We create a new PluginCompilerMain because it's not re-usable.
    PluginCompilerMain pc = new PluginCompilerMain();
    pc.config.processArguments(argv);
    pc.runOnce(false);
  }
}
