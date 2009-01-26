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

package com.google.caja.plugin;

import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.Expression;
import com.google.caja.util.CajaTestCase;
import com.google.caja.util.Name;
import com.google.caja.util.TestUtil;

public class DomProcessingEventsTest extends CajaTestCase {

  public void testAttribsMustBeClosed() throws Exception {
    try {
      DomProcessingEvents dpe = new DomProcessingEvents();
      dpe.begin(Name.html("p"));
      dpe.end(Name.html("p"));
      fail();
    } catch (IllegalStateException ex) {
      // pass
    }
    try {
      DomProcessingEvents dpe = new DomProcessingEvents();
      dpe.begin(Name.html("p"));
      dpe.pcdata("Hello");
      fail();
    } catch (IllegalStateException ex) {
      // pass
    }
    try {
      DomProcessingEvents dpe = new DomProcessingEvents();
      dpe.begin(Name.html("p"));
      dpe.toJavascript(new Block());
      fail();
    } catch (IllegalStateException ex) {
      // pass
    }
    try {
      DomProcessingEvents dpe = new DomProcessingEvents();
      dpe.begin(Name.html("p"));
      dpe.toJavascript(new Block());
      fail();
    } catch (IllegalStateException ex) {
      // pass
    }
    try {
      DomProcessingEvents dpe = new DomProcessingEvents();
      dpe.begin(Name.html("p"));
      dpe.attr(Name.html("foo"), "bar");
      dpe.toJavascript(new Block());
      fail();
    } catch (IllegalStateException ex) {
      // pass
    }
    try {
      DomProcessingEvents dpe = new DomProcessingEvents();
      dpe.begin(Name.html("p"));
      dpe.begin(Name.html("p"));
      fail();
    } catch (IllegalStateException ex) {
      // pass
    }
    try {
      DomProcessingEvents dpe = new DomProcessingEvents();
      dpe.begin(Name.html("p"));
      dpe.finishAttrs(false);
      dpe.attr(Name.html("foo"), "bar");
      fail();
    } catch (IllegalStateException ex) {
      // pass
    }
    try {
      DomProcessingEvents dpe = new DomProcessingEvents();
      dpe.attr(Name.html("foo"), "bar");
      fail();
    } catch (IllegalStateException ex) {
      // pass
    }
    try {
      DomProcessingEvents dpe = new DomProcessingEvents();
      dpe.finishAttrs(false);
      fail();
    } catch (IllegalStateException ex) {
      // pass
    }
    try {
      DomProcessingEvents dpe = new DomProcessingEvents();
      dpe.begin(Name.html("p"));
      dpe.script(js(fromString("foo();")));
      fail();
    } catch (IllegalStateException ex) {
      // pass
    }
  }

  public void testUnbalancedTags() {
    try {
      DomProcessingEvents dpe = new DomProcessingEvents();
      dpe.begin(Name.html("p"));
      dpe.finishAttrs(false);
      dpe.end(Name.html("q"));
      dpe.toJavascript(new Block());
      fail();
    } catch (IllegalStateException ex) {
      // pass
    }
    try {
      DomProcessingEvents dpe = new DomProcessingEvents();
      dpe.begin(Name.html("p"));
      dpe.finishAttrs(false);
      dpe.toJavascript(new Block());
      fail();
    } catch (IllegalStateException ex) {
      // pass
    }
    try {
      DomProcessingEvents dpe = new DomProcessingEvents();
      dpe.begin(Name.html("p"));
      dpe.finishAttrs(false);
      dpe.begin(Name.html("p"));
      dpe.finishAttrs(false);
      dpe.end(Name.html("p"));
      dpe.end(Name.html("p"));
      dpe.end(Name.html("p"));
      dpe.toJavascript(new Block());
      fail();
    } catch (IllegalStateException ex) {
      // pass
    }
  }

  public void testTooMuchRecursionFix() throws Exception {
    Expression x = jsExpr(fromString("x"));
    DomProcessingEvents dpe = new DomProcessingEvents();
    for (int i = 0; i < 30; ++i) {
      dpe.begin(Name.html("p"));
      dpe.attr(Name.html("id"), x);  // defeat optimization
      dpe.finishAttrs(false);
    }
    for (int i = 0; i < 30; ++i) { dpe.end(Name.html("p")); }

    Block block = new Block();
    dpe.toJavascript(block);
    TestUtil.removePseudoNodes(block);

    String prefix = "IMPORTS___.htmlEmitter___";
    String startOne = ".b('p').a('id',x).f(false)";
    String startTen = (startOne + startOne + startOne + startOne + startOne
                       + startOne + startOne + startOne + startOne + startOne);
    String endOne = ".e('p')";
    String endTen = (endOne + endOne + endOne + endOne + endOne
                     + endOne + endOne + endOne + endOne + endOne);

    assertEquals(
        "{"
        + prefix + startTen + startTen + startTen + endTen + ";"
        // Split across two lines
        + prefix + endTen + endTen + ";"
        + "}",
        minify(block));
  }
}
