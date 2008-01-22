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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

import junit.framework.Assert;

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
   * If dumpJsFile is not null, also put all the javascript in that file.
   */
  public static Object runJs(final String dumpJsFile, Input... inputs)
  throws IOException {
    Context context = Context.enter();
    try {
      ScriptableObject globalScope = context.initStandardObjects();
      Object stderr = Context.javaToJS(System.err, globalScope);
      ScriptableObject.putProperty(globalScope, "stderr", stderr);
      Object result = null;

      if (dumpJsFile != null) {
        String allInputs = "";
        for (Input input : inputs) {
          allInputs += readReader(input.input);
        }
        writeFile(new File("/tmp/js.all"), allInputs);
        Input input = new Input(new StringReader(allInputs), "all");
        result = context.evaluateReader(
            globalScope, input.input, input.source, 1, null);
      } else {
        for (Input input : inputs) {
          result = context.evaluateReader(
              globalScope, input.input, input.source, 1, null);
        }
      }
      return result;
    } catch (org.mozilla.javascript.JavaScriptException e) {
      Assert.fail(e.details() + "\n" + e.getScriptStackTrace());
      return null;
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

  private static String readReader(Reader reader) {
    Reader r;
    r = new BufferedReader(reader);
    Writer w = new StringWriter();

    try {
      for (int c; (c = r.read()) != -1; ) w.write(c);
    } catch (IOException e)  {
      throw new RuntimeException(e);
    }

    try {
      r.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    return w.toString();
  }

  private static void writeFile(File path, String contents) {
    Writer w;
    try {
      w = new BufferedWriter(new FileWriter(path, false));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    try {
      w.write(contents);
      if (contents.length() > 0 && !contents.endsWith("\n")) {
        w.write("\n");
      }
    } catch (IOException e)  {
      throw new RuntimeException(e);
    }

    try {
      w.flush();
      w.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }


  private RhinoTestBed() { /* uninstantiable */ }
}
