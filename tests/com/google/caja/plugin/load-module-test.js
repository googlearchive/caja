// Copyright (C) 2011 Google Inc.
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

var createTest = function(rootUrl) {
  function makeModuleText(includedModules) {
    var i;
    var instantiate = 'var load = ___.ri(IMPORTS___, "load");';
    for (i = 0; i < includedModules.length; i++) {
      instantiate += 'load.i___("' + includedModules[i] + '");';
    }
    for (i = 0; i < includedModules.length; i++) {
      includedModules[i] = '"' + includedModules[i] + '"';
    }
    includedModules = '[' + includedModules.join(',') + ']';
    return {
      js:
        '{' +
        '  ___.loadModule({' +
        '    instantiate: function(___, IMPORTS___) {' +
        '      ' + instantiate +
        '    },' +
        '    includedModules: ' + includedModules +
        '  });' +
        '}'
    };
  }

  var modules = {};

  var requests = new WeakMap();

  var mockCajolingServiceClient = {
    cajoleUrl: function (url, mimeType) {
      var key = url + '-' + mimeType;
      console.log(modules);
      if (modules[key] === undefined) {
        throw 'Not found: ' + key;
      }
      var r = Q.defer();
      setTimeout(function() {
        r.resolve(modules[key]);
        requests.set(url,
            requests.get(url) === undefined
            ? 1 : requests.get(url) + 1);
      }, 0);
      return r.promise();
    },
    cajoleContent: function() {
      throw 'Unimplemented';
    }
  };

  var loadModule = loadModuleMaker(rootUrl, mockCajolingServiceClient);

  var addModule = function(url, mimeType, deps) {
    modules[url + '-' + mimeType] = makeModuleText(deps);
  };

  return {
    addModule: addModule,
    requests: requests,
    loadModule: loadModule
  };
};