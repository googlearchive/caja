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

package com.google.caja.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptableObject;

/**
 * A testbed that allows running javascript via the Rhino interpreter.
 * TODO: maybe replace this with the JSR 223 stuff.
 *
 * @author mikesamuel@gmail.com
 */
public class RhinoTestBed {

  /**
   * Runs the javascript from the given inputs in order, and returns the
   * result.
   */
  public static Object runJs(Input... inputs) throws IOException {
    Context context = Context.enter();
    try {
      ScriptableObject globalScope = context.initStandardObjects();
      Object stderr = Context.javaToJS(System.err, globalScope);
      ScriptableObject.putProperty(globalScope, "stderr", stderr);
      Object result = null;
      for (Input input : inputs) {
        result = context.evaluateReader(
            globalScope, input.input, input.source, 1, null);
      }
      return result;
    } finally {
      Context.exit();
    }
  }

  /** An input javascript file. */
  public static final class Input {
    public final Reader input;
    public final String source;
    public Input(Class<?> base, String resource) throws IOException {
      this.source = resource;
      InputStream instream = TestUtil.getResourceAsStream(base, resource);
      this.input = new InputStreamReader(instream , "UTF-8");
    }
    /** @param source file path or url from which the javascript came. */
    public Input(Reader input, String source) {
      this.input = input;
      this.source = source;
    }
  }

  private RhinoTestBed() { /* uninstantiable */ }
}
