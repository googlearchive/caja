cd ..BROWSER USE
===========

Running the tests
-----------------
If this folder contains a file named runtests.html open it with the browser you want to test.
Otherwise, go to to folder SimpleTestHarness
at the cmd prompt run build.bat
Your tests should run in the browser.
Thats it.

Inspecting the results
----------------------
The test results are reported in the browser (Look for the testName that you gave for your test).
Thats it.


UNIX SHELL USE
================

Running the tests
-----------------

From a bash shell in this directory:
$ SimpleTestHarness/build_shell.sh # to build the runtests.js file
then
$ js runtest.js # to run the tests, where js is javascript shell you're testing
or
$ js runtests.js | grep Total # to see a summary of the test results
or
$ js runtests.js | grep -v PASS # to see only those tests which did not pass
