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

var ses;

(function() {

  ses.logger = console;

  function getText(url, cb) {
    var request = new XMLHttpRequest();
    request.onreadystatechange = function() {
      if (request.readyState === 4) {
        if (request.status === 200) {
          cb(request.responseText);
        }
      }
    };
    request.open('GET', url, true);
    request.send();
  }

  function time(name, f) {
    var d0 = new Date();
    f();
    var d1 = new Date();
    var delta = d1.getTime() - d0.getTime();
    console.log(name + ': ' + delta + ' ms');
    return delta;
  }

  function runTest(name, ses, src) {
    console.log('=== Running ' + name + ' ===');
    var mitigated;
    var tmg = time(name + ' - mitigate gotchas', function() {
      var options = ses.resolveOptions({
        rewriteTopLevelVars: true,
        rewriteTopLevelFuncs: true,
        rewriteTypeof: true
      });
      mitigated = ses.mitigateSrcGotchas(false, src, options, ses.logger);
    });
    var parsed;
    var tp = time(name + ' - parse', function() {
      parsed = ses.rewriter_.parse(src);
    });
    var generated;
    var tr = time(name + ' - render', function() {
      generated = ses.rewriter_.generate(parsed);
    });
    console.log(name + ' - delta mg = ' + (tmg - (tp + tr)) + ' ms');
    console.log(name + ' - src.length = ' + src.length);
    console.log(name + ' - mitigated.length = ' + mitigated.length);
  }

  caja.initialize({
    server: 'http://localhost:8080',
    maxAcceptableSeverity: 'NEW_SYMPTOM',
    debug: true
  });
  caja.load(undefined, caja.policy.net.ALL, function(frame) {
    getText('http://code.jquery.com/jquery-1.8.2.js', function(src) {
      runTest(
          'jQuery 1.8.2 direct functions',
          window.ses,
          '// DIRF\n' + src);
      runTest(
          'jQuery 1.8.2 from SES frame',
          frame.iframe.contentWindow.ses,
          '// SESF\n' + src);
    });
  });

  /*
  getText('./third_party/js/jqueryjs/dist/jquery.js', runTest);
  getText('./third_party/js/jqueryjs/dist/jquery.min.js', runTest);
  getText('http://code.jquery.com/ui/1.8.23/jquery-ui.js', runTest);
  getText('http://code.jquery.com/jquery-1.8.2.js', runTest);
  */

})();
