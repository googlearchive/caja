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
import com.google.caja.plugin.BrowserTestCatalog;
import com.google.common.collect.Maps;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import com.google.caja.tracing.TracingRewriterTest;
import org.mozilla.javascript.ClassShutter;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.EcmaError;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.WrapFactory;
import org.mozilla.javascript.WrappedException;
import org.w3c.dom.DOMException;
import org.xml.sax.SAXException;

/**
 * Do not instantiate directly.  Use {@link Executor.Factory} instead.
 * This will be obsoleted once a JDK ships with built-in scripting language
 * support and proper sand-boxing.
 *
 * @author mikesamuel@gmail.com
 */
public final class RhinoExecutor implements Executor {
  private final Executor.Input[] srcs;

  public RhinoExecutor(Executor.Input[] srcs) { this.srcs = srcs.clone(); }

  private static final Set<String> OBJECT_CLASS_MEMBERS = Sets.newHashSet(
      // We allow toString since that is part of JS as well, typically has
      // no side effect, and returns a JS primitive type.
      "class", "clone", "equals", "finalize", "getClass", "hashCode",
      "notify", "notifyAll", "wait");

  private static final Set<String> CLASS_WHITELIST = Sets.newHashSet(
      "junit.framework.AssertionFailedError",
      Boolean.class.getName(),
      ByteArrayInputStream.class.getName(),
      Connection.class.getName(),
      Character.class.getName(),
      DOMException.class.getName(),
      Double.class.getName(),
      BrowserTestCatalog.ParserOutput.class.getName(),
      EcmaError.class.getName(),
      EvaluatorException.class.getName(),
      Float.class.getName(),
      Integer.class.getName(),
      JavaScriptException.class.getName(),
      Long.class.getName(),
      PrintStream.class.getName(),
      RhinoException.class.getName(),
      ScriptPowerBox.class.getName(),
      Short.class.getName(),
      String.class.getName(),
      Timer.class.getName(),
      URI.class.getName(),
      WeakHashMap.class.getName(),
      WrappedException.class.getName(),
      "org.apache.xerces.*",
      "com.google.caja.parser.quasiliteral.ES53ConformanceTest$Caja",
      TracingRewriterTest.TestCollector.class.getName());

  private static final ContextFactory SANDBOXINGFACTORY = new ContextFactory() {
    @Override
    protected Context makeContext() {
      // Implement Rhino sandboxing as explained at
      //     http://codeutopia.net/blog/2009/01/02/sandboxing-rhino-in-java/
      // plus a few extra checks.
      Context context = super.makeContext();
      context.setClassShutter(new ClassShutter() {
        public boolean visibleToScripts(String fullClassName) {
          if (fullClassName.endsWith("SandBoxSafe")) { return true; }
          if (CLASS_WHITELIST.contains(fullClassName)) { return true; }
          for (int dot = fullClassName.length();
               (dot = fullClassName.lastIndexOf('.', dot - 1)) >= 0;) {
            if (CLASS_WHITELIST.contains(
                    fullClassName.substring(0, dot + 1) + "*")) {
              return true;
            }
          }
          if (fullClassName.matches("[A-Z]")) {  // is a class, not a package
            System.err.println(
                "RhinoExecutor denied access to " + fullClassName);
          }
          return false;
        }
      });
      context.setWrapFactory(new WrapFactory() {
        @Override
        public Object wrap(
            Context cx, Scriptable scope, Object javaObject,
            @SuppressWarnings("rawtypes")  // Overridden method is not generic
            Class staticType) {
          // Make java arrays behave like native JS arrays.
          // This breaks EQ, but is better than the alternative.
          if (javaObject instanceof Object[]) {
            Object[] javaArray = (Object[]) javaObject;
            int n = javaArray.length;
            Object[] wrappedElements = new Object[n];
            Class<?> compType = javaArray.getClass().getComponentType();
            for (int i = n; --i >= 0;) {
              wrappedElements[i] = wrap(cx, scope, javaArray[i], compType);
            }
            NativeArray jsArray = new NativeArray(wrappedElements);
            jsArray.setPrototype(
                ScriptableObject.getClassPrototype(scope, "Array"));
            jsArray.setParentScope(scope);
            return jsArray;
          }

          // Deny reflective access up front.  This should not be triggered due
          // to getter filtering, but let's be paranoid.
          if (javaObject != null
              && (javaObject instanceof Class<?>
              || javaObject instanceof ClassLoader
              || "java.lang.reflect".equals(
              javaObject.getClass().getPackage().getName()))) {
            return Context.getUndefinedValue();
          }

          return super.wrap(cx, scope, javaObject, staticType);
        }

        @Override
        public Scriptable wrapAsJavaObject(
            Context cx, Scriptable scope, Object javaObject,
            @SuppressWarnings("rawtypes")  // Overridden method is not generic
            Class staticType) {
          return new NativeJavaObject(scope, javaObject, staticType) {
            @Override
            public Object get(String name, Scriptable start) {
              // Deny access to all members of the base Object class since
              // some of them enable reflection, and the others are mostly for
              // serialization and timing which should not be accessible.
              // The codeutopia implementation only blacklists getClass.
              if (OBJECT_CLASS_MEMBERS.contains(name)) { return NOT_FOUND; }
              return super.get(name, start);
            }
          };
        }
      });
      return context;
    }
    @Override
    public boolean hasFeature(Context c, int feature) {
      switch (feature) {
        case Context.FEATURE_LOCATION_INFORMATION_IN_ERROR: return true;
        case Context.FEATURE_E4X: return false;
        case Context.FEATURE_ENHANCED_JAVA_ACCESS: return false;
        //case Context.FEATURE_PARENT_PROTO_PROPERTIES: return false;
        default: return super.hasFeature(c, feature);
      }
    }
  };
  static {
    ContextFactory.initGlobal(SANDBOXINGFACTORY);
  }

  public <T> T run(Map<String, ?> actuals, Class<T> expectedResultType)
      throws AbnormalExitException {
    if (SANDBOXINGFACTORY != ContextFactory.getGlobal()) {
      throw new IllegalStateException();
    }
    Context context = SANDBOXINGFACTORY.enterContext();
    // Don't bother to compile tests to a class file.  Removing this causes
    // a 5x slow-down in Rhino-heavy tests.
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
          "scriptEngine___", new ScriptPowerBox(context, globalScope),
          ScriptableObject.DONTENUM);
      Object eval = actuals.remove("eval___");
      ScriptableObject.putProperty(globalScope, "eval___", eval);
      for (Map.Entry<String, ?> e : actuals.entrySet()) {
        globalScope.defineProperty(
            e.getKey(),
            Context.javaToJS(e.getValue(), globalScope),
            ScriptableObject.DONTENUM);
      }

      Object result = null;
      synchronized (context) {
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

  // Methods are accessed reflectively by script engine.
  @SuppressWarnings("static-method")
  public static class ScriptPowerBox {
    private final Context cx;
    private final Scriptable global;

    ScriptPowerBox(Context cx, Scriptable global) {
      this.cx = cx;
      this.global = global;
    }

    public void dontEnum(Object obj, String name) {
      if (obj instanceof ScriptableObject) {
        ((ScriptableObject) obj).setAttributes(name, ScriptableObject.DONTENUM);
      }
    }

    // Some capabilities needed by env.js
    public URI currentLocation() {
      return new File("./").toURI();
    }

    public URI uri(String s) {
      return URI.create(s);
    }

    public Timer timer(Function fn, double delta) {
      return new Timer(cx, global, global, fn, delta);
    }

    public Object parseDom(InputStream in) throws IOException, SAXException {
      try {
        return DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(in);
      } catch (ParserConfigurationException ex) {
        throw new SomethingWidgyHappenedError(ex);
      }
    }

    public InputStream streamFromString(String str) {
      try {
        return new ByteArrayInputStream(
            str.getBytes(Charsets.UTF_8.name()));
      } catch (UnsupportedEncodingException ex) {
        throw new SomethingWidgyHappenedError(ex);
      }
    }

    public Map<Object, Object> weakMap() {
      return new WeakHashMap<Object, Object>();
    }

    /**
     * @param headers available in case we want to support http/https
     * @param responseHeaders available in case we want to support http/https
     */
    public Connection openConnection(
        URI uri, Object headers, Object responseHeaders) {
      String scheme = Strings.lower(uri.getScheme());
      int status;
      String statusText;
      String responseBody;
      try {
        if ("file".equals(scheme)) {
          // Load using the classpath, since the classpath is already limited
          // to files openable by the JVM, and does not allow deletion or
          // writing.
          String path = uri.getPath();

          StringBuilder sb = new StringBuilder();

          Reader in = new InputStreamReader(
              getClass().getClassLoader().getResourceAsStream(path), "UTF-8");
          try {
            char[] buf = new char[4096];
            for (int n; (n = in.read(buf)) > 0;) {
              sb.append(buf, 0, n);
            }
          } finally {
            in.close();
          }
          responseBody = sb.toString();
        } else if ("content".equals(scheme)) {
          responseBody = uri.getSchemeSpecificPart();
        } else {
          throw new IllegalArgumentException(scheme);
        }
        status = 200;
        statusText = "OK";
      } catch (FileNotFoundException ex) {
        status = 404;
        statusText = "Not Found";
        responseBody = "";
      } catch (IOException ex) {
        status = 500;
        statusText = "Access Denied";
        responseBody = "";
      }
      return new Connection(status, statusText, responseBody);
    }
  }

  private static final ScheduledExecutorService executorService
      = Executors.newSingleThreadScheduledExecutor();
  private static final Object[] ZERO_ARGS = new Object[0];
  public static class Timer {
    private final Context cx;
    private final Scriptable global;
    private final Scriptable scope;
    private final Function fn;
    private final long deltaMillis;
    private ScheduledFuture<?> future;

    Timer(Context cx, Scriptable global, Scriptable scope, Function fn,
          double deltaMillis) {
      this.cx = cx;
      this.global = global;
      this.scope = scope;
      this.fn = fn;
      this.deltaMillis = (long) deltaMillis;
    }

    public void start() {
      if (future != null) { throw new IllegalStateException(); }
      future = executorService.scheduleWithFixedDelay(new Runnable() {
        public void run() {
          synchronized (cx) {
            fn.call(cx, scope, global, ZERO_ARGS);
          }
        }
      }, deltaMillis, deltaMillis, TimeUnit.MILLISECONDS);
    }

    public void stop() {
      if (future == null) { throw new IllegalStateException(); }
      future.cancel(false);
      future = null;
    }
  }

  public static final class Connection {
    private final int status;
    private final String statusText;
    private final String responseBody;

    Connection(int status, String statusText, String responseBody) {
      this.status = status;
      this.statusText = statusText;
      this.responseBody = responseBody;
    }

    public int getStatus() { return status; }
    public String getStatusText() { return statusText; }
    public String getResponseBody() { return responseBody; }
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
        private final Map<String, URLStreamHandler> handlers = Maps.newHashMap();

        // The below scheme for extending URL handlers is written according to
        // examples at:
        // http://www.webbasedprogramming.com
        // /Tricks-of-the-Java-Programming-Gurus/ch17.htm

        public URLStreamHandler createURLStreamHandler(String protocol) {
          protocol = Strings.lower(protocol);

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
          protocol = Strings.lower(protocol);
          if ("content".equals(protocol)) {
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

  public static void init() { /* noop */ }
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
                uri.getSchemeSpecificPart().getBytes(Charsets.UTF_8.name()));
          } catch (UnsupportedEncodingException ex) {
            throw new SomethingWidgyHappenedError(
                "UTF-8 not supported", ex);
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
