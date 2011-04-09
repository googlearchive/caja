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

import com.google.caja.SomethingWidgyHappenedError;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import java.io.IOException;
import java.io.Serializable;

/**
 * A Message that can be displayed to the user.
 *
 * <p>Messages can be formatted for immediate display to the user, or can be
 * fed to another tool, such as an documentation or metadata generator, that
 * might use then produce derived files.
 *
 * @author mikesamuel@gmail.com
 */
public final class Message implements Serializable {
  private static final long serialVersionUID = -8562715364342936493L;
  private MessageTypeInt type;
  private MessageLevel level;
  private MessagePart[] parts;

  public Message(MessageTypeInt type, MessagePart... parts) {
    this(type, type.getLevel(), parts);
  }

  public Message(
      MessageTypeInt type, MessageLevel level, MessagePart... parts) {
    this.type = type;
    this.level = level;
    this.parts = new MessagePart[parts.length];
    if (null == type) { throw new NullPointerException(); }
    if (null == level) { throw new NullPointerException(); }
    if (parts.length != type.getParamCount()) {
      throw new IllegalArgumentException(
          parts.length + " != " + type.getParamCount());
    }
    for (int i = parts.length; --i >= 0;) {
      MessagePart p = parts[i];
      if (null == p) { throw new NullPointerException(String.valueOf(i)); }
      this.parts[i] = p;
    }
  }

  public MessageTypeInt getMessageType() { return type; }

  public MessageLevel getMessageLevel() { return level; }

  public List<MessagePart> getMessageParts() {
    return Collections.unmodifiableList(Arrays.asList(parts));
  }

  @Override
  public String toString() { return format(new MessageContext()); }

  public String format(MessageContext context) {
    try {
      StringBuilder sb = new StringBuilder();
      this.format(context, sb);
      return sb.toString();
    } catch (IOException ex) {
      throw new SomethingWidgyHappenedError(
          "IOException writing to StringBuilder", ex);
    }
  }

  public void format(MessageContext context, Appendable out)
      throws IOException {
    type.format(parts, context, out);
  }
}
