// Copyright (C) 2007 Google Inc.
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

import com.sun.javadoc.DocErrorReporter;
import com.sun.javadoc.Doclet;
import com.sun.javadoc.LanguageVersion;
import com.sun.javadoc.RootDoc;
import com.sun.tools.doclets.internal.toolkit.util.DocletAbortException;

import java.lang.reflect.Method;

/**
 * Generates list of the rules
 * TODO(jasvir): Generate javadoc style document pages
 *
 * @author jasvir@google.com (Jasvir Nagra)
 */
public class RuleDoclet extends Doclet {

  public static LanguageVersion languageVersion() {
    return LanguageVersion.JAVA_1_5;
  }

  public static boolean validOptions(String[][] options,
                                     DocErrorReporter reporter) {
    return true;
  }

  public static int optionLength(String option) {
    if ( option.equals("-d") ) {
      return 2;
    }
    return Doclet.optionLength(option);
  }

  public static boolean start(RootDoc root) {
    try {
      (new RuleDoclet()).processRoot(root);
    } catch (DocletAbortException exc) {
      return false;
    }
    return true;
  }

  public void processRoot(RootDoc root) {
    DefaultCajaRewriter dcr = new DefaultCajaRewriter();

    for ( Object oc : dcr.cajaRules ) {
      Class<?> c = oc.getClass();

      for ( Method mm : c.getDeclaredMethods() ) {
        RuleDescription anno = mm.getAnnotation(RuleDescription.class);
        if ( anno != null ) {
          System.out.println("Rule: " + anno.name());
          System.out.println("Synopsis: " + anno.synopsis());
          System.out.println("Reason: " + anno.reason());
          System.out.println();
        }
      }
    }
  }
}