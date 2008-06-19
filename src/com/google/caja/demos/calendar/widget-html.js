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
 * Handcoded versions of widget-*.gxp.
 *
 * @author mikesamuel@gmail.com
 */

var renderCalendar = (function () {
  function renderCalendar(layout, viewPort) {
    var policy = layout.getPolicy();
    return htmlInterp.safeHtml(eval(Template(
        '<div class="calendar">'
        + '<div class="x-axis">${renderXAxis(policy)}</div>'
        + '<div style="height:${2 * layout.getNAllDayRows()}em"'
        + ' class="all-day-grid">${renderAllDayGrid(layout)}</div>'
        + '<div class="view-port">'
          + '<div class="y-axis">${renderYAxis(viewPort, policy)}</div>'
          + '<div class="main-grid">${renderMainGrid(viewPort, layout)}</div>'
        + '</div>'
        + '</div>')));
  }

  function renderXAxis(layoutPolicy) {
    var html = eval(Template(''));

    var includeDates = layoutPolicy.getYExtent() > 1;
    for (var i = 0, n = layoutPolicy.getXExtent(); i < n; ++i) {
      var date = layoutPolicy.cellToDate(i, 0);
      var dow = widget.dayOfWeekAbbreviation[time.weekDayOf(date)];

      if (includeDates) {
        html.append(eval(Template('<span>$dow ${widget.formatDate(date)}</span>'
                                  )));
      } else {
        html.append(eval(Template('<span>$dow</span>')));
      }
    }

    return html;
  }

  function renderYAxis(viewPort, layoutPolicy) {
    var html = eval(Template(''));
    if (layoutPolicy.isSpatial()) {
      var minHour = viewPort.row0 / layoutPolicy.getGradationsPerHour();
      var maxHour = viewPort.row1 / layoutPolicy.getGradationsPerHour();
      for (var hour = minHour; hour < maxHour; ++hour) {
        html.append(eval(Template(
            '<span>${(hour%12) || 12}${hour < 12 ? \'am\' : \'pm\'}</span>')));
      }
    }

    return html;
  }

  function renderAllDayGrid(layout) {
    return renderChips(
        layout.getUntimedChips(), false,
        layout.getPolicy().getXExtent(), 0, layout.getNAllDayRows());
  }

  function renderMainGrid(viewPort, layout) {
    return renderChips(
        layout.getTimedChips(), layout.getPolicy().isSpatial(),
        layout.getPolicy().getXExtent(), viewPort.row0, viewPort.row1);
  }

  function renderChips(chips, xMajor, xExtent, viewPortRow0, viewPortRow1) {
    var html = eval(Template(''));
    var colWidth = 100 / xExtent;
    var rowHeight = 100 / (viewPortRow1 - viewPortRow0);
    for (var i = 0, n = chips.length; i < n; ++i) {
      var c = chips[i];
      if (c.culled) { continue; }
      var col0 = c.col0, col1 = c.col1, row0 = c.row0, row1 = c.row1;
      if (xMajor) {
        col0 += c.slot / c.slotCount;
        col1 = col0 + c.slotExtent / c.slotCount;
      } else {
        row0 += c.slot / c.slotCount;
        row1 = row0 + c.slotExtent / c.slotCount;
      }
      var w = colWidth * (col1 - col0);
      var x = colWidth * col0;
      var h = rowHeight * (row1 - row0);
      var y = rowHeight * (row0 - viewPortRow0);
      html.append(eval(Template(
          '<span class=chip style="'
          + 'left: ${x}%; width:  ${w}%;'
          + 'top:  ${y}%; height: ${h}%'
          + '"><span class=body>${c.event.summary}</span></span>')));
    }
    return html;
  }

  return renderCalendar;
})();
