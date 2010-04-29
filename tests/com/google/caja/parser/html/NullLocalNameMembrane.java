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

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

class NullLocalNameMembrane extends DomMembrane {

  @Override
  <T extends NodeList> T makeListWrapper(T list, Class<T> type) {
    return type.cast(new NodeListWrapper(list, this));
  }

  @Override
  <T extends NamedNodeMap> T makeMapWrapper(T map, Class<T> type) {
    return type.cast(new NamedNodeMapWrapper(map, this));
  }

  @Override
  <T extends Node> T makeWrapper(T node, Class<T> type) {
    switch (node.getNodeType()) {
      case Node.ELEMENT_NODE:
        return type.cast(new ElementWrapper((Element) node, this) {
          @Override public String getLocalName() { return null; }
          @Override public String getNamespaceURI() { return null; }
        });
      case Node.ATTRIBUTE_NODE:
        return type.cast(new AttrWrapper((Attr) node, this) {
          @Override public String getLocalName() { return null; }
          @Override public String getNamespaceURI() { return null; }
        });
      default:
        return type.cast(new NodeWrapper(node, this));
    }
  }
}
