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

package com.google.caja.service;

/**
 * @author jasvir@google.com (Jasvir Nagra)
 */
public class GadgetHandlerTest extends ServiceTestCase {
  public final void testGadget() throws Exception {
    String moduleEnvelope = (
        "<Module><ModulePrefs /><Content type=\"html\">"
        + "<![CDATA[%s]]>"
        + "</Content></Module>");

    registerUri("http://foo/bar.js", "foo()", "text/javascript");
    registerUri("http://foo/bar.xml",
                String.format(
                    moduleEnvelope,
                    "<script src=bar.js></script><p>Hello, World!</p>"),
                "application/xml");
    assertEquals(
        String.format(
            moduleEnvelope,
            ""
            + "<p>Hello, World!</p><script type=\"text/javascript\">{\n"
            + "  ___.loadModule({\n"
            + "      'instantiate': function (___, IMPORTS___) {\n"
            + "        var moduleResult___ = ___.NO_RESULT;\n"
            + "        var $v = ___.readImport(IMPORTS___, '$v', {\n"
            + "            'getOuters': {\n"
            + "              '()': { }\n"
            + "            },\n"
            + "            'initOuter': {\n"
            + "              '()': { }\n"
            + "            },\n"
            + "            'cf': {\n"
            + "              '()': { }\n"
            + "            },\n"
            + "            'ro': {\n"
            + "              '()': { }\n"
            + "            }\n"
            + "          });\n"
            + "        var $dis = $v.getOuters();\n"
            + "        $v.initOuter('onerror');\n"
            + "        try {\n"
            + "          {\n"
            + "            moduleResult___ = $v.cf($v.ro('foo'), [ ]);\n"
            + "          }\n"
            + "        } catch (ex___) {\n"
            + "          ___.getNewModuleHandler().handleUncaughtException("
                                                               + "ex___,\n"
            + "            $v.ro('onerror'), 'bar.js', '1');\n"
            + "        }\n"
            + "        {\n"
            + "          IMPORTS___.htmlEmitter___.signalLoaded();\n"
            + "        }\n"
            + "        return moduleResult___;\n"
            + "      },\n"
            + "      'includedModules': [ ],\n"
            + "      'cajolerName': 'com.google.caja',\n"
            + "      'cajolerVersion': 'testBuildVersion',\n"
            + "      'cajoledDate': 0\n"
            + "    });\n"
            + "}</script>"),
        (String) requestGet("?url=http://foo/bar.xml&mime-type=*/*"));
  }
}
