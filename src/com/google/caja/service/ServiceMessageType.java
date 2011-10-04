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

package com.google.caja.service;

import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageType;
import com.google.caja.reporting.MessageTypeInt;

import java.io.IOException;

/**
 * Messages for the cajoling service
 *
 * @author mikesamuel@gmail.com
 * @author ihab.awad@gmail.com
 */
public enum ServiceMessageType implements MessageTypeInt {
  WRONG_BUILD_VERSION("Build version error: Expected %s but was %s",
      MessageLevel.FATAL_ERROR),
  EXCEPTION_IN_SERVICE("Service threw exception %s", MessageLevel.FATAL_ERROR),
  INVALID_ARGUMENT("Invalid value %s for argument %s",
      MessageLevel.FATAL_ERROR),
  INVALID_INPUT_URL("Invalid input URL %s", MessageLevel.FATAL_ERROR),
  MISSING_ARGUMENT("Missing argument %s", MessageLevel.FATAL_ERROR),
  UNSUPPORTED_CONTENT_TYPES("Requested content types are unsupported",
      MessageLevel.FATAL_ERROR),
  UNEXPECTED_INPUT_MIME_TYPE("Expected input MIME type %s but found %s",
      MessageLevel.FATAL_ERROR),
  ;

  private final String formatString;
  private final MessageLevel level;
  private final int paramCount;

  ServiceMessageType(String formatString, MessageLevel level) {
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
