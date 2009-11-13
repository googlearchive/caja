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

package com.google.caja.parser.html;

import java.util.Collections;
import java.util.List;

import org.w3c.dom.Attr;
import org.xml.sax.Attributes;

/**
 * An implementation of org.xml.sax that wraps {@code DomTree.Attrib}s.
 * This ignores all namespacing since HTML doesn't do namespacing.
 */
final class AttributesImpl implements Attributes {
  private final List<Attr> attribs;

  static final AttributesImpl NONE = new AttributesImpl(
      Collections.<Attr>emptyList());

  AttributesImpl(List<Attr> attribs) { this.attribs = attribs; }

  public int getIndex(String qName) {
    int index = 0;
    for (Attr attrib : attribs) {
      if (attrib.getName().equals(qName)) { return index; }
      ++index;
    }
    return -1;
  }

  public int getIndex(String uri, String localName) {
    int index = 0;
    for (Attr attrib : attribs) {
      if ((uri == null
           ? attrib.getNamespaceURI() == null
           : uri.equals(attrib.getNamespaceURI()))
          && attrib.getLocalName().equals(localName)) {
        return index;
      }
      ++index;
    }
    return -1;
  }

  public int getLength() { return attribs.size(); }

  public String getLocalName(int index) {
    return attribs.get(index).getLocalName();
  }

  public String getQName(int index) { return attribs.get(index).getName(); }

  public String getType(int index) { return null; }

  public String getType(String qName) { return null; }

  public String getType(String uri, String localName) { return null; }

  public String getURI(int index) {
    return attribs.get(index).getNamespaceURI();
  }

  public String getValue(int index) {
    return attribs.get(index).getValue();
  }

  public String getValue(String qName) {
    int index = getIndex(qName);
    return index < 0 ? null : getValue(index);
  }

  public String getValue(String uri, String localName) {
    int index = getIndex(uri, localName);
    return index < 0 ? null : getValue(index);
  }

  public List<Attr> getAttributes() {
    return Collections.unmodifiableList(attribs);
  }
}
