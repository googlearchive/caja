// Copyright (C) 2012 Google Inc.
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

var currentTestName = undefined;
var divId = 0;

function newChartDiv(opt_height) {
  var container = document.createElement('div');
  var label = document.createElement('div');
  var content = document.createElement('div');

  document.getElementById('chartdiv').appendChild(container);
  container.appendChild(label);
  container.appendChild(content);

  container.style.marginTop = '10px';

  label.innerHTML = currentTestName;
  label.style.margin = '5px';
  label.style.padding = '5px';
  label.style.fontFamily = 'Courier';
  label.style.fontWeight = 'bold';

  content.style.width = '600px';
  if (opt_height) { content.style.height = opt_height; }
  content.style.border = '1px solid black';
  content.style.backgroundColor = '#c3e3e3';
  content.style.margin = '5px';
  content.style.padding = '5px';
  content.setAttribute('id', 'chartDiv-' + divId++);

  if (window.isStandalone) {
    // attempt to duplicate the "inner opaque <div>" arrangement
    // of the Caja-sandboxed case
    var innerContent = document.createElement('div');
    content.appendChild(innerContent);
    innerContent.style.width = content.style.width;
    innerContent.style.height = content.style.height;
    innerContent.setAttribute('id', content.getAttribute('id') + '-inner');
    return innerContent;
  } else {
    return content;
  }
}

function assertTrue(cond, msg) {
  if (!cond) {
    log('<font color="orange">expected true but was false:' +
        ' (' + msg + ')</font>');
  }
}

function assertEquals(a, b, msg) {
  if (a !== b) {
    log('<font color="orange">expected \u00ab' + a + '\u00bb' +
        ' but found \u00ab' + b + '\u00bb (' + msg + ')</font>');
  }
}

google.load('picker', '1.0');

google.setOnLoadCallback(function() {
  runtests();
});

function runtests() {
  for (var i = 0; i < tests.length; i++) {
//    if (!/testGeoChart.*/.test(tests[i].name)) { continue; }
    if (currentTestName) {
      throw 'Test name expected empty; found ' + currentTestName;
    }
    currentTestName = tests[i].name;
    try {
      log('<strong>start test ' + tests[i].name + '</strong>');
      tests[i]();
      log('<font color="green">test ' + tests[i].name + ' done without throwing</font>');
    } catch (e) {
      log('<font color="red">test ' + tests[i].name + ' threw ' + e + ' - ' + e.stack + '</font>');
    } finally {
      currentTestName = undefined;
    }
  }
}

var tests = [];

tests.push(function testPicker() {
  // Create and render a Picker object for searching images.
  function createPicker() {
    var ppp = new google.picker.PickerBuilder();

debugger;

    var picker = new google.picker.PickerBuilder()
          .addView(google.picker.ViewId.DOCS)
          .addView(google.picker.ViewId.DOCS_IMAGES)
          .addView(google.picker.ViewId.DOCS_IMAGES_AND_VIDEOS)
          .addView(google.picker.ViewId.DOCS_VIDEOS)
          .addView(google.picker.ViewId.DOCUMENTS)
          .addView(google.picker.ViewId.FOLDERS)
          .addView(google.picker.ViewId.FORMS)
          .addView(google.picker.ViewId.IMAGE_SEARCH)
          .addView(google.picker.ViewId.PDFS)
          .addView(google.picker.ViewId.PHOTO_ALBUMS)
          .addView(google.picker.ViewId.PHOTO_UPLOAD)
          .addView(google.picker.ViewId.PHOTOS)
          .addView(google.picker.ViewId.PRESENTATIONS)
          .addView(google.picker.ViewId.RECENTLY_PICKED)
          .addView(google.picker.ViewId.SPREADSHEETS)
          .addView(google.picker.ViewId.VIDEO_SEARCH)
          .addView(google.picker.ViewId.WEBCAM)
          .addView(google.picker.ViewId.YOUTUBE)
          .setCallback(pickerCallback)
          .build();
    picker.setVisible(true);
  }

  // A simple callback implementation.
  function pickerCallback(data) {
    var url = 'nothing';
    if (data[google.picker.Response.ACTION] == google.picker.Action.PICKED) {
      var doc = data[google.picker.Response.DOCUMENTS][0];
      url = doc[google.picker.Document.URL];
    }
    log('You picked: ' + url);
  }

  google.load('picker', '1', {callback: createPicker});
});
