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

  VALUEOF_PROPERTY_MUST_NOT_BE_SET(
      "%s: The valueOf property must not be set: %s, %s",
      MessageLevel.FATAL_ERROR),

  VALUEOF_PROPERTY_MUST_NOT_BE_DELETED(
      "%s: The valueOf property must not be deleted: %s, %s",
      MessageLevel.FATAL_ERROR),

  VARIABLES_CANNOT_END_IN_DOUBLE_UNDERSCORE(
      "%s: Variables cannot end in \"__\": %s, %s",
      MessageLevel.FATAL_ERROR),

  PROPERTIES_CANNOT_END_IN_DOUBLE_UNDERSCORE(
      "%s: Properties cannot end in \"__\": %s, %s",
      MessageLevel.FATAL_ERROR),

  GETTERS_SETTERS_NOT_SUPPORTED(
      "%s: Getter and setter properties not supported: %s",
      MessageLevel.FATAL_ERROR),

  SELECTORS_CANNOT_END_IN_DOUBLE_UNDERSCORE(
      "%s: Selectors cannot end in \"__\": %s, %s",
      MessageLevel.FATAL_ERROR),

  LABELS_CANNOT_END_IN_DOUBLE_UNDERSCORE(
      "%s: Labels cannot end in \"__\": %s",
      MessageLevel.ERROR),

  INVOKED_INSTANCEOF_ON_NON_FUNCTION(
      "%s: Invoked \"instanceof\" on non-function: %s, %s",
      MessageLevel.FATAL_ERROR),

  MEMBER_KEY_MAY_NOT_END_IN_DOUBLE_UNDERSCORE(
      "%s: Member key may not end in \"__\": %s, %s",
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

  WITH_BLOCKS_NOT_ALLOWED(
      "%s: \"with\" blocks are not allowed",
      MessageLevel.ERROR),

  NOT_DELETABLE(
      "%s: Invalid operand to delete",
      MessageLevel.ERROR),

  NONASCII_IDENTIFIER(
      "%s: identifier contains non-ASCII characters: %s",
      MessageLevel.FATAL_ERROR),

  ALPHA_RENAMING_FAILURE(
      "%s: INTERNAL COMPILER ERROR: output of alpha renamer has free vars %s."
      + "  Please report this error at "
      + "http://code.google.com/p/google-caja/issues/",
      MessageLevel.ERROR),

  ILLEGAL_IDENTIFIER_LEFT_OVER(
      "%s: INTERNAL COMPILER ERROR. "
          + "Illegal identifier passed through from rewriter: %s. "
          + "Please report this error at: http://code.google.com/p/google-caja/issues/",
      MessageLevel.FATAL_ERROR),

  UNSEEN_NODE_LEFT_OVER(
      "%s: INTERNAL COMPILER ERROR. "
          + "Unseen node left over from rewriter: %s. "
          + "Please report this error at: http://code.google.com/p/google-caja/issues/",
      MessageLevel.FATAL_ERROR),

  MULTIPLY_TAINTED(
      "%s: INTERNAL COMPILER WARNING. "
          + "Node appears multiple times in tree being tainted: %s. "
          + "Please report this error at: http://code.google.com/p/google-caja/issues/",
      MessageLevel.WARNING),

  UNMATCHED_NODE_LEFT_OVER(
      "%s: INTERNAL COMPILER ERROR. "
          + "Node did not match any rules at: %s. "
          + "Please report this error at: http://code.google.com/p/google-caja/issues/",
      MessageLevel.FATAL_ERROR),

  NOEXPAND_BINARY_DECL(
      "%s: INTERNAL COMPILER ERROR. "
          + "Can't noexpand a binary Declaration: %s. "
          + "Please report this error at: http://code.google.com/p/google-caja/issues/",
          MessageLevel.ERROR),

  BAD_RESULT_FROM_RECURSIVE_CALL(
      "%s: INTERNAL COMPILER ERROR. "
          + "Expected result from a recursive expansion: %s."
          + " Please report this error at:"
          + " http://code.google.com/p/google-caja/issues/",
          MessageLevel.ERROR),

  ARGUMENTS_IN_GLOBAL_CONTEXT(
      "%s: 'arguments' used in a global context", MessageLevel.ERROR),

  THIS_IN_GLOBAL_CONTEXT(
      "%s: \"this\" cannot be used in the global context",
      MessageLevel.FATAL_ERROR),

  CANNOT_ASSIGN_TO_FREE_VARIABLE(
      "%s: Cannot assign to a free module variable: %s, %s",
      MessageLevel.WARNING),

  FREE_VARIABLE("%s: free variable %s", MessageLevel.ERROR),

  CANNOT_MASK_IDENTIFIER(
      "%s: Cannot mask identifier \"%s\"", MessageLevel.FATAL_ERROR),

  CANNOT_ASSIGN_TO_IDENTIFIER(
      "%s: Cannot assign to identifier \"%s\"", MessageLevel.FATAL_ERROR),

  CANNOT_ASSIGN_TO_FUNCTION_NAME(
      "%s: Cannot assign to a function name: %s, %s",
      MessageLevel.FATAL_ERROR),

  CANNOT_REDECLARE_FUNCTION_NAME(
      "%s: Cannot redeclare a function name: %s, %s",
      MessageLevel.FATAL_ERROR),

  CANNOT_REDECLARE_VAR(
      "%s: Cannot redeclare %s originally declared at %s",
      MessageLevel.ERROR),

  LOADING_MODULE_FAILED(
          "%s: Loading module %s failed: %s",
          MessageLevel.FATAL_ERROR),

  INVALID_MODULE_URI(
          "%s: Invalid URI for the module: %s",
          MessageLevel.FATAL_ERROR),

  MODULE_NOT_FOUND(
          "%s: Module not found: %s",
          MessageLevel.FATAL_ERROR),

  PARSING_MODULE_FAILED(
      "%s: Parsing module failed: %s",
      MessageLevel.FATAL_ERROR),

  CANNOT_LOAD_A_DYNAMIC_ES53_MODULE(
      "%s: Dynamically computed names should use load.async()",
      MessageLevel.FATAL_ERROR),

  CANNOT_LOAD_A_DYNAMIC_SERVERJS_MODULE(
      "%s: Dynamically computed names should use require.async()",
      MessageLevel.FATAL_ERROR),

  TOP_LEVEL_FUNC_INCOMPATIBLE_WITH_CAJA(
      "%s: Caja makes top-level functions local to a script tag."
      + " If you need a global function, use: %s",
      MessageLevel.LINT);

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
