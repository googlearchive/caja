// Use a quirk of es5 vs es53 to determine which mode we're really
// executing in

var imInES5 = false;
try {
  imInES5 = "object" === typeof undeclareVar;
} catch (e) {
  imInES5 = true;
}

assertTrue("Expected ES5 mode", imInES5);