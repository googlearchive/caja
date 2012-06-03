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

// This module adds a trivial "logger" object to the global scope
// whose "log" method simply accumulates logged strings (with an extra
// newline added) into a recording, returned by getRecording(). If
// loaded after es53.js is loaded, then this script registers its
// logger with ___.setLogFunc() so es53.js will log its diagnostics to
// this logger.

// If you load triv-logger.js and log-to-console.js into the same
// system, the last one loaded wins.

// This module is written in Javascript, not Caja, and would be
// rejected by the Caja translator. 


var logger = (function(global) {
  
  var recording = [];
  function getRecording() { return recording; }
  function log(str) { recording.push(String(str) + '\n'); }

  var result = {
    getRecording: getRecording,
    log: log
  };

  if (global.___) { ___.setLogFunc(log); }

  return result;

})(this);
