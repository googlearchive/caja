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
 * @provides caja___
 */
var caja___ = (function () {
    var cajaDomSuffix = 'g___';

  // Simple flash taming that does not allow script access to the page
  function tameSimpleFlash(imports) {
    imports.outers.swfobject = {};
    imports.outers.swfobject.embedSWF = function(swfUrl, id, width, height, 
        version, expressInstall, flashvars, params, attributes, cb) {
      var tameSwfUrl = !/^https?:\/\//i.test(swfUrl) ? null : swfUrl;
      var tameId = id + '-cajoled-output-' + cajaDomSuffix;
      var tameWidth = +width;
      var tameHeight = +height;
      // Default to 9.0 if unspecified or specified < 9
      // else use whatever variant of version 9 the user suggests
      var tameVersion = version || "9.0";
      if (!/^9|([1-9][0-9])\./.test(tameVersion)) {
        tameVersion = "9.0";
      }
      var tameExpressInstall = false;
      var tameParams = { "allowScriptAccess" : "never", 
                         "allowNetworking" : "internal"};
      // TODO(jasvir): rewrite attributes
      var tameAttr = null;
      swfobject.embedSWF(tameSwfUrl, tameId, tameWidth, tameHeight, tameVersion,
          tameExpressInstall, flashvars, tameParams, tameAttr, ___.untame(cb));
    };
    ___.grantRead(imports.outers, 'swfobject');
    ___.grantFunc(imports.outers.swfobject, 'embedSWF');
  }

  function tameFlash(imports) {
    tameSimpleFlash(imports);
  }


  function tameAlert(imports) {
    imports.outers.alert = (function() {
      var remainingAlerts = 10;
      var useConsole = false;
      function tameAlert(msg) {
        if (useConsole) {
          cajita.log(msg);
        } else {
          if (remainingAlerts > 0) {
            remainingAlerts--;
            alert(msg);
          } else {
            if (confirm("Redirect remaining alerts to console?")) {
              useConsole = true;
            } else {
              remainingAlerts = 10;
            }
          }
        }
      };
      return tameAlert;
    })();
    ___.grantFunc(imports.outers, 'alert');
  }
  
  function enable(divElId) {
    divElId = divElId || 'cajoled-output';
    var imports = ___.copy(___.sharedImports);
    imports.outers = imports;
    var uriCallback = {
      rewrite: function (uri, mimeType) {
          if (!/^https?:\/\//i.test(uri)) { return null; }
          if (/^image[/]/.test(mimeType)) { return uri; }
          return ('http://caja.appspot.com/cajole?url='
              + encodeURIComponent(uri)
              + '&mimeType=' + encodeURIComponent(mimeType));
      }
    };
    var gadgetRoot = document.getElementById(divElId);
    gadgetRoot.className = cajaDomSuffix;
    
    attachDocumentStub('-' + divElId + '-' + cajaDomSuffix, uriCallback,
        imports, gadgetRoot);
    imports.htmlEmitter___ = new HtmlEmitter(gadgetRoot, imports.document);
    imports.$v = valijaMaker.CALL___(imports.outers);
    ___.getNewModuleHandler().setImports(imports);
    
    tameAlert(imports);
    tameFlash(imports);
  }

  return {
    enable: enable  
  };
})();
