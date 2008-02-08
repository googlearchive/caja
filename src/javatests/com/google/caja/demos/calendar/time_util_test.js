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


function WeekDayNum(num, wday) { return { num: num, wday: wday }; }


function testDayNumToDateInMonth() {
  //        March 2006
  // Su Mo Tu We Th Fr Sa
  //           1  2  3  4
  //  5  6  7  8  9 10 11
  // 12 13 14 15 16 17 18
  // 19 20 21 22 23 24 25
  // 26 27 28 29 30 31
  var dow0 = WeekDay.WE;
  var nDays = 31;
  var d0 = 0;

  assertEquals(
      1, time_util.dayNumToDate(dow0, nDays, 1, WeekDay.WE, d0, nDays));
  assertEquals(
      8, time_util.dayNumToDate(dow0, nDays, 2, WeekDay.WE, d0, nDays));
  assertEquals(
      29, time_util.dayNumToDate(dow0, nDays, -1, WeekDay.WE, d0, nDays));
  assertEquals(
      22, time_util.dayNumToDate(dow0, nDays, -2, WeekDay.WE, d0, nDays));

  assertEquals(
      3, time_util.dayNumToDate(dow0, nDays, 1, WeekDay.FR, d0, nDays));
  assertEquals(
      10, time_util.dayNumToDate(dow0, nDays, 2, WeekDay.FR, d0, nDays));
  assertEquals(
      31, time_util.dayNumToDate(dow0, nDays, -1, WeekDay.FR, d0, nDays));
  assertEquals(
      24, time_util.dayNumToDate(dow0, nDays, -2, WeekDay.FR, d0, nDays));

  assertEquals(
      7, time_util.dayNumToDate(dow0, nDays, 1, WeekDay.TU, d0, nDays));
  assertEquals(
      14, time_util.dayNumToDate(dow0, nDays, 2, WeekDay.TU, d0, nDays));
  assertEquals(
      28, time_util.dayNumToDate(dow0, nDays, 4, WeekDay.TU, d0, nDays));
  assertEquals(
      0, time_util.dayNumToDate(dow0, nDays, 5, WeekDay.TU, d0, nDays));
  assertEquals(
      28, time_util.dayNumToDate(dow0, nDays, -1, WeekDay.TU, d0, nDays));
  assertEquals(
      21, time_util.dayNumToDate(dow0, nDays, -2, WeekDay.TU, d0, nDays));
  assertEquals(
      7, time_util.dayNumToDate(dow0, nDays, -4, WeekDay.TU, d0, nDays));
  assertEquals(
      0, time_util.dayNumToDate(dow0, nDays, -5, WeekDay.TU, d0, nDays));
}

function testDayNumToDateInYear() {
  //        January 2006
  //  # Su Mo Tu We Th Fr Sa
  //  1  1  2  3  4  5  6  7
  //  2  8  9 10 11 12 13 14
  //  3 15 16 17 18 19 20 21
  //  4 22 23 24 25 26 27 28
  //  5 29 30 31

  //      February 2006
  //  # Su Mo Tu We Th Fr Sa
  //  5           1  2  3  4
  //  6  5  6  7  8  9 10 11
  //  7 12 13 14 15 16 17 18
  //  8 19 20 21 22 23 24 25
  //  9 26 27 28

  //           March 2006
  //  # Su Mo Tu We Th Fr Sa
  //  9           1  2  3  4
  // 10  5  6  7  8  9 10 11
  // 11 12 13 14 15 16 17 18
  // 12 19 20 21 22 23 24 25
  // 13 26 27 28 29 30 31

  var dow0 = WeekDay.SU;
  var nInMonth = 31;
  var nDays = 365;
  var d0 = 59;

  // TODO(mikesamuel): check that these answers are right
  assertEquals(
      1, time_util.dayNumToDate(dow0, nDays, 9, WeekDay.WE, d0, nInMonth));
  assertEquals(
      8, time_util.dayNumToDate(dow0, nDays, 10, WeekDay.WE, d0, nInMonth));
  assertEquals(
      29, time_util.dayNumToDate(dow0, nDays, -40, WeekDay.WE, d0, nInMonth));
  assertEquals(
      22, time_util.dayNumToDate(dow0, nDays, -41, WeekDay.WE, d0, nInMonth));

  assertEquals(
      3, time_util.dayNumToDate(dow0, nDays, 9, WeekDay.FR, d0, nInMonth));
  assertEquals(
      10, time_util.dayNumToDate(dow0, nDays, 10, WeekDay.FR, d0, nInMonth));
  assertEquals(
      31, time_util.dayNumToDate(dow0, nDays, -40, WeekDay.FR, d0, nInMonth));
  assertEquals(
      24, time_util.dayNumToDate(dow0, nDays, -41, WeekDay.FR, d0, nInMonth));

  assertEquals(
      7, time_util.dayNumToDate(dow0, nDays, 10, WeekDay.TU, d0, nInMonth));
  assertEquals(
      14, time_util.dayNumToDate(dow0, nDays, 11, WeekDay.TU, d0, nInMonth));
  assertEquals(
      28, time_util.dayNumToDate(dow0, nDays, 13, WeekDay.TU, d0, nInMonth));
  assertEquals(
      0, time_util.dayNumToDate(dow0, nDays, 14, WeekDay.TU, d0, nInMonth));
  assertEquals(
      28, time_util.dayNumToDate(dow0, nDays, -40, WeekDay.TU, d0, nInMonth));
  assertEquals(
      21, time_util.dayNumToDate(dow0, nDays, -41, WeekDay.TU, d0, nInMonth));
  assertEquals(
      7, time_util.dayNumToDate(dow0, nDays, -43, WeekDay.TU, d0, nInMonth));
  assertEquals(
      0, time_util.dayNumToDate(dow0, nDays, -44, WeekDay.TU, d0, nInMonth));
}

function testUniquify() {
  var ints = [ 1, 4, 4, 2, 7, 3, 8, 0, 0, 3 ];
  ints = time_util.uniquify(ints);
  assertEquals("0,1,2,3,4,7,8", String(ints));
}

function testNextWeekStart() {
  assertEquals(time.date(2006, 1, 24),
               time_util.nextWeekStart(time.date(2006, 1, 23), WeekDay.TU));

  assertEquals(time.date(2006, 1, 24),
               time_util.nextWeekStart(time.date(2006, 1, 24), WeekDay.TU));

  assertEquals(time.date(2006, 1, 31),
               time_util.nextWeekStart(time.date(2006, 1, 25), WeekDay.TU));

  assertEquals(time.date(2006, 1, 23),
               time_util.nextWeekStart(time.date(2006, 1, 23), WeekDay.MO));

  assertEquals(time.date(2006, 1, 30),
               time_util.nextWeekStart(time.date(2006, 1, 24), WeekDay.MO));

  assertEquals(time.date(2006, 1, 30),
               time_util.nextWeekStart(time.date(2006, 1, 25), WeekDay.MO));

  assertEquals(time.date(2006, 2, 6),
               time_util.nextWeekStart(time.date(2006, 1, 31), WeekDay.MO));
}

function testCountInPeriod() {
  //        January 2006
  //  Su Mo Tu We Th Fr Sa
  //   1  2  3  4  5  6  7
  //   8  9 10 11 12 13 14
  //  15 16 17 18 19 20 21
  //  22 23 24 25 26 27 28
  //  29 30 31
  assertEquals(5, time_util.countInPeriod(WeekDay.SU, WeekDay.SU, 31));
  assertEquals(5, time_util.countInPeriod(WeekDay.MO, WeekDay.SU, 31));
  assertEquals(5, time_util.countInPeriod(WeekDay.TU, WeekDay.SU, 31));
  assertEquals(4, time_util.countInPeriod(WeekDay.WE, WeekDay.SU, 31));
  assertEquals(4, time_util.countInPeriod(WeekDay.TH, WeekDay.SU, 31));
  assertEquals(4, time_util.countInPeriod(WeekDay.FR, WeekDay.SU, 31));
  assertEquals(4, time_util.countInPeriod(WeekDay.SA, WeekDay.SU, 31));

  //      February 2006
  //  Su Mo Tu We Th Fr Sa
  //            1  2  3  4
  //   5  6  7  8  9 10 11
  //  12 13 14 15 16 17 18
  //  19 20 21 22 23 24 25
  //  26 27 28
  assertEquals(4, time_util.countInPeriod(WeekDay.SU, WeekDay.WE, 28));
  assertEquals(4, time_util.countInPeriod(WeekDay.MO, WeekDay.WE, 28));
  assertEquals(4, time_util.countInPeriod(WeekDay.TU, WeekDay.WE, 28));
  assertEquals(4, time_util.countInPeriod(WeekDay.WE, WeekDay.WE, 28));
  assertEquals(4, time_util.countInPeriod(WeekDay.TH, WeekDay.WE, 28));
  assertEquals(4, time_util.countInPeriod(WeekDay.FR, WeekDay.WE, 28));
  assertEquals(4, time_util.countInPeriod(WeekDay.SA, WeekDay.WE, 28));
}

function testInvertWeekdayNum() {

  //        January 2006
  //  # Su Mo Tu We Th Fr Sa
  //  1  1  2  3  4  5  6  7
  //  2  8  9 10 11 12 13 14
  //  3 15 16 17 18 19 20 21
  //  4 22 23 24 25 26 27 28
  //  5 29 30 31

  // the 1st falls on a sunday, so dow0 == SU
  assertEquals(
      5,
      time_util.invertWeekdayNum(
          new WeekDayNum(-1, WeekDay.SU), WeekDay.SU, 31));
  assertEquals(
      5,
      time_util.invertWeekdayNum(
          new WeekDayNum(-1, WeekDay.MO), WeekDay.SU, 31));
  assertEquals(
      5,
      time_util.invertWeekdayNum(
          new WeekDayNum(-1, WeekDay.TU), WeekDay.SU, 31));
  assertEquals(
      4,
      time_util.invertWeekdayNum(
          new WeekDayNum(-1, WeekDay.WE), WeekDay.SU, 31));
  assertEquals(
      3,
      time_util.invertWeekdayNum(
          new WeekDayNum(-2, WeekDay.WE), WeekDay.SU, 31));


  //      February 2006
  //  # Su Mo Tu We Th Fr Sa
  //  1           1  2  3  4
  //  2  5  6  7  8  9 10 11
  //  3 12 13 14 15 16 17 18
  //  4 19 20 21 22 23 24 25
  //  5 26 27 28

  assertEquals(
      4,
      time_util.invertWeekdayNum(
          new WeekDayNum(-1, WeekDay.SU), WeekDay.WE, 28));
  assertEquals(
      4,
      time_util.invertWeekdayNum(
          new WeekDayNum(-1, WeekDay.MO), WeekDay.WE, 28));
  assertEquals(
      4,
      time_util.invertWeekdayNum(
          new WeekDayNum(-1, WeekDay.TU), WeekDay.WE, 28));
  assertEquals(
      4,
      time_util.invertWeekdayNum(
          new WeekDayNum(-1, WeekDay.WE), WeekDay.WE, 28));
  assertEquals(
      3,
      time_util.invertWeekdayNum(
          new WeekDayNum(-2, WeekDay.WE), WeekDay.WE, 28));
}
