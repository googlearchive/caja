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

package com.google.caja.reporting;

import com.google.caja.lexer.FilePosition;
import com.google.caja.util.CajaTestCase;
import java.util.Collections;

/**
 * @author mikesamuel@gmail.com
 */
public class HtmlSnippetProducerTest extends CajaTestCase {
  public final void testSnippetEscaped() {
    String src = "<style>background: url('http://<h1>foo</h1>')</style>";
    HtmlSnippetProducer sp = new HtmlSnippetProducer(
        Collections.singletonMap(is, src), mc);
    FilePosition pos = FilePosition.instance(is, 1, 24, 24, 21);
    String snippet = sp.getSnippet(new Message(
        MessageType.MALFORMED_URI,
        pos, MessagePart.Factory.valueOf("http://<h1>foo</h1>")));
    assertEquals(
        ""
        + "<a href=\"#\" class=\"filepos nocode\""
        + " onclick=\"selectLine(&#39;"
        + "http://example.org/testSnippetEscaped&#39;,1,24,1,45)\">"
        + "testSnippetEscaped:1</a>"
        + ": &lt;style&gt;background: url("
        + "<span class=\"problem\">"
        + "&#39;http://&lt;h1&gt;foo&lt;/h1&gt;&#39;"
        + "</span>"
        + ")&lt;/style&gt;",

        snippet);
  }
}
