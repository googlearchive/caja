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
    runQUnitTestCase("accordion", 282);
    // Current failure categories:
    //   * JSHint glue "error loading source"
    //   * various
  }

  public final void testAutocomplete() throws Exception {
    runQUnitTestCase("autocomplete", 33);
    // Current failure categories:
    //   * JSHint glue "error loading source"
    //   * "null is not extensible"
  }

  public final void testButton() throws Exception {
    runQUnitTestCase("button", 82);
    // Current failure categories:
    //   * JSHint glue "error loading source"
  }

  public final void testCore() throws Exception {
    runQUnitTestCase("core", 142);
  }

  public final void testDatepicker() throws Exception {
    runQUnitTestCase("datepicker", 129);
    // Current failure categories:
    //   * "Unrecognized event type Events"
    //   * "Unrecognized event type MouseEvents"
    //   * ""init" blocked by Caja"
  }

  public final void testDialog() throws Exception {
    runQUnitTestCase("dialog", 76);
    // Current failure categories:
    //   * JSHint glue "error loading source"
    //   * "Cannot set "el"" and "Cannot set "container""
    //   * Some numeric discrepancies
  }

  public final void testEffects() throws Exception {
    runQUnitTestCase("effects", 195);
    // Current failure categories:
    //   * JSHint glue "error loading source"
    //   * cssText values in "animateClass clears style properties when stopped"
  }

  public final void testMenu() throws Exception {
    runQUnitTestCase("menu", 145);
    // Current failure categories:
    //   * JSHint glue "error loading source"
    //   * "Unrecognized event type Events"
  }

  public final void testPosition() throws Exception {
    runQUnitTestCase("position", 83);
    // Current failure categories:
    //   * JSHint glue "error loading source"
  }

  public final void testProgressbar() throws Exception {
    runQUnitTestCase("progressbar", 40);
    // Current failure categories:
    //   * JSHint glue "error loading source"
  }

  public final void testSlider() throws Exception {
    runQUnitTestCase("slider", 88);
    // Current failure categories:
    //   * JSHint glue "error loading source"
    //   * "Unrecognized event type Events"
  }

  public final void testSpinner() throws Exception {
    runQUnitTestCase("spinner", 29);
    // Current failure categories:
    //   * JSHint glue "error loading source"
    //   * Lots of "null is not extensible"
  }

  // Test suite hangs on a broken ajax call, so will always fail.
  //public final void testTabs() throws Exception {
  //  runQUnitTestCase("tabs", null);
  //}

  public final void testTooltip() throws Exception {
    runQUnitTestCase("tooltip", 82);
    // Current failure categories:
    //   * JSHint glue "error loading source"
  }

  public final void testWidget() throws Exception {
    runQUnitTestCase("widget", 281);
    // Current failure categories:
    //   * JSHint glue "error loading source"
  }

}
