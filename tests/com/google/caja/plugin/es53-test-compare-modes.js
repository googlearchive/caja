// Copyright (C) 2013 Google Inc.
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
 * @fileoverview Tests which depend on having both ES5/3 and SES around.
 *
 * Note that the caja.js API is designed not to include mixed mode operation,
 * so we are skipping Caja per se and loading only ES5/3 and SES standalone.
 *
 * @author kpreid@switchb.org
 */

(function () {
  // (Cut down from version in caja.js.)
  function installAsyncScript(frameWin, scriptUrl) {
    var frameDoc = frameWin['document'];
    var script = frameDoc.createElement('script');
    script.setAttribute('type', 'text/javascript');
    script.src = scriptUrl;
    frameDoc.body.appendChild(script);
  }

  // Create a new iframe, load a script in it, and return its contentWindow.
  // (Cut down and unified from version in caja.js.)
  function createFrame(script, opt_preinit) {
    var frame = document.createElement('iframe');
    document.body.appendChild(frame);
    frame.style.display = 'none';
    // necessary timing, as noted in caja.js
    setTimeout(function() {
      (opt_preinit || function() {})(frame.contentWindow);
      setTimeout(function() {
        installAsyncScript(frame.contentWindow, script);
      }, 0);
    }, 0);
    return frame.contentWindow;
  }

  var es53Win = createFrame('/ant-lib/com/google/caja/es53.js');
  var sesWin = createFrame('/ant-lib/com/google/caja/ses/initSESPlus.js',
      function(win) {
    win.ses = win.Object();
    win.ses.maxAcceptableSeverityName = 'NOT_ISOLATED';
  });

  // Wait for both frames to load, then start testing.
  var interval = setInterval(function() {
    //console.log('Waiting for load...', !!es53Win.___, !!sesWin.cajaVM);
    if (es53Win.___ && sesWin.cajaVM) {
      clearInterval(interval);

      readyToTest();
      jsunitRun();
    }
  }, 200);

  // we're not a test-case so can't just *be* HTML, but this is slightly easier
  // to read than constructing it via DOM
  document.body.insertAdjacentHTML('beforeend',
      '<div class="testcontainer" id="testGlobalProperties">' +
        'testGlobalProperties' +
        '<table style="font: 80% monospace; border-collapse: collapse;">' +
        '</table>' +
      '</div>');

  jsunitRegister('testGlobalProperties', function testGlobalProperties() {
    var table = document.querySelector('#testGlobalProperties table');

    // Bit flags
    var ON_ES53 = 1;
    var ON_SES = 2;
    var ON_ALL = ON_ES53 | ON_SES;
    function describe(state, quiet) {
      switch (state) {
        case 0: return 'missing';
        case ON_ES53: return 'only on ES5/3';
        case ON_SES: return 'only on SES';
        case ON_ALL: return quiet ? '' : 'everywhere';
        default: return '<' + state + '>';
      }
    }

    var hop = Object.prototype.hasOwnProperty;
    function getPropertyNames(obj) {
      if (obj === null) {
        return [];
      } else {
        return getPropertyNames(Object.getPrototypeOf(obj)).concat(
            Object.getOwnPropertyNames(obj));
      }
    }
    function getPropertyDescriptor(obj, prop) {
      if (obj === null) {
        return undefined;
      } else {
        return Object.getOwnPropertyDescriptor(obj, prop) ||
            getPropertyDescriptor(Object.getPrototypeOf(obj), prop);
      }
    }

    // Note: Since we are on an SES-supporting browser, we can uses ES5
    // features, but we cannot expect them to have been repaired by repairES5
    // in this frame.

    var noDiscrepancies = true;
    var seen = new sesWin.WeakMap();

    function compare(sesObj, es53Obj, expected, prefix) {
      if (seen.has(sesObj)) { return; }
      seen.set(sesObj, 1);

      // Map property names to where they have been seen.
      var props = Object.create(null);

      // cause 'missing' state to be noted for expected properties
      for (var prop in expected) {
        props[prop] = 0;
      }

      // Check prototypes even though the property is unenumerable.
      if ((typeof sesObj === 'function' || typeof es53Obj === 'function')
          && ('prototype' in sesObj || 'prototype' in es53Obj)) {
        props['prototype'] = ON_ALL;
      }

      // Record all properties either enumerable or expected, or on the global
      // object (minus kludges for expected variances).
      // TODO(kpreid): Include prototypes' methods (which are usually
      // non-enumerable) if it's feasible.
      es53Win.___.allKeys(es53Obj).forEach(function(prop) {
        if (es53Obj[prop + '_e___'] ||
            hop.call(expected, prop) ||
            (es53Obj === es53Win.___.sharedImports &&
                !hop.call(sesWin.Object.prototype, prop))) {
          props[prop] = (props[prop] || 0) | ON_ES53;
        }
      });
      getPropertyNames(sesObj).forEach(function(prop) {
        if (getPropertyDescriptor(sesObj, prop).enumerable ||
            hop.call(expected, prop) ||
            (sesObj === sesWin.cajaVM.sharedImports &&
                !hop.call(sesWin.Object.prototype, prop))) {
          // note: Object.prototype check is for SES FREEZING_BREAKS_PROTOTYPES
          // workaround and should go away after that kludge is dead.
          props[prop] = (props[prop] || 0) | ON_SES;
        }
      });

      var sorted = Object.getOwnPropertyNames(props);
      sorted.sort();

      sorted.forEach(function(prop) {
        var actualState = props[prop];

        var row = table.insertRow(-1);
        row.insertCell(-1).textContent = prefix + prop;
        var status = row.insertCell(-1);

        // report our opinion
        if (expected.hasOwnProperty(prop)) {
          var expectedState = typeof expected[prop] === 'number'
              ? expected[prop] : ON_ALL;
          if (actualState === expectedState) {
            status.textContent = describe(actualState, true);
          } else {
            noDiscrepancies = false;
            status.appendChild(document.createElement('strong')).textContent =
                'expected ' + describe(expectedState) + ' but was ' +
                describe(actualState);
          }
        } else {
          if (actualState !== (ON_SES | ON_ES53)) {
            noDiscrepancies = false;
            status.appendChild(document.createElement('strong')).textContent =
                'newly ' + describe(actualState);
          } else {
            status.textContent = describe(actualState, true);
          }
        }

        // recurse
        if (!/__$/.test(prop)) {
          var sesValue = sesObj[prop];
          var es53Value = es53Obj.v___(prop);
          if (sesValue === sesWin.Object(sesValue) &&
              es53Value === es53Win.Object(es53Value)) {
            // TODO(kpreid): Could be an error if one side is not an object and
            // the other is.

            var expectedList = (expected.hasOwnProperty(prop) && typeof
                expected[prop] === 'object') ? expected[prop] : {};
            compare(sesValue, es53Value, expectedList, prefix + prop + '.');
          }
        }
      });
    }

    var expected = {
      // ES5/3-specific details
      false: ON_ES53,
      true: ON_ES53,
      null: ON_ES53,

      // TODO(kpreid): this is issue 1663
      unescape: ON_SES,

      // Proxy is not yet patched for WeakMap-emulation compatibility on SES
      Proxy: ON_ES53,

      // TODO(kpreid): to be fixed
      StringMap: ON_SES,

      JSON: {
        parse: ON_ALL,
        stringify: ON_ALL
      },

      cajaVM: {
        // evaluation operations -- not supported in ES5/3
        Function: ON_SES,
        compileExpr: ON_SES,
        compileModule: ON_SES,
        confine: ON_SES,
        eval: ON_SES,

        // evaluation support
        // TODO(kpreid): Add to ES5/3 for consistency and reflection support
        makeImports: ON_SES,
        sharedImports: ON_SES,
        copyToImports: ON_SES,

        // SES-specific details
        es5ProblemReports: ON_SES, // TODO(kpreid): Add to ES5/3 for known
            // deviations from spec?

        // ES5/3-specific details/legacy features
        USELESS: ON_ES53,
        manifest: ON_ES53,

        // Unreviewed or to-be-fixed discrepancies
        // TODO(kpreid): Review these.
        Token: ON_ES53,
        allKeys: ON_ES53,
        enforce: ON_ES53,
        enforceNat: ON_ES53,
        enforceType: ON_ES53,
        identity: ON_ES53,
        is: ON_SES,
        isFunction: ON_ES53
      }
    };

    compare(sesWin.cajaVM.sharedImports, es53Win.___.sharedImports, expected,
        '');

    if (noDiscrepancies) {
      jsunitPass('testGlobalProperties');
    } else {
      fail('Discrepancies found');
    }
  });
})();
