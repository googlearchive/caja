// Copyright (C) 2005 Google Inc.
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

package com.google.caja.reporting;

/**
 * The seriousness of a {@link Message}.
 *
 * @author mikesamuel@gmail.com
 */
public enum MessageLevel {
  /** Fine grained info about the internal progress of the Cajoler. */
  LOG,
  /** Broad info about the internal progress of the Cajoler. */
  SUMMARY,
  /** Information inferred about source files. */
  INFERENCE,
  /** Indicative of a possible problem in an input source file. */
  LINT,
  /** Indicative of a probable problem in an input source file. */
  WARNING,
  /**
   * Indicative of a part of an input that prevents the Cajoler from producing
   * a usable output, but the Cajoler could continue to the next stage to
   * produce messages that might shed more info on the problem.
   */
  ERROR,
  /**
   * Like {@link #ERROR} but the problem is so serious there's no reason to
   * proceed to the next processing stage.
   */
  FATAL_ERROR,
  ;
}
