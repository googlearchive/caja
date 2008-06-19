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


jsunitRegister('testWeekIntervalFilter', function testWeekIntervalFilter() {
  // *s match those that are in the weeks that should pass the filter

  var f1 = filters.weekIntervalFilter(
      2, WeekDay.MO, time.parseIcal("20050911"));
  // FOR f1
  //    September 2005
  //  Su  Mo  Tu  We  Th  Fr  Sa
  //                   1   2   3
  //   4  *5  *6  *7  *8  *9 *10
  // *11  12  13  14  15  16  17
  //  18 *19 *20 *21 *22 *23 *24
  // *25  26  27  28  29  30
  assertTrue( f1(time.parseIcal("20050909")));
  assertTrue( f1(time.parseIcal("20050910")));
  assertTrue( f1(time.parseIcal("20050911")));
  assertTrue(!f1(time.parseIcal("20050912")));
  assertTrue(!f1(time.parseIcal("20050913")));
  assertTrue(!f1(time.parseIcal("20050914")));
  assertTrue(!f1(time.parseIcal("20050915")));
  assertTrue(!f1(time.parseIcal("20050916")));
  assertTrue(!f1(time.parseIcal("20050917")));
  assertTrue(!f1(time.parseIcal("20050918")));
  assertTrue( f1(time.parseIcal("20050919")));
  assertTrue( f1(time.parseIcal("20050920")));
  assertTrue( f1(time.parseIcal("20050921")));
  assertTrue( f1(time.parseIcal("20050922")));
  assertTrue( f1(time.parseIcal("20050923")));
  assertTrue( f1(time.parseIcal("20050924")));
  assertTrue( f1(time.parseIcal("20050925")));
  assertTrue(!f1(time.parseIcal("20050926")));

  var f2 = filters.weekIntervalFilter(
      2, WeekDay.SU, time.parseIcal("20050911"));
  // FOR f2
  //    September 2005
  //  Su  Mo  Tu  We  Th  Fr  Sa
  //                   1   2   3
  //   4   5   6   7   8   9  10
  // *11 *12 *13 *14 *15 *16 *17
  //  18  19  20  21  22  23  24
  // *25 *26 *27 *28 *29 *30
  assertTrue(!f2(time.parseIcal("20050909")));
  assertTrue(!f2(time.parseIcal("20050910")));
  assertTrue( f2(time.parseIcal("20050911")));
  assertTrue( f2(time.parseIcal("20050912")));
  assertTrue( f2(time.parseIcal("20050913")));
  assertTrue( f2(time.parseIcal("20050914")));
  assertTrue( f2(time.parseIcal("20050915")));
  assertTrue( f2(time.parseIcal("20050916")));
  assertTrue( f2(time.parseIcal("20050917")));
  assertTrue(!f2(time.parseIcal("20050918")));
  assertTrue(!f2(time.parseIcal("20050919")));
  assertTrue(!f2(time.parseIcal("20050920")));
  assertTrue(!f2(time.parseIcal("20050921")));
  assertTrue(!f2(time.parseIcal("20050922")));
  assertTrue(!f2(time.parseIcal("20050923")));
  assertTrue(!f2(time.parseIcal("20050924")));
  assertTrue( f2(time.parseIcal("20050925")));
  assertTrue( f2(time.parseIcal("20050926")));
});
