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

var rootContainerDiv = document.getElementById('container');
var rootChromeDiv = document.getElementById('chrome');

// The following variables are just an aid for debugging
var LASTDIV = undefined;
var LASTMOD = undefined;

var error = function(msg) {
  rootContainerDiv.innerHTML = '<pre>\n' + msg + '</pre>';
  throw 'Error';
};

var joinUrl = function(base, path) {
  while (base[base.length - 1] === '/') {
    base = base.slice(0, base.length - 1);
  }
  while (path[0] === '/') {
    path = path.slice(1, path.length);
  }
  return base + '/' + path;
};

// URL parameter parsing code from blog at:
// http://www.netlobo.com/url_query_string_javascript.html
var getUrlParam = function(name) {
  name = name.replace(/[\[]/,"\\\[").replace(/[\]]/,"\\\]");
  var regexS = "[\\?&]"+name+"=([^&#]*)";
  var regex = new RegExp(regexS);
  var results = regex.exec(window.location.href);
  return (results == null) ? "" : results[1];
};

var getUrlParamErr = function(name, err) {
  var result = getUrlParam(name);
  if (!result) { error(err); }
  return result;
};

var getUrlParamDefault = function(name, defaultValue) {
  var result = getUrlParam(name);
  if (!result) { return defaultValue; }
  return result;
};

var cajaServer = getUrlParamDefault('cajaServer', 'http://caja.appspot.com/');
var rootModule = getUrlParamDefault('rootModule', '../gadgets/gadgetsModule.html');

var debug = Boolean(getUrlParamDefault('debug', 'false'));

var defaultURIPolicy = {
  rewrite: function(uri, mimeType) {
    return uri;
  }
};

var log = function(str) {
  console.log('+++ ' + str);
};

var initialize = function (frameGroup) {

  var instantiateModule = function (feralContainerDiv,
                                    moduleUrl,
                                    extraOuters,
                                    resultCallback) {
    frameGroup.makeES5Frame(feralContainerDiv,
                            defaultURIPolicy,
                            function (frame) {
      frame.run(moduleUrl, extraOuters, function (result) {
        if (resultCallback) {
          resultCallback(result);
        }
      });
    });
  };

  var instantiateInTameElement = function(tameContainingElement,
                                          moduleUrl,
                                          extraOuters) {
    var feralContainingElement = tameContainingElement
        ? tameContainingElement.v___('ownerDocument')
            .feralNode___(tameContainingElement)
        : undefined;
    return instantiateModule(feralContainingElement, moduleUrl, extraOuters);
  };

  instantiateModule(undefined, 'photon.js', {
    instantiateInTameElement: frameGroup.tame(instantiateInTameElement),
    log: frameGroup.tame(log)
  }, function (photonInstance) {
    instantiateModule(rootContainerDiv, "bootstrap.html", {
      photonInstance: photonInstance,
      rootModule: rootModule
    });
  });
};

var loadingTimeout = window.setTimeout(function () {
  error('Cannot contact Caja server at "' + cajaServer + '"');
}, 1000);

var cajaScript = document.createElement('script');
cajaScript.setAttribute('src', cajaServer + '/caja.js');
cajaScript.onload = function () {
  window.clearTimeout(loadingTimeout);
  try {
    caja.configure({
      cajaServer: cajaServer,
      debug: debug
    }, initialize);
  } catch (e) {
    error(e);
  }
};

document.getElementsByTagName('head')[0].appendChild(cajaScript);
