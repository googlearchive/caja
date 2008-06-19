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
 * A partial javascript timezone library.
 *
 * A timezone maps date-times in one calendar to date-times in another.
 * We represent a timezone as a function that takes a date value and returns a
 * date value.
 *
 * This glosses over the fact that a calendar can map a given date-time to
 * 0 or 2 instants during daylight-savings/standard transitions as does RFC 2445
 * itself.
 *
 * A timezone function takes two arguments,
 * (1) A time value.
 * (2) isUtc.
 *
 * If isUtc is true, then the function maps from UTC to local time,
 * and otherwise maps from local time to UTC.
 *
 * Since the function takes both dates and date-times, it could be used to
 * handle the different Julian<->Gregorian switchover for different locales
 * though this implementation does not.
 *
 * @author mikesamuel@gmail.com
 */

/** @namespace */
var timezone = {};

/**
 * @param {number} dateValue
 * @param {boolean} isUtc
 */
timezone.utc = function (dateValue, isUtc) {
  console.assert('number' === typeof dateValue);
  return dateValue;
};

/**
 * @param {number} dateValue
 * @param {boolean} isUtc
 */
timezone.local = function (dateValue, isUtc) {
  if (time.isDate(dateValue)) { return dateValue; }
  var jsDate;
  if (isUtc) {
    var jsDate = new Date(Date.UTC(
        time.year(dateValue), time.month(dateValue) - 1, time.day(dateValue),
        time.hour(dateValue), time.minute(dateValue), 0));
    return time.dateTime(
        jsDate.getFullYear(), jsDate.getMonth() + 1, jsDate.getDate(),
        jsDate.getHours(), jsDate.getMinutes(), jsDate.getSeconds());
  } else {
    var jsDate = new Date(
        time.year(dateValue), time.month(dateValue) - 1, time.day(dateValue),
        time.hour(dateValue), time.minute(dateValue), 0);
    return time.dateTime(
        jsDate.getUTCFullYear(), jsDate.getUTCMonth() + 1, jsDate.getUTCDate(),
        jsDate.getUTCHours(), jsDate.getUTCMinutes(), jsDate.getUTCSeconds());
  }
};

/**
 * Given a timezone offset in seconds, returns a timezone function.
 * @param {number} offsetSeconds
 * @return {Function}
 */
timezone.fromOffset = function (offsetSeconds) {
  console.assert('number' === typeof offsetSeconds);
  /**
   * @param {number} dateValue
   * @param {boolean} isUtc
   */
  return function (dateValue, isUtc) {
    console.assert('number' === typeof dateValue);
    if (time.isDate(dateValue)) { return dateValue; }
    return time.plusSeconds(dateValue, isUtc ? offsetSeconds : -offsetSeconds);
  };
};

caja.freeze(timezone);
