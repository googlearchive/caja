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

import java.io.IOException;
import java.io.Writer;

/**
 * Extracts and formats the rules of Caja from DefaultCajaRewriter 
 * as a wiki page output to the given file
 * 
 * @author jasvir@google.com (Jasvir Nagra)
 */
public class WikiRuleDoclet extends RuleDoclet {
  private int countRules = 0;
  
  private String heading1(String name) {
    return "=" + name + "=\n";
  }
  
  private String heading2(String name) {
    return "==" + name + "==\n";
  }
  
  private String row(String... cells) {
    StringBuilder result = new StringBuilder();
    boolean hasContent = false;
    for (String cell : cells) {
      if (cell.equals("")) {
        cell = " ";
      }
      result.append("||" + cell);
      hasContent = true;
    }
    if (hasContent) {
      result.append("||\n");
    }
    return result.toString();
  }

  private String code(String code) {
    return code.equals("") ? " " : "{{{" + code + "}}}";
  }
  
  @Override
  public void generateHeader(Writer output, RulesetDescription ruleSet) 
    throws IOException {
    output.write(heading1(ruleSet.name()));
    output.write(heading2(ruleSet.synopsis()));
  }

  @Override
  public void generateFooter(Writer output, RulesetDescription ruleSet) {}

  @Override
  public void generateRuleDocumentation(Writer output, RuleDescription anno) throws IOException {
    if (countRules == 0) {
      output.write(row("", "Rule", "Synopsis", "Reason", "Matches", "Substitutes"));
    }
    output.write(row("" + countRules++, anno.name(), anno.synopsis(), 
                          anno.reason(), code(anno.matches()), code(anno.substitutes())));
  }
}