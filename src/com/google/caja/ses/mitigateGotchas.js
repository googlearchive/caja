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
  function introducesVarScope(node) {
    return node.type === 'FunctionExpression' ||
           node.type === 'FunctionDeclaration';
  }
  
  function isTypeOf(node) {
    return (node.type === 'UnaryExpression' &&
            node.operator === 'typeof' &&
            !node.synthetic);
  }

  function isId(node) {
    return node.type === 'Identifier';
  }
  
  function isVariableDecl(node) {
    return (node.type === 'VariableDeclaration');
  }

  function isFunctionDecl(node) {
    return (node.type === 'FunctionDeclaration');
  }

  /**
   * Rewrite func decls in place by appending assignments on the global object
   * turning expression "function x() {}" to
   * function x(){}; global.x = x;
   */
  function rewriteFuncDecl(node, parent) {
    var exprNode = {
      'type': 'ExpressionStatement',
      'expression': {
        'type': 'AssignmentExpression',
        'operator': '=',
        'left': globalVarAst(node.id),
        'right': node.id
      }
    };
    var body = parent.body;
    var currentIdx = body.indexOf(node);
    var nextIdx = currentIdx + 1;

    // Insert assignment immediately after FunctionDecl
    body.splice(nextIdx, 0, exprNode);
  }

  /**
   * Rewrite var decls in place into assignments on the global object
   * turning variable declaration "var x, y = 2, z" to an expression
   * statement: 
   * "this.x = this.x, this.y = this.y, this.y = 2, this.z = this.z"
   * The rewrite also rewrites var declarations that appear in a for-loop
   * initializer "for (var x = 1;;) {}" into an expression: 
   * "for (this.x = this.x, this.x = 1;;) {}"
   */
  function rewriteVars(node, parent) {

    // TODO(jasvir): Consider mitigating top-level vars in for..in
    // loops.  We currently do not support rewriting var declarations
    // in the VarDeclarator of a ForInStatement.  Given for (var x in
    // y) { var z; }, we do not rewrite var x.  This is because our
    // standard local rewrite for var decls is incorrect in this case.

    // We can support rewriting these vars iff requested.

    if (parent.type === 'ForInStatement') {
      return;
    }
    var assignments = [];
    node.declarations.forEach(function(decl) {
      assignments.push({
        'type': 'AssignmentExpression',
        'operator': '=',
        'left': globalVarAst(decl.id),
        'right': globalVarAst(decl.id)
      });
      if (decl.init) {
        assignments.push({
          'type': 'AssignmentExpression',
          'operator': '=',
          'left': globalVarAst(decl.id),
          'right': decl.init
        });
      }
    });
    if (parent.type === 'ForStatement') {
      node.type = 'SequenceExpression';
      node.expressions = assignments;
    } else {
      node.type = 'ExpressionStatement';
      node.expression = {
        type: 'SequenceExpression',
        expressions: assignments
      };
    }
  }
  
  function globalVarAst(varName) {
    return {
      'type': 'MemberExpression',
      'object': {
        'type': 'ThisExpression'
      },
      'property': varName
    };
  }
  
  /**
   * Rewrite node in place turning expression "typeof x" to
   * (function() {
   *   try { return typeof x; } catch (e) { return "undefined"; }
   * })()
   */
  function rewriteTypeOf(node) {
    var arg = node.argument;
    node.type = 'CallExpression';
    node.arguments = [];
    node.callee = {
        'type': 'FunctionExpression',
        'id': null,
        'params': [],
        'body': {
          'type': 'BlockStatement',
          'body': [{
              'type': 'TryStatement',
              'block': {
                'type': 'BlockStatement',
                'body': [{
                    'type': 'ReturnStatement',
                    'argument': {
                      'synthetic': true,
                      'type': 'UnaryExpression',
                      'operator': 'typeof',
                      'prefix': true,
                        'argument': arg
                    }
                  }
                ]
              },
              'handlers': [{
                  'type': 'CatchClause',
                  'param': {
                    'type': 'Identifier',
                    'name': 'e'
                  },
                  'guard': null,
                  'body': {
                    'type': 'BlockStatement',
                    'body': [{
                      'type': 'ReturnStatement',
                      'argument': {
                        'type': 'Literal',
                        'value': 'undefined',
                        'raw': '\'undefined\''
                      }
                    }]
                  }
                }
              ],
              'finalizer': null
            }
          ]
        }
    };
  }
  
  function resolveOptions(options, logger) {
    function resolve(opt, defaultOption) {
      return (options && opt in options) ? options[opt] : defaultOption;
    }
    var resolved = {};
    if (options === undefined || options === null) {
      resolved.parseProgram = true;
      resolved.rewriteTopLevelVars = true;
      resolved.rewriteTopLevelFuncs = true;
      resolved.rewriteTypeOf = true;
    } else {
      if (options.parseProgram === false) {
        logger.warn('Refused to disable parsing for safety on all browsers');
      }
      // TODO(jasvir): This should only be necessary if a to-be-added
      // test in repairES5.js indicates that this platform has the
      // Function constructor bug
      resolved.parseProgram = true;
      resolved.rewriteTopLevelVars = resolve('rewriteTopLevelVars', true);
      resolved.rewriteTopLevelFuncs = resolve('rewriteTopLevelFuncs', true);
      resolved.rewriteTypeOf = resolve('rewriteTypeOf', true);
    }
    return resolved;
  }

  function needsRewriting(options) {
    return options.rewriteTopLevelVars ||
      options.rewriteTopLevelFuncs ||
      options.rewriteTypeOf;
  }

  ses.mitigateGotchas = function(programSrc, options, logger) {
    options = resolveOptions(options, logger);
    if (!options.parseProgram) {
      return programSrc;
    }
    try {
      var dirty = false;
      var path = [];
      var scopeLevel = 0;
      var ast = ses.rewriter_.parse(programSrc);
      if (!needsRewriting(options)) {
        return programSrc;
      }
      ses.rewriter_.traverse(ast, {
        enter: function enter(node) {
            var parent = path[path.length - 1];
            path.push(node);

            if (options.rewriteTopLevelFuncs &&
                isFunctionDecl(node) && scopeLevel === 0) {
              rewriteFuncDecl(node, parent);
              dirty = true;
            } else if (options.rewriteTypeOf &&
                isTypeOf(node) && isId(node.argument)) {
              rewriteTypeOf(node);
              dirty = true;
            } else if (options.rewriteTopLevelVars &&
                       isVariableDecl(node) && scopeLevel === 0) {
              rewriteVars(node, parent);
              dirty = true;
            }

            if (introducesVarScope(node)) {
              scopeLevel++;
            }
        },
        leave: function leave(node) {
            var last = path.pop();
            if (node !== last) {
              throw new Error('Internal error traversing the AST');
            }
            if (introducesVarScope(node)) {
              scopeLevel--;
            }
        }
      });
      if (dirty) {
        return "\n"
            + "/*\n"
            + " * Program rewritten to mitigate differences between\n"
            + " * Caja and strict-mode JavaScript.\n"
            + " * For more see http://code.google.com/p/google-caja/wiki/SES\n"
            + " */\n"
            + ses.rewriter_.generate(ast);
      } else {
        return programSrc;
      }
    } catch (e) {
      logger.warn('Failed to parse program', e);
      // TODO(jasvir): Consider using the thrown exception to provide
      // a more useful descriptive error message.  Be aware of naively
      // interpolating error message strings.
      return '' +
        '(function() { throw new SyntaxError("Failed to parse program"); })()';
    }
  };

})();
