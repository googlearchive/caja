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

package com.google.caja.plugin;

import org.openqa.selenium.WebDriver;

/**
 * For starting up the server and browser a BrowserTestCase does, then
 * stopping so that you can try making requests to server and/or see how the
 * browser behaves in the test environment.
 *
 * To run this easily, 'ant runbrowserstub'.
 *
 * @author kpreid@switchb.org (Kevin Reid)
 */
public class BrowserTestCaseStub extends BrowserTestCase {
  public static final void main(String[] args) throws Exception {
    new BrowserTestCaseStub().runBrowserTest("");
  }

  @Override
  @SuppressWarnings("deprecation")
  protected String driveBrowser(WebDriver driver, Object data, String pageName) {
    Thread.currentThread().suspend();
    return "";
  }
}
