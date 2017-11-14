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

import com.google.caja.lexer.escaping.Escaping;
import com.google.caja.util.Join;

import java.io.IOException;
import java.io.Writer;

/**
 * Extracts and formats the rules of Caja from DefaultCajaRewriter
 * as a array which can be loaded and displayed by dojo
 *
 * @author jasvir@google.com (Jasvir Nagra)
 */
public class DojoRuleDoclet extends RuleDoclet {
  private StringBuilder dojoDocument;
  private StringBuilder table;
  private int countRules;

  @Override
  public String getDefaultExtension() {
    return "js";
  }

  @Override
  public void initialize(Writer output) {
    dojoDocument = new StringBuilder();
    dojoDocument.append("var data = ");
  }

  @Override
  public void generateHeader(Writer output, RulesetDescription ruleSet) {
    // no header
  }

  @Override
  public void generateFooter(Writer output, RulesetDescription ruleSet) {
    dojoDocument.append(table.toString());
    dojoDocument.append("]");
  }

  @Override
  public void finish(Writer output) throws IOException {
    output.write(dojoDocument.toString());
  }

  private static String escape(String js) {
    StringBuilder result = new StringBuilder();
    Escaping.escapeJsString(js, /* asciiOnly */ false, /* embeddable */ false,
        result);
    return result.toString();
  }

  private static String quote(String js) {
    return "'" + escape(js) + "'";
  }

  @Override
  public void generateRuleDocumentation(Writer output, RuleDescription anno) {
    if (0 == countRules) {
      table = new StringBuilder();
      table.append("[");
    }

    table.append("[");
    table.append(Join.join(",", ""+countRules++, quote(anno.name()),
        quote(anno.synopsis()), quote(anno.matches()),
        quote(anno.substitutes()), quote(anno.reason())));
    table.append("],\n");
  }
}