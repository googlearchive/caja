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
 * by rewriting programs where possible.
 * See http://code.google.com/p/google-caja/wiki/SES#Source-SES_vs_Target-SES
 * for a list of these differences.
 *
 * TODO(jasvir): Rewrite top level function declarations
 *
 * Note that the parse tree manipulated in this file uses the SpiderMonkey
 * AST format
 * (https://developer.mozilla.org/en-US/docs/SpiderMonkey/Parser_API)
 *
 * //requires ses.rewriter_
 * //provides ses.mitigateSrcGotchas
 * @author Jasvir Nagra (jasvir@google.com)
 * @overrides ses
 */

var ses;

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
    return node.type === 'VariableDeclaration';
  }

  function isFunctionDecl(node) {
    return node.type === 'FunctionDeclaration';
  }

  // (o[3]), (o['p']), and (o.p) are static key property access expressions
  // (o[x]) for some reference 'x' is not static
  function isStaticKeyPropertyAccess(node) {
    return node.type === 'MemberExpression' &&
           (node.computed
               ? node.property.type === 'Literal'
               : true);
  }

  function isStaticKeyPropertyUpdateExpr(node) {
    return node.type === 'UpdateExpression' &&
           isStaticKeyPropertyAccess(node.argument);
  }

  function isStaticKeyPropertyCompoundAssignmentExpr(node) {
    return node.type === 'AssignmentExpression' &&
           node.operator.length > 1 &&
           node.operator[node.operator.length - 1] === '=' &&
           isStaticKeyPropertyAccess(node.left);
  }

  /**
   * Detects a call expression where the callee is an identifier.
   *
   * <p>This case is interesting because evaluating an identifier
   * evaluates to a reference that potentially has a base. Even when
   * the call is in strict code, if the identifier is defined by a
   * {@code with} statement, then the function would be called with
   * its {@code this} bound to the with's scope object. In this way,
   * {@code with} fails to emulate the global scope.
   *
   * <p>See <a href=
   * "https://code.google.com/p/google-caja/issues/detail?id=1755"
   * >Issue 1755: Need rewriteFunctionCalls mitigation</a>
   */
  function isFunctionCall(node) {
    return node.type === 'CallExpression' && isId(node.callee);
  }

  function parentNode(scope) {
    return scope.path[scope.path.length - 2];
  }

  /**
   * Rewrite func decls in place by appending assignments on the global object
   * turning expression "function x() {}" to
   * function x(){}; global.x = x;
   */
  function rewriteFuncDecl(scope, node) {
    var exprNode = {
      'type': 'ExpressionStatement',
      'expression': {
        'type': 'AssignmentExpression',
        'operator': '=',
        'left': globalVarAst(node.id),
        'right': node.id
      }
    };
    var body = parentNode(scope).body;
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
  function rewriteVars(scope, node) {

    // TODO(jasvir): Consider mitigating top-level vars in for..in
    // loops.  We currently do not support rewriting var declarations
    // in the VarDeclarator of a ForInStatement.  Given for (var x in
    // y) { var z; }, we do not rewrite var x.  This is because our
    // standard local rewrite for var decls is incorrect in this case.

    // We can support rewriting these vars iff requested.

    if (parentNode(scope).type === 'ForInStatement') {
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
    if (parentNode(scope).type === 'ForStatement') {
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
  function rewriteTypeOf(scope, node) {
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

  /**
   * Rewrites a function call, e.g., {@code f(x, y)} to, e.g.,
   * {@code (1,f)(x, y)} to prevent it from implicitly passing the
   * callee's base as the {@code this}-binding, in case the callee
   * evaluates to a reference.
   *
   * <p>See <a href=
   * "https://code.google.com/p/google-caja/issues/detail?id=1755"
   * >Issue 1755: Need rewriteFunctionCalls mitigation</a>
   */
  function rewriteFunctionCall(scope, node) {
    node.callee = makeTrivialSequenceExpression(node.callee);
  }

  function makeTrivialSequenceExpression(rhs) {
    return {
      type: 'SequenceExpression',
      expressions: [
        {
          type: 'Literal',
          value: 1
        },
        rhs
      ]
    };
  }

  function rewriteStaticKeyMemberExpression(scope, node) {
    rewrite(scope, node.object);
    switch (node.property.type) {
     case 'Identifier':
       node.property = makeTrivialSequenceExpression({
          type: 'Literal',
          value: node.property.name
        });
       break;
     case 'Literal':
       node.property = makeTrivialSequenceExpression(node.property);
       break;
     default:
       // Inconsistent
       throw new Error('Programming error');
    }
    node.computed = true;
  }

  function needsRewriting(options) {
    return options.rewriteTopLevelVars ||
      options.rewriteTopLevelFuncs ||
      options.rewriteFunctionCalls ||
      options.rewriteTypeOf ||
      options.rewritePropertyUpdateExpr ||
      options.rewritePropertyCompoundAssignmentExpr;
  }

  function rewrite(scope, node) {
    ses.rewriter_.traverse(node, {
      enter: function enter(node) {
          scope.path.push(node);

          if (scope.options.rewriteTopLevelFuncs &&
              isFunctionDecl(node) && scope.scopeLevel === 0) {
            rewriteFuncDecl(scope, node);
            scope.dirty = true;
          } else if (scope.options.rewriteTypeOf &&
              isTypeOf(node) && isId(node.argument)) {
            rewriteTypeOf(scope, node);
            scope.dirty = true;
          } else if (scope.options.rewriteTopLevelVars &&
                     isVariableDecl(node) && scope.scopeLevel === 0) {
            rewriteVars(scope, node);
            scope.dirty = true;
          } else if (scope.options.rewriteFunctionCalls &&
                     isFunctionCall(node)) {
            rewriteFunctionCall(scope, node);
            scope.dirty = true;
          } else if (scope.options.rewritePropertyUpdateExpr &&
                     isStaticKeyPropertyUpdateExpr(node)) {
            rewriteStaticKeyMemberExpression(scope, node.argument);
            scope.dirty = true;
          } else if (scope.options.rewritePropertyCompoundAssignmentExpr &&
                     isStaticKeyPropertyCompoundAssignmentExpr(node)) {
            rewriteStaticKeyMemberExpression(scope, node.left);
            rewrite(scope, node.right);
            scope.dirty = true;
          }

          if (introducesVarScope(node)) {
            scope.scopeLevel++;
          }
      },
      leave: function leave(node) {
          var last = scope.path.pop();
          if (node !== last) {
            throw new Error('Internal error traversing the AST');
          }
          if (introducesVarScope(node)) {
            scope.scopeLevel--;
          }
      }
    });
    return node;
  }

  function rewriteProgram(options, ast) {
    if (needsRewriting(options)) {
      var scope = {
        options: options,
        dirty: false,
        path: [],
        scopeLevel: 0
      };
      rewrite(scope, ast);
      return scope.dirty;
    } else {
      return false;
    }
  }

  /**
   * Assumes {@code options} have already been safely canonicalized by
   * startSES's {@code resolveOptions}.
   */
  ses.mitigateSrcGotchas = function(funcBodySrc, options, logger) {
    if (!needsRewriting(options) && !options.parseFunctionBody) {
      return funcBodySrc;
    }
    try {
      var ast = ses.rewriter_.parse(funcBodySrc);
      var dirty = rewriteProgram(options, ast);
      if (dirty || options.forceParseAndRender) {
        return "\n"
            + "/*\n"
            + " * Program rewritten to mitigate differences between\n"
            + " * Caja and strict-mode JavaScript.\n"
            + " * For more see "
            + " * https://code.google.com/p/google-caja/wiki/SES#"
            + "Source-SES_vs_Target-SES\n"
            + " */\n"
            + ses.rewriter_.generate(ast);
      } else {
        return funcBodySrc;
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
