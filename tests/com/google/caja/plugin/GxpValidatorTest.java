// Copyright (C) 2006 Google Inc.
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

import com.google.caja.lang.html.HtmlSchema;
import com.google.caja.parser.AncestorChain;
import com.google.caja.parser.html.DomTree;
import com.google.caja.util.CajaTestCase;

/**
 * @author mikesamuel@gmail.com (Mike Samuel)
 */
public class GxpValidatorTest extends CajaTestCase {
  public void testValidate() throws Exception {
    validate("<br/>", true);
    validate("<b>Hello</b>", true);
    validate("<b unknown=\"bogus\">Hello</b>", false);
    validate("<b id=\"bold\">Hello</b>", true);
    validate("<bogus id=\"bold\">Hello</bogus>", false);
    validate("<bogus unknown=\"bogus\">Hello</bogus>", false);
    validate("<script>disallowed</script>", false);
    validate("<b><gxp:attr name=\"id\">hi</gxp:attr>Hello</b>", true);
    validate("<b expr:id=\"yo()\">Hello</b>", true);
    validate("<b><gxp:attr name=\"bogus\">hi</gxp:attr>Hello</b>", false);
    validate("<b expr:bogus=\"yo()\">Hello</b>", false);
    validate("<b><gxp:attr>hi</gxp:attr>Hello</b>", false);
  }

  private void validate(String xhtml, boolean valid) throws Exception {
    DomTree t = xml(fromString(xhtml));
    assertEquals(xhtml, valid,
                 new GxpValidator(HtmlSchema.getDefault(mq), mq)
                 .validate(new AncestorChain<DomTree>(t)));
  }
}
