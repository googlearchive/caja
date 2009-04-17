// Copyright (C) 2009 Google Inc.
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

package com.google.caja.parser.html;

import junit.framework.TestCase;

public class NodesTest extends TestCase {
  public void testDecode() throws Exception {
    assertEquals(Nodes.decode("1 &lt; 2 &amp;&amp; 4 &gt; &quot;3&quot;"),
                 "1 < 2 && 4 > \"3\"");
    assertEquals("", Nodes.decode(""));
    assertEquals("No entities here", Nodes.decode("No entities here"));
    assertEquals("No entities here & there",
                 Nodes.decode("No entities here & there"));
    // Test that interrupted escapes and escapes at beginning and end of file
    // are handled gracefully.
    assertEquals("\\\\u000a", Nodes.decode("\\\\u000a"));
    assertEquals("\n", Nodes.decode("&#x00000a;"));
    assertEquals("\n", Nodes.decode("&#x0000a;"));
    assertEquals("\n", Nodes.decode("&#x000a;"));
    assertEquals("\n", Nodes.decode("&#x00a;"));
    assertEquals("\n", Nodes.decode("&#x0a;"));
    assertEquals("\n", Nodes.decode("&#xa;"));
    assertEquals(String.valueOf(Character.toChars(0x10000)),
                 Nodes.decode("&#x10000;"));
    assertEquals("&#xa", Nodes.decode("&#xa"));
    assertEquals("&#x00ziggy", Nodes.decode("&#x00ziggy"));
    assertEquals("&#xa00z;", Nodes.decode("&#xa00z;"));
    assertEquals("&#\n", Nodes.decode("&#&#x000a;"));
    assertEquals("&#x\n", Nodes.decode("&#x&#x000a;"));
    assertEquals("&#xa\n", Nodes.decode("&#xa&#x000a;"));
    assertEquals("&#\n", Nodes.decode("&#&#xa;"));
    assertEquals("&#x", Nodes.decode("&#x"));
    assertEquals("&#x0", Nodes.decode("&#x0"));
    assertEquals("&#", Nodes.decode("&#"));

    assertEquals("\\", Nodes.decode("\\"));
    assertEquals("&", Nodes.decode("&"));

    assertEquals("&#000a;", Nodes.decode("&#000a;"));
    assertEquals("\n", Nodes.decode("&#10;"));
    assertEquals("\n", Nodes.decode("&#010;"));
    assertEquals("\n", Nodes.decode("&#0010;"));
    assertEquals("\n", Nodes.decode("&#00010;"));
    assertEquals("\n", Nodes.decode("&#000010;"));
    assertEquals("\n", Nodes.decode("&#0000010;"));
    assertEquals("\t", Nodes.decode("&#9;"));

    assertEquals("&#10", Nodes.decode("&#10"));
    assertEquals("&#00ziggy", Nodes.decode("&#00ziggy"));
    assertEquals("&#\n", Nodes.decode("&#&#010;"));
    assertEquals("&#0\n", Nodes.decode("&#0&#010;"));
    assertEquals("&#01\n", Nodes.decode("&#01&#10;"));
    assertEquals("&#\n", Nodes.decode("&#&#10;"));
    assertEquals("&#1", Nodes.decode("&#1"));
    assertEquals("&#10", Nodes.decode("&#10"));

    // test the named escapes
    assertEquals("<", Nodes.decode("&lt;"));
    assertEquals(">", Nodes.decode("&gt;"));
    assertEquals("\"", Nodes.decode("&quot;"));
    assertEquals("'", Nodes.decode("&apos;"));
    assertEquals("&", Nodes.decode("&amp;"));
    assertEquals("&lt;", Nodes.decode("&amp;lt;"));
    assertEquals("&", Nodes.decode("&AMP;"));
    assertEquals("&AMP", Nodes.decode("&AMP"));
    assertEquals("&", Nodes.decode("&AmP;"));
    assertEquals("\u0391", Nodes.decode("&Alpha;"));
    assertEquals("\u03b1", Nodes.decode("&alpha;"));

    assertEquals("&;", Nodes.decode("&;"));
    assertEquals("&bogus;", Nodes.decode("&bogus;"));
  }
}
