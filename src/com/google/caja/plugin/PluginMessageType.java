// Copyright (C) 2006 Google Inc.
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

package com.google.caja.plugin;

import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageType;
import com.google.caja.reporting.MessageTypeInt;

import java.io.IOException;

/**
 * Messages for the PluginCompiler
 *
 * @author mikesamuel@gmail.com
 */
public enum PluginMessageType implements MessageTypeInt {
  ILLEGAL_GLOBAL_ACCESS(
      "%s: access not allowed to global %s", MessageLevel.FATAL_ERROR),
  UNSAFE_ACCESS(
      "%s: unsafe access to protected namespace: %s", MessageLevel.FATAL_ERROR),
  UNKNOWN_TAG("%s: unknown tag %s", MessageLevel.WARNING),
  UNSAFE_TAG("%s: removing disallowed tag %s", MessageLevel.WARNING),
  MISSING_ATTRIBUTE("%s: expected param %s on %s", MessageLevel.ERROR),
  UNKNOWN_ATTRIBUTE("%s: removing unknown attribute %s on %s",
      MessageLevel.WARNING),
  UNSAFE_ATTRIBUTE("%s: removing disallowed attribute %s on tag %s",
                   MessageLevel.WARNING),
  FOLDING_ELEMENT("%s: folding element %s into parent", MessageLevel.WARNING),
  CANNOT_FOLD_ATTRIBUTE("%s: removing attribute %s when folding %s into parent",
                        MessageLevel.WARNING),
  DISALLOWED_ATTRIBUTE_VALUE("%s: attribute %s cannot have value %s",
                             MessageLevel.WARNING),
  BAD_IDENTIFIER("%s: bad identifier %s", MessageLevel.FATAL_ERROR),
  ATTRIBUTE_CANNOT_BE_DYNAMIC(
      "%s: tag %s cannot have dynamic attribute %s", MessageLevel.ERROR),
  DISALLOWED_URI("%s: url %s cannot be linked to", MessageLevel.FATAL_ERROR),
  MALFORMED_URL("%s: malformed url %s", MessageLevel.FATAL_ERROR),
  MALFORMED_CSS_PROPERTY_VALUE(
      "%s: css property %s has bad value: %s", MessageLevel.WARNING),
  DISALLOWED_CSS_PROPERTY_IN_SELECTOR(
      "%s: css property %s not allowed in :visited selector at %s",
      MessageLevel.ERROR),
  UNKNOWN_CSS_PROPERTY("%s: unknown css property %s", MessageLevel.ERROR),
  CSS_VALUE_OUT_OF_RANGE(
      "%s: css property %s with value %s not in range [%s, %s]",
      MessageLevel.WARNING),
  UNSAFE_CSS_IDENTIFIER(
      "%s: css identifier '%s' contains characters that may not work"
      + " on all browsers", MessageLevel.FATAL_ERROR),
  UNSAFE_CSS_PROPERTY("%s: unsafe css property %s", MessageLevel.ERROR),
  UNSAFE_CSS_PSEUDO_SELECTOR(
      "%s: unsafe css pseudo-selector %s", MessageLevel.ERROR),
  CSS_DASHMATCH_ATTRIBUTE_OPERATOR_NOT_ALLOWED(
      "%s: css dash match attribute operation not allowed", MessageLevel.ERROR),
  CSS_URI_VALUED_ATTRIBUTE_SELECTOR_NOT_ALLOWED(
      "%s: css URI-valued attribute selector not allowed", MessageLevel.ERROR),
  CSS_ATTRIBUTE_NAME_NOT_ALLOWED_IN_SELECTOR(
      "%s: css attribute name %s not allowed in selector", MessageLevel.ERROR),
  CSS_ATTRIBUTE_TYPE_NOT_ALLOWED_IN_SELECTOR(
      "%s: css attribute type %s not allowed in selector", MessageLevel.ERROR),
  CSS_LINK_PSEUDO_SELECTOR_NOT_ALLOWED_ON_NONANCHOR(
      "%s: css :link and :visited pseudo selectors only allowed on A elements",
      MessageLevel.ERROR),
  SKIPPING_CSS_PROPERTY(
      "%s: skipping invalid css property %s", MessageLevel.WARNING),
  IMPORTS_NOT_ALLOWED_HERE("%s: @import not allowed here", MessageLevel.ERROR),
  CYCLIC_INCLUDE("%s: cyclic include of %s", MessageLevel.ERROR),
  FONT_FACE_NOT_ALLOWED("%s: @font-face not allowed", MessageLevel.ERROR),
  FAILED_TO_LOAD_EXTERNAL_URL(
      "%s: failed to load external url %s", MessageLevel.WARNING),
  NO_CONTENT("%s: no content", MessageLevel.FATAL_ERROR),
  UNRECOGNIZED_CONTENT_TYPE(
      "%s: unrecognized content type %s for %s tag", MessageLevel.WARNING),
  UNRECOGNIZED_MEDIA_TYPE(
      "%s: unrecognized media type %s", MessageLevel.WARNING),
  UNRECOGNIZED_ATTRIBUTE_VALUE(
      "%s: unrecognized attribute value %s for %s attribute of %s tag",
      MessageLevel.WARNING),
  QUOTED_CSS_VALUE("%s: quoted unquoted css value %s", MessageLevel.LINT),
  ASSUMING_PIXELS_FOR_LENGTH(
      "%s: assuming pixels for length %s", MessageLevel.WARNING),
  NON_STANDARD_COLOR(
      "%s: replacing non-standard color %s with hex %s", MessageLevel.LINT),
  MALFORMED_ENVELOPE(
      "%s: malformed envelope around cajoled code", MessageLevel.ERROR),
  SPECIALIZING_CSS_PROPERTY(
      "%s: specialized CSS property %s to %s", MessageLevel.WARNING),
  MISSING_XML_NAMESPACE(
      "%s: XML %s has prefix but no namespace", MessageLevel.ERROR),
  INVALID_PIPELINE("Cannot find plan from %s to %s", MessageLevel.FATAL_ERROR),
  ;

  private final String formatString;
  private final MessageLevel level;
  private final int paramCount;

  PluginMessageType(String formatString, MessageLevel level) {
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
