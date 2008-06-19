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
 * Functions for rearranging overlapping rectangles.
 *
 * @author mikesamuel@gmail.com
 */

/**
 * Rearranges overlapping rectangles so that they do not overlap by
 * shrinking them along their minor axis.
 *
 * <h3>Input</h3>
 * <p>We have a bunch of rectangles laid out on a 2-D page, and we he have a
 * major axis and a minor axis, so rectangles either left to right,
 * top to bottom, as in the Roman alphabet (majorAxis:Y);
 * or top to bottom, left to right, as in traditional Chinese (majorAxis:X).
 * The reading order corresponds to time-order when talking about events.
 * Week view is laid out as per Chinese reading order, and month view follows
 * Roman.
 *
 * <p>The goal is an arrangement of rectangles where no rectangles overlap,
 * the output rectangles span the same range in their minor axis, and a subset
 * of their range along the major axis.</p>
 *
 *
 * <h3>Assumptions</h3>
 * <p>The rectangles are already divided into disjoint regions along the
 * major axis, so there are no rectangles R1 and R2 such that R1 and R2 do
 * not have the same span along the major, and R1's and R2's spans intersect.
 * </p>
 *
 *
 * <h3>Algorithm</h3>
 * <p>This is achieved by the following algorithm which orders rectangles,
 * iteratively finds the minimum set of rectangles where each rectangle only
 * overlaps with others in the set, and then divides up the major axis so that
 * no rectangles overlaps others.</p>
 * <ol>
 *
 * <li>First we sort rectangles in reading order (see Roman/Chinese above).
 * <br>The sort is stable so that, below, earlier occuring items with the same
 * position in reading order receive the first slots making them more likely
 * to escape culling.
 *
 * <li>Until we've checked all rectangles
 *   <ol>
 *   <li>Pick the first rectangle in order that hasn't been resolved
 *
 *   <li>Add that to the set of rectangles to resolve.
 *
 *   <li>Walk from that point in the list of rectangles forward, adding
 *   rectangles until we encounter one that doesn't intersect any rectangle
 *   in the set.
 *
 *   <br>This is efficient because we know that we're traversing rectangles in
 *   "reading order", and that the rectangles are in disjoint "columns" along
 *   the minor axis.  These two assumptions mean that it's trivial to come up
 *   with a bounding box for the region which is the minor axis min of the
 *   first item added to the minor axis max of all elements in the set.
 *
 *   <li>Now that we know the set of overlapping rectangles, we can assign
 *   each rectangle to a subset of its "column", its span along the major
 *   axis.
 *
 *   <br>For each rectangle R in the overlapping set, ordered by reading order
 *     <ol>
 *     <li>If there are rectangles in slots that don't overlap R, mark their
 *     slot free.
 *     <li>Assign R to the first free slot, creating one if necessary
 *     <li>Mark the slot busy
 *     </ol>
 *
 *   <li>We now have an initial slot assignment, but there may be wasted
 *   space, as in the case
 *   <blockquote><pre>
 *   A B C
 *   A B C
 *       C
 *   D   C</pre><small><i>major axis is x</i></small></blockquote>
 *   D could expand to occupy two slots, using more of the available space.
 *
 *   <br>We again walk the list of overlapping rectangles, keeping track of
 *   free-slots in the same way as above, but for each slot, we also keep
 *   track of the position where it was last occupied so that we can tell when
 *   assigning a slot whether or not it could span slots to the right/down.
 *   <br>For each rectangle R in the overlapping set, ordered by reading order
 *     <ol>
 *     <li>If there are rectangles in slots that don't overlap R, mark their
 *     slot free, and store their endpoint (minor-axis max) in extent[slot].
 *     <li>If R was assigned slot S, assign it the end slot T, and mark S
 *     occupied.
 *     <li>Greedily expand into slots where there is space available.
 *     <tt>while (T < num_slots && free[T] && extent[T] < R's minor-axis min)
 *       T +=1</tt>
 *     </ol>
 *   This does not guarantee the maximum area covered, but is a good
 *   approximation in practice.
 *
 *   <li>Finally, we divvy the "column" into equally wide "sub-columns", and
 *   update each rectangles major axis bounds to span its slots.
 * </ol>
 *
 *
 * <h3>Output</h3>
 * <p>Since a rectangle may span multiple columns, the assumptions on input
 * do not apply to the output.</p>
 *
 *
 * @param {Array.<Rect>} rects objects with x, y, w, h attributes.  Modified
 *   in place.  x and y are upper left corners, while w and h are width and
 *   height respectively.  The rects must occupy discrete columns.
 * @param majorAxis the most significant axis along which time progresses.
 *   In week view, time progresses by hour along the y and days along the x,
 *   so x is the majorAxis.
 * @param {number} numReserved the number of reserved slots
 * @param {boolean} fixedSlotSize should chips be allowed to occupy more than
 *   1 slot?
 */
var overlap = (function () {
  function rearrangeStack(chips, majorAxis, numReserved, fixedSlotSize) {
    // Since this can be used to stack object in either rows (month view) or
    // columns (week view), figure out which axes we're dealing with by looking
    // at the majorAxis param.
    var minorAxis = Axis.X === majorAxis ? Axis.Y : Axis.X;

    // Sort chips by position such that chips[i] < chips[j] iff
    // majorAxis.position(chips[i]) < majorAxis.position(chips[j]) ||
    // (majorAxis.position(chips[i]) == majorAxis.position(chips[j]) &&
    //  (minorAxis.position(chips[i]) < minorAxis.position(chips[j]) ||
    //   (minorAxis.position(chips[i]) == minorAxis.position(chips[j]) &&
    //    minorAxis.extent(chips[i]) < minorAxis.extent(chips[j]))))
    chips.sort(Axis.X === majorAxis ? CHIP_COMPARATOR_X : CHIP_COMPARATOR_Y);

    var nChips = chips.length;
    for (var i = 0; i < nChips; ++i) {
      var end = -1; // -1 or the last chip overlapping chips[i]

      // The bottom point on the minor axis of the last event.
      var lastMinorEnd = minorAxis.position(chips[i]) +
                         minorAxis.extent(chips[i]);

      for (var j = i + 1; j < nChips; ++j) {
        var r = chips[j];
        if (majorAxis.position(r) !== majorAxis.position(chips[i])
            || lastMinorEnd <= minorAxis.position(r)) {
          break;
        }
        end = j;
        var rEnd = minorAxis.position(r) + minorAxis.extent(r);
        if (lastMinorEnd < rEnd) {
          // lastMinorEnd is used to check whether subsequent rectangles overlap
          // with any rectangles in [i, end].
          lastMinorEnd = rEnd;
        }
      }

      if (end >= 0) {
        var nOverlapping = end - i + 1;
        var nSlots = 0;
        // Upper bound on # slots.
        var slotUsage = bitset.makeBitSet(nOverlapping);
        // Slot assigned per overlapping rect.
        var assignedSlots = zeroes(nOverlapping);
        // True while a rectangle is using its slot.
        var occupying = bitset.makeBitSet(nOverlapping);

        for (var k = 0; k < nOverlapping; ++k) {
          var r = chips[i + k];

          // Free any slots no longer used.
          // nSlots is small enough that we probably won't benefit from using a
          // heap
          for (var m = -1; (m = bitset.nextSetBit(occupying, m + 1)) >= 0;) {
            var r2 = chips[i + m];
            if (minorAxis.position(r) >=
                minorAxis.position(r2) + minorAxis.extent(r2)) {
              // Free r2's slot.
              bitset.clearBit(occupying, m);
              bitset.clearBit(slotUsage, assignedSlots[m]);
            }
          }

          // Find a free slot.
          var slot = bitset.nextClearBit(slotUsage, numReserved);
          bitset.setBit(slotUsage, slot);
          assignedSlots[k] = slot;
          nSlots = Math.max(slot + 1, nSlots);

          bitset.setBit(occupying, k);
        }

        // Per overlapping rectangle, the number of extra slots wide it
        // occupies.
        var extraSlots = zeroes(nOverlapping);
        if (nSlots > 2
            // We do not expand when we have a fixed number of slots presumably
            // because all rectangles are supposed to have the same size.
            && !fixedSlotSize) {

          // Try to use any unclaimed space:
          // This may occur when you have the following situation
          // XY       XY
          // XYZ      XYZ
          // X Z      X Z
          //   Z  -->   Z
          // W Z      WWZ
          // W Z      WWZ
          // The space between W and Z is not being used, and W can be safely
          // expanded to fill two slots.

          bitset.clearAll(occupying);

          // Compute a contemporaries graph.
          // contemporaries[s + k * nSlots] is true iff slot s is used within
          // chips[k + i]'s range.
          var slotUser = [];

          var contemporaries = bitset.makeBitSet(nSlots * nOverlapping);
          for (var k = 0; k < nOverlapping; ++k) {
            var r = chips[i + k];
            // Free any slots no longer used.
            for (var m = -1; (m = bitset.nextSetBit(occupying, m + 1)) >= 0;) {
              var r2 = chips[i + m];
              if (minorAxis.position(r) >=
                  (minorAxis.position(r2) + minorAxis.extent(r2))) {
                // Free r2's slot.
                bitset.clearBit(occupying, m);
                slotUser[assignedSlots[m]] = 0;
              }
            }

            var slot = assignedSlots[k];
            slotUser[slot] = k + 1;
            bitset.setBit(occupying, k);

            // Update contemporaries.
            for (var s = 0; s < nSlots; ++s) {
              if (slotUser[s]) {
                bitset.setBit(contemporaries,
                              slot + (slotUser[s] - 1) * nSlots);
                bitset.setBit(contemporaries, s + k * nSlots);
              }
            }
          }

          // Widen where appropriate.  The slot assignment algorithm assigns
          // slots such that we'll never be able to expand left.  Since we don't
          // expand left, we can expand right without updating contemporaries
          // since we don't have to worry about expanding later rectangles left
          // into the space we just expanded right into.
          for (var k = 0; k < nOverlapping; ++k) {
            var slot = assignedSlots[k];
            while (++slot < nSlots) {
              if (bitset.getBit(contemporaries, slot + k * nSlots)) { break; }
            }
            extraSlots[k] = slot - assignedSlots[k] - 1;
          }
        }

        // Assign slots.
        for (var k = 0; k < nOverlapping; ++k) {
          var slot = assignedSlots[k];
          var r = chips[i + k];

          r.slot = slot;
          r.slotCount = nSlots;
          r.slotExtent = 1 + extraSlots[k];
        }

        // Skip overlapping items which have already been recomputed in master
        // loop.  The loop incrementer will go to end + 1.
        i = end;
      } else {
        // Resize if we've got a fixed number of slots.
        var r = chips[i];
        r.slot = numReserved;
        r.slotExtent = 1;
        r.slotCount = numReserved + 1;
      }
    }
  }

  /**
   * @param {Chip} a
   * @param {Chip} b
   * @return {number}
   */
  function CHIP_COMPARATOR_X(a, b) {
    // Order by column, then by position within the column, then by extent.
    return (a.col0 - b.col0) ||
           (a.row0 - b.row0) ||
           (b.row1 - a.row1) ||
           // Finally, to make the ordering total, order by eid.
           EVENT_COMPARATOR(a.event, b.event);
  }

  /**
   * @param {Chip} a
   * @param {Chip} b
   * @return {number}
   */
  function CHIP_COMPARATOR_Y(a, b) {
    return (a.row0 - b.row0) ||
           (a.col0 - b.col0) ||
           (a.timed !== b.timed
            // Order all day above timed events.
            ? (a.timed ? 1 : -1) :
            // Order by extent.
            (b.col1 - a.col1)) ||
           EVENT_COMPARATOR(a.event, b.event);
  }

  /**
   * @param {Chip} a
   * @param {Chip} b
   * @return {number}
   */
  function ALL_DAY_COMPARATOR(a, b) {
    var e = a.event,
        f = b.event;
    return (e.start - f.start || f.end - e.end) || EVENT_COMPARATOR(e, f);
  }

  /**
   * @param {Event} e
   * @param {Event} f
   * @return {number}
   */
  function EVENT_COMPARATOR(e, f) {
    var c = e.calendarId,
        d = f.calendarId;
    if (c !== d) {
      return (c < d) ? -1 : 1;
    } else {
      c = e.eid;
      d = f.eid;
      return (c < d) ? -1 : (c !== d) ? 1 : 0;
    }
  }

  /**
   * arrange day events in a timed view.
   * <p>The day events appear in a separate section above the weekly view, that
   * is dynamically sized depending on how many day events appear in the
   * range specified.
   * @param {Array.<Chip>} chips an array of chips, each of which has its
   *   {@link Chip#row0},
   *   {@link Chip#row1}, {@link Chip#slot}, {@link Chip#slotCount}, and
   *   {@link Chip#slotExtent} fields set.
   * @return {number} the number of rows required to display them all.
   */
  function arrangeDayEvents(chips) {
    if (0 === chips.length) { return 0; }

    // arranges day events to minimize usage of space.
    // this is similar to overlap, but creates new rows in the all day grid
    // instead of jamming them into the existing all day row.
    chips.sort(ALL_DAY_COMPARATOR);

    var rows = [];
    var rowUsage = bitset.makeBitSet(0);
    var lastColumn = chips[0].col0;
    var numRows = 0;
    for (var i = 0; i < chips.length; ++i) {
      var chip = chips[i];

      // Clear empty rows (if we're further along than the previous chip)
      if (lastColumn < chip.col0) {
        lastColumn = chip.col0;
        for (var m = -1; (m = bitset.nextSetBit(rowUsage, m + 1)) >= 0;) {
          if (rows[m].col1 <= lastColumn) { bitset.clearBit(rowUsage, m); }
        }
      }

      // Get index of first empty row
      var row = bitset.nextClearBit(rowUsage, 0);

      // Place the chip in the row
      rows[row] = chip;
      bitset.setBit(rowUsage, row);
      chip.row1 = (chip.row0 = row) + 1;
      chip.slot = 0;
      chip.slotCount = chip.slotExtent = 1;

      // Track how many rows we've used
      if (row >= numRows) { numRows = row + 1; }
    }

    return numRows;
  }

  function zeroes(n) {
    var out = [];
    while (--n >= 0) { out[n] = 0; }
    return out;
  }

  return {
    arrangeDayEvents: arrangeDayEvents,
    rearrangeStack: rearrangeStack
  };
})();
