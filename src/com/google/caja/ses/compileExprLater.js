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
 * @fileoverview Makes a "compileExprLater" function which acts like
 * "cajaVM.compileExpr", except that it returns a promise for the
 * outcome of attempting to compile the argument expression.
 *
 * //provides ses.compileExprLater
 * @author Mark S. Miller
 * @overrides ses
 * @requires Q, cajaVM, document, URI
 */


/* See
http://webreflection.blogspot.com/2011/08/simulate-script-injection-via-data-uri.html
*/

var ses;

(function(global) {
   "use strict";

   if (ses && !ses.ok()) { return; }

   /**
    * This implementation works and satisfies the semantics, but
    * bottoms out in the built-in, which currently does not work well
    * with the Chrome debugger.
    *
    * <p>Since SES is independent of the hosting environment, we
    * feature test on the global named "document". If it is absent,
    * then we fall back to this implementation which works in any
    * standard ES5 environment.
    *
    * <p>Eventually, we should check whether we're in an ES5/3
    * environment, in which case our choice for compileExprLater would
    * be one that sends the expression back up the server to be
    * cajoled.
    */
   function compileExprLaterFallback(exprSrc, opt_sourcePosition) {
     // Coercing an object to a string may observably run code, so do
     // this now rather than in any later turn.
     exprSrc = ''+exprSrc;

     return Q(cajaVM).send('compileExpr', exprSrc, opt_sourcePosition);
   }

   if (typeof document === 'undefined') {
     ses.compileExprLater = compileExprLaterFallback;
     return;
   }

   var resolvers = [];
   var lastResolverTicket = -1;

   function getResolverTicket(resolver) {
     ++lastResolverTicket;
     resolvers[lastResolverTicket] = resolver;
     return lastResolverTicket;
   }

   ses.redeemResolver = function(i) {
     var resolver = resolvers[i];
     delete resolvers[i];
     return resolver;
   };

   /**
    *
    */
   function compileLaterInScript(exprSrc, opt_sourceUrl) {

     var result = Q.defer();

     // The portion of the pattern in compileExpr which is appropriate
     // here as well.
     var wrapperSrc = ses.securableWrapperSrc(exprSrc, opt_sourceUrl);
     var freeNames = ses.atLeastFreeVarNames(exprSrc);

     var head = document.getElementsByTagName("head")[0];
     var script = document.createElement("script");
     head.insertBefore(script, head.lastChild);

     var resolverTicket = getResolverTicket(result.resolve);

     var scriptSrc = 'ses.redeemResolver(' + resolverTicket + ')(' +
       'Object.freeze(ses.makeCompiledExpr(' + wrapperSrc + ',\n' +
       // Freenames consist solely of identifier characters (\w|\$)+
       // which do not need to be escaped further
       '["' + freeNames.join('", "') + '"])));';

     if (opt_sourceUrl) {
       // See http://code.google.com/p/google-caja/wiki/SES#typeof_variable
       if (typeof global.URI !== 'undefined' && URI.parse) {
         var parsed = URI.parse(String(opt_sourceUrl));
         parsed = null === parsed ? null : parsed.toString();

         // Note we could try to encode these characters or search specifically
         // for */ as a pair of characters but since it is for debugging only
         // choose to avoid
         if (null !== parsed &&
             parsed.indexOf("<") < 0 &&
             parsed.indexOf(">") < 0 &&
             parsed.indexOf("*") < 0) {
           scriptSrc = '/* from ' + parsed + ' */ ' + scriptSrc;
         }
       }
     }

     // TODO(erights): It seems that on Chrome at least, the injected
     // script actually executes synchronously *now*. Is this
     // generally true? If so, perhaps we can even make synchronous
     // eval debuggable? Is such synchronous eval ok for the use case
     // here, or do we need to postpone this to another turn just in
     // case?
     script.appendChild(document.createTextNode(scriptSrc));

     function deleteScriptNode() { script.parentNode.removeChild(script); }

     Q(result.promise).then(deleteScriptNode, deleteScriptNode).end();

     return result.promise;
   }

   ses.compileExprLater = compileLaterInScript;

 })(this);
