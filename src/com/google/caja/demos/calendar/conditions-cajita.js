// Copyright (C) 2008 Google Inc.
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
 * @fileoverview
 * Conditions that determine when a recurrence ends.
 *
 * There are two conditions defined in RFC 2445.
 * UNTIL conditions iterate until a date is reached or passed.
 * COUNT conditions iterate until a certain number of results have been
 * produced.
 * In the absence of either of those conditions, the recurrence is unbounded.
 *
 * The COUNT condition is not stateless.
 *
 * A condition has the form:<pre>
 * {
 *   test: function (dateValueUtc) { return shouldContinue; }
 *   reset: function () { ... }
 * }</pre>
 *
 * @author mikesamuel@gmail.com
 */

/** @namespace */
var conditions = {
  countCondition: function (count) {
    var i = count;
    return caja.freeze({
      test: function (dateValueUtc) { return --i >= 0; },
      reset: function () { i = count; }
    });
  },

  untilCondition: function (untilDateValueUtc) {
    return caja.freeze({
      test: function (dateValueUtc) {
        return dateValueUtc <= untilDateValueUtc;
      },
      reset: function () {}
    });
  },

  unboundedCondition: function () {
    return caja.freeze({
      test: function () { return true; },
      reset: function () {}
    });
  }
};

caja.freeze(conditions);
