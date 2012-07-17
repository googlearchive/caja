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
 * @fileoverview ES53 tests of taming host objects for use by guest.
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

    // An object that will contain our tamed API.
    var api = {};

    ////////////////////////////////////////////////////////////////////////
    // TEST OBJECT FOR PROBING THE TAMING

    var testObject = {};

    var getFeralTestObject = function() {
      return testObject;
    };
    var getTamedTestObject = function() {
      frame.tame(testObject);  // Ensure done if not already
      return testObject.TAMED_TWIN___;
    };
    getFeralTestObject.i___ = getFeralTestObject;
    getTamedTestObject.i___ = getTamedTestObject;

    ////////////////////////////////////////////////////////////////////////
    // ACCESS TO OBJECTS IN TAMING FRAME

    var getTamingFrameObject = function(expr) {
      expr = '' + expr;
      return caja.iframe.contentWindow.eval(expr);
    };
    getTamingFrameObject.i___ = getTamingFrameObject;

    ////////////////////////////////////////////////////////////////////////
    // READ ONLY RECORDS

    api.readOnlyRecord = {
      x: 42,
      17: 'seventeen',
      toxicFunctionProperty: function() {},
      __proto__: 42
    };
    api.setReadOnlyRecordField = function(k, v) {
      api.readOnlyRecord[k] = v;
    };

    caja.markFunction(api.setReadOnlyRecordField,
        'setReadOnlyRecordField');
    caja.markReadOnlyRecord(api.readOnlyRecord);

    ////////////////////////////////////////////////////////////////////////
    // ARRAYS

    api.array = [
      42
    ];
    api.setArrayField = function(i, v) {
      api.array[i] = v;
    };

    // Put a reference to some otherwise well-known nonprimitive in an
    // array, to be sure that this gets tamed properly by the array taming.
    api.array[1] = api.readOnlyRecord;

    caja.markFunction(api.setArrayField, 'setArrayField');

    ////////////////////////////////////////////////////////////////////////
    // READ WRITE RECORDS

    api.readWriteRecord = {
      x: 42,
      17: 'seventeen',
      toxicFunctionProperty: function() {},
      __proto__: 42
    };
    api.setReadWriteRecordField = function(k, v) {
      api.readWriteRecord[k] = v;
    };

    caja.markFunction(api.setReadWriteRecordField,
        'setReadWriteRecordField');

    ////////////////////////////////////////////////////////////////////////
    // FUNCTIONS RETURNING PRIMITIVES

    api.functionReturningPrimitive = function (x) {
      return x + 42;
    };

    caja.markFunction(api.functionReturningPrimitive,
        'functionReturningPrimitive');

    ////////////////////////////////////////////////////////////////////////
    // CONSTRUCTORS

    // Create a "class and subclass" pair of constructors
    api.Ctor = function Ctor(x) {
      this.x = x;
      this.invisibleProperty = 17;
      this.readOnlyProperty = 19;
      this.readWriteProperty = 23;
    };
    api.Ctor.prototype.getX = function () {
      return this.x;
    };
    api.Ctor.prototype.setX = function (x) {
      this.x = x;
    };
    api.Ctor.prototype.toxicFunctionProperty = function(x) {
      return "poison";
    };
    api.Ctor.prototype.readOnlyProperty = 3;
    api.Ctor.prototype.readWriteProperty = 7;

    api.SubCtor = function SubCtor(x, y) {
      api.Ctor.call(this, x);
      this.y = y;
    }
    api.SubCtor.prototype = new api.Ctor(0);
    api.SubCtor.prototype.constructor = api.SubCtor;
    api.SubCtor.prototype.getY = function () {
      return this.y;
    };
    api.SubCtor.prototype.setY = function (y) {
      this.y = y;
    };
    api.SubCtor.prototype.getMagSquared = function () {
      return this.x * this.x + this.y * this.y;
    };
    api.SubCtor.prototype.toxicSubMethod = function() {
      return "poison";
    };

    api.functionReturningConstructed = function (x) {
      return new api.Ctor(x);
    };

    // Whitelist the 'Ctor' and 'SubCtor' as constructors, and whitelist the
    // methods except the 'toxic' ones.

    caja.grantRead(api.Ctor, 'prototype');

    caja.grantMethod(api.Ctor.prototype, 'getX');
    caja.grantMethod(api.Ctor.prototype, 'setX');
    caja.grantRead(api.Ctor.prototype, 'readOnlyProperty');
    caja.grantReadWrite(api.Ctor.prototype, 'readWriteProperty');

    caja.markCtor(api.Ctor, Object, 'Ctor');

    caja.grantMethod(api.SubCtor.prototype, 'getY');
    caja.grantMethod(api.SubCtor.prototype, 'setY');
    caja.grantMethod(api.SubCtor.prototype, 'getMagSquared');

    caja.markCtor(api.SubCtor, api.Ctor, 'SubCtor');

    caja.markFunction(api.functionReturningConstructed,
        'functionReturningConstructed');

    // Create a "wrong" constructor that we do not whitelist

    WrongCtor = function WrongCtor(x) { };

    api.functionReturningWrongConstructed = function (x) {
      return new WrongCtor(x);
    };

    // Whitelist the function returning the "wrong" constructed object

    caja.markFunction(api.functionReturningWrongConstructed,
        'functionReturningWrongConstructed');

   ////////////////////////////////////////////////////////////////////////
    // TOXIC CONSTRUCTORS

    // Create a "class and subclass" pair of constructors that we will ensure
    // the guest code cannot use, even though it can read them.
    api.ToxicCtor = function(x) {
      this.x = x;
    }
    api.ToxicCtor.prototype.getX = function() {
      return this.x;
    };
    api.ToxicSubCtor = function(x, y) {
      api.ToxicCtor.call(this, x);
      this.y = y;
    }
    api.ToxicSubCtor.prototype = new api.ToxicCtor(0);
    api.ToxicSubCtor.prototype.getY = function() {
      return this.y;
    };

    ////////////////////////////////////////////////////////////////////////
    // VARIOUS KINDS OF FUNCTIONS

    api.functionReturningRecord = function (x) {
      return {
        x: x
      };
    };
    api.functionReturningFunction = function (x) {
      return caja.markFunction(function (y) { return x + y; });
    };
    api.functionCallingMyFunction = function (f, x) {
      return f(x);
    };
    api.functionReturningMyFunction = function (f) {
      return f;
    };
    api.pureFunctionReturningThis = function () {
      return this;
    };

    caja.markFunction(api.functionReturningRecord,
        'functionReturningRecord');
    caja.markFunction(api.functionReturningFunction,
        'functionReturningFunction');
    caja.markFunction(api.functionCallingMyFunction,
        'functionCallingMyFunction');
    caja.markFunction(api.functionReturningMyFunction,
        'functionReturningMyFunction');
    caja.markFunction(api.pureFunctionReturningThis,
        'pureFunctionReturningThis');

    ////////////////////////////////////////////////////////////////////////
    // IDENTITY FUNCTION

    api.identity = function(x) {
      return x;
    };

    caja.markFunction(api.identity, 'identity');

    ////////////////////////////////////////////////////////////////////////
    // TOXIC FUNCTIONS

    api.toxicFunction = function() {
      return "poison";
    };

    ////////////////////////////////////////////////////////////////////////
    // EXOPHORIC FUNCTIONS

    api.xo4aUsingThis = function(y) {
      return this.x + y;
    };
    api.xo4aReturningThis = function() {
      return this;
    };

    caja.markXo4a(api.xo4aUsingThis, 'xo4aUsingThis');
    caja.markXo4a(api.xo4aReturningThis, 'xo4aReturningThis');

    ////////////////////////////////////////////////////////////////////////
    // PROPERTIES ON FUNCTIONS

    api.functionWithProperties = function functionWithProperties(x) {
      return x;
    };

    api.functionWithProperties.invisibleProperty = 17;
    api.functionWithProperties.readOnlyProperty = 33;
    api.functionWithProperties.readWriteProperty = 49;
    api.functionWithProperties[17] = 'seventeen';
    api.functionWithProperties.toxicFunctionProperty = function() {};

    api.setReadOnlyPropertyOnFunction = function (x) {
      api.functionWithProperties.readOnlyProperty = x;
    };

    caja.grantRead(api.functionWithProperties, 'readOnlyProperty');
    caja.grantReadWrite(api.functionWithProperties, 'readWriteProperty');
    caja.markFunction(api.functionWithProperties,
        'functionWithProperties');
    caja.markFunction(api.setReadOnlyPropertyOnFunction,
         'setReadOnlyPropertyOnFunction');

    ////////////////////////////////////////////////////////////////////////

    caja.markReadOnlyRecord(api);

    // Invoke cajoled tests, passing in the tamed API

    var extraImports = createExtraImportsForTesting(caja, frame);

    if (getUrlParam('tameUsingGlobalMembrane') === 'true') {
      console.log('taming using GLOBAL membrane');
      extraImports.tamedApi = caja.tame(api);
    } else if (getUrlParam('tameUsingGlobalMembrane') === 'false') {
      console.log('taming using FRAME membrane');
      extraImports.tamedApi = frame.tame(api);
    } else {
      // A default means we might unknowingly skip a test condition;
      // don't let's do that....
      fail('Unrecognized or missing tameUsingGlobalMembrane parameter');
    }

    extraImports.tamingFrameUSELESS = caja.USELESS;
    extraImports.tamingFrameObject = caja.iframe.contentWindow.Object;
    extraImports.tamingFrameFunction = caja.iframe.contentWindow.Function;

    extraImports.tamingFrameObject =
        caja.iframe.contentWindow.Object;
    extraImports.tamingFrameFunction =
        caja.iframe.contentWindow.Function;
    extraImports.tamingFrameArray =
        caja.iframe.contentWindow.Array;

    extraImports.getFeralTestObject = getFeralTestObject;
    extraImports.getTamedTestObject = getTamedTestObject;

    extraImports.getTamingFrameObject = getTamingFrameObject;

    extraImports.evalInHost = function(s) {
      return eval(String(s));
    };
    extraImports.evalInHost.i___ = extraImports.evalInHost;
    
    frame.code('es53-test-taming-tamed-guest.html')
         .api(extraImports)
         .run(function (_) {
             readyToTest();
             jsunitRun();
           });
  });
})();
