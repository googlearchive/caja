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
 * An enumeration of weekdays.
 * @author mikesamuel@gmail.com
 */


/**
 * Agrees with {code Date.getDay()}.
 * @enum {number}
 */
var WeekDay = {
  SU: 0,
  MO: 1,
  TU: 2,
  WE: 3,
  TH: 4,
  FR: 5,
  SA: 6
};
WeekDay.successor = function (weekDay) {
  console.assert('number' === typeof weekDay && weekDay >= 0 && weekDay <= 7
                 && (weekDay | 0) === weekDay);
  return (weekDay + 1) % 7;
};
WeekDay.predecessor = function (weekDay) {
  console.assert('number' === typeof weekDay && weekDay >= 0 && weekDay <= 7
                 && (weekDay | 0) === weekDay);
  return (weekDay + 6) % 7;
};
WeekDay.names = [];
(function () {
  // Create a reverse mapping of enum values to names.
  for (var k in WeekDay) {
    if (!WeekDay.hasOwnProperty(k)) { continue; }
    var v = Number(WeekDay[k]);
    if (v === (v & 0x7fffffff)) {  // Is a non-negative integer.
      WeekDay.names[v] = k;
    }
  }
})();

caja.freeze(WeekDay);
caja.freeze(WeekDay.names);
