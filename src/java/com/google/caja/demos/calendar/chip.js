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
 * A box on a calendar grid that represents an event.  An event may be
 * represented by multiple chips if e.g. it spans multiple days.
 *
 * @param {VEvent} e
 * @constructor
 * @author mikesamuel@gmail.com
 */
function Chip(e) {
  /**
   * the event this chip represents.
   * @type {number}
   */
  this.event = e;

  /**
   * is this the first chip for the event?
   * @type {boolean}
   */
  this.first = null;
  /**
   * is this the last chip for the event?
   * @type {boolean}
   */
  this.last = null;

  /**
   * does the event have a time associated with it?
   * @type {boolean}
   */
  this.timed = !time.isDate(e.start);

  /**
   * true iff the event's starts before the range this chip represents.
   * @type {boolean}
   */
  this.eventStartsBefore = null;
  /**
   * true iff the event's ends after the range this chip represents.
   * @type {boolean}
   */
  this.eventEndsAfter = null;

  /**
   * left column index, inclusive.
   * @type {number}
   */
  this.col0 = null;
  /**
   * right column index, exclusive.
   * @type {number}
   */
  this.col1 = null;
  /**
   * top row index, inclusive.
   * @type {number}
   */
  this.row0 = null;
  /**
   * bottom row index, exclusive.
   * @type {number}
   */
  this.row1 = null;

  /**
   * left pixel-space x-coordinate.
   * @type {number}
   */
  this.x = null;
  /**
   * top pixel-space x-coordinate.
   * @type {number}
   */
  this.y = null;
  /**
   * pixel-space x-extent.
   * @type {number}
   */
  this.width = null;
  /**
   * pixel-space y-extent.
   * @type {number}
   */
  this.height = null;

  /**
   * true iff this chip can't be displayed due to space constraints.
   * @type {boolean}
   */
  this.culled = null;

  /**
   * the index of the leftmost slot this chip occupies.
   * @type {number}
   */
  this.slot = null;
  /**
   * the number of slots that this chip occupies.
   * @type {number}
   */
  this.slotCount = null;
  /**
   * the number slots does this chip spans.
   * @type {number}
   */
  this.slotExtent = null;
};

Chip.prototype.toString = function () {
  return '[Chip ' + this.event.eid + ' ' + this.col0 + '+' + this.row0
      + ' - ' + this.col1 + '+' + this.row1 + ']';
};
