// Copyright (C) 2010 Google Inc.
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

package com.google.caja.lang.css;

import junit.framework.TestCase;
import static com.google.caja.lang.css.JSRE.alt;
import static com.google.caja.lang.css.JSRE.any;
import static com.google.caja.lang.css.JSRE.cat;
import static com.google.caja.lang.css.JSRE.lit;
import static com.google.caja.lang.css.JSRE.many;
import static com.google.caja.lang.css.JSRE.opt;
import static com.google.caja.lang.css.JSRE.raw;
import static com.google.caja.lang.css.JSRE.rep;

public class JSRETest extends TestCase {
  public final void testOptimize() {
    assertOptimized("foo", lit("foo"));
    assertOptimized("foo", raw("foo"));
    assertEquals("(?:fo)(?:o)*", "" + cat(lit("fo"), any(lit("o"))));
    assertOptimized("fo+", cat(lit("fo"), any(lit("o"))));
    assertOptimized("bar\\\\baz", lit("bar\\baz"));
    assertOptimized("bar\\baz", raw("bar\\baz"));
    assertOptimized("foo|bar", alt(lit("foo"), lit("bar")));
    assertOptimized("b(?:a|ee)r", alt(lit("bar"), lit("beer")));
    assertOptimized("be[ae]r", alt(lit("bear"), lit("beer")));
    assertOptimized(
        "a[+\\-\\n]b",
        alt(lit("a+b"), lit("a-b"), cat(raw("a"), raw("\\n"), raw("b"))));
    assertOptimized("barn?", alt(lit("bar"), lit("barn")));
    assertOptimized("b?ear", alt(lit("bear"), lit("ear")));
    assertOptimized("", alt(cat(), lit(""), raw("")));
    assertOptimized("(?:foo)?", opt(lit("foo")));
    assertOptimized("(?:foo)+", many(lit("foo")));
    assertOptimized("(?:foo)*", any(lit("foo")));
    JSRE x = lit("x");
    assertOptimized("x?", opt(opt(x)));
    assertOptimized("x*", many(opt(x)));
    assertOptimized("x*", any(opt(x)));
    assertOptimized("x*", rep(opt(x), 2, Integer.MAX_VALUE));
    assertOptimized("x{0,4}", rep(opt(x), 2, 4));
    assertOptimized("x*", opt(any(x)));
    assertOptimized("x*", any(any(x)));
    assertOptimized("x*", many(any(x)));
    assertOptimized("x*", rep(any(x), 2, 3));
    assertOptimized("x*", opt(many(x)));
    assertOptimized("x*", any(many(x)));
    assertOptimized("x+", many(many(x)));
    assertOptimized("x{2,}", rep(many(x), 2, 4));
    assertOptimized("(?:x{2,4})*", any(rep(x, 2, 4)));
    assertOptimized("(?:x{2,4})+", many(rep(x, 2, 4)));
    assertOptimized("(?:x{2,4})?", opt(rep(x, 2, 4)));
    assertOptimized("(?:x{2,4}){1,3}", rep(rep(x, 2, 4), 1, 3));
    assertOptimized("x{2,}", rep(x, 2, Integer.MAX_VALUE));
    assertOptimized("x{0,4}", rep(x, 0, 4));
    assertEquals("(?:foo)(?:foo)*", "" + cat(lit("foo"), any(lit("foo"))));
    assertOptimized("(?:foo)+", cat(lit("foo"), any(lit("foo"))));
    assertOptimized("(?:foo)+", cat(any(lit("foo")), lit("foo")));
    assertOptimized("foo|ba[rz]", alt(lit("foo"), alt(lit("bar"), lit("baz"))));
    assertOptimized("foo|bar|ak", alt(lit("foo"), alt(lit("bar"), lit("ak"))));
    assertOptimized("[xy]", alt(x, x, x, lit("y"), x));
    JSRE noop = lit("");
    assertOptimized("[xy]?", alt(x, noop, x, lit("y"), x, noop));
    assertOptimized("a(?:bac){1,2}", alt(lit("abacbac"), lit("abac")));
    assertOptimized("a(?:bac){1,2}", alt(lit("abac"), lit("abacbac")));
    assertOptimized("ab", cat(lit("a"), noop, lit("b")));
    assertOptimized("foo", rep(lit("foo"), 1, 1));
    assertOptimized("", opt(noop));
    assertOptimized("", many(noop));
    assertOptimized("x*(?:foo)?", cat(any(x), opt(cat(any(x), lit("foo")))));
    assertOptimized("[ab]", alt(lit("a"), lit("b")));
    assertOptimized("[ab]", alt(lit("b"), lit("a")));
    assertOptimized("[a-c]", alt(lit("a"), lit("b"), lit("c")));
    assertOptimized(
        "[BCa-c]", alt(lit("b"), lit("C"), lit("a"), lit("B"), lit("c")));
    assertOptimized(
        "[ACa-c]", alt(lit("b"), lit("A"), lit("a"), lit("C"), lit("c")));
  }

  private static void assertOptimized(String regex, JSRE p) {
    assertEquals(p, p);
    JSRE optimized = p.optimize();
    assertEquals(optimized, optimized);
    assertEquals(regex, "" + optimized);
  }
}
