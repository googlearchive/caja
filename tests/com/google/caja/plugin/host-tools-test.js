// Copyright (C) 2010 Google Inc.
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

/*
The following aspects of host-tools.js are not tested by this file:
  * Specifying the cajoling service location.
  * Execution without cajita-module.js

The following SHOULD be:
  * Specifying static cajoled files.
*/

//------------------------------------------------------------------------------

var tools;

function setUp() {
  tools = new HostTools();
  
  // override using appspot because we're working on localhost files
  tools.setCajolerService("http://localhost:8000/caja/cajole");
}

function tearDown() {
  tools = undefined;
}

function htt_loaded() {
  var testState = document.getElementById("automatedTestingReadyIndicator");
  testState.className = "readytotest";
  jsunitRun();
}

//------------------------------------------------------------------------------

function assertIdentical() {
  // TODO(kpreid): do this with nice output without using jsUnitCore internals?
  _validateArguments(2, arguments);
  var var1 = nonCommentArg(1, 2, arguments);
  var var2 = nonCommentArg(2, 2, arguments);
  _assert(commentArg(2, arguments),
          cajita.identical(var1, var2),
          'Expected ' + _displayStringForValue(var1) + ' but was ' +
              _displayStringForValue(var2));
}

function assertObject(label, o) {
  assertTrue(label + " is an object", typeof o === "object");
  assertTrue(label + " is not null", o !== null);
  assertTrue(label + " is frozen", cajita.isFrozen(o));
}

//------------------------------------------------------------------------------

jsunitRegister("testConstruct",
               function testConstruct() {
  assertNotUndefined(tools);
  assertNotNull(tools);
jsunit.pass(); });


jsunitRegister("testConstructSandbox",
               function testConstructSandbox() {
  var s = new tools.Sandbox();
  assertNotUndefined(s);
  assertNotNull(s);
jsunit.pass(); });

jsunitRegister("testCreateImports",
               function testCreateImports() {
  var s = new tools.Sandbox();
  var imports = s.imports;
  
  assertNotUndefined("imports", imports);
  
  cajita.forOwnKeys(___.sharedImports, ___.markFuncFreeze(function (k,v) {
    assertIdentical("in imports, " + k, cajita.readPub(imports, k), v);
  }));
  
  cajita.forOwnKeys(imports, ___.markFuncFreeze(function (k,v) {
    assertTrue(cajita.hasOwnPropertyOf(___.sharedImports, k));
  }));
jsunit.pass(); });

jsunitRegister("testRunModule",
               function testRunModule() {
  var s = new tools.Sandbox();
  s.imports.x = 1;
  s.imports.y = 1495802982;
  Q.when(s.run("../a.js"), jsunitCallback(function (moduleResult) {
    assertIdentical("module result", 1495802983, moduleResult);
    jsunit.pass();
  }), jsunitCallback(function (reason) {
    throw reason;
  }));
});

jsunitRegister("testSetBaseURL",
               function testSetBaseURL() {
  tools.setBaseURL(URI.resolve(URI.parse(document.location.toString()),
                               URI.parse("../"))
                      .toString());
  
  var s = new tools.Sandbox();
  s.imports.x = 1;
  Q.when(s.run("c.js"), jsunitCallback(function (moduleResult) {
    // We actually only care that the module ran.
    assertIdentical("module result", 2, moduleResult);
    jsunit.pass();
  }), jsunitCallback(function (reason) {
    throw reason;
  }));
});

jsunitRegister("testRunGadget",
               function testRunGadget() {
  
  var e = document.createElement("div");
  document.body.appendChild(e);
  
  var s = new tools.Sandbox();
  s.attach(e);
  
  Q.when(s.run(
      "../../../../../src/com/google/caja/demos/container/gadget-trivial.html"),
      jsunitCallback(function (moduleResult) {
    assertNotEquals(e.childNodes.length, 0);
    jsunit.pass();
  }), jsunitCallback(function (reason) {
    throw reason;
  }));
});

jsunitRegister("testGadgetNonInterference",
               function testGadgetNonInterference() {
  // Can gadgets mess with each other by looking up ids?
  // Ensure that HostTools generates unique ids
  
  var total = 2;
  var passCount = 0;
  for (var i = 1; i <= total; i++) {
    var ii = i;
    
    var e = document.createElement("div");
    document.body.appendChild(e);
  
    var s = new tools.Sandbox();
    s.attach(e);
  
    Q.when(s.run("evil-twin.html"), jsunitCallback(function (moduleResult) {
      assertEquals("touch count of twin #" + ii,
                   1,
                   s.imports.outers.touchCount);
      if (++passCount >= total) {
        jsunit.pass();
      }
    }), jsunitCallback(function (reason) {
      throw reason;
    }));
  }
});

jsunitRegister("testEvalModule",
               function testEvalModule() {
  var s = new tools.Sandbox();
  s.imports.h = 36;

  var res = s.runCajoledModuleString(
    "{___.loadModule({'instantiate':" +
      "function(___,IMPORTS___){return IMPORTS___.h + 1}})}");
  assertEquals("module result", 37, res);
  
  jsunit.pass();
});

jsunitRegister("testDefaultURIPolicy",
               function testDefaultURIPolicy() {
  var e = document.createElement("div");
  document.body.appendChild(e);
  var s = new tools.Sandbox();
  s.attach(e);
  
  Q.when(s.run(
      "host-tools-test-gadget-uri-policy.html"),
      jsunitCallback(function (moduleResult) {
      
    // TODO(kpreid): When the whole URI-policy-implementation thing is cleaned
    // up, enable this test.
    //assertEquals("dynamic equals static",
    //    e.childNodes[0].href,
    //    e.childNodes[1].href);

    // testing what we can for now
    assertEquals("static rewritten URI",
        "http://localhost:8000/caja/cajole?url=http%3A%2F%2Fwww.example.com" +
        "%2Ffoo&input-mime-type=*%2F*&output-mime-type=*%2F*",
        e.childNodes[0].href);
    assertEquals("dynamic rewritten URI",
        "http://localhost:8000/caja/cajole?url=http%3A%2F%2Fwww.example.com" +
        "%2Ffoo&input-mime-type=*%2F*&output-mime-type=*%2F*",
        e.childNodes[1].href);
    
    jsunit.pass();
  }), jsunitCallback(function (reason) {
    throw reason;
  }));
});

jsunitRegister("testSettingURIPolicy",
               function testSettingURIPolicy() {
  var e = document.createElement("div");
  document.body.appendChild(e);

  // Test the usefulness check
  var s = new tools.Sandbox();
  s.attach(e);
  assertThrows(function () {
    s.setURIPolicy({rewrite: function () {}});
  }, "setURIPolicy() must be used before attach()");
  
  // Construct sandbox for actual test
  s = new tools.Sandbox();
  s.setURIPolicy({
    rewrite: function (uri) {
      return "data:," + uri;
    }
  });
  s.attach(e);
  
  Q.when(s.run(
      "host-tools-test-gadget-uri-policy.html"),
      jsunitCallback(function (moduleResult) {
        
    // Static URI should be set at initialization time
    assertEquals("static rewrite",
                 "data:,http://www.example.com/foo",
                 e.childNodes[0].href);
    
    // Check that specified policy took effect on a dynamic link
    assertEquals("dynamic rewrite",
                 "data:,http://www.example.com/foo",
                 e.childNodes[1].href);
    jsunit.pass();
  }), jsunitCallback(function (reason) {
    throw reason;
  }));
});

