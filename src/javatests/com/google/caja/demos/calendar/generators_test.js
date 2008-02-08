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

function testSerialYearGenerator1() {
  var dtStart = time.date(2008, 6, 15);
  var g = generators.serialYearGenerator(1, dtStart);
  g.reset();

  var builder = [time.date(2008, 6, 16)];
  assertTrue(g.generate(builder));
  assertEquals('20080616', time.toIcal(builder[0]));
  assertTrue(g.generate(builder));
  assertEquals('20090616', time.toIcal(builder[0]));
  assertTrue(g.generate(builder));
  assertEquals('20100616', time.toIcal(builder[0]));
  assertTrue(g.generate(builder));
  assertEquals('20110616', time.toIcal(builder[0]));
  assertTrue(g.generate(builder));
}

function testSerialYearGenerator2() {
  var dtStart = time.date(2008, 6, 15);
  var g = generators.serialYearGenerator(4, dtStart);
  g.reset();

  var builder = [time.date(2008, 6, 16)];
  assertTrue(g.generate(builder));
  assertEquals('20080616', time.toIcal(builder[0]));
  assertTrue(g.generate(builder));
  assertEquals('20120616', time.toIcal(builder[0]));
  assertTrue(g.generate(builder));
  assertEquals('20160616', time.toIcal(builder[0]));
  assertTrue(g.generate(builder));
  assertEquals('20200616', time.toIcal(builder[0]));
  assertTrue(g.generate(builder));
}

function testSerialYearGeneratorOverflow() {
  var dtStart = time.date(2008, 6, 15);
  var g = generators.serialYearGenerator(500, dtStart);
  g.reset();

  var builder = [time.date(2008, 6, 16)];
  assertTrue(g.generate(builder));
  assertEquals('20080616', time.toIcal(builder[0]));
  assertTrue(g.generate(builder));
  assertEquals('25080616', time.toIcal(builder[0]));
  assertTrue(g.generate(builder));
  assertEquals('30080616', time.toIcal(builder[0]));
  assertTrue(g.generate(builder));
  assertEquals('35080616', time.toIcal(builder[0]));
  assertTrue(g.generate(builder));
  assertEquals('40080616', time.toIcal(builder[0]));
  assertFalse(g.generate(builder));
}

function testYearGeneratorThrottled() {
  var dtStart = time.date(2008, 6, 15);
  var g = generators.serialYearGenerator(2, dtStart);
  g.reset();

  var builder = [time.date(2008, 6, 16)];
  for (var i = 99; --i >= 0;) {
    assertTrue(g.generate(builder));
  }
  assertEquals('22040616', time.toIcal(builder[0]));
  try {
    g.generate(builder);
    throw new Error('year generator not throttled');
  } catch (ex) {
    // pass
  }

  g.reset();
  builder = [time.date(2008, 6, 16)];
  for (var i = 50; --i >= 0;) {
    assertTrue(g.generate(builder));
  }
  assertEquals('21060616', time.toIcal(builder[0]));
  g.workDone();  // Reset throttle.

  for (var i = 50; --i >= 0;) {
    assertTrue(g.generate(builder));
  }
  assertEquals('22060616', time.toIcal(builder[0]));
}

function testSerialMonthGenerator1() {
  var dtStart = time.date(2008, 6, 15);
  var g = generators.serialMonthGenerator(1, dtStart);
  g.reset();

  var builder = [time.date(2008, 6, 16)];
  assertTrue(g.generate(builder));
  assertEquals('20080616', time.toIcal(builder[0]));
  assertTrue(g.generate(builder));
  assertEquals('20080716', time.toIcal(builder[0]));
  assertTrue(g.generate(builder));
  assertEquals('20080816', time.toIcal(builder[0]));
  assertTrue(g.generate(builder));
  assertEquals('20080916', time.toIcal(builder[0]));
  assertTrue(g.generate(builder));
  assertEquals('20081016', time.toIcal(builder[0]));
  assertTrue(g.generate(builder));
  assertEquals('20081116', time.toIcal(builder[0]));
  assertTrue(g.generate(builder));
  assertEquals('20081216', time.toIcal(builder[0]));
  assertFalse(g.generate(builder));

  builder[0] = time.date(2009, 6, 16);
  assertTrue(g.generate(builder));
  assertEquals('20090116', time.toIcal(builder[0]));
  assertTrue(g.generate(builder));
  assertEquals('20090216', time.toIcal(builder[0]));

  g.reset();
  builder[0] = time.date(2008, 1, 12);  // Start at dtStart month regardless
  assertTrue(g.generate(builder));
  assertEquals('20080612', time.toIcal(builder[0]));
}


function testSerialMonthGenerator2() {
  var dtStart = time.date(2008, 6, 15);
  // test an interval coprime with 12
  var g = generators.serialMonthGenerator(5, dtStart);
  g.reset();

  var builder = [time.date(2008, 6, 16)];
  assertTrue(g.generate(builder));
  assertEquals('20080616', time.toIcal(builder[0]));
  assertTrue(g.generate(builder));
  assertEquals('20081116', time.toIcal(builder[0]));
  assertFalse(g.generate(builder));

  builder[0] = time.date(2009, 11, 16);
  assertTrue(g.generate(builder));
  assertEquals('20090416', time.toIcal(builder[0]));
  assertTrue(g.generate(builder));
  assertEquals('20090916', time.toIcal(builder[0]));
  assertFalse(g.generate(builder));

  // What if the year generator decides to skip a few years?
  builder[0] = time.date(2011, 11, 16);
  assertTrue(g.generate(builder));
  assertEquals('20110516', time.toIcal(builder[0]));
}

function testSerialMonthGenerator3() {
  var dtStart = time.date(2008, 6, 15);
  // test an interval over a year
  var g = generators.serialMonthGenerator(17, dtStart);
  g.reset();

  var builder = [time.date(2008, 6, 16)];
  assertTrue(g.generate(builder));
  assertEquals('20080616', time.toIcal(builder[0]));
  assertFalse(g.generate(builder));

  builder[0] = time.date(2009, 6, 16);
  assertTrue(g.generate(builder));
  assertEquals('20091116', time.toIcal(builder[0]));
  assertFalse(g.generate(builder));

  builder[0] = time.date(2010, 11, 16);
  assertFalse(g.generate(builder));

  builder[0] = time.date(2011, 11, 16);
  assertTrue(g.generate(builder));
  assertEquals('20110416', time.toIcal(builder[0]));
  assertFalse(g.generate(builder));
}

function testByMonthGenerator() {
  var dtStart = time.date(2008, 6, 15);
  // test an interval over a year
  var g = generators.byMonthGenerator([3, 2, 7, 7], dtStart);
  g.reset();

  var builder = [time.date(2008, 6, 16)];
  assertTrue(g.generate(builder));
  assertEquals('20080216', time.toIcal(builder[0]));
  assertTrue(g.generate(builder));
  assertEquals('20080316', time.toIcal(builder[0]));
  assertTrue(g.generate(builder));
  assertEquals('20080716', time.toIcal(builder[0]));
  assertFalse(g.generate(builder));

  builder[0] = time.date(2009, 7, 16);
  assertTrue(g.generate(builder));
  assertEquals('20090216', time.toIcal(builder[0]));
  assertTrue(g.generate(builder));
  assertEquals('20090316', time.toIcal(builder[0]));
  assertTrue(g.generate(builder));
  assertEquals('20090716', time.toIcal(builder[0]));
  assertFalse(g.generate(builder));

  builder[0] = time.date(2010, 7, 16);
  assertTrue(g.generate(builder));
  assertEquals('20100216', time.toIcal(builder[0]));
  assertTrue(g.generate(builder));
  assertEquals('20100316', time.toIcal(builder[0]));
  assertTrue(g.generate(builder));
  assertEquals('20100716', time.toIcal(builder[0]));
  assertFalse(g.generate(builder));
}

function testSerialDayGenerator() {
  var dtStart = time.date(2008, 6, 17);
  var g = generators.serialDayGenerator(4, dtStart);
  g.reset();

  var builder = [time.date(2008, 6, 18)];
  assertTrue(g.generate(builder));
  assertEquals('20080617', time.toIcal(builder[0]));
  assertTrue(g.generate(builder));
  assertEquals('20080621', time.toIcal(builder[0]));
  assertTrue(g.generate(builder));
  assertEquals('20080625', time.toIcal(builder[0]));
  assertTrue(g.generate(builder));
  assertEquals('20080629', time.toIcal(builder[0]));
  assertFalse(g.generate(builder));

  builder[0] = time.date(2008, 7, 29);
  assertTrue(g.generate(builder));
  assertEquals('20080703', time.toIcal(builder[0]));
  assertTrue(g.generate(builder));
}

function testByMonthDayGenerator() {
  var dtStart = time.date(2008, 6, 17);
  var g = generators.byMonthDayGenerator([1, 15, -2, -1, 29, 15], dtStart);
  g.reset();

  var builder = [time.date(2008, 6, 18)];
  assertTrue(g.generate(builder));
  assertEquals('20080601', time.toIcal(builder[0]));
  assertTrue(g.generate(builder));
  assertEquals('20080615', time.toIcal(builder[0]));
  assertTrue(g.generate(builder));
  assertEquals('20080629', time.toIcal(builder[0]));
  assertTrue(g.generate(builder));
  assertEquals('20080630', time.toIcal(builder[0]));
  assertFalse(g.generate(builder));

  builder[0] = time.date(2008, 7, 18);
  assertTrue(g.generate(builder));
  assertEquals('20080701', time.toIcal(builder[0]));
  assertTrue(g.generate(builder));
  assertEquals('20080715', time.toIcal(builder[0]));
  assertTrue(g.generate(builder));
  assertEquals('20080729', time.toIcal(builder[0]));
  assertTrue(g.generate(builder));
  assertEquals('20080730', time.toIcal(builder[0]));
  assertTrue(g.generate(builder));
  assertEquals('20080731', time.toIcal(builder[0]));
  assertFalse(g.generate(builder));
}

function testByDayGeneratorInMonth() {
  // $ cal 6 2008
  //      June 2008
  // Su Mo Tu We Th Fr Sa
  //  1  2  3  4  5  6  7
  //  8  9 10 11 12 13 14
  // 15 16 17 18 19 20 21
  // 22 23 24 25 26 27 28
  // 29 30

  // $ cal 7 2008
  //      July 2008
  // Su Mo Tu We Th Fr Sa
  //        1  2  3  4  5
  //  6  7  8  9 10 11 12
  // 13 14 15 16 17 18 19
  // 20 21 22 23 24 25 26
  // 27 28 29 30 31

  var dtStart = time.date(2008, 6, 17);
  var g = generators.byDayGenerator(
      [{ wday: WeekDay.SU, num: 0 },  // Every Sunday
       { wday: WeekDay.TU, num: 2 },  // Every second Tuesday
       { wday: WeekDay.FR, num: -1 }  // The last Friday
      ], false, dtStart);
  g.reset();

  var builder = [time.date(2008, 6, 18)];
  assertTrue(g.generate(builder));
  assertEquals('20080601', time.toIcal(builder[0]));
  assertTrue(g.generate(builder));
  assertEquals('20080608', time.toIcal(builder[0]));
  assertTrue(g.generate(builder));
  assertEquals('20080610', time.toIcal(builder[0]));  // Second Tueday
  assertTrue(g.generate(builder));
  assertEquals('20080615', time.toIcal(builder[0]));
  assertTrue(g.generate(builder));
  assertEquals('20080622', time.toIcal(builder[0]));
  assertTrue(g.generate(builder));
  assertEquals('20080627', time.toIcal(builder[0]));  // Last Friday
  assertTrue(g.generate(builder));
  assertEquals('20080629', time.toIcal(builder[0]));
  assertFalse(g.generate(builder));

  builder[0] = time.date(2008, 7, 18);
  assertTrue(g.generate(builder));
  assertEquals('20080706', time.toIcal(builder[0]));
  assertTrue(g.generate(builder));
  assertEquals('20080708', time.toIcal(builder[0]));
  assertTrue(g.generate(builder));
  assertEquals('20080713', time.toIcal(builder[0]));
  assertTrue(g.generate(builder));
  assertEquals('20080720', time.toIcal(builder[0]));
  assertTrue(g.generate(builder));
  assertEquals('20080725', time.toIcal(builder[0]));  // Last Friday
  assertTrue(g.generate(builder));
  assertEquals('20080727', time.toIcal(builder[0]));
  assertFalse(g.generate(builder));
}

function testByWeekNoGenerator1() {
  // $ cal 6 2008
  //      June 2008
  // Su Mo Tu We Th Fr Sa
  //  1  2  3  4  5  6  7   # Week 23
  //  8  9 10 11 12 13 14   # Week 24
  // 15 16 17 18 19 20 21   # Week 25
  // 22 23 24 25 26 27 28   # Week 26
  // 29 30                  # Week 27

  var dtStart = time.date(2008, 6, 17);
  var g = generators.byWeekNoGenerator(
      [24, -27, 24, 10, -2], WeekDay.SU, dtStart);
  g.reset();

  var builder = [time.date(2008, 6, 18)];
  assertTrue(g.generate(builder));
  assertEquals('20080608', time.toIcal(builder[0]));
  assertTrue(g.generate(builder));
  assertEquals('20080609', time.toIcal(builder[0]));
  assertTrue(g.generate(builder));
  assertEquals('20080610', time.toIcal(builder[0]));
  assertTrue(g.generate(builder));
  assertEquals('20080611', time.toIcal(builder[0]));
  assertTrue(g.generate(builder));
  assertEquals('20080612', time.toIcal(builder[0]));
  assertTrue(g.generate(builder));
  assertEquals('20080613', time.toIcal(builder[0]));
  assertTrue(g.generate(builder));
  assertEquals('20080614', time.toIcal(builder[0]));
  assertTrue(g.generate(builder));
  assertEquals('20080629', time.toIcal(builder[0]));
  assertTrue(g.generate(builder));
  assertEquals('20080630', time.toIcal(builder[0]));
  assertFalse(g.generate(builder));
}

function testByWeekNoGenerator2() {
  // $ ncal -w 6 2008
  //     June 2008
  // Mo     2  9 16 23 30
  // Tu     3 10 17 24
  // We     4 11 18 25
  // Th     5 12 19 26
  // Fr     6 13 20 27
  // Sa     7 14 21 28
  // Su  1  8 15 22 29
  //    22 23 24 25 26 27   # Week Numbers

  var dtStart = time.date(2008, 6, 17);
  var g = generators.byWeekNoGenerator(
      [23, -27, 23, 10, -2], WeekDay.MO, dtStart);
  g.reset();

  var builder = [time.date(2008, 6, 18)];
  assertTrue(g.generate(builder));
  assertEquals('20080602', time.toIcal(builder[0]));
  assertTrue(g.generate(builder));
  assertEquals('20080603', time.toIcal(builder[0]));
  assertTrue(g.generate(builder));
  assertEquals('20080604', time.toIcal(builder[0]));
  assertTrue(g.generate(builder));
  assertEquals('20080605', time.toIcal(builder[0]));
  assertTrue(g.generate(builder));
  assertEquals('20080606', time.toIcal(builder[0]));
  assertTrue(g.generate(builder));
  assertEquals('20080607', time.toIcal(builder[0]));
  assertTrue(g.generate(builder));
  assertEquals('20080608', time.toIcal(builder[0]));
  assertTrue(g.generate(builder));
  assertEquals('20080630', time.toIcal(builder[0]));
  assertFalse(g.generate(builder));
}

function testByYearDayGenerator() {
  var dtStart = time.date(2008, 6, 17);
  var g = generators.byYearDayGenerator([2, 165, -350, -1, -1, -1], dtStart);
  g.reset();

  var builder = [time.date(2008, 6, 18)];
  assertTrue(g.generate(builder));
  assertEquals('20080613', time.toIcal(builder[0]));  // 165
  assertFalse(g.generate(builder));

  builder[0] = time.date(2008, 12, 18);
  assertTrue(g.generate(builder));
  assertEquals('20081231', time.toIcal(builder[0]));  // -1
  assertFalse(g.generate(builder));
  
  builder[0] = time.date(2009, 1, 1);
  assertTrue(g.generate(builder));
  assertEquals('20090102', time.toIcal(builder[0]));  // 2
  assertTrue(g.generate(builder));
  assertEquals('20090116', time.toIcal(builder[0]));  // -350
  assertFalse(g.generate(builder));
}
