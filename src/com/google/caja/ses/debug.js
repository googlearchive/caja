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
 * <p>TODO(erights): Explore alternatives to using "instanceof Error"
 * within this file.  Using "instanceof" makes this fail when used
 * inter-realm. In ES6 no good inter-realm brand check seems
 * possible. But even intra-realm, "instanceof" is not a brand check
 * so there would be no loss of integrity in switching to some other
 * heuristic.
 *
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
   function FakeError(message) {
     return UnsafeError(message);
   }
   FakeError.prototype = UnsafeError.prototype;
   FakeError.prototype.constructor = FakeError;

   Error = FakeError;

   // Even though this section of code must preserve a security
   // invariant, this file as a whole is optional, and SES must remain
   // secure if it is omitted. If this file is omitted, then the
   // original Error constructor as a whole remains in place, and the
   // whitelist-based cleaning mechanism in startSES.js will remove
   // everything that would have made it unsafe. It is only if we
   // attempt to hide the original Error constructor where the
   // whitelisting mechanism won't find it, as the code above does,
   // that we must ensure that it really is unreachable, as the code
   // below does.
   //
   // TODO(erights): We need a more general mechanism for this kind of
   // cleanup. One that covers this case and the UnsafeFunction case
   // in startSES.js. In the meantime, please ensure this list remains
   // in sync with the *Error "subclasses" of Error in whitelist.js.
   [EvalError, RangeError, ReferenceError, SyntaxError, TypeError, URIError
   ].forEach(function(err) {
     if (Object.getPrototypeOf(err) === UnsafeError) {
       Object.setPrototypeOf(err, FakeError);
     }
   });

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

   // FF40 Nightly has moved the magic stack property to a
   // not-very-magic getter on Error.prototype. This enables us to
   // prevent unprivileged access to stack information.
   var primStackDesc = 
       Object.getOwnPropertyDescriptor(Error.prototype, 'stack');
   var primStackGetter = (primStackDesc && primStackDesc.get) ||
       function legacyPrimStackGetter() { return this.stack; };

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
         if (Object(err) !== err) { return void 0; }
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

   } else {
     (function() {
       // Each of these patterns should have the first capture group
       // be the function name, and the second capture group be the
       // source URL together with position information. Afterwards,
       // the lineColPattern will pull apart these source position
       // components. On all, we assume the function name, if any, has
       // no colon (":"), at-sign ("@"), or open paren ("("), as each
       // of these are used to recognize other parts of a debug line.

       // Seen on FF: The function name is sometimes followed by
       // argument descriptions enclosed in parens, which we
       // ignore. Then there is always an at-sign followed by possibly
       // empty source position.
       var FFFramePattern =  /^\s*([^:@(]*?)\s*(?:\(.*\))?@(.*?)$/;
       // Seen on IE: The line begins with " at ", as on v8, which we
       // ignore. Then the function name, then the source position
       // enclosed in parens.
       var IEFramePattern =  /^\s*(?:at\s+)?([^:@(]*?)\s*\((.*?)\)$/;
       // Seem on Safari (JSC): The name optionally followed by an
       // at-sign and source position information. This is like FF,
       // except that the at-sign and source position info may
       // together be absent.
       var JSCFramePatt1 =   /^\s*([^:@(]*?)\s*(?:@(.*?))?$/;
       // Also seen on Safari (JSC): Just the source position info by
       // itself, with no preceding function name. The source position
       // always seems to contain at least a colon, which is how we
       // decide that it is a source position rather than a function
       // name. The pattern here is a bit more flexible, in that it
       // will accept a function name preceding the source position
       // and separated by whitespace.
       var JSCFramePatt2 =   /^\s*?([^:@(]*?)\s*?(.*?)$/;

       // List the above patterns in priority order, where the first
       // matching pattern is the one used for any one stack line.
       var framePatterns = [FFFramePattern, IEFramePattern,
                            JSCFramePatt1, JSCFramePatt2];

       // Each of the LineColPatters should have the first capture
       // group be the source URL if any, the second by the line
       // number if any, and the third be the column number if any.

       // Seen on FF Nightly 30 for execution in evaled strings.
       // The current Causeway format is not sufficiently expressive
       // to represent the useful information here and (TODO(erights))
       // needs to be enhanced. In the meantime, this pattern captures
       // the outer source position and ignores the inner one.
       var FFEvalLineColPatterns = 
             (/^(.*?) line (\d+) > (?:[^:]*):(?:\d+):(?:\d+)$/);
       // If the source position ends in either one or two
       // colon-digit-sequence suffixes, then the first of these are
       // the line number, and the second, if present, is the column
       // number.
       var MainLineColPattern = /^(.*?)(?::(\d+)(?::(\d+))?)?$/;

       // List the above patterns in priority order, where the first
       // matching pattern is the one used for any one stack line.
       var lineColPatterns = [FFEvalLineColPatterns, MainLineColPattern];

       function getCWStack(err) {
         var stack = void 0;
         try {
           stack = primStackGetter.call(err);
         } catch (_) {
           // There's no known good inter-realm brand check for
           // whether something is an error object. Instead, we simply
           // handle the failure of stack-getting magic as another way
           // to not get any stack information.
         }
         if (typeof stack !== 'string' || stack === '') { return void 0; }
         var lines = stack.split('\n');
         if (/^\w*Error:/.test(lines[0])) {
           lines = lines.slice(1);
         }
         lines = lines.filter(function(line) { return line !== ''; });
         var frames = lines.map(function(line) {
           var name = line.trim();
           var source = '?';
           var span = [];
           // Using .some here only because it gives us a way to escape
           // the loop early. We do not use the results of the .some.
           framePatterns.some(function(framePattern) {
             var match = framePattern.exec(line);
             if (match) {
               name = match[1] || '?';
               source = match[2] || '?';
               // Using .some here only because it gives us a way to escape
               // the loop early. We do not use the results of the .some.
               lineColPatterns.some(function(lineColPattern) {
                 var sub = lineColPattern.exec(source);
                 if (sub) {
                   // sub[1] if present is the source URL.
                   // sub[2] if present is the line number.
                   // sub[3] if present is the column number.
                   source = sub[1] || '?';
                   if (sub[2]) {
                     if (sub[3]) {
                       span = [[+sub[2], +sub[3]]];
                     } else {
                       span = [[+sub[2]]];
                     }
                   }
                   return true;
                 }
                 return false;
               });
               return true;
             }
             return false;
           });
           if (name === 'Anonymous function') {
             // Adjust for weirdness seen on IE
             name = '?';
           } else if (name.indexOf('/') !== -1) {
             // Adjust for function name weirdness seen on FF.
             name = name.replace(/[/<]/g,'');
             var parts = name.split('/');
             name = parts[parts.length -1];
           }
           if (source === 'Unknown script code' || source === 'eval code') {
             // Adjust for weirdness seen on IE
             source = '?';
           }
           return {
             name: name,
             source: source,
             span: span
           };
         });
         return { calls: frames };
       }

       ses.getCWStack = getCWStack;
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
     var result;
     if (cwStack) {
       result = ses.stackString(cwStack);
     } else {
       if (err instanceof Error &&
           typeof (result = primStackGetter.call(err)) === 'string' &&
           result !== '') {
         // already in result
       } else {
         return void 0;
       }
     }
     if (err instanceof Error) {
       result = err + '\n' + result;
     }
     return result;
   };
   ses.getStack = getStack;

 })(this);
