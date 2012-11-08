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

caja.initialize({
  cajaServer: '/caja',
  debug: true,
  forceES5Mode: inES5Mode
});

var guestDiv = createDiv();

function createTestDiv() {
  var d = createDiv();
  d.setAttribute('testattr', 'testattrvaluereal');
  d.setAttribute('data-caja-testattr', 'testattrvaluevirt');
  d.setAttribute('title', 'testknownattrvalue');
  for (var i = 0; i < 2; i++) {
    var x = createDiv();
    x.setAttribute('class', 'testclass');
    x.innerHTML = "Div number " + i + " <span class='innertest'></span>";
    d.appendChild(x);
  }
  document.body.appendChild(d);
  return d;
}

caja.load(
    guestDiv,
    undefined,
    function(frame) {
      var extraImports = createExtraImportsForTesting(caja, frame);

      extraImports.getEmbeddedForeignNode = function() {
        var node = createTestDiv();
        guestDiv.firstChild.firstChild.appendChild(node);
        return frame.domicile.tameNodeAsForeign(node);
      };
      extraImports.getEmbeddedForeignNode.i___ =
          extraImports.getEmbeddedForeignNode;

      extraImports.getExternalForeignNode = function() {
        var node = createTestDiv();
        return frame.domicile.tameNodeAsForeign(node);
      };
      extraImports.getExternalForeignNode.i___ =
          extraImports.getExternalForeignNode;

      frame.code('es53-test-domado-foreign-guest.html')
           .api(extraImports)
           .run(function(result) {
                 readyToTest();
                 jsunitRun();
                 asyncRequirements.evaluate();
               });
});
