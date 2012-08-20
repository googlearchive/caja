// Copyright (C) 2012 Google Inc.
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
 * @fileoverview ES53 tests of advice around functions.
 *
 * @author ihab.awad@gmail.com
 * @requires caja, jsunitRun, readyToTest
 */

(function () {

  caja.initialize({
    cajaServer: '/caja',
    debug: true,
    forceES5Mode: inES5Mode
  });

  // Set up basic stuff
  var div = createDiv();
  var uriPolicy = {
    rewrite: function (uri, uriEffect, loaderType, hints) { return uri; }
  };

  caja.load(div, uriPolicy, function (frame) {

    function makeIdentity() { return function(a) { return a; }; }

    var extraImports = createExtraImportsForTesting(caja, frame);

    var advisedBefore = makeIdentity();
    var advisedAfter = makeIdentity();
    var advisedAround = makeIdentity();

    frame.adviseFunctionBefore(advisedBefore, function(f, self, args) {
      return [ 'bef' + args[0] ];
    });

    frame.adviseFunctionAfter(advisedAfter, function(f, self, result) {
      return result + 'aft';
    });

    frame.adviseFunctionAround(advisedAround, function(f, self, args) {
      return f(self, [ 'arn' + args[0] ]) + 'arn';
    });

    var advisedBeforeM = makeIdentity();
    var advisedAfterM = makeIdentity();
    var advisedAroundM = makeIdentity();

    frame.adviseFunctionBefore(advisedBeforeM, function(f, self, args) {
      return [ 'bef0' + args[0] ];
    });
    frame.adviseFunctionBefore(advisedBeforeM, function(f, self, args) {
      return [ 'bef1' + args[0] ];
    });
    frame.adviseFunctionBefore(advisedBeforeM, function(f, self, args) {
      return [ 'bef2' + args[0] ];
    });

    frame.adviseFunctionAfter(advisedAfterM, function(f, self, result) {
      return result + 'aft0';
    });
    frame.adviseFunctionAfter(advisedAfterM, function(f, self, result) {
      return result + 'aft1';
    });
    frame.adviseFunctionAfter(advisedAfterM, function(f, self, result) {
      return result + 'aft2';
    });

    frame.adviseFunctionAround(advisedAroundM, function(f, self, args) {
      return f(self, [ 'arn0' + args[0] ]) + 'arn0';
    });
    frame.adviseFunctionAround(advisedAroundM, function(f, self, args) {
      return f(self, [ 'arn1' + args[0] ]) + 'arn1';
    });
    frame.adviseFunctionAround(advisedAroundM, function(f, self, args) {
      return f(self, [ 'arn2' + args[0] ]) + 'arn2';
    });

    var advisedAll = makeIdentity();

    frame.adviseFunctionBefore(advisedAll, function(f, self, args) {
      return [ 'bef' + args[0] ];
    });
    frame.adviseFunctionAfter(advisedAll, function(f, self, result) {
      return result + 'aft';
    });
    frame.adviseFunctionAround(advisedAll, function(f, self, args) {
      return f(self, [ 'arn' + args[0] ]) + 'arn';
    });

    extraImports.advisedBefore = frame.tame(frame.markFunction(advisedBefore));
    extraImports.advisedAfter = frame.tame(frame.markFunction(advisedAfter));
    extraImports.advisedAround = frame.tame(frame.markFunction(advisedAround));

    extraImports.advisedBeforeM = frame.tame(frame.markFunction(advisedBeforeM));
    extraImports.advisedAfterM = frame.tame(frame.markFunction(advisedAfterM));
    extraImports.advisedAroundM = frame.tame(frame.markFunction(advisedAroundM));

    extraImports.advisedAll = frame.tame(frame.markFunction(advisedAll));

    frame.code('es53-test-taming-advice-guest.html')
         .api(extraImports)
         .run(function (_) {
             readyToTest();
             jsunitRun();
           });
  });
})();