"use strict"; "use cajita";

function strictFn1() {
  "use strict";
  return;
}

function strictFn2() {
  'use strict'; 'use cajita';
  return;
}

function strictFn3() {
  "use strict"
  "use cajita"
  return;
}

function strictFn4() {
  "use strict"  // comment
  "use cajita"
  return;
}

function strictFn5() {
  "use strict"  /* comment */
  "use cajita"
  return;
}

function strictFn6() {
  "use strict"
  "use cajita"
}

function notStrictFn1() {
  ("use strict");
  return;
}

function notStrictFn2() {
  "use strict"
  + "foo";
  return;
}

function arbitraryBlocksCannotBeStrict() {
  { "use strict"; "use cajita"; }
}

function malformedOkayWithWarning() {
  "bogusburps";
}
