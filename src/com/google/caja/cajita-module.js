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
 * @requires ___
 * @provides xhrModuleLoader, scriptModuleLoader
 * 
 * Each loader object contains one method 'async', which returns a module 
 * function given the source URL. 
 */
var xhrModuleLoader;
var scriptModuleLoader;
var clearModuleCache;

(function() {
  var cache = {};
  
  function addJsExtension(src) {
    if (src.toLowerCase().substring(src.length - 3) !== '.js') {
      src = src + ".js";
    }
    return src;
  }
  
  function xhrLoad(src) {
    var r = Q.defer();

    if (cache[src] !== undefined) {
      r.resolve(cache[src]);
    }
    else {
      var xhr = new XMLHttpRequest();    
      xhr.onreadystatechange = function() {
        if (xhr.readyState === 4) {
          if (xhr.status === 200) {
            var savedModuleHandler = ___.getNewModuleHandler();
            ___.setNewModuleHandler(___.primFreeze({
              handle: ___.markFuncFreeze(function theHandler(module) {
                try {
                  var securedModule = ___.prepareModule(module);
                  r.resolve(securedModule);
                  cache[src] = securedModule; 
              	} catch (e) {
              	  r.resolve(Q.reject(e));
              	}
              })
            }));
            //TODO: validate the response before eval it
            eval(xhr.responseText);
            ___.setNewModuleHandler(savedModuleHandler);
          } else {
            r.resolve(Q.reject(
                "Retrieving module failed, status code = " + xhr.status));
          }
        }
      };

      src = addJsExtension(src);
      xhr.open("GET", src, true);
      xhr.overrideMimeType("application/javascript");
      xhr.send(null);
    }
    return r.promise;
  }
  
  xhrModuleLoader = ___.primFreeze({ async: ___.markFuncFreeze(xhrLoad) });

  var head = 0;
  var queue = [];
  var busy = false;
  
  function scriptLoad(src) {
    var r = Q.defer();

    src = addJsExtension(src);
    
    function dequeue() {
      if (head < queue.length) {
        busy = true;
        var savedHead = head;
        
        ___.setNewModuleHandler(___.primFreeze({
    	  handle: ___.markFuncFreeze(function theHandler(module) {
    		if (savedHead === head) {
              var r = queue[head].defer;
              try {
                var securedModule = ___.prepareModule(module); 
                r.resolve(securedModule);
                cache[queue[head].src] = securedModule; 
              } catch (e) {
                r.resolve(Q.reject(e));
              }
              delete queue[head];
              head++;
              dequeue();
    		} else {
    		  // this should not happen
    		  // the module may have been mistakenly treated as a failure
    		}
    	  })
    	}));        
        
        function timeout() {
          if (savedHead === head) {
            var r = queue[head].defer;
            r.resolve(Q.reject(
                "Retrieving the module" + queue[head].src + " failed."));
            delete queue[head];
            head++;
            dequeue();
          } else {
            // the module has been loaded successfully
          }
        }

        var script = document.createElement("script");
    	script.src = queue[head].src;
    	script.onerror = function() {
    	  timeout();
          script.onreadystatechange = script.onerror = null;
    	};
    	script.onreadystatechange = function() {
    	  if (script.readyState === 'loaded'
    	      || script.readyState === 'complete') {
            timeout();
            script.onreadystatechange = script.onerror = null;
    	  }
    	};
        document.getElementsByTagName('head')[0].appendChild(script);
      } else {
    	busy = false;
      }
    }
    
    if (cache[src] !== undefined) {
      r.resolve(cache[src]);
    } else {
      var e = new Object();
      e.src = src;
      e.defer = r;
      queue.push(e);

      if (!busy) {
        dequeue();
      }
    }
    
    return r.promise;
  }
  
  scriptModuleLoader = ___.primFreeze(
      { async: ___.markFuncFreeze(scriptLoad) });
  
  clearModuleCache = ___.markFuncFreeze(function() {
    cajita.forOwnKeys(cache, ___.markFuncFreeze(function(k, v) {
      delete cache[k];
    }));
  });
})();