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
 * An event.
 * @param {number} start a date value.
 * @param {number} end a date value >= start.
 * @param {string} summary human readable html.
 * @param {string} eid opaque unique identifier.
 * @param {string} calendarId opaque unique identifier.
 * @constructor
 * @author mikesamuel@gmail.com
 */
function VEvent(eid, calendarId, summary, start, end) {
  console.assert('string' === typeof summary);
  console.assert('number' === typeof start);
  console.assert('number' === typeof end);
  this.eid = eid;
  this.calendarId = calendarId;
  this.summary = summary;
  this.start = start;
  this.end = end;
}

VEvent.prototype.isOvernightEvent = function () {
  return time.isDate(this.start) ||
      time.nextDate(time.toDate(this.start)) < time.toDateOnOrAfter(this.end);
};

VEvent.prototype.toString = function () {
  return '[Event ' + this.eid + '@' + time.toIcal(this.start)
      + '/' + time.toIcal(this.end) + ']';
};
