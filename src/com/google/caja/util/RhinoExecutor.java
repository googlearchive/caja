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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

import java.util.HashMap;
import java.util.Map;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.EcmaError;
import org.mozilla.javascript.ScriptableObject;

/**
 * Do not instantiate directly.  Use {@link Executor.Factory} instead.
 * This will be obsoleted once a JDK ships with builtin scripting language
 * support.
 *
 * @author mikesamuel@gmail.com
 */
public final class RhinoExecutor implements Executor {
  private final Executor.Input[] srcs;

  public RhinoExecutor(Executor.Input[] srcs) { this.srcs = srcs.clone(); }

  public <T> T run(Map<String, ?> actuals, Class<T> expectedResultType)
      throws AbnormalExitException {
    Context context = ContextFactory.getGlobal().enterContext();
    context.setOptimizationLevel(-1);
    try {
      return runInContext(context, actuals, expectedResultType);
    } finally {
      Context.exit();
    }
  }

  private <T> T runInContext(
      Context context, Map<String, ?> actuals, Class<T> expectedResultType)
      throws AbnormalExitException {
    ScriptableObject globalScope = context.initStandardObjects();
    try {
      globalScope.defineProperty(
          "scriptEngine___", new ScriptEngine(), ScriptableObject.DONTENUM);
      for (Map.Entry<String, ?> e : actuals.entrySet()) {
        globalScope.defineProperty(
            e.getKey(), e.getValue(), ScriptableObject.DONTENUM);
      }

      Object result = null;
      for (Input src : srcs) {
        String inputRead = drain(src.input);
        try {
          result = context.evaluateReader(
              globalScope, new StringReader(inputRead), src.source, 1, null);
        } catch (EcmaError ex) {
          System.err.println(withLineNums(inputRead));
          throw new AbnormalExitException(ex);
        }
        if (inputRead.length() > 500) { inputRead = "<ABREVIATED>"; }
      }
      if (result == null) { return null; }
      if (!expectedResultType.isInstance(result)) {
        result = Context.jsToJava(result, expectedResultType);
      }
      return expectedResultType.cast(result);
    } catch (IOException ex) {
      throw new AbnormalExitException(ex);
    }
  }

  private static final String drain(Reader r) throws IOException {
    char[] buf = new char[4096];
    StringBuilder sb = new StringBuilder();
    for (int n; (n = r.read(buf)) >= 0;) { sb.append(buf, 0, n); }
    r.close();
    return sb.toString();
  }

  private static final String withLineNums(String source) {
    StringBuilder sb = new StringBuilder();
    int ln = 0;
    for (String line : source.split("\r\n?|\n")) {
      sb.append(String.format("%04d: %s\n", ++ln, line));
    }
    return sb.toString();
  }

  public static class ScriptEngine {
    public void dontEnum(Object obj, String name) {
      if (obj instanceof ScriptableObject) {
        ((ScriptableObject) obj).setAttributes(name, ScriptableObject.DONTENUM);
      }
    }
  }

  /**
   * Allow parsing of content: URLs which can be useful for browser mocks
   * since it allows us to specify HTML in a URL which can be passed to env.js.
   * <p>
   * This registers a handler for the <code>content</code> protocol so that
   * {@code content:foo-bar} when loaded via {@code java.net.URL} will yield an
   * {@code InputStream} containing the UTF-8 encoding of the string
   * {@code "foo-bar"}.
   */
  public static void enableContentUrls() {
    // Force loading of class that registers a handler for content: URLs.
    SetupUrlHandlers.init();
  }
}

class SetupUrlHandlers {
  static {
    URL.setURLStreamHandlerFactory(new URLStreamHandlerFactory() {
        private Map<String, URLStreamHandler> handlers
            = new HashMap<String, URLStreamHandler>();

        // The below scheme for extending URL handlers is written according to
        // examples at:
        // http://www.webbasedprogramming.com
        // /Tricks-of-the-Java-Programming-Gurus/ch17.htm

        public URLStreamHandler createURLStreamHandler(String protocol) {
          protocol = Strings.toLowerCase(protocol);

          URLStreamHandler handler;
          synchronized (handlers) {
            handler = handlers.get(protocol);
            if (handler == null) {
              handler = createHandler(protocol);
            }
          }
          return handler;
        }

        private URLStreamHandler createHandler(String protocol) {
          if (Strings.equalsIgnoreCase("content", protocol)) {
            return new ContentUrlHandler();
          } else if ("http".equals(protocol) || "https".equals(protocol)) {
            // We could allow tests to stub out the internet, but
            // we definitely don't want unittests loading arbitrary URIs.
          } else {
            // Make sure that we support file: and jar: URIs so that
            // classloaders continue to work.
            try {
              String clname = "sun.net.www.protocol." + protocol + ".Handler";
              return (URLStreamHandler) Class.forName(clname).newInstance();
            } catch (Exception e) {
              System.err.println("No URL Handler for protocol " + protocol);
            }
          }
          return null;
        }
      });
  }

  public static void init() {}
}

class ContentUrlHandler extends URLStreamHandler {
  @Override
  protected URLConnection openConnection(URL url) {
    return new URLConnection(url) {
        private InputStream instream;

        @Override
        public void connect() {
          if (connected) { return; }
          connected = true;
          URI uri;
          try {
            uri = url.toURI();
          } catch (URISyntaxException ex) {
            ex.printStackTrace();
            return;
          }
          assert uri.isOpaque();
          try {
            instream = new ByteArrayInputStream(
                uri.getSchemeSpecificPart().getBytes("UTF-8"));
          } catch (UnsupportedEncodingException ex) {
            throw (AssertionError) new AssertionError(
                "UTF-8 not supported").initCause(ex);
          }
        }

        @Override
        public InputStream getInputStream() {
          if (instream == null) { throw new IllegalStateException(); }
          return instream;
        }
      };
  }
}
