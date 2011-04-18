// Copyright (C) 2008 Google Inc.
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

import com.google.caja.ancillary.opt.JsOptimizer;
import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.ExternalReference;
import com.google.caja.lexer.FetchedData;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.JsLexer;
import com.google.caja.lexer.JsTokenQueue;
import com.google.caja.lexer.ParseException;
import com.google.caja.lexer.TokenConsumer;
import com.google.caja.lexer.escaping.UriUtil;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.ParserContext;
import com.google.caja.parser.html.DomParser;
import com.google.caja.parser.html.Namespaces;
import com.google.caja.parser.html.Nodes;
import com.google.caja.parser.js.Expression;
import com.google.caja.parser.js.Minify;
import com.google.caja.parser.js.ObjectConstructor;
import com.google.caja.parser.js.Parser;
import com.google.caja.parser.js.Statement;
import com.google.caja.reporting.MarkupRenderMode;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.MessageType;
import com.google.caja.reporting.RenderContext;
import com.google.caja.reporting.SimpleMessageQueue;
import com.google.caja.reporting.SnippetProducer;
import com.google.caja.reporting.BuildInfo;
import com.google.caja.render.Concatenator;
import com.google.caja.render.Innocent;
import com.google.caja.render.JsMinimalPrinter;
import com.google.caja.render.JsPrettyPrinter;
import com.google.caja.tools.BuildService;
import com.google.caja.util.Charsets;
import com.google.caja.util.Lists;
import com.google.caja.util.Maps;
import com.google.caja.util.Pair;
import com.google.caja.util.Sets;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Build integration to {@link PluginCompiler} and {@link Minify}.
 *
 * @author mikesamuel@gmail.com
 */
public class BuildServiceImplementation implements BuildService {
  private final Map<InputSource, String> originalSources = Maps.newHashMap();

  /**
   * Cajoles inputs to output writing any messages to logger, returning true
   * iff the task passes.
   */
  public boolean cajole(
      PrintWriter logger, List<File> dependees, List<File> inputs, File output,
      Map<String, Object> options) {
    final Set<File> canonFiles = Sets.newHashSet();
    try {
      for (File f : dependees) { canonFiles.add(f.getCanonicalFile()); }
      for (File f : inputs) { canonFiles.add(f.getCanonicalFile()); }
    } catch (IOException ex) {
      logger.println(ex.toString());
      return false;
    }
    final MessageQueue mq = new SimpleMessageQueue();

    UriFetcher fetcher = new UriFetcher() {
        public FetchedData fetch(ExternalReference ref, String mimeType)
            throws UriFetchException {
          URI uri = ref.getUri();
          uri = ref.getReferencePosition().source().getUri().resolve(uri);
          InputSource is = new InputSource(uri);

          try {
            if (!canonFiles.contains(new File(uri).getCanonicalFile())) {
              throw new UriFetchException(ref, mimeType);
            }
          } catch (IllegalArgumentException ex) {
            throw new UriFetchException(ref, mimeType, ex);
          } catch (IOException ex) {
            throw new UriFetchException(ref, mimeType, ex);
          }

          try {
            String content = getSourceContent(is);
            if (content == null) {
              throw new UriFetchException(ref, mimeType);
            }
            return FetchedData.fromCharProducer(
                CharProducer.Factory.fromString(content, is),
                mimeType, Charsets.UTF_8.name());
          } catch (IOException ex) {
            throw new UriFetchException(ref, mimeType, ex);
          }
        }
      };

    UriPolicy policy;
    final UriPolicy prePolicy = new UriPolicy() {
      public String rewriteUri(
          ExternalReference u, UriEffect effect, LoaderType loader,
          Map<String, ?> hints) {
        // TODO(ihab.awad): Need to pass in the URI rewriter from the build
        // file somehow (as a Cajita program?). The below is a stub.
        return URI.create(
            "http://example.com/"
            + "?effect=" + effect + "&loader=" + loader
            + "&uri=" + UriUtil.encode("" + u.getUri()))
            .toString();
      }
    };
    final Set<?> lUrls = (Set<?>) options.get("canLink");
    if (!lUrls.isEmpty()) {
      policy = new UriPolicy() {
        public String rewriteUri(
            ExternalReference u, UriEffect effect,
            LoaderType loader, Map<String, ?> hints) {
          String uri = u.getUri().toString();
          if (lUrls.contains(uri)) { return uri; }
          return prePolicy.rewriteUri(u, effect, loader, hints);
        }
      };
    } else {
      policy = prePolicy;
    }

    MessageContext mc = new MessageContext();

    String language = (String) options.get("language");
    String rendererType = (String) options.get("renderer");

    if ("javascript".equals(language) && "concat".equals(rendererType)) {
      return concat(inputs, output, logger);
    }

    boolean passed = true;
    ParseTreeNode outputJs;
    Node outputHtml;
    if ("caja".equals(language) || "valija".equals(language)) {
      PluginMeta meta = new PluginMeta(fetcher, policy);
      meta.setEnableES53("caja".equals(language));
      PluginCompiler compiler = new PluginCompiler(
          BuildInfo.getInstance(), meta, mq);
      compiler.setMessageContext(mc);
      if (Boolean.TRUE.equals(options.get("debug"))) {
        compiler.setGoals(compiler.getGoals()
            .without(PipelineMaker.ONE_CAJOLED_MODULE)
            .with(PipelineMaker.ONE_CAJOLED_MODULE_DEBUG));
      }
      if (Boolean.TRUE.equals(options.get("onlyJsEmitted"))) {
        compiler.setGoals(
            compiler.getGoals().without(PipelineMaker.HTML_SAFE_STATIC));
      }

      // Parse inputs
      for (File f : inputs) {
        try {
          URI fileUri = f.getCanonicalFile().toURI();
          ParseTreeNode parsedInput = new ParserContext(mq)
              .withInput(new InputSource(fileUri))
              .withConfig(meta)
              .build();
          if (parsedInput == null) {
            passed = false;
          } else {
            compiler.addInput(parsedInput, fileUri);
          }
        } catch (IOException ex) {
          logger.println("Failed to read " + f);
          passed = false;
        } catch (ParseException ex) {
          logger.println("Failed to parse " + f);
          ex.toMessageQueue(mq);
          passed = false;
        } catch (IllegalStateException e) {
          logger.println("Failed to configure parser " + e.getMessage());
          passed = false;
        }
      }

      // Cajole
      passed = passed && compiler.run();

      outputJs = passed ? compiler.getJavascript() : null;
      outputHtml = passed ? compiler.getStaticHtml() : null;
    } else if ("javascript".equals(language)) {
      PluginMeta meta = new PluginMeta(fetcher, policy);
      passed = true;
      JsOptimizer optimizer = new JsOptimizer(mq);
      for (File f : inputs) {
        try {
          if (f.getName().endsWith(".env.json")) {
            loadEnvJsonFile(f, optimizer, mq);
          } else {
            ParseTreeNode parsedInput = new ParserContext(mq)
            .withInput(new InputSource(f.getCanonicalFile().toURI()))
            .withConfig(meta)
            .build();
            if (parsedInput != null) {
              optimizer.addInput((Statement) parsedInput);
            }
          }
        } catch (IOException ex) {
          logger.println("Failed to read " + f);
          passed = false;
        } catch (ParseException ex) {
          logger.println("Failed to parse " + f);
          ex.toMessageQueue(mq);
          passed = false;
        } catch (IllegalStateException e) {
          logger.println("Failed to configure parser " + e.getMessage());
          passed = false;
        }
      }
      outputJs = optimizer.optimize();
      outputHtml = null;
    } else {
      throw new RuntimeException("Unrecognized language: " + language);
    }
    passed = passed && !hasErrors(mq);

    // From the ignore attribute to the <transform> element.
    Set<?> toIgnore = (Set<?>) options.get("toIgnore");
    if (toIgnore == null) { toIgnore = Collections.emptySet(); }

    // Log messages
    SnippetProducer snippetProducer = new SnippetProducer(originalSources, mc);
    for (Message msg : mq.getMessages()) {
      if (passed && MessageLevel.LOG.compareTo(msg.getMessageLevel()) >= 0) {
        continue;
      }
      String snippet = snippetProducer.getSnippet(msg);
      if (!"".equals(snippet)) { snippet = "\n" + snippet; }
      if (!passed || !toIgnore.contains(msg.getMessageType().name())) {
        logger.println(
            msg.getMessageLevel() + " : " + msg.format(mc) + snippet);
      }
    }

    // Write the output
    if (passed) {
      // Write out as HTML if the output file has the right extension.
      boolean asXml = output.getName().endsWith(".xhtml");
      boolean emitMarkup = asXml || output.getName().endsWith(".html");

      StringBuilder jsOut = new StringBuilder();
      TokenConsumer renderer;
      if ("pretty".equals(rendererType)) {
        renderer = new JsPrettyPrinter(new Concatenator(jsOut));
      } else if ("minify".equals(rendererType)) {
        renderer = new JsMinimalPrinter(new Concatenator(jsOut));
      } else {
        throw new RuntimeException("Unrecognized renderer " + rendererType);
      }
      RenderContext rc = new RenderContext(renderer).withEmbeddable(emitMarkup);
      outputJs.render(rc);
      rc.getOut().noMoreTokens();

      String htmlOut = "";
      if (outputHtml != null) {
        htmlOut = Nodes.render(
            outputHtml, asXml ? MarkupRenderMode.XML : MarkupRenderMode.HTML);
      }

      String translatedCode;
      if (emitMarkup) {
        Document doc = DomParser.makeDocument(null, null);
        String ns = Namespaces.HTML_NAMESPACE_URI;
        Element script = doc.createElementNS(ns, "script");
        script.setAttributeNS(ns, "type", "text/javascript");
        script.appendChild(doc.createCDATASection(jsOut.toString()));
        translatedCode = htmlOut + Nodes.render(
            script, asXml ? MarkupRenderMode.XML : MarkupRenderMode.HTML);
      } else {
        if (!"".equals(htmlOut)) {
          throw new RuntimeException("Can't emit HTML to " + output);
        }
        translatedCode = jsOut.toString();
      }

      passed = write(translatedCode, output, logger);
    }
    return passed;
  }

  private boolean write(String string, File output, PrintWriter logger) {
    try {
      Writer w = new OutputStreamWriter(new FileOutputStream(output));
      try {
        w.write(string);
      } finally {
        w.close();
      }
    } catch (IOException ex) {
      logger.println("Failed to write " + output);
      return false;
    }
    return true;
  }

  private boolean concat(List<File> inputs, File output, PrintWriter logger) {
    StringBuilder result = new StringBuilder();
    boolean ok = true;
    boolean first = true;
    File oneStrict = null;
    File oneNonstrict = null;
    for (File f : inputs) {
      if (!first) {
        result.append(";\n");
      }
      first = false;
      try {
        CharSequence contents = read(f);
        if (isStrict(contents)) {
          oneStrict = f;
        } else {
          oneNonstrict = f;
        }
        result.append(contents);
      } catch (IOException ex) {
        logger.println("Failed to read " + f);
        ok = false;
      }
    }
    if (oneStrict != null && oneNonstrict != null) {
      logger.println("Can't naively concatenate strict and non-strict JS: "
          + oneStrict + " " + oneNonstrict);
      ok = false;
    }
    if (ok) {
      ok = write(result.toString(), output, logger);
    }
    return ok;
  }

  // match a top-level strict declaration.
  private static Pattern strictRE = Pattern.compile(
      "^[^{]*['\"]use\\s+strict['\"]");

  // TODO(felix8a): implement this in a non-stupid way
  private boolean isStrict(CharSequence js) {
    return strictRE.matcher(js).find(); 
  }

  private String getSourceContent(InputSource is) throws IOException {
    String content = originalSources.get(is);
    if (content == null) {
      File f = new File(is.getUri());
      // Read it in and stuff it back in the map so we can generate
      // snippets.
      Reader in = new InputStreamReader(new FileInputStream(f), Charsets.UTF_8);
      try {
        char[] buf = new char[4096];
        StringBuilder sb = new StringBuilder();
        for (int n; (n = in.read(buf, 0, buf.length)) > 0;) {
          sb.append(buf, 0, n);
        }
        content = sb.toString();
      } finally {
        in.close();
      }
      originalSources.put(is, content);
    }
    return content;
  }

  /**
   * Minifies inputs to output writing any messages to logger, returning true
   * iff the task passes.
   */
  public boolean minify(
      PrintWriter logger, List<File> dependees, List<File> inputs, File output,
      Map<String, Object> options) {
    try {
      List<Pair<InputSource, File>> inputSources = Lists.newArrayList();
      for (File f : inputs) {
        inputSources.add(
            Pair.pair(new InputSource(f.getAbsoluteFile().toURI()), f));
      }
      Writer outputWriter = new OutputStreamWriter(
          new FileOutputStream(output), Charsets.UTF_8);
      try {
        return Minify.minify(inputSources, outputWriter, logger);
      } finally {
        outputWriter.close();
      }
    } catch (IOException ex) {
      logger.println("Minifying failed: " + ex);
      return false;
    }
  }

  /**
   * Applies the innocent code transformer to inputs.  Writes
   * any messages to logger and returns true iff the task passes.
   */
  public boolean transfInnocent(
      PrintWriter logger, List<File> dependees, List<File> inputs, File output,
      Map<String, Object> options) {
    try {
      boolean ret;
      Writer outputWriter = new OutputStreamWriter(
          new FileOutputStream(output), Charsets.UTF_8);
      for (File f : inputs) {
        Pair<InputSource, File> inputSource =
          Pair.pair(new InputSource(f.getAbsoluteFile().toURI()), f);
        ret = Innocent.transfInnocent(inputSource, outputWriter, logger);
        if (!ret) {
          outputWriter.close();
          return false;
        }
      }
      outputWriter.close();
      return true;
    } catch (IOException ex) {
      logger.println("Innocent transform failed: " + ex);
      return false;
    }
  }

  private static void loadEnvJsonFile(File f, JsOptimizer op, MessageQueue mq) {
    CharProducer cp;
    try {
      cp = read(f);
    } catch (IOException ex) {
      mq.addMessage(
          MessageType.IO_ERROR, MessagePart.Factory.valueOf(ex.toString()));
      return;
    }
    ObjectConstructor envJson;
    try {
      Parser p = parser(cp, mq);
      Expression e = p.parseExpression(true); // TODO(mikesamuel): limit to JSON
      p.getTokenQueue().expectEmpty();
      if (!(e instanceof ObjectConstructor)) {
        mq.addMessage(
            MessageType.IO_ERROR,
            MessagePart.Factory.valueOf("Invalid JSON in " + f));
        return;
      }
      envJson = (ObjectConstructor) e;
    } catch (ParseException ex) {
      ex.toMessageQueue(mq);
      return;
    }
    op.setEnvJson(envJson);
  }

  private static CharProducer read(File f) throws IOException {
    InputSource is = new InputSource(f.toURI());
    return CharProducer.Factory.create(
        new InputStreamReader(new FileInputStream(f), Charsets.UTF_8), is);
  }

  private static Parser parser(CharProducer cp, MessageQueue errs) {
    JsLexer lexer = new JsLexer(cp);
    JsTokenQueue tq = new JsTokenQueue(lexer, cp.getCurrentPosition().source());
    return new Parser(tq, errs);
  }

  private static boolean hasErrors(MessageQueue mq) {
    for (Message msg : mq.getMessages()) {
      if (MessageLevel.ERROR.compareTo(msg.getMessageLevel()) <= 0) {
        return true;
      }
    }
    return false;
  }
}
