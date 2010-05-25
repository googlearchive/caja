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

package com.google.caja.util;

import com.google.caja.SomethingWidgyHappenedError;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.Map;

/**
 * Abstracts away execution of script.
 *
 * @author mikesamuel@gmail.com
 */
public interface Executor {
  /**
   * Execute in the context of the given bindings and coerce the result to the
   * given type.
   * @throws AbnormalExitException if the script could not produce a result.
   * @throws ClassCastException if the result could not be coerced to the
   *    expectedReturnType.
   */
  public <T> T run(Map<String, ?> actuals, Class<T> expectedResultType)
      throws AbnormalExitException;

  public static class AbnormalExitException extends Exception {
    public AbnormalExitException(String message) { super(message); }
    public AbnormalExitException(Throwable cause) { super(cause); }
    public AbnormalExitException(String message, Throwable cause) {
      super(message, cause);
    }

    public String getScriptTrace() {
      Throwable th = this;
      if (th.getCause() != null) { th = th.getCause(); }
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      th.printStackTrace(pw);
      pw.flush();
      return sw.toString();
    }
  }

  public static class MalformedSourceException extends Exception {
    public MalformedSourceException(String message) { super(message); }
    public MalformedSourceException(Throwable cause) { super(cause); }
    public MalformedSourceException(String message, Throwable cause) {
      super(message, cause);
    }
  }

  public static final class Factory {
    public static Executor createJsExecutor(Input... srcs)
        throws MalformedSourceException {
      Throwable cause;
      try {
        Class<? extends Executor> execClass = Class.forName(System.getProperty(
            "com.google.caja.util.Executor.JS.class",
            "com.google.caja.util.RhinoExecutor"))
            .asSubclass(Executor.class);
        return execClass.getConstructor(Input[].class)
            .newInstance((Object) srcs);
      } catch (InvocationTargetException ex) {
        throw new MalformedSourceException(ex);
      } catch (ClassNotFoundException ex) {
        cause = ex;
      } catch (IllegalAccessException ex) {
        cause = ex;
      } catch (InstantiationException ex) {
        cause = ex;
      } catch (NoSuchMethodException ex) {
        cause = ex;
      }
      throw new SomethingWidgyHappenedError("Can't recover from bad config",
          cause);
    }

    private Factory() { /* not instantiable */ }
  }

  /** An input javascript file. */
  public static final class Input {
    public final Reader input;
    public final String source;
    /** @param source file path or url from which the javascript came. */
    public Input(Reader input, String source) {
      this.input = input;
      this.source = source;
    }

    public Input(String javascript, String source) {
      this(new StringReader(javascript), source);
    }

    public Input(Class<?> base, String resource) throws IOException {
      URL url = base.getResource(resource);
      if (null == url) {
        throw new FileNotFoundException(
            "Resource " + resource + " relative to " + base);
      }
      this.source = url.toString();
      InputStream instream = base.getResourceAsStream(resource);

      this.input = new InputStreamReader(instream , Charsets.UTF_8.name());
    }

    @Override
    public String toString() { return "(Input " + source + ")"; }
  }
}
