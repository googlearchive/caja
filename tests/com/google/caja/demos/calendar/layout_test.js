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


/** @type {Array.<Event>} */
var events;

function setUp() {
  // Day   1    2    3    4    5    6    7    8
  // ==============================================
  //      |CCCC|CCCC|CCCC|FFFF|FFFF|QQQQ|QQQQ|QQQQ|
  //      |    |GGGG|GGGG|GGGG|    |    |    |    |
  //      |    |EEEE|    |IIII|IIII|    |    |    |
  // ==============================================
  // 0h   |    |    |    |    |    |OOOO|    |    |
  //      |    |    |    |    |    |OOOO|    |    |
  // 4h   |    |    |B   |    |    |    |    |    |
  //      |    |    |B   |    |    |    |    |    |
  // 8h   |    |    |BD  |    |LLMM|    |    |    |
  //      |    |    |BD  |    |LLMM|    |    |    |
  // 12h  |AAAA|    |BDH |    |NNNN|    |    |    |
  //      |AAAA|    |  HJ|    |NNNN|    |    |    |
  // 16h  |    |    |KKH |    |    |    |    |    |
  //      |    |    |KK  |    |    |    |    |    |
  // 20h  |    |    |    |    |OO  |    |    |    |
  //      |    |    |    |    |OOPP|    |    |    |
  // ==============================================

  events = [];
  events.push(new VEvent('eid-A', 'cal-0', 'A',
                         time.parseIcal('20060101T120000'),
                         time.parseIcal('20060101T160000')));
  events.push(new VEvent('eid-B', 'cal-0', 'B',
                         time.parseIcal('20060103T040000'),
                         time.parseIcal('20060103T140000')));
  events.push(new VEvent('eid-C', 'cal-0', 'C',
                         time.parseIcal('20060101'),
                         time.parseIcal('20060104')));
  events.push(new VEvent('eid-D', 'cal-1', 'D',
                         time.parseIcal('20060103T080000'),
                         time.parseIcal('20060103T140000')));
  events.push(new VEvent('eid-E', 'cal-0', 'E',
                         time.parseIcal('20060102'),
                         time.parseIcal('20060103')));
  events.push(new VEvent('eid-F', 'cal-0', 'F',
                         time.parseIcal('20060104'),
                         time.parseIcal('20060106')));
  events.push(new VEvent('eid-G', 'cal-0', 'G',
                         time.parseIcal('20060102'),
                         time.parseIcal('20060105')));
  events.push(new VEvent('eid-H', 'cal-1', 'H',
                         time.parseIcal('20060103T120000'),
                         time.parseIcal('20060103T180000')));
  events.push(new VEvent('eid-I', 'cal-1', 'I',
                         time.parseIcal('20060104'),
                         time.parseIcal('20060106')));
  events.push(new VEvent('eid-J', 'cal-1', 'J',
                         time.parseIcal('20060103T140000'),
                         time.parseIcal('20060103T160000')));
  events.push(new VEvent('eid-K', 'cal-1', 'K',
                         time.parseIcal('20060103T160000'),
                         time.parseIcal('20060103T200000')));
  events.push(new VEvent('eid-L', 'cal-1', 'L',
                         time.parseIcal('20060105T080000'),
                         time.parseIcal('20060105T120000')));
  events.push(new VEvent('eid-M', 'cal-2', 'M',
                         time.parseIcal('20060105T080000'),
                         time.parseIcal('20060105T120000')));
  events.push(new VEvent('eid-N', 'cal-2', 'N',
                         time.parseIcal('20060105T120000'),
                         time.parseIcal('20060105T160000')));
  events.push(new VEvent('eid-O', 'cal-1', 'O',
                         time.parseIcal('20060105T200000'),
                         time.parseIcal('20060106T040000')));
  events.push(new VEvent('eid-P', 'cal-0', 'P',
                         time.parseIcal('20060105T220000'),
                         time.parseIcal('20060106T000000')));
  events.push(new VEvent('eid-Q', 'cal-2', 'Q',
                         time.parseIcal('20060106'),
                         time.parseIcal('20060109')));
}

function tearDown() {
  events = null;
}

jsunitRegister('testDayView', function testDayView() {
  var start = time.date(2006, 1, 5),
        end = time.date(2006, 1, 6);
  var policy = new LayoutPolicy(1, start, 0);
  var layout = new Layout(policy);
  layout.layout(start, end, eventsIntersecting(start, end));
  // Check the position and slotting.
  // The slot number is <slot>+<extent>/<count>.
  // In this example, there are 3 overnight events, so the top 3 slots (and
  // 6 rows) are for all day events.  L and M overlap, so are in a group with
  // 2 slots.
  assertChips([
               { eid: 'eid-F', col0: 0, col1: 1, row0: 0, row1: 1,
                 slot: '0+1/1' },
               { eid: 'eid-I', col0: 0, col1: 1, row0: 1, row1: 2,
                 slot: '0+1/1' },
               { eid: 'eid-O', col0: 0, col1: 1, row0: 2, row1: 3,
                 slot: '0+1/1' }
               ], layout.untimedChips);
  assertChips([
               { eid: 'eid-L', col0: 0, col1: 1, row0: 16, row1: 24,
                 slot: '0+1/2' },
               { eid: 'eid-M', col0: 0, col1: 1, row0: 16, row1: 24,
                 slot: '1+1/2' },
               { eid: 'eid-N', col0: 0, col1: 1, row0: 24, row1: 32,
                 slot: '0+1/1' },
               { eid: 'eid-P', col0: 0, col1: 1, row0: 44, row1: 48,
                 slot: '0+1/1' }
               ], layout.timedChips);
});

jsunitRegister('testWeekView', function testWeekView() {
  var start = time.date(2006, 1, 1),
        end = time.date(2006, 1, 8);
  var policy = new LayoutPolicy(7, start, 0);
  var layout = new Layout(policy);
  layout.layout(start, end, eventsIntersecting(start, end));
  assertChips([
               { eid: 'eid-C', col0: 0, col1: 3, row0: 0, row1: 1,
                 slot: '0+1/1' },
               { eid: 'eid-E', col0: 1, col1: 2, row0: 2, row1: 3,
                 slot: '0+1/1' },
               { eid: 'eid-F', col0: 3, col1: 5, row0: 0, row1: 1,
                 slot: '0+1/1' },
               { eid: 'eid-G', col0: 1, col1: 4, row0: 1, row1: 2,
                 slot: '0+1/1' },
               { eid: 'eid-I', col0: 3, col1: 5, row0: 2, row1: 3,
                 slot: '0+1/1' },
               { eid: 'eid-O', col0: 4, col1: 6, row0: 1, row1: 2,
                 slot: '0+1/1' },
               { eid: 'eid-Q', col0: 5, col1: 7, row0: 0, row1: 1,
                 slot: '0+1/1' }
               ], layout.untimedChips);
  assertChips([
               { eid: 'eid-A', col0: 0, col1: 1, row0: 24, row1: 32,
                 slot: '0+1/1' },
               { eid: 'eid-B', col0: 2, col1: 3, row0: 8, row1: 28,
                 slot: '0+1/3' },
               { eid: 'eid-D', col0: 2, col1: 3, row0: 16, row1: 28,
                 slot: '1+1/3' },
               { eid: 'eid-H', col0: 2, col1: 3, row0: 24, row1: 36,
                 slot: '2+1/3' },
               { eid: 'eid-J', col0: 2, col1: 3, row0: 28, row1: 32,
                 slot: '0+2/3' },
               { eid: 'eid-K', col0: 2, col1: 3, row0: 32, row1: 40,
                 slot: '0+2/3' },
               { eid: 'eid-L', col0: 4, col1: 5, row0: 16, row1: 24,
                 slot: '0+1/2' },
               { eid: 'eid-M', col0: 4, col1: 5, row0: 16, row1: 24,
                 slot: '1+1/2' },
               { eid: 'eid-N', col0: 4, col1: 5, row0: 24, row1: 32,
                 slot: '0+1/1' },
               { eid: 'eid-P', col0: 4, col1: 5, row0: 44, row1: 48,
                 slot: '0+1/1' }
               ], layout.timedChips);
});

jsunitRegister('testWeekViewSansWeekends', function testWeekViewSansWeekends() {
  var start = time.date(2006, 1, 2), // advance start to skip Sunday
        end = time.date(2006, 1, 9);
  var policy = new LayoutPolicy(7, start, 2 /* skip 2 cols */);
  var layout = new Layout(policy);
  layout.layout(start, end, eventsIntersecting(start, end));
  assertChips([
               { eid: 'eid-C', col0: 0, col1: 2, row0: 0, row1: 1,
                 slot: '0+1/1' },  // clipped to col 0
               { eid: 'eid-E', col0: 0, col1: 1, row0: 2, row1: 3,
                 slot: '0+1/1' },
               { eid: 'eid-F', col0: 2, col1: 4, row0: 0, row1: 1,
                 slot: '0+1/1' },
               { eid: 'eid-G', col0: 0, col1: 3, row0: 1, row1: 2,
                 slot: '0+1/1' },
               { eid: 'eid-I', col0: 2, col1: 4, row0: 2, row1: 3,
                 slot: '0+1/1' },
               { eid: 'eid-O', col0: 3, col1: 5, row0: 1, row1: 2,
                 slot: '0+1/1' },
               { eid: 'eid-Q', col0: 4, col1: 5, row0: 0, row1: 1,
                 slot: '0+1/1' }  // clipped to col 5
               ], layout.untimedChips);
  assertChips([
               // no eid-A since that on first day
               { eid: 'eid-B', col0: 1, col1: 2, row0: 8, row1: 28,
                 slot: '0+1/3' },
               { eid: 'eid-D', col0: 1, col1: 2, row0: 16, row1: 28,
                 slot: '1+1/3' },
               { eid: 'eid-H', col0: 1, col1: 2, row0: 24, row1: 36,
                 slot: '2+1/3' },
               { eid: 'eid-J', col0: 1, col1: 2, row0: 28, row1: 32,
                 slot: '0+2/3' },
               { eid: 'eid-K', col0: 1, col1: 2, row0: 32, row1: 40,
                 slot: '0+2/3' },
               { eid: 'eid-L', col0: 3, col1: 4, row0: 16, row1: 24,
                 slot: '0+1/2' },
               { eid: 'eid-M', col0: 3, col1: 4, row0: 16, row1: 24,
                 slot: '1+1/2' },
               { eid: 'eid-N', col0: 3, col1: 4, row0: 24, row1: 32,
                 slot: '0+1/1' },
               { eid: 'eid-P', col0: 3, col1: 4, row0: 44, row1: 48,
                 slot: '0+1/1' }
               ], layout.timedChips);
});

jsunitRegister('testMonthView', function testMonthView() {
  //     0   1   2   3   4   5   6
  // 0   
  //
  // 1.1 CCCCCCCCCCC FFFFFFF QQQQQQQ
  //  .2 AAA GGGGGGGGGGG OOOOOOO
  //  .3     EEE BBB IIIIIII
  //  .4         DDD     PPP
  //  .5         HHH     LLL
  //  .6         JJJ     MMM
  //  .7         KKK     NNN
  //
  // 2.1 QQQ 
  //

  var start = time.date(2005, 12, 25),
        end = time.date(2006, 2, 5);
  var policy = new LayoutPolicy(42, start, 0);
  var layout = new Layout(policy);
  layout.layout(start, end, eventsIntersecting(start, end));
  layout.cullAt([4, 4, 4, 4, 4, 4]);
  assertChips([
               { col0:0, col1:1, eid:'eid-A', row0:1, row1:2,
                 slot:'2+1/8' },
               { col0:2, col1:3, eid:'eid-B', row0:1, row1:2,
                 slot:'3+1/8' },
               { col0:0, col1:3, eid:'eid-C', row0:1, row1:2,
                 slot:'1+1/8' },
               { col0:2, col1:3, culled:true, eid:'eid-D', row0:1, row1:2,
                 slot:'4+1/8' },
               { col0:1, col1:2, eid:'eid-E', row0:1, row1:2,
                 slot:'3+1/8' },
               { col0:3, col1:5, eid:'eid-F', row0:1, row1:2,
                 slot:'1+1/8' },
               { col0:1, col1:4, eid:'eid-G', row0:1, row1:2,
                 slot:'2+1/8' },
               { col0:2, col1:3, culled:true, eid:'eid-H', row0:1, row1:2,
                 slot:'5+1/8' },
               { col0:3, col1:5, eid:'eid-I', row0:1, row1:2,
                 slot:'3+1/8' },
               { col0:2, col1:3, culled:true, eid:'eid-J', row0:1, row1:2,
                 slot:'6+1/8' },
               { col0:2, col1:3, culled:true, eid:'eid-K', row0:1, row1:2,
                 slot:'7+1/8' },
               { col0:4, col1:5, culled:true, eid:'eid-L', row0:1, row1:2,
                 slot:'5+1/8' },
               { col0:4, col1:5, culled:true, eid:'eid-M', row0:1, row1:2,
                 slot:'6+1/8' },
               { col0:4, col1:5, culled:true, eid:'eid-N', row0:1, row1:2,
                 slot:'7+1/8' },
               { col0:4, col1:6, eid:'eid-O', row0:1, row1:2,
                 slot:'2+1/8' },
               { col0:4, col1:5, culled:true, eid:'eid-P', row0:1, row1:2,
                 slot:'4+1/8' },
               { col0:5, col1:7, eid:'eid-Q', row0:1, row1:2,
                 slot:'1+1/8' },
               { col0:0, col1:1, eid:'eid-Q', row0:2, row1:3,
                 slot:'1+1/2' }
               ], layout.untimedChips);
});

jsunitRegister('testMonthViewMoreSlots', function testMonthViewMoreSlots() {
  //     0   1   2   3   4   5   6
  // 0   
  //
  // 1.1 CCCCCCCCCCC FFFFFFF QQQQQQQ
  //  .2 AAA GGGGGGGGGGG OOOOOOO
  //  .3     EEE BBB IIIIIII
  //  .4         DDD     PPP
  //  .5         HHH     LLL
  //  .6         JJJ     MMM
  //  .7         KKK     NNN
  //
  // 2.1 QQQ 
  //

  var start = time.date(2005, 12, 25),
        end = time.date(2006, 2, 5);
  var policy = new LayoutPolicy(42, start, 0);
  var layout = new Layout(policy);
  layout.layout(start, end, eventsIntersecting(start, end));
  var nSlots = 6;
  layout.cullAt([6, 6, 6, 6, 6, 6]);
  assertChips([
               { col0:0, col1:1, eid:'eid-A', row0:1, row1:2,
                 slot:'2+1/8' },
               { col0:2, col1:3, eid:'eid-B', row0:1, row1:2,
                 slot:'3+1/8' },
               { col0:0, col1:3, eid:'eid-C', row0:1, row1:2,
                 slot:'1+1/8' },
               { col0:2, col1:3, eid:'eid-D', row0:1, row1:2,
                 slot:'4+1/8' },
               { col0:1, col1:2, eid:'eid-E', row0:1, row1:2,
                 slot:'3+1/8' },
               { col0:3, col1:5, eid:'eid-F', row0:1, row1:2,
                 slot:'1+1/8' },
               { col0:1, col1:4, eid:'eid-G', row0:1, row1:2,
                 slot:'2+1/8' },
               { col0:2, col1:3, eid:'eid-H', row0:1, row1:2,
                 slot:'5+1/8' },
               { col0:3, col1:5, eid:'eid-I', row0:1, row1:2,
                 slot:'3+1/8' },
               { col0:2, col1:3, culled:true, eid:'eid-J', row0:1, row1:2,
                 slot:'6+1/8' },
               { col0:2, col1:3, culled:true, eid:'eid-K', row0:1, row1:2,
                 slot:'7+1/8' },
               { col0:4, col1:5, eid:'eid-L', row0:1, row1:2,
                 slot:'5+1/8' },
               { col0:4, col1:5, culled:true, eid:'eid-M', row0:1, row1:2,
                 slot:'6+1/8' },
               { col0:4, col1:5, culled:true, eid:'eid-N', row0:1, row1:2,
                 slot:'7+1/8' },
               { col0:4, col1:6, eid:'eid-O', row0:1, row1:2,
                 slot:'2+1/8' },
               { col0:4, col1:5, eid:'eid-P', row0:1, row1:2,
                 slot:'4+1/8' },
               { col0:5, col1:7, eid:'eid-Q', row0:1, row1:2,
                 slot:'1+1/8' },
               // second row
               { col0:0, col1:1, eid:'eid-Q', row0:2, row1:3,
                 slot:'1+1/2' }
               ], layout.untimedChips);
});

jsunitRegister(
    'testMonthViewSansWeekends', function testMonthViewSansWeekends() {
  //     0   1   2   3   4
  // 0   
  //         
  // 1.1 GGGGGGGGGGG OOOOOOO
  //  .2 CCCCCCC FFFFFFF QQQ
  //  .3 EEE BBB IIIIIII
  //  .4     DDD     PPP
  //  .5     HHH     LLL
  //  .6     JJJ     MMM
  //  .7     KKK     NNN
  //
  // 2

  var start = time.date(2005, 12, 26), // advance start to skip Sunday
        end = time.date(2006, 2, 6);
  var policy = new LayoutPolicy(42, start, 2 /* skip cols */);
  var layout = new Layout(policy);
  var chips = [];
  layout.layout(start, end, eventsIntersecting(start, end));
  layout.cullAt([4, 4, 4, 4, 4, 4]);
  assertChips([
               // no chip for A
               { col0:1, col1:2, eid:'eid-B', row0:1, row1:2,
                 slot:'3+1/8' },
               { col0:0, col1:2, eid:'eid-C', row0:1, row1:2,
                 slot:'2+1/8' },
               { col0:1, col1:2, culled:true, eid:'eid-D', row0:1, row1:2,
                 slot:'4+1/8' },
               { col0:0, col1:1, eid:'eid-E', row0:1, row1:2,
                 slot:'3+1/8' },
               { col0:2, col1:4, eid:'eid-F', row0:1, row1:2,
                 slot:'2+1/8' },
               { col0:0, col1:3, eid:'eid-G', row0:1, row1:2,
                 slot:'1+1/8' },
               { col0:1, col1:2, culled:true, eid:'eid-H', row0:1, row1:2,
                 slot:'5+1/8' },
               { col0:2, col1:4, eid:'eid-I', row0:1, row1:2,
                 slot:'3+1/8' },
               { col0:1, col1:2, culled:true, eid:'eid-J', row0:1, row1:2,
                 slot:'6+1/8' },
               { col0:1, col1:2, culled:true, eid:'eid-K', row0:1, row1:2,
                 slot:'7+1/8' },
               { col0:3, col1:4, culled:true, eid:'eid-L', row0:1, row1:2,
                 slot:'5+1/8' },
               { col0:3, col1:4, culled:true, eid:'eid-M', row0:1, row1:2,
                 slot:'6+1/8' },
               { col0:3, col1:4, culled:true, eid:'eid-N', row0:1, row1:2,
                 slot:'7+1/8' },
               { col0:3, col1:5, eid:'eid-O', row0:1, row1:2,
                 slot:'1+1/8' },
               { col0:3, col1:4, culled:true, eid:'eid-P', row0:1, row1:2,
                 slot:'4+1/8' },
               { col0:4, col1:5, eid:'eid-Q', row0:1, row1:2,
                 slot:'2+1/8' }
               // no second chip for Q since it only covers Sunday
               ], layout.untimedChips);
});

function eventsIntersecting(start, end) {
  var intersecting = [];
  for (var i = 0; i < events.length; ++i) {
    var event = events[i];
    var intersects = !(event.start >= end || event.end <= start);
    if (intersects) {
      intersecting.push(event);
    }
  }
  return intersecting;
}

function assertChips(golden, chips) {
  var actual = [];
  for (var i = 0; i < chips.length; ++i) {
    var chip = chips[i];
    var obj = { eid: chip.event.eid,
                col0: chip.col0,
                col1: chip.col1,
                row0: chip.row0,
                row1: chip.row1,
                slot: (chip.slot + '+' + chip.slotExtent + '/' + chip.slotCount)
              };
    if (chip.culled) { obj.culled = true; }
    actual.push(obj);
  }
  var cmp =
    function (a, b) {
      var aeid = a.eid,
          beid = b.eid;
      if (aeid !== beid) {
        return aeid < beid ? -1 : 1;
      }
      var delta = (a.col0 * 42 + a.row0) - (b.col0 * 42 + b.row0);
      return delta ? (delta < 0 ? -1 : 1) : 0;
    };
  golden.sort(cmp);
  actual.sort(cmp);

  assertObjectListsEqual(golden, actual);
}

function assertObjectListsEqual(golden, actual) {
  var goldenOut = [];
  var actualOut = [];
  for (var i = 0; i < golden.length; ++i) {
    goldenOut.push(canonPodString(golden[i]));
  }
  for (var i = 0; i < actual.length; ++i) {
    actualOut.push(canonPodString(actual[i]));
  }
  assertEquals('[' + goldenOut.join('\n') + ']',
               '[' + actualOut.join('\n') + ']');
}

function canonPodString(pod) {
  var out = [];
  var keys = [];
  for (var key in pod) { keys.push(key); }
  keys.sort();
  for (var i = 0; i < keys.length; ++i) {
    var key = keys[i];
    out.push(key + '=' + pod[key]);
  }
  if (!out.length) { return '{}'; }
  return '{ ' + out.join(', ') + ' }';
}
