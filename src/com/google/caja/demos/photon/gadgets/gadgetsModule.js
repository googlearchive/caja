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

// Array Remove - By John Resig (MIT Licensed)
var arrayRemove = function(that, from, to) {
  var rest = that.slice((to || from) + 1 || that.length);
  that.length = from < 0 ? that.length + from : from;
  return Array.prototype.push.apply(that, rest);
};

var $ = function(id) { return document.getElementById(id); }
$.create = function(tag) { return document.createElement(tag); }

var gadgets = [];

var makeGadgetDisplay = function(i) {
  var topDiv = $.create('div');
  topDiv.setAttribute('class', 'window');

  var header = $.create('div');
  topDiv.appendChild(header);
  header.setAttribute('class', 'header');

  var closeButton = $.create('div');
  header.appendChild(closeButton);
  closeButton.setAttribute('class', 'button');
  closeButton.innerHTML = '&#x2718;'
  closeButton.onclick = function() {
    arrayRemove(gadgets, i);
    layoutGadgets();
  };

  var label = $.create('span');
  header.appendChild(label);
  label.setAttribute('class', 'buttonLabel');
  label.innerHTML = gadgets[i].label;

  var body = $.create('div');
  topDiv.appendChild(body);
  body.setAttribute('class', 'contentBody');

  body.appendChild(gadgets[i].element);
  return topDiv;
};

var layoutGadgets = function() {
  $('contentPanel').innerHTML = '';

  var table = $.create('table');
  $('contentPanel').appendChild(table);
  table.setAttribute('class', 'contentTable');
  table.setAttribute('cellspacing', '0');
  table.setAttribute('cellpadding', '0');

  var i = 0;
  while (i < gadgets.length) {
    var tr = $.create('tr');
    table.appendChild(tr);
    tr.setAttribute('class', 'contentTr');

    for (var j = 0; j < 3 && i < gadgets.length; i++, j++) {
      var td = $.create('td');
      tr.appendChild(td);
      td.setAttribute('class', 'contentTd');

      td.appendChild(makeGadgetDisplay(i));
    }
  }
};

$('addGadget').onclick = function() {
  $('stationeryList').style.display = 'block';
};

$('addStationery').onclick = function() {
  $('addStationeryForm').style.display = 'block';
};

var addGadget = function(label, url) {
  var element = $.create('div');
  var instance = photon.instantiateInElement(element, url, { photon: photon });
  gadgets.push({ label: label, instance: instance, element: element });
  layoutGadgets();
};

var makeGadget = function(url) {
  $('addGadgetFormSubmit').onclick = function() {
    $('addGadgetForm').style.display = 'none';
    var label = $('addGadgetFormLabel').value;
    $('addGadgetFormLabel').value = '';
    addGadget(label, url);
  };
  $('addGadgetForm').style.display = 'block';
};

var addStationery = function(label, url) {
  var element = $.create('div');
  element.innerHTML = label;
  element.setAttribute('class', 'button');
  element.onclick = function() {
    $('stationeryList').style.display = 'none';
    makeGadget(url);
  };

  var tr = $.create('tr');
  $('stationeryList').appendChild(tr);

  var td = $.create('td');
  tr.appendChild(td);

  td.appendChild(element);
};

$('addStationeryFormSubmit').onclick = function() {
  $('addStationeryForm').style.display = 'none';
  addStationery(
      $('addStationeryFormLabel').value,
      $('addStationeryFormUrl').value);
  $('addStationeryFormLabel').value = '';
  $('addStationeryFormUrl').value = '';
};


addStationery('Bank', '/photon/gadgets/bank.html');
addStationery('Buyer', '/photon/gadgets/buyer.html');
addStationery('Seller', '/photon/gadgets/seller.html');

addGadget('A bank',  '/photon/gadgets/bank.html');
addGadget('A buyer', '/photon/gadgets/buyer.html');
addGadget('A seller', '/photon/gadgets/seller.html');
