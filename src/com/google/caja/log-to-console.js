// Copyright (C) 2007 Google Inc.
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
// .............................................................................

// If this module is loaded after caja.js is loaded, and in an
// environment (such as produced by turning on Firebug) where
// <tt>console.log</tt> is a function, then it will register 
// (a wrapper around) <tt>console.log</tt> with
// <tt>___.setLogFunc()</tt> so caja.js will log its diagnostics 
// to the Firebug console.

// If you load triv-logger.js and log-to-console.js into the same
// system, the last one loaded wins.

// This module is written in Javascript, not Caja, and would be
// rejected by the Caja translator. 


(function(global) {
  
  if (global.___ && 
      global.console && 
      typeof global.console.log === 'function') {

    ___.setLogFunc(function(str, opt_stop) {
      global.console.log(str);
      if (opt_stop) {
        ({})['\nError: ' + str](str);
      }
    });
  }

})(this);
