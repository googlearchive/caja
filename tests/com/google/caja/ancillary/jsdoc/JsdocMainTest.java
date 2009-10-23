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

package com.google.caja.ancillary.jsdoc;

import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.ParseException;
import com.google.caja.util.CajaTestCase;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.Writer;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author mikesamuel@gmail.com
 */
public class JsdocMainTest extends CajaTestCase {
  public final void testPackageDescriptions() throws Exception {
    StringBuilder out = new StringBuilder();
    StringBuilder err = new StringBuilder();
    FileSystem fs = new FakeFileSystem(
        "src/init.js",          "function identity(x) { return x; }",
        "src/foo/bar.js",       ("/** @fileoverview Hi */"
                                 + "/** Foo bar */ var foo_bar;"),
        "src/baz.js",           "/** Baz */ function baz() {}",
        "src/boo/package.html", "Nothing here in boo.  Nothing at {@code all}.",
        "src/foo/package.html", "There is some stuff here in foo/bar.");
    new JsdocMain(fs, err)
        .run(Arrays.asList(
               "src/init.js"
             ),
             Arrays.asList(
               "src/foo/bar.js",
               "src/baz.js",
               "src/boo/package.html"
             ),
             null,
             out);
    assertEquals("", err.toString());
    assertEquals(
        rerender(
            ""
            + "{"
            + "  \"@fileoverview\": {"
            + "    \"boo\": {"
            + "      \"@description\": \"Nothing here in boo.  Nothing at "
                       + "<code class=\\x22prettyprint\\x22>all</code>.\","
            + "      \"@pos\": \"boo/package.html:1+1 - 46\","
            + "      \"@summary\": \"Nothing here in boo.\""
            + "    },"
            + "    \"foo\": {"
            + "      \"@description\": \"There is some stuff here in foo/bar.\","
            + "      \"@pos\": \"foo/package.html:1+1 - 37\","
            + "      \"@summary\": \"There is some stuff here in foo/bar.\","
            + "      \"bar.js\": {"
            + "        \"@description\": \"Hi \","
            + "        \"@pos\": \"foo/bar.js:1+1 - 24\","
            + "        \"@summary\": \"Hi \""
            + "      }"
            + "    }"
            + "  },"
            + "  \"baz\": {"
            + "    \"@description\": \"Baz \","
            + "    \"@extends\": [\"Object\"],"
            + "    \"@field\": {},"
            + "    \"@pos\": \"baz.js:1+1 - 11\","
            + "    \"@summary\": \"Baz \","
            + "    \"@type\": [\"Function\"],"
            + "    \"prototype\": {"
            + "      \"@type\": [\"baz\"]"
            + "    }"
            + "  },"
            + "  \"foo_bar\": {"
            + "    \"@description\": \"Foo bar \","
            + "    \"@pos\": \"foo/bar.js:1+24 - 38\","
            + "    \"@summary\": \"Foo bar \","
            + "    \"@type\": [\"undefined\"]"
            + "  }"
            + "}"),
        rerender(out.toString()));
  }

  static class FakeFileSystem implements FileSystem {
    private final Set<String> dirs;
    private final Map<String, String> files;
    FakeFileSystem(String... pathsAndContent) {
      Set<String> dirs = new HashSet<String>();
      Map<String, String> files = new HashMap<String, String>();
      for (int i = 0; i < pathsAndContent.length; i += 2) {
        String file = pathsAndContent[i], content = pathsAndContent[i + 1];
        files.put(file, content);
        for (String dir = file; (dir = dirname(dir)) != null;) {
          dirs.add(dir);
        }
      }
      this.dirs = dirs;
      this.files = files;
    }

    public String basename(String path) {
      return path.substring(path.lastIndexOf('/') + 1);
    }

    public String canonicalPath(String path) {
      return path.replaceAll("^/+|/+$", "");
    }

    public String dirname(String path) {
      int lslash = path.lastIndexOf('/');
      if (lslash < 0) { return null; }
      return path.substring(0, lslash);
    }

    public boolean exists(String path) {
      return isDirectory(path) || isFile(path);
    }

    public boolean isDirectory(String path) {
      return dirs.contains(path);
    }

    public boolean isFile(String path) {
      return files.containsKey(path);
    }

    public String join(String dir, String filename) {
      return dir + "/" + filename;
    }

    public CharProducer read(String path) {
      return CharProducer.Factory.create(
          new StringReader(files.get(path)), toInputSource(path));
    }

    public InputSource toInputSource(String path) {
      return new InputSource(URI.create("file:///" + path));
    }

    public OutputStream writeBytes(String path) throws IOException {
      throw new IOException(path);
    }

    public Writer write(String path) throws IOException {
      throw new IOException(path);
    }

    public void mkdir(String path) throws IOException {
      throw new IOException(path);
    }
  }

  private String rerender(String js) throws ParseException {
    return render(jsExpr(fromString(js)));
  }
}
