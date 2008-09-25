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
 * Complements to generators (see generators.js) that filter out dates which
 * do not match some criteria.
 *
 * The generators are stateful objects that generate candidate dates, and
 * filters are stateless predicates that accept or reject candidate dates.
 *
 * An example of a filter is the BYDAY in the rule below
 *   RRULE:FREQ=MONTHLY;BYMONTHDAY=13;BYDAY=FR   # Every Friday the 13th
 *
 * The BYMONTHDAY generator generates the 13th of the month, and a BYDAY
 * filter rejects any that are not Fridays.
 *
 * This could be done the other way -- a BYDAY generator could generate all
 * Fridays which could then be rejected if they were not the 13th day of the
 * month, but rrule.js chooses generators so as to minimize the number of
 * candidates.
 *
 * Filters are represented as pure functions from dateValues to booleans.
 *
 * @author mikesamuel@gmail.com
 */


/** @namespace */
var filters = {

  /**
   * constructs a day filter based on a BYDAY rule.
   * @param {Array.<WeekDayNum>} days
   * @param {boolean} weeksInYear are the week numbers meant to be weeks in the
   *     current year, or weeks in the current month.
   * @param {WeekDay} wkst the day of the week on which the week starts.
   */
  byDayFilter: function (days, weeksInYear, wkst) {
    return function (dateValue) {
      var dow = time.weekDayOf(dateValue);

      var nDays;
      // First day of the week in the given year or month
      var dow0;
      // Where does date appear in the year or month?
      // in [0, lengthOfMonthOrYear - 1]
      var instance;
      if (weeksInYear) {
        nDays = time.daysInYear(time.year(dateValue));
        // Day of week of the 1st of the year.
        dow0 = time.weekDayOf(time.date(time.year(dateValue), 1, 1));
        instance = time.dayOfYear(dateValue);
      } else {
        nDays = time.daysInMonth(time.year(dateValue), time.month(dateValue));
        // Day of week of the 1st of the month.
        dow0 = time.weekDayOf(time.withDay(dateValue, 1));
        instance = time.day(dateValue) - 1;
      }

      // Which week of the year or month does this date fall on?  1-indexed
      var dateWeekNo;
      if (wkst <= dow) {
        dateWeekNo = 1 + ((instance / 7) | 0);
      } else {
        dateWeekNo = ((instance / 7) | 0);
      }

      // TODO(mikesamuel): according to section 4.3.10
      //     Week number one of the calendar year is the first week which
      //     contains at least four (4) days in that calendar year. This
      //     rule part is only valid for YEARLY rules.
      // That's mentioned under the BYWEEKNO rule, and there's no mention
      // of it in the earlier discussion of the BYDAY rule.
      // Does it apply to yearly week numbers calculated for BYDAY rules in
      // a FREQ=YEARLY rule?

      for (var i = days.length; --i >= 0;) {
        var day = days[i];

        if (day.wday === dow) {
          var weekNo = day.num;
          if (0 === weekNo) { return true; }

          if (weekNo < 0) {
            weekNo = time_util.invertWeekdayNum(day, dow0, nDays);
          }

          if (dateWeekNo === weekNo) { return true; }
        }
      }
      return false;
    };
  },

  /**
   * constructs a day filter based on a BYMONTHDAY rule.
   * @param {Array.<number>} monthDays days of the month in [-31, 31] !== 0
   */
  byMonthDayFilter: function (monthDays) {
    return function (dateValue) {
      var nDays = time.daysInMonth(time.year(dateValue), time.month(dateValue));
      var dvDay = time.day(dateValue);
      for (var i = monthDays.length; --i >= 0;) {
        var day = monthDays[i];
        if (day < 0) { day += nDays + 1; }
        if (dvDay === day) {
          return true;
        }
      }
      return false;
    };
  },

  /**
   * constructs a filter that accepts only every interval-th week from the week
   * containing dtStart.
   * @param {number} interval > 0 number of weeks
   * @param {WeekDay} wkst day of the week that the week starts on.
   * @param {number} dtStart date value
   */
  weekIntervalFilter: function(interval, wkst, dtStart) {
    // The latest day with day of week wkst on or before dtStart.
    var wkStart = time.plusDays(
        dtStart, -((7 + time.weekDayOf(dtStart) - wkst) % 7));
    
    return function (dateValue) {
      var daysBetween = time.daysBetween(dateValue, wkStart);
      if (daysBetween < 0) {
        // date must be before dtStart.  Shouldn't occur in practice.
        daysBetween += (
            interval * 7 * ((1 + daysBetween / (-7 * interval)) | 0));
      }
      var offset = ((daysBetween / 7) | 0) % interval;
      return 0 === offset;
    };
  }
};

cajita.freeze(filters);
