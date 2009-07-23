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

package com.google.caja.reporting;

import junit.framework.TestCase;

import java.util.List;
import java.util.ArrayList;
import java.io.IOException;

/**
 * @author ihab.awad@gmail.com
 */
public class AbstractMessageQueueTest extends TestCase {

  /**
   * A simple message type to use for testing.
   */
  private static class TestMessageType implements MessageTypeInt {
    private final MessageLevel level;
    private final String name;

    public TestMessageType(MessageLevel level, String name) {
      this.level = level;
      this.name = name;
    }

    public void format(MessagePart[] parts,
                       MessageContext context,
                       Appendable out)
        throws IOException {
      out.append(name);
      out.append(" ");
      parts[0].format(context, out);
    }

    public MessageLevel getLevel() { return level; }

    public int getParamCount() { return 1; }

    public String name() { return name; }
  }

  private static final MessageTypeInt TEST_ERROR =
      new TestMessageType(MessageLevel.ERROR, "test error");

  private static final MessageTypeInt TEST_WARNING =
      new TestMessageType(MessageLevel.WARNING, "test warning");

  private MessageContext mc;
  private MessageQueue mq;

  @Override
  public void setUp() {
    mc = new MessageContext();
    mq = new AbstractMessageQueue() {
      private List<Message> messages = new ArrayList<Message>();
      public List<Message> getMessages() {
        return messages;
      }
    };
  }

  @Override
  public void tearDown() {
    mc = null;
    mq = null;
  }

  public final void testAddMessageSimple() {
    assertEquals(0, mq.getMessages().size());
    mq.addMessage(TEST_WARNING, MessagePart.Factory.valueOf("test"));
    assertEquals(1, mq.getMessages().size());
    assertEquals(
        MessageLevel.WARNING,
        mq.getMessages().get(0).getMessageLevel());
    assertEquals(
        "test warning test",
        mq.getMessages().get(0).format(mc));
  }

  public final void testAddMessageWithLevel() {
    assertEquals(0, mq.getMessages().size());
    mq.addMessage(
        TEST_WARNING,
        MessageLevel.ERROR,
        MessagePart.Factory.valueOf("test"));
    assertEquals(1, mq.getMessages().size());
    assertEquals(
        MessageLevel.ERROR,
        mq.getMessages().get(0).getMessageLevel());
    assertEquals(
        "test warning test",
        mq.getMessages().get(0).format(mc));
  }

  public final void testHasMessageAtLevel() {
    assertEquals(0, mq.getMessages().size());
    mq.addMessage(TEST_WARNING, MessagePart.Factory.valueOf("test"));
    assertEquals(1, mq.getMessages().size());
    assertFalse(mq.hasMessageAtLevel(MessageLevel.ERROR));
    mq.addMessage(TEST_ERROR, MessagePart.Factory.valueOf("test"));
    assertEquals(2, mq.getMessages().size());
    assertTrue(mq.hasMessageAtLevel(MessageLevel.ERROR));
  }
}