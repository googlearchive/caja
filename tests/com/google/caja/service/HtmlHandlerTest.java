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

import com.google.caja.reporting.MessageLevel;

/**
 * @author jasvir@google.com (Jasvir Nagra)
 */
public class HtmlHandlerTest extends ServiceTestCase {
  public final void testHtml() throws Exception {
    String htmlEnvelope = (
        "<html>" +
        "<head><title>Caja Test</title></head>" +
        "<body>" +
        "%s" +
        "</body>" +
        "</html>");

    registerUri("http://foo/bar.js", "foo()", "text/javascript");
    registerUri("http://foo/bar.html",
                String.format(
                    htmlEnvelope,
                    "<p>Hello, World!</p><script src=bar.js></script>"),
                "text/html");
    assertMessagesLessSevereThan(MessageLevel.WARNING);
    assertEquals(
        "<p>Hello, World!</p><script type=\"text/javascript\">{"
        + "___.loadModule({"
            + "'instantiate':function(___,IMPORTS___){"
              + "var moduleResult___=___.NO_RESULT;"
              + "var\n$v=___.readImport(IMPORTS___,'$v',{"
                  + "'getOuters':{'()':{}},"
                  + "'initOuter':{'()':{}},"
                  + "'cf':{'()':{}},"
                  + "'ro':{'()':{}}"
              + "});"
              + "var\n$dis=$v.getOuters();"
              + "$v.initOuter('onerror');"
              + "try{"
                + "{moduleResult___=$v.cf($v.ro('foo'),[])}"
              + "}catch(ex___){"
                + "___.getNewModuleHandler().handleUncaughtException("
                    + "ex___,$v.ro('onerror'),'bar.js','1')"
              + "}"
              + "{"
                + "IMPORTS___.htmlEmitter___.signalLoaded()"
              + "}"
              + "return moduleResult___"
            + "},"
          + "'includedModules':[],"
          + "'cajolerName':'com.google.caja',"
          + "'cajolerVersion':'testBuildVersion',"
          + "'cajoledDate':0"
          + "})"
        + "}</script>",
        (String) requestGet("?url=http://foo/bar.html&mime-type=*/*"));
  }
}
