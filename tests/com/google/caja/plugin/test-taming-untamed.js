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

/**
 * @fileoverview ES53 tests of un-taming guest objects for use by host.
 *
 * @author ihab.awad@gmail.com
 * @requires caja, jsunitRun, readyToTest, basicCajaConfig
 */

(function () {

  caja.initialize(basicCajaConfig);

  // Set up basic stuff
  var div = createDiv();
  var uriPolicy = {
    rewrite: function (uri, uriEffect, loaderType, hints) { return uri; }
  };

  caja.load(div, uriPolicy, function (frame) {

    // Invoke cajoled tests
    var extraImports = createExtraImportsForTesting(caja, frame);

    extraImports.tamingFrameUSELESS = frame.USELESS;

    if (!inES5Mode) {
      extraImports.guestFrameUSELESS = frame.iframe.contentWindow.___.USELESS;
    }

    // An object that can be used by cajoled guest code to store some state
    // between invocations of the "eval" functions (defined below).
    var state = {};

    // A generic function to eval() code in the host.
    // This function does *NOT* untame/tame its args/return value
    extraImports.directEval = function(s, a, b, c) {
      return eval(String(s));
    };
    extraImports.directEval.i___ = extraImports.directEval;

    // A generic function to eval() code in the host.
    // This function untames/tames its args/return value
    extraImports.tameEval =
        frame.tame(frame.markFunction(function(s, a, b, c) {
          return eval('"use strict"; ' + String(s));
        }));

    function assertException(e, name, msg) {
      assertEquals("[object Error]", Object.prototype.toString.call(e));
      assertEquals(msg, e.message);
      assertEquals(name, e.name);
      assertEquals(name + ': ' + msg, e.toString());
      assertTrue(e instanceof caja.iframe.contentWindow[name]);
    }

    function assertExceptionThrown(throwerFromGuest, name, msg) {
      try {
        throwerFromGuest();
        fail();  // if it fails to throw
      } catch (e) {
        assertException(e, name, msg);
      }
    }

    function assertExceptionReturned(returnerFromGuest, name, msg) {
      assertException(returnerFromGuest(), name, msg);
    }

    extraImports.assertExceptionThrown =
        frame.tame(frame.markFunction(assertExceptionThrown));
    extraImports.assertExceptionReturned =
        frame.tame(frame.markFunction(assertExceptionReturned));

    function assertBuiltin(o, name) {
      assertEquals('[object ' + name + ']', Object.prototype.toString.call(o));
      assertTrue(o instanceof caja.iframe.contentWindow[name]);
    }

    extraImports.assertBuiltin =
        frame.tame(frame.markFunction(assertBuiltin));

    function assertRegexp(re, source, global, ignoreCase, multiline) {
      assertEquals(source, re.source);
      assertEquals(global, re.global);
      assertEquals(ignoreCase, re.ignoreCase);
      assertEquals(multiline, re.multiline);
    }

    extraImports.assertRegexp =
        frame.tame(frame.markFunction(assertRegexp));

    function assertUntamedPropertyAccessorExceptions(o) {
      if (inES5Mode) {
        try {
          var x = o.throwingProp;
          fail();
        } catch (e) {
          assertEquals('Error: CustomException: getter threw', e.toString());
        }
        try {
          o.throwingProp = 7;
          fail();
        } catch (e) {
          assertEquals('Error: CustomException: setter threw', e.toString());
        }
      }
    }

    extraImports.assertUntamedPropertyAccessorExceptions =
        frame.tame(frame.markFunction(assertUntamedPropertyAccessorExceptions));

    extraImports.tamedJson = frame.tame({a: 1});

    frame.code('test-taming-untamed-guest.html')
         .api(extraImports)
         .run(function (_) {
             readyToTest();
             jsunitRun();
           });
  });
})();
