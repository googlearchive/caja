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

import java.util.AbstractList;
import java.util.Collection;
import java.util.List;

/**
 * A MessageQueue that ignores all messages added to it.
 *
 * @author mikesamuel@gmail.com
 */
public final class DevNullMessageQueue implements MessageQueue {

  private List<Message> list = new AbstractList<Message>() {

    @Override
    public void add(int index, Message element) {
      // does not add
    }

    @Override
    public boolean addAll(int index, Collection<? extends Message> c) {
      // does not add
      return false;
    }

    @Override
    public void clear() {
      // already cleared
    }

    @Override
    public Message get(int index) {
      throw new IndexOutOfBoundsException(String.valueOf(index));
    }

    @Override
    public Message remove(int index) {
      throw new IndexOutOfBoundsException(String.valueOf(index));
    }

    @Override
    protected void removeRange(int fromIndex, int toIndex) {
      throw new IndexOutOfBoundsException(String.valueOf(fromIndex));
    }

    @Override
    public Message set(int index, Message element) {
      throw new IndexOutOfBoundsException(String.valueOf(index));
    }

    @Override
    public boolean addAll(Collection<? extends Message> c) {
      return false;
    }

    @Override
    public boolean remove(Object o) {
      return false;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
      return false;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
      return false;
    }

    @Override
    public int size() {
      return 0;
    }
  };

  private DevNullMessageQueue() {
    // singleton
  }

  public static MessageQueue singleton() {
    return Singleton.singleton;
  }

  public List<Message> getMessages() {
    return list;
  }

  public void addMessage(MessageTypeInt type, MessagePart... parts) {
    // do nothing
  }

  public void addMessage(
      MessageTypeInt type, MessageLevel level, MessagePart... parts) {
    // do nothing
  }

  public void addMessage(Message msg) {
    // do nothing
  }

  public boolean hasMessageAtLevel(MessageLevel lvl) {
    return false;
  }

  private static class Singleton {
    static MessageQueue singleton = new DevNullMessageQueue();
  }

}
