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
 * A parser for http://microformats.org/wiki/hcalendar.
 * Describes how events are translated into boxes (chips) and how those
 * chips are laid out on a grid.
 *
 * @author mikesamuel@gmail.com
 */


/**
 * Defines a mapping from time to grid coordinates.
 * @param {number} nDays the number of days displayed.
 * @param {number} baseDate the date of the first day displayed.  A date value.
 * @param {number} skipCols the number of columns to skip.
 *     We may not render some columns, e.g. if the user prefers not to see
 *     weekends.
 * @constructor
 */
function LayoutPolicy(nDays, baseDate, skipCols) {
  /**
   * the number of days displayed.
   * @type {number}
   */
  this.nDays_ = nDays;

  /**
   * the date of the first day displayed.
   * @type {number}
   */
  this.baseDate_ = baseDate;

  /**
   * the number of columns to skip.  We may not render some columns, e.g. if the
   * user prefers not to see weekends.
   * @type {number}
   */
  this.skipCols_ = skipCols;

  // If the number of days displayed is less than a week then we have 1 row
  // with that many columns.  Otherwise we display one weeks worth of days per
  // row and pad the last row with blanks.

  /**
   * the number of columns.  We may display less than that by stripping columns
   * from the right, to e.g. omit weekends.
   * @type {number}
   */
  this.nCols_ = nDays > 7 ? 7 : nDays;

  /**
   * the number of visible columns.
   * We may display less than that by stripping columns
   * from the right, to e.g. omit weekends.
   * @type {number}
   */
  this.xExtent_ = this.nCols_ - this.skipCols_;

  /**
   * the number of days in each columns.
   * For spatial views, this is 1, and for other views it is the number of rows.
   * @type {number}
   */
  this.nDaysPerCol_ = Math.ceil(this.nDays_ / this.nCols_);

  /**
   * true iff events within a day are arranged spatially, so that events that
   * take 2 hours are twice as large as events that take one hour, and there
   * is a proportional gap between events.
   *
   * Day view is a spatial view, whereas month view is non-spatial.
   *
   * @type {boolean}
   */
  this.isSpatial_ = (this.nDaysPerCol_ === 1);

  /**
   * the y extent.
   * @type {number}
   */
  this.nRows_ = 0;

  // the following fields are only relevant if events within a day are arranged
  // spatially.
  /**
   * the number of rows per hour.
   * @type {number}
   */
  this.gradationsPerHour_ = 0;

  /**
   * number of rows per day.
   * @type {number}
   */
  this.nRowsPerDay_ = 0;

  if (this.isSpatial_) {
    this.gradationsPerHour_ = Math.max(1, 2 / this.nDaysPerCol_);
    this.nRowsPerDay_ = 24 * this.gradationsPerHour_;
    this.nRows_ = (24 * this.gradationsPerHour_) * this.nDaysPerCol_;
  } else {
    this.gradationsPerHour_ = (1./24);  // for consistency
    this.nRowsPerDay_ = 1;
    this.nRows_ = this.nDaysPerCol_;
  }
}

/**
 * true iff events within a day are arranged spatially, so that events that
 * take 2 hours are twice as large as events that take one hour, and there
 * is a proportional gap between events.
 *
 * Day view is a spatial view, whereas month view is non-spatial.
 *
 * @return {boolean}
 */
LayoutPolicy.prototype.isSpatial = function () { return this.isSpatial_; };

/**
 * The number of visible columns.
 * @return {number}
 */
LayoutPolicy.prototype.getXExtent = function () {
  return this.xExtent_;
};

/**
 * The number of actual columns.
 * @return {number}
 */
LayoutPolicy.prototype.getNCols = function () {
  return this.nCols_;
};

/**
 * The number of rows.
 * @return {number}
 */
LayoutPolicy.prototype.getYExtent = function () {
  return this.nRows_;
};

LayoutPolicy.prototype.getGradationsPerHour = function () {
  return this.gradationsPerHour_;
};

LayoutPolicy.prototype.dateToColumn = function (dateOrDateTime) {
  // equivalent to dateToCell(dateOrDateTime).col
  var daysBetween = time.daysBetween(dateOrDateTime, this.baseDate_);
  if (daysBetween >= 0 && daysBetween < this.nDays_) {
    return daysBetween % this.nCols_;
  } else if (daysBetween === this.nDays_
             && 0 === time.minuteInDay(dateOrDateTime)) {
    // Handle midnight of the last day displayed.
    return this.nCols_;
  }
  return -1;
};

LayoutPolicy.prototype.dateToRow = function (dateOrDateTime) {
  // equivalent to dateToCell(dateOrDateTime).row
  var daysBetween = time.daysBetween(dateOrDateTime, this.baseDate_);
  if (daysBetween >= 0 && daysBetween < this.nDays_) {
    var row = this.nRowsPerDay_ * Math.floor(daysBetween / this.nCols_);
    if (!time.isDate(dateOrDateTime)) {
      row += time.minuteInDay(dateOrDateTime) / 60 * this.gradationsPerHour_;
    }
    return row;
  } else if (daysBetween === this.nDays_
             && 0 === time.minuteInDay(dateOrDateTime)) {
    return 0;
  }
  return -1;
};

/**
 * Finds the cell associated with the specified date.
 *
 * @param {number} date date value whose cell is being requested
 * @return {Object} with int col, int row
 */
LayoutPolicy.prototype.dateToCell = function (dateOrDateTime) {
  var daysBetween = time.daysBetween(dateOrDateTime, this.baseDate_);
  if (daysBetween >= 0 && daysBetween < this.nDays_) {
    var col = daysBetween % this.nCols_;
    var row = this.nRowsPerDay_ * Math.floor(daysBetween / this.nCols_);
    if (!time.isDate(dateOrDateTime)) {
      row += time.minuteInDay(dateOrDateTime) / 60 * this.gradationsPerHour_;
    }
    return { col: col, row: row };
  } else if (daysBetween === this.nDays_
             && 0 === time.minuteInDay(dateOrDateTime)) {
    // Handle midnight of the last day displayed.
    return { col: this.nCols_, row: 0 };
  }
  return undefined;
};

/**
 * How discrete should we get when positioning dropped dates?
 * This is the number of intervals in a half hour that can be dropped on,
 * so 12 droppoints mean an event can be dropped on 5 minute intervals since
 * 60 / 12 = 5.
 * @type {number}
 */
LayoutPolicy.prototype.CG_DROPPOINTS_PER_HOUR = 12;

/**
 * Return the date for a given cell.
 * @param {number} col the column.  An integer, usually in
 *   <tt>[0, this.getXExtent() - 1]</tt>.
 * @param {number} row the column.  A real, usually in
 *   <tt>[0, this.getYExtent())</tt>.
 * @param {number} refDateTime the reference date-time.
 *   Where the grid doesn't allow fine grained scheduling, the output assumes
 *   fields from the reference date-time.
 *   For example, the month view doesn't allow discreteness of more than 1
 *   day, so if the reference date is Thursday at 9pm, then
 *   <tt>cellToDate</tt> will return a date-time with hours at 9pm.
 * @return {number} undefined or a date-time instance.
 */
LayoutPolicy.prototype.cellToDate = function (col, row, refDateTime) {
  if (this.isSpatial() && row > this.nRows_) {
    // 24:00 is 0:00
    row = 0;
  }

  // number of days between the date and the base date.
  var dayOffset = col;

  if (!this.isSpatial()) {
    // take extra weeks into account in month view
    // NOTE - in non-spatial mode this.nRowsPerDay is ALWAYS = 1, as such,
    // we do not need to do Math.floor(row / this.nRowsPerDay)
    // though I'll still do floor since row may not be an int
    dayOffset += Math.floor(row) * this.nCols_;
  }

  var hour, minute;
  if (this.isSpatial()) {
    hour = Math.floor(row / this.gradationsPerHour_);
    minute = Math.round(this.CG_DROPPOINTS_PER_HOUR *
                        ((row / this.gradationsPerHour_) | 0)) *
        (60 / this.CG_DROPPOINTS_PER_HOUR);
  } else if (refDateTime) {
    hour = time.hour(refDateTime);
    minute = time.minute(refDateTime);
  } else {
    hour = minute = 0;
  }

  return time.normalizedDateTime(
      time.year(this.baseDate_),
      time.month(this.baseDate_),
      time.day(this.baseDate_) + dayOffset,
      hour,
      minute);
};

/**
 * Determine the grid cells that will be covered by the portion of the given
 * event between startBound and endBound.
 *
 * @param {VEvent} event
 * @param {number} startBound lower date limit on which cells are included
 * @param {number} endBound upper date limit on which cells are included
 * @param {Array.<Object>} out filled with
 *    { col0: 0, row0: 0, col1: 0, row1: 0}.
 *    col0 and col1 are set to undefined if there is no start or end,
 *    respectively.
 */
LayoutPolicy.prototype.eventToCells
    = function (event, startBound, endBound, out) {

  // The start and end bounds checking ensure all cells returned have dates
  // between startBound and endBound. This was needed to accurately create
  // overlays for the more events popup window, which shows the events for one
  // day even though it is in the month view.
  var startDate = time.toDate(Math.max(startBound, event.start));
  var endDate = time.toDateOnOrAfter(Math.min(event.end, endBound));

  if (!this.isSpatial()) {
    var sIndex = time.daysBetween(startDate, this.baseDate_);
    if (sIndex < 0) { sIndex = 0; }

    var eIndex = time.daysBetween(endDate, this.baseDate_);
    if (eIndex > this.nDays_) { eIndex = this.nDays_; }

    var cols = this.nCols_;
    out.col0 = sIndex % cols;
    // NOTE - in non-spatial mode this.nRowsPerDay is ALWAYS = 1, as such,
    // we do not need to do ((this.nRowsPerDay * sIndex / this.nCols) | 0);
    out.row0 = (sIndex / cols) | 0;
    out.col1 = eIndex % cols;
    out.row1 = (eIndex / cols) | 0;
  } else if (event.isOvernightEvent()) {
    var sIndex = time.daysBetween(startDate, this.baseDate_);
    if (sIndex < 0) { sIndex = 0; }

    var eIndex = time.daysBetween(endDate, this.baseDate_);
    if (eIndex > this.nDays_) { eIndex = this.nDays_; }

    out.col0 = sIndex;
    out.row0 = 0;
    out.col1 = eIndex;
    out.row1 = 0;
  } else {
    var start = event.start;
    if (start < startDate) {
      start = time.isDate(start) ? start : time.toDateTime(startDate);
    }
    var end = event.end;
    if (end > endDate) {
      end = time.isDate(end) ? end : time.toDateTime(endDate);
    }

    var startColRow = this.dateToCell(start, false);
    var endColRow = this.dateToCell(end, false);

    if (startColRow) {
      out.col0 = startColRow.col;
      out.row0 = startColRow.row;
    } else {
      out.col0 = undefined;
    }

    if (endColRow) {
      out.col1 = endColRow.col;
      out.row1 = endColRow.row;
    } else {
      out.col1 = undefined;
    }
  }
};

LayoutPolicy.prototype.toString = function () {
  return '[LayoutPolicy ' + this.nDays_ + ' day(s) @ '
      + time.toIcal(this.baseDate_) + ']';
};
