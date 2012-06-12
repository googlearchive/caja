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

package com.google.caja;

import com.google.caja.util.CajaTestCase;
import com.google.caja.util.Executor;
import com.google.caja.util.Join;
import com.google.caja.util.RhinoTestBed;
import java.io.IOException;

public class ES53RhinoTest extends CajaTestCase {

  public final void testDefRecursive() {
    runJs(
        "var a = { b: { a: null }, c: [[1]] };",
        "___.whitelistAll(a, true);",
        "a.b.a = a;",
        "a.c.push(a);",
        "cajaVM.def(a);",
        "assertTrue('a', Object.isFrozen(a));",
        "assertTrue('a.b', Object.isFrozen(a.b));",
        "assertTrue('a.c', Object.isFrozen(a.c));",
        "assertTrue('a.c[0]', Object.isFrozen(a.c[0]));",
        "assertTrue('a === a.b.a', a === a.b.a);",
        "assertTrue('a === a.c[1]', a === a.c[1]);");
  }

  protected Object runJs(String... statements) {
    String js = Join.join("\n", statements);
    try {
      Object result = RhinoTestBed.runJs(
          new Executor.Input(
              getClass(), "/com/google/caja/plugin/console-stubs.js"),
          new Executor.Input(
              getClass(), "/js/json_sans_eval/json_sans_eval.js"),
          new Executor.Input(getClass(), "/com/google/caja/es53.js"),
          new Executor.Input(
              getClass(), "/js/jsunit/2.2/jsUnitCore.js"),
          new Executor.Input(
              getClass(), "/com/google/caja/log-to-console.js"),
          new Executor.Input(js, getName()));
      return result;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
