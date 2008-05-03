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
 * An implementation of RFC 2445 RRULEs in Cajita.
 *
 * <h4>Glossary</h4>
 * Period - year|month|day|...<br>
 * Day of the week - an int in [0-6].  See RRULE_WDAY_* in rrule.js<br>
 * Day of the year - zero indexed in [0,365]<br>
 * Day of the month - 1 indexed in [1,31]<br>
 * Month - 1 indexed integer in [1,12]
 * Recurrence iterator - an object that produces a series of
 *   monotonically increasing dates.  Provides next, advance, and reset
 *   operations.
 *
 * <h4>Abstractions</h4>
 * Generator - a function corresponding to an RRULE part that takes a date and
 *   returns a later (year or month or day depending on its period) within the
 *   next larger period.
 *   A generator ignores all periods in its input smaller than its period.
 * <p>
 * Filter - a function that returns true iff the given date matches the subrule.
 * <p>
 * Condition - returns true if the given date is past the end of the recurrence.
 *
 * <p>All the generators and conditions are stateful, but filters are not.
 * Generators and conditions can be reset via their reset method.
 *
 * A recurrence iterator has the form:<pre>
 * {
 *   reset: function () { ... },  // resets to dtStart
 *   next: function () { ...; return time.date* },  // returns a UTC date value
 *   hasNext: function () { return Boolean(...); },  // true if next() will work
 *   // Consumes values until the value returned by next is >= dateValueUtc.
 *   // Possibly more efficient than just consuming values in a loop.
 *   advanceTo: function (dateValueUtc) { ... }
 * }</pre>
 *
 * @author mikesamuel@gmail.com
 */


/** @namespace */
var rrule = {};

(function (module) {

/** @enum {number} */
var Frequency = caja.freeze({
  SECONDLY: 0,
  MINUTELY: 1,
  HOURLY: 2,
  DAILY: 3,
  WEEKLY: 4,
  MONTHLY: 5,
  YEARLY: 6
});

/**
 * A weekday & number pattern.
 * 2TU -> 2nd Tuesday of the month or year.
 * WE -> Every Wednesday of the month or year.
 * -1FR -> The last Friday of the month or year.
 */
function WeekDayNum(ical) {
  var m = ical.match(/^(-?\d+)?(MO|TU|WE|TH|FR|SA|SU)$/i);
  if (!m) { throw new Error('Invalid weekday number: ' + ical); }
  return caja.freeze({
    wday: WeekDay[m[2].toUpperCase()],
    num: Number(m[1]) || 0
  });
}

/**
 * create a recurrence iterator from an RRULE or EXRULE.
 * @param {Object} rule the recurrence rule to iterate with a getAttribute
 *     method that returns the string value corresponding to the given key.
 * @param {number} dtStart the start of the series, in timezone.
 * @param {function} timezone the timezone to iterate in.
 *   A function from times in one timezone to times in another.
 *   Takes a date or date-time and
 * @return {Object} with methods reset, next, hasNext, and advanceTo.
 */
function createRecurrenceIterator(rule, dtStart, timezone) {
  console.assert('function' === typeof timezone);
  console.assert('number' === typeof dtStart);
  console.assert('function' === typeof rule.getAttribute);

  function soleValue(name) {
    var values = rule.getAttribute(name);
    console.assert(values === null || values instanceof Array);
    return values && values[0].toUpperCase();
  }

  function apply(array, xform) {
    console.assert(typeof xform === 'function');
    var out = [];
    if (array !== null) {
      for (var i = -1, k = -1, n = array.length; ++i < n;) {
        var xformed = xform(array[i]);
        if (xformed !== undefined) {
          out[++k] = xformed;
        }
      }
    }
    return out;
  }

  function intBetween(minInclusive, maxInclusive) {
    return function (v) {
      var n = Number(v);
      if (n !== (n | 0)) { return undefined; }  // excludes non-ints & NaN
      if (!(n >= minInclusive && n <= maxInclusive)) { return undefined; }
      return n;
    };
  }

  function intWithMagBetween(minInclusive, maxInclusive) {
    return function (v) {
      var n = Number(v);
      if (n !== (n | 0)) { return undefined; }  // excludes non-ints & NaN
      var mag = Math.abs(n);
      if (!(mag >= minInclusive && mag <= maxInclusive)) { return undefined; }
      return n;
    };
  }

  function positiveInt(v) {
    var n = Number(v);
    if (n !== (n | 0)) { return undefined; }  // excludes non-ints & NaN
    if (!(n > 0)) { return undefined; }
    return n;
  }
  
  var freq = Frequency[soleValue('FREQ')];
  var wkst = WeekDay[soleValue('WKST')];
  if (wkst === undefined) { wkst = WeekDay.MO; }
  var untilUtc = soleValue('UNTIL') || null;
  if (untilUtc) { untilUtc = time.parseIcal(untilUtc.replace(/Z$/, '')); }
  var count = positiveInt(soleValue('COUNT')) || null;
  var interval = positiveInt(soleValue('INTERVAL')) || 1;
  var byDay = apply(rule.getAttribute('BYDAY'), WeekDayNum);
  var byMonth = apply(rule.getAttribute('BYMONTH'), intBetween(1, 12));
  var byMonthDay = apply(rule.getAttribute('BYMONTHDAY'),
                         intWithMagBetween(1, 31));
  var byWeekNo = apply(rule.getAttribute('BYWEEKNO'), intWithMagBetween(1, 53));
  var byYearDay = apply(rule.getAttribute('BYYEARDAY'),
                        intWithMagBetween(1, 366));
  var bySetPos = apply(rule.getAttribute('BYSETPOS'),
                       intWithMagBetween(1, Infinity));
  var byHour = apply(rule.getAttribute('BYHOUR'), intBetween(0, 23));
  var byMinute = apply(rule.getAttribute('BYMINUTE'), intBetween(0, 59));
  var bySecond = apply(rule.getAttribute('BYSECOND'), intBetween(0, 59));

  // Make sure that BYMINUTE, BYHOUR, and BYSECOND rules are respected if they
  // have exactly one iteration, so not causing frequency to exceed daily.
  var startTime = null;
  if (1 === (byHour.length | byMinute.length | bySecond.length)
      && !time.isDate(dtStart)) {
    startTime = time.dateTime(
          0, 1, 1,
          1 === byHour.length ? byHour[0] : tv.hour(),
          1 === byMinute.length ? byMinute[0] : tv.minute(),
          1 === bySecond.length ? bySecond[0] : tv.second());
  }

  // recurrences are implemented as a sequence of periodic generators.
  // First a year is generated, and then months, and within months, days
  var yearGenerator = generators.serialYearGenerator(
      freq === Frequency.YEARLY ? interval : 1, dtStart);
  var monthGenerator = null;
  var dayGenerator;

  // When multiple generators are specified for a period, they act as a union
  // operator.  We could have multiple generators (for day say) and then
  // run each and merge the results, but some generators are more efficient
  // than others, so to avoid generating 53 sundays and throwing away all but
  // 1 for RRULE:FREQ=YEARLY;BYDAY=TU;BYWEEKNO=1, we reimplement some of the
  // more prolific generators as filters.
  // TODO(mikesamuel): don't need a list here
  var filterList = [];

  // Choose the appropriate generators and filters.
  switch (freq) {
    case Frequency.DAILY:
      if (0 === byMonthDay.length) {
        dayGenerator = generators.serialDayGenerator(interval, dtStart);
      } else {
        dayGenerator = generators.byMonthDayGenerator(byMonthDay, dtStart);
      }
      if (0 !== byDay.length) {
        // TODO(mikesamuel): the spec is not clear on this.  Treat the week
        // numbers as weeks in the year.  This is only implemented for
        // conformance with libical.
        filterList.push(filters.byDayFilter(byDay, true, wkst));
      }
      break;
    case Frequency.WEEKLY:
      // week is not considered a period because a week may span multiple
      // months &| years.  There are no week generators, but so a filter is
      // used to make sure that FREQ=WEEKLY;INTERVAL=2 only generates dates
      // within the proper week.
      if (0 !== byDay.length) {
        dayGenerator = generators.byDayGenerator(byDay, false, dtStart);
        if (interval > 1) {
          filterList.push(filters.weekIntervalFilter(interval, wkst, dtStart));
        }
      } else {
        dayGenerator = generators.serialDayGenerator(interval * 7, dtStart);
      }
      if (0 !== byMonthDay.length) {
        filterList.push(filters.byMonthDayFilter(byMonthDay));
      }
      break;
    case Frequency.YEARLY:
      if (0 !== byYearDay.length) {
        // The BYYEARDAY rule part specifies a COMMA separated list of days of
        // the year. Valid values are 1 to 366 or -366 to -1. For example, -1
        // represents the last day of the year (December 31st) and -306
        // represents the 306th to the last day of the year (March 1st).
        dayGenerator = generators.byYearDayGenerator(byYearDay, dtStart);
        if (0 !== byDay.length) {
          filterList.push(filters.byDayFilter(byDay, true, wkst));
        }
        if (0 !== byMonthDay.length) {
          filterList.push(filters.byMonthDayFilter(byMonthDay));
        }
        // TODO(mikesamuel): filter byWeekNo and write unit tests
        break;
      }
      // fallthru to monthly cases
    case Frequency.MONTHLY:
      if (0 !== byMonthDay.length) {
        // The BYMONTHDAY rule part specifies a COMMA separated list of days
        // of the month. Valid values are 1 to 31 or -31 to -1. For example,
        // -10 represents the tenth to the last day of the month.
        dayGenerator = generators.byMonthDayGenerator(byMonthDay, dtStart);
        if (0 !== byDay.length) {
          filterList.push(
              filters.byDayFilter(byDay, Frequency.YEARLY === freq, wkst));
        }
        // TODO(mikesamuel): filter byWeekNo and write unit tests
      } else if (0 !== byWeekNo.length && Frequency.YEARLY === freq) {
        // The BYWEEKNO rule part specifies a COMMA separated list of ordinals
        // specifying weeks of the year.  This rule part is only valid for
        // YEARLY rules.
        dayGenerator = generators.byWeekNoGenerator(byWeekNo, wkst, dtStart);
        if (0 !== byDay.length) {
          filterList.push(filters.byDayFilter(byDay, true, wkst));
        }
      } else if (0 !== byDay.length) {
        // Each BYDAY value can also be preceded by a positive (n) or negative
        // (-n) integer. If present, this indicates the nth occurrence of the
        // specific day within the MONTHLY or YEARLY RRULE. For example,
        // within a MONTHLY rule, +1MO (or simply 1MO) represents the first
        // Monday within the month, whereas -1MO represents the last Monday of
        // the month. If an integer modifier is not present, it means all days
        // of this type within the specified frequency. For example, within a
        // MONTHLY rule, MO represents all Mondays within the month.
        dayGenerator = generators.byDayGenerator(
            byDay, Frequency.YEARLY === freq && 0 === byMonth.length, dtStart);
      } else {
        if (Frequency.YEARLY === freq) {
          monthGenerator = generators.byMonthGenerator(
              [ time.month(dtStart) ], dtStart);
        }
        dayGenerator = generators.byMonthDayGenerator(
            [ time.day(dtStart) ], dtStart);
      }
      break;
    default:
      throw new Error("Can't iterate more frequently than daily");
  }

  // generator inference common to all periods
  if (0 !== byMonth.length) {
    monthGenerator = generators.byMonthGenerator(byMonth, dtStart);
  } else if (null === monthGenerator) {
    monthGenerator = generators.serialMonthGenerator(
        freq === Frequency.MONTHLY ? interval : 1, dtStart);
  }

  // The condition tells the iterator when to halt.
  // The condition is exclusive, so the date that triggers it will not be
  // included.
  var condition;
  var canShortcutAdvance = true;
  if (count !== null) {
    condition = conditions.countCondition(count);
    // We can't shortcut because the countCondition must see every generated
    // instance.
    // TODO(mikesamuel): if count is large, we might try predicting the end
    // date so that we can convert the COUNT condition to an UNTIL condition.
    canShortcutAdvance = false;
  } else if (null !== untilUtc) {
    if (time.isDate(untilUtc) !== time.isDate(dtStart)) {
      // TODO(mikesamuel): warn
      if (time.isDate(dtStart)) {
        untilUtc = time.toDate(untilUtc);
      } else {
        untilUtc = time.withTime(untilUtc, 0, 0);
      }
    }
    condition = conditions.untilCondition(untilUtc);
  } else {
    condition = conditions.unboundedCondition();
  }

  // combine filters into a single function
  var filter;
  switch (filterList.length) {
    case 0:
      filter = predicates.ALWAYS_TRUE;
      break;
    case 1:
      filter = filterList[0];
      break;
    default:
      filter = predicates.and(filterList);
      break;
  }

  var instanceGenerator;
  if (bySetPos.length) {
    switch (freq) {
      case Frequency.WEEKLY:
      case Frequency.MONTHLY:
      case Frequency.YEARLY:
        instanceGenerator = instanceGenerators.bySetPosInstanceGenerator(
            bySetPos, freq, wkst, filter,
            yearGenerator, monthGenerator, dayGenerator);
        break;
      default:
        // TODO(mikesamuel): if we allow iteration more frequently than daily
        // then we will need to implement bysetpos for hours, minutes, and
        // seconds.  It should be sufficient though to simply choose the
        // instance of the set statically for every occurrence except the
        // first.
        // E.g. RRULE:FREQ=DAILY;BYHOUR=0,6,12,18;BYSETPOS=1
        // for DTSTART:20000101T130000
        // will yield
        // 20000101T180000
        // 20000102T000000
        // 20000103T000000
        // ...

        instanceGenerator = instanceGenerators.serialInstanceGenerator(
            filter, yearGenerator, monthGenerator, dayGenerator);
        break;
    }
  } else {
    instanceGenerator = instanceGenerators.serialInstanceGenerator(
        filter, yearGenerator, monthGenerator, dayGenerator);
  }

  return rruleIteratorImpl(
      dtStart, timezone, condition, filter, instanceGenerator,
      yearGenerator, monthGenerator, dayGenerator, canShortcutAdvance,
      startTime);
}

/**
 * @param {number} dtStart the start date of the recurrence
 * @param timezone the timezone that result dates should be converted
 *   <b>from</b>.  All date fields, parameters, and local variables in this
 *   class are in the tzid_ timezone, unless they carry the Utc suffix.
 * @param condition a predicate over date-values that determines when the
 *   recurrence ends, applied <b>after</b> the date is converted to UTC.
 * @param filter a function that applies secondary rules to eliminate some
 *   dates.
 * @param instanceGenerator a function that applies the various period
 *   generators to generate an entire date.
 *   This may involve generating a set of dates and discarding all but those
 *   that match the BYSETPOS rule.
 * @param yearGenerator a function that takes a date value and replaces the year
 *   field.
 *   Returns false if no more years available.
 * @param monthGenerator a function that takes a date value and
 *   replaces the month field.  Returns false if no more months
 *   available in the input's year.
 * @param dayGenerator a function that takes a date value and replaces
 *   the day of month.  Returns false if no more days available in the
 *   input's month.
 * @param {boolean} canShortcutAdvance false iff shorcutting advance would break
 *   the semantics of the iteration.  This may happen when, for example, the
 *   end condition requires that it see every item.
 * @param {number?} a date-time whose hour and minute fields should be used for
 *   the first iteration value.
 */
function rruleIteratorImpl(
    dtStart, timezone, condition, filter,
    instanceGenerator, yearGenerator, monthGenerator, dayGenerator,
    canShortcutAdvance, startTime) {

  /**
   * a date value that has been computed but not yet yielded to the user.
   * @type number?
   */
  var pendingUtc;

  /**
   * a date value used to build successive results.
   * At the start of the building process, contains the last date generated.
   * Different periods are successively inserted into it.
   * @type number
   */
  var currentDate;

  /**
   * true iff the recurrence has been exhausted.
   * @type boolean
   */
  var done;

  /**
   * A box used to shuttle the currentDate to generators for modification.
   * @type Array.<number>
   */
  var builder = [null];


  function reset() {
    condition.reset();
    yearGenerator.reset();
    monthGenerator.reset();
    dayGenerator.reset();
    instanceGenerator.reset();

    pendingUtc = null;
    done = false;

    currentDate = dtStart;
    if (startTime !== null) {
      currentDate = time.withTime(dtStart, time);
    }

    // Apply the year and month generators so that we can start with the day
    // generator on the first call to fetchNext.
    try {
      builder[0] = currentDate;
      yearGenerator.generate(builder);
      monthGenerator.generate(builder);
      currentDate = builder[0];
    } catch (ex) {  // Year generator has done too many cycles without result.
      if (ex === generators.STOP_ITERATION) {
        done = true;
      } else {
        throw ex;
      }
    }

    var dtStartUtc = timezone(dtStart, false);
    while (!done) {
      pendingUtc = generateInstance();
      if (pendingUtc === null) {
        done = true;
        break;
      } else if (pendingUtc >= dtStartUtc) {
        // We only apply the condition to the ones past dtStart to avoid
        // counting useless instances
        if (!condition.test(pendingUtc)) {
          done = true;
          pendingUtc = null;
        }
        break;
      }
    }
  }

  function hasNext() {
    if (pendingUtc === null) { fetchNext(); }
    return pendingUtc !== null;
  }

  function next() {
    if (pendingUtc === null) { fetchNext(); }
    var next = pendingUtc;
    pendingUtc = null;
    return next;
  }

  /**
   * skip over all instances of the recurrence before the given date, so that
   * the next call to {@link next} will return a date on or after the given
   * date, assuming the recurrence includes such a date.
   */
  function advanceTo(dateUtc) {
    var dateLocal = timezone(dateUtc, true);
    if (dateLocal < currentDate) {
      return;
    }
    pendingUtc = null;

    try {
      if (canShortcutAdvance) {
        // skip years before date.year
        if (time.year(currentDate) < time.year(dateLocal)) {
          builder[0] = currentDate;
          do {
            if (!yearGenerator.generate(builder)) {
              done = true;
              return;
            }
            currentDate = builder[0];
          } while (time.year(currentDate) < time.year(dateLocal));
          while (!monthGenerator.generate(builder)) {
            if (!yearGenerator.generate(builder)) {
              done = true;
              return;
            }
          }
          currentDate = builder[0];
        }
        // skip months before date.year/date.month
        while (time.year(currentDate) === time.year(dateLocal)
               && time.month(currentDate) < time.month(dateLocal)) {
          while (!monthGenerator.generate(builder)) {
            // if there are more years available fetch one
            if (!yearGenerator.generate(builder)) {
              // otherwise the recurrence is exhausted
              done = true;
              return;
            }
          }
          currentDate = builder[0];
        }
      }

      // consume any remaining instances
      while (!done) {
        var dUtc = generateInstance();
        if (dUtc === null) {
          done = true;
        } else {
          if (!condition.test(dUtc)) {
            done = true;
          } else if (dUtc >= dateUtc) {
            pendingUtc = dUtc;
            break;
          }
        }
      }
    } catch (ex) {  // Year generator has done too many cycles without result.
      // Can happen for rules like FREQ=YEARLY;INTERVAL=4;BYMONTHDAY=29 when
      // dtStart is 1 Feb 2001.
      if (ex === generators.STOP_ITERATION) {
        done = true;
      } else {
        throw ex;
      }
    }
  }

  /** calculates and stored the next date in this recurrence. */
  function fetchNext() {
    if (pendingUtc !== null || done) { return; }

    var dUtc = generateInstance();

    // check the exit condition
    if (dUtc !== null && condition.test(dUtc)) {
      pendingUtc = dUtc;
      // Tell the yearGenerator that it generated a valid date, to reset the
      // too many cycles counter.  See the catch blcok above.
      yearGenerator.workDone();
    } else {
      done = true;
    }
  }

  /**
   * make sure the iterator is monotonically increasing.
   * The local time is guaranteed to be monotonic, but because of daylight
   * savings shifts, the time in UTC may not be.
   */
  var lastUtc = time.MIN_DATE_VALUE;
  /**
   * @return {number} a date value in UTC.
   */
  function generateInstance() {
    try {
      // Make sure that the date is monotonically increasing in the output
      // timezone.
      do {
        builder[0] = currentDate;
        if (!instanceGenerator.generate(builder)) { return null; }
        currentDate = builder[0];
        // TODO(mikesamuel): apply byhour, byminute, bysecond rules here
        var dUtc = timezone(currentDate, false);
        if (dUtc > lastUtc) {
          return dUtc;
        }
      } while (true);
    } catch (ex) {
      // Year generator has done too many cycles without result.
      if (ex === generators.STOP_ITERATION) { return null; }
      throw ex;
    }
  }

  reset();

  return caja.freeze({
    reset: reset,
    next: next,
    hasNext: hasNext,
    advanceTo: advanceTo
  });
}

module.createRecurrenceIterator = createRecurrenceIterator;
module.Frequency = Frequency; 
 
})(rrule); 

caja.freeze(rrule);
