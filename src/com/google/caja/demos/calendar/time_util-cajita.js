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


/** @namespace */
var time_util = {
  /**
   * Given an array, produces a sorted array of the unique elements in the same.
   *
   * Different NaN values are considered the same for purposes of comparison,
   * but otherwise comparison is as by the <code>===</code> operator.
   * This implementation assumes that array elements coerce to a string
   * consistently across subsequent calls.
   *
   * @param {Array} array
   * @return {Array} an array containing only elements in the input that is only
   *   empty if the input is empty.
   */
  uniquify: function (array) {
    var seen = {};
    var uniq = [];

    var nNumbers = 0;
    el_loop:
    for (var i = array.length; --i >= 0;) {
      var el = array[i];
      switch (typeof el) {
        // Use a different path to optimize for arrays of primitives.
        case 'number':
          ++nNumbers;
        case 'boolean':
        case 'undefined':
          if (seen[el]) { continue el_loop; }
          seen[el] = true;
          break;
        case 'string':
          // Need to distinguish '0' from 0.
          var k = 's' + el;
          if (seen[k]) { continue el_loop; }
          seen[k] = true;
          break;
        default:
          // Coercion to a string occurs here.  Use string form as a proxy for
          // hashcode.
          var k = 'o' + el;
          var matches = seen[k];
          if (matches) {
            for (var j = matches.length; --j >= 0;) {
              if (matches[j] === el) { continue el_loop; }
            }
          } else {
            seen[k] = matches = [];
          }
          matches.push(el);
          break;
      }
      uniq.push(el);
    }
    if (nNumbers === uniq.length) {
      uniq.sort(time_util.numericComparator);
    } else {
      uniq.sort();
    }
    return uniq;
  },

  /**
   * given a weekday number, such as -1SU, returns the day of the month that it
   * falls on.
   * The weekday number may be refer to a week in the current month in some
   * contexts or a week in the current year in other contexts.
   * @param {number} dow0 the {@link WeekDay} of the first day in the current
   *   year/month.
   * @param {number} nDays the number of days in the current year/month.
   *   In [28,29,30,31,365,366].
   * @param {number} weekNum -1 in the example above.
   * @param {number} dow WeekDay.SU in the example above.
   * @param {number} d0 the number of days between the 1st day of the current
   *   year/month and the first of the current month.
   * @param {number} nDaysInMonth the number of days in the current month.
   * @return {number} a day of the month or 0 if no such day.
   */
  dayNumToDate: function (dow0, nDays, weekNum, dow, d0, nDaysInMonth) {
    // if dow is wednesday, then this is the date of the first wednesday
    var firstDateOfGivenDow = 1 + ((7 + dow - dow0) % 7);

    var date;
    if (weekNum > 0) {
      date = ((weekNum - 1) * 7) + firstDateOfGivenDow - d0;
    } else {  // Count weeks from end of month.
      // Calculate last day of the given dow.
      // Since nDays <= 366, this should be > nDays.
      var lastDateOfGivenDow = firstDateOfGivenDow + (7 * 54);
      lastDateOfGivenDow -= 7 * (((lastDateOfGivenDow - nDays + 6) / 7) | 0);
      date = lastDateOfGivenDow + 7 * (weekNum + 1) - d0;
    }
    if (date <= 0 || date > nDaysInMonth) { return 0; }
    return date;
  },

  numericComparator: function (a, b) {
    a -= 0;
    b -= 0;
    return a === b ? 0 : a < b ? -1 : 1;
  },

  /**
   * Compute an absolute week number given a relative one.
   * The day number -1SU refers to the last Sunday, so if there are 5 Sundays
   * in a period that starts on dow0 with nDays, then -1SU is 5SU.
   * Depending on where its used it may refer to the last Sunday of the year
   * or of the month.
   *
   * @param {WeekDayNum} weekdayNum -1SU in the example above.
   * @param {WeekDay} dow0 the day of the week of the first day of the month or
   *   year.
   * @param {number} nDays the number of days in the month or year.
   * @return {number} an abolute week number, e.g. 5 in the example above.
   *   Valid if in [1,53].
   */
  invertWeekdayNum: function (weekdayNum, dow0, nDays) {
    console.assert(weekdayNum.num < 0);
    // How many are there of that week?
    return time_util.countInPeriod(
        weekdayNum.wday, dow0, nDays) + weekdayNum.num + 1;
  },

  /**
   * the number of occurences of dow in a period nDays long where the first day
   * of the period has day of week dow0.
   *
   * @param {WeekDay} dow a day of the week.
   * @param {WeekDay} dow0 the day of the week of the first day of the month or
   *   year.
   * @param {number} nDays the number of days in the month or year.
   */
  countInPeriod: function (dow, dow0, nDays) {
    // Two cases
    //    (1a) dow >= dow0: count === (nDays - (dow - dow0)) / 7
    //    (1b) dow < dow0:  count === (nDays - (7 - dow0 - dow)) / 7
    if (dow >= dow0) {
      return 1 + (((nDays - (dow - dow0) - 1) / 7) | 0);
    } else {
      return 1 + (((nDays - (7 - (dow0 - dow)) - 1) / 7) | 0);
    }
  },

  /**
   * The earliest day on or after d that falls on wkst.
   * @param {number} dateValue
   * @param {WeekDay} wkst the day of the week on which the week starts.
   */
  nextWeekStart: function (dateValue, wkst) {
    var delta = (7 - ((7 + (time.weekDayOf(dateValue) - wkst)) % 7)) % 7;
    return time.plusDays(dateValue, delta);
  }

};

caja.freeze(time_util);
