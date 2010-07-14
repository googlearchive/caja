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
  var grantAdditionalPowers = function(imports) {
    for (var tamer in tamings___) {
      if (tamings___.hasOwnProperty(tamer)) {
        tamings___[tamer].call(___.USELESS, imports);
      }
    }
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
              + '&mime-type=' + encodeURIComponent(mimeType));
      }
    };
    var gadgetRoot = document.getElementById(divElId);
    gadgetRoot.className += ' ' + divElId + '-' + cajaDomSuffix;

    attachDocumentStub('-' + divElId + '-' + cajaDomSuffix, uriCallback,
        imports, gadgetRoot);
    imports.htmlEmitter___ = new HtmlEmitter(gadgetRoot, imports.document);
    imports.$v = valijaMaker.CALL___(imports.outers);
    ___.getNewModuleHandler().setImports(imports);
    grantAdditionalPowers(imports);
  }

  return {
    enable: enable
  };
})();
