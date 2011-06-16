// Copyright (C) 2011 Google Inc.
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

function fetch(url, callback) {
  var xhr = bridal.makeXhr();
  xhr.open('GET', url, true);
  xhr.onreadystatechange = function() {
    if (xhr.readyState === 4 && xhr.status === 200) {
      callback(xhr.responseText);
    }
  };
  xhr.send(null);
}

function initFrame(div, frameCallback) {
  caja.configure({
    cajaServer: 'http://localhost:8000/caja',
    debug: true
  }, function(frameGroup) {
    frameGroup.makeES5Frame(
        document.getElementById('untrusted_content'),
        {
          rewrite: function (uri) {
            return '[[' + uri + ']]';
          }
        },
        function(frame) { frameCallback(frameGroup, frame); });
  });
}

var idPattern = /^.*\-IDSUFFIX$/;

function rewriteIdSuffixes(node, idSuffix) {
  if (!node.getAttribute) { return; }
  var id = node.getAttribute('id');
  if (id && idPattern.test(id)) {
    node.setAttribute('id', id.replace('IDSUFFIX', idSuffix));
  }
  for (var n = node.firstChild; n; n = n.nextSibling) {
    rewriteIdSuffixes(n, idSuffix);
  }
}

var testDiv = document.createElement('div');
document.body.appendChild(testDiv);

fetch('es53-test-domita-special-initial-state.html', function(initialHtml) {
  testDiv.innerHTML = initialHtml;
  var virtualDoc = document.getElementById('untrusted_content');
  initFrame(virtualDoc, function(frameGroup, frame) {
    rewriteIdSuffixes(virtualDoc, frame.idSuffix);
    frame.url('es53-test-domita-special-guest.html')
         .run(createExtraImportsForTesting(frameGroup, frame),
             function(result) {
               readyToTest();
               jsunitRun();
               asyncRequirements.evaluate();
             });
     });
  });



