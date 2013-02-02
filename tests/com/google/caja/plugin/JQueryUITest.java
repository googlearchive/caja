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
 * @author kpreid@switchb.org
 */
public class JQueryUITest extends QUnitTestCase {

  @Override
  protected String getTestURL(String testCase) {
    return "/third_party/js/jquery-ui/tests/unit/" + testCase + "/" + testCase +
        ".html";
  }

  public final void testAccordion() throws Exception {
    runQUnitTestCase("accordion", 285);
    // Current failure categories:
    //   * unexamined
  }

  public final void testAutocomplete() throws Exception {
    runQUnitTestCase("autocomplete", 120);
    // Current modifications made to test suite:
    //   * Work around lost-signal problems due to lack of event simulation
    // Current failure categories:
    //   * keystroke response discrepancies
    //   * unexamined
  }

  public final void testButton() throws Exception {
    runQUnitTestCase("button", null);
  }

  public final void testCore() throws Exception {
    runQUnitTestCase("core", 143);
    // Current failure categories
    //   * Something about focusability
  }

  public final void testDatepicker() throws Exception {
    runQUnitTestCase("datepicker", 482);
    // Current failure categories:
    //   * keystroke response discrepancies
    //   * parseDate
    //   * got absolute URL where relative expected
  }

  public final void testDialog() throws Exception {
    runQUnitTestCase("dialog", 226 /* Firefox; ??? on Chrome */);
    // Current failure categories:
    //   * What may be event simulation failures
    //   * Some numeric discrepancies
  }

  public final void testEffects() throws Exception {
    runQUnitTestCase("effects", 208);
    // Current failure categories:
    //   * cssText values in "animateClass clears style properties when stopped"
  }

  public final void testMenu() throws Exception {
    runQUnitTestCase("menu", 164);
    // Current failure categories:
    //   * unexamined
  }

  public final void testPosition() throws Exception {
    runQUnitTestCase("position", null);
  }

  public final void testProgressbar() throws Exception {
    runQUnitTestCase("progressbar", null);
  }

  public final void testSlider() throws Exception {
    runQUnitTestCase("slider", 92);
    // Current failure categories:
    //   * Keyboard events not working
  }

  public final void testSpinner() throws Exception {
    runQUnitTestCase("spinner", 137);
    // Current failure categories:
    //   * unexamined
  }

  public final void testTabs() throws Exception {
    runQUnitTestCase("tabs", 488);
    // Current modifications made to test suite:
    //   * Work around lost-signal problems due to lack of event simulation
    // Current failure categories:
    //   * not yet examined
  }

  public final void testTooltip() throws Exception {
    runQUnitTestCase("tooltip", null);
  }

  public final void testWidget() throws Exception {
    runQUnitTestCase("widget", null);
  }

}
