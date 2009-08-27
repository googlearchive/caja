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

package com.google.caja.util;

/**
 * @author mikesamuel@gmail.com
 */
public final class Pair<A, B> {
  public final A a;
  public final B b;

  public Pair(A a, B b) {
    this.a = a;
    this.b = b;
  }

  public Pair(Pair<A, B> p) {
    this(p.a, p.b);
  }

  public static <S, T> Pair<S, T> pair(S a, T b) {
    return new Pair<S, T>(a, b);
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Pair<?, ?>)) { return false; }
    Pair<?, ?> that = (Pair<?, ?>) o;
    return (this.a == null ? that.a == null : this.a.equals(that.a))
      && (this.b == null ? that.b == null : this.b.equals(that.b));
  }

  @Override
  public int hashCode() {
    int hca = this.a != null ? this.a.hashCode() : 0;
    int hcb = this.b != null ? this.b.hashCode() : 0;
    return hca ^ ((hcb >>> 16) | (hcb << 16));
  }

  @Override
  public String toString() {
    return "[" + this.a + ", " + this.b + "]";
  }

}
