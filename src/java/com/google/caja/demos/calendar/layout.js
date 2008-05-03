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
 * Encapsulates a set of event chips positioned on a grid based on a layout
 * policy.
 *
 * @author mikesamuel@gmail.com
 */


/**
 * functions for laying out chips on a grid given a set of events to display.
 *
 * @param {LayoutPolicy} policy
 * @constructor
 */
function Layout(policy) {
  /** @type {LayoutPolicy} */
  this.policy_ = policy;
  /** @type {Array.<Chip>} */
  this.timedChips = null;
  /** @type {Array.<Chip>} */
  this.untimedChips = null;
  /** @type {number} */
  this.nAllDayRows = null;
}

Layout.prototype.getPolicy = function () { return this.policy_; };
Layout.prototype.getTimedChips = function () {
  return this.timedChips.slice(0);
};
Layout.prototype.getUntimedChips = function () {
  return this.untimedChips.slice(0);
};
Layout.prototype.getNAllDayRows = function () { return this.nAllDayRows; };

/**
 * Fills the lists of chips of events present in the given date range.
 * The start date is inclusive, while the end date is exclusive.
 * @param {number} startDate inclusive
 * @param {number} endDate exclusive
 * @param {Array.<VEvent>} events all events to be displayed.
 */
Layout.prototype.layout = function (startDate, endDate, events) {
  if (this.policy_.isSpatial()) {
    var dayEvents = [];
    var timedEvents = [];
    for (var i = 0; i < events.length; ++i) {
      var event = events[i];
      if (event.end <= startDate || event.start >= endDate) { continue; }
      (event.isOvernightEvent() ? dayEvents : timedEvents).push(event);
    }
    var dayChips = [],
      timedChips = [];
    this.generateChipList(startDate, endDate, dayEvents, dayChips);
    var dayRows = overlap.arrangeDayEvents(dayChips);
    this.generateChipList(startDate, endDate, timedEvents, timedChips);
    overlap.rearrangeStack(timedChips, Axis.X, 0, false);
    this.untimedChips = dayChips;
    this.timedChips = timedChips;
    this.nAllDayRows = dayRows;
  } else {
    var dayChips = [];
    this.generateChipList(startDate, endDate, events, dayChips);
    overlap.rearrangeStack(dayChips, Axis.Y, 1, true);
    this.timedChips = null;
    this.untimedChips = dayChips;
    this.nAllDayRows = 0;
  }
};

Layout.prototype.generateChipList = function (
    startDate, endDate, events, chips) {
  var isSpatial = this.policy_.isSpatial();
  var xExtent = this.policy_.getXExtent(),
      yExtent = this.policy_.getYExtent();

  // Figure out what's in the time range
  var pos = {};
  for (var i = 0; i < events.length; ++i) {
    var event = events[i];

    // Crop the event range to the display range so that we can come up with
    // proper start and end cells

    this.policy_.eventToCells(event, startDate, endDate, pos);
    if (pos.col0 === undefined) {
      pos.col0 = pos.row0 = 0;
    }
    if (pos.col1 === undefined) {
      pos.col1 = xExtent;
      if (isSpatial && event.isOvernightEvent()) {
        pos.row1 = 0;
      } else {
        pos.row1 = yExtent;
      }
    }
    var startsBeforeRange = (startDate > time.toDate(event.start));
    var endsAfterRange = (endDate < time.toDateOnOrAfter(event.end));
    this.generateChipsForEvent(event, pos, chips,
                               startsBeforeRange, endsAfterRange);
  }
};


/**
 * Generates the Chip (see cal.js) objects for an event.
 *
 * @param {Event} e the event
 * @param {Object} pos  { col0, row0, col1, row1 }
 * @param {Array.<Chip>} chips array to add new chips to.
 * @param {boolean} startsBeforeRange true if event starts before visible range.
 * @param {boolean} endsAfterRange true if event ends after visible range.
 */
Layout.prototype.generateChipsForEvent = function (
    e, pos, chips, startsBeforeRange, endsAfterRange) {
  // There are this.nRows * this.nCols blocks and each bloack is
  // this.nRowsPerDay rows high.
  // See the comments in DateToCell regarding the end row.
  var col0 = pos.col0, col1 = pos.col1;
  var row0 = pos.row0, row1;
  var firstChip = true;

  var xExtent = this.policy_.getXExtent(),
      yExtent = this.policy_.getYExtent(),
      nCols = this.policy_.getNCols(),
      isSpatial = this.policy_.isSpatial();

  if (isSpatial && !e.isOvernightEvent()) {
    for (var col = col0; col <= col1; ++col) {
      row1 = col < col1 ? yExtent : pos.row1;
      if (((row1 > row0) && (col < xExtent)) || (e.start === e.end)) {
        var chip = new Chip(e);
        chip.col0 = col;
        chip.col1 = col + 1;
        chip.row0 = row0;
        chip.row1 = row1;
        chip.first = firstChip;
        chip.last = false;
        chips.push(chip);
        firstChip = false;
      }
      row0 = 0;
    }
  } else {
    row1 = pos.row1;
    for (var row = row0; row <= row1; ++row) {
      // The chip extends to the end of the row
      // if we've still got more rows to go
      var colEnd = row === row1 ? col1 : nCols;
      // We may be hiding some columns on the right, such as weekends.
      colEnd = Math.min(xExtent, colEnd);
      if (col0 < colEnd) {
        // Determine how many days this is from the base
        var offsetDay = col0 + row0 * nCols;
        var chip = new Chip(e);
        chip.col0 = col0;
        chip.col1 = colEnd;
        chip.row0 = row;
        chip.row1 = row + 1;
        chip.first = firstChip;
        chip.last = false;
        chip.eventStartsBefore = startsBeforeRange || !firstChip;
        chip.eventEndsAfter = endsAfterRange;
        if (!firstChip) {
          chips[chips.length - 1].eventEndsAfter = true;
        }
        chips.push(chip);
        firstChip = false;
      }
      col0 = 0;
    }
  }
};

Layout.prototype.cullAt = function (slotsPerRow) {
  for (var i = this.untimedChips.length; --i >= 0;) {
    var ch = this.untimedChips[i];
    ch.culled = ch.slot >= slotsPerRow[ch.row0];
  }
};
