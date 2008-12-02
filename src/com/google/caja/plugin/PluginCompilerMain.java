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

import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.CssLexer;
import com.google.caja.lexer.CssTokenType;
import com.google.caja.lexer.HtmlLexer;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.JsLexer;
import com.google.caja.lexer.JsTokenQueue;
import com.google.caja.lexer.ParseException;
import com.google.caja.lexer.Token;
import com.google.caja.lexer.TokenConsumer;
import com.google.caja.lexer.TokenQueue;
import com.google.caja.parser.AncestorChain;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.css.CssParser;
import com.google.caja.parser.html.DomParser;
import com.google.caja.parser.js.Parser;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.MessageType;
import com.google.caja.reporting.RenderContext;
import com.google.caja.reporting.SimpleMessageQueue;
import com.google.caja.util.Callback;
import com.google.caja.util.Criterion;
import com.google.caja.util.CapturingReader;
import com.google.caja.render.JsMinimalPrinter;
import com.google.caja.render.SourceSnippetRenderer;
import com.google.caja.render.JsPrettyPrinter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.io.Reader;
import java.io.FileNotFoundException;
import java.io.FileInputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.HashMap;

/**
 * An executable that invokes the {@link PluginCompiler}.
 *
 * @author mikesamuel@gmail.com
 */
public final class PluginCompilerMain {
  private final MessageQueue mq;
  private final MessageContext mc;
  private final Map<InputSource, CapturingReader> originalInputs
      = new HashMap<InputSource, CapturingReader>();
  private final Config config = new Config(
      getClass(), System.err,
      "Cajoles HTML, CSS, and JS files to JS.");

  private class CachingEnvironment extends FileSystemEnvironment {
    public CachingEnvironment(File f) { super(f); }

    @Override
    protected Reader newReader(File f) throws FileNotFoundException {
      return createReader(new InputSource(f), new FileInputStream(f));
    }
  }

  private PluginCompilerMain() {
    mq = new SimpleMessageQueue();
    mc = new MessageContext();
    mc.inputSources = new ArrayList<InputSource>();
  }

  private int run(String[] argv) {
    if (!config.processArguments(argv)) {
      return -1;
    }

    boolean success = false;
    MessageContext mc = null;
    ParseTreeNode compiledOutput = null;
    try {
      PluginMeta meta = new PluginMeta(makeEnvironment(config));
      meta.setDebugMode(config.debugMode());
      meta.setValijaMode(config.cajaMode());
      PluginCompiler compiler = new PluginCompiler(meta, mq);
      mc = compiler.getMessageContext();
      compiler.setCssSchema(config.getCssSchema(mq));
      compiler.setHtmlSchema(config.getHtmlSchema(mq));

      success = parseInputs(config.getInputUris(), compiler) && compiler.run();
      if (success) {
        compiledOutput = compiler.getJavascript();
      }
    } finally {
      if (mc == null) { mc = new MessageContext(); }
      MessageLevel maxMessageLevel = dumpMessages(mq, mc, System.err);
      success &= MessageLevel.ERROR.compareTo(maxMessageLevel) > 0;
    }

    if (success) {
      writeFile(config.getOutputJsFile(), compiledOutput);
    } else {
      // Make sure there is no previous output file from a failed run.
      config.getOutputJsFile().delete();
      // If it wasn't there in the first place, or is not writable, that's OK,
      // so ignore the return value.
    }

    return success ? 0 : -1;
  }

  private boolean parseInputs(Collection<URI> inputs, PluginCompiler pluginc) {
    boolean parsePassed = true;
    for (URI input : inputs) {
      try {
        ParseTreeNode parseTree = parseInput(input);
        if (null != parseTree) {
          pluginc.addInput(new AncestorChain<ParseTreeNode>(parseTree));
        }
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

  /** Parse one input from a URI. */
  private ParseTreeNode parseInput(URI input)
      throws IOException, ParseException {
    InputSource is = new InputSource(input);
    mc.inputSources.add(is);

    CharProducer cp = CharProducer.Factory.create(
        createReader(is, input.toURL().openStream()), is);
    try {
      return parseInput(is, cp, mq);
    } finally {
      cp.close();
    }
  }

  /** Classify an input by extension and use the appropriate parser. */
  static ParseTreeNode parseInput(
      InputSource is, CharProducer cp, MessageQueue mq)
      throws ParseException {

    String path = is.getUri().getPath();

    ParseTreeNode input;
    if (path.endsWith(".js")) {
      JsLexer lexer = new JsLexer(cp);
      JsTokenQueue tq = new JsTokenQueue(lexer, is);
      if (tq.isEmpty()) { return null; }
      Parser p = new Parser(tq, mq);
      input = p.parse();
      tq.expectEmpty();
    } else if (path.endsWith(".html") || path.endsWith(".xhtml")) {
      DomParser p = new DomParser(new HtmlLexer(cp), is, mq);
      if (p.getTokenQueue().isEmpty()) { return null; }
      input = p.parseFragment();
      p.getTokenQueue().expectEmpty();
    } else if (path.endsWith(".css")) {
      CssLexer lexer = new CssLexer(cp);
      TokenQueue<CssTokenType> tq = new TokenQueue<CssTokenType>(
          lexer, is, new Criterion<Token<CssTokenType>>() {
            public boolean accept(Token<CssTokenType> tok) {
              return tok.type != CssTokenType.COMMENT
                  && tok.type != CssTokenType.SPACE;
            }
          });
      if (tq.isEmpty()) { return null; }

      CssParser p = new CssParser(tq);
      input = p.parseStyleSheet();
      tq.expectEmpty();
    } else {
      throw new AssertionError("Can't classify input " + is);
    }
    return input;
  }

  /** Write the given parse tree to the given file. */
  private void writeFile(File f, ParseTreeNode output) {
    if (output == null) { return; }
    Callback<IOException> ioHandler = new Callback<IOException>() {
      public void handle(IOException ex) {
        mq.addMessage(
            MessageType.IO_ERROR, MessagePart.Factory.valueOf(ex.toString()));
      }
    };
    try {
      Writer out = new OutputStreamWriter(new FileOutputStream(f), "UTF-8");
      TokenConsumer tc;
      switch (config.renderer()) {
        case PRETTY:
          tc = output.makeRenderer(out, ioHandler);
          break;
        case MINIFY:
          tc = new JsMinimalPrinter(out,  ioHandler);
          break;
        case SIDEBYSIDE:
          tc = new SourceSnippetRenderer(
              buildOriginalInputCharSequences(), mc, out, ioHandler) {
            @Override
            protected TokenConsumer createDelegateRenderer(
                Appendable out, Callback<IOException> exHandler) {
              return new JsPrettyPrinter(out, exHandler);
            }
          };
          break;
        default:
          throw new AssertionError(
              "Unrecognized renderer: " + config.renderer());
      }
      try {
        RenderContext rc = new RenderContext(mc, true, true, tc);
        output.render(rc);
        tc.noMoreTokens();
        out.append('\n');
      } finally {
        out.close();
      }
    } catch (IOException ex) {
      ioHandler.handle(ex);
    }
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
        out.append(level.name() + ": ");
        m.format(mc, out);
        out.append("\n");

        if (maxLevel.compareTo(level) < 0) { maxLevel = level; }
      }
    } catch (IOException ex) {
      ex.printStackTrace();
    }
    return maxLevel;
  }

  private PluginEnvironment makeEnvironment(Config config) {
    try {
      return new CachingEnvironment(
          new File(config.getInputUris().iterator().next()).getParentFile());
    } catch (IllegalArgumentException ex) {  // Not a file: URI
      return PluginEnvironment.CLOSED_PLUGIN_ENVIRONMENT;
    }
  }

  private Reader createReader(InputSource is, InputStream stream) {
    InputStreamReader isr;

    try {
      isr = new InputStreamReader(stream, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new AssertionError(e);
    }

    if (config.renderer() == Config.SourceRenderMode.SIDEBYSIDE) {
      CapturingReader cr = new CapturingReader(isr);
      originalInputs.put(is, cr);
      return cr;
    } else {
      return isr;
    }
  }

  private Map<InputSource, CharSequence> buildOriginalInputCharSequences()
      throws IOException {
    Map<InputSource, CharSequence> results =
        new HashMap<InputSource, CharSequence>();
    for (InputSource is : originalInputs.keySet()) {
      results.put(is, originalInputs.get(is).getCapture());
    }
    return results;
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
}
