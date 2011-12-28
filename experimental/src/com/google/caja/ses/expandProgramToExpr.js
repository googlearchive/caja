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
 * @fileoverview Pseudo-code explaining the source-SES to target-SES
 * translation.
 *
 * <p>See <a href="http://code.google.com/p/google-caja/wiki/SES"
 * >http://code.google.com/p/google-caja/wiki/SES</a> for an
 * explanation of the differences between source-SES and target-SES.
 *
 * <p>Serves as pseudo-code until we can run it. This section explains
 * what more is needed to turn this into running code.
 *
 * We express our translations using JavaScript <a href=
 * "http://wiki.ecmascript.org/doku.php?id=harmony:quasis"
 * >quasiliterals</a> and a hypothetical JavaScript quasi-parser named
 * "js". We additionally assume that the expression syntax {a, b} is
 * shorthand not for <pre>
 * {a: a, b: b}
 * </pre> as currently proposed, but rather for
 * <pre>
 * {
 *   get a() { return a; },
 *   set a(n) { a = n; },
 *   get b() { return b; },
 *   set b(n) { b = n; }
 * }</pre> as proposed at the end of the <a href=
 * "http://wiki.ecmascript.org/doku.php?id=strawman:object_initialiser_shorthand#discussion"
 * >object initializer discussion</a> section.
 *
 * Together, this means that (quasiParser`stuff${{varName}}stuff`) can
 * create an object that can be used as a pattern, placing the values
 * extracted from the specimen into varName, which must have already
 * been declared. As a further elaboration, we assume predicate
 * pattern functions that wrap such a literal
 * (quasiParser`stuff${isThing({varName})}stuff`) and report whether
 * the proposed assignment meets it criteria. If not, that match
 * fails.
 *
 * Separately from the rules below, we also prohibit identifiers
 * ending in ___ (triple underbar) in the input, so that we may use
 * them in the output without fear of capture.
 *
 * The JavaScript quasiParser accepts one additional element besides
 * JavaScript syntax and $ holes. In addition, when an "*", "+", or
 * "?" appears immediately to the right of a $ hole with no
 * intervening space, then it is taken as a quantifier on the binding
 * of the $ hole to consecutive AST elements.
 *
 * Besides the "js" quasi-parser, the following code depends on these
 * helper functions.
 * isStatementOrDecl
 * isExpr
 * isGlobal          - a global variable name?
 * isFree            - a free variable name?
 * isGlobalOrFree
 * mustNotShadow     - verify name doesn't shadow name in this function
 * getFirstTokenName - printed form of first token
 * getChildren       - of an ast node
 * makeAstLike       - like arg, with alternate children
 *
 * The expanded code assumes bindings for
 * declareGlobal___
 * defineGlobal___
 * cleanErr___
 *
 * Additional names reserved for the output that should be prohibited
 * on input
 * global___
 * e___
 *
 * <p>The input ast to all of the expand* functions below comes after
 * parsing and scope analysis of a complete Program production, so
 * that questions like isGlobal have an answer. If all the $-hole
 * inputs to a call to the js quasi-parser are asts, then js
 * quasi-parser produces pure ast fragments without scope analysis as
 * its output. If and of the $-hole inputs are one property objects or
 * their predicate wrappers, as explained above, then that call
 * produces an object with a ".test(ast)" method which matches against
 * the ast and, if true, assigns the extracted fragments to these
 * singleton properties.
 *
 * @author Mark S. Miller
 * @provides expandProgramToExpr
 */

var expandProgramToExpr;

(function() {
   "use strict";

   /**
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