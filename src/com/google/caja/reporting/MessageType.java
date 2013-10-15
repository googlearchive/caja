// Copyright (C) 2011 Google Inc.
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

  INTERNAL_ERROR("Internal error: %s", MessageLevel.FATAL_ERROR),
  IO_ERROR("I/O Error: %s", MessageLevel.FATAL_ERROR),
  NO_SUCH_FILE("%s: could not read input", MessageLevel.FATAL_ERROR),

  // lexing messages
  UNTERMINATED_STRING_TOKEN("%s: Unclosed string", MessageLevel.FATAL_ERROR),
  UNTERMINATED_COMMENT_TOKEN(
      "%s: Unclosed comment", MessageLevel.FATAL_ERROR),
  UNREPRESENTABLE_INTEGER_LITERAL(
      "%s: Integer literal %s doesn't fit in 51 bits", MessageLevel.WARNING),
  MALFORMED_NUMBER("%s: Malformed number %s", MessageLevel.FATAL_ERROR),
  UNRECOGNIZED_ESCAPE("%s: Unrecognized escape '%s'", MessageLevel.FATAL_ERROR),
  MALFORMED_STRING("%s: Illegal char in string '%s'", MessageLevel.FATAL_ERROR),
  MALFORMED_URI("%s: Not a valid uri: '%s'", MessageLevel.FATAL_ERROR),
  MALFORMED_XHTML("%s: malformed xhtml: %s", MessageLevel.FATAL_ERROR),
  MALFORMED_HTML_ENTITY(
      "%s: HTML entity missing closing semicolon %s", MessageLevel.WARNING),
  REDUNDANT_ESCAPE_SEQUENCE(
      "%s: escape %s is redundant in a quoted string", MessageLevel.LINT),
  AMBIGUOUS_ESCAPE_SEQUENCE(
      "%s: escape sequence %s does not work in all interpreters",
      MessageLevel.WARNING),
  INVALID_CSS_COMMENT(
      "%s: Line comments non-standard in CSS.", MessageLevel.LINT),

  // parsing
  END_OF_FILE("Unexpected end of input in %s", MessageLevel.ERROR),
  EXPECTED_TOKEN("%s: Expected %s not %s", MessageLevel.ERROR),
  UNUSED_TOKENS("%s: Unused tokens: %s ...", MessageLevel.ERROR),
  MAYBE_MISSING_SEMI("%s: Maybe missing semicolon", MessageLevel.WARNING),
  SEMICOLON_INSERTED("%s: Semicolon inserted", MessageLevel.LINT),
  PLACEHOLDER_INSERTED("%s: Placeholder inserted", MessageLevel.WARNING),
  RESERVED_WORD_USED_AS_IDENTIFIER(
      "%s: Reserved word %s used as an identifier", MessageLevel.ERROR),
  INVALID_IDENTIFIER("%s: Malformed identifier %s", MessageLevel.ERROR),
  INVALID_TAG_NAME("%s: Malformed tag name %s", MessageLevel.ERROR),
  UNEXPECTED_TOKEN("%s: Unexpected token %s", MessageLevel.ERROR),
  DUPLICATE_FORMAL_PARAM(
      "%s: Duplicate formal parameter %s", MessageLevel.ERROR),
  UNRECOGNIZED_REGEX_MODIFIERS(
      "%s: Unrecognized regular expression modifiers %s", MessageLevel.ERROR),
  PARSE_ERROR("%s: Parse error", MessageLevel.ERROR),
  AMBIGUOUS_ATTRIBUTE_VALUE(
      "%s: attribute %s has ambiguous value \"%s\"", MessageLevel.WARNING),
  UNEXPECTED_IN_XML_TAG(
      "%s: Removed unexpected \"%s\" in XML tag",
      MessageLevel.WARNING),
  MISSING_ATTRIBUTE_VALUE(
      "%s: missing value for attribute %s", MessageLevel.FATAL_ERROR),
  OCTAL_LITERAL("%s: octal literal %s", MessageLevel.LINT),
  UNRECOGNIZED_DIRECTIVE_IN_PROLOGUE(
      "%s: unrecognized directive in prologue: %s",
      MessageLevel.WARNING),
  SKIPPING("%s: Skipping malformed content", MessageLevel.WARNING),
  DUPLICATE_ATTRIBUTE(
      "%s: attribute %s duplicates one at %s", MessageLevel.WARNING),
  NO_SUCH_NAMESPACE(
      "%s: unrecognized namespace %s on %s", MessageLevel.WARNING),
  ILLEGAL_NAMESPACE_NAME(
      "%s: illegal namespace name: %s", MessageLevel.WARNING),
  CANNOT_OVERRIDE_DEFAULT_NAMESPACE_IN_HTML(
      "%s: cannot override default XML namespace in HTML",
      MessageLevel.WARNING),
  INVALID_HTML_COMMENT(
      "%s: invalid '--' in html/xml comment", MessageLevel.WARNING),

  // platform context
  NOT_IE("%s: Will not work in IE", MessageLevel.WARNING),

  // symbol errors
  SYMBOL_REDEFINED("%s: %s originally defined at %s", MessageLevel.LINT),
  UNDOCUMENTED_GLOBAL("%s: Undocumented global %s", MessageLevel.LINT),
  INVALID_ASSIGNMENT(
      "%s: Invalid assignment to %s", MessageLevel.ERROR),
  MASKING_SYMBOL(
      "%s: Declaration of %s masks declaration at %s", MessageLevel.LINT),
  UNDEFINED_SYMBOL(
      "%s: Symbol %s has not been defined", MessageLevel.ERROR),
  ASSIGN_TO_NON_LVALUE(
      "%s: Assignment to non lvalue: %s", MessageLevel.ERROR),

  // runtime, as during constant folding
  DIVISION_BY_ZERO("%s: Division by zero", MessageLevel.WARNING),
  INDEX_OUT_OF_BOUNDS("%s: Index out of bounds %s", MessageLevel.WARNING),
  INVALID_MEMBER_ACCESS("%s: Invalid member %s", MessageLevel.WARNING),
  INVALID_REGEXP_FLAGS("%s: Invalid regexp flags %s", MessageLevel.WARNING),
  INVALID_SHIFT_AMOUNT("%s: Cannot shift by %s bits", MessageLevel.WARNING),
  INVALID_MASK("%s: Masking outside 32 bits: %s", MessageLevel.WARNING),
  POSSIBLE_SIDE_EFFECT("%s: Possible side-effecting operation's value not used",
                       MessageLevel.WARNING),
  NO_SIDE_EFFECT("%s: Operation has no effect", MessageLevel.WARNING),

  // logging
  BUILD_INFO("Google Caja. Copyright (C) 2011, Google Inc. Rev %s built on %s.",
             MessageLevel.LOG),
  COMPILER_DONE("compiler done in %s msec (%s msec in %s)", MessageLevel.LOG);

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

  @SuppressWarnings("resource")
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
