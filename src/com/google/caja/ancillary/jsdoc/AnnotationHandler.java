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

package com.google.caja.ancillary.jsdoc;

import com.google.caja.parser.js.Expression;
import com.google.caja.reporting.MessageQueue;

/**
 * Resolves a part of a comment to a javascript expression that will produce
 * either part of the doc tree, or an error message.
 * <p>
 * The expression can be a JSON value that will show up in the Jsdoc output,
 * or it can be a function that produces a JSON value.  The function will be
 * executed after the code has been executed, and will be defined in the scope
 * in which the comment appears.
 * <p>
 * The {@code jsdoc___} API defined in <tt>jsdoc.js</tt> is available to the
 * returned expression.  See especially the {@code warn}, {@code error}, and
 * {@code later} functions.
 *
 * @author mikesamuel@gmail.com
 */
public interface AnnotationHandler {
  /**
   * @param a the annotation to handle.
   * @param mq receives messages about problems in the annotation.  If the
   *     annotation could not be handled (returns null), this MUST receive
   *     {@link com.google.caja.reporting.MessageLevel#ERROR}.
   * @return an Expression that will resolve to a JSON value, or null if the
   *     annotation could not be handled.
   *     If the expression evaluates to a function, that function will be
   *     called to produce a JSON value after the rewritten code has executed.
   */
  Expression handle(Annotation a, MessageQueue mq);
}
