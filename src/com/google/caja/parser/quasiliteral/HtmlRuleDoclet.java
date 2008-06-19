// Copyright (C) 2008 Google Inc.
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

package com.google.caja.parser.quasiliteral;

import java.io.Writer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;

/**
 * Extracts and formats the rules of Caja from DefaultCajaRewriter
 * as a html page output to the given file
 *
 * @author jasvir@google.com (Jasvir Nagra)
 */
public class HtmlRuleDoclet extends RuleDoclet {
  private Document htmlDocument;
  private Element body;
  private Element table;
  private int countRules;

  @Override
  public String getDefaultExtension() {
    return "html";
  }

  @Override
  public void initialize(Writer output) {
    DOMImplementation impl;
    try {
      impl = DOMImplementationRegistry.newInstance().getDOMImplementation(null);
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
    String qname = "html";
    String systemId = "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd";
    String publicId = "-//W3C//DTD XHTML 1.0 Transitional//EN";
    htmlDocument = impl.createDocument(
        systemId, qname, impl.createDocumentType(qname, publicId, systemId));
    Element root = htmlDocument.createElement("html");
    htmlDocument.appendChild(root);
  }

  private Element headerRow(String... cells) {
    Element thead = htmlDocument.createElement("thead");
    thead.appendChild(row(cells));
    return thead;
  }

  private Element row(String... cells) {
    Element tr = htmlDocument.createElement("tr");
    for (String cell : cells) {
      Element td = htmlDocument.createElement("td");
      td.appendChild(htmlDocument.createTextNode(cell));
      tr.appendChild(td);
    }
    return tr;
  }

  @Override
  public void generateHeader(Writer output, RulesetDescription ruleSet) {
    Element head = htmlDocument.createElement("head");
    Element title = htmlDocument.createElement("title");
    title.appendChild(htmlDocument.createTextNode(ruleSet.name()));
    head.appendChild(title);
    Element meta = htmlDocument.createElement("meta");
    meta.setAttribute("http-equiv", "Content-Type");
    meta.setAttribute("content", "text/html; charset=utf-8");
    head.appendChild(meta);

    Element style = htmlDocument.createElement("style");
    style.setAttribute("type", "text/css");
    style.appendChild(htmlDocument.createTextNode(
        "h1 { text-align: center }\n"
        + "div.centered { text-align: center }\n"
        + "div.centered table { margin: 0 auto; text-align: left }\n"));
    head.appendChild(style);
    htmlDocument.getDocumentElement().appendChild(head);

    body = htmlDocument.createElement("body");
    Element h1 = htmlDocument.createElement("h1");
    h1.appendChild(htmlDocument.createTextNode(ruleSet.name()));
    body.appendChild(h1);

    Element h2 = htmlDocument.createElement("h2");
    h2.appendChild(htmlDocument.createTextNode(ruleSet.synopsis()));
    body.appendChild(h2);
    htmlDocument.getDocumentElement().appendChild(body);
  }

  @Override
  public void generateFooter(Writer output, RulesetDescription ruleSet) {}

  @Override
  public void finish(Writer output) {
    DOMSource src = new DOMSource(htmlDocument);
    StreamResult result = new StreamResult(output);
    try {
      TransformerFactory.newInstance().newTransformer().transform(src, result);
    } catch (TransformerException ex) {
      throw new RuntimeException(ex);
    }
  }

  @Override
  public void generateRuleDocumentation(Writer output, RuleDescription anno) {
    if (0 == countRules) {
      table = htmlDocument.createElement("table");
      table.appendChild(headerRow("", "Rule", "Synopsis", "Reason", "Matches",
                                  "Substitutes"));
      body.appendChild(table);
    }
    table.appendChild(row("" + countRules++, anno.name(), anno.synopsis(),
                          anno.reason(), anno.matches(), anno.substitutes()));
  }
}
