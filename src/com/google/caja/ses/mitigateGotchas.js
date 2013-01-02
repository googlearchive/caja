// Copyright (C) 2012 Google Inc.
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
 * @fileoverview Mitigate deviations between SES and ES5-strict code
 * by rewriting programs where possible.  The output of this stage is
 * outside the TCB.
 * See http://code.google.com/p/google-caja/wiki/SES#Source-SES_vs_Target-SES
 * for a list of these differences.
 *
 * TODO(jasvir): Rewrite top level function declarations
 *
 * Note that the parse tree manipulated in this file uses the SpiderMonkey
 * AST format
 * (https://developer.mozilla.org/en-US/docs/SpiderMonkey/Parser_API)
 * 
 * @author Jasvir Nagra (jasvir@google.com)
 * @overrides ses
 */

(function() {
  function introducesScope(node) {
    return node.type === 'FunctionExpression' ||
           node.type === 'FunctionDeclaration';
  }
  
  function isTypeOf(node) {
    return (node.type === 'UnaryExpression' &&
            node.operator === 'typeof' &&
            !node.synthetic);
  }
  
  function isVariableDecl(node) {
    return (node.type === 'VariableDeclaration');
  }

  /**
   * Rewrite var decls in place into assignments on the global object
   * turning expression "var x, y = 2, z" to
   * window.x = window.x, window.y = 2, window.z = window.z
   */
  function rewriteVars(node) {
    var assignments = [];
    node.declarations.forEach(function(decl) {
      assignments.push({
        "type": "AssignmentExpression",
        "operator": "=",
        "left": globalVarAst(decl.id),
        "right": decl.init || globalVarAst(decl.id)
      });
  
    });
    node.type = "ExpressionStatement";
    node.expression = {
      "type": "SequenceExpression",
      "expressions": assignments
    };
  }
  
  function globalVarAst(varName) {
    return {
      "type": "MemberExpression",
      "object": {
        "type": "Identifier",
        "name": "window"
      },
      "property": varName
    };
  }
  
  /**
   * Rewrite node in place turning expression "typeof x" to
   * (function() { try { typeof x } catch (e) { return "undefined" } })()
   */
  function rewriteTypeOf(node) {
    var arg = node.argument;
    node.type = "CallExpression";
    node.arguments = [];
    node.callee = {
        "type": "FunctionExpression",
        "id": null,
        "params": [],
        "body": {
          "type": "BlockStatement",
          "body": [{
              "type": "TryStatement",
              "block": {
                "type": "BlockStatement",
                "body": [{
                    "type": "ReturnStatement",
                    "argument": {
                      "synthetic": true,
                      "type": "UnaryExpression",
                      "operator": "typeof",
                      "prefix": true,
                        "argument": arg
                    }
                  }
                ]
              },
              "handlers": [{
                  "type": "CatchClause",
                  "param": {
                    "type": "Identifier",
                    "name": "e"
                  },
                  "guard": null,
                  "body": {
                    "type": "BlockStatement",
                    "body": [{
                      "type": "ReturnStatement",
                      "argument": {
                        "type": "Literal",
                        "value": "undefined",
                        "raw": "'undefined'"
                      }
                    }]
                  }
                }
              ],
              "finalizer": null
            }
          ]
        }
    };
  }
  
  function resolveOptions(options) {
    function resolve(opt, defaultOption) {
      return (options && opt in options) ? opt : defaultOption;
    }
    var resolved = {};
    resolved.rewriteTopLevelVars = resolve('rewriteTopLevelVars', true);
    resolved.rewriteTypeOf = resolve('rewriteTypeOf', true);
    return resolved;
  }

  ses.mitigateGotchas = function(programSrc, options) {
    try {
      options = resolveOptions(options);
      var scopeLevel = 0;
      var ast = ses.rewriter_.parse(programSrc);
      ses.rewriter_.traverse(ast, {
        enter: function enter(node) {
            if (introducesScope(node)) {
              scopeLevel++;
            } else if (options.rewriteTypeOf && isTypeOf(node)) {
              rewriteTypeOf(node);
            } else if (options.rewriteTopLevelVars &&
                       isVariableDecl(node) && scopeLevel === 0) {
              rewriteVars(node);
            }
        },
        leave: function leave(node) {
            if (introducesScope(node)) {
              scopeLevel--;
            }
        }
      });
      return ses.rewriter_.generate(ast);
    } catch (e) {
      ses.logger.warn("Failed to mitigate SES gotchas. Proceeding anyways.", e);
      return programSrc;
    }
  };

})();
