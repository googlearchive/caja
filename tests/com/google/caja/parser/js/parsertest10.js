"use strict,cajita";

function strictFn1() {
  "use strict";
  return;
}

function strictFn2() {
  'use strict,cajita';
  return;
}

function strictFn3() {
  "use strict, cajita";
  return;
}

function notStrictFn1() {
  "use strict,cajita"
  return;
}

function notStrictFn2() {
  ("use strict,cajita");
  return;
}

function notStrictFn3() {
  "use strict,cajita"
  + "foo";
  return;
}

function arbitraryBlocksCannotBeStrict() {
  { "use strict,cajita"; }
}

function malformed1() {
  "usestrict,cajita";
}

function unrecognizedSet1() {
  "use bogus";
}

function unrecognizedSet2() {
  "use strict,bogus";
}
