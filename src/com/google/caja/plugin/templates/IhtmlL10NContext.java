// Copyright (C) 2009 Google Inc.
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

package com.google.caja.plugin.templates;

import java.awt.ComponentOrientation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * The locale context in which a template is compiled.  Encapsulates the
 * message strings for a locale, and details of the character set, such as
 * whether it is right-to-left or left-to-right.
 *
 * @author mikesamuel@gmail.com
 */
public final class IhtmlL10NContext {
  private final Locale locale;
  private final Map<String, LocalizedHtml> messages;

  public IhtmlL10NContext(Locale locale, Map<String, LocalizedHtml> messages) {
    this.locale = locale;
    this.messages = new LinkedHashMap<String, LocalizedHtml>(messages);
  }

  public LocalizedHtml getMessageByName(String messageName) {
    return messages.get(messageName);
  }

  public boolean isLeftToRight() {
    return ComponentOrientation.getOrientation(locale).isLeftToRight();
  }

  public Iterable<String> getMessageNames() {
    return Collections.unmodifiableCollection(messages.keySet());
  }

  public Locale getLocale() {
    return locale;
  }
}
