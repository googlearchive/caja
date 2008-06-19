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

import com.google.caja.parser.js.StringLiteral;

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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.Assert;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptableObject;

/**
 * A testbed that allows running javascript via the Rhino interpreter.
 * TODO(mikesamuel): maybe replace this with the JSR 223 stuff.
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
        writeFile(new File(dumpJsFile), allInputs);
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

  /**
   * Given an HTML file that references javascript sources, load all
   * the scripts, set up the DOM using env.js, and start JSUnit.
   *
   * This lets us write test html files that can be run both
   * in a browser, and automatically via ANT.
   *
   * @param base the class which should be used to resolve javascript files as
   *   classpath resources.
   * @param htmlResource path to html file relative to base
   */
  public static void runJsUnittestFromHtml(Class<?> base, String htmlResource)
      throws IOException {
    TestUtil.enableContentUrls();

    StringBuilder html = new StringBuilder();
    List<Input> inputs = new ArrayList<Input>();

    // Stub out the Browser
    inputs.add(new Input(RhinoTestBed.class, "../plugin/console-stubs.js"));
    inputs.add(new Input(RhinoTestBed.class, "/js/jqueryjs/runtest/env.js"));
    int injectHtmlIndex = inputs.size();

    extractScriptsFromHtml(new Input(base, htmlResource), base, html, inputs);

    // Set up the DOM.  env.js requires that location be set to a URI before it
    // creates a DOM.  Since it fetches HTML via java.net.URL and passes it off
    // to the org.w3c parser, we use a content: URL which is handled by handlers
    // registed in TestUtil so that we can provide html without having a file
    // handy.
    String domJs = "window.location = "
        + StringLiteral.toQuotedValue(TestUtil.makeContentUrl(html.toString()))
        + ";";
    inputs.add(injectHtmlIndex, new Input(domJs, htmlResource));
    inputs.add(new Input(
        "(function () {\n"
        + "   var onload = document.body.getAttribute('onload');\n"
        + "   onload && eval(onload);\n"
        + " })();", htmlResource));


    // Execute for side-effect
    runJs(null, inputs.toArray(new Input[0]));
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

    public Input(String javascript, String source) {
      this(new StringReader(javascript), source);
    }

    @Override
    public String toString() { return "(InputSource " + source + ")"; }
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

  private static void extractScriptsFromHtml(
      Input input, Class<?> base, Appendable htmlOut, List<Input> jsOut)
      throws IOException {
    // Some quick and dirty parsing.  TODO(mikesamuel): do this properly
    Pattern scriptPattern = Pattern.compile(
        "<script\\b[^>]*>.*?</script\\b[^>]*>",
        Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    String html = readReader(input.input);
    Matcher m = scriptPattern.matcher(html);
    int pos = 0;
    while (m.find()) {
      htmlOut.append(html.substring(pos, m.start()));
      pos = m.end();

      String script = m.group();
      int open = script.indexOf('>') + 1;

      Pattern srcPattern
          = Pattern.compile("\\bsrc\\s*=\\s*[\"']?([^\\s>\"']+)");
      Matcher m2 = srcPattern.matcher(script.substring(0, open));
      if (m2.find()) {
        String src = m2.group(1);
        jsOut.add(new Input(base, src));
      } else {
        int close = script.lastIndexOf("</");
        String inlineScript = script.substring(open, close);
        jsOut.add(new Input(new StringReader(inlineScript), input.source));
      }
    }
    htmlOut.append(html.substring(pos));
  }

  private RhinoTestBed() { /* uninstantiable */ }
}
