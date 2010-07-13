// Copyright 2010 Google Inc. All Rights Reserved.
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

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.google.caja.lexer.ExternalReference;
import com.google.caja.lexer.FetchedData;
import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.escaping.UriUtil;
import com.google.caja.parser.html.Namespaces;
import com.google.caja.parser.html.Nodes;
import com.google.caja.parser.js.ArrayConstructor;
import com.google.caja.parser.js.CajoledModule;
import com.google.caja.parser.js.Expression;
import com.google.caja.parser.js.IntegerLiteral;
import com.google.caja.parser.js.ObjectConstructor;
import com.google.caja.parser.js.StringLiteral;
import com.google.caja.parser.js.ValueProperty;
import com.google.caja.plugin.UriFetcher;
import com.google.caja.plugin.UriPolicy;
import com.google.caja.render.Concatenator;
import com.google.caja.render.JsMinimalPrinter;
import com.google.caja.reporting.BuildInfo;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.RenderContext;
import com.google.caja.util.Callback;
import com.google.caja.util.Lists;
import com.google.caja.util.Pair;

/**
 * Common parent class for handlers that invoke the cajoler
 * and render the result
 *
 * @author jasvir@google.com (Jasvir Nagra)
 */
public abstract class AbstractCajolingHandler implements ContentHandler {
  protected final BuildInfo buildInfo;
  protected final UriFetcher uriFetcher;
  protected final UriPolicy uriPolicy;

  public AbstractCajolingHandler(
      BuildInfo buildInfo, final String hostedService,
      final UriFetcher uriFetcher) {
    this.buildInfo = buildInfo;
    this.uriFetcher = uriFetcher != null ? uriFetcher : UriFetcher.NULL_NETWORK;
    this.uriPolicy = new UriPolicy() {
      public String rewriteUri(
          ExternalReference u, UriEffect effect, LoaderType loader,
          Map<String, ?> hints) {
        if (hostedService != null) {
          return hostedService
              + "?url=" + UriUtil.encode(u.getUri().toString())
              + "&effect=" + effect + "&loader=" + loader;
        } else {
          return null;
        }
      }
    };
  }

  public abstract boolean canHandle(URI uri,
      CajolingService.Transform transform,
      List<CajolingService.Directive> directives,
      String inputContentType, String outputContentType,
      ContentTypeCheck checker);

  public abstract Pair<String,String> apply(URI uri,
      CajolingService.Transform transform,
      List<CajolingService.Directive> directives,
      ContentHandlerArgs args,
      String inputContentType,
      String outputContentType,
      ContentTypeCheck checker,
      FetchedData input,
      OutputStream response,
      MessageQueue mq)
      throws UnsupportedContentTypeException;

  protected void renderAsHtml(Document doc,
      Node staticHtml, CajoledModule javascript,
      Expression moduleCallback, Appendable output)
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

  private static StringLiteral lit(String s) {
    return StringLiteral.valueOf(FilePosition.UNKNOWN, s);
  }

  private static IntegerLiteral lit(int i) {
    return new IntegerLiteral(FilePosition.UNKNOWN, i);
  }

  private static ArrayConstructor arr(List<? extends Expression> items) {
    return new ArrayConstructor(FilePosition.UNKNOWN, items);
  }

  private static ObjectConstructor obj(List<? extends ValueProperty> props) {
    return new ObjectConstructor(FilePosition.UNKNOWN, props);
  }

  private static ValueProperty prop(String key, Expression e) {
    return new ValueProperty(FilePosition.UNKNOWN, lit(key), e);
  }

  protected void renderAsJSON(Document doc,
      Node staticHtml, CajoledModule javascript, Expression moduleCallback,
      MessageQueue mq, Appendable output)
      throws IOException {
    List<ValueProperty> props = Lists.newArrayList();

    if (staticHtml != null) {
      props.add(prop("html", lit(Nodes.render(staticHtml))));
    }
    if (javascript != null) {
      props.add(prop("js", lit(renderJavascript(javascript, moduleCallback))));
    }
    if (mq.hasMessageAtLevel(MessageLevel.LOG)) {
      List<Expression> messages = Lists.newArrayList();
      for (Message m : mq.getMessages()) {
        messages.add(obj(Arrays.asList(
            prop("level", lit(m.getMessageLevel().ordinal())),
            prop("name", lit(m.getMessageLevel().name())),
            prop("type", lit(m.getMessageType().name())),
            prop("message", lit(m.toString())))));
      }
      props.add(prop("messages", arr(messages)));
    }

    class IOCallback implements Callback<IOException> {
      IOException ex;
      public void handle(IOException e) {
        if (this.ex != null) { this.ex = e; }
      }
    }

    IOCallback callback = new IOCallback();
    RenderContext rc = new RenderContext(new JsMinimalPrinter(new Concatenator(
        output, callback)))
        .withJson(true);
    obj(props).render(rc);
    rc.getOut().noMoreTokens();
    if (callback.ex != null) { throw callback.ex; }
  }

  protected void renderAsJavascript(CajoledModule javascript,
      Expression moduleCallback, Appendable output)
      throws IOException {
    output.append(renderJavascript(javascript, moduleCallback));
  }

  protected String renderJavascript(CajoledModule javascript,
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
