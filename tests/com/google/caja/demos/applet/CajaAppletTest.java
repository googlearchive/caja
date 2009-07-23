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

import com.google.caja.reporting.TestBuildInfo;
import com.google.caja.util.CajaTestCase;

import java.awt.HeadlessException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Tests the public interface of CajaApplet without invoking a browser
 *
 * @author Jasvir Nagra <jasvir@gmail.com>
 */
public class CajaAppletTest extends CajaTestCase {

  private CajaApplet makeApplet() throws HeadlessException {
    return new CajaApplet() {
        { setBuildInfo(new TestBuildInfo()); }
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
  }

  public final void testCajoleInValija() throws Exception {
    if (checkHeadless()) return;
    CajaApplet applet = makeApplet();
    // TODO(mikesamuel): move these goldens into files
    String sp = "                 ";
    assertEquals(
      "['\\x3cspan id=\\\"id_1___\\\"\\x3eHowdy\\x3c/span\\x3eThere" +
      "\\x3cscript type=\\\"text/javascript\\\"\\x3e" +
      "{\n" +
      "  ___.loadModule({\n" +
      sp + "  \\'instantiate\\': function (___, IMPORTS___) {\n" +
      sp + "    var moduleResult___ = ___.NO_RESULT;\n" +
      sp + "    var $v = ___.readImport(IMPORTS___, \\'$v\\', {\n" +
      sp + "          \\'getOuters\\': {\n" +
      sp + "            \\'()\\': { }\n" +
      sp + "          },\n" +
      sp + "          \\'initOuter\\': {\n" +
      sp + "            \\'()\\': { }\n" +
      sp + "          },\n" +
      sp + "          \\'cf\\': {\n" +
      sp + "            \\'()\\': { }\n" +
      sp + "          },\n" +
      sp + "          \\'ro\\': {\n" +
      sp + "            \\'()\\': { }\n" +
      sp + "          }\n" +
      sp + "        });\n" +
      sp + "    var $dis = $v.getOuters();\n" +
      sp + "    $v.initOuter(\\'onerror\\');\n" +
      sp + "    {\n" +
      sp + "      var el___;\n" +
      sp + "      var emitter___ = IMPORTS___.htmlEmitter___;\n" +
      sp + "      emitter___.unwrap(emitter___.attach(\\'id_1___\\'));\n" +
      sp + "    }\n" +
      sp + "    try {\n" +
      sp + "      {\n" +
      sp + "        moduleResult___ = $v.cf($v.ro(\\'alert\\'), [ 2 + 2 ]);\n" +
      sp + "      }\n" +
      sp + "    } catch (ex___) {\n" +
      sp + "      ___.getNewModuleHandler().handleUncaughtException(ex___,\n" +
      sp + "          $v.ro(\\'onerror\\'), \\'example.com\\', \\'1\\');\n" +
      sp + "    }\n" +
      sp + "    {\n" +
      sp + "      el___ = emitter___.finish();\n" +
      sp + "      emitter___.signalLoaded();\n" +
      sp + "    }\n" +
      sp + "    return moduleResult___;\n" +
      sp + "  },\n" +
      sp + "  \\'cajolerName\\': \\'com.google.caja\\',\n" +
      sp + "  \\'cajolerVersion\\': \\'testBuildVersion\\',\n" +
      sp + "  \\'cajoledDate\\': 0\n" +
      sp + "});\n" +
      "}\\x3c/script\\x3e','']",
      applet.cajole("Howdy<script>alert(2 + 2);</script>There", "VALIJA_MODE")
          .replace("\\n", "\n"));
  }

  public final void testCajoleInCajita() throws Exception {
    if (checkHeadless()) return;
    CajaApplet applet = makeApplet();
    String sp = "                 ";
    assertEquals(
      "['\\x3cspan id=\\\"id_1___\\\"\\x3eHowdy\\x3c/span\\x3eThere" +
      "\\x3cscript type=\\\"text/javascript\\\"\\x3e" +
      "{\n" +
      "  ___.loadModule({\n" +
      sp + "  \\'instantiate\\': function (___, IMPORTS___) {\n" +
      sp + "    var moduleResult___ = ___.NO_RESULT;\n" +
      sp + "    var alert = ___.readImport(IMPORTS___, \\'alert\\');\n" +
      sp + "    var onerror = ___.readImport(IMPORTS___, \\'onerror\\');\n" +
      sp + "    {\n" +
      sp + "      var el___;\n" +
      sp + "      var emitter___ = IMPORTS___.htmlEmitter___;\n" +
      sp + "      emitter___.unwrap(emitter___.attach(\\'id_1___\\'));\n" +
      sp + "    }\n" +
      sp + "    try {\n" +
      sp + "      {\n" +
      sp + "        moduleResult___ = alert.CALL___(2 + 2);\n" +
      sp + "      }\n" +
      sp + "    } catch (ex___) {\n" +
      sp + "      ___.getNewModuleHandler().handleUncaughtException(ex___,\n" +
      sp + "          onerror, \\'example.com\\', \\'1\\');\n" +
      sp + "    }\n" +
      sp + "    {\n" +
      sp + "      el___ = emitter___.finish();\n" +
      sp + "      emitter___.signalLoaded();\n" +
      sp + "    }\n" +
      sp + "    return moduleResult___;\n" +
      sp + "  },\n" +
      sp + "  \\'cajolerName\\': \\'com.google.caja\\',\n" +
      sp + "  \\'cajolerVersion\\': \\'testBuildVersion\\',\n" +
      sp + "  \\'cajoledDate\\': 0\n" +
      sp + "});\n" +
      "}\\x3c/script\\x3e','']",
      applet.cajole("Howdy<script>alert(2 + 2);</script>There", "")
          .replace("\\n", "\n"));
  }
}
