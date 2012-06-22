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

package com.google.caja.util;

import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessagePart;
import java.io.IOException;

/**
 * A case insensitive identifier such as an element or identifier name that can
 * be used for equality checks, and as a key in Maps and Sets.
 *
 * <p>
 * This should be derived from a String as close to the parsing as possible so
 * that it's obvious what kind of case-sensitivity conventions are required for
 * the label.
 *
 * @author mikesamuel@gmail.com
 */
public final class Name implements Comparable<Name>, MessagePart {
  private final String canonicalForm;

  private Name(String canonicalForm) {
    this.canonicalForm = canonicalForm;
  }

  /**
   * The case-insensitive name of a CSS property, symbol, function or keyword.
   */
  public static Name css(String cssName) {
    return new Name(Strings.lower(cssName));
  }

  public String getCanonicalForm() { return canonicalForm; }

  @Override
  public boolean equals(Object o) {
    if (o == null || o.getClass() != Name.class) { return false; }
    return canonicalForm.equals(((Name) o).canonicalForm);
  }

  @Override
  public int hashCode() {
    return canonicalForm.hashCode();
  }

  @Override
  public String toString() {
    return canonicalForm;
  }

  public int compareTo(Name that) {
    return this.canonicalForm.compareTo(that.canonicalForm);
  }

  public void format(MessageContext context, Appendable out)
      throws IOException {
    out.append(canonicalForm);
  }
}
