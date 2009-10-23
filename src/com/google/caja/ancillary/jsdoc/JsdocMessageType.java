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

package com.google.caja.ancillary.jsdoc;

import java.io.IOException;

import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageType;
import com.google.caja.reporting.MessageTypeInt;

public enum JsdocMessageType implements MessageTypeInt {
  SCRIPT_FAILED_AT_RUNTIME(
      "Exception thrown in interpreter: %s", MessageLevel.FATAL_ERROR),
  RUNTIME_MESSAGE("Jsdoc: %s", MessageLevel.ERROR),
  ANNOTATION_OUT_OF_PLACE(
      "%s: Annotation %s cannot appear here", MessageLevel.ERROR),
  BAD_LINK(
      "%s: Expected reference or URL, not %s", MessageLevel.ERROR),
  DID_YOU_MEAN(
      "%s: Did you mean \"%s\" instead of \"%s\"", MessageLevel.LINT),
  DUPLICATE_DOCUMENTATION(
      "%s: Documentation conflicts with that at %s", MessageLevel.ERROR),
  EXPECTED_DOCUMENTATION_TEXT(
      "%s: Expected documentation, not %s", MessageLevel.ERROR),
  EXPECTED_EMAIL_OR_NAME(
      "%s: Expected an email or name, not %s", MessageLevel.ERROR),
  EXPECTED_IDENTIFIER(
      "%s: Expected identifier, not %s", MessageLevel.ERROR),
  EXPECTED_TYPE(
      "%s: Expected type, not %s", MessageLevel.ERROR),
  EXPECTED_URL_OR_REFERENCE(
      "%s: Expected URL or reference, not %s", MessageLevel.ERROR),
  UNRECOGNIZED_ANNOTATION(
      "%s: Unrecognized annotation %s", MessageLevel.ERROR),
  UNEXPECTED_CONTENT(
      "%s: Unexpected content for %s", MessageLevel.ERROR),
  ;

  private final String formatString;
  private final MessageLevel level;
  private final int paramCount;

  JsdocMessageType(String formatString, MessageLevel level) {
    this.formatString = formatString;
    this.level = level;
    this.paramCount = MessageType.formatStringArity(formatString);
  }

  public void format(MessagePart[] parts, MessageContext context,
                     Appendable out) throws IOException {
    MessageType.formatMessage(formatString, parts, context, out);
  }

  public MessageLevel getLevel() { return level; }

  public int getParamCount() {
    return paramCount;
  }
}
