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

// If this module is loaded after cajita.js is loaded, and in an
// environment (such as produced by turning on Firebug) where
// <tt>console.log</tt> is a function, then it will register 
// a wrapper around <tt>console.log</tt> (or <tt>console.info</tt> 
// and <tt>console.error</tt> if available) using 
// <tt>___.setLogFunc()</tt>, so cajita.js will log its diagnostics
// to the Firebug console.

// If you load triv-logger.js and log-to-console.js into the same
// system, the last one loaded wins.

// This module is written in Javascript, not Caja, and would be
// rejected by the Caja translator. 


(function(global) {
  
  var console;

  if (global.___ && 
      (console = global.console) && 
      typeof console.log === 'function') {

    function logToConsole(str, opt_stop) {
      if (opt_stop && typeof console.error === 'function') {
	console.error(str);
      } else if (typeof console.info === 'function') {
	console.info(str);
      } else {
	console.log(str);
      }
      if (opt_stop) {
        // breakpoint here by uncommenting out the following line:
        debugger;
        // or by setting a breakpoint on this useless line:
        return;
      }
    };
    ___.setLogFunc(logToConsole);
  }

})(this);
