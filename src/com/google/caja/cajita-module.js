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
 * @author maoziqing@gmail.com, kpreid@switchb.org
 * @requires ___, bridal, Q, URI
 * @provides xhrModuleLoadMaker, scriptModuleLoadMaker, clearModuleCache,
 *           defaultModuleIdResolver, defaultCajolerFinder,
 *           CajolingServiceFinder
 * To obtain the dependencies of this file, load:
 *   cajita.js, bridal.js, uri.js, cajita-promise.js
 *
 * Each load maker object, given the absolute URL of the current module, an
 * identifier resolver, and a cajoler finder, returns a load object.
 *
 * A load object is a function object; load() returns a module object,
 * given its module identifier.
 * load.async() returns a promise to the module, given the module identifier.
 * 
 * What a module identifier is is entirely up to the identifier resolver. The
 * identifier resolver is a function of two parameters, (current module 
 * absolute URL, module identifier), returning the absolute URL for the
 * identified module. The default module identifier resolver considers the
 * module identifier to be a relative URL.
 * 
 * The cajoler finder is a function which should take an absolute module URL
 * and return a URL for cajoled code to load as if by a <script> element. (It
 * need not point to an actual cajoling service and could instead use static
 * .out.js files, depending on the application.)
 *
 * Note that this system never actually fetches the module absolute URL, only
 * passes it to the cajoler. But it *is* used as a key in the cache of loaded
 * modules, so a module absolute URL should always have the same module.
 *
 * TODO(kpreid): explain static (sync) loading module id semantics.
 */
var xhrModuleLoadMaker;
var scriptModuleLoadMaker;
var defaultModuleIdResolver;
var defaultCajolerFinder;
var CajolingServiceFinder;
var clearModuleCache;

(function() {
  // Map from absolute module URLs to module objects.
  var cache = {};

  defaultModuleIdResolver = function(thisModURL, mid) {
    return URI.resolve(URI.parse(thisModURL), URI.parse(mid)).toString();
  };
  
  /**
   * Constructor for a cajoler finder given the URL of a cajoling service.
   */
  CajolingServiceFinder = function(serviceURL) {
    function cajolingServiceFinder(uncajoledSourceURL) {
      var inputMimeType;
      if (/\.js$/.test(uncajoledSourceURL)) {
        inputMimeType = 'application/javascript';
      } else if (/\.html$/.test(uncajoledSourceURL)) {
        inputMimeType = 'text/html';
      } else {
        inputMimeType = 'application/javascript';
      }

      return serviceURL +
          '?url=' + encodeURIComponent(uncajoledSourceURL) +
          '&input-mime-type=' + inputMimeType +
          '&output-mime-type=application/javascript';
    }
    return cajolingServiceFinder;
  };
  defaultCajolerFinder = new CajolingServiceFinder(
      'http://caja.appspot.com/cajole');

  function syncLoad(modURL) {
    if (cache[modURL] === undefined || Q.near(cache[modURL]).isPromise___) {
      throw new Error("The static module " + modURL + " cannot be resolved.");
    }
    return Q.near(cache[modURL]);
  }

  function loadMaker(thisModURL, midResolver, cajolerFinder, asyncLoad) {
    var load = function(mid) {
      return syncLoad(midResolver(thisModURL, mid));
    };

    var async = function(mid) {
      return asyncLoad(midResolver(thisModURL, mid),
                       midResolver, cajolerFinder);
    };

    var asyncAll = function(moduleNames) {
      var r = Q.defer();
      var i;
      var modulePromises = [];
      var modules = {};

      for (i = 0; i < moduleNames.length; ++i) {
        modulePromises[i] = async(moduleNames[i]);
      }

      var waitNext = function(idx) {
        if (idx === moduleNames.length) {
          r.resolve(modules);
        } else {
          Q.when( modulePromises[idx], function(theModule) {
            modules[moduleNames[idx]] = theModule;
            waitNext(idx + 1);
          }, function(reason) {
            r.resolve(Q.reject(reason));
          });
        }
      };
      waitNext(0);
      return r.promise();
    };

    load.FUNC___ = 'load';
    ___.setStatic(load, 'async', ___.markFuncFreeze(async));
    ___.setStatic(load, 'asyncAll', ___.markFuncFreeze(asyncAll));
    return ___.primFreeze(load);
  }

  function resolveDependency(module, load) {
    var r = Q.defer();
    if (module.includedModules !== undefined
        && module.includedModules.length !== 0) {
      var size = module.includedModules.length;
      var count = 0;
      for (var i = 0; i < size; i++) {
        var mid = module.includedModules[i];
        var m = load.async(mid);
        Q.when(m, function(childModule) {
                    count++;
                    if (count === size) {
                      r.resolve(true);
                    }
                  },
                  function(reason) {
                    r.resolve(Q.reject(
                        "Retrieving the module " + mid + " failed."));
                  });
      }
    } else {
      r.resolve(true);
    }
    return r.promise;
  }

  function noop() {}
  
  /** 
   * Given a method of async loading, produce the load-maker that the client
   * will use.
   */
  function makeConcreteLoadMaker(asyncLoadFunction) {
    return ___.markFuncFreeze(function(mid, midResolver, cajolerFinder) {
      if (midResolver === undefined) {
        midResolver = defaultModuleIdResolver;
      }
      if (cajolerFinder === undefined) {
        cajolerFinder = defaultCajolerFinder;
      }

      return loadMaker(mid, midResolver, cajolerFinder, asyncLoadFunction);
    });
  }

  function xhrAsyncLoad(modURL, midResolver, cajolerFinder) {
    if (cache[modURL] !== undefined) {
      return cache[modURL];
    }

    var r = Q.defer();
    cache[modURL] = r.promise;

    var xhr = bridal.makeXhr();
    xhr.onreadystatechange = function() {
      if (xhr.readyState === 4) {
        xhr.onreadystatechange = noop;  // avoid memory leak
        if (xhr.status === 200) {
          var savedModuleHandler = ___.getNewModuleHandler();
          ___.setNewModuleHandler(___.primFreeze({
            handle: ___.markFuncFreeze(function theHandler(module) {
              try {
                var load = loadMaker(modURL, midResolver, cajolerFinder, 
                                     xhrAsyncLoad);
                module.moduleURL = modURL;
                var securedModule = ___.prepareModule(module, load);
                var dependency = resolveDependency(module, load);
                Q.when(dependency, function(result) {
                                     r.resolve(securedModule);
                                   },
                                   function(reason) {
                                     r.resolve(Q.reject(
                                         "Resolving dependency for the"
                                         + "module " + modURL + " failed."));
                                   });
              } catch (e) {
                r.resolve(Q.reject(e));
              }
            }),
            handleUncaughtException: savedModuleHandler.handleUncaughtException
          }));
          //TODO: validate the response before eval it
          try {
            eval(xhr.responseText);
          } finally {
            ___.setNewModuleHandler(savedModuleHandler);
          }
        } else {
          r.resolve(Q.reject(
              "Retrieving the module " + modURL + " failed, "
              + "status code = " + xhr.status));
        }
      }
    };
    
    var cajoledModURL = cajolerFinder(modURL);

    xhr.open("GET", cajoledModURL, true);
    if (typeof xhr.overrideMimeType === 'function') {
      xhr.overrideMimeType("application/javascript");
    }
    xhr.send(null);
    return r.promise;
  }

  xhrModuleLoadMaker = makeConcreteLoadMaker(xhrAsyncLoad);

  var head = 0;
  var queue = [];
  var busy = false;

  function scriptAsyncLoad(modURL, midResolver, cajolerFinder) {
    if (cache[modURL] !== undefined) {
      return cache[modURL];
    }

    var r = Q.defer();
    cache[modURL] = r.promise;

    function dequeue() {
      if (head < queue.length) {
        busy = true;
        var savedHead = head;

        // TODO(ihab.awad): Modifying a frozen structure (the new module
        // handler) in un-cajoled code is possible but poor practice.

        // TODO(ihab.awad): Consider refactoring the <script> module loader to
        // always use a JSONP-style callback function so that it does not
        // trample on code that uses the standard ___.loadModule().

        // TODO(ihab.awad): Consider deprecating the entire newModuleHandler
        // system since it relies on globally visible modifications to "___";
        // instead maybe we should just cajole to a well-known callback if none
        // is supplied, like:
        //     cajaModuleCallback___(___.prepareModule({ ... module ... }));

        var newModuleHandler = ___.makeNormalNewModuleHandler();
        newModuleHandler.handle = ___.markFuncFreeze(
            function theHandler(module) {
              if (savedHead === head) {
                var r = queue[head].defer;
                try {
                  var curModURL = queue[head].modURL;
                  var load = loadMaker(curModURL, midResolver, cajolerFinder,
                                       scriptAsyncLoad);
                  module.moduleURL = modURL;
                  var securedModule = ___.prepareModule(module, load);

                  var dependency = resolveDependency(module, load);
                  Q.when(dependency,
                      function(result) {
                        r.resolve(securedModule);
                      },
                      function(reason) {
                        r.resolve(Q.reject(
                            "Resolving dependency for the module "
                            + curModURL + " failed."));
                      });
                } catch (e) {
                  r.resolve(Q.reject(e));
                }
                delete queue[head];
                head++;
                dequeue();
              } else {
                throw new Error('Module queue got out of sync');
              }
            });
        // TODO: don't we need to save the old module handler here so we
        // can restore it in timeout.
        ___.setNewModuleHandler(newModuleHandler);

        var timeout = function () {
          // Don't prevent GC
          script.onreadystatechange = script.onerror = null;
          if (savedHead === head) {
            var r = queue[head].defer;
            r.resolve(Q.reject(
                "Retrieving the module " + queue[head].modURL + " failed."));
            delete queue[head];
            head++;
            dequeue();
          } else {
            // the module has been loaded successfully
          }
        };

        var script = document.createElement("script");
        script.src = cajolerFinder(queue[head].modURL);
        script.onerror = timeout;
        script.onreadystatechange = function() {
          if (script.readyState === 'loaded'
              || script.readyState === 'complete') {
            timeout();
          }
        };
        document.getElementsByTagName('head')[0].appendChild(script);
      } else {
        busy = false;
      }
    }

    queue.push({ modURL: modURL, defer: r });

    if (!busy) {
      dequeue();
    }

    return r.promise;
  }

  scriptModuleLoadMaker = makeConcreteLoadMaker(scriptAsyncLoad);

  clearModuleCache = ___.markFuncFreeze(function() {
    cajita.forOwnKeys(cache, ___.markFuncFreeze(function(k, v) {
      delete cache[k];
    }));
  });
})();
