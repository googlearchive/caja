// Copyright (C) 2010 Google Inc.
//      
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

/**
 * @fileoverview Utilities for common patterns in host pages.
 *
 * @author kpreid@switchb.org
 * @requires document, ___, cajita, attachDocumentStub, valijaMaker,
 *           Q, defaultCajolerFinder, 
 * @provides HostTools
 *
 * (HostTools can also work without cajita-module, i.e. without
 * defaultCajolerFinder, at the cost of sandbox.run() functionality.)
 */

var HostTools;
(function () {
  var hasModuleLoader = "defaultCajolerFinder" in window;
  var toolsInstanceCounter = 0;
  
  HostTools = function () {
    var toolsInstanceIndex = ++toolsInstanceCounter;
    var gadgetInstanceCounter = 0;

    // user-modifiable state
    var cajolerFinder = hasModuleLoader ? defaultCajolerFinder : null;
    var baseURL = document.location.toString();
    // TODO(kpreid): the above probably does the wrong thing in the case where
    // the document has a <base>; fix.
    var load;
    
    // internal functions
    function updateLoad() {
      if (!hasModuleLoader) {
        load = null;
      } else {
        // TODO(kpreid): allow subbing module id resolver
        // TODO(kpreid): Using XHR loader didn't work; why?
        load = scriptModuleLoadMaker(baseURL,
                                     defaultModuleIdResolver,
                                     cajolerFinder);
      }
    }
    updateLoad();
    
    // public methods
    
    function setCajolerService(url) {
      cajolerFinder = new CajolingServiceFinder(url);
      updateLoad();
    }
    
    function setBaseURL(url) {
      baseURL = url;
      updateLoad();
    }
    
    function Sandbox() {
      // user-modifiable state
      var imports = ___.copy(___.sharedImports);
      
      // public methods
      
      function run(mid) {
        if (load == null) {
          throw new Error("HostTools: Loaded without cajita-module.js, so " +
              "cannot dynamically load modules.");
        }
        
        // TODO: review error handling -- are errors propagated and descriptive?
        return Q.when(load.async(mid), function (moduleFunc) {
          //console.log("got load Q callback");
          return moduleFunc(imports);
        });
      }
      
      function attach(vdocBody, options) {
        // Generate unique element id suffix for Domita.
        // There are two counters just to make them a little more decodable.
        var gadgetInstanceIndex = ++gadgetInstanceCounter;
        var idSuffix = "-HostToolsGadget-" + toolsInstanceIndex + "-" +
            gadgetInstanceIndex + "___";
        
        options = ___.copy(options ? options : {});
        if (options.valija === undefined) { options.valija = true; }
        
        // TODO(kpreid): do we want to reject multiple attach attempts?
        vdocBody.className = vdocBody.className + " vdoc-body___";
        
        if (options.valija) { imports.outers = imports; }
        
        // TODO(kpreid): provide control over this
        var uriCallback = cajita.freeze({
            rewrite: function() {
                return null;
            }
        });
        attachDocumentStub(idSuffix, uriCallback, imports, vdocBody);
        imports.htmlEmitter___ = new HtmlEmitter(vdocBody, imports.document);
        
        if (options.valija) { imports.$v = valijaMaker.CALL___(imports.outers); }
      }
      
      return cajita.freeze({
        attach: attach,
        imports: imports,
        run: run
      });
    }
    
    return cajita.freeze({
      getLoad: function () { return load; },
      setBaseURL: setBaseURL,
      setCajolerService: setCajolerService,
      Sandbox: Sandbox
    });
  }
})();
