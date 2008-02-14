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

package com.google.caja.config;

import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageType;
import com.google.caja.reporting.MessageTypeInt;

import java.io.IOException;

/**
 * Messages for the Config Parsers.
 *
 * @author mikesamuel@gmail.com
 */
public enum ConfigMessageType implements MessageTypeInt {
  MALFORMED_CONFIG("%s: malformed config file: expected %s, not %s",
                   MessageLevel.FATAL_ERROR),
  BAD_URL("%s: malformed URI: %s = %s", MessageLevel.FATAL_ERROR),
  AMBIGUOUS_DEFINITION("%s: ambiguous type definition %s != %s",
                       MessageLevel.FATAL_ERROR),
  // If a key appeared in a white list that was not recognized.
  // If "denies" is mis-spelled as "denied", then this will trigger.
  UNRECOGNIZED_KEY("%s: unrecognized key %s", MessageLevel.WARNING),
  ;

  private final String formatString;
  private final MessageLevel level;
  private final int paramCount;

  ConfigMessageType(String formatString, MessageLevel level) {
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
