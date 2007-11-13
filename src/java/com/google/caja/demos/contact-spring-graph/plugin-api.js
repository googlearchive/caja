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
 * Registers functions that the plugin can use to get data from the embedding
 * application.  This is the part of the embedding application that the plugin
 * can directly access, so exported functions have to be careful about input
 * and output.
 *
 * @author mikesamuel@gmail.com
 */

(function () {
  var token = {};

  function ExportedCalEvent(event) {
    this.event_ = event;
  }
  ExportedCalEvent.prototype.getId = function () { return this.event_.id_; };
  ExportedCalEvent.prototype.getAttendees = function () {
    return this.event_.attendees_.slice(0);
  };
  var consAndUnlock = protect(
      ExportedCalEvent, token, ['getId', 'getAttendees']);
  var CalEventCapability = consAndUnlock[0];
  var unlockCalEvent = consAndUnlock[1];

  function ExportedCalendar(calendar) {
    this.calendar_ = calendar;
  }
  ExportedCalendar.prototype.getEvents = function () {
    var events = this.calendar_.events_;
    var exportedEvents = [];
    for (var i = events.length; --i >= 0;) {
      exportedEvents[i] = new CalEventCapability(token, events[i]);
    }
    return exportedEvents;
  };
  consAndUnlock = protect(ExportedCalendar, token, ['getEvents']);
  var CalendarCapability = consAndUnlock[0];
  var unlockCalendar = consAndUnlock[1];

  plugin_export({ getCalendar:
                  function getCalendar() {
                    return new CalendarCapability(token, MY_CALENDAR);
                  },
                  setSelectedUser:
                  function setSelectedUser(name) {
                    document.getElementById('currentUser').innerHTML =
                      plugin_html___('The plugin has selected ' + name);
                  },
                  ME: ME
                });

})();
