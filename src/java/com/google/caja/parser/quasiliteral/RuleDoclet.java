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
import java.lang.reflect.Method;

/**
 * Extracts and formats the rules of Caja from DefaultCajaRewriter 
 * 
 * @author jasvir@google.com (Jasvir Nagra)
 */
public abstract class RuleDoclet {
  
  protected Rewriter rewriter;
  
  /**
   * Sets the rewriter this {@code RuleDoclet} documents 
   */
  public void setRewriter(Rewriter rewriter) {
    this.rewriter = rewriter;
  }
  
  /**
   * Initializes the RuleDoclet
   * 
   * This method is called before any documentation generation occurs
   * and overriden to initialize variables or open files  
   */
  public void initialize(Writer output) {}

  /**
   * Initializes the RuleDoclet
   * 
   * This method is called before any documentation generation occurs
   * and overriden to initialize variables or open files  
   */
  @SuppressWarnings("unused")
  public void finish(Writer output) throws IOException {}

  
  /**
   * Emits a documentation about {@code ruleSet} into the header
   * of the document being written to in {@code output}
   */
  public abstract void generateHeader(Writer output, RulesetDescription ruleSet) 
    throws IOException;

  /**
   * Emits a documentation about {@code ruleSet} into the header
   * of the document being written to in {@code output}
   */
  public abstract void generateFooter(Writer output, RulesetDescription ruleSet) 
    throws IOException;

  /**
   * Emits a documentation describing {@code rule} to {@code output} 
   */
  public abstract void generateRuleDocumentation(Writer output, RuleDescription rule) 
    throws IOException;
  
  /**
   * Emits documentation for a set of rules 
   * @throws IOException 
   */
  protected void generateDocumentation(Writer output) 
    throws IOException {
    RulesetDescription ruleSetDescription = 
      rewriter.getClass().getAnnotation(RulesetDescription.class);
    initialize(output);
    generateHeader(output, ruleSetDescription);
    for (Object oc : rewriter.getRules()) {
      Class<?> c = oc.getClass();
      boolean annotated = false;
      for (Method mm : c.getMethods()) {
        RuleDescription anno = mm.getAnnotation(RuleDescription.class);
        if (anno != null) {
          if (!mm.getName().equals("fire")) {
            throw new RuntimeException("RuleDescription should only be used to annotate the \"fire\" method, not " + mm.getName());
          }
          if (!annotated) {
            generateRuleDocumentation(output, anno);
            annotated = true;
          } else {
            throw new RuntimeException("RuleDescription annotation used more than once in the same rule");            
          }     
        }
      }
    }
    generateFooter(output, ruleSetDescription);
    finish(output);
  }
}