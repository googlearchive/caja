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
var caja___ = (function () {
  var cajaDomSuffix = 'g___';
  var grantAdditionalPowers = function(___, imports) {
    for (var tamer in tamings___) {
      if (tamings___.hasOwnProperty(tamer)) {
        tamings___[tamer].call(___.USELESS, ___, imports);
      }
    }
  }

  var uriPolicy = {
      rewrite: function (uri, mimeType) {
        if (!/^https?:\/\//i.test(uri)) { return null; }
        if (/^image[\/]/.test(mimeType)) { return uri; }
        return ('http://caja.appspot.com/cajole?url='
            + encodeURIComponent(uri)
            + '&mime-type=' + encodeURIComponent(mimeType));
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
    grantAdditionalPowers(___, imports);
    eval(js);
  }

  var cajoledJS = "";
  var policyJS = "";
  var currentFrame = null;
  function enableES53(parent, policy, html, js) {
    configureHTML(parent, html);
    
    var hiddenDiv = document.getElementById("es53frames");
    currentFrame = document.createElement('iframe');
    currentFrame.src = "es53.html?rnd=" + Math.floor(Math.random() * 10000);
    currentFrame.id = "es53frame";
    policyJS = policy;
    cajoledJS = js;
    hiddenDiv.appendChild(currentFrame);
  }
  
  function onReady(initJS, childFrame) {
    initJS(document.getElementById(id), uriPolicy, policyJS, cajoledJS,
        grantAdditionalPowers);
  }
  
  function tearDownES53() {
    try {
      document.body.removeChild(document.getElementById("es53frame"));
    } catch (e) {
      // failure is an option
    }    
  }
  
  function enable(es53, parent, policy, html, js) {
    tearDownES53();
    if (es53) {
      enableES53(parent, policy, html, js);
    } else {
      enableCajita(parent, policy, html, js);
    }
  }
  
  return {
    enable: enable,
    onReady: onReady,
    logFunc: function noOp() {}
  }
})();
