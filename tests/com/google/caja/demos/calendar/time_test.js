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


jsunitRegister('testParseIcal', function testParseIcal() {
  assertEquals(
      '20061125', time.date(2006, 11, 25), time.parseIcal('20061125'));
  assertEquals(
      '19001125', time.date(1900, 11, 25), time.parseIcal('19001125'));
  assertEquals(
      '19000228', time.date(1900, 2, 28), time.parseIcal('19000228'));
  assertEquals(
      '20061125T110000', time.dateTime(2006, 11, 25, 11, 0, 0),
      time.parseIcal('20061125T110000'));
  assertEquals(
      '20061125T113000', time.dateTime(2006, 11, 25, 11, 30, 0),
      time.parseIcal('20061125T113000'));
  assertEquals(
      '20061125T000000', time.dateTime(2006, 11, 25, 0, 0, 0),
      time.parseIcal('20061125T000000'));
  // 2400 -> next day
  assertEquals(
      '20061125T240000', time.dateTime(2006, 11, 26, 0, 0, 0),
      time.parseIcal('20061125T240000'));

  // test bad strings
  var badStrings = ['20060101T', 'foo', '', '123', '1234',
                    'D123456', 'P1D', '20060102/20060103',
                    null, undefined, '20060101T12',
                    '20060101TT120', '2006Ja01' ];
  for (var i = 0; i < badStrings.length; ++i) {
    try {
      time.parseIcal(badStrings[i]);
      fail('time.parseIcal did not fail on bad ical ' + badStrings[i]);
    } catch (e) {
      // pass
    }
  }
});

jsunitRegister('testDate', function testDate() {
  assertEquals(time.date(2006, 1, 1), time.date(2006, 1, 1));
  assertLessThan(time.date(2006, 1, 1), time.date(2006, 1, 2));
  assertLessThan(time.date(2006, 1, 1), time.date(2006, 2, 1));
  assertLessThan(time.date(2006, 1, 3), time.date(2006, 2, 1));
  assertLessThan(time.date(2005, 12, 31), time.date(2006, 1, 1));
  assertEquals(time.date(1, 1, 1), time.date(1, 1, 1));
  assertLessThan(time.date(1, 1, 1), time.date(1, 1, 2));
  assertLessThan(time.date(1, 1, 1), time.date(2006, 1, 1));
  assertLessThan(time.date(1, 1, 1), time.date(1, 2, 1));
  assertLessThan(time.date(0, 12, 31), time.date(1, 1, 1));
  assertLessThan(time.MIN_DATE_VALUE, time.MAX_DATE_VALUE);
});

jsunitRegister('testDateTime', function testDateTime() {
  assertEquals(
      time.dateTime(2006, 1, 1, 0, 0), time.dateTime(2006, 1, 1, 0, 0));
  assertLessThan(
      time.dateTime(2006, 1, 1, 0, 0), time.dateTime(2006, 1, 1, 1, 0));
  assertLessThan(
      time.dateTime(2006, 1, 1, 0, 0), time.dateTime(2006, 1, 1, 0, 1));
  assertLessThan(
      time.dateTime(2006, 1, 1, 12, 59), time.dateTime(2006, 1, 2, 0, 0));
  assertLessThan(
      time.dateTime(2006, 1, 1, 0, 0), time.dateTime(2006, 1, 2, 0, 0));
  assertLessThan(
      time.dateTime(2006, 1, 1, 0, 0), time.dateTime(2006, 2, 1, 0, 0));
  assertLessThan(
      time.dateTime(2006, 1, 3, 0, 0), time.dateTime(2006, 2, 1, 0, 0));
  assertLessThan(
      time.dateTime(2005, 12, 31, 0, 0), time.dateTime(2006, 1, 1, 0, 0));
  assertEquals(
      time.dateTime(1, 1, 1, 0, 0), time.dateTime(1, 1, 1, 0, 0));
  assertLessThan(
      time.dateTime(1, 1, 1, 0, 0), time.dateTime(1, 1, 2, 0, 0));
  assertLessThan(
      time.dateTime(1, 1, 1, 0, 0), time.dateTime(2006, 1, 1, 0, 0));
  assertLessThan(
      time.dateTime(1, 1, 1, 0, 0), time.dateTime(1, 2, 1, 0, 0));
  assertLessThan(
      time.dateTime(0, 12, 31, 0, 0), time.dateTime(1, 1, 1, 0, 0));

  // comparing date and date times
  assertLessThan(time.date(2006, 1, 1), time.dateTime(2006, 1, 1, 0, 0));
  assertLessThan(time.dateTime(2006, 1, 1, 12, 59), time.date(2006, 1, 2));
});

jsunitRegister('testNormalizedDate', function testNormalizedDate() {
  assertEquals('20060301', time.toIcal(time.normalizedDate(2006,  2, 29)));
  assertEquals('20071001', time.toIcal(time.normalizedDate(2006, 22, 1)));
  assertEquals('20050801', time.toIcal(time.normalizedDate(2006, -4, 1)));
  assertEquals('20060520', time.toIcal(time.normalizedDate(2006,  4, 50)));
  assertEquals('20060331', time.toIcal(time.normalizedDate(2006,  4, 0)));
  assertEquals('20060321', time.toIcal(time.normalizedDate(2006,  4, -10)));

  assertEquals('20041115', time.toIcal(time.normalizedDate(2006, -13, 15)));
  assertEquals('20041215', time.toIcal(time.normalizedDate(2006, -12, 15)));
  assertEquals('20050115', time.toIcal(time.normalizedDate(2006, -11, 15)));
  assertEquals('20051215', time.toIcal(time.normalizedDate(2006,   0, 15)));
  assertEquals('20061115', time.toIcal(time.normalizedDate(2006,  11, 15)));
  assertEquals('20061215', time.toIcal(time.normalizedDate(2006,  12, 15)));
  assertEquals('20070115', time.toIcal(time.normalizedDate(2006,  13, 15)));
  assertEquals('20081115', time.toIcal(time.normalizedDate(2006,  35, 15)));
  assertEquals('20081215', time.toIcal(time.normalizedDate(2006,  36, 15)));
  assertEquals('20090115', time.toIcal(time.normalizedDate(2006,  37, 15)));

  assertEquals('20330831', time.toIcal(time.normalizedDate(2006, 4, 10015)));
  assertEquals('19781128', time.toIcal(time.normalizedDate(2006, 4, -9985)));
});

jsunitRegister('testNormalizedDateTime', function testNormalizedDateTime() {
  assertEquals('20060301T120000',
               time.toIcal(time.normalizedDateTime(2006,  2, 29, 12, 0)));
  assertEquals('20060301T000000',
               time.toIcal(time.normalizedDateTime(2006,  2, 28, 24, 0)));
  assertEquals('20060302T020000',
               time.toIcal(time.normalizedDateTime(2006,  2, 28, 50, 0)));
  assertEquals('20060302T033000',
               time.toIcal(time.normalizedDateTime(2006,  2, 28, 50, 90)));
  assertEquals('20060227T233000',
               time.toIcal(time.normalizedDateTime(2006,  2, 28, -1, 30)));
  assertEquals('20060228T233000',
               time.toIcal(time.normalizedDateTime(2006,  3, 1, -1, 30)));
  assertEquals('20051231T233000',
               time.toIcal(time.normalizedDateTime(2006,  1, 1, -1, 30)));
});

jsunitRegister('testYear', function testYear() {
  assertEquals(2006, time.year(time.date(2006, 1, 1)));
  assertEquals(2006, time.year(time.dateTime(2006, 1, 1, 12, 0)));
  assertEquals(1900, time.year(time.date(1900, 1, 1)));
  assertEquals(4000, time.year(time.date(4000, 1, 1)));
  assertEquals(50, time.year(time.date(50, 1, 1)));
});

jsunitRegister('testMonth', function testMonth() {
  assertEquals(1, time.month(time.date(2006, 1, 1)));
  assertEquals(2, time.month(time.date(2006, 2, 1)));
  assertEquals(3, time.month(time.date(2006, 3, 1)));
  assertEquals(4, time.month(time.date(2006, 4, 1)));
  assertEquals(5, time.month(time.date(2006, 5, 1)));
  assertEquals(6, time.month(time.date(2006, 6, 1)));
  assertEquals(7, time.month(time.date(2006, 7, 1)));
  assertEquals(8, time.month(time.date(2006, 8, 1)));
  assertEquals(9, time.month(time.date(2006, 9, 1)));
  assertEquals(10, time.month(time.date(2006, 10, 1)));
  assertEquals(11, time.month(time.date(2006, 11, 1)));
  assertEquals(12, time.month(time.date(2006, 12, 1)));
  assertEquals(6, time.month(time.dateTime(2006, 6, 1, 12, 59)));
});

jsunitRegister('testDay', function testDay() {
  assertEquals(31, time.day(time.date(2006, 1, 31)));
  assertEquals(27, time.day(time.dateTime(2006, 1, 27, 12, 0)));
  assertEquals(12, time.day(time.date(2006, 1, 12)));
  assertEquals(14, time.day(time.date(3000, 9, 14)));
  assertEquals(15, time.day(time.date(-47, 3, 15)));
});

jsunitRegister('testHourMinuteMinuteInDay', function testHourMinuteMinuteInDay() {
  // combined tests for 3 accessors into one method to get around
  // 32K bytecode limit for classes
  assertEquals(0, time.hour(time.dateTime(2006, 1, 31, 0, 0)));
  assertEquals(4, time.hour(time.dateTime(2006, 1, 27, 4, 15)));
  assertEquals(12, time.hour(time.dateTime(2006, 1, 12, 12, 45)));
  assertEquals(18, time.hour(time.dateTime(3000, 9, 14, 18, 30)));
  assertEquals(23, time.hour(time.dateTime(-47, 3, 15, 23, 59)));

  assertEquals(0, time.minute(time.dateTime(2006, 1, 31, 0, 0)));
  assertEquals(15, time.minute(time.dateTime(2006, 1, 27, 4, 15)));
  assertEquals(45, time.minute(time.dateTime(2006, 1, 12, 12, 45)));
  assertEquals(30, time.minute(time.dateTime(3000, 9, 14, 18, 30)));
  assertEquals(59, time.minute(time.dateTime(-47, 3, 15, 23, 59)));

  assertEquals(0, time.minuteInDay(time.dateTime(2006, 1, 31, 0, 0)));
  assertEquals(255, time.minuteInDay(time.dateTime(2006, 1, 27, 4, 15)));
  assertEquals(765, time.minuteInDay(time.dateTime(2006, 1, 12, 12, 45)));
  assertEquals(1110, time.minuteInDay(time.dateTime(3000, 9, 14, 18, 30)));
  assertEquals(1439, time.minuteInDay(time.dateTime(-47, 3, 15, 23, 59)));
});

jsunitRegister('testHasTime', function testHasTime() {
  assertTrue(time.isDate(time.date(2006, 1, 1)));
  assertTrue(time.isDate(time.date(2006, 12, 31)));
  assertTrue(time.isDate(time.date(2006, 2, 28)));
  assertTrue(time.isDate(time.date(2006, 2, 29)));
  assertFalse(time.isDate(time.dateTime(2006, 1, 1, 0, 0)));
  assertFalse(time.isDate(time.dateTime(2006, 1, 1, 4, 0)));
  assertFalse(time.isDate(time.dateTime(2006, 12, 31, 0, 0)));
  assertFalse(time.isDate(time.dateTime(2006, 2, 28, 12, 59)));
  assertFalse(time.isDate(time.dateTime(2006, 2, 28, 0, 0)));
  assertFalse(time.isDate(time.dateTime(2006, 2, 29, 6, 30)));
});

jsunitRegister('testPlusDays', function testPlusDays() {
  assertEquals(
      '20080101', time.toIcal(time.plusDays(time.date(2008, 1, 1), 0)));
  assertEquals(
      '20071231', time.toIcal(time.plusDays(time.date(2008, 1, 1), -1)));
  assertEquals(
      '20071201', time.toIcal(time.plusDays(time.date(2008, 1, 1), -31)));
  assertEquals(
      '20080102', time.toIcal(time.plusDays(time.date(2008, 1, 1), 1)));
  assertEquals(
      '20080131', time.toIcal(time.plusDays(time.date(2008, 1, 1), 30)));
  assertEquals(
      '20080201', time.toIcal(time.plusDays(time.date(2008, 1, 1), 31)));
  assertEquals(
      '20080302', time.toIcal(time.plusDays(time.date(2008, 2, 1), 30)));
  assertEquals(
      '20081231', time.toIcal(time.plusDays(time.date(2008, 1, 1), 365)));
  assertEquals(
      '20090101', time.toIcal(time.plusDays(time.date(2008, 1, 1), 366)));
  assertEquals(
      '20080101', time.toIcal(time.plusDays(time.date(2007, 1, 1), 365)));
  assertEquals(
      '20080102', time.toIcal(time.plusDays(time.date(2007, 1, 1), 366)));
  assertEquals(
      '20070101', time.toIcal(time.plusDays(time.date(2008, 1, 1), -365)));
  assertEquals(
      '20061231', time.toIcal(time.plusDays(time.date(2008, 1, 1), -366)));
  assertEquals(
      '20071201T123000',
      time.toIcal(time.plusDays(time.dateTime(2008, 1, 1, 12, 30), -31)));
  assertEquals(
      '20080102T040000',
      time.toIcal(time.plusDays(time.dateTime(2008, 1, 1, 4, 0), 1)));
  assertEquals(
      '20080131T151500',
      time.toIcal(time.plusDays(time.dateTime(2008, 1, 1, 15, 15), 30)));
});

jsunitRegister('testNextDate', function testNextDate() {
  assertEquals('20080101', time.toIcal(time.nextDate(time.date(2007, 12, 31))));
  assertEquals('20080102', time.toIcal(time.nextDate(time.date(2008, 1, 1))));
  assertEquals('20080103', time.toIcal(time.nextDate(time.date(2008, 1, 2))));
  assertEquals('20080104', time.toIcal(time.nextDate(time.date(2008, 1, 3))));
  assertEquals('20080131', time.toIcal(time.nextDate(time.date(2008, 1, 30))));
  assertEquals('20080201', time.toIcal(time.nextDate(time.date(2008, 1, 31))));
  assertEquals('20080202', time.toIcal(time.nextDate(time.date(2008, 2, 1))));
  assertEquals('20080228', time.toIcal(time.nextDate(time.date(2008, 2, 27))));
  assertEquals('20080229', time.toIcal(time.nextDate(time.date(2008, 2, 28))));
  assertEquals('20080301', time.toIcal(time.nextDate(time.date(2008, 2, 29))));
  assertEquals('20070228', time.toIcal(time.nextDate(time.date(2007, 2, 27))));
  assertEquals('20070301', time.toIcal(time.nextDate(time.date(2007, 2, 28))));
  assertEquals('20070302', time.toIcal(time.nextDate(time.date(2007, 3, 1))));

  assertEquals(
      '20080101T123000',
      time.toIcal(time.nextDate(time.dateTime(2007, 12, 31, 12, 30))));
  assertEquals(
      '20080102T043000',
      time.toIcal(time.nextDate(time.dateTime(2008, 1, 1, 4, 30))));
  assertEquals(
      '20080131T164500',
      time.toIcal(time.nextDate(time.dateTime(2008, 1, 30, 16, 45))));
  assertEquals(
      '20080201T123000',
      time.toIcal(time.nextDate(time.dateTime(2008, 1, 31, 12, 30))));
});

jsunitRegister('testDaysBetween', function testDaysBetween() {
  assertEquals(   0,
      time.daysBetween(time.date(2003, 12, 31), time.date(2003, 12, 31)));
  assertEquals( -60,
      time.daysBetween(time.date(2003, 12, 31), time.date(2004, 2, 29)));
  assertEquals( -66,
      time.daysBetween(time.date(2003, 12, 31), time.date(2004, 3, 6)));
  assertEquals( -69,
      time.daysBetween(time.date(2003, 12, 31), time.date(2004, 3, 9)));
  assertEquals(-305,
      time.daysBetween(time.date(2003, 12, 31), time.date(2004, 10, 31)));
  assertEquals(-306,
      time.daysBetween(time.date(2003, 12, 31), time.date(2004, 11, 1)));

  assertEquals(  60,
      time.daysBetween(time.date(2004, 2, 29), time.date(2003, 12, 31)));
  assertEquals(   0,
      time.daysBetween(time.date(2004, 2, 29), time.date(2004, 2, 29)));
  assertEquals(  -6,
      time.daysBetween(time.date(2004, 2, 29), time.date(2004, 3, 6)));
  assertEquals(  -9,
      time.daysBetween(time.date(2004, 2, 29), time.date(2004, 3, 9)));
  assertEquals(-245,
      time.daysBetween(time.date(2004, 2, 29), time.date(2004, 10, 31)));
  assertEquals(-246,
      time.daysBetween(time.date(2004, 2, 29), time.date(2004, 11, 1)));

  assertEquals(  66,
      time.daysBetween(time.date(2004, 3, 6), time.date(2003, 12, 31)));
  assertEquals(   6,
      time.daysBetween(time.date(2004, 3, 6), time.date(2004, 2, 29)));
  assertEquals(   0,
      time.daysBetween(time.date(2004, 3, 6), time.date(2004, 3, 6)));
  assertEquals(  -3,
      time.daysBetween(time.date(2004, 3, 6), time.date(2004, 3, 9)));
  assertEquals(-239,
      time.daysBetween(time.date(2004, 3, 6), time.date(2004, 10, 31)));
  assertEquals(-240,
      time.daysBetween(time.date(2004, 3, 6), time.date(2004, 11, 1)));

  assertEquals(  69,
      time.daysBetween(time.date(2004, 3, 9), time.date(2003, 12, 31)));
  assertEquals(   9,
      time.daysBetween(time.date(2004, 3, 9), time.date(2004, 2, 29)));
  assertEquals(   3,
      time.daysBetween(time.date(2004, 3, 9), time.date(2004, 3, 6)));
  assertEquals(   0,
      time.daysBetween(time.date(2004, 3, 9), time.date(2004, 3, 9)));
  assertEquals(-236,
      time.daysBetween(time.date(2004, 3, 9), time.date(2004, 10, 31)));
  assertEquals(-237,
      time.daysBetween(time.date(2004, 3, 9), time.date(2004, 11, 1)));

  assertEquals( 305,
      time.daysBetween(time.date(2004, 10, 31), time.date(2003, 12, 31)));
  assertEquals( 245,
      time.daysBetween(time.date(2004, 10, 31), time.date(2004, 2, 29)));
  assertEquals( 239,
      time.daysBetween(time.date(2004, 10, 31), time.date(2004, 3, 6)));
  assertEquals( 236,
      time.daysBetween(time.date(2004, 10, 31), time.date(2004, 3, 9)));
  assertEquals(   0,
      time.daysBetween(time.date(2004, 10, 31), time.date(2004, 10, 31)));
  assertEquals(  -1,
      time.daysBetween(time.date(2004, 10, 31), time.date(2004, 11, 1)));

  assertEquals( 306,
      time.daysBetween(time.date(2004, 11, 1), time.date(2003, 12, 31)));
  assertEquals( 246,
      time.daysBetween(time.date(2004, 11, 1), time.date(2004, 2, 29)));
  assertEquals( 240,
      time.daysBetween(time.date(2004, 11, 1), time.date(2004, 3, 6)));
  assertEquals( 237,
      time.daysBetween(time.date(2004, 11, 1), time.date(2004, 3, 9)));
  assertEquals(   1,
      time.daysBetween(time.date(2004, 11, 1), time.date(2004, 10, 31)));
  assertEquals(   0,
      time.daysBetween(time.date(2004, 11, 1), time.date(2004, 11, 1)));

  assertEquals(-365,
      time.daysBetween(time.date(2003, 1, 1), time.date(2004, 1, 1)));
  assertEquals(-366,
      time.daysBetween(time.date(2004, 1, 1), time.date(2005, 1, 1)));
  assertEquals(-365,
      time.daysBetween(time.date(2005, 1, 1), time.date(2006, 1, 1)));
});

jsunitRegister('testDayOfYear', function testDayOfYear() {
  assertEquals(0, time.dayOfYear(time.date(2005, 1, 1)));
  assertEquals(31, time.dayOfYear(time.date(2006, 2, 1)));
  assertEquals(31 + 28, time.dayOfYear(time.date(2007, 3, 1)));
  assertEquals(31 + 28 + 31, time.dayOfYear(time.date(2009, 4, 1)));
  assertEquals(31 + 28 + 31 + 30, time.dayOfYear(time.date(2010, 5, 1)));
  assertEquals(31 + 28 + 31 + 30,
      time.dayOfYear(time.date(2011, 5, 1)));
  assertEquals(31 + 28 + 31 + 30 + 31,
      time.dayOfYear(time.date(2013, 6, 1)));
  assertEquals(31 + 28 + 31 + 30 + 31 + 30,
      time.dayOfYear(time.date(2014, 7, 1)));
  assertEquals(31 + 28 + 31 + 30 + 31 + 30 + 31,
      time.dayOfYear(time.date(2015, 8, 1)));
  assertEquals(31 + 28 + 31 + 30 + 31 + 30 + 31 + 31,
      time.dayOfYear(time.date(2017, 9, 1)));
  assertEquals(31 + 28 + 31 + 30 + 31 + 30 + 31 + 31 + 30,
      time.dayOfYear(time.date(2018, 10, 1)));
  assertEquals(31 + 28 + 31 + 30 + 31 + 30 + 31 + 31 + 30 + 31 + 20,
      time.dayOfYear(time.date(2019, 11, 21)));
  assertEquals(31 + 28 + 31 + 30 + 31 + 30 + 31 + 31 + 30 + 31 + 30,
      time.dayOfYear(time.date(2021, 12, 1)));
  assertEquals(364, time.dayOfYear(time.date(2022, 12, 31)));

  assertEquals(0, time.dayOfYear(time.date(2004, 1, 1)));
  assertEquals(20, time.dayOfYear(time.date(2004, 1, 21)));
  assertEquals(31,
      time.dayOfYear(time.date(2004, 2, 1)));
  assertEquals(31 + 29,
      time.dayOfYear(time.date(2004, 3, 1)));
  assertEquals(31 + 29 + 31,
      time.dayOfYear(time.date(2008, 4, 1)));
  assertEquals(31 + 29 + 31 + 30,
      time.dayOfYear(time.date(2012, 5, 1)));
  assertEquals(31 + 29 + 31 + 30 + 31 + 7,
      time.dayOfYear(time.date(2020, 6, 8)));
  assertEquals(31 + 29 + 31 + 30 + 31 + 30,
      time.dayOfYear(time.date(2024, 7, 1)));
  assertEquals(31 + 29 + 31 + 30 + 31 + 30 + 31,
      time.dayOfYear(time.date(2028, 8, 1)));
  assertEquals(31 + 29 + 31 + 30 + 31 + 30 + 31 + 31,
      time.dayOfYear(time.date(2032, 9, 1)));
  assertEquals(31 + 29 + 31 + 30 + 31 + 30 + 31 + 31 + 30,
      time.dayOfYear(time.date(2036, 10, 1)));
  assertEquals(31 + 29 + 31 + 30 + 31 + 30 + 31 + 31 + 30 + 31,
      time.dayOfYear(time.date(2040, 11, 1)));
  assertEquals(31 + 29 + 31 + 30 + 31 + 30 + 31 + 31 + 30 + 31 + 30,
      time.dayOfYear(time.date(2044, 12, 1)));
  assertEquals(365, time.dayOfYear(time.date(2048, 12, 31)));
});

jsunitRegister('testToDateOnOrAfter', function testToDateOnOrAfter() {
  assertEquals(
      '20080101',
      time.toIcal(time.toDateOnOrAfter(time.parseIcal('20080101'))));
  assertEquals(
      '20080102',
      time.toIcal(time.toDateOnOrAfter(time.parseIcal('20080102'))));
  assertEquals(
      '20080615',
      time.toIcal(time.toDateOnOrAfter(time.parseIcal('20080615'))));

  assertEquals(
      '20080101',
      time.toIcal(time.toDateOnOrAfter(time.parseIcal('20080101T000000'))));
  assertEquals(
      '20080102',
      time.toIcal(time.toDateOnOrAfter(time.parseIcal('20080102T000000'))));
  assertEquals(
      '20080615',
      time.toIcal(time.toDateOnOrAfter(time.parseIcal('20080615T000000'))));

  assertEquals(
      '20080102',
      time.toIcal(time.toDateOnOrAfter(time.parseIcal('20080101T000100'))));
  assertEquals(
      '20080103',
      time.toIcal(time.toDateOnOrAfter(time.parseIcal('20080102T120000'))));
  assertEquals(
      '20080616',
      time.toIcal(time.toDateOnOrAfter(time.parseIcal('20080615T235900'))));
});

jsunitRegister('testWithYear', function testWithYear() {
  assertEquals(
      '20070101',
      time.toIcal(time.withYear(time.date(2007, 1, 1), 2007)));
  assertEquals(
      '20080301',
      time.toIcal(time.withYear(time.date(0, 3, 1), 2008)));
  assertEquals(
      '00070103',
      time.toIcal(time.withYear(time.date(2107, 1, 3), 7)));
  assertEquals(
      '20070615',
      time.toIcal(time.withYear(time.date(2007, 6, 15), 2007)));
  assertEquals(
      '20081114',
      time.toIcal(time.withYear(time.date(3007, 11, 14), 2008)));
  assertEquals(
      '30000401T124500',
      time.toIcal(time.withYear(time.dateTime(2007, 4, 1, 12, 45), 3000)));
});

jsunitRegister('testWithMonth', function testWithMonth() {
  assertEquals(
      '20070101',
      time.toIcal(time.withMonth(time.date(2007, 1, 1), 1)));
  assertEquals(
      '00000401',
      time.toIcal(time.withMonth(time.date(0, 3, 1), 4)));
  assertEquals(
      '21071203',
      time.toIcal(time.withMonth(time.date(2107, 1, 3), 12)));
  assertEquals(
      '20070715',
      time.toIcal(time.withMonth(time.date(2007, 6, 15), 7)));
  assertEquals(
      '30070114',
      time.toIcal(time.withMonth(time.date(3007, 11, 14), 1)));
  assertEquals(
      '20071201T124500',
      time.toIcal(time.withMonth(time.dateTime(2007, 4, 1, 12, 45), 12)));
});

jsunitRegister('testWithDay', function testWithDay() {
  assertEquals(
      '20070101',
      time.toIcal(time.withDay(time.date(2007, 1, 1), 1)));
  assertEquals(
      '00000304',
      time.toIcal(time.withDay(time.date(0, 3, 1), 4)));
  assertEquals(
      '21070131',
      time.toIcal(time.withDay(time.date(2107, 1, 3), 31)));
  assertEquals(
      '20070607',
      time.toIcal(time.withDay(time.date(2007, 6, 15), 7)));
  assertEquals(
      '30071101',
      time.toIcal(time.withDay(time.date(3007, 11, 14), 1)));
  assertEquals(
      '20070412T124500',
      time.toIcal(time.withDay(time.dateTime(2007, 4, 1, 12, 45), 12)));
});

jsunitRegister('testWithHour', function testWithHour() {
  assertEquals(
      '20070401T124500',
      time.toIcal(time.withHour(time.dateTime(2007, 4, 1, 12, 45), 12)));
  assertEquals(
      '20070606T034500',
      time.toIcal(time.withHour(time.dateTime(2007, 6, 6, 12, 45), 3)));
  assertEquals(
      '20071231T234500',
      time.toIcal(time.withHour(time.dateTime(2007, 12, 31, 12, 45), 23)));
  assertEquals(
      '20071231', time.toIcal(time.withHour(time.date(2007, 12, 31), 23)));
});

jsunitRegister('testWithMinute', function testWithMinute() {
  assertEquals(
      '20070401T124500',
      time.toIcal(time.withMinute(time.dateTime(2007, 4, 1, 12, 45), 45)));
  assertEquals(
      '20070606T121500',
      time.toIcal(time.withMinute(time.dateTime(2007, 6, 6, 12, 45), 15)));
  assertEquals(
      '20071231T125900',
      time.toIcal(time.withMinute(time.dateTime(2007, 12, 31, 12, 45), 59)));
  assertEquals(
      '20071231', time.toIcal(time.withMinute(time.date(2007, 12, 31), 23)));
});

jsunitRegister('testWithTime', function testWithTime() {
  assertEquals(
      '20070401T124500',
      time.toIcal(time.withTime(time.dateTime(2007, 4, 1, 12, 45), 12, 45)));
  assertEquals(
      '20070606T030100',
      time.toIcal(time.withTime(time.dateTime(2007, 6, 6, 12, 45), 3, 1)));
  assertEquals(
      '20071231T235900',
      time.toIcal(time.withTime(time.dateTime(2007, 12, 31, 12, 45), 23, 59)));
  assertEquals(
      '20071231T061500',
      time.toIcal(time.withTime(time.date(2007, 12, 31), 6, 15)));

});
