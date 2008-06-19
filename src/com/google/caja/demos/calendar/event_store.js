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
 * Transforms a set of ical content lines to vevents.
 *
 * @author mikesamuel@gmail.com
 */


/** @namespace */
var event_store = (function () {
  var autoEidCounter = 0;

  /**
   * Converts hcalendar to events, and expands recurrences within the specified
   * date range.
   * @param {Array.<ContentLine>} icalContentLines
   * @param {number} startDate a date per time.date
   * @param {number} endDate a date per time.date
   * @return {VCalendar}
   */
  function toCalendar(icalContentLines, startDate, endDate) {
    var calendarId, summary;

    var events = [];
    for (var i = 0, n = icalContentLines.length; i < n; ++i) {
      var cl = icalContentLines[i];
      switch (cl.getName()) {
        case 'BEGIN':
          if ('VEVENT' === cl.getValues()[0]) {
            for (var j = i + 1; j < n; ++j) {
              cl = icalContentLines[j];
              if ('END' === cl.getName() && 'VEVENT' === cl.getValues()[0]) {
                addEvent(icalContentLines.slice(i + 1, j), startDate, endDate,
                         events);
                break;
              }
            }
            i = j;
          }
          break;
        case 'X-WR-CALNAME':
          summary = cl.getValues().join(',');
          break;
        case 'X-ORIGINAL-URL':
          calendarId = cl.getValues()[0];
          break;
      }
    }
    return new VCalendar(calendarId || '', summary || '', events);
  }

  /**
   * Produces an object suitable for consumption by rrule-cajita.js.
   * @private
   */
  function makeRRule(rruleText) {
    var attribs = {};
    var pairs = rruleText.split(/;/g);
    for (var i = 0, n = pairs.length; i < n; ++i) {
      var pair = pairs[i];
      var eq = pair.indexOf('=');
      attribs[pair.substring(0, eq).toUpperCase()]
          = pair.substring(eq + 1).split(/,/g);
    }
    return { getAttribute: function (k) { return attribs[k] || null; },
             toString: function () { return rruleText; } };
  }

  /**
   * @param {Array.<ContentLine> icalContentLines content lines between
   *     BEGIN:EVENT and END:EVENT.
   * @param {number} startDateLimit start of period within which recurrences are
   *     expanded.
   * @param {number} endDateLimit end of period within which recurrences are
   *     expanded.
   * @param {Array.<VEvent>} out appended to.
   * @private
   */
  function addEvent(icalContentLines, startDateLimit, endDateLimit, out) {
    var eid, calendarId, summary, start, end, dur, status, rruleContent;
    var tz = timezone.local;
    for (var i = 0, n = icalContentLines.length; i < n; ++i) {
      var cl = icalContentLines[i];
      switch (cl.getName()) {
        case 'SUMMARY':
          summary = cl.getValues().join(',');
          break;
        case 'DTSTART':
          var value = cl.getValues()[0].toUpperCase();
          if (/Z$/.test(value)) {
            value = value.substring(value.length - 1);
            tz = timezone.utc;
            // TODO(mikesamuel): look at tzid
          }
          start = time.parseIcal(value);
          break;
        case 'DTEND':
          var value = cl.getValues()[0].toUpperCase();
          if (/Z$/.test(value)) {
            value = value.substring(value.length - 1);
            // TODO(mikesamuel): translate to start tzid
          }
          end = time.parseIcal(value);
          break;
        case 'DURATION':
          dur = time.parseDuration(cl.getValues()[0].toUpperCase());
          break;
        case 'RRULE':
          rruleContent = makeRRule(cl.getValues().join('').toUpperCase());
          // TODO(mikesamuel): handle multiple RRULES, EXRULES, RDATES, EXDATES
          break;
        case 'EID':
          eid = cl.getValues().join(',');
          break;
        case 'STATUS':
          status = cl.getValues()[0].toUpperCase();
          break;
      }
    }
    if (status === 'CANCELLED') { return; }
    if (start && !end) {
      if (dur === undefined) {
        dur = (time.isDate(start)
               ? time.duration(1, 0, 0, 0)    // 1 day
               : time.duration(0, 1, 0, 0));  // 1 hour
      }
      end = time.plusDuration(start, dur);
    }
    if (!(start && end)) { return; }
    if (!eid) { eid = 'autoeid-' + ++autoEidCounter; }
    if (rruleContent) {
      if (dur === undefined) { dur = time.durationBetween(end, start); }
      var riter = rrule.createRecurrenceIterator(rruleContent, start, tz);
      riter.advanceTo(tz(startDateLimit, false));
      while (riter.hasNext()) {
        var instanceStartUtc = riter.next();
        var instanceStart = tz(instanceStartUtc, true);
        if (instanceStart >= endDateLimit) { break; }
        var instanceEnd = time.plusDuration(instanceStart, dur);
        var instanceId = eid + '-' + time.toIcal(instanceStartUtc);
        out.push(new VEvent(instanceId, calendarId, summary,
                            instanceStart, instanceEnd));
      }
    } else if (end <= startDateLimit || start >= endDateLimit) {
      return;
    } else {
      out.push(new VEvent(eid, calendarId, summary, start, end));
    }
  }

  return { toCalendar: toCalendar };
})();
