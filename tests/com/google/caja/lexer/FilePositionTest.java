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

package com.google.caja.lexer;

import java.net.URI;

import junit.framework.TestCase;

@SuppressWarnings("static-method")
public class FilePositionTest extends TestCase {
  public final void testInstance1() {
    InputSource is = new InputSource(URI.create("t:///foo"));
    FilePosition inst = FilePosition.instance(is, 1, 1, 1);
    assertEquals(is, inst.source());
    assertEquals(1, inst.startLineNo());
    assertEquals(1, inst.startCharInFile());
    assertEquals(1, inst.startCharInLine());
    assertEquals(1, inst.endLineNo());
    assertEquals(1, inst.endCharInFile());
    assertEquals(1, inst.endCharInLine());
    assertEquals(0, inst.length());
    assertEquals("foo:1+1@1", inst.toString());
  }

  public final void testInstance2() {
    InputSource is = new InputSource(URI.create("t:///foo"));
    FilePosition inst = FilePosition.instance(is, 1, 1, 1, 1);
    assertEquals(is, inst.source());
    assertEquals(1, inst.startLineNo());
    assertEquals(1, inst.startCharInFile());
    assertEquals(1, inst.startCharInLine());
    assertEquals(1, inst.endLineNo());
    assertEquals(2, inst.endCharInFile());
    assertEquals(2, inst.endCharInLine());
    assertEquals(1, inst.length());
    assertEquals("foo:1+1@1 - 2@2", inst.toString());
  }

  public final void testInstance3() {
    InputSource is = new InputSource(URI.create("t:///foo"));
    FilePosition inst = FilePosition.instance(is, 2, 30, 2, 3);
    assertEquals(is, inst.source());
    assertEquals(2, inst.startLineNo());
    assertEquals(30, inst.startCharInFile());
    assertEquals(2, inst.startCharInLine());
    assertEquals(2, inst.endLineNo());
    assertEquals(33, inst.endCharInFile());
    assertEquals(5, inst.endCharInLine());
    assertEquals(3, inst.length());
    assertEquals("foo:2+2@30 - 5@33", inst.toString());
  }

  public final void testInstance4() {
    InputSource is = new InputSource(URI.create("t:///foo"));
    FilePosition inst = FilePosition.instance(is, 2, 30, 1);
    assertEquals(is, inst.source());
    assertEquals(2, inst.startLineNo());
    assertEquals(30, inst.startCharInFile());
    assertEquals(1, inst.startCharInLine());
    assertEquals(2, inst.endLineNo());
    assertEquals(30, inst.endCharInFile());
    assertEquals(1, inst.endCharInLine());
    assertEquals(0, inst.length());
    assertEquals("foo:2+1@30", inst.toString());
  }

  public final void testFilePositionFor() {
    InputSource is = new InputSource(URI.create("t:///bar"));
    FilePosition inst = FilePosition.fromLinePositions(is, 1, 1, 10, 100);
    assertEquals(is, inst.source());
    assertEquals(1, inst.startLineNo());
    assertEquals(1, inst.startCharInLine());
    assertEquals(10, inst.endLineNo());
    assertEquals(100, inst.endCharInLine());
  }
}
