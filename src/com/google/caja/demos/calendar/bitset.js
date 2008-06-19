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


/** @namespace */
var bitset = {};

/**
 * An auto-expanding set of bits.  Optimized for dense bitsets.
 *
 * Creates a BitSet with an optional initial capacity.
 *
 * @param {number} opt_nbits count of bits that we need to store.  The bitset is
 *   expandable, but if you know the size ahead of time it will preallocate.
 * @return an opaque bitset that can be passed to the other operations in this
 *   file.
 * @constructor
 * @author mikesamuel@gmail.com (Mike Samuel)
 */
bitset.makeBitSet = function (opt_nbits) {
  var bs = [];
  if (opt_nbits > 0) {
    for (var i = (opt_nbits + 31) >> 5; --i >= 0;) {
      bs[i] = 0;
    }
  }
  return bs;
};

/**
 * sets the b-th bit.
 * @param {Array} a bitset as returned by {@link #makeBitSet}.
 * @param {number} b a bit index.
 */
bitset.setBit = function (bs, b) {
  var word = b >> 5;
  while (word >= bs.length) { bs[bs.length] = 0; }
  bs[word] |= 1 << (b & 0x1f);
};

/** true iff the b-th bit is set. */
bitset.getBit = function (bs, b) {
  return (b >>> 5) < bs.length &&
      (bs[b >>> 5] & (1 << (b & 0x1f))) != 0;
};

/** the number of 32b words used by the given bitset. */
bitset.getNWords = function (bs) { return bs.length; };

/** clears a single bit. */
bitset.clearBit = function (bs, b) {
  var word = b >> 5;
  if (word < bs.length) {
    bs[word] &= ~(1 << (b & 0x1f));
  }
};

bitset.clearAll = function (bs) {
  for (var i = bs.length; --i >= 0;) { bs[i] = 0; }
};

bitset.toString = function (bs) {
  var s = '[BitSet';
  var chars = '0123456789abcdef';
  for (var word = 0; word < bs.length; word += 1) {
    var w = bs[word];
    s += ' ' + chars.charAt(w & 0xf) +
         chars.charAt((w >> 4) & 0xf) +
         chars.charAt((w >> 8) & 0xf) +
         chars.charAt((w >> 12) & 0xf) +
         chars.charAt((w >> 16) & 0xf) +
         chars.charAt((w >> 20) & 0xf) +
         chars.charAt((w >> 24) & 0xf) +
         chars.charAt((w >> 28) & 0xf);
  }
  return s + ']';
};

(function () {
  /** the index of the next clear bit in the bitset on or after bit b. */
  bitset.nextClearBit = function (bs, b) {
    var word = b >>> 5;
    if (word >= bs.length) { return b; }

    var bit = b & 0x1f;
    do {
      var w = ((~bs[word]) >>> bit) & 0xffffffff;
      if (w) { return (word << 5) + bit + nextSetBitInWord(w); }
      bit = 0;
    } while (++word < bs.length);
    return bs.length << 5;
  };

  /**
   * the index of the next set bit in the bitset on or after bit b or -1 if no
   * such bit.
   */
  bitset.nextSetBit = function (bs, b) {
    var word = b >>> 5;
    var bit = b & 0x1f;
    while (word < bs.length) {
      var w = bs[word] >>> bit;
      if (w) { return (word << 5) + bit + nextSetBitInWord(w); }
      bit = 0;
      ++word;
    }
    return -1;
  };

  /**
   * maps integers in [1, 255] to the index in [0, 7] of the first set bit.
   */
  var NSB_LOOKUP_TABLE = [];

  NSB_LOOKUP_TABLE[0] = -1;
  for (var i = 0; i < 8; i++) {
    var pow2 = 1 << i;
    for (var j = pow2; j < 256; j += pow2) { NSB_LOOKUP_TABLE[j] = i; }
  }

  function nextSetBitInWord(w) {
    return (w & 0xff) ? NSB_LOOKUP_TABLE[w & 0xff] :
        (w & 0xff00) ? 8 + NSB_LOOKUP_TABLE[(w >> 8) & 0xff] :
        (w & 0xff0000) ? 16 + NSB_LOOKUP_TABLE[(w >> 16) & 0xff] :
        (w & 0xff000000) ? 24 + NSB_LOOKUP_TABLE[(w >> 24) & 0xff] : -1;
  }
})();
