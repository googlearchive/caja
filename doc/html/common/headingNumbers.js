// Copyright (C) 2008 Google Inc.
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

/**
 * @author ihab.awad@gmail.com
 */

function applyNumberToElement(element, numbers) {
  element.innerHTML = numbers.join('.') + '&nbsp;&nbsp;' + element.innerHTML;
}

function numberDocument() {
  var numbers = [];

  for (var i = 0; i < document.body.childNodes.length; i++) {

    var node = document.body.childNodes[i];

    if (!node.tagName) continue;
    var headingLevel = node.tagName.match(/[Hh]([0-9])/);
    if (!headingLevel) continue;
    headingLevel = headingLevel[1];

    while (numbers.length < headingLevel) { numbers.push(0); }
    while (headingLevel < numbers.length) { numbers.pop(); }

    numbers[numbers.length - 1]++;
    applyNumberToElement(node, numbers);
  }
}
