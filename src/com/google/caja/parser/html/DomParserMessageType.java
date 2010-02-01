// Copyright (C) 2007 Google Inc.
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

package com.google.caja.parser.html;

import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageType;
import com.google.caja.reporting.MessageTypeInt;

import java.io.IOException;

/**
 * Messages for the Dom Parser
 *
 * @author mikesamuel@gmail.com
 */
public enum DomParserMessageType implements MessageTypeInt {
  UNMATCHED_END("%s: end tag %s does not match open tag %s",
                MessageLevel.FATAL_ERROR),
  MISPLACED_CONTENT("%s: markup outside document", MessageLevel.ERROR),
  UNCLOSED_TAG("%s: tag is not closed", MessageLevel.ERROR),
  MISSING_END("%s: element %s at %s is not closed", MessageLevel.WARNING),
  IGNORING_TOKEN("%s: ignoring token %s", MessageLevel.WARNING),
  MOVING_TO_HEAD("%s: moving element %s to head", MessageLevel.LINT),
  MISSING_DOCUMENT_ELEMENT("%s: no document element", MessageLevel.ERROR),
  GENERIC_SAX_ERROR("%s: %s", MessageLevel.FATAL_ERROR),
  ;

  private final String formatString;
  private final MessageLevel level;
  private final int paramCount;

  DomParserMessageType(String formatString, MessageLevel level) {
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
