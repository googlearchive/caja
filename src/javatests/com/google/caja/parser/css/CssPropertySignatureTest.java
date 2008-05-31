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

package com.google.caja.parser.css;

import com.google.caja.util.CajaTestCase;
import com.google.caja.util.MoreAsserts;

import java.io.IOException;
import java.util.Arrays;

public class CssPropertySignatureTest extends CajaTestCase {
  public void testParser1() {
    assertParseTree(
        "<length> <length>? | inherit",

        "SetSignature",
        "  SeriesSignature",
        "    SymbolSignature : length",
        "    RepeatedSignature : [0, 1]",
        "      SymbolSignature : length",
        "  LiteralSignature : inherit"
        );
  }

  public void testParser2() {
    assertParseTree(
        "<border-style>{1,4} | inherit",

        "SetSignature",
        "  RepeatedSignature : [1, 4]",
        "    SymbolSignature : border-style",
        "  LiteralSignature : inherit"
        );
  }

  public void testParser3() {
    assertParseTree(
        "[ <border-width> || <border-style> || 'border-top-color' ] | inherit",

        "SetSignature",
        "  RepeatedSignature : [1, 3]",
        "    ExclusiveSetSignature",
        "      SymbolSignature : border-width",
        "      SymbolSignature : border-style",
        "      PropertyRefSignature : border-top-color",
        "  LiteralSignature : inherit"
        );
  }

  public void testParser4() {
    assertParseTree(
        ""
        + "[ [ <percentage> | <length> | left | center | right ]"
        + " [ <percentage> | <length> | top | center | bottom ]? ]"
        + " | [ [ left | center | right ] || [ top | center | bottom ] ]"
        + " | inherit",

        "SetSignature",
        "  SeriesSignature",  // covers first two linws
        "    SetSignature",
        "      SymbolSignature : percentage",
        "      SymbolSignature : length",
        "      LiteralSignature : left",
        "      LiteralSignature : center",
        "      LiteralSignature : right",
        "    RepeatedSignature : [0, 1]",  // see '?'
        "      SetSignature",
        "        SymbolSignature : percentage",
        "        SymbolSignature : length",
        "        LiteralSignature : top",
        "        LiteralSignature : center",
        "        LiteralSignature : bottom",
        "  RepeatedSignature : [1, 2]",  // covers 3rd line
        "    ExclusiveSetSignature",
        "      SetSignature",
        "        LiteralSignature : left",
        "        LiteralSignature : center",
        "        LiteralSignature : right",
        "      SetSignature",
        "        LiteralSignature : top",
        "        LiteralSignature : center",
        "        LiteralSignature : bottom",
        "  LiteralSignature : inherit"
        );
  }

  public void testParser5() {
    assertParseTree(
        ""
        + "[ [<uri> ,]*"
        + " [ auto | crosshair | default | pointer | move | e-resize"
        + " | ne-resize | nw-resize | n-resize | se-resize | sw-resize"
        + " | s-resize | w-resize | text | wait | help | progress ] ]"
        + " | inherit",

        "SetSignature",
        "  SeriesSignature",
        "    RepeatedSignature : [0, " + Integer.MAX_VALUE + "]",
        "      SeriesSignature",
        "        SymbolSignature : uri",
        "        LiteralSignature : ,",
        "    SetSignature",
        "      LiteralSignature : auto",
        "      LiteralSignature : crosshair",
        "      LiteralSignature : default",
        "      LiteralSignature : pointer",
        "      LiteralSignature : move",
        "      LiteralSignature : e-resize",
        "      LiteralSignature : ne-resize",
        "      LiteralSignature : nw-resize",
        "      LiteralSignature : n-resize",
        "      LiteralSignature : se-resize",
        "      LiteralSignature : sw-resize",
        "      LiteralSignature : s-resize",
        "      LiteralSignature : w-resize",
        "      LiteralSignature : text",
        "      LiteralSignature : wait",
        "      LiteralSignature : help",
        "      LiteralSignature : progress",
        "  LiteralSignature : inherit"        
        );
  }

  public void testParser6() {
    assertParseTree(
        "rect(<top>, <right>, <bottom>, <left>)",

        "CallSignature",
        "  LiteralSignature : rect",
        "  SeriesSignature",
        "    SymbolSignature : top",
        "    LiteralSignature : ,",
        "    SymbolSignature : right",
        "    LiteralSignature : ,",
        "    SymbolSignature : bottom",
        "    LiteralSignature : ,",
        "    SymbolSignature : left"
        );
  }

  private static CssPropertySignature sig(String sig) {
    return CssPropertySignature.Parser.parseSignature(sig);
  }

  private void assertParseTree(String sig, String... golden) {
    StringBuilder actual = new StringBuilder();
    try {
      sig(sig).formatTree(mc, 0, actual);
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
    MoreAsserts.assertListsEqual(
        Arrays.asList(golden), Arrays.asList(actual.toString().split("\n")));
  }
}
