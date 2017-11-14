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
 * @fileoverview ES53 tests of taming and untaming of language primitives.
 *
 * @author ihab.awad@gmail.com
 * @requires caja, jsunitRun, readyToTest, basicCajaConfig
 */

(function() {

  caja.initialize(basicCajaConfig);

  // Set up basic stuff

  var div = createDiv();
  var uriPolicy = {
    rewrite: function (uri, uriEffect, loaderType, hints) { return uri; }
  };

  caja.load(div, uriPolicy, function (frame) {

    function assertPrimitives(f) {
      assertEquals(null, f(null));
      assertEquals(void 0, f(void 0));
      assertEquals(37, f(37));
      assertEquals(true, f(true));      
      assertEquals(false, f(false));      
      assertEquals('abc', f('abc'));
    }

    jsunitRegister('testPrimitivesTaming',
                   function testPrimitivesTaming() {
      assertPrimitives(frame.tame);                   
      jsunitPass('testPrimitivesTaming');
    });
    
    jsunitRegister('testPrimitivesUntaming',
                   function testPrimitivesUntaming() {
      assertPrimitives(frame.untame);
      jsunitPass('testPrimitivesUntaming');
    });

    readyToTest();
    jsunitRun();
  });
})();
