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
 * @fileoverview An optional part of the SES initialization process
 * that saves potentially valuable debugging aids on the side before
 * startSES.js would remove these, and adds a debugging API which uses
 * these without compromising SES security.
 *
 * <p>NOTE: The currently exposed debugging API is far from
 * settled. This module is currently in an exploratory phase.
 *
 * <p>Meant to be run sometime after repairs are done and a working
 * WeakMap is available, but before startSES.js. initSESPlus.js includes
 * this. initSES.js does not.
 *
 * //provides ses.UnsafeError,
 * //provides ses.getCWStack ses.stackString ses.getStack
 * @author Mark S. Miller
 * @requires WeakMap, this
 * @overrides Error, ses, debugModule
 */

var Error;
var ses;

(function debugModule(global) {
   "use strict";

   if (typeof ses !== 'undefined' && ses.okToLoad && !ses.okToLoad()) {
     // already too broken, so give up
     return;
   }

   /**
    * Save away the original Error constructor as ses.UnsafeError and
    * make it otherwise unreachable. Replace it with a reachable
    * wrapping constructor with the same standard behavior.
    *
    * <p>When followed by the rest of SES initialization, the
    * UnsafeError we save off here is exempt from whitelist-based
    * extra property removal and primordial freezing. Thus, we can
    * use any platform specific APIs defined on Error for privileged
    * debugging operations, unless explicitly turned off below.
    */
   var UnsafeError = Error;
   ses.UnsafeError = Error;
   function FakeError(message) {
     return UnsafeError(message);
   }
   FakeError.prototype = UnsafeError.prototype;
   FakeError.prototype.constructor = FakeError;

   Error = FakeError;

   /**
    * Should be a function of an argument object (normally an error
    * instance) that returns the stack trace associated with argument
    * in Causeway format.
    *
    * <p>See http://wiki.erights.org/wiki/Causeway_Platform_Developer
    *
    * <p>Currently, there is no one portable technique for doing
    * this. So instead, each platform specific branch of the if below
    * should assign something useful to getCWStack.
    */
   ses.getCWStack = function uselessGetCWStack(err) { return void 0; };

   if ('captureStackTrace' in UnsafeError) {
     (function() {
       // Assuming http://code.google.com/p/v8/wiki/JavaScriptStackTraceApi
       // So this section is v8 specific.

       UnsafeError.prepareStackTrace = function(err, sst) {
         if (ssts === void 0) {
           // If an error happens in the debug module after setting up
           // this prepareStackTrace but before or during the
           // initialization of ssts, then this method gets called
           // with ssts still undefined (void 0). In that case, we
           // should report the error we're asked to prepare, rather
           // than an error thrown by failing to prepare it.
           ses.logger.error('Error while initializing debug module', err);
         } else {
           ssts.set(err, sst);
         }
         // Technically redundant, but prepareStackTrace is supposed
         // to return a value, so this makes it clearer that this value
         // is undefined (void 0).
         return void 0;
       };

       var unsafeCaptureStackTrace = UnsafeError.captureStackTrace;

       // TODO(erights): This seems to be write only. Can this be made
       // safe enough to expose to untrusted code?
       UnsafeError.captureStackTrace = function(obj, opt_MyError) {
         var wasFrozen = Object.isFrozen(obj);
         var stackDesc = Object.getOwnPropertyDescriptor(obj, 'stack');
         try {
           var result = unsafeCaptureStackTrace(obj, opt_MyError);
           var ignore = obj.stack;
           return result;
         } finally {
           if (wasFrozen && !Object.isFrozen(obj)) {
             if (stackDesc) {
               Object.defineProperty(obj, 'stack', stackDesc);
             } else {
               delete obj.stack;
             }
             Object.freeze(obj);
           }
         }
       };

       var ssts = new WeakMap(); // error -> sst

       /**
        * Returns a stack in Causeway format.
        *
        * <p>Based on
        * http://code.google.com/p/causeway/source/browse/trunk/src/js/com/teleometry/causeway/purchase_example/workers/makeCausewayLogger.js
        */
       function getCWStack(err) {
         var sst = ssts.get(err);
         if (sst === void 0 && err instanceof Error) {
           // We hope it triggers prepareStackTrace
           var ignore = err.stack;
           sst = ssts.get(err);
         }
         if (sst === void 0) { return void 0; }

         return { calls: sst.map(function(frame) {
           return {
             name: '' + (frame.getFunctionName() ||
                         frame.getMethodName() || '?'),
             source: '' + (frame.getFileName() || '?'),
             span: [ [ frame.getLineNumber(), frame.getColumnNumber() ] ]
           };
         })};
       };
       ses.getCWStack = getCWStack;
     })();

   } else if (global.opera) {
     (function() {
       // Since pre-ES5 browsers are disqualified, we can assume a
       // minimum of Opera 11.60.
     })();


   } else if (new Error().stack) {
     (function() {
       var FFFramePattern = (/^([^@]*)@(.*?):?(\d*)$/);

       // stacktracejs.com suggests that this indicates FF. Really?
       function getCWStack(err) {
         var stack = err.stack;
         if (!stack) { return void 0; }
         var lines = stack.split('\n');
         var frames = lines.map(function(line) {
           var match = FFFramePattern.exec(line);
           if (match) {
             return {
               name: match[1].trim() || '?',
               source: match[2].trim() || '?',
               span: [[+match[3]]]
             };
           } else {
             return {
               name: line.trim() || '?',
               source: '?',
               span: []
             };
           }
         });
         return { calls: frames };
       }

       ses.getCWStack = getCWStack;
     })();

   } else {
     (function() {
       // Including Safari and IE10.
     })();

   }

   /**
    * Turn a Causeway stack into a v8-like stack traceback string.
    */
   function stackString(cwStack) {
     if (!cwStack) { return void 0; }
     var calls = cwStack.calls;

     var result = calls.map(function(call) {

       var spanString = call.span.map(function(subSpan) {
         return subSpan.join(':');
       }).join('::');
       if (spanString) { spanString = ':' + spanString; }

       return '  at ' + call.name + ' (' + call.source + spanString + ')';

     });
     return result.join('\n');
   };
   ses.stackString = stackString;

   /**
    * Return the v8-like stack traceback string associated with err.
    */
   function getStack(err) {
     if (err !== Object(err)) { return void 0; }
     var cwStack = ses.getCWStack(err);
     if (!cwStack) { return void 0; }
     var result = ses.stackString(cwStack);
     if (err instanceof Error) { result = err + '\n' + result; }
     return result;
   };
   ses.getStack = getStack;

 })(this);
