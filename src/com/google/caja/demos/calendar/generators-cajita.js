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
 * Rules that generate parts of a date.
 * There are separate generators for years, months, weeks, etc. which allows us
 * to decouple parts of RRULES like BYMONTH from BYDAY.
 *
 * A generator is represented as <pre>
 * {
 *   generate: function (builder) { ...; return Boolean(dateGenerated); }
 *   reset: function () { ... } // reset to start from dtStart
 * }</pre>
 *
 * The builder is an array containing a date value.  The array may be modified
 * in place to change the date.  A generator for a certain period may modify
 * its period only, and should return false when there are no more of its
 * period in the larger period, and should ignore smaller periods entirely.
 * E.g., a month generator should look at the year and generate the next
 * month in that year, ignoring the day completely.
 *
 * Note: the builder does not contain a normalized date.  If a month generator
 * increments the month from 20080130, the resulting date can be 20080230.
 * It is then the day generators responsibility to return false if there is
 * no valid day in February, or to produce a day in the appropriate range.
 *
 * The generate function generates the next period by modifying builder, and
 * returns true iff there was a new period could be generated.
 *
 * @author mikesamuel@gmail.com
 */


/** @namespace */
var generators = {};

/**
 * the maximum number of years generated between instances.
 * Year generators are throttled so that they do not generate more than this
 * many years of instances without a call to the <code>workDone</code> method.
 *
 * Note: this counts the number of years generated, not a span of actual years,
 * so for FREQ=YEARLY;INTERVAL=4 the generator would try 100 individual years
 * over a span of 400 years before giving up and concluding that the rule
 * generates no usable dates.
 *
 * @type number
 */
generators.MAX_YEARS_BETWEEN_INSTANCES = 100;

/**
 * An opaque signal to higher layers that a throttled generator has reached its
 * limit.
 */
generators.STOP_ITERATION = 'Stop Iteration';

/**
 * constructs a throttled generator that generates years successively counting
 * from the first year passed in.
 * @param {number} interval number of years to advance each step.
 * @param {number} dtStart date value
 * @return a generator which generates the year in dtStart the first time called
 *     and interval + last return value on subsequent calls.
 */
generators.serialYearGenerator = function (interval, dtStart) {
  /** the last year seen */
  var year;
  var throttle;

  function reset() {
    year = time.year(dtStart) - interval;
    throttle = generators.MAX_YEARS_BETWEEN_INSTANCES;
  }

  var MAX_YEAR = time.year(time.MAX_DATE_VALUE);

  function generate(builder) {
    // Make sure er halt even if the rrule is bad.
    // Rules like
    //   FREQ=YEARLY;BYMONTHDAY=30;BYMONTH=2
    // should halt

    if (--throttle < 0) {
      throw generators.STOP_ITERATION;
    }
    year += interval;
    if (year > MAX_YEAR) { return false; }
    builder[0] = time.withYear(builder[0], year);
    return true;
  }

  function workDone() {
    throttle = generators.MAX_YEARS_BETWEEN_INSTANCES;
  }

  return cajita.freeze({ generate: generate, workDone: workDone, reset: reset });
};

/**
 * constructs a generator that generates months in the given builder's year
 * successively counting from the first month passed in.
 * @param {number} interval number of months to advance each step.
 * @param {number} dtStart date value.
 * @return a generator which yields the month in dtStart the first time called
 *   and interval + last return value on subsequent calls.
 */
generators.serialMonthGenerator = function (interval, dtStart) {
  var year, month;

  function reset() {
    year = time.year(dtStart);
    month = time.month(dtStart) - interval;
    while (month < 1) {
      month += 12;
      --year;
    }
  }

  function generate(builder) {
    var nmonth;
    var byear = time.year(builder[0]);
    if (year !== byear) {
      var monthsBetween = (byear - year) * 12 - (month - 1);
      nmonth = ((interval - (monthsBetween % interval)) % interval) + 1;
      if (nmonth > 12) {
        // Don't update year so that the difference calculation above is
        // correct when this function is reentered with a different year.
        return false;
      }
      year = byear;
    } else {
      nmonth = month + interval;
      if (nmonth > 12) {
        return false;
      }
    }
    month = nmonth;
    builder[0] = time.withMonth(builder[0], month);
    return true;
  }

  return cajita.freeze({ generate: generate, reset: reset });
};

/**
 * constructs a generator that generates every day in the current month that
 * is an integer multiple of interval days from dtStart.
 */
generators.serialDayGenerator = function (interval, dtStart) {
  var year, month, date;
  /** ndays in the last month encountered */
  var nDays;

  function reset() {
    // Step back one interval
    var dtStartMinus1 = time.plusDays(dtStart, -interval);
     
    year = time.year(dtStartMinus1);
    month = time.month(dtStartMinus1);
    date = time.day(dtStartMinus1);
    nDays = time.daysInMonth(year, month);
  }

  function generate(builder) {
    var ndate;
    var byear = time.year(builder[0]), bmonth = time.month(builder[0]);
    if (year === byear && month === bmonth) {
      ndate = date + interval;
      if (ndate > nDays) {
        return false;
      }
    } else {
      nDays = time.daysInMonth(byear, bmonth);
      if (interval !== 1) {
        // Calculate the number of days between the first of the new
        // month andthe old date and extend it to make it an integer
        // multiple of interval.
        var daysBetween = time.daysBetween(
             time.date(byear, bmonth, 1),
             time.date(year, month, date));
        ndate = ((interval - (daysBetween % interval)) % interval) + 1;
        if (ndate > nDays) {
          // Need to early out without updating year or month so that the
          // next time we enter, with a different month, the daysBetween
          // call above compares against the proper last date.
          // This can happen if e.g. interval > 31.
          return false;
        }
      } else {
        ndate = 1;
      }
      year = byear;
      month = bmonth;
    }
    date = ndate;
    builder[0] = time.withDay(builder[0], date);
    return true;
  }

  return cajita.freeze({ generate: generate, reset: reset });
};

/**
 * constructs a generator that yields the specified months in increasing order
 * for each year.
 * @param {Array.<number>} months values in [1-12]
 * @param {number} dtStart date value
 */
generators.byMonthGenerator = function (months, dtStart) {
  months = time_util.uniquify(months);

  var i;  // index into months.
  var year;
  function reset() {
    year = time.year(dtStart);
    i = 0;
  }

  function generate(builder) {
    var byear = time.year(builder[0]);
    if (year !== byear) {
      i = 0;
      year = byear;
    }
    if (i >= months.length) { return false; }
    builder[0] = time.withMonth(builder[0], months[i++]);
    return true;
  }

  return cajita.freeze({ generate: generate, reset: reset });
};

/**
 * constructs a function that yields the specified dates
 * (possibly relative to end of month) in increasing order
 * for each month seen.
 * @param {Array.<number>} dates elements in [-31,31] !== 0
 * @param {number} dtStart date value
 */
generators.byMonthDayGenerator = function (dates, dtStart) {
  dates = time_util.uniquify(dates);

  var year;
  var month;
  /**
   * list of absolute generated dates for the current month, guaranteed to
   * be in [1,time.daysInMonth(month)]
   */
  var posDates;
  /** index of next date to return */
  var i;

  function reset() {
    year = time.year(dtStart);
    month = time.month(dtStart);
    posDates = null;
    i = 0;

    convertDatesToAbsolute();
  }

  function convertDatesToAbsolute() {
    var absDates = [];
    var nDays = time.daysInMonth(year, month);
    for (var j = dates.length; -- j >= 0;) {
      var date = dates[j];
      if (date < 0) {
        date += nDays + 1;
      }
      if (date >= 1 && date <= nDays) {
        absDates.push(date);
      }
    }
    posDates = time_util.uniquify(absDates);
  }

  function generate(builder) {
    var byear = time.year(builder[0]);
    var bmonth = time.month(builder[0]);
    if (year !== byear || month !== bmonth) {
      year = byear;
      month = bmonth;
      convertDatesToAbsolute();
      i = 0;
    }
    if (i >= posDates.length) { return false; }
    builder[0] = time.withDay(builder[0], posDates[i++]);
    return true;
  }

  return cajita.freeze({ generate: generate, reset: reset });
};

/**
 * constructs a day generator based on a BYDAY rule.
 *
 * @param {Array.<WDayNum>} days day of week, number pairs,
 *   e.g. SU,3MO means every sunday and the 3rd monday.
 * @param {boolean} weeksInYear Are the week numbers meant to be weeks in the
 *   current year, or weeks in the current month?
 * @param {number} dtStart a date value
 */
generators.byDayGenerator = function (days, weeksInYear, dtStart) {
  var year;
  var month;
  /** list of generated dates for the current month */
  var dates;
  /** index into dates of next date to return */
  var i;

  function reset() {
    year = time.year(dtStart);
    month = time.month(dtStart);
    i = 0;
    generateDates();
  }

  function generateDates() {
    var nDays;
    // The week day of the first day in the month or year (see weeksInYear).
    var dow0;
    var nDaysInMonth = time.daysInMonth(year, month);
    // Index of the first day of the month in the month or year.
    var d0;

    if (weeksInYear) {
      nDays = time.daysInYear(year);
      dow0 = time.weekDayOf(time.date(year, 1, 1));
      d0 = time.dayOfYear(time.date(year, month, 1));
    } else {
      nDays = nDaysInMonth;
      dow0 = time.weekDayOf(time.date(year, month, 1));
      d0 = 0;
    }

    // An index not greater than the first week of the month in the month
    // or year.
    var w0 = (d0 / 7) | 0;

    // Iterate through days and resolve each [week, day of week] pair to a
    // day of the month.
    var udates = {};
    for (var j = 0; j < days.length; ++j) {
      var wdayNum = days[j];
      if (0 !== wdayNum.num) {
        var date = time_util.dayNumToDate(
            dow0, nDays, wdayNum.num, wdayNum.wday, d0, nDaysInMonth);
        if (date) { udates[date] = true; }
      } else {
        var wn = w0 + 6;
        for (var w = w0; w <= wn; ++w) {
          var date = time_util.dayNumToDate(
              dow0, nDays, w, wdayNum.wday, d0, nDaysInMonth);
          if (date) { udates[date] = true; }
        }
      }
    }
    dates = [];
    for (var k in udates) { dates.push(Number(k)); }
    dates.sort(time_util.numericComparator);
  }

  function generate(builder) {
    var byear = time.year(builder[0]), bmonth = time.month(builder[0]);
    if (year !== byear || month !== bmonth) {
      year = byear;
      month = bmonth;

      generateDates();
      // start at the beginning of the month
      i = 0;
    }
    if (i >= dates.length) { return false; }
    builder[0] = time.withDay(builder[0], dates[i++]);
    return true;
  }

  return cajita.freeze({ generate: generate, reset: reset });
};

/**
 * constructs a generator that yields each day in the current month that falls
 * in one of the given weeks of the year.
 * @param {Array.<number>} weekNos (elements in [-53,53] !== 0) week numbers.
 * @param {number} wkst in WeekDay, day of the week on which the week starts.
 * @param {number} dtStart a date value.
 */
generators.byWeekNoGenerator = function (weekNos, wkst, dtStart) {
  weekNos = time_util.uniquify(weekNos);

  var year;
  var month;
  /** number of weeks in the last year seen */
  var weeksInYear;
  /** dates generated anew for each month seen */
  var dates;
  /** index into dates */
  var i;

  /**
   * day of the year of the start of week 1 of the current year.
   * Since week 1 may start on the previous year, this may be negative.
   */
  var doyOfStartOfWeek1;

  function reset() {
    year = time.year(dtStart);
    month = time.month(dtStart);
    i = 0;

    checkYear();
    checkMonth();
  }

  function checkYear() {
    // If the first day of jan is wkst, then there are 7.
    // If the first day of jan is wkst + 1, then there are 6.
    // If the first day of jan is wkst + 6, then there is 1.
    var dowJan1 = time.weekDayOf(time.date(year, 1, 1));
    var nDaysInFirstWeek = 7 - ((7 + dowJan1 - wkst) % 7);
    // number of days not in any week
    var nOrphanedDays = 0;
    // According to RFC 2445
    //     Week number one of the calendar year is the first week which
    //     contains at least four (4) days in that calendar year.
    if (nDaysInFirstWeek < 4) {
      nOrphanedDays = nDaysInFirstWeek;
      nDaysInFirstWeek = 7;
    }

    // Calculate the day of year (possibly negative) of the start of the
    // first week in the year.  This day must be of wkst.
    doyOfStartOfWeek1 = nDaysInFirstWeek - 7 + nOrphanedDays;

    weeksInYear = ((time.daysInYear(year) - nOrphanedDays + 6) / 7) | 0;
  }

  function checkMonth() {
    // The day of the year of the 1st day in the month.
    var doyOfMonth1 = time.dayOfYear(time.date(year, month, 1));
    // The week of the year of the 1st day of the month.  approximate.
    var weekOfMonth = (((doyOfMonth1 - doyOfStartOfWeek1) / 7) | 0) + 1;
    // Number of days in the month.
    var nDays = time.daysInMonth(year, month);

    // Generate the dates in the month
    var udates = {};
    for (var j = 0; j < weekNos.length; ++j) {
      var weekNo = weekNos[j];
      if (weekNo < 0) {
        weekNo += weeksInYear + 1;
      }
      if (weekNo >= weekOfMonth - 1 && weekNo <= weekOfMonth + 6) {
        for (var d = 0; d < 7; ++d) {
          var date = (
              (weekNo - 1) * 7 + d + doyOfStartOfWeek1 - doyOfMonth1) + 1;
          if (date >= 1 && date <= nDays) {
            udates[date] = true;
          }
        }
      }
    }
    dates = [];
    for (var k in udates) { dates.push(Number(k)); }
    dates.sort(time_util.numericComparator);
  }

  function generate(builder) {
    var byear = time.year(builder[0]), bmonth = time.month(builder[0]);
    // This is a bit odd, since we're generating days within the given
    // weeks of the year within the month/year from builder.
    if (year !== byear || month !== bmonth) {
      if (year !== byear) {
        year = byear;
        checkYear();
      }
      month = bmonth;
      checkMonth();

      i = 0;
    }

    if (i >= dates.length) { return false; }
    builder[0] = time.withDay(builder[0], dates[i++]);
    return true;
  }

  return cajita.freeze({ generate: generate, reset: reset });
};

/**
 * constructs a day generator that generates dates in the current month that
 * fall on one of the given days of the year.
 * @param {Array.<number>} yearDays elements in [-366,366] !== 0
 * @param {number} dtStart a date value.
 */
generators.byYearDayGenerator = function (yearDays, dtStart) {
  yearDays = time_util.uniquify(yearDays);

  var year;
  var month;
  var dates;  // Absolute dates in the current month.
  var i;  // Index into dates.

  function reset() {
    year = time.year(dtStart);
    month = time.month(dtStart);
    i = 0;
    checkMonth();
  }

  function checkMonth() {
    // Now, calculate the first week of the month.
    var doyOfMonth1 = time.dayOfYear(time.date(year, month, 1));
    var nDays = time.daysInMonth(year, month);
    var nYearDays = time.daysInYear(year);
    var udates = {};
    for (var j = 0; j < yearDays.length; j++) {
      var yearDay = yearDays[j];
      if (yearDay < 0) { yearDay += nYearDays + 1; }
      var date = yearDay - doyOfMonth1;
      if (date >= 1 && date <= nDays) { udates[date] = true; }
    }
    dates = [];
    for (var k in udates) { dates.push(Number(k)); }
    dates.sort(time_util.numericComparator);
  }

  function generate(builder) {
    var byear = time.year(builder[0]), bmonth = time.month(builder[0]);
    if (year !== byear || month !== bmonth) {
      year = byear;
      month = bmonth;
      checkMonth();
      i = 0;
    }
    if (i >= dates.length) { return false; }
    builder[0] = time.withDay(builder[0], dates[i++]);
    return true;
  }

  return cajita.freeze({ generate: generate, reset: reset });
};

cajita.freeze(generators);
