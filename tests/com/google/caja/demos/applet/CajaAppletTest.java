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

package com.google.caja.demos.applet;

import com.google.caja.util.CajaTestCase;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Tests the public interface of CajaApplet without invoking a browser
 *
 * @author Jasvir Nagra <jasvir@gmail.com>
 */
public class CajaAppletTest extends CajaTestCase {
  CajaApplet applet = new CajaApplet() {
    @Override
    public URL getDocumentBase() {
      try {
        return new URL("http://example.com");
      } catch (MalformedURLException e) {
        assert(false);
      }
      return null;
    }
  };

  private String replaceValue(String inString, String key) {
    return inString.replaceFirst(
        "\\\\'" + key + "\\\\'"
        + ":\\s*(\\\\')?\\w+(\\\\')?",
        "\\\\'" + key + "\\\\': \\\\'*\\\\'");
  }

  /**
   * Does a string equality comparison on expected vs actual output of
   * a cajoled module.  Removes the cajoled date and version from the
   * comparison
   *
   * @param expected expected output without date and version number
   * @param actual cajoled module output
   */
  private void assertModule(String expected, String actual) {
    String deterministic = replaceValue(actual, "cajolerVersion");
    deterministic = replaceValue(deterministic, "cajoledDate");
    assertEquals(expected, deterministic);
  }

  private final String INDENT = "                 ";
  public void testCajoleInValija() throws Exception {
    assertModule(
        "['\\x3cscript type=\\\"text/javascript\\\"\\x3e" +
        "{\\n" +
        "  ___.loadModule({\\n" +
        INDENT + "  \\'instantiate\\': function (___, IMPORTS___) {\\n" +
        INDENT + "    var moduleResult___ = ___.NO_RESULT;\\n" +
        INDENT + "    var $v = ___.readImport(IMPORTS___, \\'$v\\', {\\n" +
        INDENT + "          \\'getOuters\\': {\\n" +
        INDENT + "            \\'()\\': { }\\n" +
        INDENT + "          },\\n" +
        INDENT + "          \\'initOuter\\': {\\n" +
        INDENT + "            \\'()\\': { }\\n" +
        INDENT + "          }\\n" +
        INDENT + "        });\\n" +
        INDENT + "    var $dis = $v.getOuters();\\n" +
        INDENT + "    moduleResult___ = $v.initOuter(\\'onerror\\');\\n" +
        INDENT + "    IMPORTS___.htmlEmitter___.pc(\\'var x = 1;\\').cd();\\n" +
        INDENT + "    return moduleResult___;\\n" +
        INDENT + "  },\\n" +
        INDENT + "  \\'cajolerName\\': \\'com.google.caja\\',\\n" +
        INDENT + "  \\'cajolerVersion\\': \\'*\\',\\n" +
        INDENT + "  \\'cajoledDate\\': \\'*\\'\\n" +
        INDENT + "});\\n" +
        "}\\x3c/script\\x3e','']",
        applet.cajole("var x = 1;", "VALIJA_MODE"));
  }

  public void testCajoleInCajita() throws Exception {
    assertModule(
        "['\\x3cscript type=\\\"text/javascript\\\"\\x3e" +
        "{\\n" +
        "  ___.loadModule({\\n" +
        INDENT + "  \\'instantiate\\': function (___, IMPORTS___) {\\n" +
        INDENT + "    var moduleResult___ = ___.NO_RESULT;\\n" +
        INDENT + "    IMPORTS___.htmlEmitter___.pc(\\'var x = 1;\\').cd();\\n" +
        INDENT + "    return moduleResult___;\\n" +
        INDENT + "  },\\n" +
        INDENT + "  \\'cajolerName\\': \\'com.google.caja\\',\\n" +
        INDENT + "  \\'cajolerVersion\\': \\'*\\',\\n" +
        INDENT + "  \\'cajoledDate\\': \\'*\\'\\n" +
        INDENT + "});\\n" +
        "}\\x3c/script\\x3e','']",
        applet.cajole("var x = 1;", ""));
  }
}
