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
 * @fileoverview ES53 test that checks that taming is done correctly going into
 * and out of the taming boundary.
 *
 * For each of:
 *
 *     * host objects tamed to be accessible from the guest
 *     * guest objects un-tamed to be accessible from the host
 *
 * we test proper taming/untaming (as appropriate) of values, arguments
 * and return values for:
 *
 *     * pure functions
 *     * elements of arrays
 *     * data properties of records
 *     * data properties of constructed objects
 *     * methods of constructed objects
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
  var uriPolicy = {
    rewrite: function (uri, uriEffect, loaderType, hints) { return uri; }
  };
  
  caja.load(undefined, uriPolicy, function (frame) {

    // Provide access to USELESS in scope for testing purposes.
    var USELESS = caja.USELESS;

    // An object that will contain our tamed API.
    var api = {};

    ////////////////////////////////////////////////////////////////////////
    // HELPERS FOR PROBING THE TAMING
    //
    // Note that these functions are not themselves tamed; the arguments
    // and return values are directly transmitted across the boundary. In
    // contrast, the test fixtures defined later on are actually tamed.

    var testObject = {};

    var getFeralTestObject = function() {
      return testObject;
    };
    var getTamedTestObject = function() {
      return frame.tame(testObject);
    };
    var evalInHost = function(s, a, b, c) {
      return eval(String(s));
    };

    // For ES5/3
    getFeralTestObject.i___ = getFeralTestObject;
    getTamedTestObject.i___ = getTamedTestObject;
    evalInHost.i___ = evalInHost;

    ////////////////////////////////////////////////////////////////////////
    // TAMED HOST APIs FOR TESTING

    api.tamedHostPureFunction = function(s, a, b, c) {
      return eval(String(s));
    };
    caja.markFunction(api.tamedHostPureFunction);

    api.tamedHostRecord = {
      prop: getFeralTestObject()
    };
    caja.grantReadWrite(api.tamedHostRecord, 'prop');

    api.Ctor = function() {
      this.prop = 42;
    };
    api.Ctor.prototype.meth = function(s, a, b, c) {
      return eval(String(s));
    };
    caja.grantReadWrite(api.Ctor.prototype, 'prop');
    caja.grantMethod(api.Ctor.prototype, 'meth');
    caja.markCtor(api.Ctor, Object, 'Ctor');

    ////////////////////////////////////////////////////////////////////////

    caja.markReadOnlyRecord(api);

    // Invoke cajoled tests, passing in the tamed API

    var extraImports = createExtraImportsForTesting(caja, frame);
    
    extraImports.tamedApi = frame.tame(api);

    extraImports.getFeralTestObject = getFeralTestObject;
    extraImports.getTamedTestObject = getTamedTestObject;
    extraImports.evalInHost = evalInHost;

    frame.code('es53-test-taming-inout-guest.js')
         .api(extraImports)
         .run(function (_) {
             readyToTest();
             jsunitRun();
           });
  });
})();

