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
 * (HostTools can also work without cajita-module, i.e. without
 * scriptModuleLoadMaker, at the cost of sandbox.run() functionality.)
 *
 * @author kpreid@switchb.org
 * @requires eval, window, document, ___, cajita, attachDocumentStub,
 *           valijaMaker, Q,
 *           scriptModuleLoadMaker, defaultModuleIdResolver,
 *           CajolingServiceFinder,
 *           HtmlEmitter
 * @provides HostTools
 */

var HostTools;
(function () {
  var hasModuleLoader = "scriptModuleLoadMaker" in window;
  var toolsInstanceCounter = 0;
  
  HostTools = function () {
    var toolsInstanceIndex = ++toolsInstanceCounter;
    var gadgetInstanceCounter = 0;

    // user-modifiable state
    var cajolingService = "http://caja.appspot.com/cajole";
    var baseURL = document.location.toString();
    // TODO(kpreid): the above probably does the wrong thing in the case where
    // the document has a <base>; fix.
    
    // cache
    var load;
    
    // internal functions
    function updateLoad() {
      if (!hasModuleLoader) {
        load = null;
      } else {
        // TODO(kpreid): allow subbing module id resolver
        // TODO(kpreid): Using XHR loader didn't work; why?
        load = scriptModuleLoadMaker(
          baseURL,
          defaultModuleIdResolver,
          new CajolingServiceFinder(cajolingService));
      }
    }
    updateLoad();
    
    // public methods
    
    function setCajolerService(url) {
      cajolingService = url;
      updateLoad();
    }
    
    function setBaseURL(url) {
      baseURL = url;
      updateLoad();
    }
    
    function Sandbox() {
      var attached = false;
      
      // user-modifiable state
      var imports = ___.copy(___.sharedImports);
      var uriPolicy = cajita.freeze({
        rewrite: function (uri, mimeType) {
          // TODO(kpreid): This needs to be redefined in terms of effect/loader;
          // we don't necessarily know the actual specific mime type before
          // making the request.
          return cajolingService +
              '?url=' + encodeURIComponent(uri) +
              '&input-mime-type=' + encodeURIComponent(mimeType) +
              '&output-mime-type=' + encodeURIComponent(mimeType);
          
        }
      });
  
      // public methods
      
      function setURIPolicy(newPolicy) {
        if (attached) {
          throw("setURIPolicy() must be used before attach()");
        }
        uriPolicy = newPolicy;
      }

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
      
      function runCajoledModuleString(js) {
        // This is primarily useful when used from caja.js (Caja in an iframe),
        // since the host page doesn't have eval of the frame

        var pumpkin = {};
        var moduleFunc = pumpkin;
        var oldModuleHandler = ___.getNewModuleHandler();
        var newModuleHandler = ___.makeNormalNewModuleHandler();
        newModuleHandler.handle = ___.markFuncFreeze(
            function theHandler(module) {
              // TODO: does not support dependencies. Needs to tie in to
              // cajita-module.js for that and to give the module a proper
              // relative load function.
              moduleFunc = ___.prepareModule(module, load);
            });
        try {
          ___.setNewModuleHandler(newModuleHandler);
          eval(js);
        } finally {
          ___.setNewModuleHandler(oldModuleHandler);
        }
        if (moduleFunc === pumpkin) {
          throw new Error("runCajoledModuleString: the provided code did not " +
            "invoke the new module handler");
        }
        return moduleFunc(imports);
      }
      
      function attach(vdocBody, options) {
        attached = true;

        // Generate unique element id suffix for Domita.
        // There are two counters just to make them a little more decodable.
        var gadgetInstanceIndex = ++gadgetInstanceCounter;
        var idSuffix = "-HostToolsGadget-" + toolsInstanceIndex + "-" +
            gadgetInstanceIndex + "___";
        
        options = ___.copy(options ? options : {});
        if (options.valija === undefined) { options.valija = true; }
        
        // TODO(kpreid): do we want to reject multiple attach attempts?
        
        if (options.valija) { imports.outers = imports; }
        
        attachDocumentStub(idSuffix, uriPolicy, imports, vdocBody);
        imports.htmlEmitter___ = new HtmlEmitter(vdocBody, imports.document);
        
        if (options.valija) { imports.$v = valijaMaker.CALL___(imports.outers); }
      }
      
      return cajita.freeze({
        attach: attach,
        setURIPolicy: setURIPolicy,
        imports: imports,
        run: run,
        runCajoledModuleString: runCajoledModuleString
      });
    }
    
    return cajita.freeze({
      getLoad: function () { return load; },
      setBaseURL: setBaseURL,
      setCajolerService: setCajolerService,
      Sandbox: Sandbox
    });
  };
})();
