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

import java.io.IOException;
import java.util.Formatter;

/**
 * The type of a {@link Message message}.
 *
 * @author mikesamuel@gmail.com
 */
public enum MessageType implements MessageTypeInt {
  // TODO(mikesamuel): rename this to CommonMessageType and rename
  // MessageTypeInt to MessageType

  UNIMPLEMENTED("Lazy author error", MessageLevel.FATAL_ERROR),
  INTERNAL_ERROR("Internal error: %s", MessageLevel.FATAL_ERROR),
  IO_ERROR("Error reading from %s", MessageLevel.FATAL_ERROR),

  // command line flags
  NO_SOURCE_FILES("No source files specified", MessageLevel.FATAL_ERROR),
  UNRECOGNIZED_FLAG("Unrecognized flag: %s", MessageLevel.FATAL_ERROR),
  DUPLICATE_FLAG(
      "duplicate command line argument %s", MessageLevel.FATAL_ERROR),

  // lexing messages
  UNTERMINATED_STRING_TOKEN("%s: Unclosed string", MessageLevel.FATAL_ERROR),
  UNTERMINATED_COMMENT_TOKEN(
      "%s: Unclosed comment", MessageLevel.FATAL_ERROR),
  UNREPRESENTABLE_INTEGER_LITERAL(
      "Integer literal %s at %s doesn't fit in 51 bits", MessageLevel.WARNING),
  MALFORMED_NUMBER("%s: Malformed number %s", MessageLevel.FATAL_ERROR),
  UNRECOGNIZED_ESCAPE("%s: Unrecognized escape '%s'", MessageLevel.FATAL_ERROR),
  MALFORMED_STRING("%s: Illegal char in string '%s'", MessageLevel.FATAL_ERROR),
  MALFORMED_URI("%s: Not a valid uri: '%s'", MessageLevel.FATAL_ERROR),
  MALFORMED_XHTML("%s: malformed xhtml: %s", MessageLevel.FATAL_ERROR),
  MISSING_ENDTAG("%s: missing end tag %s, saw %s instead",
                 MessageLevel.FATAL_ERROR),

  // parsing
  END_OF_FILE("Unexpected end of input in %s", MessageLevel.ERROR),
  EXPECTED_TOKEN("%s: Expected %s not %s", MessageLevel.ERROR),
  UNUSED_TOKENS("%s: Unused tokens: %s ...", MessageLevel.ERROR),
  SEMICOLON_INSERTED("%s: Semicolon inserted", MessageLevel.LINT),
  PLACEHOLDER_INSERTED("%s: Placeholder inserted", MessageLevel.WARNING),
  RESERVED_WORD_USED_AS_IDENTIFIER(
      "Reserved word %s used as an identifier at %s", MessageLevel.ERROR),
  INVALID_IDENTIFIER(
      "Malformed identifier %s at %s", MessageLevel.ERROR),
  UNEXPECTED_TOKEN("%s: Unexpected token %s", MessageLevel.ERROR),
  DUPLICATE_FORMAL_PARAM("Duplicate formal parameter %s at %s",
      MessageLevel.ERROR),
  UNRECOGNIZED_REGEX_MODIFIERS(
      "%s: Unrecognized regular expression modifiers %s", MessageLevel.ERROR),
  PARSE_ERROR("%s: Parse error in %s", MessageLevel.ERROR),
  ILLEGAL_NAME("%s: Illegal name: %s", MessageLevel.ERROR),

  // platform context
  NOT_IE("%s: Will not work in IE", MessageLevel.WARNING),

  // symbol errors
  IMPLICIT_DEFINITION(
      "Symbol %s implicitly defined at %s", MessageLevel.WARNING),
  SYMBOL_REDEFINED("%s: %s originally defined at %s", MessageLevel.ERROR),
  UNRECOGNIZED_TYPE("%s: No such type %s", MessageLevel.ERROR),
  INVALID_TYPE("Invalid type %s at %s", MessageLevel.ERROR),
  INVALID_DECLARATION(
      "Invalid declaration of %s at %s", MessageLevel.ERROR),
  INVALID_DECLARATION_GENERIC_ENUM(
      "Invalid declaration of %s at %s: enumerations can't be generic",
      MessageLevel.ERROR),
  ACCESS_VIOLATION("%s: Cannot access %s defined at %s", MessageLevel.ERROR),
  MEMBER_ACCESS_VIOLATION("%s: Cannot access member %s of %s",
      MessageLevel.ERROR),
  NO_SUCH_MEMBER("%s: No such member %s in type %s", MessageLevel.ERROR),
  SYMBOL_PARTIALLY_AVAILABLE(
      "Symbol %s declared at %s partially defined in context of %s",
      MessageLevel.ERROR),
  TYPE_NOT_EXTENDABLE("%s: Type %s cannot be bound with %s to %s",
      MessageLevel.ERROR),
  EXTRA_TEMPLATE_PARAM(
      "Extraneous paramater %s specified at %s for type defined at %s",
      MessageLevel.ERROR),
  MISSING_TEMPLATE_PARAM(
      "Type at %s is missing paramater %s declared at %s",
      MessageLevel.ERROR),
  WRONG_TEMPLATE_PARAMS(
      "Type %s at %s has wrong parameters.  Expected %s",
      MessageLevel.ERROR),
  MASKING_SYMBOL(
      "%s: Declaration of %s masks declaration at %s", MessageLevel.LINT),
  SCOPE_BLEED(
      "Symbol %s defined at %s is accessed across scope boundaries at %s",
      MessageLevel.ERROR),
  UNDEFINED_SYMBOL(
      "Symbol %s referenced at %s has not been defined", MessageLevel.ERROR),
  PROTOTYPE_REDEFINED(
      "Prototype for class %s redefined at %s", MessageLevel.ERROR),
  SUPER_CLASS_MISMATCH(
      "Superclass of %s at %s does not match earlier declaration at %s",
      MessageLevel.ERROR),
  SUB_CLASSING_WARNING(
      "%s: Obscure subclassing of %s", MessageLevel.WARNING),
  EXPECTED_CLASS_TYPE(
      "Type %s declared at %s conflicts with constructor at %s -- not a class",
      MessageLevel.ERROR),
  CONST_WITHOUT_INITIALIZER(
      "Failed to determine initializer for const %s declared at %s",
      MessageLevel.ERROR),
  BREAK_OUTSIDE_LOOP(
      "Break statement at %s outside loop", MessageLevel.ERROR),
  CONTINUE_OUTSIDE_LOOP(
      "Continue statement at %s outside loop", MessageLevel.ERROR),

  // caught to decide whether to treat a directive comment as a declaration or
  // a type for a variable declaration
  NO_DECLARATION(
      "No declaration at %s", MessageLevel.ERROR),

  // lint checks
  NAMING_CONVENTION_VIOLATION(
      "Identifier %s at %s violates naming convention %s", MessageLevel.LINT),

  // other inference
  GENERATED_IDENTIFIER(
      "Generated unique identifier %s for node at %s", MessageLevel.INFERENCE),

  // runtime, as during constant folding
  DIVISION_BY_ZERO("%s: division by zero", MessageLevel.WARNING),
  INDEX_OUT_OF_BOUNDS("%s: index out of bounds %s", MessageLevel.WARNING),
  INVALID_MEMBER_ACCESS("%s: invalid member %s", MessageLevel.WARNING),
  INVALID_REGEXP_FLAGS("%s: invalid regexp flags %s", MessageLevel.WARNING),
  INVALID_SHIFT_AMOUNT("%s: cannot shift by %s bits", MessageLevel.WARNING),
  INVALID_MASK("%s: masking outside 32 bits: %s", MessageLevel.WARNING),
  POSSIBLE_SIDE_EFFECT("%s: possible side-effecting operation's value not used",
                       MessageLevel.WARNING),


  // logging
  CHECKPOINT("Checkpoint: %s at T+%s seconds", MessageLevel.LOG),
  BUILD_INFO("Google Caja. Copyright (C) 2008, Google Inc. Rev %s built on %s.",
             MessageLevel.LOG),    
  ;

  private final String formatString;
  private final MessageLevel level;
  private final int paramCount;

  MessageType(String formatString, MessageLevel level) {
    this.formatString = formatString;
    this.level = level;
    this.paramCount = formatStringArity(formatString);
  }

  public void format(MessagePart[] parts, MessageContext context,
                     Appendable out) throws IOException {
    formatMessage(formatString, parts, context, out);
  }

  public MessageLevel getLevel() { return level; }

  public int getParamCount() {
    return paramCount;
  }

  public static void formatMessage(
      String formatString, MessagePart[] parts, MessageContext context,
      Appendable out)
      throws IOException {
    Object[] partStrings = new Object[parts.length];
    for (int i = 0; i < parts.length; ++i) {
      StringBuilder sb = new StringBuilder();
      parts[i].format(context, sb);
      partStrings[i] = sb.toString();
    }
    new Formatter(out).format(formatString, partStrings);
  }

  public static int formatStringArity(String formatString) {
    int count = 0;
    for (int i = 0, n = formatString.length(); i < n; ++i) {
      char ch = formatString.charAt(i);
      if ('%' == ch) {
        if (i + 1 < n && '%' != formatString.charAt(i + 1)) {
          ++count;
        } else {
          ++i;
        }
      }
    }
    return count;
  }
}
