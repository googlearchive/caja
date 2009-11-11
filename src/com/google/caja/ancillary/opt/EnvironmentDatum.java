// Copyright (C) 2009 Google Inc.
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

package com.google.caja.ancillary.opt;

import com.google.caja.lexer.ParseException;

/**
 * Symbolic names for snippets of code that give information about the quirks
 * of the interpreter the code is running in, and the API available to programs
 * run in that environment.
 *
 * @author mikesamuel@gmail.com
 */
public enum EnvironmentDatum {
  NAV_USER_AGENT("navigator.userAgent"),
  IS_WINDOW_GLOBAL("!!this.window && this === window"),
  ES5_STRICT_AVAILABLE("!(function () { return this; }.call(null))"),
  NEG_INDICES_SAFE("void 0 === ((function () {})[-2])"),
  FN_CTORS_PURE(
      "void 0 === ((function () { var b, a = function b(){}; return b; })())"),
  CATCH_BLOCK_SCOPED(
      "(function () { var e=true; try{throw false; }catch(e){} return e; })()"),
  FN_INIT_HOISTED(
      "function () { var a; if (0) function a(){} return void 0 === a; }()"),
  ;

  private final String js;

  EnvironmentDatum(String js) {
    try {
      this.js = EnvironmentData.normJs(js, EnvironmentData.LOUD_MQ);
    } catch (ParseException ex) {
      throw new RuntimeException(ex);
    }
  }

  public String getCode() { return js; }
}
