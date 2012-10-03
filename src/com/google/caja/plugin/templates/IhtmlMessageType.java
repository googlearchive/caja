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

package com.google.caja.plugin.templates;

import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageType;
import com.google.caja.reporting.MessageTypeInt;

import java.io.IOException;

public enum IhtmlMessageType implements MessageTypeInt {
  BAD_ATTRIB(
      "%s: Element %s has bad attribute %s with value `%s`",
      MessageLevel.ERROR),
  BAD_ELEMENT("%s: Unrecognized element %s", MessageLevel.ERROR),
  DUPLICATE_IDENTIFIER(
      "%s: Identifier %s already declared at %s", MessageLevel.FATAL_ERROR),
  DUPLICATE_MESSAGE("%s: Message %s masks one at %s", MessageLevel.ERROR),
  DUPLICATE_PLACEHOLDER(
      "%s: Placeholder %s masks one at %s", MessageLevel.ERROR),
  IGNORED_TAG("%s: removing ignorable element %s", MessageLevel.WARNING),
  IHTML_IN_MESSAGE_OUTSIDE_PLACEHOLDER(
      "%s: %s in message outside placeholder", MessageLevel.ERROR),
  ILLEGAL_NAME("%s: illegal name %s", MessageLevel.ERROR),
  INAPPROPRIATE_CONTENT(
      "%s: cannot have content inside %s", MessageLevel.ERROR),
  MALFORMED_MESSAGE("%s: Message %s is malformed", MessageLevel.ERROR),
  MALFORMED_URI("%s: URI %s is malformed", MessageLevel.ERROR),
  MISPLACED_ELEMENT("%s: Element %s not allowed inside %s", MessageLevel.ERROR),
  MISSING_ATTRIB(
      "%s: Element %s is missing required attribute %s", MessageLevel.ERROR),
  MISSING_PLACEHOLDER("%s: No such message with name %s", MessageLevel.ERROR),
  NESTED_MESSAGE("%s: Message nested inside another at %s", MessageLevel.ERROR),
  ORPHANED_PLACEHOLDER(
      "%s: Placeholder not inside a message", MessageLevel.ERROR),
  ORPHANED_PLACEHOLDER_END(
      "%s: Placeholder end does not match an open", MessageLevel.ERROR),
  UNCLOSED_PLACEHOLDER("%s: Placeholder not closed", MessageLevel.ERROR),
  UNSAFE_ROOT_TAG("%s: Cannot remove element at root: %s", MessageLevel.ERROR),
  UNSAFE_UNVIRT_TAG("%s: Disallowed element: %s", MessageLevel.ERROR),
  UNTRANSLATED_MESSAGE(
      "%s: No such message with name %s in locale %s", MessageLevel.WARNING),
  ;

  private final String formatString;
  private final MessageLevel level;
  private final int paramCount;

  IhtmlMessageType(String formatString, MessageLevel level) {
    this.formatString = formatString;
    this.level = level;
    this.paramCount = MessageType.formatStringArity(formatString);
  }

  public int getParamCount() {
    return paramCount;
  }

  public void format(MessagePart[] parts, MessageContext context,
                     Appendable out) throws IOException {
    MessageType.formatMessage(formatString, parts, context, out);
  }

  public MessageLevel getLevel() { return level; }
}
