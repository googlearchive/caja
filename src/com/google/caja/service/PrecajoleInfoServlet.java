// Copyright (C) 2012 Google Inc.
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

import com.google.caja.precajole.StaticPrecajoleMap;
import com.google.caja.util.Charsets;
import com.google.caja.util.Json;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * Servlet that reports precajoled resources.
 *
 * @author felix8a@gmail.com (Felix Lee)
 */

public class PrecajoleInfoServlet extends HttpServlet {

  private static String[] COMMENT = {
    "The cajoler has the following resources precajoled.",
    "All URLs in a group are treated as the same resource.",
  };

  @Override
  public void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException
  {
    doGet(req, resp);
  }

  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException
  {
    // URL path parameters can trick IE into misinterpreting responses as HTML
    if (req.getRequestURI().contains(";")) {
      throw new ServletException("Invalid URL path parameter");
    }
    try {
      JSONObject info = jsonInfo();
      String rendered;
      String format = req.getParameter("format");
      if ("json".equals(format)) {
        rendered = info.toString();
        resp.setContentType("application/json;charset=utf-8");
      } else if ("html".equals(format)) {
        rendered = formatHtml(info);
        resp.setContentType("text/html;charset=utf-8");
      } else if ("text".equals(format)) {
        resp.setContentType("text/plain;charset=utf-8");
        rendered = formatText(info);
      } else {
        resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
            "Please specify format=json, text, or html");
        return;
      }
      resp.setHeader("X-Content-Type-Options", "nosniff");
      byte[] bytes = utf8(rendered);
      resp.setContentLength(bytes.length);
      resp.getOutputStream().write(bytes);
      resp.getOutputStream().close();
    } catch (IOException e) {
      throw new ServletException(e);
    }
  }

  private static String formatHtml(JSONObject info) {
    StringBuilder out = new StringBuilder();
    out.append("<!doctype html>\n");
    out.append("<html>\n");
    out.append("<body>\n");
    out.append("  <div>\n");
    out.append("    <a href='?format=text'>[plaintext]</a>\n");
    out.append("    <a href='?format=json'>[json]</a>\n");
    out.append("  </div>\n");
    out.append("  <pre id='info'></pre>\n");
    out.append("  <script>\n");
    out.append("    var info = ").append(info.toString()).append(";\n");
    out.append("    var s = JSON.stringify(info, null, '  ');\n");
    out.append("    var t = document.createTextNode(s);\n");
    out.append("    document.getElementById('info').appendChild(t);\n");
    out.append("  </script>\n");
    out.append("</body>\n");
    out.append("</html>\n");
    return out.toString();
  }

  private String formatText(JSONObject info) {
    StringBuilder out = new StringBuilder();
    formatTextTo(out, info, "");
    return out.toString();
  }

  private void formatTextTo(StringBuilder out, Object obj, String indent) {
    if (obj instanceof JSONObject) {
      JSONObject jobj = (JSONObject) obj;
      for (String key : jsonKeys(jobj)) {
        out.append(indent).append(key).append("\n");
        formatTextTo(out, jobj.get(key), "  " + indent);
      }
    } else if (obj instanceof JSONArray) {
      JSONArray jarr = (JSONArray) obj;
      for (int k = 0; k < jarr.size(); k++) {
        formatTextTo(out, jarr.get(k), indent);
      }
      out.append(indent).append("\n");
    } else {
      out.append(indent).append(obj.toString()).append("\n");
    }
  }

  private static List<String> jsonKeys(JSONObject obj) {
    @SuppressWarnings("unchecked")
    List<String> list = new ArrayList<String>(obj.keySet());
    Collections.sort(list);
    return list;
  }

  private static byte[] utf8(String s) {
    return s.getBytes(Charsets.UTF_8);
  }

  private static JSONObject jsonInfo() {
    JSONObject info = new JSONObject();

    JSONArray comment = new JSONArray();
    for (String line : COMMENT) {
      Json.push(comment, line);
    }
    Json.put(info, "comment", comment);

    JSONArray groups = new JSONArray();
    for (List<String> urls : getUrlGroups()) {
      JSONArray group = new JSONArray();
      for (String url : urls) {
        Json.push(group, url);
      }
      Json.push(groups, group);
    }
    Json.put(info, "urlGroups", groups);

    return info;
  }

  private static List<List<String>> getUrlGroups() {
    StaticPrecajoleMap spm = StaticPrecajoleMap.getInstance();
    List<List<String>> urlGroups = spm.getUrlGroups();

    // sort the urlGroups into a friendly order
    for (List<String> urls : urlGroups) {
      Collections.sort(urls);
    }
    Collections.sort(urlGroups, new Comparator<List<String>>() {
      @Override
      public int compare(List<String> a, List<String> b) {
        return a.get(0).compareTo(b.get(0));
      }
    });
    return urlGroups;
  }
}
