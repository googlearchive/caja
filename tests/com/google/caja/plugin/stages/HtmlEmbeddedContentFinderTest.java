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

package com.google.caja.plugin.stages;

import com.google.caja.lang.html.HtmlSchema;
import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.FilePosition;
import com.google.caja.plugin.UriFetcher;
import com.google.caja.util.CajaTestCase;

import java.util.Iterator;

public final class HtmlEmbeddedContentFinderTest extends CajaTestCase {

  public final void testFilePositionOfAttrib() throws Exception {
    //                          1
    //                 12345678901234567890
    String htmlText = "<hr onclick='foo()'>";
    HtmlEmbeddedContentFinder f = new HtmlEmbeddedContentFinder(
        HtmlSchema.getDefault(mq), is.getUri(), mq, mc);
    Iterator<EmbeddedContent> iterator = f.findEmbeddedContent(
        htmlFragment(fromString(htmlText))).iterator();
    assertTrue(iterator.hasNext());
    EmbeddedContent content = iterator.next();
    CharProducer cp = content.getContent(UriFetcher.NULL_NETWORK);
    FilePosition pos = content.getPosition();
    assertEquals(" foo() ", cp.toString());
    assertEquals("testFilePositionOfAttrib:1+13@13 - 20@20", pos.toString());
    assertFalse(iterator.hasNext());
  }

  public final void testFilePositionOfJsUrl() throws Exception {
    //                          1         2         3
    //                 12345678901234567890123456789012345
    String htmlText = "<a href='javascript:foo()'>Foo</a>";
    HtmlEmbeddedContentFinder f = new HtmlEmbeddedContentFinder(
        HtmlSchema.getDefault(mq), is.getUri(), mq, mc);
    Iterator<EmbeddedContent> iterator = f.findEmbeddedContent(
        htmlFragment(fromString(htmlText))).iterator();
    assertTrue(iterator.hasNext());
    EmbeddedContent content = iterator.next();
    CharProducer cp = content.getContent(UriFetcher.NULL_NETWORK);
    FilePosition pos = content.getPosition();
    //            'javascript:foo()'
    assertEquals("            foo() ", cp.toString());
    assertEquals("testFilePositionOfJsUrl:1+9@9 - 27@27", pos.toString());
    assertFalse(iterator.hasNext());
  }
}
