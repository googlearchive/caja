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

package com.google.caja.demos.applet;

import com.google.caja.reporting.TestBuildInfo;
import com.google.caja.util.CajaTestCase;
import com.google.caja.util.TestUtil;

import java.awt.HeadlessException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Tests the public interface of CajaApplet without invoking a browser
 *
 * @author Jasvir Nagra <jasvir@gmail.com>
 */
public class CajaAppletTest extends CajaTestCase {

  private CajaApplet makeApplet() throws HeadlessException {
    return new CajaApplet() {
        { setBuildInfo(new TestBuildInfo()); }
        @Override
        public URL getDocumentBase() {
          try {
            return new URL("http://example.com");
          } catch (MalformedURLException e) {
            assert(false);
          }
          return null;
        }
    };
  }

  public final void testCajoleInValija() throws Exception {
    if (checkHeadless()) return;
    CajaApplet applet = makeApplet();
    assertEquals(
        TestUtil.readResource(getClass(), "caja-applet-valija-golden.js"),
        applet.cajole("Howdy<script>alert(2 + 2);</script>There", "")
            .replace("\\n", "\n"));
  }
}
