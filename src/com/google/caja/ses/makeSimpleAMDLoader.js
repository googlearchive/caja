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
 * @fileoverview Makes a loader for a simple subset of the
 * Asynchronous Module Loader (AMD) API
 * https://github.com/amdjs/amdjs-api/wiki/AMD . Based on
 * http://wiki.ecmascript.org/doku.php?id=strawman:concurrency#amd_loader_lite
 *
 * //provides makeSimpleAMDLoader
 * @author Mark S. Miller
 * @requires StringMap, cajaVM
 * @requires this, compileExprLater, Q
 */


(function(pseudoGlobal){
   "use strict";

   var bind = Function.prototype.bind;
   // See
   // http://wiki.ecmascript.org/doku.php?id=conventions:safe_meta_programming
   var uncurryThis = bind.bind(bind.call);

   var applyFn = uncurryThis(bind.apply);
   var mapFn = uncurryThis([].map);

   var freeze = Object.freeze;
   var constFunc = cajaVM.constFunc;


   /**
    * A pumpkin is a unique value that must never escape, and so may
    * safely be used to test for the absence of any possible
    * user-provided value.
    */
   var defineNotCalledPumpkin = {};

   /**
    * Makes a loader for a simple subset of the Asynchronous Module
    * Loader (AMD) API.
    *
    * <p>Terminology: When we say a function "reveals" a value X, we
    * means that it either immediately returns an X or it immediately
    * returns a promise that it eventually fulfills with an X. Unless
    * stated otherwise, we implicitly elide the error conditions from
    * such statements. For the more explicit statement, append: "or it
    * throws, or it does not terminate, or it breaks the returned
    * promise, or it never resolves the returned promise."
    *
    * <p>The provided "fetch" function should be a function from a
    * module name string to revealing the source string for that
    * module. This source string is assumed to be in (our simple
    * subset of) AMD format. When run under Caja/SES, the module
    * source is executed in a scope consisting of only the whitelisted
    * globals and the "define" function from our subset of the AMD
    * API. Our "define" function always takes at least the following
    * two arguments: <ul>
    * <li>"deps" --- a "dependencies" array of module name strings,
    * <li>and a factory function.
    * </ul>
    * The factory function should have one parameter for accepting
    * each module instance corresponding to each of these module
    * names. Whatever the factory function reveals is taken to be the
    * instance of this module.
    *
    * <p>Note that in this subset, a module's source does not get to
    * determine its own module name. Rather, this naming is only
    * according to the mapping provided by the "fetch"
    * function. However, in a concession to jQuery, the module can
    * provide the "define" function's first optional argument, in
    * which it declares its own module name. The "define" function
    * will then check whether this agrees with the module name as
    * determined by the "fetch" function, and if not, break the
    * promise for the module instance.
    *
    * <p>The opt_moduleMap, if provided, should be a mapping from
    * module names to module instances. To endow a subsystem with the
    * ability to import connections to the outside world, provide an
    * opt_moduleMap already initialized with some pre-existing
    * name-to-instance associations.
    */
   function makeSimpleAMDLoader(fetch, opt_moduleMap) {
     var moduleMap = opt_moduleMap || StringMap();

     var loader;

     function rawLoad(id) {
       return Q(fetch(id)).then(function(src) {

         var result = defineNotCalledPumpkin;
         function define(opt_id, deps, factory) {
           if (typeof opt_id === 'string') {
             if (opt_id !== id) {
               result = Q.reject(new Error('module "' + id +
                                           '" thinks it is "' + opt_id + '"'));
               return;
             }
           } else {
             factory = deps;
             deps = opt_id;
           }
           var amdImportPs = mapFn(deps, loader);
           result = Q.all(amdImportPs).then(function(amdImports) {
             return applyFn(factory, void 0, amdImports);
           });
         }
         // TODO(erights): Once we're jQuery compatible, change
         // jQuery: to true.
         define.amd = freeze({ lite: true, caja: true, jQuery: false });

         var imports = cajaVM.makeImports();
         cajaVM.copyToImports(imports, {define: constFunc(define)});
         cajaVM.def(imports);

         var compiledExprP = compileExprLater(
           '(function(){' + src + '})()', id);
         return Q(compiledExprP).then(function(compiledExpr) {

           compiledExpr(imports);
           if (result === defineNotCalledPumpkin) {
             result = Q.reject(new Error('"define" not called by: ' + id));
           }
           return result;
         });
       });
     }
     return loader = Q.memoize(rawLoad, moduleMap);
   }

   pseudoGlobal.makeSimpleAMDLoader = constFunc(makeSimpleAMDLoader);

 })(this);
