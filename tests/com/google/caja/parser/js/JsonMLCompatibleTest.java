// Copyright (C) 2010 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.caja.parser.js;

import java.util.Collections;

import com.google.caja.lexer.ParseException;
import com.google.caja.lexer.SourceBreaks;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.util.CajaTestCase;
import com.google.javascript.jscomp.jsonml.JsonML;
import com.google.javascript.jscomp.jsonml.TagAttr;

public class JsonMLCompatibleTest extends CajaTestCase {
  public final void testToJsonML() throws Exception {
    // These tests are adapted from es-lab.

    // This
    testExpression("this", "[ 'ThisExpr', {} ]");

    // Identifiers
    testExpression("x", "[ 'IdExpr', { name : 'x' } ]");

    // Literals
    testExpression(
        "10",    "[ 'LiteralExpr', { type: 'number',  value: 10 } ]");
    testExpression(
        "'foo'", "[ 'LiteralExpr', { type: 'string',  value: 'foo' } ]");
    testExpression(
        "'foo'", "[ 'LiteralExpr', { type: 'string',  value: 'foo' } ]");
    testExpression(
        "true",  "[ 'LiteralExpr', { type: 'boolean', value: true } ]");
    testExpression(
        "false", "[ 'LiteralExpr', { type: 'boolean', value: false } ]");
    testExpression(
        "null",  "[ 'LiteralExpr', { type: 'null',    value: null } ]");

    // Regular Expression 'literals'
    testExpression(
        "/foo(.*)/g", "[ 'RegExpExpr', { body: \"foo(.*)\", flags: 'g' } ]");

    // Array 'Literals'
    testExpression("[]", "[ 'ArrayExpr', {} ]");
    testExpression("[   ]", "[ 'ArrayExpr', {} ]");
    testExpression(
        "[1]",
        "[ 'ArrayExpr', {}, [ 'LiteralExpr', { type: 'number',  value: 1 } ] ]"
        );
    testExpression(
        "[1,2]",
        "[ 'ArrayExpr', {}, [ 'LiteralExpr', { type: 'number',  value: 1 } ]," +
        "[ 'LiteralExpr', { type: 'number',  value: 2 } ] ]");
    testExpression(
        "[1,2,,]",
        "[ 'ArrayExpr', {}, [ 'LiteralExpr', { type: 'number',  value: 1 } ]," +
        "[ 'LiteralExpr', { type: 'number',  value: 2 } ], ['Empty', {}] ]");
    testExpression(
        "[1,2,3]",
        "[ 'ArrayExpr', {}, [ 'LiteralExpr', { type: 'number',  value: 1 } ]," +
        "[ 'LiteralExpr', { type: 'number',  value: 2 } ]," +
        "[ 'LiteralExpr', { type: 'number',  value: 3 } ] ]");
    testExpression(
        "[1,2,3,,,]",
        "[ 'ArrayExpr', {}, [ 'LiteralExpr', { type: 'number',  value: 1 } ]," +
        "[ 'LiteralExpr', { type: 'number',  value: 2 } ]," +
        "[ 'LiteralExpr', { type: 'number',  value: 3 } ]," +
        "['Empty', {}],['Empty', {}] ]");

    // Object 'Literals'
    testExpression("{}", "[ 'ObjectExpr', {} ]");
    testExpression(
        "{x:5}",
        "[ 'ObjectExpr', {}, ['DataProp', { name: 'x' }," +
        "[ 'LiteralExpr', { type: 'number',  value: 5 } ] ] ]");
    testExpression(
        "{x:5,y:6}",
        "[ 'ObjectExpr', {}, ['DataProp', { name: 'x' }," +
        " [ 'LiteralExpr', { type: 'number',  value: 5 } ] ]," +
        " ['DataProp', { name: 'y' }," +
        " [ 'LiteralExpr', { type: 'number',  value: 6 } ] ] ]");
    testExpression(
        "{x:5,}",
        "[ 'ObjectExpr', {}, ['DataProp', { name: 'x' }," +
        " [ 'LiteralExpr', { type: 'number',  value: 5 } ] ] ]");

    testExpression(
        "{if:5}",
        "[ 'ObjectExpr', {}, ['DataProp', { name: 'if' }," +
        " [ 'LiteralExpr', { type: 'number',  value: 5 } ] ] ]");

    testExpression(
        "{ get x() {this;} }",
        "['ObjectExpr', {}, [ 'GetterProp', {name:'x'}, ['FunctionExpr', {}," +
        " ['Empty', {}], ['ParamDecl',{}]," +
        " ['ThisExpr', {}] ] ] ]");
    testExpression(
        "{ set y(a) {this;} }",
        "['ObjectExpr', {}, [ 'SetterProp', {name:'y'}, ['FunctionExpr', {}," +
        " ['Empty', {}], ['ParamDecl',{},['IdPatt', {name:'a'}]]," +
        " ['ThisExpr', {}] ] ] ]");

    // MemberExpressions
    testExpression(
        "o.m",
        "[ 'MemberExpr', { 'op': '.' }, [ 'IdExpr', {name:'o'} ]," +
        " [ 'LiteralExpr',{type: 'string', value: 'm'} ] ]");
    testExpression(
        "o['m']",
        "[ 'MemberExpr', { 'op': \"[]\" }, [ 'IdExpr', {name:'o'} ]," +
        " [ 'LiteralExpr',{type: 'string', value: 'm'} ] ]");
    testExpression(
        "o['n']['m']",
        "[ 'MemberExpr', { 'op': \"[]\" }, [ 'MemberExpr', { 'op': \"[]\" }," +
        " [ 'IdExpr', {name:'o'} ]," +
        " [ 'LiteralExpr',{type:'string',value: 'n'}]]," +
        " [ 'LiteralExpr',{type: 'string', value: 'm'} ] ]");
    testExpression(
        "o.n.m",
        "[ 'MemberExpr', { 'op': '.' }, [ 'MemberExpr', { 'op': '.' }," +
        " [ 'IdExpr', {name:'o'} ]," +
        " [ 'LiteralExpr',{type:'string',value: 'n'}]]," +
        " [ 'LiteralExpr',{type:'string', value: 'm'} ] ]");
    testExpression(
        "o.if",
        "[ 'MemberExpr', { 'op': '.' }, [ 'IdExpr', {name:'o'} ]," +
        " [ 'LiteralExpr',{type: 'string', value: 'if'} ] ]");

    // CallExpressions and InvokeExpressions
    testExpression("f()", "[ 'CallExpr',{},['IdExpr',{name:'f'}] ]");
    testExpression(
        "f(x)",
        "[ 'CallExpr',{},['IdExpr',{name:'f'}], ['IdExpr',{name:'x'}] ]");
    testExpression(
        "f(x,y)",
        "[ 'CallExpr',{},['IdExpr',{name:'f'}], ['IdExpr',{name:'x'}]," +
        " ['IdExpr',{name:'y'}] ]");
    testExpression(
        "o.m()",
        "[ 'InvokeExpr',{ 'op': '.' },['IdExpr',{name:'o'}]," +
        " ['LiteralExpr',{ type: 'string', value:'m' }] ]");
    testExpression(
        "o['m']()",
        "[ 'InvokeExpr',{ 'op': \"[]\" },['IdExpr',{name:'o'}]," +
        " ['LiteralExpr',{ type: 'string', value:'m' }] ]");
    testExpression(
        "o.m(x)",
        "[ 'InvokeExpr',{ 'op': '.' },['IdExpr',{name:'o'}]," +
        " ['LiteralExpr',{ type: 'string', value:'m' }]," +
        " ['IdExpr',{name:'x'}] ]");
    testExpression(
        "o['m'](x)",
        "[ 'InvokeExpr',{ 'op': \"[]\" },['IdExpr',{name:'o'}]," +
        " ['LiteralExpr',{ type: 'string', value:'m' }]," +
        " ['IdExpr',{name:'x'}] ]");
    testExpression(
        "o.m(x,y)",
        "[ 'InvokeExpr',{ 'op': '.' },['IdExpr',{name:'o'}]," +
        " ['LiteralExpr',{ type: 'string', value:'m' }]," +
        " ['IdExpr',{name:'x'}], ['IdExpr',{name:'y'}] ]");
    testExpression(
        "o['m'](x,y)",
        "[ 'InvokeExpr',{ 'op': \"[]\" },['IdExpr',{name:'o'}]," +
        " ['LiteralExpr',{ type: 'string', value:'m' }]," +
        " ['IdExpr',{name:'x'}], ['IdExpr',{name:'y'}] ]");

    testExpression(
        "f(x)(y)",
        "[ 'CallExpr', {}, [ 'CallExpr',{}, ['IdExpr',{name:'f'}]," +
        " ['IdExpr',{name:'x'}] ]," +
        " ['IdExpr',{name:'y'}] ]");
    testExpression(
        "f().x",
        "[ 'MemberExpr', { 'op': '.' }, [ 'CallExpr',{}," +
        "['IdExpr',{name:'f'}] ], ['LiteralExpr', {type:'string',value:'x'}]]");

    // EvalExpressions (identify possible uses of a 'direct call' to eval)
    testExpression(
        "eval('x')",
        "['EvalExpr', {}, ['LiteralExpr',{ type: 'string', value:'x' }] ]");
    testExpression(
        "(eval)('x')",
        "['EvalExpr', {}, ['LiteralExpr',{ type: 'string', value:'x' }] ]");
    testExpression(
        "(1,eval)('x')",
        "['CallExpr', {}, ['BinaryExpr', {op:','}," +
        " [ 'LiteralExpr', { type: 'number',  value: 1 } ]," +
        " ['IdExpr',{name:'eval'}] ]," +
        " ['LiteralExpr',{ type: 'string', value:'x' }] ]");
    testExpression(
        "eval(x,y)",
        "['EvalExpr', {}, ['IdExpr',{name:'x'}], ['IdExpr',{name:'y'}] ]");

    // NewExpressions
    testExpression("new f()", "[ 'NewExpr', {}, ['IdExpr', { name: 'f' } ] ]");
    testExpression("new o", "[ 'NewExpr', {}, ['IdExpr',{name:'o'}] ]");
    testExpression(
        "new o.m",
        "['NewExpr', {}, ['MemberExpr', { 'op': '.' }, ['IdExpr',{name:'o'}]," +
        " ['LiteralExpr',{ type: 'string', value:'m' }] ] ]");
    testExpression(
        "new o.m(x)",
        "['NewExpr', {}, ['MemberExpr', { 'op': '.' }, ['IdExpr',{name:'o'}]," +
        " ['LiteralExpr',{ type: 'string', value:'m' }] ]," +
        " ['IdExpr',{name:'x'}] ]");
    testExpression(
        "new o.m(x,y)",
        "['NewExpr', {}, ['MemberExpr', { 'op': '.' }, ['IdExpr',{name:'o'}]," +
        " ['LiteralExpr',{ type: 'string', value:'m' }] ]," +
        " ['IdExpr',{name:'x'}]," +
        " ['IdExpr',{name:'y'}] ]");

    // pre- and postfix increment and decrement (CountExpr)
    testExpression(
        "++x",
        "['CountExpr', {isPrefix: true,  op: \"++\"}, ['IdExpr',{name:'x'}] ]");
    testExpression(
        "x++",
        "['CountExpr', {isPrefix: false, op: \"++\"}, ['IdExpr',{name:'x'}] ]");
    testExpression(
        "--x",
        "['CountExpr', {isPrefix: true,  op: \"--\"}, ['IdExpr',{name:'x'}] ]");
    testExpression(
        "x--",
        "['CountExpr', {isPrefix: false, op: \"--\"}, ['IdExpr',{name:'x'}] ]");

    // spaces before ++ allowed
    testExpression(
        "x  ++",
        "['CountExpr', {isPrefix: false, op: \"++\"}, ['IdExpr',{name:'x'}] ]");

    // delete
    testExpression("delete x", "[ 'DeleteExpr', {}, ['IdExpr',{name:'x'}] ]");

    // Unary Expressions
    testExpression(
        "void x",
         "[ 'UnaryExpr', {op:'void'}, ['IdExpr',{name:'x'}] ]");
    testExpression(
        "+ x",
         "[ 'UnaryExpr', {op:'+'   }, ['IdExpr',{name:'x'}] ]");
    testExpression(
        "-x",
         "[ 'UnaryExpr', {op:'-'   }, ['IdExpr',{name:'x'}] ]");
    testExpression(
        "~x",
         "[ 'UnaryExpr', {op:'~'   }, ['IdExpr',{name:'x'}] ]");
    testExpression(
        "!x",
         "[ 'UnaryExpr', {op:'!'   }, ['IdExpr',{name:'x'}] ]");

    // precedence of postfix and unary operators
    // +(x++)
    testExpression(
        "+x++",
        "[ 'UnaryExpr', {op:'+'}, ['CountExpr', {isPrefix:false,op:\"++\"}," +
        " ['IdExpr',{name:'x'}] ] ]");

    // typeof
    testExpression("typeof x", "[ 'TypeofExpr', {}, ['IdExpr',{name:'x'}] ]");

    // Expression Expressions
    testExpression(
        "1 * 2",
        "[ 'BinaryExpr', {op: '*'}," +
        " [ 'LiteralExpr', { type: 'number',  value: 1 } ]," +
        " [ 'LiteralExpr', { type: 'number',  value: 2 } ] ]");
    testExpression(
        "1 / 2",
        "[ 'BinaryExpr', {op: '/'}," +
        " [ 'LiteralExpr', { type: 'number',  value: 1 } ]," +
        " [ 'LiteralExpr', { type: 'number',  value: 2 } ] ]");
    testExpression(
        "1 % 2",
        "[ 'BinaryExpr', {op: '%'}," +
        " [ 'LiteralExpr', { type: 'number',  value: 1 } ]," +
        " [ 'LiteralExpr', { type: 'number',  value: 2 } ] ]");
    testExpression(
        "1 + 2",
        "[ 'BinaryExpr', {op: '+'}," +
        " [ 'LiteralExpr', { type: 'number',  value: 1 } ]," +
        " [ 'LiteralExpr', { type: 'number',  value: 2 } ] ]");
    testExpression(
        "1 - 2",
        "[ 'BinaryExpr', {op: '-'}," +
        " [ 'LiteralExpr', { type: 'number',  value: 1 } ]," +
        " [ 'LiteralExpr', { type: 'number',  value: 2 } ] ]");
    testExpression(
        "1 << 2",
        "[ 'BinaryExpr', {op: \"<<\"}," +
        " [ 'LiteralExpr', { type: 'number',  value: 1 } ]," +
        " [ 'LiteralExpr', { type: 'number',  value: 2 } ] ]");
    testExpression(
        "1 >>> 2",
        "[ 'BinaryExpr', {op: \">>>\"}," +
        " [ 'LiteralExpr', { type: 'number',  value: 1 } ]," +
        " [ 'LiteralExpr', { type: 'number',  value: 2 } ] ]");
    testExpression(
        "1 >> 2",
        "[ 'BinaryExpr', {op: \">>\"}," +
        " [ 'LiteralExpr', { type: 'number',  value: 1 } ]," +
        " [ 'LiteralExpr', { type: 'number',  value: 2 } ] ]");

    // test precedence
    // * precedes +
    testExpression(
        "1 * 2 + 3",
        "['BinaryExpr', {op:'+'}, ['BinaryExpr', {op:'*'}," +
        " ['LiteralExpr', {type: 'number', value: 1}]," +
        " ['LiteralExpr', {type: 'number', value: 2}]]," +
        " ['LiteralExpr', {type: 'number', value: 3}]]");
    // now + precedes *
    testExpression(
        "(1 + 2) * 3",
        "[ 'BinaryExpr', {op:'*'}, ['BinaryExpr', {op:'+'}," +
        " ['LiteralExpr', {type:'number', value:1}]," +
        " ['LiteralExpr', {type:'number', value:2}] ]," +
        " [ 'LiteralExpr', { type: 'number',  value: 3 } ] ]");
    // now + precedes *
    testExpression(
        "1 * (2 + 3)",
        "['BinaryExpr', {op:'*'}," +
        " ['LiteralExpr', { type: 'number',  value: 1 } ]," +
        " ['BinaryExpr', {op:'+'}, ['LiteralExpr', {type:'number', value:2}]," +
        " ['LiteralExpr', {type:'number', value:3}] ] ]");

    testExpression(
        "x < y",
        "[ 'BinaryExpr', {op: '<'  }, ['IdExpr',{name:'x'}]," +
        " ['IdExpr',{name:'y'}] ]");
    testExpression(
        "x > y",
        "[ 'BinaryExpr', {op: '>'  }, ['IdExpr',{name:'x'}]," +
        " ['IdExpr',{name:'y'}] ]");
    testExpression(
        "x <= y",
        "[ 'BinaryExpr', {op: \"<=\" }, ['IdExpr',{name:'x'}]," +
        " ['IdExpr',{name:'y'}] ]");
    testExpression(
        "x >= y",
        "[ 'BinaryExpr', {op: \">=\" }, ['IdExpr',{name:'x'}]," +
        " ['IdExpr',{name:'y'}] ]");
    testExpression(
        "x instanceof y",
        "[ 'BinaryExpr', {op: 'instanceof' }, ['IdExpr',{name:'x'}]," +
        " ['IdExpr',{name:'y'}] ]");
    testExpression(
        "x in y",
        "[ 'BinaryExpr', {op: 'in' }, ['IdExpr',{name:'x'}]," +
        " ['IdExpr',{name:'y'}] ]");

    testExpression(
        "x & y",
        "[ 'BinaryExpr', {op: '&' }, ['IdExpr',{name:'x'}]," +
        " ['IdExpr',{name:'y'}] ]");
    testExpression(
        "x ^ y",
        "[ 'BinaryExpr', {op: '^' }, ['IdExpr',{name:'x'}]," +
        " ['IdExpr',{name:'y'}] ]");
    testExpression(
        "x | y",
        "[ 'BinaryExpr', {op: '|' }, ['IdExpr',{name:'x'}]," +
        " ['IdExpr',{name:'y'}] ]");

    // test precedence
    // & precedes |
    testExpression(
        "x & y | z",
        "[ 'BinaryExpr', {op: '|' }, ['BinaryExpr', {op:'&'}," +
        " ['IdExpr', {name:'x'}],['IdExpr', {name:'y'}] ]," +
        " ['IdExpr',{name:'z'}] ]");

    // logical operators
    testExpression(
        "x && y",
        "['LogicalAndExpr', {}, ['IdExpr',{name:'x'}], ['IdExpr',{name:'y'}]]");
    testExpression(
        "x || y",
        "['LogicalOrExpr', {}, ['IdExpr',{name:'x'}], ['IdExpr',{name:'y'}]]");

    // test precedence
    // && precedes ||
    testExpression(
        "x && y || z",
        "['LogicalOrExpr', {}, ['LogicalAndExpr', {}, ['IdExpr', {name:'x'}]," +
        " ['IdExpr', {name:'y'}] ]," +
        " ['IdExpr',{name:'z'}] ]");
    // && precedes ||
    testExpression(
        "x || y && z",
        "['LogicalOrExpr', {}, ['IdExpr',{name:'x'}], ['LogicalAndExpr', {}," +
        " ['IdExpr', {name:'y'}]," +
        " ['IdExpr', {name:'z'}] ] ]");

    // conditional operator
    testExpression(
        "x < y ? z : w",
        "['ConditionalExpr', {}, ['BinaryExpr', {op:'<'}," +
        " ['IdExpr', {name:'x'}], ['IdExpr', {name:'y'}] ]," +
        " ['IdExpr', {name:'z'}]," +
        " ['IdExpr', {name:'w'}] ]");

    // assignment
    testExpression(
        "x >>>= y",
        "['AssignExpr', {op: \">>>=\"}, ['IdExpr',{name:'x'}]," +
        " ['IdExpr',{name:'y'}] ]");
    testExpression(
        "x <<= y",
        "['AssignExpr', {op: \"<<=\" }, ['IdExpr',{name:'x'}]," +
        " ['IdExpr',{name:'y'}] ]");
    testExpression(
        "x = y",
        "[ 'AssignExpr', {op: '='   }, ['IdExpr',{name:'x'}]," +
        " ['IdExpr',{name:'y'}] ]");
    testExpression(
        "x += y",
        "[ 'AssignExpr', {op: \"+=\"  }, ['IdExpr',{name:'x'}]," +
        " ['IdExpr',{name:'y'}] ]");

    testExpression(
        "x /= y",
        "[ 'AssignExpr', {op: \"/=\"  }, ['IdExpr',{name:'x'}]," +
        " ['IdExpr',{name:'y'}] ]");

    // comma operator
    testExpression(
        "x , y",
        "[ 'BinaryExpr', {op: ','}, ['IdExpr',{name:'x'}]," +
        " ['IdExpr',{name:'y'}] ]");

    // statements

    // blocks
    testStatement("{}", "['BlockStmt', {}]");
    testStatement("{x;}", "['BlockStmt', {}, ['IdExpr', {name:'x'}] ]");
    testStatement(
        "{x;y;}",
        "['BlockStmt', {}, ['IdExpr', {name:'x'}], ['IdExpr', {name:'y'}] ]");

    // variable declarations
    testStatement("var x;", "[ 'VarDecl', {}, ['IdPatt', {name: 'x'}] ]");
    testStatement(
        "var x,y;",
        "[ 'VarDecl', {}, ['IdPatt', {name: 'x'}], ['IdPatt', {name:'y'}] ]");
    testStatement(
        "var x=1,y=2;",
        "[ 'VarDecl', {}, ['InitPatt', {}, ['IdPatt', {name: 'x'}]," +
        " ['LiteralExpr', { type: 'number',  value: 1 } ] ], ['InitPatt', {}," +
        " ['IdPatt', {name: 'y'}]," +
        " ['LiteralExpr', { type: 'number',  value: 2 } ] ] ]");
    testStatement(
        "var x,y=2;",
        "['VarDecl', {}, ['IdPatt', {name: 'x'} ], ['InitPatt', {}," +
        " ['IdPatt', {name: 'y'}]," +
        " [ 'LiteralExpr', { type: 'number',  value: 2 } ] ] ]");
    // empty statement
    testStatement(";", "['EmptyStmt', {}]");
    testStatement("\n;", "['EmptyStmt', {}]");

    // expression statements
    testStatement("x;", "[ 'IdExpr', {name:'x'} ]");
    testStatement("5;", "[ 'LiteralExpr', {type:'number', value: 5}]");
    testStatement(
        "1 + 2;",
        "[ 'BinaryExpr', {op:'+'}," +
        " [ 'LiteralExpr', { type: 'number',  value: 1 } ]," +
        " [ 'LiteralExpr', { type: 'number',  value: 2 } ]]");

    // if statements
    testStatement(
        "if (c) x; else y;",
        "[ 'IfStmt',{}, ['IdExpr',{name:'c'}], ['IdExpr', {name:'x'}]," +
        " ['IdExpr', {name:'y'}] ]");
    testStatement(
        "if (c) x;",
        "[ 'IfStmt',{}, ['IdExpr',{name:'c'}], ['IdExpr', {name:'x'}]," +
        " ['EmptyStmt', {}] ]");
    testStatement(
        "if (c) {} else {}",
        "[ 'IfStmt',{}, ['IdExpr',{name:'c'}], ['BlockStmt', {}]," +
        " ['BlockStmt', {}] ]");
    testStatement(
        "if (c1) if (c2) s1; else s2;",
        "[ 'IfStmt',{}, ['IdExpr',{name:'c1'}], [ 'IfStmt', {}," +
        " ['IdExpr',{name:'c2'}], ['IdExpr', {name:'s1'}]," +
        " ['IdExpr', {name:'s2'}] ]," +
        " [ 'EmptyStmt', {}] ]");

    // do-while statement
    testStatement(
        "do s; while (e);",
        "[ 'DoWhileStmt', {}, ['IdExpr', {name:'s'}], ['IdExpr',{name:'e'}] ]");

    // while statement
    testStatement(
        "while (e) s;",
        "[ 'WhileStmt',{}, ['IdExpr',{name:'e'}], ['IdExpr', {name:'s'}] ]");

    // for statements
    testStatement(
        "for (;;) ;",
        "['ForStmt', {}, ['EmptyStmt', {}]," +
        " ['LiteralExpr', {type: 'boolean', 'value': true}]," +
        " ['EmptyStmt', {}], ['EmptyStmt',{}] ]");
    testStatement(
        "for (;c;x++) x;",
        "[ 'ForStmt', {}, ['EmptyStmt', {}], ['IdExpr',{name:'c'}]," +
        " ['CountExpr', {isPrefix:false,op:\"++\"}, ['IdExpr',{name:'x'}] ]," +
        " ['IdExpr', {name:'x'}] ]");
    testStatement(
        "for (i;i<10;i++) {}",
        "['ForStmt', {}, ['IdExpr',{name:'i'}], ['BinaryExpr', {op:'<'}," +
        " ['IdExpr', {name:'i'}], ['LiteralExpr', {type:'number',value:10}]]," +
        " ['CountExpr', {isPrefix:false,op:\"++\"}, ['IdExpr',{name:'i'}] ]," +
        " ['BlockStmt', {}] ]");
    testStatement(
        "for (var i=0;i<len;i++) {}",
        "[ 'ForStmt', {}, [ 'VarDecl',{}, ['InitPatt', {}," +
        " ['IdPatt',{name:'i'}]," +
        " [ 'LiteralExpr', { type: 'number',  value: 0 } ]] ]," +
        " ['BinaryExpr', {op:'<'}, ['IdExpr', {name:'i'}]," +
        "['IdExpr', {name:'len'}] ], ['CountExpr', {isPrefix:false,op:\"++\"}," +
        " ['IdExpr',{name:'i'}] ]," +
        " ['BlockStmt', {}] ]");
    testStatement(
        "for (var i=0,j=0;;) {}",
        "[ 'ForStmt', {}, [ 'VarDecl',{}, ['InitPatt', {}," +
        " ['IdPatt',{name:'i'}]," +
        " [ 'LiteralExpr', { type: 'number',  value: 0 } ]], ['InitPatt', {}," +
        " ['IdPatt',{name:'j'}]," +
        " [ 'LiteralExpr', { type: 'number',  value: 0 } ]] ]," +
        " ['LiteralExpr', {type:'boolean', value:true}] , ['EmptyStmt', {}]," +
        " ['BlockStmt', {}] ]");

    testStatement(
        "for ((x in b); c; u) {}",
        "['ForStmt', {}, ['BinaryExpr', {op:'in'}, ['IdExpr', {name:'x'}]," +
        "['IdExpr', {name:'b'}]], ['IdExpr',{name:'c'}]," +
        " ['IdExpr', {name:'u'}], ['BlockStmt', {}]]");

    // for-in statement without variable declaration
    testStatement(
        "for (x in a) ;",
        "['ForInStmt', {}, ['IdExpr', {name:'x'}], ['IdExpr', {name:'a'}]," +
        " ['EmptyStmt', {}]]");

    // for-in statement with variable declaration
    testStatement(
        "for (var x in a) {}",
        "['ForInStmt', {}, ['VarDecl', {}, ['IdPatt', {name:'x'}]]," +
        " ['IdExpr', {name:'a'}]," +
        " ['BlockStmt', {}] ]");

    // break, continue and return statements
    testStatement("continue;", "['ContinueStmt', {}]");
    testStatement("continue label;", "['ContinueStmt',{label:'label'}]");
    testStatement("break;", "['BreakStmt',{}]");
    testStatement("break label;", "['BreakStmt',{label:'label'}]");
    testStatement("continue\n", "['ContinueStmt', {}]");
    testStatement("return;", "['ReturnStmt', {}]");
    testStatement(
        "return 0;",
        "['ReturnStmt',{}, [ 'LiteralExpr', { type: 'number',  value: 0 } ]]");
    testStatement(
        "return 0 + \n 1;",
        "['ReturnStmt',{}, ['BinaryExpr', {op:'+'}," +
        " ['LiteralExpr', {type:'number', value:0}]," +
        "['LiteralExpr', {type:'number', value:1}] ]]");

    // with statement
    testStatement(
        "with (e) s;",
        "['WithStmt', {}, ['IdExpr',{name:'e'}], ['IdExpr', {name:'s'}] ]");

    // switch statements
    testStatement(
        "switch (e) { case x: s; }",
        "['SwitchStmt',{}, ['IdExpr',{name:'e'}], ['Case', {}," +
        " ['IdExpr',{name:'x'}]," +
        " ['IdExpr', {name:'s'}] ] ]");
    testStatement(
        "switch (e) { case x: s1;s2; default: s3; case y: s4; }",
        "['SwitchStmt',{}, ['IdExpr',{name:'e'}], ['Case', {}," +
        " ['IdExpr',{name:'x'}], ['IdExpr', {name:'s1'}]," +
        " ['IdExpr', {name:'s2'}] ], ['DefaultCase', {}," +
        " ['IdExpr', {name:'s3'}] ], ['Case', {}, ['IdExpr',{name:'y'}]," +
        " ['IdExpr', {name:'s4'}] ] ]");
    testStatement(
        "switch (e) { default: s1; case x: s2; case y: s3; }",
        "['SwitchStmt',{}, ['IdExpr',{name:'e'}], ['DefaultCase', {}," +
        " ['IdExpr', {name:'s1'}] ], ['Case', {}, ['IdExpr',{name:'x'}]," +
        " ['IdExpr', {name:'s2'}] ], ['Case', {}, ['IdExpr',{name:'y'}]," +
        " ['IdExpr', {name:'s3'}] ] ]");
    testStatement(
        "switch (e) { default: s; }",
        "['SwitchStmt',{}, ['IdExpr',{name:'e'}], ['DefaultCase', {}," +
        " ['IdExpr', {name:'s'}] ] ]");
    testStatement(
        "switch (e) { case x: s1; case y: s2; }",
        "['SwitchStmt',{}, ['IdExpr',{name:'e'}], ['Case', {}," +
        " ['IdExpr',{name:'x'}], ['IdExpr', {name:'s1'}] ], ['Case', {}," +
        " ['IdExpr',{name:'y'}]," +
        " ['IdExpr', {name:'s2'}] ] ]");

    // labelled statements
    testStatement(
        "foo : x;", "['LabelledStmt',{label:'foo'}, ['IdExpr', {name:'x'}]]");

    // throw statements
    testStatement("throw x;", "['ThrowStmt',{}, ['IdExpr',{name:'x'}]]");
    testStatement("throw x\n", "['ThrowStmt',{}, ['IdExpr',{name:'x'}]]");

    // try-catch statements
    testStatement(
        "try { s1; } catch (e) { s2; }",
        "['TryStmt',{}, ['BlockStmt',{},['IdExpr', {name:'s1'}]]," +
        " [ 'CatchClause', {}, ['IdPatt',{name:'e'}], ['BlockStmt',{}," +
        "['IdExpr', {name:'s2'}]] ] ]");
    testStatement(
        "try { s1; } finally { s2; }",
        "['TryStmt',{}, ['BlockStmt',{},['IdExpr', {name:'s1'}]]," +
        " ['Empty', {}], ['BlockStmt',{}," +
        "['IdExpr', {name:'s2'}]] ]");
    testStatement(
        "try { s1; } catch (e) { s2; } finally { s3; }",
        "['TryStmt',{}, ['BlockStmt',{},['IdExpr', {name:'s1'}]]," +
        " [ 'CatchClause', {}, ['IdPatt',{name:'e'}], ['BlockStmt',{}," +
        "['IdExpr', {name:'s2'}]] ], ['BlockStmt',{}," +
        "['IdExpr', {name:'s3'}]] ]");

    // debugger statement
    testStatement("debugger;", "['DebuggerStmt',{}]");

    // function declaration
    testStatement(
        "function f(x) { e; return x; }",
        "[ 'FunctionDecl', {}, ['IdPatt',{name:'f'}], ['ParamDecl',{}," +
        "['IdPatt',{name:'x'}]], ['IdExpr', {name:'e'}], ['ReturnStmt', {}," +
        " ['IdExpr', { name: 'x' }]] ]");
    testStatement(
        "function f() { x; y; }",
        "[ 'FunctionDecl', {}, ['IdPatt',{name:'f'}], ['ParamDecl',{}]," +
        " ['IdExpr', {name:'x'}]," +
        " ['IdExpr', {name:'y'}] ]");
    testStatement(
        "function f(x,y) { var z; return x; }",
        "[ 'FunctionDecl', {}, ['IdPatt',{name:'f'}], ['ParamDecl',{}," +
        "['IdPatt',{name:'x'}], ['IdPatt',{name:'y'}]], ['VarDecl', {}," +
        " ['IdPatt', { name: 'z' }] ], ['ReturnStmt', {}," +
        " ['IdExpr', { name: 'x' }]] ]");

    // function expression
    testExpression(
        "function f(x) { return x; }",
        "[ 'FunctionExpr', {}, ['IdPatt',{name:'f'}], ['ParamDecl',{}," +
        "['IdPatt',{name:'x'}]], ['ReturnStmt', {}," +
        " ['IdExpr', { name: 'x' }]] ]");
    testStatement(
        "(function empty() {})",
        "[ 'FunctionExpr', {}, ['IdPatt',{name:'empty'}], ['ParamDecl',{}] ]");
    testStatement(
        "(function f(x,y) { var z; return x; })",
        "[ 'FunctionExpr', {}, ['IdPatt',{name:'f'}], ['ParamDecl',{}," +
        "['IdPatt',{name:'x'}], ['IdPatt',{name:'y'}]], ['VarDecl', {}," +
        " ['IdPatt', { name: 'z' }] ], ['ReturnStmt', {}," +
        " ['IdExpr', { name: 'x' }]] ]");
    testStatement(
        "(function (x) { })",
        "[ 'FunctionExpr', { }, ['Empty', {}], ['ParamDecl',{}," +
        "['IdPatt',{name:'x'}]] ]");

    // program
    testProgram("", "['Program',{}]");
    testProgram("x", "['Program', {}, ['IdExpr', {name:'x'}]]");
    testProgram(
        "var x; function f(){} null",
        "['Program',{}, ['VarDecl', {}, ['IdPatt', {name:'x'}]]," +
        " ['FunctionDecl', {}, ['IdPatt', {name:'f'}], ['ParamDecl',{}]]," +
        " ['LiteralExpr', {type: 'null', value:null}] ]");
    // 2 empty statements
    testProgram(
        ";;",
        "['Program',{}, ['EmptyStmt',{}], ['EmptyStmt', {}]]");
    testProgram(
        "{ x; y; z; }",
        "['Program', {}, ['BlockStmt',{}, ['IdExpr', {name:'x'}]," +
        " ['IdExpr', {name:'y'}]," +
        " ['IdExpr', {name:'z'}] ] ]");

    // test nested function declarations
    testProgram(
        "function f() { function g() { }}",
        "['Program', {}, ['FunctionDecl', {}, ['IdPatt', {name:'f'}]," +
        " ['ParamDecl',{}], ['FunctionDecl', {}, ['IdPatt', {name:'g'}]," +
        " ['ParamDecl',{}]] ]]");

    // automatic semicolon insertion

    // parsed as 'continue; foo;'
    testProgram(
        "continue \n foo;",
        "['Program',{}, ['ContinueStmt',{}], ['IdExpr', {name:'foo'}] ]");
    // parsed as 'break; foo;'
    testProgram(
        "break \n foo;",
        "['Program',{}, ['BreakStmt',{}], ['IdExpr', {name:'foo'}] ]");
    // parsed as 'return; foo;'
    testProgram(
        "return\nfoo;",
        "['Program',{}, ['ReturnStmt',{}], ['IdExpr', {name:'foo'}] ]");

    // Directive Prologues
    testProgram(
        "\"use strict\"; 'bla'\n foo",
        "['Program', {}," +
        " ['PrologueDecl'," +
        " {'directive': \"use strict\", 'value': \"use strict\"}]," +
        " ['PrologueDecl', { 'directive':'bla', 'value': 'bla' } ]," +
        " ['IdExpr', { 'name':'foo' } ] ]");
    testProgram(
        "\"use\\x20strict\";",
        "['Program', {}," +
        " ['PrologueDecl'," +
        " {'directive': \"use\\\\x20strict\", 'value': \"use strict\"}] ]");
    testExpression(
        "function() { \"use strict\"; 'bla'\n foo }",
        "['FunctionExpr', {}, ['Empty', {}], ['ParamDecl',{}]," +
        " ['PrologueDecl'," +
        " { 'directive':\"use strict\", 'value':\"use strict\" } ]," +
        " ['PrologueDecl', { 'directive':'bla', 'value':'bla' } ]," +
        " ['IdExpr', { 'name':'foo' } ] ]");
    testProgram(
        "\"use\\ strict\";",
        "['Program', {}," +
        " ['PrologueDecl'," +
        " { directive:\"use\\\\ strict\", value: \"use strict\" }]]");
    testProgram(
        "foo; \"use strict\";",
        "['Program', {}, ['IdExpr', {'name':'foo'} ]," +
        " ['LiteralExpr', {type:'string', value:\"use strict\"} ] ]");
    // examples from the spec (sec 7.9.2)
    testProgram(
        "{ 1 \n 2 } 3", // semicolon inserted after 1,2 and 3
        "[ 'Program', {}, ['BlockStmt', {}," +
        " ['LiteralExpr', {type: 'number', value:1}]," +
        " ['LiteralExpr', {type:'number', value:2}] ]," +
        " ['LiteralExpr', {type:'number', value: 3}] ]");

    testProgram(
        "return\na + b", // semicolon inserted after return
        "[ 'Program', {}, ['ReturnStmt',{}], ['BinaryExpr',{op:'+'}," +
        "['IdExpr',{name:'a'}]," +
        "['IdExpr',{name:'b'}] ] ]");

    testProgram(
        "a = b \n ++c", // semicolon inserted after a = b
        "[ 'Program', {}, ['AssignExpr',{op:'='}, ['IdExpr',{name:'a'}]," +
        " ['IdExpr',{name:'b'}] ], ['CountExpr', {isPrefix:true,op:\"++\"}," +
        " ['IdExpr',{name:'c'}] ] ]");
  }

  private void testExpression(String js, String jsonMLGolden)
      throws ParseException {
    Expression e = jsExpr(fromString(js));
    JsonML actual = ((JsonMLCompatible) e).toJsonML();
    checkJsonML(e, actual, jsonMLGolden, Expression.class);
  }

  private void testStatement(String js, String jsonMLGolden)
      throws ParseException {
    Block bl = js(fromString(js));
    assertEquals(1, bl.children().size());
    Statement s = bl.children().get(0);
    JsonML actual = s.toJsonML();
    checkJsonML(s, actual, jsonMLGolden, Statement.class);
  }

  private void testProgram(String js, String jsonMLGolden)
      throws ParseException {
    Block program = "".equals(js) ? new Block() : js(fromString(js));
    JsonML actual = program.toJsonMLAsProgram();
    checkJsonML(program, actual, jsonMLGolden, Block.class);
  }

  private <T extends ParseTreeNode> void checkJsonML(
      T n, JsonML actual, String jsonMLGolden, Class<T> type)
      throws ParseException {
    stripPositionInfo(actual);
    assertEquals(
        render(jsExpr(fromString(jsonMLGolden))),
        render(jsExpr(fromString(actual.toString()))));
    T reconverted = new JsonMLConverter(
        Collections.<String, SourceBreaks>emptyMap())
        .toNode(actual, type);
    assertEquals(normRender(n), normRender(reconverted));
  }

  private static String normRender(ParseTreeNode n) {
    return render(n).replaceAll("\\.0\\b", "")
        .replaceAll("\\b(new [\\w.]+)\\(\\)", "$1");
  }

  private static void stripPositionInfo(JsonML jsonml) {
    jsonml.getAttributes().remove(TagAttr.SOURCE);
    jsonml.getAttributes().remove(TagAttr.OPAQUE_POSITION);
    for (JsonML child : jsonml.getChildren()) {
      stripPositionInfo(child);
    }
  }
}
