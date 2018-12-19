// Copyright (C) 2011 Google Inc.
//      
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

var hidden = {};

function toggleClick(target) {
  if (hidden[target]) {
    $('#' + target).show('blind', {}, 125);
    delete hidden[target];
  } else {
    $('#' + target).hide('blind', {}, 125);
    hidden[target] = 1;
  }
}

function addClickable(el) {
  var hc = el.getAttribute('hidecontrol');
  if (hc) {
    $(el).click(function() { toggleClick(hc); });
  }
}

function addClickables(el) {
  addClickable(el);
  for (var n = el.firstChild; n; n = n.nextSibling) {
    if (n instanceof Element) { addClickables(n); }
  }
}

function initialize() {
  prettyPrint();
  addClickables(document.body);
}