// Copyright 2008 Google Inc. All Rights Reserved.
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

package com.google.caja.plugin.stages;

import com.google.caja.parser.AncestorChain;
import com.google.caja.parser.js.Block;
import com.google.caja.plugin.Job;
import com.google.caja.plugin.Jobs;
import com.google.caja.plugin.PluginMeta;
import com.google.caja.reporting.Message;
import com.google.caja.util.CajaTestCase;
import com.google.caja.util.Pipeline;
import com.google.caja.util.RhinoTestBed;

/**
 * @author msamuel@google.com (Mike Samuel)
 */
public class DebuggingSymbolsStageTest extends CajaTestCase {

  public void testAttachedMethodReflection() throws Exception {
    assertStackTrace(
          "function Point() {}\n"
        + "Point.prototype.add3 = function(x){return x+3;};\n"
        + "var p = new Point();\n"
        + "p.add3.call({}, 4);\n",
        //        ^^^^ 4+8 - 12
        "testAttachedMethodReflection:4+8 - 12");
    assertStackTrace(
          "function Point() {}\n"
        + "Point.prototype.add3 = function(x){return x+3;};\n"
        + "var p = new Point();\n"
        + "p.add3.apply({}, [4]);\n",
        //        ^^^^^ 4+8 - 13
        "testAttachedMethodReflection:4+8 - 13");
    assertStackTrace(
          "function Point() { this; }\n"
        + "Point.prototype.freezeMe = function(){this.freeze_();};\n"
        + "Point.prototype.addXProperty = function(x){this.x = 1;};\n"
        //                                                 ^ 3+49 - 50
        + "var p = new Point();\n"
        + "p.freezeMe();\n"
        + "p.addXProperty(1);\n",
        //   ^^^^^^^^^^^^ 6+3 - 15
          "testAttachedMethodReflection:6+3 - 15\n"
        + "testAttachedMethodReflection:3+49 - 50");
    assertStackTrace(
          "function Point() { this; }\n"
        + "Point.prototype.freeze = function(){this.freeze_.call();};\n"
        //                                                 ^^^^^ 2+50 - 54
        + "var p = new Point();\n"
        + "p.freeze();\n",
        //   ^^^^^^ 4+3 - 9
          "testAttachedMethodReflection:4+3 - 9\n"
        + "testAttachedMethodReflection:2+50 - 54");
  }

  public void testDereferenceNull() throws Exception {
    assertStackTrace(
        "var x = null;\n"
        + "var xDotFoo = (x).foo;",
        //                   ^^^ 2+19-22

        "testDereferenceNull:2+19 - 22");
  }

  public void testCallOnNullObject() throws Exception {
    assertStackTrace(
        "{\n"
        + "  function f(x) { return x.foo(); }\n"
        //                            ^^^ 2+28-31
        + "  function g() { return f(null); }\n"
        //                         ^ 3+25-26
        + "\n"
        + "  g();\n"
        //   ^ 5+3-4
        + "}",

        "testCallOnNullObject:5+3 - 4\n"
        + "testCallOnNullObject:3+25 - 26\n"
        + "testCallOnNullObject:2+28 - 31");
  }

  public void testCallUndefinedMethod() throws Exception {
    assertStackTrace(
        "{\n"
        + "  function f(x) { return x.noSuchMethod(); }\n"
        //                            ^^^^^^^^^^^^ 2+28-40
        + "  function g() { return f(new Date); }\n"
        //                         ^ 3+25-26
        + "\n"
        + "  g();\n"
        //   ^ 5+3-4
        + "}",

        "testCallUndefinedMethod:5+3 - 4\n"
        + "testCallUndefinedMethod:3+25 - 26\n"
        + "testCallUndefinedMethod:2+28 - 40");
  }

  public void testInaccessibleProperty() throws Exception {
    assertStackTrace(
        "{\n"
        + "  function f(x, k) { return x['foo_'] = 0; }\n"
        //                             ^^^^^^^^^^^^^ 2+29-42
        + "  f(new Date);\n"
        //   ^ 3+3-4
        + "}",

        "testInaccessibleProperty:3+3 - 4\n"
        + "testInaccessibleProperty:2+29 - 42");
  }

  public void testSetOfNullObject() throws Exception {
    assertStackTrace(
        "(null).x = 0;",
        //      ^ 1+8-9
        "testSetOfNullObject:1+8 - 9");
  }

  public void testDeleteOfNullObject() throws Exception {
    assertStackTrace(
        "delete (null).x;",
        //       ^^^^^^^ 1+9-16
        "testDeleteOfNullObject:1+9 - 16");
  }

  public void testMethodCalling() throws Exception {
    assertStackTrace(
        "{\n"
        + "function f(x) { return x.foo; }\n"
        //                          ^^^ 2+26-29
        + "\n"
        + "function Clazz() { this; }\n"
        + "caja.def(\n"
        + "    Clazz, Object,\n"
        + "    { method: function () { return f(this.foo_); } });\n"
        //                                    ^ 7+36-37
        + "\n"
        + "(new Clazz).method(null);\n"
        //             ^^^^^^ 9+13-19
        + "}",

        "testMethodCalling:9+13 - 19\n"
        + "testMethodCalling:7+36 - 37\n"
        + "testMethodCalling:2+26 - 29");
  }

  public void testEnumerateOfNull() throws Exception {
    assertStackTrace(
        "(function () {\n"
        + "  var myObj = null;\n"
        + "  for (var k in myObj) {\n"
        //                 ^^^^^ 3+17-22
        + "    ;\n"
        + "  }\n"
        + "})();",

        "testEnumerateOfNull:2+7 - 4+6\n"
        + "testEnumerateOfNull:3+17 - 22");
  }

  public void testPropertyInNull() throws Exception {
    assertStackTrace(
        "(function (x) {\n"
        + "  return 'k' in x;\n"
        //                 ^ 2+17-18
        + "})(null);",

        "testPropertyInNull:1+12 - 2+18\n"
        + "testPropertyInNull:2+17 - 18");
  }

  public void testIllegalAccessInsideHoistedFunction() throws Exception {
    assertStackTrace(
        "var x = true;\n"
        + "if (x) {\n"
        + "  var y = 5;\n"
        + "  function f() {\n"
        + "    var x = 'y___';\n"
        + "    this[x] = 1;\n"
        //          ^^^^^^
        + "  }\n"
        + "}\n"
        + "new f();",
        //     ^

        "testIllegalAccessInsideHoistedFunction:9+5 - 6\n"
        + "testIllegalAccessInsideHoistedFunction:6+10 - 16");
  }

  public void testWrappedConstructors() throws Exception {
    // Normally, trying to subclass a Date results in an object whose
    // methods fail with 'Method "<name>" called on incompatible object.'
    // in both Rhino and SpiderMonkey.

    // Make sure that the way we wrap constructors to catch errors does not
    // cause (new Date()) to produce an unusable value.
    assertConsistent("new Date(0).toString()");
    assertConsistent("new Array(4).length");
    assertConsistent("new Array('4.1').length");
    assertConsistent("new Array(0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15).length");
    assertConsistent("(new RegExp('foo', 'i')).test('foobar')");
    assertConsistent(
        ""
        + "(function () {\n"
        + "  function sum(nums) {\n"
        + "    var n = 0;\n"
        + "    for (var i = nums.length; --i >= 0;) { n += nums[i]; }\n"
        + "    return n;\n"
        + "  }\n"
        + "  function Clazz() { this.x = sum(arguments); }\n"
        + "  caja.def(Clazz, Object, {get: function () { return this.x; }});\n"
        + "  var c = new Clazz(0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16);\n"
        + "  return c.get() + ',' + (c instanceof Clazz);\n"
        + "})()");
    assertConsistent(
        ""
        + "(function () {\n"
        + "  function sum(nums) {\n"
        + "    var n = 0;\n"
        + "    for (var i = nums.length; --i >= 0;) { n += nums[i]; }\n"
        + "    return n;\n"
        + "  }\n"
        + "  function Clazz() { this.x = sum(arguments); }\n"
        + "  caja.def(Clazz, Object, {get: function () { return this.x; }});\n"
        + "  function Factory (a,b,c,d,e,f,g,h,i,j,k,l,m,n,o,p,q) {\n"
        + "    return new Clazz(a,b,c,d,e,f,g,h,i,j,k,l,m,n,o,p,q);\n"
        + "  }\n"
        + "  var c = new Factory(0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16);\n"
        + "  return c.get() + ',' + (c instanceof Clazz);\n"
        + "})()");
  }

  public void testExophora() throws Exception {
    System.err.println("\n\n\ntestExophora");
    assertConsistent("({ f: function (y) { return this.x * y; }, x: 4 }).f(2)");
    assertStackTrace(
        "({ f: function (y) { return this[y](); }, x: 4 }).f('foo___')",
        //                                ^ 1+34-35        ^ 1+51-52

        "testExophora:1+51 - 52\n"
        + "testExophora:1+34 - 35");
  }

  private void assertStackTrace(String js, String golden) throws Exception {
    runCajoled(js, golden,
               "var stack = '<no-stack>';                              "
               + "try {                                                "
               + "  %s                                                 "
               + "} catch (e) {                                        "
               + "  stack = e.cajaStack___;                            "
               + "  if (!stack) { throw e; }                           "
               + "  stack = ___.unsealCallerStack(stack).join('\\n');  "
               + "}                                                    "
               + "stack                                               ");
  }

  private void assertConsistent(String js) throws Exception {
    // Execute js in the presence of caja so it can use caja.def.
    Object golden = RhinoTestBed.runJs(
        null,
        new RhinoTestBed.Input(getClass(), "/com/google/caja/caja.js"),
        new RhinoTestBed.Input(js, getName()));
    runCajoled("result(" + js + ");", golden,
               "var output = '<no-output>';"
               + "___.getNewModuleHandler().getImports().result = "
               + "    ___.simpleFunc(function (x) { output = x; });"
               + "%s;"
               + "output");
  }

  private void runCajoled(String js, Object golden, String context)
      throws Exception {
    Block block = js(fromString(js));
    System.err.println("\n\nblock\n=====\n" + block.toStringDeep(1));

    PluginMeta meta = new PluginMeta();
    meta.setDebugMode(true);
    meta.setWartsMode(true);

    Jobs jobs = new Jobs(mc, mq, meta);
    jobs.getJobs().add(new Job(AncestorChain.instance(block)));

    Pipeline<Jobs> pipeline = new Pipeline<Jobs>();
    pipeline.getStages().add(new ConsolidateCodeStage());
    pipeline.getStages().add(new ValidateJavascriptStage());
    pipeline.getStages().add(new InferFilePositionsStage());
    pipeline.getStages().add(new DebuggingSymbolsStage());
    if (!pipeline.apply(jobs)) {
      StringBuilder sb = new StringBuilder();
      for (Message msg : mq.getMessages()) {
        if (0 == sb.length()) { sb.append('\n'); }
        sb.append(msg.getMessageLevel()).append(": ");
        msg.format(mc, sb);
      }
      fail(sb.toString());
    }

    block = jobs.getJobs().get(0).getRoot().cast(Block.class).node;

    try {
      String cajoled = String.format(context, render(block));
      Object actual = RhinoTestBed.runJs(
          null,
          new RhinoTestBed.Input(getClass(), "../console-stubs.js"),
          new RhinoTestBed.Input(getClass(), "/com/google/caja/caja.js"),
          new RhinoTestBed.Input(
              getClass(), "/com/google/caja/log-to-console.js"),
          new RhinoTestBed.Input(
              getClass(), "/com/google/caja/caja-debugmode.js"),
          new RhinoTestBed.Input(cajoled, getName()));
      assertEquals(golden, actual);
    } catch (Exception ex) {
      System.err.println(render(block));
      throw ex;
    } catch (Error ex) {
      System.err.println(render(block));
      throw ex;
    }
  }
}
