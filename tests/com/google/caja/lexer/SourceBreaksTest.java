// Copyright (C) 2005 Google Inc.
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

package com.google.caja.lexer;

import com.google.caja.util.CajaTestCase;

public class SourceBreaksTest extends CajaTestCase {
  SourceBreaks breaks;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    breaks = new SourceBreaks(is, 1);
    breaks.lineStartsAt(4);
    breaks.lineStartsAt(10);
    breaks.lineStartsAt(16);
    breaks.lineStartsAt(17);
    breaks.lineStartsAt(18);
    breaks.lineStartsAt(22);
    breaks.lineStartsAt(30);
  }

  public final void testCharInLineAt() {
    assertEquals(1, breaks.charInLineAt(1));
    assertEquals(2, breaks.charInLineAt(2));
    assertEquals(3, breaks.charInLineAt(3));
    assertEquals(1, breaks.charInLineAt(4));
    assertEquals(2, breaks.charInLineAt(5));
    assertEquals(3, breaks.charInLineAt(6));
    assertEquals(4, breaks.charInLineAt(7));
    assertEquals(5, breaks.charInLineAt(8));
    assertEquals(6, breaks.charInLineAt(9));
    assertEquals(1, breaks.charInLineAt(10));
    assertEquals(2, breaks.charInLineAt(11));
    assertEquals(3, breaks.charInLineAt(12));
    assertEquals(4, breaks.charInLineAt(13));
    assertEquals(5, breaks.charInLineAt(14));
    assertEquals(6, breaks.charInLineAt(15));
    assertEquals(1, breaks.charInLineAt(16));
    assertEquals(1, breaks.charInLineAt(17));
    assertEquals(1, breaks.charInLineAt(18));
    assertEquals(2, breaks.charInLineAt(19));
    assertEquals(3, breaks.charInLineAt(20));
    assertEquals(4, breaks.charInLineAt(21));
    assertEquals(1, breaks.charInLineAt(22));
    assertEquals(2, breaks.charInLineAt(23));
    assertEquals(3, breaks.charInLineAt(24));
    assertEquals(4, breaks.charInLineAt(25));
    assertEquals(5, breaks.charInLineAt(26));
    assertEquals(6, breaks.charInLineAt(27));
    assertEquals(7, breaks.charInLineAt(28));
    assertEquals(8, breaks.charInLineAt(29));
    assertEquals(1, breaks.charInLineAt(30));
    assertEquals(2, breaks.charInLineAt(31));
    assertEquals(3, breaks.charInLineAt(32));
  }

  public final void testLineAt() {
    assertEquals(1, breaks.lineAt(1));
    assertEquals(1, breaks.lineAt(2));
    assertEquals(1, breaks.lineAt(3));
    assertEquals(2, breaks.lineAt(4));
    assertEquals(2, breaks.lineAt(5));
    assertEquals(2, breaks.lineAt(6));
    assertEquals(2, breaks.lineAt(7));
    assertEquals(2, breaks.lineAt(8));
    assertEquals(2, breaks.lineAt(9));
    assertEquals(3, breaks.lineAt(10));
    assertEquals(3, breaks.lineAt(11));
    assertEquals(3, breaks.lineAt(12));
    assertEquals(3, breaks.lineAt(13));
    assertEquals(3, breaks.lineAt(14));
    assertEquals(3, breaks.lineAt(15));
    assertEquals(4, breaks.lineAt(16));
    assertEquals(5, breaks.lineAt(17));
    assertEquals(6, breaks.lineAt(18));
    assertEquals(6, breaks.lineAt(19));
    assertEquals(6, breaks.lineAt(20));
    assertEquals(6, breaks.lineAt(21));
    assertEquals(7, breaks.lineAt(22));
    assertEquals(7, breaks.lineAt(23));
    assertEquals(7, breaks.lineAt(24));
    assertEquals(7, breaks.lineAt(25));
    assertEquals(7, breaks.lineAt(26));
    assertEquals(7, breaks.lineAt(27));
    assertEquals(7, breaks.lineAt(28));
    assertEquals(7, breaks.lineAt(29));
    assertEquals(8, breaks.lineAt(30));
    assertEquals(8, breaks.lineAt(31));
    assertEquals(8, breaks.lineAt(32));
  }
}
