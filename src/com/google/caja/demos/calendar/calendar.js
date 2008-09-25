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
 * A collection of events covering a range of dates.
 * @param {string} calendarId opaque unique identifier.
 * @param {string} summary human readable html.
 * @param {Array.<VEvent>} eid opaque unique identifier.
 * @constructor
 * @author mikesamuel@gmail.com
 */
function VCalendar(calendarId, summary, events) {
  this.calendarId = calendarId;
  this.summary = summary;
  this.events = events.slice(0);
  if ((typeof caja) !== 'undefined') {
    cajita.freeze(this.events);
  }
}

VCalendar.prototype.toString = function () {
  return '[Calendar ' + this.calendarId + ']';
};
