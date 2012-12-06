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

// URL parameter parsing code from blog at:
// http://www.netlobo.com/url_query_string_javascript.html
function getUrlParam(name) {
  name = name.replace(/[\[]/,"\\\[").replace(/[\]]/,"\\\]");
  var regexS = "[\\?&]"+name+"=([^&#]*)";
  var regex = new RegExp(regexS);
  var results = regex.exec(window.location.href);
  return decodeURIComponent((results == null) ? "" : results[1]);
}

function fetch(url, mime, cb) {
  var r = new XMLHttpRequest();
  r.open("GET", url, true);
  r.onreadystatechange = function() {
    if (r.readyState === 4) {
      if (r.status === 200) {
        cb(r.responseText);
      } else {
        console.log('Fetch failed: ' + url + ' had status ' + r.status);
      }
    }
  };
  r.send(null);
}

function getTests(cb) {
  var test = getUrlParam('test');
  if (test) {
    setTimeout(function() { cb([ test ]); }, 0);
  } else {
    fetch(getUrlParam('testsList'), 'application/json', function(testsText){
      cb(JSON.parse(testsText));
    });
  }
}

function getCajaServer() {
  return getUrlParam('cajaServer') || 'http://localhost:8080';
}

function resolve(url) {
  var a = document.createElement('a');
  a.href = url;
  return '' + a.href;
}

function loadScript(src, cb) {
  if (src instanceof Array) {
    if (src.length === 0) {
      cb();
    } else {
      loadScript(src[0], function() {
        loadScript(src.slice(1), cb);
      });
    }
  } else {
    var script = document.createElement('script');
    script.setAttribute('src', src);
    script.onload = cb;
    document.head.appendChild(script);
  }
}

