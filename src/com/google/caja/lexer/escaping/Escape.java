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

/**
 * Encapsulates an ascii character and it's escaped form.
 *
 * @author mikesamuel@gmail.com (Mike Samuel)
 */
final class Escape implements Comparable<Escape> {
  final byte raw;
  final String escaped;

  Escape(char ch, String escaped) {
    if ((ch & ~0x7f) != 0) { throw new IllegalArgumentException(); }
    this.raw = (byte) ch;
    this.escaped = escaped;
  }

  public int compareTo(Escape other) {
    return this.raw - other.raw;
  }

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof Escape)) {
      return false;
    }
    return this.raw == ((Escape)other).raw;
  }

  @Override
  public int hashCode () {
    return raw;
  }
}
