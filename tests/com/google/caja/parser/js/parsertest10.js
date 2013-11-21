"use strict"; "use strict";

function strictFn1() {
  "use strict";
  return;
}

function strictFn2() {
  'use strict'; 'use strict';
  return;
}

function strictFn3() {
  "use strict"
  "use strict"
  return;
}

function strictFn4() {
  "use strict"  // comment
  "use strict"
  return;
}

function strictFn5() {
  "use strict"  /* comment */
  "use strict"
  return;
}

function strictFn6() {
  "use strict"
  "use strict"
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
  { "use strict"; "use strict"; }
}

function malformedOkayWithWarning() {
  "bogusburps";
}

function directiveCannotHaveEscape() {
  "use\x20strict";
}

function directiveCannotHaveContinuation() {
  "use \
strict";
}
