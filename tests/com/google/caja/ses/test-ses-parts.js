// Copyright (C) 2013 Google Inc.
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

var loadSesScript = document.createElement('script');
loadSesScript.src = '../ses/initSES.js';
loadSesScript.onload = function() {
  readyToTest();
  jsunitRun();
};
document.body.appendChild(loadSesScript);

jsunitRegister('testOk', function() {
  assertEquals(false, ses.ok('MAGICAL_UNICORN'));
  assertEquals(true, ses.ok('NEW_SYMPTOM'));

  // Check behavior given invalid severity name
  expectFailure(function() {
    ses.ok('FOO');
  });

  jsunitPass();
});
