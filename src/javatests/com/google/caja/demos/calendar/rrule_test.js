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

function StubContentLine(rruleText) {
  rruleText = rruleText
      .replace(/(\r\n?|\n)[ \t]/g, '')  // Fold content lines
      .replace(/^(RRULE|EXRULE):/, '');  // Remove content line name
  var attribs = {};
  var parts = rruleText.split(/;/);
  for (var i = parts.length; --i >= 0;) {
    var m = parts[i].match(/^(\w+)=(.*)/);
    attribs[m[1]] = m[2].split(/,/g);
  }
  this.attribs = attribs;
}
StubContentLine.prototype.getAttribute = function (k) {
  return k in this.attribs ? this.attribs[k].slice(0) : null;
};

/**
 * @param {string} rruleText
 * @param {number} dtStart a dateValue
 * @param {number} limit max number of values to generate
 * @param {string} golden comma separated date values in ical format.
 * @param {number} opt_advanceTo optional date value to advance to before
 *     iterating.
 * @param {Function} opt_tz the timezone to use.  defaults to UTC
 */
function runRecurrenceIteratorTest(
    rruleText, dtStart, limit, golden, opt_advanceTo, opt_tz) {
  console.group('RRULE=' + rruleText);

  var advanceTo = opt_advanceTo || null;
  var tz = opt_tz || timezone.utc;

  var ri = rrule.createRecurrenceIterator(
      new StubContentLine(rruleText), dtStart, tz);
  if (null !== advanceTo) {
    ri.advanceTo(advanceTo);
  }
  var sb = [];
  var k = 0;
  while (ri.hasNext() && --limit >= 0) {
    sb.push(time.toIcal(ri.next()));
  }
  if (limit < 0) { sb.push("..."); }
  assertEquals(golden, sb.join(','));
  console.groupEnd();
}

function testFrequencyLimits() {
  try {
    rrule.createRecurrenceIterator(
      parseRRule(
          "RRULE:FREQ=SECONDLY;BYSECOND=0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,"
          + "15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,"
          + "30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,"
          + "45,46,47,48,49,50,51,52,53,54,55,56,57,58,59"),
      time.parseIcal("20000101"), timezones.utc);
    fail("Don't do that");
  } catch (ex) {
    // pass
  }
}

function testSimpleDaily() {
  runRecurrenceIteratorTest(
      "RRULE:FREQ=DAILY", time.parseIcal("20060120"), 5,
      "20060120,20060121,20060122,20060123,20060124,...");
}

function testSimpleWeekly() {
  runRecurrenceIteratorTest(
      "RRULE:FREQ=WEEKLY", time.parseIcal("20060120"), 5,
      "20060120,20060127,20060203,20060210,20060217,...");
}

function testSimpleMonthly() {
  runRecurrenceIteratorTest(
      "RRULE:FREQ=MONTHLY", time.parseIcal("20060120"), 5,
      "20060120,20060220,20060320,20060420,20060520,...");
}

function testSimpleYearly() {
  runRecurrenceIteratorTest(
      "RRULE:FREQ=YEARLY", time.parseIcal("20060120"), 5,
      "20060120,20070120,20080120,20090120,20100120,...");
}

// from section 4.3.10
function testMultipleByParts() {
  runRecurrenceIteratorTest(
      "RRULE:FREQ=YEARLY;INTERVAL=2;BYMONTH=1;BYDAY=SU",
      time.parseIcal("19970105"), 8,
      "19970105,19970112,19970119,19970126," +
      "19990103,19990110,19990117,19990124,...");
}

function testCountWithInterval() {
  runRecurrenceIteratorTest(
      "RRULE:FREQ=DAILY;COUNT=10;INTERVAL=2",
      time.parseIcal("19970105"), 11,
      "19970105,19970107,19970109,19970111,19970113," +
      "19970115,19970117,19970119,19970121,19970123");
}

// from section 4.6.5
function testNegativeOffsets() {
  runRecurrenceIteratorTest(
      "RRULE:FREQ=YEARLY;BYDAY=-1SU;BYMONTH=10",
      time.parseIcal("19970105"), 5,
      "19971026,19981025,19991031,20001029,20011028,...");
  runRecurrenceIteratorTest(
      "RRULE:FREQ=YEARLY;BYDAY=1SU;BYMONTH=4",
      time.parseIcal("19970105"), 5,
      "19970406,19980405,19990404,20000402,20010401,...");
  runRecurrenceIteratorTest(
      "RRULE:FREQ=YEARLY;BYDAY=1SU;BYMONTH=4;UNTIL=19980404T150000Z",
      time.parseIcal("19970105"), 5, "19970406");
}

// from section 4.8.5.4
function testDailyFor10Occ() {
  runRecurrenceIteratorTest(
      "RRULE:FREQ=DAILY;COUNT=10",
      time.parseIcal("19970902T090000"), 11,
      "19970902T090000,19970903T090000,19970904T090000,19970905T090000," +
      "19970906T090000,19970907T090000,19970908T090000,19970909T090000," +
      "19970910T090000,19970911T090000");

}

function testDailyUntilDec4() {
  runRecurrenceIteratorTest(
      "RRULE:FREQ=DAILY;UNTIL=19971204",
      time.parseIcal("19971128"), 11,
      "19971128,19971129,19971130,19971201,19971202,19971203,19971204");
}

function testEveryOtherDayForever() {
  runRecurrenceIteratorTest(
      "RRULE:FREQ=DAILY;INTERVAL=2",
      time.parseIcal("19971128"), 5,
      "19971128,19971130,19971202,19971204,19971206,...");
}

function testEvery10Days5Occ() {
  runRecurrenceIteratorTest(
      "RRULE:FREQ=DAILY;INTERVAL=10;COUNT=5",
      time.parseIcal("19970902"), 5,
      "19970902,19970912,19970922,19971002,19971012");
}

function goldenDateRange(dateStr, opt_interval) {
  var interval = opt_interval || 1;

  var slash = dateStr.indexOf('/');

  var d = time.parseIcal(dateStr.substring(0, slash));
  var end = time.parseIcal(dateStr.substring(slash + 1));
  var out = [];
  while (true) {
    if (d > end) { break; }
    out.push(time.toIcal(d));
    d = time.plusDays(d, interval);
  }
  return out.join(',');
}

function testEveryDayInJanuaryFor3Years() {
  runRecurrenceIteratorTest(
      "RRULE:FREQ=YEARLY;UNTIL=20000131T090000Z;\n" +
      " BYMONTH=1;BYDAY=SU,MO,TU,WE,TH,FR,SA",
      time.parseIcal("19980101"), 100,
      goldenDateRange("19980101/19980131") + ","
      + goldenDateRange("19990101/19990131") + ","
      + goldenDateRange("20000101/20000131"));
}

function testWeeklyFor10Occ() {
  runRecurrenceIteratorTest(
      "RRULE:FREQ=WEEKLY;COUNT=10",
      time.parseIcal("19970902"), 10,
      "19970902,19970909,19970916,19970923,19970930," +
      "19971007,19971014,19971021,19971028,19971104");
}

function testWeeklyUntilDec24() {
  runRecurrenceIteratorTest(
      "RRULE:FREQ=WEEKLY;UNTIL=19971224",
      time.parseIcal("19970902"), 25,
      "19970902,19970909,19970916,19970923,19970930," +
      "19971007,19971014,19971021,19971028,19971104," +
      "19971111,19971118,19971125,19971202,19971209," +
      "19971216,19971223");
}

function testEveryOtherWeekForever() {
  runRecurrenceIteratorTest(
      "RRULE:FREQ=WEEKLY;INTERVAL=2;WKST=SU",
      time.parseIcal("19970902"), 11,
      "19970902,19970916,19970930,19971014,19971028," +
      "19971111,19971125,19971209,19971223,19980106," +
      "19980120,...");
}

function testWeeklyOnTuesdayAndThursdayFor5Weeks() {
  // if UNTIL date does not match start date, then until date treated as
  // occurring on midnight.
  runRecurrenceIteratorTest(
      "RRULE:FREQ=WEEKLY;UNTIL=19971007;WKST=SU;BYDAY=TU,TH",
      time.parseIcal("19970902T090000"), 11,
      "19970902T090000,19970904T090000,19970909T090000,19970911T090000," +
      "19970916T090000,19970918T090000,19970923T090000,19970925T090000," +
      "19970930T090000,19971002T090000");
  runRecurrenceIteratorTest(
      "RRULE:FREQ=WEEKLY;UNTIL=19971007T000000Z;WKST=SU;BYDAY=TU,TH",
      time.parseIcal("19970902T090000"), 11,
      "19970902T090000,19970904T090000,19970909T090000,19970911T090000," +
      "19970916T090000,19970918T090000,19970923T090000,19970925T090000," +
      "19970930T090000,19971002T090000");
  runRecurrenceIteratorTest(
      "RRULE:FREQ=WEEKLY;COUNT=10;WKST=SU;BYDAY=TU,TH",
      time.parseIcal("19970902"), 11,
      "19970902,19970904,19970909,19970911,19970916," +
      "19970918,19970923,19970925,19970930,19971002");
}

function testEveryOtherWeekOnMWFUntilDec24() {
  runRecurrenceIteratorTest(
      "RRULE:FREQ=WEEKLY;INTERVAL=2;UNTIL=19971224T000000Z;WKST=SU;\n" +
      " BYDAY=MO,WE,FR",
      time.parseIcal("19970903T090000"), 25,
      "19970903T090000,19970905T090000,19970915T090000,19970917T090000," +
      "19970919T090000,19970929T090000,19971001T090000,19971003T090000," +
      "19971013T090000,19971015T090000,19971017T090000,19971027T090000," +
      "19971029T090000,19971031T090000,19971110T090000,19971112T090000," +
      "19971114T090000,19971124T090000,19971126T090000,19971128T090000," +
      "19971208T090000,19971210T090000,19971212T090000,19971222T090000");

  // if the UNTIL date is timed, when the start is not, the time should be
  // ignored, so we get one more instance
  runRecurrenceIteratorTest(
      "RRULE:FREQ=WEEKLY;INTERVAL=2;UNTIL=19971224T000000Z;WKST=SU;\n" +
      " BYDAY=MO,WE,FR",
      time.parseIcal("19970903"), 25,
      "19970903,19970905,19970915,19970917," +
      "19970919,19970929,19971001,19971003," +
      "19971013,19971015,19971017,19971027," +
      "19971029,19971031,19971110,19971112," +
      "19971114,19971124,19971126,19971128," +
      "19971208,19971210,19971212,19971222," +
      "19971224");

  // The below rule is not correct but is simple.  It says that daylight savings
  // starts on September 1 and ends April 1 and approximates the
  // First Sun in Apr, Last Sun in Oct rule in effect in 1997.
  function simplePacificTime(dateValue, isUtc) {
    var offsetHours = -8;
    var month = time.month(dateValue);
    var day = time.day(dateValue);
    if (!((month > 10 || month === 10 && day >= 26)
          || (month < 4 || (month === 4 && day < 6)))) {
      offsetHours = -7;
    }
    return time.plusSeconds(dateValue, offsetHours * 3600 * (isUtc ? 1 : -1));
  }

  // test with an alternate timezone
  runRecurrenceIteratorTest(
      "RRULE:FREQ=WEEKLY;INTERVAL=2;UNTIL=19971224T090000Z;WKST=SU;\n" +
      " BYDAY=MO,WE,FR",
      time.parseIcal("19970903T090000"), 25,
      "19970903T160000,19970905T160000,19970915T160000,19970917T160000," +
      "19970919T160000,19970929T160000,19971001T160000,19971003T160000," +
      "19971013T160000,19971015T160000,19971017T160000,19971027T170000," +
      "19971029T170000,19971031T170000,19971110T170000,19971112T170000," +
      "19971114T170000,19971124T170000,19971126T170000,19971128T170000," +
      "19971208T170000,19971210T170000,19971212T170000,19971222T170000",
      undefined,
      simplePacificTime);
}

function testEveryOtherWeekOnTuThFor8Occ() {
  runRecurrenceIteratorTest(
      "RRULE:FREQ=WEEKLY;INTERVAL=2;COUNT=8;WKST=SU;BYDAY=TU,TH",
      time.parseIcal("19970902"), 8,
      "19970902,19970904,19970916,19970918,19970930," +
      "19971002,19971014,19971016");
}

function testMonthlyOnThe1stFridayFor10Occ() {
  runRecurrenceIteratorTest(
      "RRULE:FREQ=MONTHLY;COUNT=10;BYDAY=1FR",
      time.parseIcal("19970905"), 10,
      "19970905,19971003,19971107,19971205,19980102," +
      "19980206,19980306,19980403,19980501,19980605");
}

function testMonthlyOnThe1stFridayUntilDec24() {
  runRecurrenceIteratorTest(
      "RRULE:FREQ=MONTHLY;UNTIL=19971224T000000Z;BYDAY=1FR",
      time.parseIcal("19970905"), 4,
      "19970905,19971003,19971107,19971205");
}

function testEveryOtherMonthOnThe1stAndLastSundayFor10Occ() {
  runRecurrenceIteratorTest(
      "RRULE:FREQ=MONTHLY;INTERVAL=2;COUNT=10;BYDAY=1SU,-1SU",
      time.parseIcal("19970907"), 10,
      "19970907,19970928,19971102,19971130,19980104," +
      "19980125,19980301,19980329,19980503,19980531");
}

function testMonthlyOnTheSecondToLastMondayOfTheMonthFor6Months() {
  runRecurrenceIteratorTest(
      "RRULE:FREQ=MONTHLY;COUNT=6;BYDAY=-2MO",
      time.parseIcal("19970922"), 6,
      "19970922,19971020,19971117,19971222,19980119," +
      "19980216");
}

function testMonthlyOnTheThirdToLastDay() {
  runRecurrenceIteratorTest(
      "RRULE:FREQ=MONTHLY;BYMONTHDAY=-3",
      time.parseIcal("19970928"), 6,
      "19970928,19971029,19971128,19971229,19980129,19980226,...");
}

function testMonthlyOnThe2ndAnd15thFor10Occ() {
  runRecurrenceIteratorTest(
      "RRULE:FREQ=MONTHLY;COUNT=10;BYMONTHDAY=2,15",
      time.parseIcal("19970902"), 10,
      "19970902,19970915,19971002,19971015,19971102," +
      "19971115,19971202,19971215,19980102,19980115");
}

function testMonthlyOnTheFirstAndLastFor10Occ() {
  runRecurrenceIteratorTest(
      "RRULE:FREQ=MONTHLY;COUNT=10;BYMONTHDAY=1,-1",
      time.parseIcal("19970930"), 10,
      "19970930,19971001,19971031,19971101,19971130," +
      "19971201,19971231,19980101,19980131,19980201");
}

function testEvery18MonthsOnThe10thThru15thFor10Occ() {
  runRecurrenceIteratorTest(
      "RRULE:FREQ=MONTHLY;INTERVAL=18;COUNT=10;BYMONTHDAY=10,11,12,13,14,\n" +
      " 15",
      time.parseIcal("19970910"), 10,
      "19970910,19970911,19970912,19970913,19970914," +
      "19970915,19990310,19990311,19990312,19990313");
}

function testEveryTuesdayEveryOtherMonth() {
  runRecurrenceIteratorTest(
      "RRULE:FREQ=MONTHLY;INTERVAL=2;BYDAY=TU",
      time.parseIcal("19970902"), 18,
      "19970902,19970909,19970916,19970923,19970930," +
      "19971104,19971111,19971118,19971125,19980106," +
      "19980113,19980120,19980127,19980303,19980310," +
      "19980317,19980324,19980331,...");
}

function testYearlyInJuneAndJulyFor10Occurrences() {
  // Note: Since none of the BYDAY, BYMONTHDAY or BYYEARDAY components
  // are specified, the day is gotten from DTSTART
  runRecurrenceIteratorTest(
      "RRULE:FREQ=YEARLY;COUNT=10;BYMONTH=6,7",
      time.parseIcal("19970610"), 10,
      "19970610,19970710,19980610,19980710,19990610," +
      "19990710,20000610,20000710,20010610,20010710");
}

function testEveryOtherYearOnJanuaryFebruaryAndMarchFor10Occurrences() {
  runRecurrenceIteratorTest(
      "RRULE:FREQ=YEARLY;INTERVAL=2;COUNT=10;BYMONTH=1,2,3",
      time.parseIcal("19970310"), 10,
      "19970310,19990110,19990210,19990310,20010110," +
      "20010210,20010310,20030110,20030210,20030310");
}

function testEvery3rdYearOnThe1st100thAnd200thDayFor10Occurrences() {
  runRecurrenceIteratorTest(
      "RRULE:FREQ=YEARLY;INTERVAL=3;COUNT=10;BYYEARDAY=1,100,200",
      time.parseIcal("19970101"), 10,
      "19970101,19970410,19970719,20000101,20000409," +
      "20000718,20030101,20030410,20030719,20060101");
}

function testEvery20thMondayOfTheYearForever() {
  runRecurrenceIteratorTest(
      "RRULE:FREQ=YEARLY;BYDAY=20MO",
      time.parseIcal("19970519"), 3,
      "19970519,19980518,19990517,...");
}

function testMondayOfWeekNumber20WhereTheDefaultStartOfTheWeekIsMonday() {
  runRecurrenceIteratorTest(
      "RRULE:FREQ=YEARLY;BYWEEKNO=20;BYDAY=MO",
      time.parseIcal("19970512"), 3,
      "19970512,19980511,19990517,...");
}

function testEveryThursdayInMarchForever() {
  runRecurrenceIteratorTest(
      "RRULE:FREQ=YEARLY;BYMONTH=3;BYDAY=TH",
      time.parseIcal("19970313"), 11,
      "19970313,19970320,19970327,19980305,19980312," +
      "19980319,19980326,19990304,19990311,19990318," +
      "19990325,...");
}

function testEveryThursdayButOnlyDuringJuneJulyAndAugustForever() {
  runRecurrenceIteratorTest(
      "RRULE:FREQ=YEARLY;BYDAY=TH;BYMONTH=6,7,8",
      time.parseIcal("19970605"), 39,
      "19970605,19970612,19970619,19970626,19970703," +
      "19970710,19970717,19970724,19970731,19970807," +
      "19970814,19970821,19970828,19980604,19980611," +
      "19980618,19980625,19980702,19980709,19980716," +
      "19980723,19980730,19980806,19980813,19980820," +
      "19980827,19990603,19990610,19990617,19990624," +
      "19990701,19990708,19990715,19990722,19990729," +
      "19990805,19990812,19990819,19990826,...");
}

function testEveryFridayThe13thForever() {
  runRecurrenceIteratorTest(
      "RRULE:FREQ=MONTHLY;BYDAY=FR;BYMONTHDAY=13",
      time.parseIcal("19970902"), 5,
      "19980213,19980313,19981113,19990813,20001013," +
      "...");
}

function testTheFirstSaturdayThatFollowsTheFirstSundayOfTheMonthForever() {
  runRecurrenceIteratorTest(
      "RRULE:FREQ=MONTHLY;BYDAY=SA;BYMONTHDAY=7,8,9,10,11,12,13",
      time.parseIcal("19970913"), 10,
      "19970913,19971011,19971108,19971213,19980110," +
      "19980207,19980307,19980411,19980509,19980613," +
      "...");
}

function testEvery4YearsThe1stTuesAfterAMonInNovForever() {
  // US Presidential Election Day
  runRecurrenceIteratorTest(
      "RRULE:FREQ=YEARLY;INTERVAL=4;BYMONTH=11;BYDAY=TU;BYMONTHDAY=2,3,4,\n" +
      " 5,6,7,8",
      time.parseIcal("19961105"), 3,
      "19961105,20001107,20041102,...");
}

function testThe3rdInstanceIntoTheMonthOfOneOfTuesWedThursForNext3Months() {
  runRecurrenceIteratorTest(
      "RRULE:FREQ=MONTHLY;COUNT=3;BYDAY=TU,WE,TH;BYSETPOS=3",
      time.parseIcal("19970904"), 3,
      "19970904,19971007,19971106");
}

function testThe2ndToLastWeekdayOfTheMonth() {
  runRecurrenceIteratorTest(
      "RRULE:FREQ=MONTHLY;BYDAY=MO,TU,WE,TH,FR;BYSETPOS=-2",
      time.parseIcal("19970929"), 7,
      "19970929,19971030,19971127,19971230,19980129," +
      "19980226,19980330,...");
}

function testEvery3HoursFrom900AmTo500PmOnASpecificDay() {
  if (false) { // TODO(msamuel): implement hourly iteration
    runRecurrenceIteratorTest(
        "RRULE:FREQ=HOURLY;INTERVAL=3;UNTIL=19970903T010000Z",
        time.parseIcal("19970902"), 7,
        "00000902,19970909,19970900,19970912,19970900," +
        "19970915,19970900");
  }
}

function testEvery15MinutesFor6Occurrences() {
  if (false) { // TODO(msamuel): implement minutely iteration
    runRecurrenceIteratorTest(
        "RRULE:FREQ=MINUTELY;INTERVAL=15;COUNT=6",
        time.parseIcal("19970902"), 13,
        "00000902,19970909,19970900,19970909,19970915," +
        "19970909,19970930,19970909,19970945,19970910," +
        "19970900,19970910,19970915");
  }
}

function testEveryHourAndAHalfFor4Occurrences() {
  if (false) { // TODO(msamuel): implement minutely iteration
    runRecurrenceIteratorTest(
        "RRULE:FREQ=MINUTELY;INTERVAL=90;COUNT=4",
        time.parseIcal("19970902"), 9,
        "00000902,19970909,19970900,19970910,19970930," +
        "19970912,19970900,19970913,19970930");
  }
}

function testAnExampleWhereTheDaysGeneratedMakesADifferenceBecauseOfWkst() {
  runRecurrenceIteratorTest(
      "RRULE:FREQ=WEEKLY;INTERVAL=2;COUNT=4;BYDAY=TU,SU;WKST=MO",
      time.parseIcal("19970805"), 4,
      "19970805,19970810,19970819,19970824");
}

function testAnExampleWhereTheDaysGeneratedMakesADifferenceBecauseOfWkst2() {
  runRecurrenceIteratorTest(
      "RRULE:FREQ=WEEKLY;INTERVAL=2;COUNT=4;BYDAY=TU,SU;WKST=SU",
      time.parseIcal("19970805"), 8,
      "19970805,19970817,19970819,19970831");
}

function testWithByDayAndByMonthDayFilter() {
  runRecurrenceIteratorTest(
      "RRULE:FREQ=WEEKLY;COUNT=4;BYDAY=TU,SU;" +
      "BYMONTHDAY=13,14,15,16,17,18,19,20",
      time.parseIcal("19970805"), 8,
      "19970817,19970819,19970914,19970916");
}

function testAnnuallyInAugustOnTuesAndSunBetween13thAnd20th() {
  runRecurrenceIteratorTest(
      "RRULE:FREQ=YEARLY;COUNT=4;BYDAY=TU,SU;" +
      "BYMONTHDAY=13,14,15,16,17,18,19,20;BYMONTH=8",
      time.parseIcal("19970605"), 8,
      "19970817,19970819,19980816,19980818");
}

function testLastDayOfTheYearIsASundayOrTuesday() {
  runRecurrenceIteratorTest(
      "RRULE:FREQ=YEARLY;COUNT=4;BYDAY=TU,SU;BYYEARDAY=-1",
      time.parseIcal("19940605"), 8,
      "19951231,19961231,20001231,20021231");
}

function testLastWeekdayOfMonth() {
  runRecurrenceIteratorTest(
      "RRULE:FREQ=MONTHLY;BYSETPOS=-1;BYDAY=-1MO,-1TU,-1WE,-1TH,-1FR",
      time.parseIcal("19940605"), 8,
      "19940630,19940729,19940831,19940930,"
      + "19941031,19941130,19941230,19950131,...");
}

function testMonthsThatStartOrEndOnFriday() {
  runRecurrenceIteratorTest(
      "RRULE:FREQ=MONTHLY;BYMONTHDAY=1,-1;BYDAY=FR;COUNT=6",
      time.parseIcal("19940605"), 8,
      "19940701,19940930,19950331,19950630,19950901,19951201");
}

function testMonthsThatStartOrEndOnFridayOnEvenWeeks() {
  // figure out which of the answers from the above fall on even weeks
  var dtStart = time.parseIcal("19940603");
  var golden = [];
  var dates = [
    time.parseIcal("19940701"),
    time.parseIcal("19940930"),
    time.parseIcal("19950331"),
    time.parseIcal("19950630"),
    time.parseIcal("19950901"),
    time.parseIcal("19951201")];
  for (var i = 0; i < dates.length; ++i) {
    if (0 === time.daysBetween(dates[i], dtStart) % 14) {
      golden.push(time.toIcal(dates[i]));
    }
  }

  runRecurrenceIteratorTest(
      "RRULE:FREQ=WEEKLY;INTERVAL=2;BYMONTHDAY=1,-1;BYDAY=FR;COUNT=3",
      dtStart, 8, golden.join(','));
}

function testCenturiesThatAreNotLeapYears() {
  // I can't think of a good reason anyone would want to specify both a
  // month day and a year day, so here's a really contrived example
  runRecurrenceIteratorTest(
      "RRULE:FREQ=YEARLY;INTERVAL=100;BYYEARDAY=60;BYMONTHDAY=1",
      time.parseIcal("19000101"), 4,
      "19000301,21000301,22000301,23000301,...");
}

function testNextCalledWithoutHasNext() {
  var riter = rrule.createRecurrenceIterator(
      new StubContentLine("RRULE:FREQ=DAILY"),
      time.parseIcal("20000101"), timezone.utc);
  assertEquals(time.parseIcal("20000101"), riter.next());
  assertEquals(time.parseIcal("20000102"), riter.next());
  assertEquals(time.parseIcal("20000103"), riter.next());
}

function testNoInstancesGenerated() {
  var riter = rrule.createRecurrenceIterator(
      new StubContentLine("RRULE:FREQ=DAILY;UNTIL=19990101"),
      time.parseIcal("20000101"), timezone.utc);
  assertTrue(!riter.hasNext());

  assertNull(riter.next());
  assertNull(riter.next());
  assertNull(riter.next());
}

function testNoInstancesGenerated2() {
  var riter = rrule.createRecurrenceIterator(
      new StubContentLine("RRULE:FREQ=YEARLY;BYMONTH=2;BYMONTHDAY=30"),
      time.parseIcal("20000101"), timezone.utc);
  assertTrue(!riter.hasNext());
}

function testNoInstancesGenerated3() {
  var riter = rrule.createRecurrenceIterator(
      new StubContentLine("RRULE:FREQ=YEARLY;INTERVAL=4;BYYEARDAY=366"),
      time.parseIcal("20010101"), timezone.utc);
  assertTrue(!riter.hasNext());
}

function testLastWeekdayOfMarch() {
  runRecurrenceIteratorTest(
      "RRULE:FREQ=MONTHLY;BYMONTH=3;BYDAY=SA,SU;BYSETPOS=-1",
      time.parseIcal("20000101"), 4,
      "20000326,20010331,20020331,20030330,...");
}

function testFirstWeekdayOfMarch() {
  runRecurrenceIteratorTest(
      "RRULE:FREQ=MONTHLY;BYMONTH=3;BYDAY=SA,SU;BYSETPOS=1",
      time.parseIcal("20000101"), 4,
      "20000304,20010303,20020302,20030301,...");
}


//     January 1999
// Mo Tu We Th Fr Sa Su
//              1  2  3    // < 4 days, so not a week
//  4  5  6  7  8  9 10

//     January 2000
// Mo Tu We Th Fr Sa Su
//                 1  2    // < 4 days, so not a week
//  3  4  5  6  7  8  9

//     January 2001
// Mo Tu We Th Fr Sa Su
//  1  2  3  4  5  6  7
//  8  9 10 11 12 13 14

//     January 2002
// Mo Tu We Th Fr Sa Su
//     1  2  3  4  5  6
//  7  8  9 10 11 12 13

/**
 * Find the first weekday of the first week of the year.
 * The first week of the year may be partial, and the first week is considered
 * to be the first one with at least four days.
 */
function testFirstWeekdayOfFirstWeekOfYear() {
  runRecurrenceIteratorTest(
      "RRULE:FREQ=YEARLY;BYWEEKNO=1;BYDAY=MO,TU,WE,TH,FR;BYSETPOS=1",
      time.parseIcal("19990101"), 4,
      "19990104,20000103,20010101,20020101,...");
}

function testFirstSundayOfTheYear1() {
  runRecurrenceIteratorTest(
      "RRULE:FREQ=YEARLY;BYWEEKNO=1;BYDAY=SU",
      time.parseIcal("19990101"), 4,
      "19990110,20000109,20010107,20020106,...");
}

function testFirstSundayOfTheYear2() {
  // TODO(msamuel): is this right?
  runRecurrenceIteratorTest(
      "RRULE:FREQ=YEARLY;BYDAY=1SU",
      time.parseIcal("19990101"), 4,
      "19990103,20000102,20010107,20020106,...");
}

function testFirstSundayOfTheYear3() {
  runRecurrenceIteratorTest(
      "RRULE:FREQ=YEARLY;BYDAY=SU;BYYEARDAY=1,2,3,4,5,6,7,8,9,10,11,12,13"
      + ";BYSETPOS=1",
      time.parseIcal("19990101"), 4,
      "19990103,20000102,20010107,20020106,...");
}

function testFirstWeekdayOfYear() {
  runRecurrenceIteratorTest(
      "RRULE:FREQ=YEARLY;BYDAY=MO,TU,WE,TH,FR;BYSETPOS=1",
      time.parseIcal("19990101"), 4,
      "19990101,20000103,20010101,20020101,...");
}

function testLastWeekdayOfFirstWeekOfYear() {
  runRecurrenceIteratorTest(
      "RRULE:FREQ=YEARLY;BYWEEKNO=1;BYDAY=MO,TU,WE,TH,FR;BYSETPOS=-1",
      time.parseIcal("19990101"), 4,
      "19990108,20000107,20010105,20020104,...");
}

//     January 1999
// Mo Tu We Th Fr Sa Su
//              1  2  3
//  4  5  6  7  8  9 10
// 11 12 13 14 15 16 17
// 18 19 20 21 22 23 24
// 25 26 27 28 29 30 31

function testSecondWeekday1() {
  runRecurrenceIteratorTest(
      "RRULE:FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR;BYSETPOS=2",
      time.parseIcal("19990101"), 4,
      "19990105,19990112,19990119,19990126,...");
}

//     January 1997
// Mo Tu We Th Fr Sa Su
//        1  2  3  4  5
//  6  7  8  9 10 11 12
// 13 14 15 16 17 18 19
// 20 21 22 23 24 25 26
// 27 28 29 30 31

function testSecondWeekday2() {
  runRecurrenceIteratorTest(
      "RRULE:FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR;BYSETPOS=2",
      time.parseIcal("19970101"), 4,
      "19970102,19970107,19970114,19970121,...");
}

function testByYearDayAndByDayFilterInteraction() {
  runRecurrenceIteratorTest(
      "RRULE:FREQ=YEARLY;BYYEARDAY=15;BYDAY=3MO",
      time.parseIcal("19990101"), 4,
      "20010115,20070115,20180115,20240115,...");
}

function testByDayWithNegWeekNoAsFilter() {
  runRecurrenceIteratorTest(
      "RRULE:FREQ=MONTHLY;BYMONTHDAY=26;BYDAY=-1FR",
      time.parseIcal("19990101"), 4,
      "19990226,19990326,19991126,20000526,...");
}

function testLastWeekOfTheYear() {
  runRecurrenceIteratorTest(
      "RRULE:FREQ=YEARLY;BYWEEKNO=-1",
      time.parseIcal("19990101"), 6,
      "19991227,19991228,19991229,19991230,19991231,20001225,...");
}

function testUserSubmittedTest1() {
  runRecurrenceIteratorTest(
      "RRULE:FREQ=WEEKLY;INTERVAL=2;WKST=WE;BYDAY=SU,TU,TH,SA"
      + ";UNTIL=20000215T113000Z",
      time.parseIcal("20000127T033000"), 20,
      "20000127T033000,20000129T033000,20000130T033000,20000201T033000,"
      + "20000210T033000,20000212T033000,20000213T033000,20000215T033000");
}

function testAdvanceTo() {
  // a bunch of tests grabbed from above with an advance-to date tacked on
  runRecurrenceIteratorTest(
      "RRULE:FREQ=YEARLY;BYMONTH=3;BYDAY=TH",
      time.parseIcal("19970313"), 11,
      /*"19970313,19970320,19970327,"*/"19980305,19980312," +
      "19980319,19980326,19990304,19990311,19990318," +
      "19990325,20000302,20000309,20000316,...",
      time.parseIcal("19970601"));

  runRecurrenceIteratorTest(
      "RRULE:FREQ=YEARLY;BYDAY=20MO",
      time.parseIcal("19970519"), 3,
      /*"19970519,"*/"19980518,19990517,20000515,...",
      time.parseIcal("19980515"));

  runRecurrenceIteratorTest(
      "RRULE:FREQ=YEARLY;INTERVAL=3;UNTIL=20090101;BYYEARDAY=1,100,200",
      time.parseIcal("19970101"), 10,
      /*"19970101,19970410,19970719,20000101,"*/"20000409," +
      "20000718,20030101,20030410,20030719,20060101,20060410,20060719," +
      "20090101",
      time.parseIcal("20000228"));

  // make sure that count preserved
  runRecurrenceIteratorTest(
      "RRULE:FREQ=YEARLY;INTERVAL=3;COUNT=10;BYYEARDAY=1,100,200",
      time.parseIcal("19970101"), 10,
      /*"19970101,19970410,19970719,20000101,"*/"20000409," +
      "20000718,20030101,20030410,20030719,20060101",
      time.parseIcal("20000228"));

  runRecurrenceIteratorTest(
      "RRULE:FREQ=YEARLY;INTERVAL=2;COUNT=10;BYMONTH=1,2,3",
      time.parseIcal("19970310"), 10,
      /*"19970310,"*/"19990110,19990210,19990310,20010110," +
      "20010210,20010310,20030110,20030210,20030310",
      time.parseIcal("19980401"));

  runRecurrenceIteratorTest(
      "RRULE:FREQ=WEEKLY;UNTIL=19971224",
      time.parseIcal("19970902"), 25,
      /*"19970902,19970909,19970916,19970923,"*/"19970930," +
      "19971007,19971014,19971021,19971028,19971104," +
      "19971111,19971118,19971125,19971202,19971209," +
      "19971216,19971223",
      time.parseIcal("19970930"));

  runRecurrenceIteratorTest(
      "RRULE:FREQ=MONTHLY;INTERVAL=18;BYMONTHDAY=10,11,12,13,14,\n" +
      " 15",
      time.parseIcal("19970910"), 5,
      /*"19970910,19970911,19970912,19970913,19970914," +
        "19970915,"*/"19990310,19990311,19990312,19990313,19990314,...",
      time.parseIcal("19990101"));

  // advancing into the past
  runRecurrenceIteratorTest(
      "RRULE:FREQ=MONTHLY;INTERVAL=18;BYMONTHDAY=10,11,12,13,14,\n" +
      " 15",
      time.parseIcal("19970910"), 11,
      "19970910,19970911,19970912,19970913,19970914," +
      "19970915,19990310,19990311,19990312,19990313,19990314,...",
      time.parseIcal("19970901"));

  // skips first instance
  runRecurrenceIteratorTest(
      "RRULE:FREQ=YEARLY;INTERVAL=100;BYMONTH=2;BYMONTHDAY=29",
      time.parseIcal("19000101"), 4,
      // would return 2000
      "24000229,28000229,32000229,36000229,...",
      time.parseIcal("20040101"));

  // filter hits until date before first instnace
  runRecurrenceIteratorTest(
      "RRULE:FREQ=YEARLY;INTERVAL=100;BYMONTH=2;BYMONTHDAY=29;UNTIL=21000101",
      time.parseIcal("19000101"), 5,
      "",
      time.parseIcal("20040101"));

  // advancing something that returns no instances
  runRecurrenceIteratorTest(
      "RRULE:FREQ=YEARLY;BYMONTH=2;BYMONTHDAY=30",
      time.parseIcal("20000101"), 10,
      "",
      time.parseIcal("19970901"));

  // advancing something that returns no instances and has a BYSETPOS rule
  runRecurrenceIteratorTest(
      "RRULE:FREQ=YEARLY;BYMONTH=2;BYMONTHDAY=30,31;BYSETPOS=1",
      time.parseIcal("20000101"), 10,
      "",
      time.parseIcal("19970901"));

  // advancing way past year generator timeout
  runRecurrenceIteratorTest(
      "RRULE:FREQ=YEARLY;BYMONTH=2;BYMONTHDAY=28",
      time.parseIcal("20000101"), 10,
      "",
      time.parseIcal("25000101"));

  // TODO(msamuel): check advancement of more examples
}


/** a testcase that yielded dupes due to bysetPos evilness */
function testCaseThatYieldedDupes() {
  runRecurrenceIteratorTest(
      "RRULE:FREQ=WEEKLY;WKST=SU;INTERVAL=1;BYMONTH=9,1,12,8"
      + ";BYMONTHDAY=-9,-29,24;BYSETPOS=-1,-4,10,-6,-1,-10,-10,-9,-8",
      time.parseIcal("20060528"), 200,
      "20060924,20061203,20061224,20070902,20071223,20080803,20080824,"
      + "20090823,20100103,20100124,20110123,20120902,20121223,20130922,"
      + "20140803,20140824,20150823,20160103,20160124,20170924,20171203,"
      + "20171224,20180902,20181223,20190922,20200823,20210103,20210124,"
      + "20220123,20230924,20231203,20231224,20240922,20250803,20250824,"
      + "20260823,20270103,20270124,20280123,20280924,20281203,20281224,"
      + "20290902,20291223,20300922,20310803,20310824,20330123,20340924,"
      + "20341203,20341224,20350902,20351223,20360803,20360824,20370823,"
      + "20380103,20380124,20390123,20400902,20401223,20410922,20420803,"
      + "20420824,20430823,20440103,20440124,20450924,20451203,20451224,"
      + "20460902,20461223,20470922,20480823,20490103,20490124,20500123,"
      + "20510924,20511203,20511224,20520922,20530803,20530824,20540823,"
      + "20550103,20550124,20560123,20560924,20561203,20561224,20570902,"
      + "20571223,20580922,20590803,20590824,20610123,20620924,20621203,"
      + "20621224,20630902,20631223,20640803,20640824,20650823,20660103,"
      + "20660124,20670123,20680902,20681223,20690922,20700803,20700824,"
      + "20710823,20720103,20720124,20730924,20731203,20731224,20740902,"
      + "20741223,20750922,20760823,20770103,20770124,20780123,20790924,"
      + "20791203,20791224,20800922,20810803,20810824,20820823,20830103,"
      + "20830124,20840123,20840924,20841203,20841224,20850902,20851223,"
      + "20860922,20870803,20870824,20890123,20900924,20901203,20901224,"
      + "20910902,20911223,20920803,20920824,20930823,20940103,20940124,"
      + "20950123,20960902,20961223,20970922,20980803,20980824,20990823,"
      + "21000103,21000124,21010123,21020924,21021203,21021224,21030902,"
      + "21031223,21040803,21040824,21050823,21060103,21060124,21070123,"
      + "21080902,21081223,21090922,21100803,21100824,21110823,21120103,"
      + "21120124,21130924,21131203,21131224,21140902,21141223,21150922,"
      + "21160823,21170103,21170124,21180123,21190924,21191203,21191224,"
      + "21200922,21210803,21210824,21220823,...");
}
