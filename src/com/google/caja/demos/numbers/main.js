// Copyright (C) 2010 Google Inc.
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

var log = function(s) {
  console.log(s);
};

var addGadgetDiv = function(name) {
  var td = document.createElement('td');
  td.style.verticalAlign = 'top';
  var title = document.createElement('h3');
  title.innerHTML = name;
  var outerDiv = document.createElement('div');
  document.getElementById('gadgets').appendChild(td);
  td.appendChild(title);
  td.appendChild(outerDiv);
  return outerDiv;
};

var defaultUriPolicy = {
  rewrite: function(uri, mimeType) {
    return undefined;
  }
};

var tame = function(frameGroup, api) {
  frameGroup.markReadOnlyRecord(api);
  frameGroup.markFunction(api.getSelection);
  frameGroup.markFunction(api.setSelection);
  frameGroup.markFunction(api.addSelectionListener);
  frameGroup.markReadOnlyRecord(api.data);
  frameGroup.markFunction(api.data.getNumRows);
  frameGroup.markFunction(api.data.getNumCols);
  frameGroup.markFunction(api.data.getColHeader);
  frameGroup.markFunction(api.data.get);
  return frameGroup.tame(api);
};

var loadGadget = function(frameGroup, tameApi, name, url) {
  var extraOuters = {
    api: tameApi,
    log: frameGroup.tame(frameGroup.markFunction(function(s) {
      log(name + ' (' + url + ') : ' + String(s));
    }))
  };

  frameGroup.makeES5Frame(addGadgetDiv(name), defaultUriPolicy, function (frame) {
    frame.url(url).run(extraOuters, function (result) { 
      log('Gadget ' + name + ' (' + url + ') loaded');
    });
  });
};

caja.configure({
  cajaServer: 'http://localhost:8080/',
  debug: true
}, function(frameGroup) {
  var tameApi = tame(frameGroup, api /* from api.js */);
  loadGadget(frameGroup, tameApi, 'Prudhoe Bay 2002', './table.html');
  loadGadget(frameGroup, tameApi, 'Prudhoe Bay 2002', './graph.html');
});
