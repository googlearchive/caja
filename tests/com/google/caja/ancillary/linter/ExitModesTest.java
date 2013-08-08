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

package com.google.caja.ancillary.linter;

import com.google.caja.lexer.FilePosition;
import com.google.caja.parser.js.BreakStmt;
import com.google.caja.parser.js.Declaration;
import com.google.caja.parser.js.Identifier;

import junit.framework.TestCase;

@SuppressWarnings("static-method")
public class ExitModesTest extends TestCase {
  public final void testUnion() {
    assertEquals(
        ExitModes.COMPLETES, ExitModes.COMPLETES.union(ExitModes.COMPLETES));

    LiveSet x = LiveSet.EMPTY.with(decl("x"));
    LiveSet y = LiveSet.EMPTY.with(decl("y"));

    ExitModes foo = ExitModes.COMPLETES.withBreak(b("foo"), x);
    ExitModes bar = ExitModes.COMPLETES.withBreak(b("bar"), y);
    ExitModes foobar = foo.withBreak(b("bar"), y);

    assertEquals("{bfoo=((x@-1) always)}", foo.toString());
    assertEquals("{bbar=((y@-1) always)}", bar.toString());
    assertEquals("{bfoo=((x@-1) always), bbar=((y@-1) always)}",
                 foobar.toString());

    assertEquals(foo, ExitModes.COMPLETES.union(foo));
    assertEquals(foo, foo.union(ExitModes.COMPLETES));
    assertEquals(bar, ExitModes.COMPLETES.union(bar));
    assertEquals(bar, bar.union(ExitModes.COMPLETES));
    assertEquals(foobar, bar.union(foo));
    assertEquals(foobar, foo.union(bar));
  }

  public final void testIntersection() {
    assertEquals(
        ExitModes.COMPLETES,
        ExitModes.COMPLETES.intersection(ExitModes.COMPLETES));

    LiveSet x = LiveSet.EMPTY.with(decl("x"));
    LiveSet y = LiveSet.EMPTY.with(decl("y"));
    LiveSet xy = x.with(decl("y"));

    ExitModes foo = ExitModes.COMPLETES.withBreak(b("foo"), x);
    ExitModes bar = ExitModes.COMPLETES.withBreak(b("bar"), y);
    ExitModes baz = ExitModes.COMPLETES.withBreak(b("baz"), xy);
    ExitModes foobar = foo.withBreak(b("bar"), y);
    ExitModes foobaz = foo.withBreak(b("baz"), xy);
    ExitModes barbaz = bar.withBreak(b("baz"), xy);
    ExitModes foobarbaz = foobar.withBreak(b("baz"), xy);

    ExitModes fooS = ExitModes.COMPLETES.intersection(foo);
    ExitModes barS = ExitModes.COMPLETES.intersection(bar);
    ExitModes bazS = ExitModes.COMPLETES.intersection(baz);
    ExitModes fooSbarS = ExitModes.COMPLETES.intersection(foobar);
    ExitModes fooSbarSbaz = fooSbarS.union(baz);
    ExitModes fooSbarSbazS = fooSbarS.intersection(bazS);

    assertEquals("{bfoo=((x@-1) always)}", foo.toString());
    assertEquals("{bbar=((y@-1) always)}", bar.toString());
    assertEquals("{bbaz=((x@-1 y@-1) always)}", baz.toString());
    assertEquals("{bfoo=((x@-1))}", fooS.toString());
    assertEquals("{bbar=((y@-1))}", barS.toString());
    assertEquals("{bbaz=((x@-1 y@-1))}", bazS.toString());
    assertEquals("{bfoo=((x@-1)), bbar=((y@-1))}",
                 fooSbarS.toString());
    assertEquals("{bbaz=((x@-1 y@-1) always), bfoo=((x@-1)), bbar=((y@-1))}",
                 fooSbarSbaz.toString());

    assertFalse(foo.equals(fooS));
    assertFalse(bar.equals(barS));
    assertFalse(foobar.equals(fooSbarS));

    assertEquals(foo, foo.intersection(foo));
    assertEquals(fooS, ExitModes.COMPLETES.intersection(foo));
    assertEquals(fooS, foo.intersection(ExitModes.COMPLETES));
    assertEquals(bar, bar.intersection(bar));
    assertEquals(barS, ExitModes.COMPLETES.intersection(bar));
    assertEquals(barS, bar.intersection(ExitModes.COMPLETES));
    // Since neither foo nor bar complete, the always bits don't get stripped.
    assertEquals(foobar, bar.intersection(foo));
    assertEquals(foobar, foo.intersection(bar));
    assertEquals(foobarbaz, foobaz.intersection(barbaz));
    assertEquals(foobarbaz, foobaz.intersection(barbaz));
    assertEquals(foobarbaz, barbaz.intersection(foobaz));
    // But since fooSBarS does complete, the always bits get stripped from baz
    assertEquals(fooSbarSbazS, fooSbarS.intersection(barbaz));
    assertEquals(fooSbarSbazS, barbaz.intersection(fooSbarS));
  }

  private static BreakStmt b(String label) {
    return new BreakStmt(FilePosition.UNKNOWN, label);
  }
  private static Declaration decl(String name) {
    return new Declaration(
        FilePosition.UNKNOWN, new Identifier(FilePosition.UNKNOWN, name), null);
  }
}
