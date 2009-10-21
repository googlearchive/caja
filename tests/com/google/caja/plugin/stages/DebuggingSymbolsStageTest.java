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
import com.google.caja.parser.js.CajoledModule;
import com.google.caja.plugin.Job;
import com.google.caja.plugin.Jobs;
import com.google.caja.plugin.PluginMeta;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.TestBuildInfo;
import com.google.caja.util.CajaTestCase;
import com.google.caja.util.Executor;
import com.google.caja.util.FailureIsAnOption;
import com.google.caja.util.Pipeline;
import com.google.caja.util.RhinoTestBed;

/**
 * @author mikesamuel@gmail.com (Mike Samuel)
 */
public class DebuggingSymbolsStageTest extends CajaTestCase {

  public final void testDereferenceNull() throws Exception {
    assertStackTrace(
        "var x = null;\n"
        + "var xDotFoo = (x).foo;",
        //                ^^^^^^ 2+16-22

        "testDereferenceNull:2+16 - 22");
  }

  public final void testCallOnNullObject() throws Exception {
    assertStackTrace(
        "{\n"
        + "  function f(x) { return x.foo(); }\n"
        //                          ^^^^^ 2+26-31
        + "  function g() { return f(null); }\n"
        //                         ^ 3+25-26
        + "\n"
        + "  g();\n"
        //   ^ 5+3-4
        + "}",

        "testCallOnNullObject:5+3 - 4\n"
        + "testCallOnNullObject:3+25 - 26\n"
        + "testCallOnNullObject:2+26 - 31");
  }

  public final void testCallUndefinedMethod() throws Exception {
    assertStackTrace(
        "{\n"
        + "  function f(x) { return x.noSuchMethod(); }\n"
        //                          ^^^^^^^^^^^^^^ 2+26-40
        + "  function g() { return f(new Date); }\n"
        //                         ^ 3+25-26
        + "\n"
        + "  g();\n"
        //   ^ 5+3-4
        + "}",

        "testCallUndefinedMethod:5+3 - 4\n"
        + "testCallUndefinedMethod:3+25 - 26\n"
        + "testCallUndefinedMethod:2+26 - 40");
  }

  public final void testReflectiveInvocation() throws Exception {
    // Constructors cannot be called or applied unless they are also simple fns.
    assertStackTrace(
        "Date.call({}, 4);\n",
        //^^^^^^^^^^^^^^ 1+1 - 16
        "testReflectiveInvocation:1+1 - 16");
    assertStackTrace(
        "Date.apply({}, 4);\n",
        //^^^^^^^^^^^^^^^ 1+1 - 17
        "testReflectiveInvocation:1+1 - 17");
  }

  public final void testInaccessibleProperty() throws Exception {
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

  public final void testSetOfNullObject() throws Exception {
    assertStackTrace(
        "(null).x = 0;",
        //^^^^^^^^^^^ 1+2-13
        "testSetOfNullObject:1+2 - 13");
  }

  public final void testDeleteOfNullObject() throws Exception {
    assertStackTrace(
        "{ delete (null).x; }",
        // ^^^^^^^^^^^^^^^ 1+3-18
        "testDeleteOfNullObject:1+3 - 18");
  }

  public final void testSetOfFrozenObject() throws Exception {
    assertStackTrace(
        ""
        + "var o = cajita.freeze({ x: 1 });\n"
        + "(null).x = 0;",
        //  ^^^^^^^^^^^ 2+2-13
        "testSetOfFrozenObject:2+2 - 13");
  }

  public final void testDeleteOfFrozenObject() throws Exception {
    assertStackTrace(
        ""
        + "var o = cajita.freeze({ x: 1 });\n"
        + "delete (null).x;",
        // ^^^^^^^^^^^^^^^ 2+1-16
        "testDeleteOfFrozenObject:2+1 - 16");
  }

  @FailureIsAnOption
  public final void testEnumerateOfNull() throws Exception {
    assertStackTrace(
        ""
        + "{\n"
        + "  (function () {\n"
        //    ^ 2+4
        + "    var myObj = null;\n"
        + "    for (var k in myObj) {\n"
        //                   ^^^^^ 4+19-24
        + "      ;\n"
        + "    }\n"
        + "  })();\n"
        //   ^ 7+4
        + "}",

        "testEnumerateOfNull:2+4 - 7+4\n"
        + "testEnumerateOfNull:4+19 - 24");
  }

  public final void testPropertyInNull() throws Exception {
    assertStackTrace(
        ""
        + "(function (x) {\n"
        //  ^ 1+2
        + "  return 'k' in x;\n"
        //          ^^^^^^^^ 2+10-18
        + "})(null);",
        //  ^ 3+2

        "testPropertyInNull:1+2 - 3+2\n"
        + "testPropertyInNull:2+10 - 18");
  }

  public final void testConstruction() throws Exception {
    assertStackTrace(
        ""
        + "var foo = function () { throw new Error('hi'); };\n"
        //                         ^^^^^^^^^^^^^^^^^^^^^ 1+25-46
        + "new foo();",
        // ^^^^^^^^^ 2+1-10

        "testConstruction:2+1 - 10\n"
        + "testConstruction:1+25 - 46"
        );
  }

  public final void testIllegalAccessInsideHoistedFunction() throws Exception {
    assertStackTrace(
        "var x = true;\n"
        + "var foo = {};\n"
        + "if (x) {\n"
        + "  var y = 5;\n"
        + "  function f() {\n"
        + "    var x = 'y___';\n"
        + "    foo[x] = 1;\n"
        //     ^^^^^^^^^^ 7+5 - 15
        + "  }\n"
        + "}\n"
        + "new f();",
        // ^^^^^^^ 10+1 - 8

        "testIllegalAccessInsideHoistedFunction:10+1 - 8\n"
        + "testIllegalAccessInsideHoistedFunction:7+5 - 15");
  }

  public final void testWrappedConstructors() throws Exception {
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
        + "  function Clazz() {\n"
        + "    var x = sum(arguments);\n"
        + "    return { get: function() { return x; } };\n"
        + "  }\n"
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
        + "  function Clazz() {\n"
        + "    var x = sum(arguments);\n"
        + "    return { get: function() { return x; } };\n"
        + "  }\n"
        + "  function Factory (a,b,c,d,e,f,g,h,i,j,k,l,m,n,o,p,q) {\n"
        + "    return new Clazz(a,b,c,d,e,f,g,h,i,j,k,l,m,n,o,p,q);\n"
        + "  }\n"
        + "  var c = new Factory(0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16);\n"
        + "  return c.get() + ',' + (c instanceof Clazz);\n"
        + "})()");
  }

  private void assertStackTrace(String js, String golden) throws Exception {
    runCajoled(js, golden,
               "var stack = '<no-stack>';                              "
               + "try {                                                "
               + "  %s                                                 "
               + "} catch (e) {                                        "
               + "  stack = e.cajitaStack___;                          "
               + "  if (!stack) { throw e; }                           "
               + "  stack = stack.join('\\n');  "
               + "}                                                    "
               + "stack                                               ");
  }

  private void assertConsistent(String js) throws Exception {
    // Execute js in the presence of cajita so it can use Caja symbols.
    Object golden = RhinoTestBed.runJs(
        new Executor.Input(
            getClass(), "/js/json_sans_eval/json_sans_eval.js"),
        new Executor.Input(getClass(), "/com/google/caja/cajita.js"),
        new Executor.Input(js, getName()));
    runCajoled("result(" + js + ");", golden,
               "var output = '<no-output>';"
               + "___.getNewModuleHandler().getImports().result = "
               + "    ___.markFuncFreeze(function (x) { output = x; });"
               + "%s;"
               + "output");
  }

  private void runCajoled(String js, Object golden, String context)
      throws Exception {
    Block uncajoledModuleBody = js(fromString(js));
    System.err.println("\n\nblock\n=====\n" +
                       uncajoledModuleBody.toStringDeep(1));

    PluginMeta meta = new PluginMeta();
    meta.setDebugMode(true);

    Jobs jobs = new Jobs(mc, mq, meta);
    jobs.getJobs().add(new Job(AncestorChain.instance(uncajoledModuleBody)));

    Pipeline<Jobs> pipeline = new Pipeline<Jobs>();
    pipeline.getStages().add(new ConsolidateCodeStage());
    pipeline.getStages().add(new ValidateJavascriptStage(new TestBuildInfo()));
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

    CajoledModule cajoledModule =
        jobs.getJobs().get(0).getRoot().cast(CajoledModule.class).node;

    try {
      String cajoledText = String.format(context, render(cajoledModule));
      Object actual = RhinoTestBed.runJs(
          new Executor.Input(
              getClass(), "/js/json_sans_eval/json_sans_eval.js"),
          new Executor.Input(getClass(), "../console-stubs.js"),
          new Executor.Input(getClass(), "/com/google/caja/cajita.js"),
          new Executor.Input(getClass(), "/com/google/caja/log-to-console.js"),
          new Executor.Input(
              getClass(), "/com/google/caja/cajita-debugmode.js"),
          new Executor.Input(cajoledText, getName()));
      assertEquals(golden, actual);
    } catch (Exception ex) {
      System.err.println(render(cajoledModule));
      throw ex;
    } catch (Error ex) {
      System.err.println(render(cajoledModule));
      throw ex;
    }
  }
}
