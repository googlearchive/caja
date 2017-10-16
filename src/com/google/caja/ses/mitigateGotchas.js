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
  // There is a bug which is problematic for us: JS implementations vary on
  // whether they consider a reserved word written using unicode escapes to
  // actually be a reserved word; for example, the following program can be
  // parsed two ways:
  //    de\u006Cete /"x/ //";
  //
  // The specification is somewhat unclear but the consensus is that the correct
  // behavior is to parse an escaped reserved word as a reserved word. See:
  //   https://bugs.ecmascript.org/show_bug.cgi?id=277
  //   https://code.google.com/p/v8/issues/detail?id=2222
  //   https://bugzilla.mozilla.org/show_bug.cgi?id=744784
  //   https://bugzilla.mozilla.org/show_bug.cgi?id=694360
  //   https://bugs.webkit.org/show_bug.cgi?id=90678
  //
  // This has two consequences for us:
  // 1. We cannot rely on the JS implementation to parse such a program in the
  //    same way we do (unless we were to vary our parser's behavior based on a
  //    feature test, which we currently do not), so if we parse a program, then
  //    we must also rerender it in an unambiguous way matching our
  //    interpretation (rather than passing the original source to the browser's
  //    parser); but our parser, Acorn, takes the incorrect "de\u006Cete is an
  //    identifier" option which cannot be supported on all platforms.
  // 2. Our parser, Acorn, and renderer, escodegen, are inconsistent with each
  //    other: an escaped reserved word is parsed as an identifier, but will be
  //    rendered unescaped.
  //
  // Due to the above issues, and since such programs are therefore unportable,
  // we currently take the simplest approach, namely to reject such programs.
  // In order to do so, we must traverse all programs we parse.

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

  var nameIsReservedWord = (function() {
    // TODO(kpreid): Fragile; find a better way to do this that does not depend
    // on Acorn's data structures quite so much.
    var tokTypes = ses.rewriter_.tokTypes;
    var table = new Set();
    for (var k in tokTypes) {
      if ('keyword' in tokTypes[k]) {
        table.add(tokTypes[k].keyword, 0);
      }
    }
    return table.has.bind(table);
  })();

  // Is the node one whose identifier child is an IdentifierName in the ES5
  // grammar, and therefore allowed to be a ReservedWord?
  function isIdentifierNameContext(node) {
    var type = node.type;
    // property initializer nodes have no .type; they have a .kind but so do
    // labels
    return type === 'MemberExpression' || !!(node.kind && node.key);
  }

  /**
   * Rewrite func decls in place by appending assignments on the global object
   * turning expression "function x() {}" to
   * function x(){}; global.x = x;
   */
  function rewriteFuncDecl(scope, node, parentNode) {
    var exprNode = {
      'type': 'ExpressionStatement',
      'expression': {
        'type': 'AssignmentExpression',
        'operator': '=',
        'left': globalVarAst(node.id),
        'right': node.id
      }
    };
    var body = parentNode.body;
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
  function rewriteVars(scope, node, parentNode) {

    // TODO(jasvir): Consider mitigating top-level vars in for..in
    // loops.  We currently do not support rewriting var declarations
    // in the VarDeclarator of a ForInStatement.  Given for (var x in
    // y) { var z; }, we do not rewrite var x.  This is because our
    // standard local rewrite for var decls is incorrect in this case.

    // We can support rewriting these vars iff requested.

    if (parentNode.type === 'ForInStatement') {
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
    if (parentNode.type === 'ForStatement') {
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

  function rewrite(scope, node) {
    ses.rewriter_.traverse(node, {
      enter: function enter(node, parentNode) {

          if (isId(node)) {
            if (nameIsReservedWord(node.name) &&
                !isIdentifierNameContext(parentNode)) {
              throw new SyntaxError(
                  'Programs containing Unicode escapes in reserved words ' +
                  'will be misparsed on some platforms and are not currently ' +
                  'permitted by SES.');
            }
          }

          if (scope.options.rewriteTopLevelFuncs &&
              isFunctionDecl(node) && scope.scopeLevel === 0) {
            rewriteFuncDecl(scope, node, parentNode);
            scope.dirty = true;
          } else if (scope.options.rewriteTypeOf &&
              isTypeOf(node) && isId(node.argument)) {
            rewriteTypeOf(scope, node);
            scope.dirty = true;
          } else if (scope.options.rewriteTopLevelVars &&
                     isVariableDecl(node) && scope.scopeLevel === 0) {
            rewriteVars(scope, node, parentNode);
            scope.dirty = true;
          } else if (scope.options.rewriteFunctionCalls &&
                     isFunctionCall(node)) {
            rewriteFunctionCall(scope, node);
            scope.dirty = true;
          }

          if (introducesVarScope(node)) {
            scope.scopeLevel++;
          }
      },
      leave: function leave(node) {
          if (introducesVarScope(node)) {
            scope.scopeLevel--;
          }
      }
    });
    return node;
  }

  function rewriteProgram(options, ast) {
    var scope = {
      options: options,
      dirty: false,
      scopeLevel: 0
    };
    rewrite(scope, ast);
    if (scope.scopeLevel !== 0) {
      throw new Error('Internal error traversing the AST');
    }
    return scope.dirty;
  }

  /**
   * if asExpr, then src should be the code for an expression.
   * Otherwise, src should be the code for a function body.
   * The only difference between a Program and a FunctionBody is that
   * the latter admits return statements.
   *
   * Assumes {@code options} have already been safely canonicalized by
   * startSES's {@code resolveOptions}.
   */
  ses.mitigateSrcGotchas = function(asExpr, src, options, logger) {
    if (asExpr) {
      src = '(' + src + '\n);';
    } else {
      src = '(function() { ' +
        src +
        '\n});\n';
    }
    // src should now be code for an ExpressionStatement wrapping the
    // original.
    try {
      // TODO(erights): Is it safe not to set forbidReserved: true ?
      var ast = ses.rewriter_.parse(src, {forbidReserved: false});
      if (ast.type !== 'Program') {
        throw new SyntaxError('Internal malformed parse: ' + src);
      }
      if (ast.body.length !== 1) {
        throw new SyntaxError('Expected an expression: ' + src);
      }
      if (ast.body[0].type !== 'ExpressionStatement') {
        throw new SyntaxError('Expected expression: ' + src);
      }
      ast = ast.body[0].expression;
      // ast is now the parsed expression within that expression
      // statement.
      if (!asExpr) {
        if (ast.type !== 'FunctionExpression') {
          throw new SyntaxError('Internal: expected function: ' + src);
        }
        ast = ast.body;
        if (ast.type === 'BlockStatement') {
          // There is no ast type for FunctionBody, which is really
          // how the src got parsed. By changing to type Program, it
          // gets rendered correctly, without the enclosing
          // curlies. The difference is that FunctionBody can contain
          // a return statement and Program cannot. However, this
          // difference does not impede rendering.
          ast.type = 'Program';
        }
      }
      rewriteProgram(options, ast);
      return ses.rewriter_.generate(ast);
    } catch (e) {
      var message = '' + e;
      // Chrome console does not display an Error object usefully but as
      // "Error {}" so we also log it.
      logger.warn('Failed to parse program: ' + message);
      throw e;
    }
  };

})();
