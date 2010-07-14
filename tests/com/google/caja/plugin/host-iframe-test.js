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

//------------------------------------------------------------------------------

function setUp() {
}

function tearDown() {
}

function testPageLoaded() {
  var testState = document.getElementById("automatedTestingReadyIndicator");
  testState.className = "readytotest";
  jsunitRun();
}

//------------------------------------------------------------------------------

/*
Unfortunately, this test doesn't work because other tests running asynchronously
also change the layout of the page.

jsunitRegister("testDoesNotShowUpOnPage",
               function testDoesNotShowUpOnPage() {
  function getSize() {
    return window.getComputedStyle(document.body, null).height;
  }
  // not using jsunitCallback because that creates opportunities for the on-page
  // console to change the size.
  window.setTimeout((function () {
    var size = getSize();
    loadCaja((function (framedCaja) {
      assertEquals("page size after loading Caja frame", size, getSize());
      jsunit.pass("testDoesNotShowUpOnPage");
    }));
  }), 0);
});
*/

jsunitRegister("testBadOptionsIsCaught",
               function testBadOptionsIsCaught() {
  try {
    loadCaja(jsunitCallback(function (framedCaja) {
      throw new Error("Shouldn't reach here (1)");
    }), "http://localhost:8080/");
  } catch (e) {
    console.log("(passed due to error ", e, ")");
    jsunit.pass();
    return;
  }
  throw new Error("Shouldn't reach here (2)");
});

jsunitRegister("testCajaServerOptionIsSignificant",
               function testCajaServerOptionIsSignificant() {
  var fail = false;
  loadCaja(jsunitCallback(function (framedCaja) {
    fail = true;
    throw new Error("Shouldn't reach here (1)");
  }), {cajaServer: "http://bogus.example.invalid:48261/"});
  setTimeout(jsunitCallback(function () {
    // TODO: arrange to reliably detect loading errors in loadCaja and report
    if (!fail) {
      jsunit.pass();
    }
  }), 2000);
});

jsunitRegister("testRunGadget",
               function testRunGadget() {
  var e = document.createElement("div");
  document.body.appendChild(e);
  
  loadCaja(jsunitCallback(function (framedCaja) {
    var tools = framedCaja.hostTools;
    var s = new tools.Sandbox();
    s.attach(e);
  
    framedCaja.Q.when(s.run("../../../../../src/com/google/caja/" +
                            "demos/container/gadget-trivial.html"),
        jsunitCallback(function (moduleResult) {
      assertNotEquals(e.childNodes.length, 0);
      jsunit.pass();
    }), jsunitCallback(function (reason) {
      throw reason;
    }));
  }), {cajaServer: "http://localhost:8080/"});
});

