// Copyright (C) 2009 Google Inc.
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

package com.google.caja.parser.js.scope;

import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.ParseException;
import com.google.caja.parser.AncestorChain;
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.Identifier;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageType;
import com.google.caja.util.CajaTestCase;
import com.google.caja.util.Join;
import com.google.caja.util.MoreAsserts;
import com.google.common.collect.Lists;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class ScopeAnalyzerTest extends CajaTestCase {
  List<String> events;
  ScopeListener<TestScope> listener;

  public final void testGlobalScope() throws ParseException {
    assertScoping(
        program(
            "var x = 1, y;",
            "alert(x + z)"),
        allImplementations(listener),
        "enterScope PROGRAM at 0 @ testGlobalScope:1+1 - 2+13",
        "  locals:   [x, y]",
        "  read:     [outer alert at 0, x at 0, outer z at 0]",
        "  assigned: [x at 0]",
        "exitScope at 0");
    assertNoErrors();
  }

  public final void testFnDecls() throws ParseException {
    assertScoping(
        program(
            "var x = 1, y;",
            "alert(x + z);",
            "function alert(msg) { console.log(msg); }"),
        notJScript(listener),
        "enterScope PROGRAM at 0 @ testFnDecls:1+1 - 3+42",
        "  enterScope FUNCTION at 1 @ testFnDecls:3+1 - 42",
        "    locals:   [alert, msg]",
        "    read:     [msg at 1]",
        "  exitScope at 1",
        "  locals:   [x, y, alert]",
        "  read:     [outer console at 1, alert at 0, x at 0, outer z at 0]",
        "  assigned: [x at 0, alert at 0]",
        "exitScope at 0");
    assertNoErrors();
  }

  public final void testFnCtorsInES5() throws ParseException {
    new ES5ScopeAnalyzer<TestScope>(listener).apply(program(
        "var x = 1, y;",
        "var alert = function z(msg) { console.log(msg); };",
        "alert(x + z);"));
    assertEvents(
        "enterScope PROGRAM at 0 @ testFnCtorsInES5:1+1 - 3+14",
        "  enterScope FUNCTION at 1 @ testFnCtorsInES5:2+13 - 50",
        "    locals:   [z, msg]",
        "    read:     [msg at 1]",
        "  exitScope at 1",
        "  locals:   [x, y, alert]",
        "  read:     [outer console at 1, alert at 0, x at 0, outer z at 0]",
        "  assigned: [x at 0, alert at 0]",
        "exitScope at 0");
    assertNoErrors();
  }

  public final void testFnCtorsInJScript() throws ParseException {
    new JScriptScopeAnalyzer<TestScope>(listener).apply(program(
        "var x = 1, y;",
        "var alert = function z(msg) { console.log(msg); };",
        "alert(x + z);"));
    assertEvents(
        "enterScope PROGRAM at 0 @ testFnCtorsInJScript:1+1 - 3+14",
        "  enterScope FUNCTION at 1 @ testFnCtorsInJScript:2+13 - 50",
        "    locals:   [msg]",  // JScript does not declare a z here.
        "    read:     [msg at 1]",
        "  exitScope at 1",
        "  locals:   [x, y, alert, z]",  // JScript declares z here.
        // z not an outer.
        "  read:     [outer console at 1, alert at 0, x at 0, z at 0]",
        "  assigned: [x at 0, alert at 0]",
        "exitScope at 0");
    assertNoErrors();
  }

  public final void testFnCtorsInWorstCase() throws ParseException {
    new WorstCaseScopeAnalyzer<TestScope>(listener).apply(program(
        "var x = 1, y;",
        "var alert = function z(msg) { console.log(msg); };",
        "alert(x + z);"));
    assertEvents(
        "enterScope PROGRAM at 0 @ testFnCtorsInWorstCase:1+1 - 3+14",
        "  enterScope FUNCTION at 1 @ testFnCtorsInWorstCase:2+13 - 50",
        "    masked z at 0",
        // JScript does not declare z here, but worst case does
        "    locals:   [z, msg]",
        "    read:     [msg at 1]",
        "  exitScope at 1",
        "  locals:   [x, y, alert, z]",  // JScript declares z here.
        // z not an outer.
        "  read:     [outer console at 1, alert at 0, x at 0, z at 0]",
        "  assigned: [x at 0, alert at 0]",
        "exitScope at 0");
    assertNoErrors();
  }

  public final void testHoistingOfDecls1() throws ParseException {
    assertScoping(
        program(
            "try {",
            "  for (var i = 0; i < n; ++i) {",
            "    foo(arr[i]);",
            "  }",
            "} catch (e) {",
            "  alert('stopped at ' + i);",
            "}"),
        allImplementations(listener),
        "enterScope PROGRAM at 0 @ testHoistingOfDecls1:1+1 - 7+2",
        "  enterScope CATCH at 1 @ testHoistingOfDecls1:5+3 - 7+2",
        "    locals:   [e]",
        "  exitScope at 1",
        "  locals:   [i]",
        "  read:     [outer alert at 1, i at 1, i at 0, outer n at 0, i at 0,"
                  + " outer foo at 0, outer arr at 0, i at 0]",
        "  assigned: [i at 0, i at 0]",
        "exitScope at 0");
  }

  public final void testHoistingOfDecls2() throws ParseException {
    assertScoping(
        program(
            "(function () {",
            "try {",
            "  for (var i = 0; i < n; ++i) {",
            "    foo(arr[i]);",
            "  }",
            "} catch (e) {",
            "  alert('stopped at ' + i);",
            "}",
            "})();"),
        allImplementations(listener),
        "enterScope PROGRAM at 0 @ testHoistingOfDecls2:1+1 - 9+6",
        "  enterScope FUNCTION at 1 @ testHoistingOfDecls2:1+2 - 9+2",
        "    enterScope CATCH at 2 @ testHoistingOfDecls2:6+3 - 8+2",
        "      locals:   [e]",
        "    exitScope at 2",
        "    locals:   [i]",
        "    read:     [i at 2, i at 1, i at 1, i at 1]",
        "    assigned: [i at 1, i at 1]",
        "  exitScope at 1",
        "  read:     [outer alert at 2, outer n at 1, outer foo at 1,"
                  + " outer arr at 1]",
        "exitScope at 0");
    assertNoErrors();
  }

  public final void testUsageOfThis1() throws ParseException {
    assertScoping(
        program(
            "this.location = 'foo';",
            "try {",
            "  new XMLHttpRequest;",
            "} catch (e) {",
            "  this.XMLHttpRequest = bar;",
            "}"),
        allImplementations(listener),
        "enterScope PROGRAM at 0 @ testUsageOfThis1:1+1 - 6+2",
        "  enterScope CATCH at 1 @ testUsageOfThis1:4+3 - 6+2",
        "    locals:   [e]",
        "  exitScope at 1",
        "  read:     [this at 1, outer bar at 1,"
                  + " this at 0, outer XMLHttpRequest at 0]",
        "exitScope at 0");
    assertNoErrors();
  }

  public final void testUsageOfThis2() throws ParseException {
    assertScoping(
        program(
            "(function () {",
            "this.location = 'foo';",
            "try {",
            "  new XMLHttpRequest;",
            "} catch (e) {",
            "  this.XMLHttpRequest = bar;",
            "}",
            "})();"),
        allImplementations(listener),
        "enterScope PROGRAM at 0 @ testUsageOfThis2:1+1 - 8+6",
        "  enterScope FUNCTION at 1 @ testUsageOfThis2:1+2 - 8+2",
        "    enterScope CATCH at 2 @ testUsageOfThis2:5+3 - 7+2",
        "      locals:   [e]",
        "    exitScope at 2",
        "    read:     [this at 2, this at 1]",
        "  exitScope at 1",
        "  read:     [outer bar at 2, outer XMLHttpRequest at 1]",
        "exitScope at 0");
    assertNoErrors();
  }

  public final void testUsageOfArguments() throws ParseException {
    assertScoping(
        program(
            "if (typeof arguments === 'undefined') {",
            "  try {",
            "    arguments = (function () { return arguments; })();",
            "  } catch (ex) {",
            "    arguments = 'arguments';",
            "  }",
            "}"),
        allImplementations(listener),
        "enterScope PROGRAM at 0 @ testUsageOfArguments:1+1 - 7+2",
        "  enterScope FUNCTION at 1 @ testUsageOfArguments:3+18 - 51",
        "    read:     [arguments at 1]",
        "  exitScope at 1",
        "  enterScope CATCH at 1 @ testUsageOfArguments:4+5 - 6+4",
        "    locals:   [ex]",
        "  exitScope at 1",
        "  read:     [outer arguments at 0]",
        "  assigned: [outer arguments at 1, outer arguments at 0]",
        "exitScope at 0");
    assertNoErrors();
  }

  public final void testDupesAndMasking() throws ParseException {
    AncestorChain<Block> prog = program(
        "var ex, arguments;",
        "try {",
        "  var arguments = (function (arguments) { return arguments[z]; })();",
        "} catch (ex) {",
        "  arguments = 'arguments';",
        "  var z;",
        "}");
    assertMessage(  // Generated by parser
        true, MessageType.DUPLICATE_FORMAL_PARAM, MessageLevel.ERROR,
        MessagePart.Factory.valueOf("arguments"));
    assertScoping(
        prog,
        allImplementations(listener),
        "enterScope PROGRAM at 0 @ testDupesAndMasking:1+1 - 7+2",
        "  Dupe arguments",
        "  enterScope FUNCTION at 1 @ testDupesAndMasking:3+20 - 65",
        "    Dupe arguments",
        "    locals:   [arguments]",
        "    read:     [arguments at 1]",
        "  exitScope at 1",
        "  enterScope CATCH at 1 @ testDupesAndMasking:4+3 - 7+2",
        "    masked ex at 0",
        "    locals:   [ex]",
        "  exitScope at 1",
        "  locals:   [ex, arguments, arguments, z]",
        "  read:     [z at 1]",
        "  assigned: [arguments at 1, arguments at 0]",
        "exitScope at 0");
    assertNoErrors();
  }

  public final void testMasking1() throws ParseException {
    assertScoping(
        program(
            "var x;",
            "(function (x) {});"),
        allImplementations(listener),
        "enterScope PROGRAM at 0 @ testMasking1:1+1 - 2+19",
        "  enterScope FUNCTION at 1 @ testMasking1:2+2 - 17",
        "    masked x at 0",
        "    locals:   [x]",
        "  exitScope at 1",
        "  locals:   [x]",
        "exitScope at 0");
    assertNoErrors();
  }

  public final void testSplitInitialization() throws ParseException {
    assertScoping(
        program(
            "try {",
            "  throw 1;",
            "} catch (e) {",
            "  var e = 1;",
            "}",
            "return e;"),
        allImplementations(listener),
        "enterScope PROGRAM at 0 @ testSplitInitialization:1+1 - 6+10",
        "  split initialization of e at 0 into 1",
        "  enterScope CATCH at 1 @ testSplitInitialization:3+3 - 5+2",
        "    masked e at 0",
        "    locals:   [e]",
        "    assigned: [e at 1]",  // assigned here
        "  exitScope at 1",
        "  locals:   [e]",
        "  read:     [e at 0]",
        // not assigned here
        "exitScope at 0");
    assertNoErrors();
  }

  public final void testMasking2() throws ParseException {
    assertScoping(
        program(
            "(function (x) {});",
            "var x;"),
        allImplementations(listener),
        "enterScope PROGRAM at 0 @ testMasking2:1+1 - 2+7",
        "  enterScope FUNCTION at 1 @ testMasking2:1+2 - 17",
        "    masked x at 0",
        "    locals:   [x]",
        "  exitScope at 1",
        "  locals:   [x]",
        "exitScope at 0");
    assertNoErrors();
  }

  public final void testAssignments() throws ParseException {
    new ES5ScopeAnalyzer<TestScope>(new TestScopeListener() {
      @Override
      public void assigned(
          AncestorChain<Identifier> id, TestScope useSite, TestScope defSite) {
        emit("set  " + id.node.getName());
      }
      @Override
      public void read(
          AncestorChain<Identifier> id, TestScope useSite, TestScope defSite) {
        emit("read " + id.node.getName());
      }
    }).apply(program("var x = 1; ++y; w = z += x;"));
    assertEvents(
        "enterScope PROGRAM at 0 @ testAssignments:1+1 - 28",
        "  set  x",
        "  read y",
        "  set  y",
        "  set  w",
        "  read z",
        "  set  z",
        "  read x",
        "  locals:   [x]",
        "exitScope at 0");
    assertNoErrors();
  }

  public final void testForEachLoopAssignsPropName1() throws ParseException {
    assertScoping(
        program("for (var k in obj) { count(); }"),
        allImplementations(listener),
        "enterScope PROGRAM at 0 @ testForEachLoopAssignsPropName1:1+1 - 32",
        "  locals:   [k]",
        "  read:     [outer obj at 0, outer count at 0]",
        "  assigned: [k at 0]",
        "exitScope at 0");
    assertNoErrors();
  }

  public final void testForEachLoopAssignsPropName2() throws ParseException {
    assertScoping(
        program("for (k in obj) { count(); }"),
        allImplementations(listener),
        "enterScope PROGRAM at 0 @ testForEachLoopAssignsPropName2:1+1 - 28",
        "  read:     [outer obj at 0, outer count at 0]",
        "  assigned: [outer k at 0]",
        "exitScope at 0");
    assertNoErrors();
  }

  public final void testSameName1() throws ParseException {
    new ES5ScopeAnalyzer<TestScope>(listener).apply(
        program("var x = function x() {};"));  // not masking
    assertEvents(
        "enterScope PROGRAM at 0 @ testSameName1:1+1 - 25",
        "  enterScope FUNCTION at 1 @ testSameName1:1+9 - 24",
        "    locals:   [x]",
        "  exitScope at 1",
        "  locals:   [x]",
        "  assigned: [x at 0]",
        "exitScope at 0");
    assertNoErrors();
  }

  public final void testFnDeclInCatch() throws ParseException {
    assertScoping(
        program(
            "try {",
            "} catch (e) {",
            "  function foo() { var e; }",
            "}"),
        notJScript(listener),
        "enterScope PROGRAM at 0 @ testFnDeclInCatch:1+1 - 4+2",
        "  enterScope CATCH at 1 @ testFnDeclInCatch:2+3 - 4+2",
        "    enterScope FUNCTION at 2 @ testFnDeclInCatch:3+3 - 28",
        "      masked e at 1",
        "      locals:   [foo, e]",
        "    exitScope at 2",
        "    locals:   [e]",
        "  exitScope at 1",
        "  locals:   [foo]",
        "  assigned: [foo at 1]",
        "exitScope at 0");
    assertNoErrors();
  }

  public final void testFnDeclInCatchJS() throws ParseException {
    new JScriptScopeAnalyzer<TestScope>(listener).apply(program(
        "try {",
        "} catch (e) {",
        "  function foo() { var e; }",
        "}"));
    assertEvents(
        "enterScope PROGRAM at 0 @ testFnDeclInCatchJS:1+1 - 4+2",
        "  enterScope CATCH at 1 @ testFnDeclInCatchJS:2+3 - 4+2",
        "    enterScope FUNCTION at 2 @ testFnDeclInCatchJS:3+3 - 28",
        "      masked e at 1",
        "      locals:   [e]",
        "    exitScope at 2",
        "    locals:   [e]",
        "  exitScope at 1",
        "  locals:   [foo]",
        "  assigned: [foo at 1]",
        "exitScope at 0");
    assertNoErrors();
  }

  public final void testExceptionReadAndAssign() throws ParseException {
    assertScoping(
        program(
            "try {",
            "  throw new Error();",
            "} catch (e) {",
            "  e = new ErrorWrapper(e);",
            "  throw e;",
            "}"),
        allImplementations(listener),
        "enterScope PROGRAM at 0 @ testExceptionReadAndAssign:1+1 - 6+2",
        "  enterScope CATCH at 1 @ testExceptionReadAndAssign:3+3 - 6+2",
        "    locals:   [e]",
        "    read:     [e at 1, e at 1]",
        "    assigned: [e at 1]",
        "  exitScope at 1",
        "  read:     [outer ErrorWrapper at 1, outer Error at 0]",
        "exitScope at 0");
    assertNoErrors();
  }

  public final void testSameName2() throws ParseException {
    new ES5ScopeAnalyzer<TestScope>(listener).apply(program(
        "var x;",
        "x = function x() {};"));  // not masking
    assertEvents(
        "enterScope PROGRAM at 0 @ testSameName2:1+1 - 2+21",
        "  enterScope FUNCTION at 1 @ testSameName2:2+5 - 20",
        "    locals:   [x]",
        "  exitScope at 1",
        "  locals:   [x]",
        "  assigned: [x at 0]",
        "exitScope at 0");
    assertNoErrors();
  }
  public final void testSameName3() throws ParseException {
    new JScriptScopeAnalyzer<TestScope>(listener).apply(program(
        "var x;",
        "x = function x() {};"));  // not masking
    assertEvents(
        "enterScope PROGRAM at 0 @ testSameName3:1+1 - 2+21",
        "  enterScope FUNCTION at 1 @ testSameName3:2+5 - 20",
        "  exitScope at 1",
        "  locals:   [x, x]",
        "  assigned: [x at 0]",
        "exitScope at 0");
    assertNoErrors();
  }

  public final void testSameName4() throws ParseException {
    new WorstCaseScopeAnalyzer<TestScope>(listener).apply(program(
        "var x;",
        "x = function x() {};"));  // not masking
    assertEvents(
        "enterScope PROGRAM at 0 @ testSameName4:1+1 - 2+21",
        "  enterScope FUNCTION at 1 @ testSameName4:2+5 - 20",
        "    locals:   [x]",
        "  exitScope at 1",
        "  locals:   [x, x]",
        "  assigned: [x at 0]",
        "exitScope at 0");
    assertNoErrors();
  }

  public final void testWithBlock() throws ParseException {
    assertScoping(
        program(
            "with (obj) {",
            "  var sum = x + y;",
            "  alert(sum);",
            "}"),
        allImplementations(listener),
        "enterScope PROGRAM at 0 @ testWithBlock:1+1 - 4+2",
        "  enterScope WITH at 1 @ testWithBlock:1+1 - 4+2",
        "  exitScope at 1",
        "  locals:   [sum]",
        "  read:     [outer x at 1, outer y at 1, outer alert at 1, sum at 1,"
                  + " outer obj at 0]",  // obj is in program scope
        "  assigned: [sum at 1]",
        "exitScope at 0");
    assertNoErrors();
  }

  public final void testImmediatelyCalledFns1() throws ParseException {
    new ES5ScopeAnalyzer<TestScope>(listener).apply(program(
        "alert([function () { return 'Hello'; }(),",
        "       function f() { return ' World!'; }()]);"));
    assertEvents(
        "enterScope PROGRAM at 0 @ testImmediatelyCalledFns1:1+1 - 2+47",
        "  enterScope FUNCTION at 1 @ testImmediatelyCalledFns1:1+8 - 39",
        "  exitScope at 1",
        "  enterScope FUNCTION at 1 @ testImmediatelyCalledFns1:2+8 - 42",
        "    locals:   [f]",
        "  exitScope at 1",
        "  read:     [outer alert at 0]",
        "exitScope at 0");
    assertNoErrors();
  }

  public final void testImmediatelyCalledFns2() throws ParseException {
    new JScriptScopeAnalyzer<TestScope>(listener).apply(program(
        "alert([function () { return 'Hello'; }(),",
        "       function f() { return ' World!'; }()]);"));
    assertEvents(
        "enterScope PROGRAM at 0 @ testImmediatelyCalledFns2:1+1 - 2+47",
        "  enterScope FUNCTION at 1 @ testImmediatelyCalledFns2:1+8 - 39",
        "  exitScope at 1",
        "  enterScope FUNCTION at 1 @ testImmediatelyCalledFns2:2+8 - 42",
        "  exitScope at 1",
        "  locals:   [f]",
        "  read:     [outer alert at 0]",
        "exitScope at 0");
    assertNoErrors();
  }

  public final void testImmediatelyCalledFns3() throws ParseException {
    new WorstCaseScopeAnalyzer<TestScope>(listener).apply(program(
        "alert([function () { return 'Hello'; }(),",
        "       function f() { return ' World!'; }()]);"));
    assertEvents(
        "enterScope PROGRAM at 0 @ testImmediatelyCalledFns3:1+1 - 2+47",
        "  enterScope FUNCTION at 1 @ testImmediatelyCalledFns3:1+8 - 39",
        "  exitScope at 1",
        "  enterScope FUNCTION at 1 @ testImmediatelyCalledFns3:2+8 - 42",
        "    masked f at 0",
        "    locals:   [f]",
        "  exitScope at 1",
        "  locals:   [f]",
        "  read:     [outer alert at 0]",
        "exitScope at 0");
    assertNoErrors();
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();
    events = Lists.newArrayList();
    listener = new TestScopeListener();
  }

  static class TestScope implements AbstractScope {
    final ScopeType type;
    final FilePosition pos;
    final int depth;
    final TestScope parent;
    final List<String> locals = Lists.newArrayList();
    final List<String> read = Lists.newArrayList();
    final List<String> assigned = Lists.newArrayList();
    final List<TestScope> inner = Lists.newArrayList();

    TestScope(ScopeType type, TestScope parent, FilePosition pos) {
      this.type = type;
      this.pos = pos;
      this.depth = parent != null ? parent.depth + 1 : 0;
      this.parent = parent;
    }

    public TestScope getContainingScope() { return parent; }

    public ScopeType getType() { return type; }

    public boolean isSymbolDeclared(String name) {
      return locals.contains(name);
    }

    TestScope getRoot() {
      if (parent == null) { return this; }
      return parent.getRoot();
    }

    @Override
    public String toString() {
      return "[TestScope " + type + " @ " + depth + "]";
    }
  }

  private class TestScopeListener implements ScopeListener<TestScope> {
    String prefix = "";

    protected void emit(String s) { events.add(prefix + s); }

    public void assigned(
        AncestorChain<Identifier> id, TestScope useSite, TestScope defSite) {
      if (defSite != null) {
        defSite.assigned.add(id.node.getName() + " at " + useSite.depth);
      } else {
        useSite.getRoot().assigned.add(
            "outer " + id.node.getName() + " at " + useSite.depth);
      }
    }

    public void declaration(AncestorChain<Identifier> d, TestScope scope) {
      scope.locals.add(d.node.getName());
    }

    public void duplicate(AncestorChain<Identifier> id, TestScope scope) {
      emit("Dupe " + id.node.getName());
    }

    public TestScope createScope(
        ScopeType t, AncestorChain<?> root, TestScope parent) {
      return new TestScope(t, parent, root.node.getFilePosition());
    }

    public void enterScope(TestScope scope) {
      StringBuilder sb;
      try {
        sb = new StringBuilder(
            "enterScope " + scope.getType() + " at " + scope.depth + " @ ");
        scope.pos.format(mc, sb);
      } catch (IOException ex) {
        throw new RuntimeException(ex);
      }
      emit(sb.toString());
      prefix += "  ";
    }

    public void exitScope(TestScope scope) {
      if (!scope.locals.isEmpty()) {
        emit("locals:   " + scope.locals);
      }
      if (!scope.read.isEmpty()) {
        emit("read:     " + scope.read);
      }
      if (!scope.assigned.isEmpty()) {
        emit("assigned: " + scope.assigned);
      }
      prefix = prefix.substring(2);
      emit("exitScope at " + scope.depth);
    }

    public void inScope(AncestorChain<?> ac, TestScope scope) { /* noop */ }

    public void masked(
        AncestorChain<Identifier> id, TestScope narrower, TestScope wider) {
      emit("masked " + id.node.getName() + " at " + wider.depth);
    }

    public void read(
        AncestorChain<Identifier> id, TestScope useSite, TestScope defSite) {
      if (defSite != null) {
        defSite.read.add(id.node.getName() + " at " + useSite.depth);
      } else {
        useSite.getRoot().read.add(
            "outer " + id.node.getName() + " at " + useSite.depth);
      }
    }

    public void splitInitialization(
        AncestorChain<Identifier> declared, TestScope declScope,
        AncestorChain<Identifier> initialized, TestScope catchScope) {
      emit("split initialization of " + declared.node.getName()
           + " at " + declScope.depth + " into " + catchScope.depth);
    }
  }

  private AncestorChain<Block> program(String... lines) throws ParseException {
    return AncestorChain.instance(js(fromString(Join.join("\n", lines))));
  }

  private void assertEvents(String... expected) {
    MoreAsserts.assertListsEqual(Arrays.asList(expected), events);
  }

  private void assertScoping(
      AncestorChain<Block> program, List<ScopeAnalyzer<TestScope>> impls,
      String... goldenEvents) {
    assertTrue(events.isEmpty());
    for (ScopeAnalyzer<TestScope> impl : impls) {
      impl.apply(program);
      assertEvents(goldenEvents);
      events.clear();
    }
  }

  private static <S extends AbstractScope>
  List<ScopeAnalyzer<S>> allImplementations(ScopeListener<S> listener) {
    List<ScopeAnalyzer<S>> impls = Lists.newArrayList();
    impls.add(new ES5ScopeAnalyzer<S>(listener));
    impls.add(new JScriptScopeAnalyzer<S>(listener));
    impls.add(new WorstCaseScopeAnalyzer<S>(listener));
    return impls;
  }

  private static <S extends AbstractScope>
  List<ScopeAnalyzer<S>> notJScript(ScopeListener<S> listener) {
    List<ScopeAnalyzer<S>> impls = Lists.newArrayList();
    impls.add(new ES5ScopeAnalyzer<S>(listener));
    impls.add(new WorstCaseScopeAnalyzer<S>(listener));
    return impls;
  }
}
