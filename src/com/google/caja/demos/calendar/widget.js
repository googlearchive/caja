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
 * A widget that displays a events on a calendar grid.
 *
 * @author mikesamuel@gmail.com
 */

/** @namespace */
var widget = (function () {
  var dayOfWeekAbbreviation = ['S', 'M', 'T', 'W', 'Th', 'F', 'Sa'];

  function render(layout) {
    var viewPort = computeVisibleArea(layout);
    return renderCalendar(layout, viewPort);
  }

  function computeVisibleArea(layout) {
    var policy = layout.getPolicy();
    var row0 = 0, row1 = policy.getYExtent();
    if (policy.isSpatial()) {
      var minRow = row1, maxRow = row0;
      var chips = layout.getTimedChips();
      for (var i = 0, n = chips.length; i < n; ++i) {
        var chip = chips[i];
        if (chip.culled) { continue; }
        if (chip.row0 < minRow) { minRow = chip.row0; }
        if (chip.row1 > maxRow) { maxRow = chip.row1; }
      }
      if (minRow >= maxRow) { minRow = maxRow = (row1 / 2) | 0; }
      row0 = Math.max(row0, (minRow - 2) & ~1);
      row1 = Math.min(row1, (maxRow + 2) & ~1);
    }
    return { row0: row0, row1: row1 };
  }

  function formatDate(date) {
    return time.year(date) + dash(time.month(date)) + dash(time.day(date));
  }
  
  function dash(x) { return (x < 10 ? '-0' : '-') + x; }

  return { render: render,
           formatDate: formatDate,
           dayOfWeekAbbreviation: dayOfWeekAbbreviation };
})();
