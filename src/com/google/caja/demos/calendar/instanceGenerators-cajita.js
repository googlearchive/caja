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
 * An instance generator operates on groups of generators to generate full
 * dates, and has the same form as a generator.
 *
 * @author mikesamuel@gmail.com
 */


/** @namespace */
var instanceGenerators = {};


/**
 * a collector that yields each date in the period without doing any set
 * collecting.
 *
 * @param {Object} filter a filter as described in filters.js.
 * @param {Object} yearGenerator
 *     a throttled generator as described in generators.js.
 * @param {Object} monthGenerator a generator as described in generators.js.
 * @param {Object} dayGenerator a generator as described in generators.js.
 */
instanceGenerators.serialInstanceGenerator = function (
    filter, yearGenerator, monthGenerator, dayGenerator) {
  function generate(builder) {
    // Cascade through periods to compute the next date
    do {
      // until we run out of days in the current month
      while (!dayGenerator.generate(builder)) {
        // until we run out of months in the current year
        while (!monthGenerator.generate(builder)) {
          // if there are more years available fetch one
          if (!yearGenerator.generate(builder)) {
            // otherwise the recurrence is exhausted
            return false;
          }
        }
      }
      // apply filters to generated dates
    } while (!filter(builder[0]));

    return true;
  }

  function reset(builder) {}

  return cajita.freeze({ generate: generate, reset: reset });
};

/**
 * @param {Array.<number>} setPos indices into all the dates for one of the
 *    recurrences primary periods (a MONTH for FREQ=MONTHLY).
 * @param {Frequency} freq the primary period which defines how many dates
 *    are collected before applying the BYSETPOS rules.
 * @param {WeekDay} wkst the day of the week on which the week starts.
 * @param {Object} filter a filter as described in filters.js.
 * @param {Object} yearGenerator
 *     a throttled generator as described in generators.js.
 * @param {Object} monthGenerator a generator as described in generators.js.
 * @param {Object} dayGenerator a generator as described in generators.js.
 */
instanceGenerators.bySetPosInstanceGenerator = function (
      setPos, freq, wkst, filter, yearGenerator, monthGenerator, dayGenerator) {
  setPos = time_util.uniquify(setPos);

  // Create a simpler generator to generate the dates for a primary period.
  var serialInstanceGenerator = instanceGenerators.serialInstanceGenerator(
      filter, yearGenerator, monthGenerator, dayGenerator);

  // True if all of the BYSETPOS indices are positive.  Negative indices are
  // relative to the end of the period.
  // If they are then we need not iterate every period to exhaustion which is
  // nice for rules like FREQ=YEARLY;BYDAY=MO,TU,WE,TH,FR;BYSETPOS=50 --
  // the fiftieth weekday of the year.
  var allPositive = setPos[0] > 0;  // since setPos is sorted.
  // The maximum SETPOS used to short circuit a period if we have enough.
  var maxPos = setPos[setPos.length - 1];

  var pushback = null;
  /**
   * Is this the first instance we generate?
   * We need to know so that we don't clobber dtStart.
   */
  var first = true;
  /** Do we need to halt iteration once the current set has been used? */
  var done = false;

  /** The elements in the current set, filtered by set pos */
  var candidates;
  /**
   * index into candidates.  The number of elements in candidates already
   * consumed.
   */
  var i;

  function reset() {
    pushback = null;
    first = true;
    done = false;
    candidates = null;
    i = 0;
  }

  function generate(builder) {
    while (null === candidates || i >= candidates.length) {
      if (done) { return false; }

      // (1) Make sure that builder is appropriately initialized so that
      // we only generate instances in the next set

      var d0 = null;
      if (null !== pushback) {
        d0 = pushback;
        builder[0] = time.withDate(builder[0], d0);
        pushback = null;
      } else if (!first) {
        // We need to skip ahead to the next item since we didn't exhaust
        // the last period.
        switch (freq) {
          case rrule.Frequency.YEARLY:
            if (!yearGenerator.generate(builder)) { return false; }
            // fallthru
          case rrule.Frequency.MONTHLY:
            while (!monthGenerator.generate(builder)) {
              if (!yearGenerator.generate(builder)) { return false; }
            }
            break;
          case rrule.Frequency.WEEKLY:
            // Consume because just incrementing date doesn't do anything.
            var nextWeek = time_util.nextWeekStart(builder[0], wkst);
            do {
              if (!serialInstanceGenerator.generate(builder)) {
                return false;
              }
            } while (builder[0] < nextWeek);
            d0 = time.toDate(builder[0]);
            break;
          default:
            break;
        }
      } else {
        first = false;
      }

      // (2) Build a set of the dates in the year/month/week that match
      // the other rule.
      var dates = [];
      if (null !== d0) { dates.push(d0); }

      // Optimization: if min(bySetPos) > 0 then we already have absolute
      // positions, so we don't need to generate all of the instances for
      // the period.
      // This speeds up things like the first weekday of the year:
      //     RRULE:FREQ=YEARLY;BYDAY=MO,TU,WE,TH,FR,BYSETPOS=1
      // that would otherwise generate 260+ instances per one emitted
      // TODO(mikesamuel): this may be premature.  If needed, We could
      // improve more generally by inferring a BYMONTH generator based on
      // distribution of set positions within the year.
      var limit = allPositive ? maxPos : Infinity;

      while (limit > dates.length) {
        if (!serialInstanceGenerator.generate(builder)) {
          // If we can't generate any, then make sure we return false
          // once the instances we have generated are exhausted.
          // If this is returning false due to some artificial limit, such
          // as the 100 year limit in serialYearGenerator, then we exit
          // via an exception because otherwise we would pick the wrong
          // elements for some uSetPoses that contain negative elements.
          done = true;
        }
        var d = time.toDate(builder[0]);
        var contained = false;
        if (null === d0) {
          d0 = d;
          contained = true;
        } else {
          switch (freq) {
            case rrule.Frequency.WEEKLY:
              var nb = time.daysBetween(d, d0);
              // Two dates (d, d0) are in the same week
              // if there isn't a whole week in between them and the
              // later day is later in the week than the earlier day.
              contained = (
                  nb < 7
                  && ((7 + time.weekDayOf(d) - wkst) % 7)
                  > ((7 + time.weekDayOf(d0) - wkst) % 7));
              break;
            case rrule.Frequency.MONTHLY:
              contained = time.sameMonth(d0, d);
              break;
            case rrule.Frequency.YEARLY:
              contained = time.year(d0) === time.year(d);
              break;
            default:
              break;
          }
        }
        if (contained) {
          dates.push(d);
        } else {
          // reached end of the set
          pushback = d;  // save d so we can use it later
          break;
        }
      }

      // (3) Resolve the positions to absolute positions and order them
      var absSetPos;
      if (allPositive) {
        absSetPos = setPos;
      } else {
        var uAbsSetPos = {};
        for (var j = setPos.length; --j >= 0;) {
          var p = setPos[j];
          if (p < 0) { p = dates.length + p + 1; }
          uAbsSetPos[p] = true;
        }
        absSetPos = [];
        for (var k in uAbsSetPos) { absSetPos.push(Number(k)); }
        absSetPos.sort(time_util.numericComparator);
      }

      candidates = [];
      for (var j = 0; j < absSetPos.length; ++j) {
        var p = absSetPos[j] - 1;
        if (p >= 0 && p < dates.length) {
          candidates.push(dates[p]);
        }
      }
      i = 0;
      if (!candidates.length) {
        // none in this region, so keep looking
        candidates = null;
        continue;
      }
    }
    // (5) Emit a date.  It will be checked against the end condition and
    // dtStart elsewhere.
    var d = candidates[i++];
    builder[0] = time.withDate(builder[0], d);
    return true;
  }

  return cajita.freeze({ generate: generate, reset: reset });
};

cajita.freeze(instanceGenerators);
