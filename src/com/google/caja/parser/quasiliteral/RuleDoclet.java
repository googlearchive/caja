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
   * and may be overridden to initialize variables or open files
   *
   * @param output a stream to which documentation can be written.
   */
  public void initialize(Writer output) {}

  /**
   * Initializes the RuleDoclet
   *
   * This method is called before any documentation generation occurs
   * and may be overridden to initialize variables or open files
   *
   * @param output a stream to which documentation can be written.
   * @throws IOException if any operation on output raises an IOException.
   */
  @SuppressWarnings("unused")
  public void finish(Writer output) throws IOException {}

  /**
   * Returns the default file extension for the format output by this doclet
   */
  public abstract String getDefaultExtension();


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
  public abstract void generateRuleDocumentation(
      Writer output, RuleDescription rule)
    throws IOException;

  /**
   * Emits documentation for a set of rules
   * @throws IOException
   */
  public void generateDocumentation(Writer output)
    throws IOException {
    try {
      RulesetDescription ruleSetDescription =
          rewriter.getClass().getAnnotation(RulesetDescription.class);
      initialize(output);
      generateHeader(output, ruleSetDescription);
      for (Rule rule : rewriter.getRules()) {
        RuleDescription anno = rule.getRuleDescription();
        if (anno != null) {
          generateRuleDocumentation(output, anno);
        }
      }
      generateFooter(output, ruleSetDescription);
      finish(output);
    } catch (RuntimeException ex) {
      ex.printStackTrace();  // ANT hides exceptions otherwise
      throw ex;
    }
  }
}
