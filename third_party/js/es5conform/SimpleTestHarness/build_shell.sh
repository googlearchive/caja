#!/bin/sh
echo "
load('SimpleTestHarness/sth.js')
load('SimpleTestHarness/sth_shell.js')

var aryTestCasePaths = [
  `find TestCases -name '*.js' | sed -e 's/^/"/' -e 's/$/",/'`
  ]
var ES5Harness = new sth(this);

ES5Harness.startTesting();
" > runtests.js
