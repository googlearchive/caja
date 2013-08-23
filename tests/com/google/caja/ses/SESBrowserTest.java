// Copyright (C) 2013 Google Inc.
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

package com.google.caja.ses;

import org.junit.runner.RunWith;

import com.google.caja.plugin.CatalogRunner;
import com.google.caja.plugin.CatalogTestCase;

/**
 * Browser-driving tests for SES standalone.
 *
 * @author kpreid@switchb.org
 */
@RunWith(CatalogRunner.class)
@CatalogRunner.CatalogName("ses-tests.json")
public class SESBrowserTest extends CatalogTestCase {

  @Override
  protected boolean alwaysCapture(String label) {
    return label.equals("ses-explicit");
  }
}
