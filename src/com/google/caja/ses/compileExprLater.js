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
 * @fileoverview Makes a "ses.compileExprLater" function which acts like
 * "cajaVM.compileExpr", except that it returns a promise for the
 * outcome of attempting to compile the argument expression. 
 * Likewise, makes a "ses.confineLater" as an eventual analog of
 * "cajaVM.confine".
 *
 * <p>There are two reasons why one might use compileExprLater rather
 * than just using compileExpr asynchronously:
 *
 * <ul>
 * <li>On some browsers (including Safari 7.0.1, which is current at
 *     the time of this writing), code loaded via script tags are given
 *     more accurate source locations in stack traces than code loaded via
 *     eval. Script tags can generally only load code asynchronously.
 * <li>Some loading scenarios require the code to be translated
 *     first. However, translators can be large, so we may sometimes want
 *     to load them asynchronously.
 * </ul>
 *
 * //requires ses.okToLoad, ses.securableWrapperSrc, ses.atLeastFreeVarNames,
 * //requires ses.makeCompiledExpr, ses.prepareExpr
 * //provides ses.compileExprLater, ses.confineLater
 * //provides ses.redeemResolver for its own use
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

   if (ses && !ses.okToLoad()) { return; }

   /**
    * This implementation works and satisfies the semantics, but
    * bottoms out in the built-in, which currently does not work well
    * with the Chrome debugger.
    *
    * <p>Since SES is independent of the hosting environment, we
    * feature test on the global named "document". If it is absent,
    * then we fall back to this implementation which works in any
    * standard ES5 environment.
    */
   function compileExprLaterFallback(exprSrc, opt_mitigateOpts) {
     // Coercing an object to a string may observably run code, so do
     // this now rather than in any later turn.
     exprSrc = ''+exprSrc;

     return Q(cajaVM).send('compileExpr',
                           exprSrc, opt_mitigateOpts);
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
    * Implements an eventual compileExpr using injected script tags
    */
   function compileLaterInScript(exprSrc, opt_mitigateOpts) {
     var prep = ses.prepareExpr(exprSrc, opt_mitigateOpts);

     var result = Q.defer();
     var resolverTicket = getResolverTicket(result.resolve);

     // Freenames consist solely of identifier characters (\w|\$)+
     // which do not need to be escaped further
     var freeNamesList = prep.freeNames.length == 0 ? '[]' :
         '["' + prep.freeNames.join('", "') + '"]';

     var scriptSrc =
       'ses.redeemResolver(' + resolverTicket + ')(' +
         'Object.freeze(ses.makeCompiledExpr(' +
           prep.wrapperSrc + ',\n' +
           freeNamesList + ', ' +
           JSON.stringify(prep.options) +
         '))' +
       ');' + prep.suffixSrc;

     var head = document.getElementsByTagName("head")[0];
     var script = document.createElement("script");
     head.insertBefore(script, head.lastChild);
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

   if (typeof document === 'undefined') {
     ses.compileExprLater = compileExprLaterFallback;
   } else {
     ses.compileExprLater = compileLaterInScript;
   }

   /**
    * Implements an eventual confine using compileExprLater
    */
   function confineLater(exprSrc, opt_endowments, opt_mitigateOpts) {
     // not necessary, since we only use it once below with a callee
     // which is itself safe. But we coerce to a string anyway to be
     // more robust against future refactorings.
     exprSrc = ''+exprSrc;

     var imports = cajaVM.makeImports();
     if (opt_endowments) {
       cajaVM.copyToImports(imports, opt_endowments);
     }
     cajaVM.def(imports);
     var compiledP = ses.compileExprLater(exprSrc, opt_mitigateOpts);
     return Q(compiledP).fcall(imports);
   }

   ses.confineLater = confineLater;

 })(this);
