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

// By default, we point 'cajaServer' to the root URL of wherever this file
// is getting served from. This makes our Caja demo server (invoked via
// the 'ant runserver' command) work out of the box.
var cajaServer = getUrlParamDefault('cajaServer', '/');
var rootModule = getUrlParamDefault('rootModule',
    '../gadgets/gadgetsModule.html');

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

  // This function is not tamed because 'extraOuters' needs to be passed as
  // imports via privileged code to newly instantiated modules, and passing this
  // through an untame()/tame() cycle somehow breaks things.
  var instantiateInTameElement = function(tameContainingElement,
                                          moduleUrl,
                                          extraOuters) {
    var feralContainingElement = tameContainingElement
        ? tameContainingElement.FERAL_TWIN___
        : undefined;
    return instantiateModule(feralContainingElement, moduleUrl, extraOuters);
  };
  instantiateInTameElement.i___ = instantiateInTameElement;

  instantiateModule(undefined, 'photon.js', {
    instantiateInTameElement: instantiateInTameElement,
    log: frameGroup.tame(frameGroup.markFunction(log))
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
cajaScript.setAttribute('src', joinUrl(cajaServer, 'caja.js'));
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
