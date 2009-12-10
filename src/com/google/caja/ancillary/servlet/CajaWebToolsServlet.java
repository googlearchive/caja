// Copyright (C) 2009 Google Inc.
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

package com.google.caja.ancillary.servlet;

import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.ParseException;
import com.google.caja.parser.html.DomParser;
import com.google.caja.parser.html.HtmlQuasiBuilder;
import com.google.caja.parser.html.Nodes;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.SimpleMessageQueue;
import com.google.caja.util.Lists;
import com.google.caja.util.Pair;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Node;

/**
 * Allows web developers to lint, minify, and generate documentation for their
 * code via a web interface.
 *
 * @author mikesamuel@gmail.com
 */
public class CajaWebToolsServlet extends HttpServlet {
  final StaticFiles staticFiles;
  private final Pattern staticFilePath;

  /**
   * @param cacheId an alphanumeric string that can be added to a directory
   *     name in a URL to version all resources in that directory.
   */
  public CajaWebToolsServlet(String cacheId) {
    this.staticFiles = new StaticFiles(cacheId);
    // Matches "favicon.ico" and paths under <tt>/files-.../</tt> that do not
    // contain any pathname element that starts with a ., so no parent directory
    // names, and no UNIX hidden files.
    this.staticFilePath = Pattern.compile(
        "^/(?:(favicon\\.ico)|"
        + Pattern.quote("files-" + cacheId) // A directory containing cache Id
        + "/((?:[^/.]+/)*[^/.]+(?:\\.[^/.]+)))$");
  }

  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws IOException {
    URI reqUri = URI.create(req.getRequestURI());
    String reqPath = reqUri.getPath();
    // Redirect to /index preserving any query string.
    if ("/".equals(reqPath)) {
      try {
        String query = req.getQueryString();
        URI indexUri = new URI(null, null, Verb.INDEX.requestPath, query, null);
        resp.sendRedirect(indexUri.toString());
      } catch (URISyntaxException ex) {
        ex.printStackTrace();
        // Let process report an error
      }
      return;
    }
    Matcher m = staticFilePath.matcher(reqPath);
    if (m.matches()) {
      // Allow GETs of static files.
      String path = m.group(2);
      if (path == null) { path = m.group(1); }
      staticFiles.serve("files/" + path, req, resp);
    } else {
      // Process a dynamic operation.
      process(reqUri.getPath(), req.getQueryString(), resp);
    }
  }

  @Override
  public void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws IOException {
    URI reqUri = URI.create(req.getRequestURI());
    String reqPath = reqUri.getPath();
    // Special case uploads since they require very different processing.
    if ("/upload".equals(reqPath)) {
      UploadPage.doUpload(req, resp);
      return;
    }
    StringBuilder query = new StringBuilder();
    Reader in = req.getReader();
    try {
      char[] buf = new char[1024];
      for (int n; (n = in.read(buf)) > 0;) { query.append(buf, 0, n); }
    } finally {
      in.close();
    }
    process(reqUri.getPath(), query.toString(), resp);
  }

  /**
   * Processes a dynamic request which cannot be satisfied by
   * {@link StaticFiles} or the special upload handler.
   */
  private void process(String reqPath, String query, HttpServletResponse out)
      throws IOException {
    Result result = handle(reqPath, parseQueryString(query));
    // Serve the result
    if (result.status != 0) { out.setStatus(result.status); }
    String contentType = result.getContentType();
    if (contentType != null) { out.setContentType(contentType); }
    for (Pair<String, String> header : result.headers) {
      if (containsControlChar(header.b)) {
        throw new IOException("Split header <<" + header + ">>");
      }
      out.setHeader(header.a, header.b);
    }
    if (result.content != null) {
      if (result.content.isText()) {
        Writer w = out.getWriter();
        try {
          result.content.toWriter(w);
        } finally {
          w.close();
        }
      } else {
        OutputStream os = out.getOutputStream();
        try {
          result.content.toOutputStream(os);
        } finally {
          os.close();
        }
      }
    }
  }

  /** Expose query parameters in an order-preserving way. */
  private static List<Pair<String, String>> parseQueryString(String query) {
    List<Pair<String, String>> out = Lists.newArrayList();
    if (query != null) {
      if (query.startsWith("?")) { query = query.substring(1); }
      if (!"".equals(query)) {
        for (String kv : query.split("&")) {
          int eq = kv.indexOf('=');
          if (eq >= 0) {
            out.add(Pair.pair(uriDecode(kv.substring(0, eq)),
                              uriDecode(kv.substring(eq + 1))));
          } else {
            out.add(Pair.pair(uriDecode(kv), ""));
          }
        }
      }
    }
    return out;
  }

  private static String uriDecode(String s) {
    try {
      return URLDecoder.decode(s, "UTF-8");
    } catch (UnsupportedEncodingException ex) {
      throw new RuntimeException(ex);
    }
  }

  /**
   * Handles a request.
   * @param reqPath the URI path requested.
   * @param params query parameters in the order they appear.
   * @return the response to send back.
   */
  Result handle(String reqPath, List<Pair<String, String>> params) {
    MessageQueue mq = new SimpleMessageQueue();
    Request req;
    {
      // The verb is specified in the path, but in the index page, there is
      // a select box for the verb, so for /index, the param processing below
      // might set the verb in request.
      Verb verb = Verb.fromReqPath(reqPath);
      if (verb == null) {
        return errorPage(
            404, "File not found " + reqPath + ".  Expected a path in "
            + EnumSet.allOf(Verb.class),
            mq, new Request());
      }
      req = Request.create(verb, staticFiles);
    }

    List<Job> inputJobs = Lists.newArrayList();
    Processor p = new Processor(req, mq);
    try {
      Verb v = req.verb;
      // Process all the parameters
      for (Pair<String, String> cgiParam : params) {
        String name = cgiParam.a;
        String value = cgiParam.b;
        if ("".equals(value)) { continue; }
        Request.handler(v, name).handle(name, value, req);
      }
      // Parse all the inputs.
      for (Input input : req.inputs) {
        if ("".equals(input.code.trim())) { continue; }
        InputSource is = new InputSource(req.baseUri.resolve(input.path));
        CharProducer cp = CharProducer.Factory.fromString(input.code, is);
        req.srcMap.put(is, cp.clone());
        req.mc.addInputSource(is);
        try {
          inputJobs.add(p.parse(cp, input.t, null));
        } catch (ParseException ex) {
          ex.toMessageQueue(mq);
        }
      }
    } catch (BadInputException ex) {
      return errorPage(ex.getMessage(), mq, req);
    }

    // Take the inputs and generate output jobs.
    List<Job> jobs;
    if (req.verb == Verb.INDEX) {
      jobs = Collections.singletonList(Job.html(IndexPage.render(req)));
    } else if (req.verb == Verb.HELP) {
      jobs = Collections.singletonList(Job.html(HelpPage.render(staticFiles)));
    } else {
      try {
        jobs = p.process(inputJobs);
      } catch (IOException ex) {
        ex.printStackTrace();
        return errorPage(ex.getMessage(), mq, req);
      }
      if (jobs.isEmpty() && !inputJobs.isEmpty()) {
        return errorPage(null, mq, req);
      }
    }

    // Reduce the output jobs down to one output job.
    // This may involve concatenating javascript or css files, or combining
    // heterogenous file types into a single HTML file.
    Content content = p.reduce(jobs);

    // Report errors if we have unresolved errors.
    // For the lint page, which incorporates errors into the regular output,
    // the message queue has already been drained.
    if (MessageLevel.ERROR.compareTo(maxMessageLevel(mq)) < 0) {
      return errorPage(null, mq, req);
    }

    Result result = new Result(HttpServletResponse.SC_OK, content, mq);
    if (req.verb == Verb.ECHO) {
      // Force a download so that /echo can't be used as an open redirector.
      String downloadPath = null;
      if (req.inputs.size() == 1) { downloadPath = req.inputs.get(0).path; }
      if (downloadPath == null || "".equals(downloadPath)
          || downloadPath.startsWith("unnamed-")
          || containsControlChar(downloadPath)) {
        downloadPath = "caja_tools_output." + content.type.ext;
      }
      result.headers.add(Pair.pair(
          "Content-disposition",
          "attachment; filename=" + rfc822QuotedString(downloadPath)));
    }
    return result;
  }

  private static MessageLevel maxMessageLevel(MessageQueue mq) {
    MessageLevel max = MessageLevel.values()[0];
    for (Message msg : mq.getMessages()) {
      MessageLevel lvl = msg.getMessageLevel();
      if (max.compareTo(lvl) < 0) { max = lvl; }
    }
    return max;
  }

  private Result errorPage(String title, MessageQueue mq, Request req) {
    return errorPage(HttpServletResponse.SC_BAD_REQUEST, title, mq, req);
  }

  private Result errorPage(
      int status, String title, MessageQueue mq, Request req) {
    Document doc = DomParser.makeDocument(null, null);
    HtmlQuasiBuilder b = HtmlQuasiBuilder.getBuilder(doc);
    DocumentFragment messages = Reporter.messagesToFragment(mq, req, b);
    Node errorDoc = b.substV(
        ""
        + "<html>"
        + "<head>"
        + "<meta http-equiv=Content-Type content=text/html;charset=UTF-8 />"
        + "<title>@title</title>"
        + "</head>"
        + "<body>@header@messages</body>"
        + "</html>",
        "title", title != null ? title : "Errors in input",
        "header", title != null ? b.substV("<h1>@t</h1>", "t", title) : "",
        "messages", messages);
    Content errorHtml = new Content(Nodes.render(errorDoc), ContentType.HTML);
    return new Result(status, errorHtml, mq);
  }

  private static boolean containsControlChar(String s) {
    for (int i = 0, n = s.length(); i < n; ++i) {
      if (s.charAt(i) < 0x20 && s.charAt(i) != '\t') { return true; }
    }
    return false;
  }

  // as referenced from rfc 2045 via rfc 2183
  private static String rfc822QuotedString(String s) {
    int n = s.length();
    StringBuilder sb = new StringBuilder(n + 16);
    sb.append('"');
    int pos = 0;
    for (int i = 0; i < n; ++i) {
      char ch = s.charAt(i);
      if (ch == '"' || ch == '\\') {
        sb.append(s, pos, i).append('\\');
        pos = i;
      }
    }
    sb.append(s, pos, n).append('"');
    return sb.toString();
  }
}
