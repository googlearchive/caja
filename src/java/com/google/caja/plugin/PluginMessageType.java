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
  XHTML_NO_SUCH_TAG("%s: unrecognized tag %s", MessageLevel.FATAL_ERROR),
  XHTML_NO_SUCH_PARAM(
      "%s: unrecognized param %s on <%s>", MessageLevel.FATAL_ERROR),
  UNKNOWN_TAG("%s: unknown tag %s", MessageLevel.ERROR),
  UNSAFE_TAG("%s: tag %s is not allowed", MessageLevel.ERROR),
  NO_SUCH_TEMPLATE("%s: no such template %s", MessageLevel.FATAL_ERROR),
  MISSING_ATTRIBUTE("%s: expected param %s on %s", MessageLevel.ERROR),
  DUPLICATE_ATTRIBUTE(
      "%s: attribute %s duplicates one at %s", MessageLevel.WARNING),
  UNKNOWN_ATTRIBUTE("%s: unknown attribute %s on %s", MessageLevel.ERROR),
  UNSAFE_ATTRIBUTE("%s: attribute %s is not allowed on tag %s",
                   MessageLevel.ERROR),
  FOLDING_ELEMENT("%s: folding element %s into parent", MessageLevel.WARNING),
  CANNOT_FOLD_ATTRIBUTE("%s: cannot fold attribute %s on %s into parent",
                        MessageLevel.ERROR),
  DISALLOWED_ATTRIBUTE_VALUE("%s: attribute %s cannot have value %s",
                             MessageLevel.ERROR),
  EXTRANEOUS_CONTENT("%s: unused content in tag %s", MessageLevel.FATAL_ERROR),
  UNKNOWN_TEMPLATE_PARAM(
      "%s: template %s defined at %s does not define a parameter %s",
      MessageLevel.FATAL_ERROR),
  MISSING_TEMPLATE_PARAM(
      "%s: template %s is missing parameter %s defined at %s",
      MessageLevel.FATAL_ERROR),
  BAD_IDENTIFIER("%s: bad identifier %s", MessageLevel.FATAL_ERROR),
  ATTRIBUTE_CANNOT_BE_DYNAMIC(
      "%s: tag %s cannot have dynamic attribute %s", MessageLevel.ERROR),
  DISALLOWED_URI("%s: url %s cannot be linked to", MessageLevel.FATAL_ERROR),
  MALFORMED_URL("%s: malformed url %s", MessageLevel.FATAL_ERROR),
  REWROTE_STYLE("%s: rewrote unsafe style attribute %s",
                MessageLevel.FATAL_ERROR),
  MALFORMED_CSS_PROPERTY_VALUE("%s: css property %s has bad value: %s",
                               MessageLevel.ERROR),
  CANT_CONVERT_TO_GXP(
      "%s: can't convert %s to a gxp", MessageLevel.FATAL_ERROR),
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
  SKIPPING_CSS_PROPERTY(
      "%s: skipping invalid css property %s", MessageLevel.WARNING),
  TAG_NOT_ALLOWED_IN_ATTRIBUTE(
      "%s: tags not allowed inside an attribute: %s", MessageLevel.ERROR),
  CSS_SUBSTITUTION_NOT_ALLOWED_HERE(
      "%s: css substitution not allowed for type %s", MessageLevel.FATAL_ERROR),
  FAILED_TO_LOAD_EXTERNAL_URL(
      "%s: failed to load external url %s", MessageLevel.WARNING),
  UNRECOGNIZED_CONTENT_TYPE(
      "%s: unrecognized content type %s for %s tag", MessageLevel.WARNING),
  UNRECOGNIZED_MEDIA_TYPE(
      "%s: unrecognized media type %s", MessageLevel.WARNING),
  QUOTED_CSS_VALUE("%s: quoted unquoted css value %s", MessageLevel.LINT),
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
