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
 * @fileoverview Serves as pseudo-code until we can run it.
 *
 * <p>See the "Translation" section of
 * http://code.google.com/p/google-caja/wiki/SES for the requirements
 * assumed by this code.
 *
 * @author Mark S. Miller
 * @provides expandProgramToExpr
 */

// Besides the "js" quasi-parser, the following code depends on these
// helper functions.
// isStatementOrDecl
// isExpr
// isGlobal          - a global variable name?
// isFree            - a free variable name?
// isGlobalOrFree
// mustNotShadow     - verify name doesn't shadow name in this function
// getFirstTokenName - printed form of first token
// getChildren       - of an ast node
// makeAstLike       - like arg, with alternate children
//
// The expanded code assumes bindings for
// declareGlobal___
// defineGlobal___
// cleanErr___
//
// Additional names reserved for the output that should be prohibited
// on input
// global___
// e___

var expandProgramToExpr;

(function() {
   "use strict";

   /**
    * The input ast to all of these comes after parsing and scope
    * analysis of a complete Program production, so that questions
    * like isGlobal have an answer. The js quasi-parser
    */
   expandProgramToExpr = function expandProgramToExpr(ast) {
     if (!isProgram.test(ast)) { return null; }
     var prog, expr;

     if (js`${isStatementOrDecl({prog})}*
            ${isExpr({expr})}`.test(ast)) {

       return js`(function(global___) {
                    ${expandAll(prog)}*
                    return ${expand(expr)};
                  }).call(this, this)`
     }
     if (js`${isStatementOrDecl({prog})}*`.test(ast)) {

       return js`(function(global___) {
                    ${expandAll(prog)}*
                  }).call(this, this)`
     }
     return null;
   }

   var byStartToken = StringMap();
   var others = [];

   function expandAll(asts) {
     return asts.map(expand);
   }

   function expand(ast) {
     var expander, result;

     var firstTokenName = getFirstTokenName(ast);

     if (firstTokenName) {
       expander = byStartToken(firstTokenName);
       if (expander) {
           result = expander(ast);
           if (result) { return result; }
       }
     }
     for (var i = 0, len = others.length; i++) {
       expander = others[i];
       result = expander(ast);
       if (result) { return result; }
     }

     var children = getChildren(ast);
     return makeAstLike(ast, expandAll(children));
   }

   function expandTopDecl(ast) {
     var name, expr;

     if (js`var ${isGlobal({name})};`.test(ast)) {

       return js`declareGlobal___("$name");`;
     }
     if (js`var ${isGlobal({name})} = ${{expr}};`.test(ast)) {

       return js`defineGlobal___("$name", ${expand(expr)};`;
     }
     return null;
   }
   byStartToken.set('var', expandTopDecl);

   function expandTypeof(ast) {
     var name;

     if (js`typeof ${isFree({name})}`.test(ast)) {

       return js`($name in global___) ? typeof $name : 'undefined'`;
     }
     return null;
   }
   byStartToken.set('typeof', expandTypeof);

   function expandGlobalFunctionCall(ast) {
     var name, args;

     if (js`${isGlobalOrFree({name})}(${{args}}*)`.test(ast)) {

       return js`$name.call(void 0, ${expandAll(args)*})`;
     }
     return null
   }
   others.push(expandGlobalFunctionCall);


   function expandTryCatch(ast) {
     var tryBlock, param, catchBlock, finallyBlock;

     if (js`try {
            ${{tryBlock}}
          } catch (${mustNotShadow({param})}) {
            ${{catchBlock}}
          }`.test(ast)) {

       return js`try {
                   ${expand(tryBlock)}
                 } catch (e___) { var $param = cleanErr___(e___);
                   ${expand(catchBlock)}
                 }`;
     }
     if (js`try {
            ${{tryBlock}}
          } catch (${mustNotShadow({param})}) {
            ${{catchBlock}}
          } finally {
            ${{finallyBlock}}
          }`.test(ast)) {

       return js`try {
                   ${expand(tryBlock)}
                 } catch (e___) { var $param = cleanErr___(e___);
                   ${expand(catchBlock)}
                 } finally {
                   ${expand(finallyBlock)}
                 }`;
     }
     return null;
   }
   byStartToken.set('try', expandTryCatch);
 })();