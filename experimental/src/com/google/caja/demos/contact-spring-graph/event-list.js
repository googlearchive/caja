// Copyright (C) 2006 Google Inc.
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
 * A list of events.  This is a simple demo so the events aren't indexed in
 * any way, and only contain a list of attendees and a uid.
 *
 * @author mikesamuel@gmail.com
 */

/**
 * a very simple event object -- doesn't even have time.
 * @param {String} id a simple id.
 * @param {Array.<String>} attendees a list of email addresses.
 */
function CalEvent(id, attendees) {
  this.id_ = id;
  this.attendees_ = caja.freeze(attendees.slice(0));
}
___.simpleFunc(CalEvent);
CalEvent.prototype.toString = ___.method(function () {
  return '[CalEvent ' + this.id_ + ' : ' + this.attendees_ + ']';
}, 'toString');
CalEvent.prototype.getAttendees = ___.method(function () {
  return this.attendees_;
}, 'getAttendees');
CalEvent.prototype.getId = ___.method(function () {
  return this.id_;
}, 'getId');
___.grantCall(CalEvent.prototype, 'getAttendees');
___.grantCall(CalEvent.prototype, 'getId');

/**
 * a calendar is a group of events.
 * @param {Array.<CalEvent>} events
 */
function Calendar(events) {
  this.events_ = caja.freeze(events.slice(0));
}
___.simpleFunc(Calendar);
Calendar.prototype.getEvents = ___.method(
    function () { return this.events_; }, 'getEvents');
Calendar.prototype.toString = ___.method(
    function () { return '[Calendar]'; }, 'toString');
___.grantCall(Calendar.prototype, 'getEvents');


// for demo purposes, hard-code a list of contacts and events

// some contacts
var ADAM_ = 'adam@127.0.0.1';
var BIANCA_ = 'bianca@127.0.0.1';
var CHARLES_ = 'charles@127.0.0.1';
var DIANE_ = 'diane@127.0.0.1';
var ERIK_ = 'erik@127.0.0.1';
var FRANCESCA_ = 'francesca@127.0.0.1';
var GUY_ = 'guy@127.0.0.1';
var HILLARY_ = 'hillary@127.0.0.1';
var ME = 'me@127.0.0.1';

/**
 * a list of all contacts
 * @type {Array.<String>}
 */
var CONTACTS = [ ADAM_, BIANCA_, CHARLES_, DIANE_, ERIK_, FRANCESCA_, GUY_,
                 HILLARY_ ];

/** a dummy calendar */
var MY_CALENDAR = new Calendar(
    [
      new CalEvent(0, [ADAM_, CHARLES_, DIANE_, ME]),
      new CalEvent(1, [BIANCA_, ERIK_, ME]),
      new CalEvent(2, [DIANE_, CHARLES_, FRANCESCA_, GUY_, ME]),
      new CalEvent(3, [CHARLES_, ME]),
      new CalEvent(4, [ADAM_, HILLARY_, ME]),
      new CalEvent(5, [BIANCA_, ERIK_, FRANCESCA_, GUY_, ME]),
      new CalEvent(6, [DIANE_, GUY_, ME]),
      new CalEvent(7, [DIANE_, FRANCESCA_, ME]),
      new CalEvent(8, [ADAM_, HILLARY_, ME]),
      new CalEvent(9, [ADAM_, BIANCA_, ERIK_, FRANCESCA_, GUY_, HILLARY_, ME])
      ]);
