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
import java.io.Writer;
import java.net.URI;
import java.util.List;
import java.util.Map;

import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.JsLexer;
import com.google.caja.lexer.JsTokenQueue;
import com.google.caja.lexer.ParseException;
import com.google.caja.lexer.TokenConsumer;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.js.Identifier;
import com.google.caja.parser.js.Parser;
import com.google.caja.parser.quasiliteral.QuasiBuilder;
import com.google.caja.render.JsMinimalPrinter;
import com.google.caja.render.JsPrettyPrinter;
import com.google.caja.reporting.SimpleMessageQueue;
import com.google.caja.util.ContentType;
import com.google.caja.util.Json;
import com.google.caja.util.Maps;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.w3c.dom.Node;

import com.google.caja.lexer.FetchedData;
import com.google.caja.parser.html.Nodes;
import com.google.caja.plugin.UriFetcher;
import com.google.caja.render.Concatenator;
import com.google.caja.reporting.BuildInfo;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.RenderContext;
import com.google.caja.util.Callback;
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
  protected final String hostedService;

  public AbstractCajolingHandler(
      BuildInfo buildInfo, String hostedService, UriFetcher uriFetcher) {
    this.buildInfo = buildInfo;
    this.hostedService = hostedService;
    this.uriFetcher = uriFetcher != null ? uriFetcher : UriFetcher.NULL_NETWORK;
  }

  public abstract boolean canHandle(URI uri,
      CajolingService.Transform transform,
      List<CajolingService.Directive> directives,
      String inputContentType,
      ContentTypeCheck checker);

  public abstract Pair<String,String> apply(URI uri,
      CajolingService.Transform transform,
      List<CajolingService.Directive> directives,
      ContentHandlerArgs args,
      String inputContentType,
      ContentTypeCheck checker,
      FetchedData input,
      OutputStream response,
      MessageQueue mq)
      throws UnsupportedContentTypeException;

  /**
   * Checks whether a string is a JavaScript Identifier.
   */
  /* visible for testing */ static boolean checkIdentifier(String candidate) {
    // Using a simple regex is possible if we reject anything but 7-bit ASCII.
    // However, this implementation ensures Caja has a single point of truth
    // regarding what constitutes a JS identifier.
    MessageQueue mq = new SimpleMessageQueue();
    Parser parser = new Parser(
        new JsTokenQueue(
            new JsLexer(
                CharProducer.Factory.fromString(
                    "var " + candidate + ";",
                    InputSource.UNKNOWN)),
            InputSource.UNKNOWN),
        mq);
    ParseTreeNode node;
    try { node = parser.parse(); } catch (ParseException e) { return false; }
    if (node == null || !mq.getMessages().isEmpty()) { return false; }
    Map<String, ParseTreeNode> bindings = Maps.newHashMap();
    if (!QuasiBuilder.match("{ var @p; }", node, bindings)) { return false; }
    if (bindings.size() != 1) { return false; }
    if (bindings.get("p") == null) { return false; }
    if (!(bindings.get("p") instanceof Identifier)) { return false; }
    Identifier p = (Identifier) bindings.get("p");
    if (!candidate.equals(p.getName())) { return false; }
    return true;
  }

  private static class IOCallback implements Callback<IOException> {
    public IOException ex = null;
    public void handle(IOException e) {
      if (this.ex != null) { this.ex = e; }
    }
  }

  protected static void renderAsJSON(
      Node staticHtml,
      ParseTreeNode javascript,
      String jsonpCallback,
      MessageQueue mq,
      Writer output,
      boolean pretty)
      throws IOException {
    String html = staticHtml == null ? null : Nodes.render(staticHtml);
    String js =
      javascript == null ? null : renderJavascript(javascript, pretty);
    renderAsJSON(html, js, jsonpCallback, mq, output, pretty);
  }

  protected static void renderAsJSON(
      String staticHtml,
      String javascript,
      String jsonpCallback,
      MessageQueue mq,
      Writer output,
      boolean pretty) throws IOException {
    if (jsonpCallback != null && !checkIdentifier(jsonpCallback)) {
      throw new RuntimeException("Detected XSS attempt; aborting request");
    }

    JSONObject o = new JSONObject();
    JSONArray messages = new JSONArray();

    if (staticHtml != null) { Json.put(o, "html", staticHtml); }
    if (javascript != null) { Json.put(o, "js", javascript); }
    Json.put(o, "messages", messages);

    for (Message m : mq.getMessages()) {
      JSONObject msg = new JSONObject();
      Json.put(msg, "level", m.getMessageLevel().ordinal());
      Json.put(msg, "name", m.getMessageLevel().name());
      Json.put(msg, "type", m.getMessageType().name());
      Json.put(msg, "message", m.toString());
      Json.push(messages, msg);
    }

    String rendered = o.toJSONString();

    output.append(
        (jsonpCallback != null)
            ? jsonpCallback + "(" + rendered + ");"
            : rendered);
    output.flush();
  }

  private static String renderJavascript(
      ParseTreeNode javascript, boolean pretty)
      throws IOException {
    StringBuilder jsOut = new StringBuilder();
    IOCallback callback = new IOCallback();
    RenderContext rc = makeRenderContext(jsOut, callback, pretty, false);
    javascript.render(rc);
    rc.getOut().noMoreTokens();
    if (callback.ex != null) { throw callback.ex; }
    return jsOut.toString();
  }

  private static RenderContext makeRenderContext(
      Appendable a, IOCallback cb,
      boolean pretty,
      boolean json) {
    TokenConsumer tc = pretty
        ? new JsPrettyPrinter(new Concatenator(a, cb))
        : new JsMinimalPrinter(new Concatenator(a, cb));
    return new RenderContext(tc).withJson(json);
  }

  protected static Pair<ContentType, String> getReturnedContentParams(
      ContentHandlerArgs args) {
    String alt = CajaArguments.ALT.get(args);
    if ("json".equals(alt) || alt == null) {
      return Pair.pair(ContentType.JSON, null);
    } else if ("json-in-script".equals(alt)) {
      String callback = CajaArguments.CALLBACK.get(args);
      if (callback == null) {
        throw new RuntimeException(
            "Missing value for parameter " + CajaArguments.CALLBACK);
      } else {
        return Pair.pair(ContentType.JS, callback);
      }
    } else {
      throw new RuntimeException(
          "Invalid value " + alt + " for parameter " + CajaArguments.ALT);
    }
  }
}
