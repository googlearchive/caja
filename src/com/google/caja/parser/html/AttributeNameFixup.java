// Copyright (C) 2010 Google Inc.
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

package com.google.caja.parser.html;

/**
 * Maps XML qualified names like {@code prefix:localName} including ones in the
 * special {@code xmlns:namespaceName} namespace to valid XML local names
 * {@code f:fixupName}.
 * <p>
 * This lets us shuttle names through several stages:
 * <ol>
 * <li>From the {@link Html5ElementStack} where we know attribute names but not
 * where elements begin and end.
 * <p>In this stage, XML elements and attributes have a no namespace and a local
 * name containing a colon.
 * <li>Through the {@link CajaTreeBuilder} which builds a DOM that cannot
 * have attributes whose names start with {@code xmlns:}.  At this stage,
 * we do not have all the parent context to make namespace declarations, so
 * we shuttle names outside the default HTML namespace into non namespaced
 * elements.
 * <p>The Xerces DOM implementation can deal with {@code ':'}s in local names
 * for non-namespaced elements and attributes, but rejects any attributes
 * whose names start with {@code xmlns:}.
 * <p>In this stage, XML elements and attributes have a no namespace and a local
 * name containing a colon.
 * <li>To the {@link DomParser} which walks the resulting DOM to find elements
 * and attributes with namespace prefixes that need to be fixed and rewrites
 * them.
 * <p>After this stage finishes, all elements and attributes are properly
 * namespaced.
 * </ol>
 *
 * @see DomParser#fixup
 *
 * @author mikesamuel@gmail.com
 */
final class AttributeNameFixup {
  /** A prefix of all names of attributes that need to be fixed up. */
  static final String PREFIX = "f:";
  /** A prefix of all fixup names that are encoded namespace declarations. */
  static final String XMLNS_PREFIX = fixupNameFromQname("xmlns:");

  static String fixupNameFromQname(String qname) {
    int n = qname.length();
    StringBuilder adjName = new StringBuilder(n + 16);
    adjName.append(PREFIX);
    int pos = 0;
    for (int i = 0; i < n; ++i) {
      char ch = qname.charAt(i);
      switch (ch) {
        // '9' and ':' are adjacent, so this packs well.
        case '9':
          adjName.append(qname, pos, i).append("99");
          pos = i + 1;
          break;
        case ':':
          adjName.append(qname, pos, i).append("90");
          pos = i + 1;
          break;
      }
    }
    return adjName.append(qname, pos, n).toString();
  }

  static String qnameFromFixupName(String fixupName) {
    int pos = PREFIX.length();
    StringBuilder adjName = null;
    int n = fixupName.length();
    for (int i = pos; i < n; ++i) {
      char ch = fixupName.charAt(i);
      if (ch == '9') {
        if (adjName == null) { adjName = new StringBuilder(n); }
        adjName.append(fixupName, pos, i);
        adjName.append(fixupName.charAt(++i) == '9' ? '9' : ':');
        pos = i + 1;
      }
    }
    if (adjName == null) { return fixupName.substring(pos); }
    return adjName.append(fixupName, pos, n).toString();
  }
}
