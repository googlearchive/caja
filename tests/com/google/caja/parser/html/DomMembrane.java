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

import com.google.common.collect.Maps;

import java.util.Map;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

abstract class DomMembrane {
  // No need to make this weakly keyed since only used for short lived tests.
  private final Map<Object, Object> wrappers = Maps.newIdentityHashMap();

  <T extends Node> T wrap(T node, Class<T> type) {
    if (node == null) { return null; }
    Node wrapper = (Node) wrappers.get(node);
    if (wrapper == null) {
      wrappers.put(node, wrapper = makeWrapper(node, type));
      wrappers.put(wrapper, node);
    }
    return type.cast(wrapper);
  }

  <T extends NodeList> T wrap(T list, Class<T> type) {
    if (list == null) { return null; }
    NodeList wrapper = (NodeList) wrappers.get(list);
    if (wrapper == null) {
      wrappers.put(list, wrapper = makeListWrapper(list, type));
      wrappers.put(wrapper, list);
    }
    return type.cast(wrapper);
  }

  <T extends NamedNodeMap> T wrap(T map, Class<T> type) {
    if (map == null) { return null; }
    NamedNodeMap wrapper = (NamedNodeMap) wrappers.get(map);
    if (wrapper == null) {
      wrappers.put(map, wrapper = makeMapWrapper(map, type));
      wrappers.put(wrapper, map);
    }
    return type.cast(wrapper);
  }

  <T> T unwrap(T node, Class<T> type) {
    if (node == null) { return null; }
    Node wrapper = (Node) wrappers.get(node);
    return (wrapper == null) ? node : type.cast(wrapper);
  }

  abstract <T extends Node> T makeWrapper(T node, Class<T> type);

  abstract <T extends NodeList> T makeListWrapper(T list, Class<T> type);

  abstract <T extends NamedNodeMap> T makeMapWrapper(T map, Class<T> type);
}
