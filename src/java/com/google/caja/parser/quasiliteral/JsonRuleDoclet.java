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

import com.google.caja.util.Json;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.io.Writer;

/**
 * Extracts and formats the rules of Caja from DefaultCajaRewriter 
 * as a json page which can be loaded and displayed by webpage
 * 
 * @author jasvir@google.com (Jasvir Nagra)
 */
public class JsonRuleDoclet extends RuleDoclet {
  private JSONObject jsonDocument;
  private JSONArray table;
  private int countRules;

  @Override
  public String getDefaultExtension() {
    return "json";
  }
  
  @Override
  public void initialize(Writer output) {
    jsonDocument = new JSONObject();
  }
  
  @Override
  public void generateHeader(Writer output, RulesetDescription ruleSet) {
    JSONObject head = Json.formatAsJson("name", ruleSet.name(),
        "synopsis", ruleSet.synopsis());
    Json.putJson(jsonDocument, "header", head);
  }
  
  @Override
  public void generateFooter(Writer output, RulesetDescription ruleSet) {}
  
  @Override
  public void finish(Writer output) throws IOException {
    output.write(jsonDocument.toString());
  }

  @Override
  public void generateRuleDocumentation(Writer output, RuleDescription anno) {
    if (0 == countRules) {
      table = new JSONArray();
      Json.putJson(jsonDocument, "rules", table);
    }
    Json.pushJson(table, Json.formatAsJson("number", countRules++, 
        "name", anno.name(), "synopsis", anno.synopsis(),
        "reason", anno.reason(), "matches", anno.matches(), 
        "substitutes", anno.substitutes()));
  }
}