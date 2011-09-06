// Copyright (C) 2011 Google Inc.
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

import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.TokenConsumer;
import com.google.caja.reporting.JsIdentifierSyntax;
import com.google.caja.reporting.RenderContext;
import com.google.caja.util.CajaTestCase;

import javax.annotation.Nullable;

public class IdentifierTest extends CajaTestCase {
  private void gwtY(String s) {
    assertTrue(
        "Should be valid GWT identifier: " + s,
        Identifier.isValidGWT(s));
  }

  private void gwtN(String s) {
    assertFalse(
        "Should NOT be valid GWT identifier: " + s,
        Identifier.isValidGWT(s));
  }

  public final void testGwtIdentifiers() throws Exception {
    gwtY("@com.foo.Bar::baz");
    gwtY("@com.foo.Bar::baz([Ljava/lang/String;)");

    gwtY("@Bar::baz([Ljava/lang/String;)");
    gwtY("@Bar::baz([Ljava/lang/String;[[[DLjava/lang/Object;)");

    gwtN("com");
    gwtN("com.foo.Bar");
    gwtN("com.foo.Bar::baz");
    gwtN("com.foo.Bar::baz()");
    gwtN("com.foo.Bar::baz(Ljava/lang/String;)");

    gwtN("@com");
    gwtN("@com.foo.Bar");
    gwtN("@com.foo.Bar::");
    gwtN("@com.foo.Bar()");
    gwtN("@com.foo.Bar::()");
    gwtN("@com.foo.Bar(Ljava/lang/String;)");
    gwtN("@com.foo.Bar::(Ljava/lang/String;)");
  }

  private void parseY(String s) {
    new Identifier(FilePosition.UNKNOWN, s);
  }

  private void parseN(String s) {
    try {
      new Identifier(FilePosition.UNKNOWN, s);
      fail("Identifier <" + s + "> should have been disallowed");
    } catch (Exception e) {
      // pass
    }
  }

  public final void testParse() throws Exception {
    parseY("foo");
    parseY("foo123");
    parseY("@foo");
    parseN("&foo");
    parseN("foo&");
    parseN("foo.bar");
    parseN("foo::bar");
  }

  private static Object[] rc(JsIdentifierSyntax s) {
    final Object[] result = new Object[2];
    result[1] = "";
    result[0] = (new RenderContext(new TokenConsumer() {
      @Override public void mark(@Nullable FilePosition pos) {}
      @Override public void consume(String text) {
        result[1] = result[1] + text;
      }
      @Override public void noMoreTokens() {}
    })).withJsIdentiferSyntax(s);
    return result;
  }

  private void renderY(String s, JsIdentifierSyntax jis) {
    Identifier id = new Identifier(FilePosition.UNKNOWN, s);
    Object[] rc = rc(jis);
    id.render((RenderContext) rc[0]);
    assertEquals(
        "Identifier <" + s + "> "
        + "should have been allowed to render in mode " + jis
        + "(was actually rendered as <" + rc[1] + ">)",
        s,
        rc[1]);
  }

  private void renderN(String s, JsIdentifierSyntax jis) {
    Identifier id = new Identifier(FilePosition.UNKNOWN, s);
    Object[] rc = rc(jis);
    try {
      id.render((RenderContext) rc[0]);
      fail("Should not have allowed identifier <" + s + ">"
          + " to be rendered in mode " + jis);
    } catch (Exception e) {
      // pass
    }
  }

  public final void testRenderJavascript() throws Exception {
    renderY("foo", JsIdentifierSyntax.JAVASCRIPT);
    renderN("@foo", JsIdentifierSyntax.JAVASCRIPT);
    renderN("@a.b.Foo::bar(Ljava/lang/String;)", JsIdentifierSyntax.JAVASCRIPT);
  }

  public final void testRenderQuasiliteral() throws Exception {
    renderY("foo", JsIdentifierSyntax.QUASILITERAL);
    renderY("@foo", JsIdentifierSyntax.QUASILITERAL);
    renderN("@a.b.Foo::bar(Ljava/lang/String;)",
        JsIdentifierSyntax.QUASILITERAL);
  }

  public final void testRenderGwt() throws Exception {
    renderY("foo", JsIdentifierSyntax.GWT);
    renderN("@foo", JsIdentifierSyntax.GWT);
    renderY("@a.b.Foo::bar(Ljava/lang/String;)", JsIdentifierSyntax.GWT);
  }
}
