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

var LASTDIV = undefined;
var LASTMOD = undefined;

var error = function(msg) {
  rootContainerDiv.innerHTML = '<pre>\n' + msg + '</pre>';
  throw 'Error';
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

var cajaServer = getUrlParamErr('cajaServer',
    'Must specify a Caja server as:\n' +
    '    ...?cajaServer=<em>value</em>\n' +
    'where <em>value</em> is a URL like "http://caja.appspot.com/"\n');

var rootModule = getUrlParamErr('rootModule',
    'Must specify a root module URL as:\n' +
    '    ...?rootModule=<em>value</em>\n' +
    'where <em>value</em> is the URL of an HTML page\n');

var debug = Boolean(getUrlParamDefault('debug', 'false'));

var initializeRootModule = function(caja) {

  caja.Q.when(caja.hostTools.getLoad().async('photon.js'),
      function(photonModule) {

    var instantiateModule = function(
        feralContainerDiv, moduleUrl, extraOuters) {
      var sandbox = new caja.hostTools.Sandbox();
      sandbox.setURIPolicy({
        rewrite: function(uri, mimeType) {
	  return uri;
	}
      });
  
      feralContainerDiv.innerHTML = '';
      sandbox.attach(feralContainerDiv);
  
      if (extraOuters) {
        for (var p in extraOuters) {
          if (/.*___$/.test(p)) { continue; }
	  if (Object.prototype.hasOwnProperty.call(extraOuters, p)) {
            caja.cajita.setPub(sandbox.imports.outers, p, extraOuters[p]);
	  }
        }
      }
  
      return sandbox.run(moduleUrl);
    };
  
    var instantiateInTameElement =
        function(tameContainingElement, moduleUrl, extraOuters) {

      var feralInnerDiv = document.createElement('div');
      feralInnerDiv.setAttribute('class', 'innerHull');
      
      var feralOuterDiv = document.createElement('div');
      feralOuterDiv.setAttribute('class', 'outerHull');

      var feralContainingElement =
          caja.cajita.readPub(tameContainingElement, 'ownerDocument')
          .feralNode___(tameContainingElement);

      LASTDIV = feralContainingElement;
      LASTMOD = moduleUrl;

      feralContainingElement.innerHTML = '';
      feralContainingElement.appendChild(feralOuterDiv);

      feralOuterDiv.appendChild(feralInnerDiv);

      return instantiateModule(feralInnerDiv, moduleUrl, extraOuters);
    };
  
    var photonInstance = photonModule({ 
      instantiateInTameElement:
          caja.___.markFuncFreeze(instantiateInTameElement)
    });

    instantiateModule(rootContainerDiv, "bootstrap.html", {
      photonInstance: photonInstance,
      rootModule: rootModule
    });
  });
};

var loadingTimeout = window.setTimeout(function() {
  error('Cannot contact Caja server at "' + cajaServer + '"');
}, 1000);

var cajaScript = document.createElement('script');
cajaScript.setAttribute('src', cajaServer + 'caja.js');
cajaScript.onload = function() {
  window.clearTimeout(loadingTimeout);
  try {
    loadCaja(initializeRootModule, {
      cajaServer: cajaServer,
      debug: debug
    });
  } catch (e) {
    error(e);
  }
};

document.getElementsByTagName('head')[0].appendChild(cajaScript);
