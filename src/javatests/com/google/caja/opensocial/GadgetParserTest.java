// Copyright (C) 2007 Google Inc.
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

package com.google.caja.opensocial;

import com.google.caja.util.TestUtil;
import junit.framework.TestCase;

import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;

/**
 * @author ihab.awad@gmail.com (Ihab Awad)
 */
public class GadgetParserTest extends TestCase {

  private interface Tests {
    void test(GadgetSpec spec);
  }

  public void testListFriends() throws Exception {
    testFile("listfriends.xml", new Tests() {
      public void test(GadgetSpec spec) {
        assertEquals(1, spec.getRequiredFeatures().size());
        assertEquals("opensocial-0.5", spec.getRequiredFeatures().get(0));
        assertEquals(1, spec.getModulePrefs().size());
        assertEquals("My Friends List", spec.getModulePrefs().get("title"));
        assertEquals("html", spec.getContentType());
      }
    });
  }

  public void testTestParsing() throws Exception {
    testFile("test-parsing.xml", new Tests() {
      public void test(GadgetSpec spec) {
        assertEquals(2, spec.getRequiredFeatures().size());
        assertEquals("opensocial-0.5", spec.getRequiredFeatures().get(0));
        assertEquals("dynamic-height", spec.getRequiredFeatures().get(1));
        assertEquals(10, spec.getModulePrefs().size());
        assertEquals("A title", spec.getModulePrefs().get("title"));
        assertEquals("html", spec.getContentType());
      }
    });
  }

  private GadgetSpec parseFile(String gadgetFile) throws Exception {
    return parseString(TestUtil.readResource(getClass(), gadgetFile));
  }

  private GadgetSpec parseString(String gadgetSpec) throws Exception {
    return new GadgetParser().parse(new StringReader(gadgetSpec));
  }

  private String render(GadgetSpec spec) throws Exception {
    StringWriter output = new StringWriter();
    new GadgetParser().render(spec, output);
    return output.toString();
  }

  private void testFile(String gadgetFile, Tests tests) throws Exception {
    GadgetSpec spec = parseFile(gadgetFile);
    tests.test(spec);
    tests.test(parseString(render(spec)));
  }
}