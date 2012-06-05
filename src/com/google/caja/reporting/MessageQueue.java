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

import java.util.List;

/**
 * A group of {@link Message message}s.
 *
 * @author mikesamuel@gmail.com
 */
public interface MessageQueue {

  /**
   * Returns a list of the messages published thus far.
   */
  List<Message> getMessages();

  /** Adds a message to this group's message list. */
  void addMessage(Message msg);

  /** Adds a message to this group's message list. */
  void addMessage(MessageTypeInt type, MessagePart... parts);

  /** Adds a message to this group's message list. */
  void addMessage(MessageTypeInt type, MessageLevel lvl, MessagePart... parts);

  /**
   * Queries whether this group contains a message of at least the specified
   * message level.
   */
  boolean hasMessageAtLevel(MessageLevel lvl);
}
