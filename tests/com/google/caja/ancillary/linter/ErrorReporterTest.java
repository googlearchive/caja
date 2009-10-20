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
import com.google.caja.lexer.InputSource;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageTypeInt;
import com.google.caja.util.MoreAsserts;

import java.io.IOException;
import java.net.URI;

import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

public class ErrorReporterTest extends TestCase {
  enum TestMessageType implements MessageTypeInt {
    MT,
    ;

    public int getParamCount() { return 1; }
    public MessageLevel getLevel() { return MessageLevel.ERROR; }
    public void format(MessagePart[] parts, MessageContext mc, Appendable out)
        throws IOException {
      for (int i = 0, n = parts.length; i < n; ++i) {
        if (i != 0) { out.append(", "); }
        parts[i].format(mc, out);
      }
    }
  }

  public final void testMessageSorting() {
    InputSource a = new InputSource(URI.create("test:///a"));
    InputSource b = new InputSource(URI.create("test:///b"));
    InputSource c = new InputSource(URI.create("test:///c"));
    FilePosition a1 = FilePosition.instance(a, 1, 10, 10);
    FilePosition a2 = FilePosition.instance(a, 2, 20, 10);
    FilePosition a12 = FilePosition.span(a1, a2);
    FilePosition b2 = FilePosition.instance(b, 2, 20, 10);
    FilePosition c1 = FilePosition.instance(c, 1, 10, 10);
    FilePosition c2 = FilePosition.instance(c, 2, 20, 10);
    FilePosition c12 = FilePosition.span(c1, c2);

    Message ma12 = new Message(TestMessageType.MT, a12);
    Message mc2 = new Message(TestMessageType.MT, c2);
    Message mc12 = new Message(TestMessageType.MT, c12);
    Message mb2 = new Message(TestMessageType.MT, b2);
    Message ma = new Message(TestMessageType.MT, a);
    Message mb = new Message(TestMessageType.MT, b);
    Message mc1 = new Message(TestMessageType.MT, c1);
    Message ma1 = new Message(TestMessageType.MT, a1);
    Message ma2 = new Message(TestMessageType.MT, a2);
    Message mhi = new Message(
        TestMessageType.MT, MessagePart.Factory.valueOf("hi"));
    Message mhithere = new Message(
        TestMessageType.MT, MessagePart.Factory.valueOf("hi there"));

    List<Message> messages = Arrays.asList(
        ma12, mhithere, mc2, mc12, mb2, ma, mb, mc1, ma1, ma2, mhi);

    MoreAsserts.assertListsEqual(
        Arrays.asList(
            ma, ma1, ma12, ma2, mb, mb2, mc1, mc12, mc2, mhi, mhithere),
        ErrorReporter.sortMessages(messages));
  }
}
