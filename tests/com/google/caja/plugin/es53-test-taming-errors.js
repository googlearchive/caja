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
 * @fileoverview ES53 taming error tests
 * This file is written in JavaScript, not ES53, and is loaded by the host page
 *
 * @author ihab.awad@gmail.com
 * @requires caja, jsunitRun, readyToTest
 */

(function () {

  jsunitRegister('testInitFeralFrameOther',
                 function testInitFeralFrameOther() {
    var frame = document.createElement('iframe');
    document.body.appendChild(frame);

    window.setTimeout(function() {
      var win = frame.contentWindow;
      var doc = frame.contentWindow.document;

      caja.initFeralFrame(win);

      assertTrue(!!win.___);
      assertTrue(!!win.Object.FERAL_FRAME_OBJECT___);
      assertEquals(win.Object, win.Object.FERAL_FRAME_OBJECT___);

      var inner = doc.createElement('div');
      inner.setAttribute('class', 'caja_innerContainer___');
      doc.body.appendChild(inner);

      assertEquals(
          'relative',
          win.getComputedStyle(inner, null)
              .getPropertyValue('position'));

      jsunit.pass('testInitFeralFrameOther');
    }, 0);
  });

  caja.configure({
    cajaServer: 'http://localhost:8000/caja',
    debug: true
  }, function (frameGroup) {

    jsunitRegister('testInitFeralFrameSelf',
                   function testInitFeralFrameSelf() {
      assertTrue(!!window.___);
      assertTrue(!!window.Object.FERAL_FRAME_OBJECT___);
      assertEquals(window.Object, window.Object.FERAL_FRAME_OBJECT___);
      jsunit.pass('testInitFeralFrameSelf');
    });

    jsunitRegister('testToxicSuperCtorFails',
                   function testToxicSuperCtorFails() {
      function ToxicCtor() {}
      function SubCtor() {}
      SubCtor.prototype = new ToxicCtor();

      expectFailure(function() {
        frameGroup.markCtor(SubCtor, ToxicCtor, 'SubCtor');
      });
      jsunit.pass('testToxicSuperCtorFails');
    });

    jsunitRegister('testMethodOfUntamedCtorFails',
                   function testMethodOfUntamedCtorFails() {
      function UntamedCtor() {}
      UntamedCtor.prototype.meth = function() {};

      expectFailure(function() {
        frameGroup.grantMethod(UntamedCtor, 'meth');
      });
      jsunit.pass('testMethodOfUntamedCtorFails');
    });

    readyToTest();
    jsunitRun();
  });
})();

