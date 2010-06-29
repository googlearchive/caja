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

import java.util.Arrays;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.google.caja.reporting.MessageLevel;

/**
 * @author jasvir@google.com (Jasvir Nagra)
 */
public class HtmlHandlerTest extends ServiceTestCase {
  private String requestString(String inputMimeType,
                               String outputMimeType,
                               String moduleCallback) {
    String result = "?url=http://foo/bar.html";
    if (inputMimeType != null) {
      result += "&mime-type=" + inputMimeType;
    }
    if (outputMimeType != null) {
      result += "&output-mime-type=" + outputMimeType;
    }
    if (moduleCallback != null) {
      result += "&module-callback=" + moduleCallback;
    }
    return result;
  }

  private String moduleCallbackPrefix(String moduleCallback) {
    return moduleCallback == null ?
        "___.loadModule(" : moduleCallback + "(___.prepareModule(";
  }

  private String moduleCallbackSuffix(String moduleCallback) {
    return moduleCallback == null ?
        ")" : "))";
  }

  private void assertHtml2Html(String inputMimeType,
                               String outputMimeType,
                               String moduleCallback)
      throws Exception {
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
        + moduleCallbackPrefix(moduleCallback) + "{"
          + "'instantiate':function(___,IMPORTS___){"
            + "return ___.prepareModule({"
                + "'instantiate':function(___,IMPORTS___){"
                  + "var\n$v=___.readImport(IMPORTS___,'$v',{"
                      + "'getOuters':{'()':{}},"
                      + "'initOuter':{'()':{}},"
                      + "'cf':{'()':{}},"
                      + "'ro':{'()':{}}"
                  + "});"
                  + "var\nmoduleResult___,$dis;"
                  + "moduleResult___=___.NO_RESULT;"
                  + "$dis=$v.getOuters();"
                  + "$v.initOuter('onerror');"
                  + "try{"
                    + "{moduleResult___=$v.cf($v.ro('foo'),[])}"
                  + "}catch(ex___){"
                    + "___.getNewModuleHandler().handleUncaughtException("
                        + "ex___,$v.ro('onerror'),'bar.js','1')"
                  + "}"
                  + "return moduleResult___"
                + "},"
                + "'cajolerName':'com.google.caja',"
                + "'cajolerVersion':'testBuildVersion',"
                + "'cajoledDate':0"
              + "})(IMPORTS___),___.prepareModule({"
                + "'instantiate':function(___,IMPORTS___){"
                  + "var\nmoduleResult___;"
                  + "moduleResult___=___.NO_RESULT;"
                  + "{"
                    + "IMPORTS___.htmlEmitter___.signalLoaded()"
                  + "}"
                  + "return moduleResult___"
                + "},"
                + "'cajolerName':'com.google.caja',"
                + "'cajolerVersion':'testBuildVersion',"
                + "'cajoledDate':0"
              + "})(IMPORTS___)"
            + "},"
          + "'cajolerName':'com.google.caja',"
          + "'cajolerVersion':'testBuildVersion',"
          + "'cajoledDate':0"
          + "}" + moduleCallbackSuffix(moduleCallback)
        + "}</script>",
        (String) requestGet(
            requestString(inputMimeType, outputMimeType, moduleCallback)));
  }

  public final void testHtml2Html() throws Exception {
    assertHtml2Html("*/*", "text/html", null);
    assertHtml2Html("*/*", "*/*", null);  // HTML -> HTML is default
    assertHtml2Html("text/html", "text/html", null);
    assertHtml2Html("text/html", "text/html", "foo.bar.baz");
  }

  public final void testHtml2Json() throws Exception {
    assertHtml2Json("*/*", "application/json", null);
    assertHtml2Json("text/html", "application/json", null);
    assertHtml2Json("text/html", "application/json", "foo.bar.baz");
  }  
  
  private void assertHtml2Json(String inputMimeType,
                               String outputMimeType,
                               String moduleCallback)
      throws Exception {
    registerUri("http://foo/bar.html", "<p>hi</p><script>42;</script><p>bye</p>",
      "text/html");

    Object result = json((String)requestGet(
        requestString(inputMimeType, outputMimeType, moduleCallback)));  
    assertTrue(result instanceof JSONObject);
    JSONObject json = (JSONObject) result;

    // Check html generation is correct
    assertEquals("<p>hi<span id=\"id_1___\"></span></p><p>bye</p>",
        (String)json.get("html"));
    assertEquals("{"
        + moduleCallbackPrefix(moduleCallback) + "{"
        + "'instantiate':function(___,IMPORTS___){"
          + "return ___.prepareModule({"
              + "'instantiate':function(___,IMPORTS___){"
                + "var\nmoduleResult___,el___,emitter___;"
                +	"moduleResult___=___.NO_RESULT;"
                + "{"
                  +	"emitter___=IMPORTS___.htmlEmitter___;"
                  + "emitter___.discard(emitter___.attach('id_1___'))"
                + "}"
                + "return moduleResult___"
              + "},"
              + "'cajolerName':'com.google.caja',"
              + "'cajolerVersion':'testBuildVersion',"
              + "'cajoledDate':0})(IMPORTS___),"
              + "___.prepareModule({"
                + "'instantiate':function(___,IMPORTS___){var\n"
                  + "$v=___.readImport(IMPORTS___,'$v',{"
                      + "'getOuters':{'()':{}},"
                      + "'initOuter':{'()':{}},"
                      + "'ro':{'()':{}}"
                  + "});"
                  + "var\nmoduleResult___,$dis;"
                  + "moduleResult___=___.NO_RESULT;"
                  + "$dis=$v.getOuters();"
                  + "$v.initOuter('onerror');"
                  + "try{"
                    + "{moduleResult___=42}"
                  + "}catch(ex___){"
                    + "___.getNewModuleHandler().handleUncaughtException("
                        + "ex___,$v.ro('onerror'),'bar.html','1')"
                  + "}"
                  + "return moduleResult___"
                + "},"
                + "'cajolerName':'com.google.caja',"
                + "'cajolerVersion':'testBuildVersion',"
                + "'cajoledDate':0"
              + "})(IMPORTS___),___.prepareModule({"
                + "'instantiate':function(___,IMPORTS___){"
                  + "var\nmoduleResult___,el___,emitter___;"
                  + "moduleResult___=___.NO_RESULT;"
                  + "{"
                    + "emitter___=IMPORTS___.htmlEmitter___;"
                    + "el___=emitter___.finish();"
                    + "emitter___.signalLoaded()"
                  + "}"
                  + "return moduleResult___"
                + "},"
                + "'cajolerName':'com.google.caja',"
                + "'cajolerVersion':'testBuildVersion',"
                + "'cajoledDate':0"
              + "})(IMPORTS___)"
            + "},"
          + "'cajolerName':'com.google.caja',"
          + "'cajolerVersion':'testBuildVersion',"
          + "'cajoledDate':0"
          + "}" + moduleCallbackSuffix(moduleCallback)
        + "}",
      (String)json.get("js"));
    
    assertTrue(json.get("messages") instanceof JSONArray);
    JSONArray messages = (JSONArray)json.get("messages");
    assertMessagesLessSevereThan(messages, MessageLevel.ERROR);
  }
  
  private void assertHtml2Js(String inputMimeType,
                             String outputMimeType,
                             String moduleCallback)
      throws Exception {
    registerUri("http://foo/bar.html",
                "<p>hi</p><script>42;</script><p>bye</p>",
                "text/html");
    String escapedHtmlString =
        "'<p>hi<span id=\\\"id_1___\\\"></span></p><p>bye</p>'"
        .replace("<", "\\x3c")
        .replace(">", "\\x3e");
    assertEquals(
          "{" + moduleCallbackPrefix(moduleCallback) + "{"
            + "'instantiate':function(___,IMPORTS___){"
              + "return ___.prepareModule({"
              + "'instantiate':function(___,IMPORTS___){"
                + "var\nmoduleResult___;moduleResult___=___.NO_RESULT;"
                + "{IMPORTS___.htmlEmitter___.emitStatic("
                      + escapedHtmlString + ")}"
                + "return moduleResult___"
              + "},"
              + "'cajolerName':'com.google.caja',"
              + "'cajolerVersion':'testBuildVersion',"
              + "'cajoledDate':0"
            + "})(IMPORTS___),___.prepareModule({"
              + "'instantiate':function(___,IMPORTS___){"
                + "var\nmoduleResult___,el___,emitter___;"
                + "moduleResult___=___.NO_RESULT;"
                + "{"
                  + "emitter___=IMPORTS___.htmlEmitter___;"
                  + "emitter___.discard(emitter___.attach('id_1___'))"
                + "}"
                + "return moduleResult___"
              + "},"
              + "'cajolerName':'com.google.caja',"
              + "'cajolerVersion':'testBuildVersion',"
              + "'cajoledDate':0"
            + "})(IMPORTS___),___.prepareModule({"
              + "'instantiate':function(___,IMPORTS___){"
                + "var\n$v=___.readImport(IMPORTS___,'$v',{"
                    + "'getOuters':{'()':{}},"
                    + "'initOuter':{'()':{}},"
                    + "'ro':{'()':{}}"
                  + "});"
                + "var\nmoduleResult___,$dis;"
                + "moduleResult___=___.NO_RESULT;"
                + "$dis=$v.getOuters();"
                + "$v.initOuter('onerror');"
                + "try{"
                  + "{moduleResult___=42}"
                + "}catch(ex___){"
                  + "___.getNewModuleHandler().handleUncaughtException("
                      + "ex___,$v.ro('onerror'),'bar.html','1')"
                + "}"
                + "return moduleResult___"
              + "},"
              + "'cajolerName':'com.google.caja',"
              + "'cajolerVersion':'testBuildVersion',"
              + "'cajoledDate':0"
            + "})(IMPORTS___),___.prepareModule({"
              + "'instantiate':function(___,IMPORTS___){"
                + "var\nmoduleResult___,el___,emitter___;"
                + "moduleResult___=___.NO_RESULT;"
                + "{"
                  + "emitter___=IMPORTS___.htmlEmitter___;"
                  + "el___=emitter___.finish();"
                  + "emitter___.signalLoaded()"
                + "}"
                + "return moduleResult___"
              + "},"
              + "'cajolerName':'com.google.caja',"
              + "'cajolerVersion':'testBuildVersion',"
              + "'cajoledDate':0"
            + "})(IMPORTS___)"
          + "},"
          + "'cajolerName':'com.google.caja',"
          + "'cajolerVersion':'testBuildVersion',"
          + "'cajoledDate':0"
          + "}" + moduleCallbackSuffix(moduleCallback)
        + "}",
        (String) requestGet(
            requestString(inputMimeType, outputMimeType, moduleCallback)));
  }

  public final void testHtml2Js() throws Exception {
    assertHtml2Js("*/*", "text/javascript", null);
    assertHtml2Js("*/*", "application/javascript", null);
    assertHtml2Js("text/html", "application/javascript", null);
    assertHtml2Js("text/html", "text/javascript", null);
    assertHtml2Js("text/html", "text/javascript", "foo.bar.baz");
  }
}
