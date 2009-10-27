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

import com.google.caja.util.CajaTestCase;
import com.google.caja.util.Maps;
import com.google.caja.util.RhinoExecutor;
import com.google.caja.util.RhinoTestBed;

import java.util.Map;

import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

public class EnvironmentChecksTest extends CajaTestCase {
  public final void testEnvironmentData() throws Exception {
    Scriptable s = (Scriptable) RhinoTestBed.runJs(
        new RhinoExecutor.Input(  // TODO: make an ASCII-art Rhino.
            "var navigator = { userAgent: 'Rhino Rhino Rhino' };", "rhino-env"),
        new RhinoExecutor.Input(getClass(), "environment-checks.js"),
        new RhinoExecutor.Input("env", getName())
        );
    Map<String, Object> env = Maps.newHashMap();
    for (Object key : ScriptableObject.getPropertyIds(s)) {
      String code = "" + key;
      env.put(EnvironmentData.normJs(code, mq), s.get(code, s));
    }
    for (EnvironmentDatum datum : EnvironmentDatum.values()) {
      assertTrue(datum.name(), env.containsKey(datum.getCode()));
      System.err.println(datum + " = " + env.get(datum.getCode()));
    }
    assertEquals(
        "Rhino Rhino Rhino",
        env.get(EnvironmentDatum.NAV_USER_AGENT.getCode()));
  }
}
