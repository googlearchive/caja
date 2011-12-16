// Copyright (C) 2011 Google Inc.
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
 * A module loader. This loader caches the eval()-ed code of modules.
 *
 * @param rootUrl the root URL of the root loader, against which the root-most
 *     module loading URLs will be resolved.
 * @param cajolingServiceClient a cajoling service client to use for contacting
 *     a cajoling service.
 *
 * @requires Q, ___
 * @provides loadModuleMaker
 */
var loadModuleMaker = function(rootUrl, cajolingServiceClient, uriUtils) {

  // A cache where each key is a fully qualified module URL and each value is
  // a promise for a prepared module object.
  var moduleCache = {};

  var evalModuleObjFromJson = function(moduleJson) {
    var moduleObj = undefined;
    (function() {
      var ___ = { loadModule: function(m) { moduleObj = m; } };
      eval(moduleJson.js);
    })();
    return moduleObj;
  };

  var resolveDependencies = function(moduleObj, loadForThisModule) {
    var result = Q.defer();
    if (moduleObj.includedModules !== undefined
        && moduleObj.includedModules.length !== 0) {
      var size = moduleObj.includedModules.length;
      var count = 0;
      for (var i = 0; i < size; i++) {
        Q.when(
            loadForThisModule.async(moduleObj.includedModules[i]),
            function(childModule) {
              count++;
              if (count === size) {
                result.resolve(true);
              }
            },
            function(err) {
              result.resolve(Q.reject(err));
            });
      }
    } else {
      result.resolve(true);
    }
    return result.promise;
  };

  var makeLoad = function(baseUrl) {
    var load = function(url) {
      var fullUrl = uriUtils.resolve(baseUrl, url);
      if (moduleCache['$' + fullUrl]
          || Q.near(moduleCache['$' + fullUrl]).isPromise___) {
        throw new Error(
            'The static module ' + fullUrl + ' cannot be resolved.');
      }
      return Q.near(moduleCache['$' + fullUrl]);
    };

    var evalAndResolveFromJson = function(url, moduleJson) {
      var result = Q.defer();
      var loadForThisModule = makeLoad(url);
      var moduleObj = evalModuleObjFromJson(moduleJson);
      Q.when(
          resolveDependencies(moduleObj, loadForThisModule),
          function(_) {
            try {
              result.resolve(
                  ___.prepareModule(moduleObj, loadForThisModule));
            } catch (ex) {
              result.resolve(Q.reject(ex));
            }
          },
          function(ex) {
            result.resolve(Q.reject(ex));
          });
      return result.promise();
    };

    var loadCajoledJson___ = function(url, moduleJson) {
      if (moduleCache['$' + url]) {
        throw new Error('Module already loaded: ' + url);
      }
      var moduleDeferred = Q.defer();
      Q.when(
          evalAndResolveFromJson(url, moduleJson),
          function(module) {
            moduleDeferred.resolve(module);
          },
          function(ex) {
            moduleDeferred.resolve(Q.reject(ex));
          });
      moduleCache['$' + url] = moduleDeferred.promise;
      return moduleDeferred.promise;
    };

    var async = function(url, contentType) {
      var fullUrl = uriUtils.resolve(baseUrl, url);
      if (moduleCache['$' + fullUrl]) {
        // Return the promise in the cache (may be as of yet unfulfilled)
        return moduleCache['$' + fullUrl];
      }
      var moduleDeferred = Q.defer();
      var mimeType = contentType || uriUtils.mimeTypeOf(url);
      Q.when(
          cajolingServiceClient.cajoleUrl(fullUrl, mimeType),
          function(moduleJson) {
            Q.when(
                evalAndResolveFromJson(fullUrl, moduleJson),
                function(module) {
                  moduleDeferred.resolve(module);
                },
                function(ex) {
                  moduleDeferred.resolve(Q.reject(ex));
                });
          },
          function(ex) {
            moduleDeferred.resolve(Q.reject(ex));
          });
      moduleCache['$' + fullUrl] = moduleDeferred.promise;
      return moduleDeferred.promise;
    };

    var asyncAll = function(moduleUrls, contentType) {
      var result = Q.defer();
      var i;
      var modulePromises = [];
      var modules = {};

      for (i = 0; i < moduleUrls.length; ++i) {
        modulePromises[i] = async(moduleUrls[i], contentType);
      }

      var waitNext = function(idx) {
        if (idx === moduleNames.length) {
          result.resolve(modules);
        } else {
          Q.when(modulePromises[idx], function(theModule) {
            modules[moduleNames[idx]] = theModule;
            waitNext(idx + 1);
          }, function(reason) {
            result.resolve(Q.reject(reason));
          });
        }
      };
      waitNext(0);
      return result.promise;
    };

    // We use direct exposure via markFuncFreeze, rather than the membrane
    // taming, because 'load' is a system function and it's far easier to
    // reason about how it is called and what it returns without having to
    // think about the semantics of a taming layer.

    load.DefineOwnProperty___('async', {
          value: ___.markFuncFreeze(async),
          writable: false,
          enumerable: true,
          configurable: false
        });
    load.DefineOwnProperty___('asyncAll', {
          value: ___.markFuncFreeze(asyncAll),
          writable: false,
          enumerable: true,
          configurable: false
        });
    load.loadCajoledJson___ = loadCajoledJson___;
    return ___.markFuncFreeze(load);
  };

  return makeLoad(rootUrl);
};
