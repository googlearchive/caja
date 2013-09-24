// Copyright (C) 2008 Google Inc.
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

package com.google.caja.ancillary.linter;

import com.google.caja.lexer.InputSource;
import com.google.caja.parser.js.Block;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.SimpleMessageQueue;
import com.google.caja.util.CajaTestCase;
import com.google.caja.util.MoreAsserts;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class LinterTest extends CajaTestCase {

  public final void testProvides() throws Exception {
    runLinterTest(
        jobs(new LintJobMaker(js(fromString("var x;"))).make()),
        "LINT: testProvides:1+1 - 6: Undocumented global x");
    runLinterTest(
        jobs(new LintJobMaker(js(fromString("var x = 1;")))
             .withProvides("x")
             .make())
        );
    runLinterTest(
        jobs(new LintJobMaker(js(fromString("var x = 1;")))
             .withProvides("x", "y")
             .make()),
        "ERROR: testProvides: @provides y not provided"
        );
    runLinterTest(
        jobs(new LintJobMaker(js(fromString(
             ""
             + "var x = 1;\n"
             + "var y;\n"
             + "if (Math.random()) { y = 1; }"  // Not always assigned.
             )))
             .withProvides("x", "y")
             .make()),
        "ERROR: testProvides: @provides y not provided"
        );
    runLinterTest(
        jobs(new LintJobMaker(js(fromString("for (var i;;) {}"))).make()),
        "LINT: testProvides:1+6 - 11: Undocumented global i");
  }

  public final void testMultiplyProvidedSymbols() throws Exception {
    runLinterTest(
        jobs(new LintJobMaker(js(fromString(
            "var foo = 0, bar = 1;", new InputSource(URI.create("test:///f1"))
            )))
            .withProvides("foo", "bar")
            .make(),
            new LintJobMaker(js(fromString(
            "var bar = 2, baz = 4;", new InputSource(URI.create("test:///f2"))
            )))
            .withProvides("bar", "baz")
            .make()),
        "ERROR: f2: Another input, f1, already @provides bar");
  }

  public final void testRequires() throws Exception {
    runLinterTest(
        jobs(new LintJobMaker(js(fromString("x();"))).make()),
        "ERROR: testRequires:1+1 - 2: Symbol x has not been defined");
    runLinterTest(
        jobs(new LintJobMaker(js(fromString("x();")))
             .withRequires("x")
             .make())
        );
    runLinterTest(
        jobs(new LintJobMaker(js(fromString("x();")))
             .withRequires("x", "y")
             .make()),
        "ERROR: testRequires: @requires y not used"
        );
  }

  public final void testOverrides() throws Exception {
    runLinterTest(
        jobs(new LintJobMaker(js(fromString(
            "Object = Array;\n")))
        .make()),
        "ERROR: testOverrides:1+1 - 7: Invalid assignment to Object");
    runLinterTest(
        jobs(new LintJobMaker(js(fromString(
            "Date.now = function () { return (new Date).getTime(); };\n")))
        .make()),
        "ERROR: testOverrides:1+1 - 5: Invalid assignment to Date.now");
    runLinterTest(
        jobs(new LintJobMaker(js(fromString(
            "Date.now = function () { return (new Date).getTime(); };\n")))
        .withOverrides("Date")
        .make()));
    runLinterTest(
        jobs(new LintJobMaker(js(fromString(
            "var Date;\n")))
        .withOverrides("Date")
        .make()));
    // No override needed when used as a RHS
    runLinterTest(
        jobs(new LintJobMaker(js(fromString(
            "var data = {}; data[Date] = new Date();\n")))
        .withProvides("data")
        .make()));
  }

  public final void testFunctionScopes() throws Exception {
    runLinterTest(
        jobs(new LintJobMaker(js(fromString(
            ""
            + "function p(b, c) {\n"
            + "  var d;\n"
            + "  d = r * a - b + c * r;\n"
            + "  return d;"
            + "}")))
            .withProvides("p")
            .withRequires("r")
            .make()),
        "ERROR: testFunctionScopes:3+11 - 12: Symbol a has not been defined");
  }

  public final void testRedefinition() throws Exception {
    runLinterTest(
        jobs(new LintJobMaker(js(fromString(
            "(function () { var a = 1; var a = 2; })();"))).make()),
        ("LINT: testRedefinition:1+27 - 36: "
         + "a originally defined at testRedefinition:1+16 - 25"));
  }

  public final void testCatchBlocks() throws Exception {
    runLinterTest(
        jobs(new LintJobMaker(js(fromString(
            ""
            + "try {\n"
            + "  throw caution();\n"
            + "} catch (e) {\n"
            + "  if (e.to === THE_WIND) {\n"
            + "    panic(e);\n"
            + "  }\n"
            + "}")))
            .withRequires("caution", "THE_WIND")
            .make()),
        "ERROR: testCatchBlocks:5+5 - 10: Symbol panic has not been defined");
    runLinterTest(
        jobs(new LintJobMaker(js(fromString(
            ""
            + "try {\n"
            + "  throw new Error();\n"
            + "} catch (e) {\n"
            + "  // swallow exception\n"
            + "}")))
            .withProvides("e")
            .make()),
        "ERROR: testCatchBlocks: @provides e not provided");
    runLinterTest(
        jobs(new LintJobMaker(js(fromString(
            ""
            + "var e;\n"
            + "try {\n"
            + "  throw new Error();\n"
            + "} catch (e) {\n"
            + "  // swallow exception\n"
            + "}")))
            .make()),
        "LINT: testCatchBlocks:1+1 - 6: Undocumented global e",
        ("WARNING: testCatchBlocks:4+10 - 11: Declaration of e masks"
         + " declaration at testCatchBlocks:1+1 - 6"));
    runLinterTest(
        jobs(new LintJobMaker(js(fromString(
            ""
            + "try {\n"
            + "  throw new Error();\n"
            + "} catch (e) {\n"
            + "  var f = e.stack;\n"
            + "  print(f);\n"
            + "}")))
            .withRequires("print")
            .make()),
        "LINT: testCatchBlocks:4+3 - 18: Undocumented global f");
    runLinterTest(
        jobs(new LintJobMaker(js(fromString(
            ""
            + "function g(e, f) {\n"
            + "  try {\n"
            + "    f();\n"
            + "  } catch (e) {\n"  // does not mask the formal parameter e
            + "    panic();\n"
            + "  }\n"
            + "  return e;\n"
            + "}")))
            .withProvides("g")
            .make()),
        ("WARNING: testCatchBlocks:4+12 - 13:"
         + " Declaration of e masks declaration at testCatchBlocks:1+12 - 13"),
        "ERROR: testCatchBlocks:5+5 - 10: Symbol panic has not been defined");
    runLinterTest(
        jobs(new LintJobMaker(js(fromString(
            ""
            + "function g(e, f) {\n"
            + "  try {\n"
            + "    f();\n"
            + "  } catch (e) {\n"
            + "    var e = f(false);\n"
            + "    return e + 1;\n"
            + "  }\n"
            + "  return e;\n"
            + "}")))
            .withProvides("g")
            .make()),
        ("LINT: testCatchBlocks:5+5 - 21: e originally defined at"
         + " testCatchBlocks:1+12 - 13"),
        ("WARNING: testCatchBlocks:4+12 - 13: Declaration of e"
         + " masks declaration at testCatchBlocks:1+12 - 13"));
  }

  public final void testLoops() throws Exception {
    runLinterTest(
        jobs(new LintJobMaker(js(fromString(
            ""
            + "(function () {\n"
            + "  for (var i = 0; i < 10; ++i) { f(i); }\n"
            + "  for (var i = 10; --i >= 0;) { f(i); }\n"
            + "})();")))
            .withRequires("f").make())
        );
    runLinterTest(
        jobs(new LintJobMaker(js(fromString(
            ""
            + "(function () {\n"
            + "  for (var i = 0; i < 10; ++i) {\n"
            + "    for (var i = 10; --i >= 0;) { f(i); }\n"
            + "  }\n"
            + "})();")))
            .withRequires("f").make()),
        ("ERROR: testLoops:3+10 - 20:"
         + " Declaration of i masks declaration at testLoops:2+8 - 17"));
    runLinterTest(
        jobs(new LintJobMaker(js(fromString(
            ""
            + "(function () {\n"
            + "  for (var i = 0; i < 10; ++i) { f(i); }\n"
            + "  return i;"
            + "})();")))
            .withRequires("f").make()),
        ("ERROR: testLoops:3+10 - 11: Usage of i declared at "
         + "testLoops:2+8 - 17 is out of block scope.")
        );
    runLinterTest(
        jobs(new LintJobMaker(js(fromString(
            ""
            + "(function (i) {\n"  // variable i
            + "  return function (arr) {\n"
            + "    var sum = 0;\n"
            + "    for (var i = arr.length; --i >= 0;) {\n"  // i masks formal
            + "      sum += arr[i];\n"
            + "    }\n"
            + "    return sum / i;\n"  // apparent reference to a masked outer
            + "  };\n"
            + "})(4);"))).make()),
        ("ERROR: testLoops:7+18 - 19: Usage of i declared at "
         + "testLoops:4+10 - 28 is out of block scope.")
        );
    runLinterTest(
        jobs(new LintJobMaker(js(fromString(
            ""
            + "(function () {\n"
            + "  var k;\n"
            + "  for (k in o);\n"
            + "})();"))).make()),
        "ERROR: testLoops:3+13 - 14: Symbol o has not been defined"
        );
    runLinterTest(
        jobs(new LintJobMaker(js(fromString("for (var k in o);"))).make()),
        "ERROR: testLoops:1+15 - 16: Symbol o has not been defined",
        "LINT: testLoops:1+6 - 11: Undocumented global k");
  }

  public final void testIgnoredValue() throws Exception {
    runLinterTest(
        jobs(new LintJobMaker(js(fromString(
            ""
            + "var c; f;  \n"  // Line 1
            + "+n;  \n"  // line 2
            + "a.b;  \n"  // line 3
            + "f && g();  f && g;  f() && g;  \n"  // line 4: 1st OK, others not
            + "a = b, c = d;  \n"  // line 5
            + "a = b;  \n"  // OK
            + "m += n;  \n"  // OK
            + "f();  \n"  // OK
            + "new Array;  \n"  // line 9
            + "new Array();  \n"  // line 10
            + "for (a = b, c = d; !a; ++a, --m, ++c) f;  \n"  // line 11
            + "++c;  \n"  // OK
            + "while (1) { 1; }\n"  // line 13.  First allowed, second not
            + "({ x: 32 });\n"
            + "new Array()();\n"
            //          1         2         3         4
            // 1234567890123456789012345678901234567890
            )))
            .withRequires("b", "d", "f", "g", "n")
            .withOverrides("a", "m")
            .withProvides("c")
            .make()),
        "WARNING: testIgnoredValue:1+8 - 9: Operation has no effect",
        "WARNING: testIgnoredValue:2+1 - 3: Operation has no effect",
        "WARNING: testIgnoredValue:3+1 - 4: Operation has no effect",
        "WARNING: testIgnoredValue:4+12 - 18: Operation has no effect",
        "WARNING: testIgnoredValue:4+21 - 29: Operation has no effect",
        "WARNING: testIgnoredValue:5+1 - 13: Operation has no effect",
        "WARNING: testIgnoredValue:9+1 - 10: Operation has no effect",
        "WARNING: testIgnoredValue:10+1 - 12: Operation has no effect",
        "WARNING: testIgnoredValue:11+39 - 40: Operation has no effect",
        "WARNING: testIgnoredValue:13+13 - 14: Operation has no effect",
        "WARNING: testIgnoredValue:14+1 - 12: Operation has no effect");
  }

  public final void testDeadCode() throws Exception {
    runLinterTest(
        jobs(new LintJobMaker(js(fromString(
            ""
            + "(function () {\n"
            + "  return\n"
            + "      foo();\n"
            + "})();")))
            .withRequires("foo")
            .make()),
        // runLinterTest makes its own message queue so the lint message about
        // semicolons does not show here.
        // Linter's main method does use the same message queue though, so
        // parsing messages will be reported.
        "WARNING: testDeadCode:3+7 - 12: Code is not reachable");
    runLinterTest(
        jobs(new LintJobMaker(js(fromString(
            ""
            + "with (o) {\n"
            // Is reachable even though code within a with block is not
            // analyzable.  Ignore reachability in with blocks.
            + "  foo();\n"
            + "}\n")))
            .withRequires("o", "foo")
            .make())
        );
  }

  public final void testBareKeywords() throws Exception {
    runLinterTest(
        jobs(new LintJobMaker(js(fromString(
            ""
            + "var p = { 'if': 1 };\n"
            + "p['if']++\n"
            + "var q = { if: 2 };\n"
            + "q.if++\n"
            + "function f() {\n"
            + "  var r = { 'for': 3 };\n"
            + "  r['for']++;\n"
            + "  var s = { for: 4 };\n"
            + "  s.for++;\n"
            + "}\n")))
            .withProvides("p", "q", "f")
            .make()),
        ("ERROR: testBareKeywords:3+11 - 13:"
         + " IE<=8 does not allow bare literal use of keyword 'if'"),
        ("ERROR: testBareKeywords:4+3 - 5:"
         + " IE<=8 does not allow bare literal use of keyword 'if'"),
        ("ERROR: testBareKeywords:8+13 - 16:"
         + " IE<=8 does not allow bare literal use of keyword 'for'"),
        ("ERROR: testBareKeywords:9+5 - 8:"
         + " IE<=8 does not allow bare literal use of keyword 'for'"));
  }

  public final void testLiveness() throws Exception {
    runLinterTest(
        jobs(new LintJobMaker(js(fromString(
            ""
            + "var a;\n"
            + "if (Math.random()) {\n"
            + "  a = new Error();\n"
            + "}\n"
            + "throw a;\n"
            )))
            .withProvides("a")
            .make()),
        ("WARNING: testLiveness:5+7 - 8: Symbol a may be used before"
         + " being initialized"),
        "ERROR: testLiveness: @provides a not provided",
        ("LINT: testLiveness:5+1 - 8:"
         + " Uncaught exception thrown during initialization")
        );
  }

  public final void testLabels() throws Exception {
    runLinterTest(
        jobs(new LintJobMaker(js(fromString(
            "foo: for (;;) { foo: for (;;); }"))).make()),
        ("ERROR: testLabels:1+17 - 31:"
         + " Label foo nested inside testLabels:1+1 - 33"));
    // Side-by-side is fine.
    runLinterTest(
        jobs(new LintJobMaker(js(fromString(
            "foo: for (;;); foo: for (;;);"))).make()));
    // And it's ok if they have the default label
    runLinterTest(
        jobs(new LintJobMaker(js(fromString(
            "for (;;) { for (;;); }"))).make()));
    // or different labels
    runLinterTest(
        jobs(new LintJobMaker(js(fromString(
            "foo: for (;;) { bar: for (;;); }"))).make()));
  }

  public final void testMisplacedExits() throws Exception {
    runLinterTest(
        jobs(new LintJobMaker(js(fromString("return;"))).make()),
        ("ERROR: testMisplacedExits:1+1 - 7:"
         + " Return does not appear inside a function"));
    runLinterTest(
        jobs(new LintJobMaker(js(fromString(
            "foo: for (;;) { break bar; }"
            ))).make()),
        ("ERROR: testMisplacedExits:1+17 - 26:"
         + " Unmatched break or continue to label bar"));
    runLinterTest(
        jobs(new LintJobMaker(js(fromString(
            "switch (Math.random()) { case 0: continue; }"
            ))).make()),
        ("ERROR: testMisplacedExits:1+34 - 42:"
         + " Unmatched break or continue to label <default>"));
  }

  public final void testIEQuirksScoping() throws Exception {
    runLinterTest(
        jobs(new LintJobMaker(js(fromString(
            ""
            + "var myObject = {\n"
            + "    foo: function foo() {}\n"
            + "};\n"
            )))
            .withProvides("myObject")
            .make()),
        "LINT: testIEQuirksScoping:2+10 - 27: Undocumented global foo");
    // No problem if done inside a closure.
    runLinterTest(
        jobs(new LintJobMaker(js(fromString(
            ""
            + "var myObject = (function () {\n"
            + "  return {\n"
            + "      foo: function foo() {}\n"
            + "  };\n"
            + "})();"
            )))
            .withProvides("myObject")
            .make()));
    // Unless doing so would conflict with a local variable.
    // Even if the variable isn't used later, it could still break recursion
    // within the function.
    runLinterTest(
        jobs(new LintJobMaker(js(fromString(
            ""
            + "var myObject = (function () {\n"
            + "  var foo = {\n"
            + "      foo: function foo() {}\n"
            + "  };\n"
            + "  return foo;\n"
            + "})();"
            )))
            .withProvides("myObject")
            .make()),
        ("LINT: testIEQuirksScoping:3+12 - 29: foo originally defined"
         + " at testIEQuirksScoping:2+3 - 4+4"));
    // And we report a slightly different message when there's a block scope in
    // between.
    runLinterTest(
        jobs(new LintJobMaker(js(fromString(
            ""
            + "var myObject = (function () {\n"
            + "  var foo;\n"
            + "  do \n"
            + "    foo = {\n"
            + "        foo: function foo() {}\n"
            + "    };\n"
            + "  while (false);\n"
            + "  return foo;\n"
            + "})();"
            )))
            .withProvides("myObject")
            .make()),
        ("LINT: testIEQuirksScoping:5+14 - 31: foo originally defined"
         + " at testIEQuirksScoping:2+3 - 10"));
  }

  // TODO(mikesamuel):
  // check that function bodies either never return or never complete.
  // E.g. function f(x, y) { if (x) { return y; } } is missing an else.

  final static class LintJobMaker {
    private final Block node;
    private final Set<String> provides = Sets.newLinkedHashSet(),
        requires = Sets.newLinkedHashSet(),
        overrides = Sets.newLinkedHashSet();
    LintJobMaker(Block node) {
      this.node = node;
    }

    LintJobMaker withProvides(String... idents) {
      provides.addAll(Arrays.asList(idents));
      return this;
    }

    LintJobMaker withRequires(String... idents) {
      requires.addAll(Arrays.asList(idents));
      return this;
    }

    LintJobMaker withOverrides(String... idents) {
      overrides.addAll(Arrays.asList(idents));
      return this;
    }

    Linter.LintJob make() {
      return new Linter.LintJob(
          node.getFilePosition().source(), requires, provides, overrides, node);
    }
  }

  private void runLinterTest(List<Linter.LintJob> inputs, String... messages) {
    MessageQueue mq = new SimpleMessageQueue();
    Linter.lint(inputs, new Linter.Environment(Sets.<String>newHashSet()), mq);
    List<String> actualMessageStrs = Lists.newArrayList();
    for (Message msg : mq.getMessages()) {
      actualMessageStrs.add(
          msg.getMessageLevel().name() + ": " + msg.format(mc));
    }

    List<String> goldenMessageStrs = Lists.newArrayList(messages);
    Collections.sort(actualMessageStrs);
    Collections.sort(goldenMessageStrs);
    MoreAsserts.assertListsEqual(goldenMessageStrs, actualMessageStrs);
  }

  private static List<Linter.LintJob> jobs(Linter.LintJob... jobs) {
    return Arrays.asList(jobs);
  }
}
