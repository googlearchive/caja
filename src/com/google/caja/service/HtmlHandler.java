// Copyright 2009 Google Inc. All Rights Reserved.
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

package com.google.caja.service;

import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.ExternalReference;
import com.google.caja.lexer.HtmlLexer;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.ParseException;
import com.google.caja.opensocial.UriCallback;
import com.google.caja.opensocial.UriCallbackException;
import com.google.caja.parser.AncestorChain;
import com.google.caja.parser.quasiliteral.QuasiBuilder;
import com.google.caja.parser.html.DomParser;
import com.google.caja.parser.html.Namespaces;
import com.google.caja.parser.html.Nodes;
import com.google.caja.parser.js.CajoledModule;
import com.google.caja.parser.js.Expression;
import com.google.caja.plugin.Dom;
import com.google.caja.plugin.PluginCompiler;
import com.google.caja.plugin.PluginEnvironment;
import com.google.caja.plugin.PluginMeta;
import com.google.caja.render.Concatenator;
import com.google.caja.render.JsMinimalPrinter;
import com.google.caja.reporting.BuildInfo;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.RenderContext;
import com.google.caja.reporting.SimpleMessageQueue;
import com.google.caja.util.Pair;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Retrieves html files and cajoles them
 *
 * @author jasvir@google.com (Jasvir Nagra)
 */
public class HtmlHandler implements ContentHandler {
  private final BuildInfo buildInfo;
  private final PluginEnvironment pluginEnvironment;
  private final static String DEFAULT_HOSTED_SERVICE
      = "http://caja.appspot.com/cajoler";

  public HtmlHandler(BuildInfo buildInfo) {
    this(buildInfo, DEFAULT_HOSTED_SERVICE, null);
  }

  public HtmlHandler(
      BuildInfo buildInfo, final String hostedService,
      final UriCallback retriever) {
    this.buildInfo = buildInfo;
    this.pluginEnvironment = new PluginEnvironment() {
      public CharProducer loadExternalResource(
          ExternalReference ref, String mimeType) {
        try {
          Reader in = retriever != null
              ? retriever.retrieve(ref, mimeType) : null;
          InputSource is = new InputSource(ref.getUri());
          return in != null ? CharProducer.Factory.create(in, is) : null;
        } catch (UriCallbackException ex) {
          return null;
        } catch (IOException ex) {
          return null;
        }
      }

      public String rewriteUri(ExternalReference uri, String mimeType) {
        if (hostedService != null) {
          try {
            return hostedService
                + "?url="
                + URLEncoder.encode(uri.getUri().toString(), "UTF-8")
                + "&mime-type=" + mimeType;
          } catch (UnsupportedEncodingException e) {
            return null;
          }
        } else {
          return null;
        }
      }
    };
  }

  public boolean canHandle(URI uri, CajolingService.Transform transform,
      String inputContentType, String outputContentType,
      ContentTypeCheck checker) {
    return checker.check("text/html", inputContentType)
        && (checker.check(outputContentType, "text/html")
            || checker.check(outputContentType, "*/*")
            || checker.check(outputContentType, "text/javascript"));
  }

  public Pair<String,String> apply(URI uri,
                                   CajolingService.Transform transform,
                                   ContentHandlerArgs args,
                                   String inputContentType,
                                   String outputContentType,
                                   ContentTypeCheck checker,
                                   String charset,
                                   byte[] content,
                                   OutputStream response)
      throws UnsupportedContentTypeException {
    PluginMeta meta = new PluginMeta(pluginEnvironment);
    if (checker.check(outputContentType, "text/html")
        || checker.check(outputContentType, "*/*")) {
      meta.setOnlyJsEmitted(false);
    } else if (checker.check(outputContentType, "text/javascript")) {
      meta.setOnlyJsEmitted(true);
    } else {
      throw new UnsupportedContentTypeException();
    }

    if (charset == null) { charset = "UTF-8"; }

    String moduleCallbackString = CajaArguments.MODULE_CALLBACK.get(args);
    Expression moduleCallback = (Expression)
        (moduleCallbackString == null
            ? null
            : QuasiBuilder.substV(moduleCallbackString));

    try {
      OutputStreamWriter writer = new OutputStreamWriter(response, "UTF-8");
      cajoleHtml(uri, new StringReader(new String(content, charset)),
          meta, moduleCallback, writer);
      writer.flush();
    } catch (IOException e) {
      throw new UnsupportedContentTypeException();
    }

    return new Pair<String, String>(
        meta.isOnlyJsEmitted() ? "text/javascript" : "text/html",
        "UTF-8");
  }

  public void printMessages(MessageQueue mq, MessageContext mc,
      Appendable out) {
    try {
      for (Message m : mq.getMessages()) {
        MessageLevel level = m.getMessageLevel();
        out.append(level.name() + ": ");
        m.format(mc, out);
        out.append("\n");
      }
    } catch (IOException ex) {
      ex.printStackTrace();
    }
  }

  private void cajoleHtml(URI inputUri, Reader cajaInput, PluginMeta meta,
                          Expression moduleCallback, Appendable output)
      throws IOException, UnsupportedContentTypeException {
    InputSource is = new InputSource (inputUri);
    CharProducer cp = CharProducer.Factory.create(cajaInput,is);
    MessageQueue mq = new SimpleMessageQueue();
    boolean okToContinue = true;
    try {
      DomParser p = new DomParser(new HtmlLexer(cp), is, mq);
      if (p.getTokenQueue().isEmpty()) { okToContinue = false; }

      Dom html = new Dom(p.parseFragment());
      Document doc = html.getValue().getOwnerDocument();
      p.getTokenQueue().expectEmpty();

      PluginCompiler compiler = new PluginCompiler(buildInfo, meta, mq);

      compiler.addInput(AncestorChain.instance(html));
      if (okToContinue) {
        okToContinue &= compiler.run();
      }
      if (okToContinue) {
        if (meta.isOnlyJsEmitted()) {
          renderAsJavascript(compiler.getJavascript(),
                             moduleCallback,
                             output);
        } else {
          renderAsHtml(doc,
                       compiler.getStaticHtml(),
                       compiler.getJavascript(),
                       moduleCallback,
                       output);
        }
      } else {
        MessageContext mc = new MessageContext();
        printMessages(mq, mc, System.err);
      }
    } catch (ParseException e) {
      throw new UnsupportedContentTypeException();
    } catch (IllegalArgumentException e) {
      throw new UnsupportedContentTypeException();
    } catch (IOException e) {
      throw new UnsupportedContentTypeException();
    }
  }

  private void renderAsHtml(Document doc,
                            Node staticHtml,
                            CajoledModule javascript,
                            Expression moduleCallback,
                            Appendable output)
      throws IOException {
    if (staticHtml != null) {
      output.append(Nodes.render(staticHtml));
    }
    if (javascript != null) {
      String htmlNs = Namespaces.HTML_NAMESPACE_URI;
      Element script = doc.createElementNS(htmlNs, "script");
      script.setAttributeNS(htmlNs, "type", "text/javascript");
      script.appendChild(doc.createTextNode(
          renderJavascript(javascript, moduleCallback)));
      output.append(Nodes.render(script));
    }
  }

  private void renderAsJavascript(CajoledModule javascript,
                                  Expression moduleCallback,
                                  Appendable output)
      throws IOException {
    output.append(renderJavascript(javascript, moduleCallback));
  }

  private String renderJavascript(CajoledModule javascript,
                                  Expression moduleCallback) {
    StringBuilder jsOut = new StringBuilder();
    RenderContext rc = new RenderContext(
        new JsMinimalPrinter(new Concatenator(jsOut)))
        .withEmbeddable(true);
    javascript.render(moduleCallback, rc);
    rc.getOut().noMoreTokens();
    return jsOut.toString();
  }
}
