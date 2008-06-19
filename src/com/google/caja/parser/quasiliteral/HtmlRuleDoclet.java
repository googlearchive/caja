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

import org.jdom.Attribute;
import org.jdom.DocType;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import java.io.IOException;
import java.io.Writer;

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
    htmlDocument = new Document();
    DocType type = new DocType("html", "-//W3C//DTD XHTML 1.0 Transitional//EN", 
                               "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd");
    htmlDocument.setDocType(type);
    Element root = new Element("html");
    htmlDocument.setRootElement(root);
  }

  private Element headerRow(String... cells) {
    Element thead = new Element("thead");
    thead.addContent(row(cells));
    return thead;
  }
  
  private Element row(String... cells) {
    Element tr = new Element("tr");    
    for (String cell : cells) {
      Element td = new Element("td");
      td.setText(cell);
      tr.addContent(td);
    }
    return tr;
  }
  
  @Override
  public void generateHeader(Writer output, RulesetDescription ruleSet) {
    Element head = new Element("head");
    head.addContent(new Element("title").setText(ruleSet.name()));
    Element meta = new Element("meta");
    meta.setAttribute(new Attribute("http-equiv", "Content-Type"));
    meta.setAttribute(new Attribute("content", "text/html; charset=utf-8"));
    head.addContent(meta);
    
    Element style = new Element("style");
    style.setAttribute(new Attribute("type", "text/css"));
    style.setText("h1 { text-align: center; } " +
                  "div.centered {text-align: center;} " +
                  "div.centered table {margin: 0 auto; text-align: left;}" +
                  "}"); 
    head.addContent(style);
    htmlDocument.getRootElement().addContent(head);

    body = new Element("body");
    Element h1 = new Element("h1");
    h1.setText(ruleSet.name());
    body.addContent(h1);
    
    Element h2 = new Element("h2");
    h2.setText(ruleSet.synopsis());
    body.addContent(h2);
    htmlDocument.getRootElement().addContent(body);
  }
  
  @Override
  public void generateFooter(Writer output, RulesetDescription ruleSet) {}
  
  @Override
  public void finish(Writer output) throws IOException {
    XMLOutputter prettyHtml = new XMLOutputter(Format.getPrettyFormat());
    prettyHtml.output(htmlDocument, output);
  }

  @Override
  public void generateRuleDocumentation(Writer output, RuleDescription anno) {
    if (0 == countRules) {
      table = new Element("table");
      table.addContent(headerRow("", "Rule", "Synopsis", "Reason", "Matches",
                                 "Substitutes"));
      body.addContent(table);
    }
    table.addContent(row("" + countRules++, anno.name(), anno.synopsis(), 
                          anno.reason(), anno.matches(), anno.substitutes()));
  }
}