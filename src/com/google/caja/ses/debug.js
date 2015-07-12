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

 * <p>This API defines an "Extended Causeway Stacktrace", a
 * JSON representation of a stacktrace, based on Causeway's stacktrace
 * format, as documented at
 * http://wiki.erights.org/wiki/Causeway_Platform_Developer and
 * implemented at
 * https://github.com/cocoonfx/causeway/blob/master/src/js/com/teleometry/causeway/purchase_example/workers/makeCausewayLogger.js
 * and
 * https://github.com/cocoonfx/causeway/blob/master/src/js/com/teleometry/causeway/log/html/instrument.js
 * The extension is to allow nested frames, in order to represent
 * nested eval positions. An Extended Causeway Stacktrace has the form:
 * <pre>
 * stacktrace ::= {calls: [frame*]};
 * frame ::= {name: functionName,
 *            source: source,
 *            span: [[startLine, startCol?], [endLine, endCol?]?]};
 * functionName ::= STRING;
 * startLine, startCol, endLine, endCol ::= INTEGER
 * source ::= STRING | frame;
 * </pre>
 * <p>TODO(erights): Move this frame nesting into Causeway itself.
 *
 * <p>The functionName might be the name of the function, a name
 * associated with where the function is stored, i.e., the property
 * name as "method name", or "?" to indicate unknown.
 *
 * <p>When the call happens at callsite P in code that is evaled from
 * callsite Q, then the outer frame represents callsite P and the
 * <tt>source</tt> of that outer frame is an inner frame representing
 * callsite Q.
 *
 * When the source is a string, it should be either a URI ideally
 * saying where to obtain exactly that same source, or a "?" to
 * indicate unknown.
 *
 * <p>ses.getCWStack(err) obtains an Extended Causeway Stacktrace from
 * an error object if possible.
 *
 * <p>ses.stackString(stacktrace) converts an Extended Causeway
 * Stacktrace into a v8-like stack traceback string as documented at
 * https://code.google.com/p/v8/wiki/JavaScriptStackTraceApi
 *
 * <p>ses.getStack(err) obtains a stack traceback string from an error
 * object if possible. Ideally, it does so by
 * stackString(getCWStack(err)), yielding a v8-like stack traceback
 * string in a mostly platform-independent mannner. However, it may
 * report a platform-dependent string when getCWStack(err) on that
 * platform does not succeed.
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

   // This object should not actually be exposed. It is exposed specifically
   // because some applications want to do things like setting
   // Error.stackTraceLimit. In the future, this will be replaced with a better-
   // designed API.
   //
   // Applications should make sure that they do not reveal this object to
   // any unprivileged code, and be prepared to cope with its absence in future
   // versions.
   //
   // Some history:
   // https://github.com/google/caja/issues/1516
   // https://groups.google.com/forum/#!topic/google-caja-discuss/46_j5Rb6cTc
   ses.UnsafeError = Error;

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
    * in Extended Causeway format, as defined above.
    *
    * <p>Currently, there is no one portable technique for doing
    * extracting stack trace information from an Error
    * object. Instead, each platform specific branch of the
    * <tt>if</tt> below should assign something useful to getCWStack.
    */
   ses.getCWStack = function uselessGetCWStack(err) { return void 0; };

   // FF40 Nightly has moved the magic stack property to a
   // not-very-magic getter on Error.prototype. This enables us to
   // prevent unprivileged access to stack information, by capturing
   // this getter before startSES removes it, since it is not
   // whitelisted. 
   var primStackDesc =
       Object.getOwnPropertyDescriptor(Error.prototype, 'stack');
   var primStackGetter = (primStackDesc && primStackDesc.get) ||
       function legacyPrimStackGetter() { return this.stack; };

   /**
    * line2CWFrame(line) takes a (typically) single line string,
    * representing a single stackframe of a stacktrace, and returns an
    * Extended Causeway frame JSON object as defined above.
    *
    * <p>There is no standard for how these lines are formatted and
    * they vary widely between platforms. line2CWFrame scrapes this
    * line unreliably by using a variety of regular expressions that
    * we've accumulated over time, to cover all the cases we've seen
    * across platforms. There are a variety of user-triggered
    * conditions that can cause this scraping to fail, such as a
    * methodName that contains an "(" or "@" character.
    */
   var line2CWFrame = (function() {
     // Each of these frame patterns should have the first capture
     // group be the function name, and the second capture group be
     // the source URL together with position
     // information. Afterwards, the lineColPattern will pull apart
     // these source position components. On all, we assume the
     // function name, if any, has no colon (":"), at-sign ("@"), or
     // open paren ("("), as each of these are used to recognize
     // other parts of a debug line.

     // See https://code.google.com/p/v8/issues/detail?id=4268
     var V8NestedCallSitePattern = /^eval at (.*) \((.*)\)$/;

     // Seen on FF: The function name is sometimes followed by
     // argument descriptions enclosed in parens, which we
     // ignore. Then there is always an at-sign followed by possibly
     // empty source position.
     var FFFramePattern = /^\s*([^:@(]*?)\s*(?:\(.*\))?@(.*?)$/;

     // Seen on IE: The line begins with " at ", as on v8, which we
     // ignore. Then the function name, then the source position
     // enclosed in parens.
     var IEFramePattern = /^\s*(?:at\s+)?([^:@(]*?)\s*\((.*?)\)$/;

     // Seem on Safari (JSC): The name optionally followed by an
     // at-sign and source position information. This is like FF,
     // except that the at-sign and source position info may
     // together be absent.
     var JSCFramePatt1 = /^\s*([^:@(]*?)\s*(?:@(.*?))?$/;

     // Also seen on Safari (JSC): Just the source position info by
     // itself, with no preceding function name. The source position
     // always seems to contain at least a colon, which is how we
     // decide that it is a source position rather than a function
     // name. The pattern here is a bit more flexible, in that it
     // will accept a function name preceding the source position
     // and separated by whitespace.
     var JSCFramePatt2 = /^\s*?([^:@(]*?)\s*?(.*?)$/;

     // List the above patterns in priority order, where the first
     // matching pattern is the one used for any one stack line.
     var framePatterns = [V8NestedCallSitePattern,
                          FFFramePattern, IEFramePattern,
                          JSCFramePatt1, JSCFramePatt2];


     // Each of the LineColPatterns should have the first capture
     // group be the source URL if any, the second be the line
     // number if any, and the third be the column number if any.
     // If there are more, then we have an eval where the next three
     // are the function-name, line, and column within the evaled string.

     // Seen on FF Nightly 30 for execution in evaled strings.
     // On the left of the &gt; is the position from which eval was
     // called. On the right is the position within the evaled
     // string.
     //
     // TODO(erights): Handle multiple eval nestings. This is low
     // priority because SES only exposes eval through functions
     // that call eval, and so SES never has direct eval
     // nestings. In any case, if the multiple eval syntax is
     // encountered, e.g., 
     //   http://example.com line 16 > eval line 1 > eval:2:8
     // it will match this pattern, but with the first capture
     // group being "http://example.com line 16 > eval"
     var FFEvalLineColPatterns =
           /^(.*) line (\d+)() > ([^:]*):(\d+):(\d+)$/;

     // If the source position ends in either one or two
     // colon-digit-sequence suffixes, then the first of these are
     // the line number, and the second, if present, is the column
     // number.
     var MainLineColPattern = /^(.*?)(?::(\d+)(?::(\d+))?)?$/;

     // List the above patterns in priority order, where the first
     // matching pattern is the one used for any one stack line.
     var lineColPatterns = [FFEvalLineColPatterns, MainLineColPattern];

     function line2CWFrame(line) {
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
               // sub[4] if present is the function name within the evaled
               // string.
               // sub[5] if present is the line number within the
               // evaled string.
               // sub[6] if present is the column number within
               // the evaled string.
               source = sub[1] || '?';
               if (sub[2]) {
                 if (sub[3]) {
                   span = [[+sub[2], +sub[3]]];
                 } else {
                   span = [[+sub[2]]];
                 }
               }
               if (sub.length >= 5) {
                 source = {
                   name: sub[4] === 'eval' ? '?' : (sub[4] || '?'),
                   source: source,
                   span: span
                 };
                 span = [];
                 if (sub[5]) {
                   if (sub[6]) {
                     span = [[+sub[5], +sub[6]]];
                   } else {
                     span = [[+sub[5]]];
                   }
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
     }

     return line2CWFrame;
   })();

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
        * Given a v8 CallSite object as defined at
        * https://code.google.com/p/v8-wiki/wiki/JavaScriptStackTraceApi
        * return a stack frame in Extended Causeway Format as defined
        * above.
        */
       function callSite2CWFrame(callSite) {
         if (typeof callSite === 'string') {
           // See https://code.google.com/p/v8/issues/detail?id=4268
           return line2CWFrame(callSite);
         }
         var source = callSite.isEval() ?
             callSite2CWFrame(callSite.getEvalOrigin()) :
             '' + (callSite.getFileName() || '?');
         var name = '' + (callSite.getFunctionName() ||
                       callSite.getMethodName() || '?');
         return {
           name: name,
           source: source,
           span: [ [ callSite.getLineNumber(), callSite.getColumnNumber() ] ]
         };
       }

       /**
        * Returns a stack in Extended Causeway Format as defined above.
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

         return {calls: sst.map(callSite2CWFrame)};
       };
       ses.getCWStack = getCWStack;
     })();

   } else {
     (function() {
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
         var frames = lines.map(line2CWFrame);
         return { calls: frames };
       }

       ses.getCWStack = getCWStack;
     })();
   }

   /**
    * Turn an Extended Causeway stackframe object into a stackframe line
    * from a v8-like stack traceback string as defined at
    * https://code.google.com/p/v8-wiki/wiki/JavaScriptStackTraceApi
    */
   function frameString(frame) {
     var spanString = frame.span.map(function(subSpan) {
       return subSpan.join(':');
     }).join('::');
     if (spanString) { spanString = ':' + spanString; }
     var source = frame.source;
     if (typeof source !== 'string') {
       source = 'eval' + frameString(source);
     }
     return ' at ' + frame.name + ' (' + source + spanString + ')';
   }

   /**
    * Turn an Extended Causeway Stacktrace object into a v8-like stack
    * traceback string.
    */
   function stackString(cwStack) {
     if (!cwStack) { return void 0; }
     var result = cwStack.calls.map(frameString);
     return ' ' + result.join('\n ');
   };
   ses.stackString = stackString;

   /**
    * Return a stack traceback string associated with err. Ideally,
    * this is in a platform idependent v8-like format, but this may
    * not always be possible.
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
