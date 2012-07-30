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

package com.google.caja.ancillary.linter;

import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageType;
import com.google.caja.reporting.MessageTypeInt;

import java.io.IOException;

public enum LinterMessageType implements MessageTypeInt {
  UNUSED_PROVIDE("%s: @provides %s not provided", MessageLevel.ERROR),
  UNUSED_REQUIRE("%s: @requires %s not used", MessageLevel.ERROR),
  LABEL_DOES_NOT_MATCH_LOOP(
      "%s: Unmatched break or continue to label %s", MessageLevel.ERROR),
  RETURN_OUTSIDE_FUNCTION(
      "%s: Return does not appear inside a function", MessageLevel.ERROR),
  UNCAUGHT_THROW_DURING_INIT(
      "%s: Uncaught exception thrown during initialization",
      MessageLevel.LINT),
  SYMBOL_NOT_LIVE(
      "%s: Symbol %s may be used before being initialized",
      MessageLevel.WARNING),
  DUPLICATE_LABEL("%s: Label %s nested inside %s", MessageLevel.ERROR),
  MULTIPLY_PROVIDED_SYMBOL(
      "%s: Another input, %s, already @provides %s", MessageLevel.ERROR),
  OUT_OF_BLOCK_SCOPE(
      "%s: Usage of %s declared at %s is out of block scope.",
      MessageLevel.ERROR),
  CODE_NOT_REACHABLE("%s: Code is not reachable", MessageLevel.WARNING),
  EMBED_HAZARD(
      "%s: '%s' may cause problems when embedding script.",
      MessageLevel.ERROR),
  BARE_KEYWORD(
      "%s: IE<=8 does not allow bare literal use of keyword '%s'",
      MessageLevel.ERROR),
  ;

  private final String formatString;
  private final MessageLevel level;
  private final int paramCount;

  LinterMessageType(String formatString, MessageLevel level) {
    this.formatString = formatString;
    this.level = level;
    this.paramCount = MessageType.formatStringArity(formatString);
  }

  public int getParamCount() {
    return paramCount;
  }

  public void format(MessagePart[] parts, MessageContext context,
                     Appendable out)
      throws IOException {
    MessageType.formatMessage(formatString, parts, context, out);
  }

  public MessageLevel getLevel() { return level; }
}
