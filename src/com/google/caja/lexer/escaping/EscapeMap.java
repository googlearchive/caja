// Copyright (C) 2007 Google Inc.
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

package com.google.caja.lexer.escaping;

import java.util.Arrays;

/**
 * Maps ascii codepoints (lower 7f) to the escaped form.  This is a lookup
 * table that performs efficiently for latin strings.
 *
 * @author mikesamuel@gmail.com (Mike Samuel)
 */
final class EscapeMap {
  private final int min;
  private final String[] escapes;

  EscapeMap(Escape... asciiEscapes) {
    this(null, asciiEscapes);
  }

  EscapeMap(EscapeMap base, Escape... asciiEscapes) {
    if (asciiEscapes.length == 0) {
      min = 0;
      escapes = new String[0];
    } else {
      Arrays.sort(asciiEscapes);
      int max;
      if (base == null) {
        this.min = asciiEscapes[0].raw;
        max = asciiEscapes[asciiEscapes.length - 1].raw;
      } else {
        this.min = Math.min(base.min, asciiEscapes[0].raw);
        max = Math.max(asciiEscapes[asciiEscapes.length - 1].raw,
                       base.min + base.escapes.length - 1);
      }
      this.escapes = new String[max - min + 1];
      if (base != null) {
        System.arraycopy(base.escapes, 0, this.escapes, base.min - this.min,
                         base.escapes.length);
      }
      for (Escape esc : asciiEscapes) {
        int idx = esc.raw - min;
        if (escapes[idx] == null) { escapes[idx] = esc.escaped; }
      }
    }
  }

  String getEscape(int codepoint) {
    int offset = codepoint - min;
    if (offset < 0 || offset >= escapes.length) {
      return null;
    }
    return escapes[offset];
  }

  EscapeMap plus(Escape... asciiEscapes) {
    return new EscapeMap(this, asciiEscapes);
  }
}
