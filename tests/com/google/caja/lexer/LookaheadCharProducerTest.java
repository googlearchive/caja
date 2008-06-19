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

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;

import junit.framework.TestCase;

/**
 *
 * @author mikesamuel@gmail.com
 */
public class LookaheadCharProducerTest extends TestCase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  public void testReadAndLookahead() throws Exception {
    LookaheadCharProducer cp = new LookaheadCharProducer(
        CharProducer.Factory.create(new StringReader("abcdefgh"),
                                    new InputSource(URI.create("test:///"))),
        1);
    assertEquals('a', cp.read());
    assertEquals('b', cp.lookahead());
    assertEquals('b', cp.read());
    assertEquals('c', cp.lookahead());
    assertEquals('c', cp.lookahead());
    assertEquals('c', cp.read());
    assertEquals('d', cp.read());
    assertEquals('e', cp.read());
    assertEquals('f', cp.lookahead());
    assertEquals('f', cp.read());
    assertEquals('g', cp.lookahead());
    assertEquals('g', cp.read());
    assertEquals('h', cp.lookahead());
    assertEquals('h', cp.read());
    assertEquals(-1, cp.lookahead());
    assertEquals(-1, cp.read());
  }

  public void testCurrentFilePosition() throws Exception {
    LookaheadCharProducer cp = new LookaheadCharProducer(
        CharProducer.Factory.create(new StringReader("abcdefgh"),
                                    new InputSource(URI.create("test:///"))),
        1);
    CharProducer.MutableFilePosition buf =
      new CharProducer.MutableFilePosition();
    cp.getCurrentPosition(buf);
    assertEquals(1, buf.charInLine);
    assertEquals('a', cp.read());
    cp.getCurrentPosition(buf);
    assertEquals(2, buf.charInLine);
    assertEquals('b', cp.lookahead());
    cp.getCurrentPosition(buf);
    assertEquals(2, buf.charInLine);
    assertEquals('b', cp.read());
    cp.getCurrentPosition(buf);
    assertEquals(3, buf.charInLine);
    assertEquals('c', cp.lookahead());
    cp.getCurrentPosition(buf);
    assertEquals(3, buf.charInLine);
    assertEquals('c', cp.lookahead());
    cp.getCurrentPosition(buf);
    assertEquals(3, buf.charInLine);
    assertEquals('c', cp.read());
    cp.getCurrentPosition(buf);
    assertEquals(4, buf.charInLine);
    assertEquals('d', cp.read());
    cp.getCurrentPosition(buf);
    assertEquals(5, buf.charInLine);
    assertEquals('e', cp.read());
    cp.getCurrentPosition(buf);
    assertEquals(6, buf.charInLine);
    assertEquals('f', cp.lookahead());
    cp.getCurrentPosition(buf);
    assertEquals(6, buf.charInLine);
    assertEquals('f', cp.read());
    cp.getCurrentPosition(buf);
    assertEquals(7, buf.charInLine);
    assertEquals('g', cp.lookahead());
    cp.getCurrentPosition(buf);
    assertEquals(7, buf.charInLine);
    assertEquals('g', cp.read());
    cp.getCurrentPosition(buf);
    assertEquals(8, buf.charInLine);
    assertEquals('h', cp.lookahead());
    cp.getCurrentPosition(buf);
    assertEquals(8, buf.charInLine);
    assertEquals('h', cp.read());
    cp.getCurrentPosition(buf);
    assertEquals(9, buf.charInLine);
    assertEquals(-1, cp.lookahead());
    cp.getCurrentPosition(buf);
    assertEquals(9, buf.charInLine);
    assertEquals(-1, cp.read());
    cp.getCurrentPosition(buf);
    assertEquals(9, buf.charInLine);
  }

  public void testClose() throws Exception {
    LookaheadCharProducer cp = new LookaheadCharProducer(
        CharProducer.Factory.create(new StringReader("abcdefgh"),
                                    new InputSource(URI.create("test:///"))),
        1);
    assertEquals('a', cp.read());
    assertEquals('b', cp.lookahead());
    assertEquals('b', cp.read());
    assertEquals('c', cp.lookahead());

    cp.close();
    try {
      cp.lookahead();
      fail("lookahead masked closed reader");
    } catch (IOException ex) {
      // pass
    }
  }
}
