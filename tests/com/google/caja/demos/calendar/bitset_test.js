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

jsunitRegister('testEmptyBitSet', function testEmptyBitSet() {
  var bs = bitset.makeBitSet(0);
  assertFalse(bitset.getBit(bs, 0));
  assertFalse(bitset.getBit(bs, 1));
  assertFalse(bitset.getBit(bs, 2));
  assertFalse(bitset.getBit(bs, 31));
  assertFalse(bitset.getBit(bs, 32));
  assertFalse(bitset.getBit(bs, 33));
  assertEquals(-1, bitset.nextSetBit(bs, 0));
  assertEquals(-1, bitset.nextSetBit(bs, 1));
  assertEquals(-1, bitset.nextSetBit(bs, 2));
  assertEquals(-1, bitset.nextSetBit(bs, 31));
  assertEquals(-1, bitset.nextSetBit(bs, 32));
  assertEquals(-1, bitset.nextSetBit(bs, 33));
  assertEquals(0, bitset.nextClearBit(bs, 0));
  assertEquals(1, bitset.nextClearBit(bs, 1));
  assertEquals(2, bitset.nextClearBit(bs, 2));
  assertEquals(31, bitset.nextClearBit(bs, 31));
  assertEquals(32, bitset.nextClearBit(bs, 32));
  assertEquals(33, bitset.nextClearBit(bs, 33));
  assertEquals('[BitSet]', bitset.toString(bs));
});

jsunitRegister(
    'testPreallocedEmptyBitSet', function testPreallocedEmptyBitSet() {
  var bs = bitset.makeBitSet(1);
  assertFalse(bitset.getBit(bs, 0));
  assertFalse(bitset.getBit(bs, 1));
  assertFalse(bitset.getBit(bs, 2));
  assertFalse(bitset.getBit(bs, 31));
  assertFalse(bitset.getBit(bs, 32));
  assertFalse(bitset.getBit(bs, 33));
  assertEquals(-1, bitset.nextSetBit(bs, 0));
  assertEquals(-1, bitset.nextSetBit(bs, 1));
  assertEquals(-1, bitset.nextSetBit(bs, 2));
  assertEquals(-1, bitset.nextSetBit(bs, 31));
  assertEquals(-1, bitset.nextSetBit(bs, 32));
  assertEquals(-1, bitset.nextSetBit(bs, 33));
  assertEquals(0, bitset.nextClearBit(bs, 0));
  assertEquals(1, bitset.nextClearBit(bs, 1));
  assertEquals(2, bitset.nextClearBit(bs, 2));
  assertEquals(31, bitset.nextClearBit(bs, 31));
  assertEquals(32, bitset.nextClearBit(bs, 32));
  assertEquals(33, bitset.nextClearBit(bs, 33));
  assertEquals('[BitSet 00000000]', bitset.toString(bs));
});

jsunitRegister('testSparseBitSet', function testSparseBitSet() {
  var bs = bitset.makeBitSet(16);
  // setup bitset
  //          LSB
  // Word 1 : .1.. .... .... ...1 | .... .... .... .... ||
  // Word 2 : .... .... .... ...1 | .... .... .... ..1.
  bitset.setBit(bs, 47);
  bitset.setBit(bs, 1);
  bitset.setBit(bs, 62);
  bitset.setBit(bs, 15);
  // check that only the set bits are set.
  for (var i = 0; i <= 256; ++i) {
    assertEquals(i === 1 || i === 15 || i === 47 || i === 62,
                 bitset.getBit(bs, i));
  }
  // check nextSetBit
  assertEquals(1, bitset.nextSetBit(bs, 0));
  assertEquals(1, bitset.nextSetBit(bs, 1));
  assertEquals(15, bitset.nextSetBit(bs, 2));
  assertEquals(15, bitset.nextSetBit(bs, 3));
  assertEquals(15, bitset.nextSetBit(bs, 14));
  assertEquals(15, bitset.nextSetBit(bs, 15));
  assertEquals(47, bitset.nextSetBit(bs, 16));
  assertEquals(47, bitset.nextSetBit(bs, 17));
  assertEquals(47, bitset.nextSetBit(bs, 31));
  assertEquals(47, bitset.nextSetBit(bs, 32));
  assertEquals(47, bitset.nextSetBit(bs, 33));
  assertEquals(47, bitset.nextSetBit(bs, 46));
  assertEquals(47, bitset.nextSetBit(bs, 47));
  assertEquals(62, bitset.nextSetBit(bs, 48));
  assertEquals(62, bitset.nextSetBit(bs, 49));
  assertEquals(62, bitset.nextSetBit(bs, 50));
  assertEquals(62, bitset.nextSetBit(bs, 61));
  assertEquals(62, bitset.nextSetBit(bs, 62));
  assertEquals(-1, bitset.nextSetBit(bs, 63));
  assertEquals(-1, bitset.nextSetBit(bs, 64));
  assertEquals(-1, bitset.nextSetBit(bs, 10000));

  // check nextClearBit
  assertEquals(0, bitset.nextClearBit(bs, 0));
  assertEquals(2, bitset.nextClearBit(bs, 1));
  assertEquals(2, bitset.nextClearBit(bs, 2));
  assertEquals(3, bitset.nextClearBit(bs, 3));
  assertEquals(14, bitset.nextClearBit(bs, 14));
  assertEquals(16, bitset.nextClearBit(bs, 15));
  assertEquals(16, bitset.nextClearBit(bs, 16));
  assertEquals(17, bitset.nextClearBit(bs, 17));
  assertEquals(31, bitset.nextClearBit(bs, 31));
  assertEquals(32, bitset.nextClearBit(bs, 32));
  assertEquals(33, bitset.nextClearBit(bs, 33));
  assertEquals(46, bitset.nextClearBit(bs, 46));
  assertEquals(48, bitset.nextClearBit(bs, 47));
  assertEquals(48, bitset.nextClearBit(bs, 48));
  assertEquals(49, bitset.nextClearBit(bs, 49));
  assertEquals(50, bitset.nextClearBit(bs, 50));
  assertEquals(61, bitset.nextClearBit(bs, 61));
  assertEquals(63, bitset.nextClearBit(bs, 62));
  assertEquals(63, bitset.nextClearBit(bs, 63));
  assertEquals(64, bitset.nextClearBit(bs, 64));
  assertEquals(10000, bitset.nextClearBit(bs, 10000));

  // check idioms for iterating over bits
  var bits;
  var b;
  for (b = -1, bits = []; (b = bitset.nextSetBit(bs, b + 1)) >= 0;) {
    bits.push(b);
  }
  assertEquals('1,15,47,62', bits.join(','));

  bits = [];
  for (b = -1, bits = [];
       (b = bitset.nextClearBit(bs, b + 1)) >= 0 && b <= 20;) {
    bits.push(b);
  }
  assertEquals('0,2,3,4,5,6,7,8,9,10,11,12,13,14,16,17,18,19,20',
               bits.join(','));

  // clear a bit
  bitset.clearBit(bs, 15);
  bitset.clearBit(bs, 16);  // already cleared

  for (b = -1, bits = []; (b = bitset.nextSetBit(bs, b + 1)) >= 0;) {
    bits.push(b);
  }
  assertEquals('1,47,62', bits.join(','));

  bits = [];
  for (b = -1, bits = [];
       (b = bitset.nextClearBit(bs, b + 1)) >= 0 && b <= 20;) {
    bits.push(b);
  }
  assertEquals('0,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20',
               bits.join(','));

  assertEquals('[BitSet 20000000 00080004]', bitset.toString(bs));
  bitset.clearAll(bs);
  assertEquals(-1, bitset.nextSetBit(bs, 0));
  assertEquals('[BitSet 00000000 00000000]', bitset.toString(bs));
});

jsunitRegister('testDenseBitSet', function testDenseBitSet() {
  var bs = bitset.makeBitSet(16);
  // setup bitset
  //          LSB
  // Word 1 : .111 11.. 1111 .111 | 1111 1111 1111 1111 ||
  // Word 2 : 1111 1..1 1..1 ...1 | .... .... .... ....

  // e3feffff f9980000
  var setBits = [ 1,2,3,4,5, 8,9,10,11, 13,14,15,
                  16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,
                  32,33,34,35,36, 39,40, 43, 47];
  for (var b = 0; b < setBits.length; ++b) {
    bitset.setBit(bs, setBits[b]);
  }
  // check nextSetBit
  assertEquals(1, bitset.nextSetBit(bs, 0));
  assertEquals(1, bitset.nextSetBit(bs, 1));
  assertEquals(2, bitset.nextSetBit(bs, 2));
  assertEquals(3, bitset.nextSetBit(bs, 3));
  assertEquals(14, bitset.nextSetBit(bs, 14));
  assertEquals(15, bitset.nextSetBit(bs, 15));
  assertEquals(16, bitset.nextSetBit(bs, 16));
  assertEquals(17, bitset.nextSetBit(bs, 17));
  assertEquals(31, bitset.nextSetBit(bs, 31));
  assertEquals(32, bitset.nextSetBit(bs, 32));
  assertEquals(33, bitset.nextSetBit(bs, 33));
  assertEquals(47, bitset.nextSetBit(bs, 46));
  assertEquals(47, bitset.nextSetBit(bs, 47));
  assertEquals(-1, bitset.nextSetBit(bs, 48));
  assertEquals(-1, bitset.nextSetBit(bs, 49));
  assertEquals(-1, bitset.nextSetBit(bs, 50));
  assertEquals(-1, bitset.nextSetBit(bs, 61));
  assertEquals(-1, bitset.nextSetBit(bs, 62));
  assertEquals(-1, bitset.nextSetBit(bs, 63));
  assertEquals(-1, bitset.nextSetBit(bs, 64));
  assertEquals(-1, bitset.nextSetBit(bs, 10000));

  // check nextClearBit
  assertEquals(0, bitset.nextClearBit(bs, 0));
  assertEquals(6, bitset.nextClearBit(bs, 1));
  assertEquals(6, bitset.nextClearBit(bs, 2));
  assertEquals(6, bitset.nextClearBit(bs, 3));
  assertEquals('ncb 14', 37, bitset.nextClearBit(bs, 14));
  assertEquals('ncb 15', 37, bitset.nextClearBit(bs, 15));
  assertEquals('ncb 16', 37, bitset.nextClearBit(bs, 16));
  assertEquals('ncb 17', 37, bitset.nextClearBit(bs, 17));
  assertEquals('ncb 31', 37, bitset.nextClearBit(bs, 31));
  assertEquals('ncb 32', 37, bitset.nextClearBit(bs, 32));
  assertEquals('ncb 33', 37, bitset.nextClearBit(bs, 33));
  assertEquals(46, bitset.nextClearBit(bs, 46));
  assertEquals(48, bitset.nextClearBit(bs, 47));
  assertEquals(48, bitset.nextClearBit(bs, 48));
  assertEquals(49, bitset.nextClearBit(bs, 49));
  assertEquals(50, bitset.nextClearBit(bs, 50));
  assertEquals(61, bitset.nextClearBit(bs, 61));
  assertEquals(62, bitset.nextClearBit(bs, 62));
  assertEquals(63, bitset.nextClearBit(bs, 63));
  assertEquals(64, bitset.nextClearBit(bs, 64));
  assertEquals(10000, bitset.nextClearBit(bs, 10000));

  // check idioms for iterating over bits
  var bits;
  var b;
  for (b = -1, bits = []; (b = bitset.nextSetBit(bs, b + 1)) >= 0;) {
    bits.push(b);
  }
  assertEquals(setBits.join(','), bits.join(','));

  bits = [];
  for (b = -1, bits = [];
       (b = bitset.nextClearBit(bs, b + 1)) >= 0 && b < 50;) {
    bits.push(b);
  }
  assertEquals(bs, '0,6,7,12,37,38,41,42,44,45,46,48,49', bits.join(','));

  // set a bit
  bitset.setBit(bs, 0);
  bitset.setBit(bs, 1);  // already set

  for (b = -1, bits = []; (b = bitset.nextSetBit(bs, b + 1)) >= 0;) {
    bits.push(b);
  }
  assertEquals('0,' + setBits.join(','), bits.join(','));

  bits = [];
  for (b = -1, bits = [];
       (b = bitset.nextClearBit(bs, b + 1)) >= 0 && b < 50;) {
    bits.push(b);
  }
  assertEquals(bs, '6,7,12,37,38,41,42,44,45,46,48,49', bits.join(','));

  bitset.clearAll(bs);
  assertEquals(-1, bitset.nextSetBit(bs, 0));
  assertEquals('[BitSet 00000000 00000000]', bitset.toString(bs));
});
