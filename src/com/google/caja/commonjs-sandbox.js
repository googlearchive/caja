// Copyright (C) 2009 Google Inc.
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

'use strict';
'use cajita';

/**
 * @author maoziqing@gmail.com
 * @provides commonJsSandboxMaker
 * 
 */
var commonJsSandboxMaker = (function(env, valijaMaker) {
  if (env.Q === undefined) {
    throw new Error('Include Q in env for serverJS sandbox modules.');
  }
  var exportsTable = cajita.newTable();

  function requireMaker(mid) {
    function theRequire(securedModule) {
      var theExports = exportsTable.get(securedModule.moduleId);
      if (theExports !== void 0) {
        return theExports;
      } else {
        var require = requireMaker(securedModule.moduleId);
        var exports = {};
        exportsTable.set(newMid, exports);

        var valijaOuters = {env: env, require: require, exports: exports};
        var cajitaImports = cajita.freeze({$v: valijaMaker(valijaOuters)});
        securedModule(cajitaImports);
        return exports;
      }
    }

    function async(m) {
      var Q = env.Q;
      var r = Q.defer();
      Q.when(m, function(module) {
                  var theExports = exportsTable.get(module.moduleId);
                  if (theExports !== void 0) {
                    r.resolve(theExports);
                  } else {
        	        var require = requireMaker(module.moduleId);
                    var exports = {};
                    exportsTable.set(module.moduleId, exports);

                    var valijaOuters
                        = {env: env, require: require, exports: exports};
                    var cajitaImports
                        = cajita.freeze({$v: valijaMaker(valijaOuters)});
                    module(cajitaImports);
                    r.resolve(exports);
                  }
                },
                function(reason) {
                  r.resolve(Q.reject(
                      "Loading module " + src + " failed, " + reason));
                });
      return r.promise; 
    }
    
    theRequire.async = async;
    theRequire.moduleId = mid;
    return cajita.freeze(theRequire);
  }
  
  function loadModule(securedModulePromise) {
    return requireMaker('').async(securedModulePromise);
  }

  return cajita.freeze({ loadModule: loadModule });
});

if (typeof loader !== 'undefined') {
  loader.provide(commonJsSandboxMaker);
}

// If this module is called with the new-style Cajita module convention,
// passing 'env', 'valijaModule' and 'load' as Cajita-level parameters, the
// value returned from instantiating the module should be the fully constructed
// ServerJS sandbox object.
if (typeof env !== 'undefined'
    && typeof valijaModule !== 'undefined') {
  commonJsSandboxMaker(env, function(valijaOuters) {
    return valijaModule({ outers: valijaOuters });
  });
}
