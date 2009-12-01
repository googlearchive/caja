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

import com.google.caja.parser.html.DomParser;
import com.google.caja.parser.html.HtmlQuasiBuilder;
import com.google.caja.parser.html.Namespaces;

import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;

/**
 * Produces HTML describing the interface to the tools servlet.
 *
 * @see Verb#HELP
 * @author mikesamuel@gmail.com
 */
final class HelpPage {
  static DocumentFragment render(StaticFiles staticFiles) {
    Document doc = DomParser.makeDocument(null, null);
    HtmlQuasiBuilder b = HtmlQuasiBuilder.getBuilder(doc);
    DocumentFragment out = doc.createDocumentFragment();
    out.appendChild(b.substV(
        (
         ""
         + "<html><head>"
         + "<meta http-equiv=Content-Type content=text/html;charset=UTF-8 />"
         + "<title>Caja Web Tools Help</title>"
         + "<link rel=stylesheet type=text/css href=files-@cacheId/styles.css>"
         + "</head>"
         + "<body>"
         + "<h1>Caja Web Tools Help</h1>"
         + "<p>Below are the URLs paths and parameters that show how to"
         + " apply the Caja web tools to your web application.  Use the"
         + " <a href=@index>interactive form</a> to experiment.</p>"
         + "@verbs</body></html>"),
        "index", Verb.INDEX.requestPath,
        "verbs", renderVerbHelp(b),
        "cacheId", staticFiles.cacheId));
    return out;
  }

  private static final String HTML_NS = Namespaces.HTML_NAMESPACE_URI;
  private static Element renderVerbHelp(HtmlQuasiBuilder b) {
    Element verbs = b.getDocument().createElementNS(HTML_NS, "ul");
    verbs.setAttributeNS(HTML_NS, "class", "verbs-help");
    for (Verb v : Verb.values()) {
      Object reqPath = v.requestPath;
      if (v == Verb.INDEX) {
        reqPath = b.substV("<a href=@reqPath>@reqPath</a>", "reqPath", reqPath);
      }
      verbs.appendChild(b.substV(
          "<li><span class=verb>@reqPath</span>  &mdash; @manual @params</li>",
          "reqPath", reqPath,
          "manual", v.manual,
          "params", renderParamHelp(v, b)));
    }
    return verbs;
  }

  private static Element renderParamHelp(Verb v, HtmlQuasiBuilder b) {
    Element params = b.getDocument().createElementNS(HTML_NS, "table");
    String pn = "?";
    for (String name : Request.paramsAllowed(v)) {
      params.appendChild(b.substV(
          "<tr><th>@pn@name=&hellip;</th><td>@manual</td></tr>",
          "pn", pn,
          "name", name,
          "manual", Request.handler(v, name).manual()));
      pn = "&";
    }
    return params;
  }
}
