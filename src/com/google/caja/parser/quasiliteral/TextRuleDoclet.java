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
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Date;

/**
 * Extracts and formats the rules of Caja from DefaultCajaRewriter 
 * output to the console
 * 
 * @author jasvir@google.com (Jasvir Nagra)
 */
public class TextRuleDoclet extends RuleDoclet {
  @Override
  public String getDefaultExtension() {
    return "txt";
  }

  @Override
  public void generateHeader(Writer output, RulesetDescription ruleSet) 
    throws IOException {
    output.write("Rules for " + ruleSet.name() + "\n");
    output.write(ruleSet.synopsis() + "\n");
  }

  @Override
  public void generateFooter(Writer output, RulesetDescription ruleSet) 
    throws IOException {
    output.write("Generated " + new Date());
  }

  @Override
  public void generateRuleDocumentation(Writer output, RuleDescription anno) throws IOException {
    output.write("  Rule: " + anno.name());
    output.write("\n  Synopsis: " + anno.synopsis());
    output.write("\n  Reason: " + anno.reason()); 
    output.write("\n  Matches: " + anno.reason()); 
    output.write("\n  Substitutes: " + anno.reason()); 
    output.append("\n\n");
  }
  
  public static void main(String[] args) throws IOException {
    TextRuleDoclet trd = new TextRuleDoclet();
    trd.setRewriter(new DefaultCajaRewriter(false));
    trd.generateDocumentation(new OutputStreamWriter(System.out));
  }
}