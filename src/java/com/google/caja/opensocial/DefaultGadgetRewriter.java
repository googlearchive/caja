// Copyright (C) 2007 Google Inc.
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

import com.google.caja.lang.css.CssSchema;
import com.google.caja.lang.html.HtmlSchema;
import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.ExternalReference;
import com.google.caja.lexer.HtmlLexer;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.ParseException;
import com.google.caja.lexer.TokenConsumer;
import com.google.caja.parser.AncestorChain;
import com.google.caja.parser.html.DomParser;
import com.google.caja.parser.html.DomTree;
import com.google.caja.parser.js.Block;
import com.google.caja.plugin.PluginCompiler;
import com.google.caja.plugin.PluginEnvironment;
import com.google.caja.plugin.PluginMeta;
import com.google.caja.render.JsPrettyPrinter;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.MessageType;
import com.google.caja.reporting.RenderContext;
import com.google.caja.util.Callback;
import com.google.caja.util.ReadableReader;

import java.io.IOException;
import java.io.Reader;
import java.net.URI;

/**
 * A default implementation of the Caja/OpenSocial gadget rewriter.
 *
 * @author ihab.awad@gmail.com (Ihab Awad)
 */
public class DefaultGadgetRewriter implements GadgetRewriter, GadgetContentRewriter {
  private final MessageQueue mq;
  private CssSchema cssSchema;
  private HtmlSchema htmlSchema;
  private boolean debugMode;

  public DefaultGadgetRewriter(MessageQueue mq) {
    this.mq = mq;
  }

  public MessageQueue getMessageQueue() {
    return mq;
  }

  public void setCssSchema(CssSchema cssSchema) {
    this.cssSchema = cssSchema;
  }
  public void setHtmlSchema(HtmlSchema htmlSchema) {
    this.htmlSchema = htmlSchema;
  }
  /**
   * @param debugMode whether to include debugging info in cajoled output.
   */
  public void setDebugMode(boolean debugMode) { this.debugMode = debugMode; }

  public void rewrite(ExternalReference gadgetRef, UriCallback uriCallback,
                      String view, Appendable output)
      throws UriCallbackException, GadgetRewriteException, IOException,
          ParseException {
    assert gadgetRef.getUri().isAbsolute() : gadgetRef.toString();
    rewrite(
        gadgetRef.getUri(),
        CharProducer.Factory.create(
            uriCallback.retrieve(gadgetRef, "text/xml"),
            new InputSource(gadgetRef.getUri())),
        uriCallback,
        view,
        output);
  }

  public void rewrite(URI baseUri, CharProducer gadgetSpec, UriCallback uriCallback,
                      String view, Appendable output)
      throws GadgetRewriteException, IOException, ParseException {
    GadgetParser parser = new GadgetParser();
    GadgetSpec spec = parser.parse(
        gadgetSpec, new InputSource(baseUri), view, mq);
    StringBuilder rewritten = new StringBuilder();
    rewriteContent(baseUri, spec.getContent(), uriCallback, rewritten);
    spec.setContent(rewritten.toString());
    parser.render(spec, output);
  }

  public void rewriteContent(URI baseUri,
                             Readable gadgetSpec,
                             UriCallback uriCallback,
                             Appendable output)
      throws GadgetRewriteException, IOException {
    CharProducer content = readReadable(gadgetSpec, new InputSource(baseUri));
    output.append(rewriteContent(baseUri, content, uriCallback));
  }

  public void rewriteContent(URI baseUri,
                             CharProducer content,
                             UriCallback uriCallback,
                             Appendable output)
      throws GadgetRewriteException, IOException {
    output.append(rewriteContent(baseUri, content, uriCallback));
  }

  private String rewriteContent(
      URI baseUri, CharProducer content, UriCallback callback)
      throws GadgetRewriteException {

    DomTree.Fragment htmlContent;
    try {
      htmlContent = parseHtml(content, new InputSource(baseUri));
    } catch (ParseException ex) {
      ex.toMessageQueue(mq);
      throw new GadgetRewriteException(ex);
    }

    PluginCompiler compiler = compileGadget(htmlContent, baseUri, callback);

    MessageContext mc = compiler.getMessageContext();
    StringBuilder style = new StringBuilder();
    StringBuilder script = new StringBuilder();

    Callback<IOException> errorHandler = new Callback<IOException>() {
      public void handle(IOException ex) {
        mq.addMessage(MessageType.IO_ERROR,
                      MessagePart.Factory.valueOf("" + ex));
      }
    };

    Block js = compiler.getJavascript();
    if (js != null) {
      TokenConsumer tc = new JsPrettyPrinter(script, errorHandler);
      js.render(createRenderContext(tc, mc));
      tc.noMoreTokens();
    }

    if (!compiler.getJobs().hasNoErrors()) {
      throw new GadgetRewriteException();
    }

    return rewriteContent(script.toString());
  }

  private DomTree.Fragment parseHtml(CharProducer htmlContent, InputSource src)
      throws GadgetRewriteException, ParseException {
    DomParser p = new DomParser(new HtmlLexer(htmlContent), src, mq);
    if (p.getTokenQueue().isEmpty()) {
      mq.addMessage(OpenSocialMessageType.NO_CONTENT, src);
      throw new GadgetRewriteException("No content");
    }
    return p.parseFragment();
  }

  private PluginCompiler compileGadget(
      DomTree.Fragment content, final URI baseUri, final UriCallback callback)
      throws GadgetRewriteException {
    PluginMeta meta = new PluginMeta(
        new PluginEnvironment() {
          public CharProducer loadExternalResource(
              ExternalReference ref, String mimeType) {
            ExternalReference absRef = new ExternalReference(
                baseUri.resolve(ref.getUri()), ref.getReferencePosition());
            Reader content;
            try {
              content = callback.retrieve(absRef, mimeType);
            } catch (UriCallbackException ex) {
              ex.toMessageQueue(getMessageQueue());
              return null;
            }
            return CharProducer.Factory.create(
                content, new InputSource(absRef.getUri()));
          }

          public String rewriteUri(ExternalReference ref, String mimeType) {
            ExternalReference absRef = new ExternalReference(
                baseUri.resolve(ref.getUri()), ref.getReferencePosition());
            try {
              URI uri = callback.rewrite(absRef, mimeType);
              if (uri == null) { return null; }
              return uri.toString();
            } catch (UriCallbackException ex) {
              return null;
            }
          }
        });
    meta.setDebugMode(debugMode);

    PluginCompiler compiler = createPluginCompiler(meta, mq);

    compiler.addInput(new AncestorChain<DomTree.Fragment>(content));

    if (!compiler.run()) {
      throw new GadgetRewriteException("Gadget has compile errors");
    }

    return compiler;
  }

  private String rewriteContent(String script) {
    return "<script type=\"text/javascript\">" + script + "</script>";
  }

  private CharProducer readReadable(Readable input, InputSource src) {
    return CharProducer.Factory.create(new ReadableReader(input), src);
  }

  protected RenderContext createRenderContext(
      TokenConsumer tc, MessageContext mc) {
    return new RenderContext(mc, true, tc);
  }

  protected PluginCompiler createPluginCompiler(
      PluginMeta meta, MessageQueue mq) {
    PluginCompiler compiler = new PluginCompiler(meta, mq);
    if (cssSchema != null) { compiler.setCssSchema(cssSchema); }
    if (htmlSchema != null) { compiler.setHtmlSchema(htmlSchema); }
    return compiler;
  }
}
