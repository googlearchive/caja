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
 * @fileoverview Parser for JSON data describing HTML/JS test files to run.
 * <p>
 * A catalog file consists of a JSON array containing test records with the
 * following keys:
 * <dl>
 * 
 * <dt>label
 * <dd>A name for the test or group of tests. Labels are joined with hyphens
 * to form qualified names which must be unique.
 *
 * <dt>comment
 * <dd>Ignored. May in the future be displayed in listings of tests. Should be
 * an array of strings.
 *
 * <dt>tests
 * <dd>An array of more test records, making this record a group of tests.
 * Fields in this record other than 'label', 'comment', and 'tests' are
 * inherited by contained records.
 *
 * <dt>bare
 * <dd>The document to load. Defaults to 'browser-test-case.html'.
 *
 * <dt>bare-template
 * <dd>If 'bare-template' is present, the 'bare' field is transformed by
 * substituting it in place of all '*' characters in the 'bare-template'. This
 * is mainly useful when inherited.
 *
 * <dt>driver
 * <dd>The test driver to load, when applicable; corresponds to URL parameter
 * 'test-driver' (see browser-test-case.js).
 *
 * <dt>driver-template
 * <dd>Template for 'driver' URL in the same format as 'bare-template'.
 *
 * <dt>guest
 * <dd>The guest content to load, when applicable; corresponds to URL parameter
 * 'test-case' (see browser-test-case.js).
 *
 * <dt>guest-template
 * <dd>Template for 'guest' URL in the same format as 'bare-template'.
 *
 * <dt>params
 * <dd>A record containing additional URL parameters (will be escaped). This
 * field is inherited parameter-by-parameter; a parameter can be suppressed by
 * giving its value as undefined.
 *
 * <dt>minified
 * <dd>Selects whether Caja is loaded minified; corresponds to URL parameter
 * 'minified'. Valid values are true or false. Any test record not specifying
 * this field is implicitly turned into a group of two subtests, one for each
 * mode, and the non-minified test will be flagged as <strong>manual</strong>.
 *
 * <dt>expected-pass
 * <dd>Used to inform test drivers for third-party tests how many of the tests
 * are expected to pass when run under Caja. Valid values are a number, 'all',
 * or a record like <code>{ "firefox": a, "chrome": b }<code> where a and b are
 * themelves valid values.
 *
 * <dt>manual
 * <dd>Boolean; if true, indicates that this test should not be run as part of
 * automated tests.
 *
 * <dt>disabled
 * <dd>Boolean; if true, indicates that this test should not be run at all and
 * is only present for documentation purposes.
 *
 * <dt>failureIsAnOption
 * <dd>If present, indicates the test is currently expected to fail. Value
 * should be a string explaining the situation.
 *
 * </dl>
 *
 * @author kpreid@switchb.org
 * @requires JSON
 * @provides parseTestCatalog
 */
var parseTestCatalog;
(function() {
  function forEach(array, callback) {
    for (var i = 0; i < array.length; i++) {
      callback(array[i], i);
    }
  }

  function joinUrl(path, params) {
    var url = '';
    for (var k in params) {
      url += '&' + encodeURIComponent(k) + '=' + encodeURIComponent(params[k]);
    }
    url = url.length ? path + '?' + url.substr(1) : path;
    return url;
  }

  function basicMerge(parent, record, noInherit, alsoMerge) {
    var merged = {};
    for (var k in parent) {
      if (noInherit.indexOf(k) === -1) {
        merged[k] = parent[k];
      }
    }
    for (var k in record) {
      if (alsoMerge.indexOf(k) !== -1 && k in merged) {
        merged[k] = basicMerge(merged[k], record[k], [], []);
      } else {
        merged[k] = record[k];
      }
    }
    return merged;
  }

  function validateKeys(label, record, keys) {
    for (var key in record) {
      if (keys.indexOf(key) === -1) {
        throw new Error(label + ' contained unknown key ' + key);
      }
    }
  }

  function validateAndStringifyPassCount(label, passCount) {
    if (typeof passCount === 'object') {
      for (var key in passCount) {
        if (!(key === 'firefox' || key === 'chrome')) {
          throw new Error(label + ' pass count contained unknown key ' + key);
        }
        validateAndStringifyPassCount(label, passCount[key]);
      }
      return JSON.stringify(passCount);
    } else if (typeof passCount === 'number') {
      return JSON.stringify(passCount);
    } else if (passCount === 'all') {
      return 'all';
    } else {
      throw new Error(label + ' pass count invalid value: ' + passCount);
    }
  }

  function deriveURL(record, name) {
    var url = record[name];
    if ((name + '-template') in record) {
      url = record[name + '-template'].replace(/\*/g, url);
    }
    return url;
  }

  function addPossibleUncajoled(record) {
    // TODO(kpreid): Using this as a proxy for 'this is a third-party test'
    if ('expected-pass' in record && 'guest' in record) {
      record.tests.push({
        'bare': deriveURL(record, 'guest'),
        'label': 'uncajoled',
        'manual': true,
        'params': {
          'test-case': undefined,
          'test-driver': undefined,
          'expected-pass': undefined,
          'es5': undefined,
          'minified': undefined
        }
      });
    }
  }

  function labelFromFilename(prefix, name) {
    forEach([
      /^\.\.\//,
      /^test-/,
      new RegExp("^" + prefix),
      /(-guest|-test|_test)?.(html|js)$/
    ], function(pat) {
      name = name.replace(pat, '');
    });
    return name;
  }

  var inheritDefaults = {
    params: {},
    manual: false,
    disabled: false
  };

  // bit flags (currently only one)
  var SPLIT_MIN = 1;

  function parseTestCatalog_(json, output, onlyAutomatedTests) {
    var seenLabels = {};
    function checkSeen(longLabel) {
      if (longLabel in seenLabels) {
        throw new Error(longLabel + ' is a duplicate test label');
      }
      seenLabels[longLabel] = 1;
    }

    function addFromRecord(output, prefix, record, index, inherit, splits) {
      var label = 'label' in record ? record.label :
          'guest' in record ? labelFromFilename(prefix, record.guest) :
          'driver' in record ? labelFromFilename(prefix, record.driver) :
          'bare' in record ? labelFromFilename(prefix, record.bare) :
          '[' + index + ']';
      var longLabel = label === null ? prefix.substr(0, prefix.length - 1)
        : prefix + label;
      if (label === null) { label = inherit.label; }

      validateKeys(longLabel, record, [
          'label', 'comment', 'tests',
          'bare', 'bare-template',
          'driver', 'driver-template',
          'guest', 'guest-template',
          'params',
          'minified', 'expected-pass',
          'manual', 'disabled', 'failureIsAnOption',
          '_mini']);

      // inheritance
      var merged = basicMerge(
          inherit, record, ['label', 'comment', 'tests'], ['params']);
      merged.label = label;

      // format comment
      var comment = (merged.comment || {}) instanceof Array
          ? merged.comment.join('\n')
          : merged.comment || '';

      // Split leaves into virtual children
      var newSplits = splits;
      if (!('tests' in merged)) {
        if (!('minified' in merged.params) && !('bare' in merged) &&
          !(splits & SPLIT_MIN)) {
          // Split browser-test-case.html tests into minified and not.
          // TODO(kpreid): condition is sloppy: we actually mean "is this a
          // browser-test-case.html test, which uses the minified flag".
          merged._mini = true;
          merged.tests = [
            {'params': {'minified': 'true'}, 'label': 'min'},
            {'params': {'minified': 'false'}, 'label': 'nomin', 'manual': true}
          ];
          newSplits |= SPLIT_MIN;
        }
      }

      if ('tests' in merged) {
        var tests = merged.tests;
        var group = merged._mini
            ? output.addMiniGroup(label, comment)
            : output.addGroup(label, comment);
        forEach(tests, function(sub, i) {
          addFromRecord(group, prefix + label + '-', sub, i, merged, newSplits);
        });
      } else {
        var outPath = 'browser-test-case.html';
        var outParams = {};
        if ('guest' in merged) {
          outParams['test-case'] = deriveURL(merged, 'guest');
        }
        if ('driver' in merged) {
          outParams['test-driver'] = deriveURL(merged, 'driver');
        }
        if ('bare' in merged) {
          outPath = deriveURL(merged, 'bare');
        }
        if ('expected-pass' in merged) {
          outParams['expected-pass'] =
              validateAndStringifyPassCount(longLabel,
                  merged['expected-pass']);
        }
        var params = merged.params;
        for (var k in params) {
          if (params[k] === undefined) {
            delete outParams[k];
          } else {
            outParams[k] = params[k];
          }
        }
        var mayFail = 'failureIsAnOption' in merged ? merged.failureIsAnOption
            : null;

        if (merged.disabled || merged.manual && onlyAutomatedTests) {
          output.addNonTest(label, comment);
        } else {
          var url = joinUrl(outPath, outParams);
          output.addTest(url, label, prefix + label, comment, merged.manual,
              mayFail);
        }
      }
    }

    forEach(json, function(record, i) {
      addFromRecord(output, '', record, i, inheritDefaults, 0);
    });
  }
  parseTestCatalog = parseTestCatalog_;
})();
