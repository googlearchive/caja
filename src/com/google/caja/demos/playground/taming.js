// Copyright (C) 2009 Google Inc.
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
 * @fileoverview
 * Tamed apis that are exposed to Caja playground.
 *
 * @author Jasvir Nagra (jasvir@gmail.com)
 * @requires document, window
 * @requires swfobject
 * @requires ___, attachDocumentStub, cajita
 * @provides caja___, tamings___
 */
var tamings___ = tamings___ || [];
var onReadyCallbacks___ = onReadyCallbacks___ || [];
var caja___ = (function () {
  var cajaDomSuffix = 'g___';
  var grantAdditionalPowers = function(tamings___, ___, imports) {
    for (var tamer = 0; tamer < tamings___.length; ++tamer) {
      tamings___[tamer].call(___.USELESS, ___, imports);
    }
  }

  var uriPolicy = {
      rewrite: function (uri, uriEffect, loaderType, hints) {
        if (!/^https?:\/\//i.test(uri)) { return void 0; }
        if (uriEffect === html4.ueffect.NEW_DOCUMENT ||
            (uriEffect === html4.ueffect.SAME_DOCUMENT &&
             loaderType === html4.ltype.SANDBOXED)) {
          return uri;
        }
        return null;
      }
  };

  var id = "cajoled-output" + '-' + cajaDomSuffix;
  function configureHTML(parent, html) {
    var gadgetRoot = document.createElement('div');
    gadgetRoot.id = id;
    gadgetRoot.className = id;
    gadgetRoot.innerHTML = html;
    parent.appendChild(gadgetRoot);
  }
  
  function enableCajita(parent, policy, html, js) {
    configureHTML(parent, html);
    var gadgetRoot = document.getElementById(id);
    var imports = ___.copy(___.sharedImports);
    imports.outers = imports;
    attachDocumentStub('-' + id, uriPolicy, imports, gadgetRoot);
    imports.htmlEmitter___ = new HtmlEmitter(gadgetRoot, imports.document);
    imports.$v = valijaMaker.CALL___(imports.outers);
    ___.setLogFunc(function(x) { caja___.logFunc(x); })
    ___.getNewModuleHandler().setImports(imports);
    eval(policy);
    grantAdditionalPowers(tamings___, ___, imports);
    eval(js);
  }

  var cachedImports;
  function reset() {
    cachedImports = undefined;
  }

  function enableES53(parent, policy, html, js, callback, cache) {
    configureHTML(parent, html);
    
    var hiddenDiv = document.getElementById("es53frames");
    var currentFrame = document.createElement('iframe');
    currentFrame.src = "/es53.html?rnd=" + Math.floor(Math.random() * 10000);
    currentFrame.id = "es53frame";

    onReadyCallbacks___.push(function(api, childFrame) {
        var result;
        if (cache) {
          if (!cachedImports) {
            cachedImports = api.configureImports(
              document.getElementById(id), uriPolicy, policy, grantAdditionalPowers);
          }
          result = api.run(cachedImports, {js : js});
        } else {
          result = api.initJS(document.getElementById(id), uriPolicy, policy,
              {js : js}, grantAdditionalPowers);
        }
        if ('function' == typeof callback) {
          callback(result);
        }
    });
    hiddenDiv.appendChild(currentFrame);
  }
  
  function tearDownES53() {
    try {
      document.body.removeChild(document.getElementById("es53frame"));
    } catch (e) {
      // failure is an option
    }    
  }
  
  function enable(es53, parent, policy, html, js, callback, cache) {
    tearDownES53();
    if (es53) {
      enableES53(parent, policy, html, js, callback, cache);
    } else {
      enableCajita(parent, policy, html, js, callback, cache);
    }
  }

  function onReady(api, frameElement) {
    var callback;
    for (callback = 0; callback < onReadyCallbacks___.length; callback++) {
      onReadyCallbacks___[callback](api, frameElement);
    }
    onReadyCallbacks___ = [];
  }

  return {
    reset: reset,
    enable: enable,
    onReady: onReady,
    logFunc: function noOp() {}
  }
})();
