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

(function () {

  caja.configure({
    cajaServer: 'http://localhost:8000/caja',
    debug: true
  }, function (frameGroup) {

    // Create a "class and subclass" pair of constructors

    function Ctor(x) {
      this.x = x;
    }
    Ctor.prototype.getX = function () {
      return this.x;
    };
    Ctor.prototype.setX = function (x) {
      this.x = x;
    };

    function SubCtor(x, y) {
      Ctor.call(this, x);
      this.y = y;
    }
    SubCtor.prototype = new Ctor(0);
    SubCtor.prototype.getY = function () {
      return this.y;
    };
    SubCtor.prototype.setY = function (y) {
      this.y = y;
    };
    SubCtor.prototype.getMagSquared = function () {
      return this.x * this.x + this.y * this.y;
    };

    // Define some API

    var api = {
      readOnlyRecord: {
        // Read-only taming is the default
        x: 42,
        17: 'seventeen'
      },
      setReadOnlyRecordField: function(k, v) {
        api.readOnlyRecord[k] = v;
      },
      array: [
        42
      ],
      setArrayField: function(i, v) {
        api.array[i] = v;
      },
      readWriteRecord: {
        x: 42,
        17: 'seventeen'
      },
      setReadWriteRecordField: function(k, v) {
        api.readWriteRecord[k] = v;
      },
      functionReturningPrimitive: function (x) {
        return x + 42;
      },
      Ctor: Ctor,
      SubCtor: SubCtor,
      functionReturningRecord: function (x) {
        return {
          x: x,
        };
      },
      functionReturningFunction: function (x) {
        return function (y) { return x + y; };
      },
      functionReturningConstructed: function (x) {
        var o = new Ctor(x);
        frameGroup.markConstructed(o);
        return o;
      },
      functionCallingMyFunction: function (f, x) {
        return f(x);
      },
      functionReturningMyFunction: function (f) {
        return f;
      },
      pureFunctionReturningThis: function() {
        return this;
      },
      xo4aUsingThis: function(y) {
        return this.x + y;
      },
      xo4aReturningThis: function() {
        return this;
      }
    };

    api.array[1] = api.readOnlyRecord;

    frameGroup.markReadWrite(api.readWriteRecord);

    frameGroup.markCtor(api.Ctor);
    frameGroup.markCtor(api.SubCtor, api.Ctor);

    frameGroup.markXo4a(api.xo4aUsingThis);
    frameGroup.markXo4a(api.xo4aReturningThis);

    // Set up basic stuff

    var div = createDiv();
    function uriCallback(uri, mimeType) { return uri; }

    // Invoke cajoled tests, passing in the tamed API

    frameGroup.makeES5Frame(div, uriCallback, function (frame) {
      var extraImports = createExtraImportsForTesting(frameGroup, frame);
      
      extraImports.tamedApi = frameGroup.tame(api);

      extraImports.USELESS = frameGroup.iframe.contentWindow.___.USELESS;
      extraImports.tamingFrameObject = frameGroup.iframe.contentWindow.Object;
      extraImports.tamingFrameArray = frameGroup.iframe.contentWindow.Array;

      extraImports.directTaming = function(o) {
        return frameGroup.tame(o);
      };
      extraImports.directTaming.i___ = extraImports.directTaming;
      
      frame.run('es53-test-taming-cajoled.html', extraImports, function (_) {
        readyToTest();
        jsunitRun();
      });
    });
  });
})();

