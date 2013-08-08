// Copyright (C) 2011 Google Inc.
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

import com.google.caja.util.Pair;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;

/**
 * Servlet to return some simple cross-domain JSON content for testing. No
 * defensive escaping of content is done; all content is assumed to be 7bit
 * ASCII with no embedded quotes or escape sequences.
 *
 * @author ihab.awad@gmail.com (Ihab Awad)
 */
public class JsonTestServlet extends HttpServlet {
  private static final Pair<String, String> UMP =
    Pair.pair("Access-Control-Allow-Origin", "*");

  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException {
    if (!"json".equals(req.getParameter("alt"))) {
      throw new ServletException("Expected alt=json");
    }
    respond(resp,
        req.getParameter("callback"),
        "sanityCheck", "sane",
        "testParam",   req.getParameter("testParam"),
        "content",     "",
        "contentType", "");
  }

  @Override
  public void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException {
    if (!"json".equals(req.getParameter("alt"))) {
      throw new ServletException("Expected alt=json");
    }
    respond(resp,
        req.getParameter("callback"),
        "sanityCheck", "sane",
        "testParam",   req.getParameter("testParam"),
        "content",     readInputStream(req),
        "contentType", req.getContentType());
  }

  private static void respond(HttpServletResponse resp,
                              String jsonpCallbackName,
                              String... keyValuePairs)
      throws ServletException {
    StringBuilder sb = new StringBuilder();
    if (jsonpCallbackName != null) { sb.append(jsonpCallbackName + "("); }
    sb.append("{");
    for (int i = 0; i < keyValuePairs.length; ) {
      sb.append("\"" + keyValuePairs[i++] + "\":");
      sb.append("\"" + keyValuePairs[i++] + "\"");
      if (i < keyValuePairs.length) { sb.append(","); }
    }
    sb.append("}");
    if (jsonpCallbackName != null) { sb.append(");"); }
    respondString(resp, sb.toString());
  }

  private static void respondString(HttpServletResponse resp, String response)
      throws ServletException {

    System.err.println(response);

    resp.setStatus(HttpServletResponse.SC_OK);
    try {
      byte[] content = response.getBytes();
      resp.setContentType("application/json");
      // resp.setCharacterEncoding("UTF-8");
      resp.setContentLength(content.length);
      resp.setHeader(UMP.a, UMP.b);
      resp.getOutputStream().write(content);
      resp.getOutputStream().close();
    } catch (IOException ex) {
      throw (ServletException) new ServletException().initCause(ex);
    }
  }

  private static String readInputStream(HttpServletRequest req)
      throws ServletException {
    try {
      Reader r = new InputStreamReader(req.getInputStream(), "UTF-8");
      Writer w = new StringWriter();
      for (int c; (c = r.read()) != -1; ) {
        w.write((char) c);
      }
      return w.toString();
    } catch (IOException e) {
      throw new ServletException(e);
    }
  }
}