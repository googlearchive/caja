// Copyright (C) 2010 Google Inc.
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

package com.google.caja.service;

import com.google.caja.SomethingWidgyHappenedError;
import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.ExternalReference;
import com.google.caja.lexer.FetchedData;
import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.JsLexer;
import com.google.caja.lexer.JsTokenQueue;
import com.google.caja.lexer.ParseException;
import com.google.caja.lexer.escaping.Escaping;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.js.Identifier;
import com.google.caja.parser.js.Parser;
import com.google.caja.parser.quasiliteral.QuasiBuilder;
import com.google.caja.plugin.UriFetcher;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.SimpleMessageQueue;
import com.google.caja.util.Charsets;
import com.google.caja.util.ContentType;
import com.google.caja.util.Json;
import com.google.caja.util.Pair;
import com.google.common.collect.Maps;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

/**
 * Proxy service used to allow Caja to load cross-origin content.
 * 
 * @author jasvir@gmail.com (Jasvir Nagra)
 * @author ihab.awad@gmail.com (Ihab Awad)
 */
public class ProxyServlet extends HttpServlet {
  // TODO(kpreid): This code needs cleanup; it has many relics of its origin
  // as the CajolingService.

  private static final long serialVersionUID = 5055670217887121398L;
  private static final Pair<String, String> UMP =
    Pair.pair("Access-Control-Allow-Origin", "*");

  private static class HttpContentHandlerArgs extends ContentHandlerArgs {
    private final HttpServletRequest request;

    public HttpContentHandlerArgs(HttpServletRequest request) {
      this.request = request;
    }

    @Override
    public String get(String name) {
      return request.getParameter(name);
    }
  }

  private final ContentTypeCheck typeCheck = new LooseContentTypeCheck();
  private final UriFetcher uriFetcher;

  /**
   * Use default UriFetcher.
   */
  public ProxyServlet() {
    this(new UriFetcher() {
        public FetchedData fetch(ExternalReference ref, String mimeType)
            throws UriFetchException {
          try {
            HttpURLConnection conn = (HttpURLConnection)
                ref.getUri().toURL().openConnection();
            // appengine has a caching http proxy; this limits it
            conn.setRequestProperty("Cache-Control", "max-age=10");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            return FetchedData.fromConnection(conn);
          } catch (IOException ex) {
            throw new UriFetchException(ref, mimeType, ex);
          }
        }
      });
  }

  public ProxyServlet(UriFetcher fetcher) {
    this.uriFetcher = fetcher;
  }

  /**
   * Set an error status on a servlet response and close its stream cleanly.
   *
   * @param resp a servlet response.
   * @param error an error message.
   */
  private static void closeBadRequest(HttpServletResponse resp,
      int httpStatus, String error)
      throws ServletException {
    try {
      resp.sendError(httpStatus, error);
    } catch (IOException ex) {
      throw (ServletException) new ServletException().initCause(ex);
    }
  }

  /**
   * Set an error status on a servlet response and close its stream cleanly.
   *
   * @param resp a servlet response.
   * @param httpStatus status response level.
   * @param mq a {@link MessageQueue} with messages to include as an error page.
   */
  private static void closeBadRequest(HttpServletResponse resp,
      int httpStatus, MessageQueue mq)
      throws ServletException {
    closeBadRequest(resp, httpStatus, serializeMessageQueue(mq));
  }

  // TODO(jasvir): The service like the gwt version should accumulate
  // input sources and use html snippet producer to produce messages
  private static String serializeMessageQueue(MessageQueue mq) {
    StringBuilder sb = new StringBuilder();
    MessageContext mc = new MessageContext();
    for (Message m : mq.getMessages()) {
      sb.append(m.getMessageLevel().name()).append(": ");
      Escaping.escapeXml(m.format(mc), false, sb);
      sb.append("\n");
    }
    return sb.toString();
  }

  @Override
  protected void doOptions(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    // CORS requires that browsers do an OPTIONS request before allowing
    // cross-site POSTs.  UMP does not require this, but no browser implements
    // UMP at this time.  So, we reply to the OPTIONS request to trick
    // browsers into effectively implementing UMP.
    resp.setHeader("Access-Control-Allow-Origin", "*");
    resp.setHeader("Access-Control-Allow-Methods", "GET, POST");
    resp.setHeader("Access-Control-Allow-Headers", "Content-Type");
    resp.setHeader("Access-Control-Max-Age", "86400");
  }

  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException {
    ContentHandlerArgs args = new HttpContentHandlerArgs(req);

    // URL path parameters can trick IE into misinterpreting responses as HTML
    if (req.getRequestURI().contains(";")) {
      throw new ServletException("Invalid URL path parameter");
    }

    MessageQueue mq = new SimpleMessageQueue();
    FetchedData result = handle(args, mq);
    if (result == null) {
      closeBadRequest(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, mq);
      return;
    }

    resp.setStatus(HttpServletResponse.SC_OK);

    String responseContentType = result.getContentType();
    if (result.getCharSet() != null) {
      responseContentType += ";charset=" + result.getCharSet();
    }
    if (containsNewline(responseContentType)) {
      throw new IllegalArgumentException(responseContentType);
    }

    try {
      byte[] content = result.getByteContent();
      resp.setContentType(responseContentType);
      resp.setContentLength(content.length);
      resp.setHeader(UMP.a, UMP.b);
      resp.setHeader("X-Content-Type-Options", "nosniff");

      resp.getOutputStream().write(content);
      resp.getOutputStream().close();
    } catch (IOException ex) {
      throw (ServletException) new ServletException().initCause(ex);
    }
  }

  // Used to protect against header splitting attacks.
  private static boolean containsNewline(String s) {
    return s.indexOf('\n') >= 0 || s.indexOf('\r') >= 0;
  }

  /**
   * Main entry point for the fetching proxy.
   *
   * @param args a set of arguments to the cajoling service.
   * @param mq a message queue into which status and error messages will be
   *     placed. The caller should query for the most severe status of the
   *     messages in this queue to determine the overall success of the
   *     invocation.
   * @return the output content, or {@code null} if a serious error occurred
   *     that prevented the content from being generated.
   */
  public FetchedData handle(ContentHandlerArgs args,
                            MessageQueue mq) {
    FetchedData result = doHandle(args, mq);
    if (result == null) {
      ByteArrayOutputStream intermediateResponse = new ByteArrayOutputStream();
      Pair<ContentType, String> contentParams =
          getReturnedContentParams(args);
      OutputStreamWriter writer = new OutputStreamWriter(
          intermediateResponse, Charsets.UTF_8);
      try {
        renderAsJSON(
            (String)null, (String)null, contentParams.b, mq, writer, false);
      } catch (IOException e) {
        // Unlikely IOException to byte array; rethrow
        throw new SomethingWidgyHappenedError(e);
      }
      result = FetchedData.fromBytes(
          intermediateResponse.toByteArray(),
          contentParams.a.mimeType,
          "UTF-8",
          InputSource.UNKNOWN);
    }
    return result;
  }

  private FetchedData doHandle(ContentHandlerArgs args,
                               MessageQueue mq) {
    String inputUrlString = CajaArguments.URL.get(args);
    URI inputUri;
    if (inputUrlString == null) {
      mq.addMessage(
          ServiceMessageType.MISSING_ARGUMENT,
          MessagePart.Factory.valueOf(CajaArguments.URL.toString()));
      return null;
    } else {
      try {
        inputUri = new URI(inputUrlString);
      } catch (URISyntaxException ex) {
        mq.addMessage(
            ServiceMessageType.INVALID_INPUT_URL,
            MessagePart.Factory.valueOf(inputUrlString));
        return null;
      }
    }

    String expectedInputContentType = CajaArguments.INPUT_MIME_TYPE.get(args);
    if (expectedInputContentType == null) {
      mq.addMessage(
          ServiceMessageType.MISSING_ARGUMENT,
          MessagePart.Factory.valueOf(
              CajaArguments.INPUT_MIME_TYPE.toString()));
      return null;
    }

    FetchedData inputFetchedData;
    try {
      inputFetchedData = uriFetcher.fetch(
          new ExternalReference(inputUri, FilePosition.UNKNOWN),
          expectedInputContentType);
    } catch (UriFetcher.UriFetchException ex) {
      ex.toMessageQueue(mq);
      return null;
    }

    if (!typeCheck.check(
            expectedInputContentType,
            inputFetchedData.getContentType())) {
      mq.addMessage(
          ServiceMessageType.UNEXPECTED_INPUT_MIME_TYPE,
          MessagePart.Factory.valueOf(expectedInputContentType),
          MessagePart.Factory.valueOf(inputFetchedData.getContentType()));
      return null;
    }

    ByteArrayOutputStream intermediateResponse = new ByteArrayOutputStream();
    Pair<String, String> contentInfo;
    try {
      contentInfo = applyHandler(
          inputUri,
          args,
          inputFetchedData,
          intermediateResponse,
          mq);
    } catch (UnsupportedContentTypeException e) {
      mq.addMessage(ServiceMessageType.UNSUPPORTED_CONTENT_TYPES);
      return null;
    } catch (RuntimeException e) {
      mq.addMessage(
          ServiceMessageType.EXCEPTION_IN_SERVICE,
          MessagePart.Factory.valueOf(e.toString()));
      return null;
    }

    return FetchedData.fromBytes(
        intermediateResponse.toByteArray(),
        contentInfo.a,
        contentInfo.b,
        new InputSource(inputUri));
  }

  private Pair<String, String> applyHandler(URI uri, ContentHandlerArgs args,
      FetchedData fetched, OutputStream response, MessageQueue mq)
      throws UnsupportedContentTypeException {
    Pair<ContentType, String> contentParams =
        getReturnedContentParams(args);

    OutputStreamWriter writer;
    try {
      writer = new OutputStreamWriter(response, Charsets.UTF_8);
      renderAsJSON(
          fetched.getTextualContent().toString(),
          null,
          contentParams.b, mq, writer, true);
      writer.flush();
      return Pair.pair(contentParams.a.mimeType, Charsets.UTF_8.name());
    } catch (UnsupportedEncodingException ex) {
      return null;
    } catch (IOException ex) {
      throw new SomethingWidgyHappenedError(ex);
    }
  }

  /**
   * Checks whether a string is a JavaScript Identifier.
   */
  /* visible for testing */ static boolean checkIdentifier(String candidate) {
    // Using a simple regex is possible if we reject anything but 7-bit ASCII.
    // However, this implementation ensures Caja has a single point of truth
    // regarding what constitutes a JS identifier.
    // TODO(kpreid): Reevaluate whether this is worth the complexity and the
    // runtime dependency on the JS parser now that there is no cajoler. 
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

  private static Pair<ContentType, String> getReturnedContentParams(
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

  private static void renderAsJSON(
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
}
