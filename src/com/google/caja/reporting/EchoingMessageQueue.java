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

package com.google.caja.reporting;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;

/**
 * A message queue that immediately writes messages to a PrintWriter.
 *
 * @author mikesamuel@gmail.com
 */
public class EchoingMessageQueue extends AbstractMessageQueue {
  private final List<Message> messages;
  private final MessageContext mc;
  private boolean dumpStack;

  public EchoingMessageQueue(PrintWriter out, MessageContext context) {
    this(out, context, true);
  }

  public EchoingMessageQueue(
      final PrintWriter out, MessageContext context, boolean dumpStack) {
    this.mc = context;
    this.messages = new AbstractList<Message>() {
      List<Message> backing = new ArrayList<Message>();

      @Override
      public void add(int index, Message element) {
        backing.add(index, element);
        try {
          out.append(element.getMessageLevel().name()).append(':');
          element.format(mc, out);
          out.append('\n');
          if (EchoingMessageQueue.this.dumpStack && element.getMessageLevel()
              .compareTo(MessageLevel.LINT) >= 0) {
            new Exception().printStackTrace(out);
          }
          out.flush();
        } catch (IOException ex) {
          ex.printStackTrace();
        }
      }

      @Override
      public Message get(int index) {
        return backing.get(index);
      }

      @Override
      public Message remove(int index) {
        return backing.remove(index);
      }

      @Override
      public int size() {
        return backing.size();
      }

    };
    this.dumpStack = dumpStack;
  }

  public List<Message> getMessages() {
    return messages;
  }

  public MessageContext getMessageContext() { return mc; }

  public void setDumpStack(boolean dumpStack) { this.dumpStack = dumpStack; }
}
