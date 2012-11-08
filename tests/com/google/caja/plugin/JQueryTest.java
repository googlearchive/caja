// Copyright (C) 2012 Google Inc.
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



/**
 * @author metaweta@gmail.com
 */
public class JQueryTest extends QUnitTestCase {

  public final void testCore() throws Exception {
    runQUnitTestCase("core", 1285);
    // Current modifications made to test suite:
    //   * Removed unnecessary octal literal.
    // Current failure categories:
    //   * Complaint about lack of PHP server
    //   * TODO(jasvir): window.eval is absent (this includes the jQuery('html')
    //     failure)
    //   * We don't implement XML yet.
    //   * We don't implement iframes yet.
    //   * We don't implement document.styleSheets.
    //   * We don't implement document.getElementsByName.
  }

  public final void testCallbacks() throws Exception {
    runQUnitTestCase("callbacks", null);
    // Current modifications made to test suite:
    //   * Adjusted "context is window" test assuming callee is non-strict
  }

  public final void testDeferred() throws Exception {
    runQUnitTestCase("deferred", null);
    // Current modifications made to test suite:
    //   * Adjusted tests assuming callee is non-strict
  }

  public final void testSupport() throws Exception {
    runQUnitTestCase("support", 1);
    // Current failure categories:
    //   * We don't implement iframes yet.
  }

  public final void testData() throws Exception {
    runQUnitTestCase("data", 290);
    // Current failure categories:
    //   * We don't implement iframes yet (used incidentally).
  }

  public final void testQueue() throws Exception {
    runQUnitTestCase("queue", null);
  }

  public final void testAttributes() throws Exception {
    runQUnitTestCase("attributes", 418);
    // Current failure categories:
    //   * URI rewriting is visible to the guest.
    //   * Simple event handler rewriting is visible to the guest.
    //   * We don't implement XML yet.
    //   * Unknown - "Second radio was checked when clicked" - .click() problem?
    //   * Rejection of HTML5 autofocus attribute assignment is visible.
    //   * Removing style= attributes is misbehaving according to jQuery.
    //   * We don't support tabindex on non-form-elements yet (HTML5).
    //   * We don't support document.createAttribute yet.
    //   * Something to do with multiple-select.
    //   * Expects a form name/id (?) to be reflected on document.
  }

  public final void testEvent() throws Exception {
    runQUnitTestCase("event", 377);
    // Current failure categories:
    //   * Various lost-signal failures:
    //        in 'bind(),live(),delegate() with non-null,defined data'
    //        live() and delegate() tests
    //        trigger() tests
    //   * We don't implement document.createEvent of other than 'HTMLEvents'
    //   * We don't implement iframes yet.
    //   * jQuery reports leak in 'bind(name, false), unbind(name, false)'
    //   * submit listeners not firing in 'trigger(type, [data], [fn])'
    //   * "Object [domado object HTMLInputElement] has no method 'click'"
    //   * Something about "quickIs".
  }

  public final void testSelector() throws Exception {
    runQUnitTestCase("selector", 25);
    // Current failure categories:
    //   * We don't implement iframes yet.
  }

  public final void testTraversing() throws Exception {
    runQUnitTestCase("traversing", 286);
    // Current failure categories:
    //   * We don't implement iframes yet.
  }

  public final void testManipulation() throws Exception {
    runQUnitTestCase("manipulation", 474);
    // Current modifications made to test suite:
    //   * Removed SES-incompatible Array.prototype modification; was only for
    //     testing jQuery robustness.
    // Current failure categories:
    //   * Something wrong with checked radio buttons.
    //   * Something wrong with jQuery's <script>-based ajax transport.
    //   * We don't make non-JS <script> elements readable/preserved.
    //   * We don't implement XML yet.
    //   * We don't support runtime-created <style> elements, even virtualized?
    //   * Something wrong with "jQuery.cleanData" test.
    //   * We don't implement some case of dynamic <script> creation that
    //     "html() - execute scripts..." and "html() - script exceptions..."
    //     are using.
    //   * "window.eval is undefined" in appendTo test -- REGRESSION
  }

  public final void testCSS() throws Exception {
    runQUnitTestCase("css", 196);
    // Current failure categories:
    //   * We don't implement SVG (fill-opacity CSS property).
    //   * Something doesn't work such that defaultDisplay() in jquery falls
    //     back to a strategy creating an iframe, which we don't implement.
    //     This means that .show() and presumably .hide() doesn't work.
  }

  // Currently doesn't work because jQuery needs a PHP sever for ajax tests.
  /*
    public final void testAJAX() throws Exception {
      runJQueryTestCase("ajax", ??);
    }
  */

  public final void testEffects() throws Exception {
    runQUnitTestCase("effects", 528);
    // Current modifications made to test suite:
    //   * Fixed maybe-accidental undeclared global 'calls'.
    // Current failure categories:
    //   * We don't implement SVG (fill-opacity CSS property).
  }

  public final void testOffset() throws Exception {
    runQUnitTestCase("offset", 18);
    // Current failure categories:
    //   * We don't implement iframes yet.
  }

  public final void testDimensions() throws Exception {
    runQUnitTestCase("dimensions", 133);
    // Current modifications made to test suite:
    //   * Fixed nested function in strict mode.
    // Current failure categories:
    //   * We don't implement iframes yet.
  }

  public final void testExports() throws Exception {
    runQUnitTestCase("exports", null);
  }

  @Override
  protected String getTestURL(String testCase) {
    return "/ant-testlib/js/jqueryjs/test/" +
    testCase +
    "-uncajoled.html";
  }
}
