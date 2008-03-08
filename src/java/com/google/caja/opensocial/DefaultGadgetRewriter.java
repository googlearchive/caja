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

import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.ExternalReference;
import com.google.caja.lexer.HtmlTokenType;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.ParseException;
import com.google.caja.lexer.TokenQueue;
import com.google.caja.parser.AncestorChain;
import com.google.caja.parser.css.CssTree;
import com.google.caja.parser.html.DomParser;
import com.google.caja.parser.html.DomTree;
import com.google.caja.parser.html.OpenElementStack;
import com.google.caja.parser.js.Block;
import com.google.caja.plugin.HtmlPluginCompiler;
import com.google.caja.plugin.PluginEnvironment;
import com.google.caja.plugin.PluginMeta;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.RenderContext;
import com.google.caja.util.ReadableReader;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.net.URI;

/**
 * A default implementation of the Caja/OpenSocial gadget rewriter.
 *
 * @author ihab.awad@gmail.com (Ihab Awad)
 */
public class DefaultGadgetRewriter implements GadgetRewriter, GadgetContentRewriter {
  private static final String DOM_PREFIX = "DOM-PREFIX";

  private MessageQueue mq;

  public DefaultGadgetRewriter(MessageQueue mq) {
    this.mq = mq;
  }

  public MessageQueue getMessageQueue() {
    return mq;
  }

  public void rewrite(ExternalReference gadgetRef, UriCallback uriCallback,
                      Appendable output)
      throws UriCallbackException, GadgetRewriteException, IOException {
    assert gadgetRef.getUri().isAbsolute() : gadgetRef.toString();
    rewrite(
        gadgetRef.getUri(),
        uriCallback.retrieve(gadgetRef, "text/xml"),
        uriCallback,
        output);
  }

  public void rewrite(URI baseUri, Readable gadgetSpec, UriCallback uriCallback, Appendable output)
      throws GadgetRewriteException, IOException {
    GadgetParser parser = new GadgetParser();
    GadgetSpec spec = parser.parse(gadgetSpec);
    spec.setContent(rewriteContent(baseUri, spec.getContent(), uriCallback));
    parser.render(spec, output);
  }

  public void rewriteContent(URI baseUri,
                             Readable gadgetSpec,
                             UriCallback uriCallback,
                             Appendable output)
      throws GadgetRewriteException, IOException {
    String contentString = readReadable(gadgetSpec);
    output.append(rewriteContent(baseUri, contentString, uriCallback));
  }

  private String rewriteContent(
      URI baseUri, String content, UriCallback callback)
      throws GadgetRewriteException {

    DomTree.Fragment htmlContent;
    try {
      htmlContent = parseHtml(baseUri, content);
    } catch (ParseException ex) {
      ex.toMessageQueue(mq);
      throw new GadgetRewriteException(ex);
    }

    HtmlPluginCompiler compiler = compileGadget(htmlContent, baseUri, callback);

    MessageContext mc = compiler.getMessageContext();
    StringBuilder style = new StringBuilder();
    StringBuilder script = new StringBuilder();
    try {
      CssTree css = compiler.getCss(); 
      if (css != null) { css.render(createRenderContext(style, mc)); }
      Block js = compiler.getJavascript();
      if (js != null) { js.render(createRenderContext(script, mc)); }
    } catch (IOException ex) {
      // StringBuilders should not throw IOExceptions.
      throw new RuntimeException(ex);
    }

    return rewriteContent(style.toString(), script.toString());
  }

  private DomTree.Fragment parseHtml(URI uri, String htmlContent)
      throws GadgetRewriteException, ParseException {
    InputSource is = new InputSource(uri); // TODO(mikesamuel): is this correct?
    TokenQueue<HtmlTokenType> tq = DomParser.makeTokenQueue(
        is, new StringReader(htmlContent), false);
    if (tq.isEmpty()) { return null; }
    DomTree.Fragment contentTree = DomParser.parseFragment(
        tq, OpenElementStack.Factory.createHtml5ElementStack(mq));

    if (contentTree == null) {
      mq.addMessage(OpenSocialMessageType.NO_CONTENT, is);
      throw new GadgetRewriteException("No content");
    }
    return contentTree;
  }

  private HtmlPluginCompiler compileGadget(
      DomTree.Fragment content, final URI baseUri, final UriCallback callback)
      throws GadgetRewriteException {
    PluginMeta meta = new PluginMeta(DOM_PREFIX,
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
    
    HtmlPluginCompiler compiler = new HtmlPluginCompiler(mq, meta);

    // Compile
    compiler.addInput(new AncestorChain<DomTree.Fragment>(content));

    if (!compiler.run()) {
      throw new GadgetRewriteException();
    }

    return compiler;
  }

  private String rewriteContent(String style, String script) {
    StringBuilder results = new StringBuilder();
    if (!"".equals(style)) {
      results.append("<style type=\"text/css\">\n")
          .append(style)
          .append("</style>\n");
    }

    if (!"".equals(script)) {
      results.append("<script type=\"text/javascript\">\n")
          .append(script)
          .append("</script>\n");
    }

    return results.toString();
  }

  private String readReadable(Readable input) {
    StringBuilder sb = new StringBuilder();
    Reader r = new ReadableReader(input);
    try {
      while (true) {
        int c = r.read();
        if (c < 0) break;
        sb.append((char)c);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return sb.toString();
  }

  protected RenderContext createRenderContext(
      Appendable out, MessageContext mc) {
    return new RenderContext(mc, out, true);
  }
}
