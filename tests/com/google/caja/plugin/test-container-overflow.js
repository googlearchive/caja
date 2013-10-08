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
 * @author ihab.awad@gmail.com
 * @requires caja, jsunitRun, readyToTest, minifiedMode
 */

(function () {

  var origin = location.protocol + '//' + location.host;

  caja.initialize({
    cajaServer: origin + '/caja',
    debug: !minifiedMode,
    maxAcceptableSeverity: 'NEW_SYMPTOM'
  });
  
  // Remove test harness toolbar so it doesn't affect the layout
  var toolbar = document.getElementById('toolbar');
  toolbar.parentNode.removeChild(toolbar);

  // Remove whitespace around the document so we don't waste space
  document.body.style.margin = '0px';
  document.body.style.borderWidth = '0px';

  // Force DIV style to 'inline' to ensure our styling is robust to that
  var style = document.createElement('style');
  style.innerHTML = 'div { display: inline; }';
  document.body.appendChild(style);

  var containerDiv = document.createElement('div');
  containerDiv.style.width = '5px';
  containerDiv.style.height = '5px';
  containerDiv.style.backgroundColor = '#00ff00';
  containerDiv.style.overflow = 'hidden';
  containerDiv.style.display = 'block';

  document.body.appendChild(containerDiv);

  var statusDiv = document.createElement('div');
  statusDiv.style.width = '0px';
  statusDiv.style.height = '0px';
  statusDiv.style.display = 'none';
  statusDiv.setAttribute('class', 'testcontainer waiting');
  statusDiv.setAttribute('id', 'testInnerNotVisible');

  document.body.appendChild(statusDiv);

  function getInnerGuestDiv() {
    // The sandboxed ID assignment policy is hard-coded in here. This test will
    // need to be changed if we change the IDs. If we do change the IDs, this
    // test will fail to signal the need to do so.
    var d = document.getElementById('inner-caja-guest-0___');
    if (!d) {
      fail('Cannnot find inner guest DIV to test');
    }
    return d;
  }

  function getCenterPoint(el) {
    var r = el.getBoundingClientRect();
    return {
      x: ((r.left + r.right) / 2),
      y: ((r.top + r.bottom) / 2)
    };
  }

  jsunitRegister('testInnerNotVisible', function() {
    assertTrue(
        'Cannot run test: Document width inadequate',
        document.documentElement.clientWidth >= 25);
    assertTrue(
        'Cannot run test: Document height inadequate',
        document.documentElement.clientHeight >= 25);

    assertEquals(0, containerDiv.offsetLeft);
    assertEquals(0, containerDiv.offsetTop);
    assertEquals(5, containerDiv.clientWidth);
    assertEquals(5, containerDiv.clientHeight);

    var innerGuestDiv = getInnerGuestDiv();
    var innerGuestCenter = getCenterPoint(innerGuestDiv);

    var elementAtCenter =
        document.elementFromPoint(innerGuestCenter.x, innerGuestCenter.y);

    if ((elementAtCenter === document.body) ||
        (elementAtCenter === document.documentElement)) {
      jsunitPass('testInnerNotVisible');
    } else {
      if (elementAtCenter === innerGuestDiv) {
        fail('CSS rules trivially fail to visually sandbox guest content');
      } else {
        fail('CSS rules do not sandbox guest content with strange results, '
             + 'elementAtCenter = ' + elementAtCenter + ', '
             + 'elementAtCenter.innerHTML = ' + elementAtCenter.innerHTML + ', '
             + 'elementAtCenter.outerHTML = ' + elementAtCenter.outerHTML);
      }
    }
  });

  function runTests() {
    window.setTimeout(function() {
      readyToTest();
      window.setTimeout(function() {
        jsunitRun();
      }, 0);
    }, 0);
  }

  caja.load(containerDiv, undefined, function (frame) {
    frame.code('test-container-overflow-guest.html')
         .run(function (_) { runTests(); });
  });
})();
