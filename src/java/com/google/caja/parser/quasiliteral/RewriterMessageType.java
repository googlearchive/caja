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

package com.google.caja.parser.quasiliteral;

import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageType;
import com.google.caja.reporting.MessageTypeInt;

import java.io.IOException;

/**
 * The type of a {Message message} for the JavaScript quasiliteral rewriter.
 *
 * @author mikesamuel@gmail.com
 * @author ihab.awad@gmail.com
 */
public enum RewriterMessageType implements MessageTypeInt {

  PARAMETERS_TO_SUPER_CONSTRUCTOR_MAY_NOT_CONTAIN_THIS(
      "%s: Parameters to super constructor may not contain \"this\": %s, %s",
      MessageLevel.FATAL_ERROR),

  VARIABLES_CANNOT_END_IN_DOUBLE_UNDERSCORE(
      "%s: Variables cannot end in \"__\": %s, %s",
      MessageLevel.FATAL_ERROR),

  PROPERTIES_CANNOT_END_IN_DOUBLE_UNDERSCORE(
      "%s: Properties cannot end in \"__\": %s, %s",
      MessageLevel.FATAL_ERROR),

  SELECTORS_CANNOT_END_IN_DOUBLE_UNDERSCORE(
      "%s: Selectors cannot end in \"__\": %s, %s",
      MessageLevel.FATAL_ERROR),

  GLOBALS_CANNOT_END_IN_UNDERSCORE(
      "%s: Globals cannot end in \"_\": %s, %s",
      MessageLevel.FATAL_ERROR),

  PUBLIC_PROPERTIES_CANNOT_END_IN_UNDERSCORE(
      "%s: Public properties cannot end in \"_\": %s, %s",
      MessageLevel.FATAL_ERROR),

  PUBLIC_SELECTORS_CANNOT_END_IN_UNDERSCORE(
      "%s: Public selectors cannot end in \"_\": %s, %s",
      MessageLevel.FATAL_ERROR),

  ANONYMOUS_FUNCTION_REFERENCES_THIS(
      "%s: Anonymous function references \"this\" but isn't part of a class definition: %s, %s",
      MessageLevel.FATAL_ERROR),

  CONSTRUCTOR_CANNOT_ESCAPE(
      "%s: Constructor cannot escape: %s, %s",
      MessageLevel.FATAL_ERROR),

  INVOKED_INSTANCEOF_ON_NON_FUNCTION(
      "%s: Invoked \"instanceof\" on non-function: %s, %s",
      MessageLevel.FATAL_ERROR),

  MAP_EXPRESSION_EXPECTED(
      "%s: Map expression expected: %s, %s",
      MessageLevel.FATAL_ERROR),

  KEY_MAY_NOT_END_IN_UNDERSCORE(
      "%s: Key may not end in \"_\": %s, %s",
      MessageLevel.FATAL_ERROR),

  MEMBER_KEY_MAY_NOT_END_IN_DOUBLE_UNDERSCORE(
      "%s: Member key may not end in \"__\": %s, %s",
      MessageLevel.FATAL_ERROR),

  CONSTRUCTORS_ARE_NOT_FIRST_CLASS(
      "%s: Constructors are not first class: %s, %s",
      MessageLevel.FATAL_ERROR),

  CAJA_DEF_ON_NON_CTOR(
      "%s: caja.def called with non-constructor: %s, %s",
      MessageLevel.FATAL_ERROR),

  DUPLICATE_DEFINITION_OF_LOCAL_VARIABLE(
      "%s: Duplicate definition of local variable: %s",
      MessageLevel.FATAL_ERROR),

  NEW_ON_ARBITRARY_EXPRESSION_DISALLOWED(
      "%s: Cannot invoke \"new\" on an arbitrary expression: %s, %s",
      MessageLevel.FATAL_ERROR),

  CANNOT_ASSIGN_TO_THIS(
      "%s: Cannot assign to \"this\": %s, %s",
      MessageLevel.FATAL_ERROR),
  ;

  private final String formatString;
  private final MessageLevel level;
  private final int paramCount;

  RewriterMessageType(String formatString, MessageLevel level) {
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
