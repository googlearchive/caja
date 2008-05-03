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


jsunitRegister('testEmptyEventStore', function testEmptyEventStore() {
  assertEvents(
      [],
      [new ContentLine('BEGIN', 'VCALENDAR'),
       new ContentLine('END', 'VCALENDAR')
       ]);
});

jsunitRegister('testOutOfRangeEventStore', function testOutOfRangeEventStore() {
  assertEvents(
      [],
      [new ContentLine('BEGIN', 'VCALENDAR'),
       new ContentLine('BEGIN', 'VEVENT'),
       new ContentLine('SUMMARY', 'Foo'),
       new ContentLine('DTSTART', '20080129'),
       new ContentLine('DTEND', '20080201'),
       new ContentLine('END', 'VEVENT'),
       new ContentLine('BEGIN', 'VEVENT'),
       new ContentLine('SUMMARY', 'Bar'),
       new ContentLine('DTSTART', '20080629'),
       new ContentLine('DTEND', '20080701'),
       new ContentLine('END', 'VEVENT'),
       new ContentLine('END', 'VCALENDAR')
       ]);
});

jsunitRegister('testEventStoreWithSingleEvents',
function testEventStoreWithSingleEvents() {
  assertEvents(
      ['(Event eid="autoeid-3" summary="Baz" 20080315/20080317)',
       '(Event eid="eid-0" summary="Far" 20080402T120000/20080402T123000)'],
      [new ContentLine('BEGIN', 'VCALENDAR'),
       new ContentLine('BEGIN', 'VEVENT'),
       new ContentLine('SUMMARY', 'Foo'),
       new ContentLine('DTSTART', '20080129'),
       new ContentLine('DTEND', '20080201'),
       new ContentLine('END', 'VEVENT'),
       new ContentLine('BEGIN', 'VEVENT'),
       new ContentLine('SUMMARY', 'Bar'),
       new ContentLine('DTSTART', '20080629'),
       new ContentLine('DTEND', '20080701'),
       new ContentLine('END', 'VEVENT'),
       new ContentLine('BEGIN', 'VEVENT'),
       new ContentLine('SUMMARY', 'Baz'),
       new ContentLine('DTSTART', '20080315'),
       new ContentLine('DTEND', '20080317'),
       new ContentLine('END', 'VEVENT'),
       new ContentLine('BEGIN', 'VEVENT'),
       new ContentLine('SUMMARY', 'Far'),
       new ContentLine('DTSTART', '20080402T120000'),
       new ContentLine('DTEND', '20080402T123000'),
       new ContentLine('EID', 'eid-0'),
       new ContentLine('END', 'VEVENT'),
       new ContentLine('END', 'VCALENDAR')
       ]);
});

jsunitRegister('testEventStoreWithRecurringEvents',
function testEventStoreWithRecurringEvents() {
  assertEvents(
      ['(Event eid="eid-0-20080202" summary="Far" 20080202/20080203)',
       '(Event eid="eid-0-20080502" summary="Far" 20080502/20080503)'],
      [new ContentLine('BEGIN', 'VCALENDAR'),
       new ContentLine('END', 'VEVENT'),
       new ContentLine('BEGIN', 'VEVENT'),
       new ContentLine('SUMMARY', 'Far'),
       new ContentLine('DTSTART', '20070202'),
       new ContentLine('DURATION', 'P1D'),
       new ContentLine('RRULE', 'FREQ=MONTHLY;INTERVAL=3'),
       new ContentLine('EID', 'eid-0'),
       new ContentLine('END', 'VEVENT'),
       new ContentLine('END', 'VCALENDAR')
       ]);
});

jsunitRegister('testEventWithDuration', function testEventWithDuration() {
  assertEvents(
      ['(Event eid="eid-1" summary="Far" 20080303T140000/20080304T170000)'],
      [new ContentLine('BEGIN', 'VCALENDAR'),
       new ContentLine('END', 'VEVENT'),
       new ContentLine('BEGIN', 'VEVENT'),
       new ContentLine('SUMMARY', 'Far'),
       new ContentLine('DTSTART', '20080303T140000'),
       new ContentLine('DURATION', 'P1DT3H'),
       new ContentLine('EID', 'eid-1'),
       new ContentLine('END', 'VEVENT'),
       new ContentLine('END', 'VCALENDAR')
       ]);
});


function assertEvents(golden, contentLines) {
  var actual = event_store.toCalendar(
      contentLines, time.parseIcal('20080201'), time.parseIcal('20080601'))
      .events;

  var fmted = [];
  for (var i = 0; i < actual.length; ++i) {
    var e = actual[i];
    fmted.push('(Event eid="' + e.eid + '" summary="' + e.summary + '" '
               + time.toIcal(e.start) + '/' + time.toIcal(e.end) + ')');
  }

  assertEquals(golden.join(' ; '), fmted.join(' ; '));
}
