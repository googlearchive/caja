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

/**
 * @author maoziqing@gmail.com
 * @provides serverJsSandboxMaker
 * 
 */
var serverJsSandboxMaker = (function(env, valijaMaker, load) {
  if (env.Q === undefined) {
    throw new Error('Include Q in env for serverJS sandbox modules.');
  }
  var exportsTable = cajita.newTable();
  
  function resolveModuleId(mid, src) {
    if (src.substring(src.length - 3) !== '.js') {
      src = src + '.js';
    }

    if (src[0] !== '.') {
      return src;
    }

    var k = mid.lastIndexOf("/");
    var newMid;
    if (k === -1) {
      newMid = src;
    } else {
      newMid = mid.substring(0, k + 1) + src;
    }
    
    while((k = newMid.indexOf("/./")) !== -1) {
      newMid = newMid.substring(0, k) + newMid.substring(k + 2);    	
    }
    
    while((k = newMid.indexOf("/../")) !== -1) {
      var p = newMid.lastIndexOf("/", k - 1);
      if (p === -1) {
        newMid = newMid.substring(k + 4);	
      } else {
        newMid = newMid.substring(0, p - 1) + newMid.substring(k + 4);
      }          
    }
    
    return newMid;
  }
  
  function requireMaker(mid) {
    function async(src) {
      var newMid = resolveModuleId(mid, src);
      var theExports = exportsTable.get(newMid);
      var Q = env.Q;
      var r = Q.defer();
      if (theExports !== void 0) {
        r.resolve(theExports);
      } else {
    	var require = requireMaker(newMid);
        var exports = {};
        exportsTable.set(newMid, exports);

        var valijaOuters = {env: env, require: require, exports: exports};
        var cajitaImports = cajita.freeze(
            {load: load, $v: valijaMaker(valijaOuters)});

        var m = load.async(newMid);
        Q.when(m, function(module) {
                    module(cajitaImports);
                    r.resolve(exports);
                  },
                  function(reason) {
                    r.resolve(Q.reject(
                        "Loading module " + newMid + "failed, " + reason));
                  });
      }
      return r.promise; 
    }
    
    var theRequire = {};
    theRequire.async = async;
    theRequire.moduleId = mid;
    return cajita.freeze(theRequire);
  }
  
  function loadModule(mid) {
    return requireMaker('').async(mid);
  }

  return cajita.freeze({ loadModule: loadModule });
});

if (typeof loader !== 'undefined') {
  loader.provide(serverJsSandboxMaker);
}
