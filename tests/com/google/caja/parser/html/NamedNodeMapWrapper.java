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

import org.w3c.dom.DOMException;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

class NamedNodeMapWrapper implements NamedNodeMap {
  protected final NamedNodeMap underlying;
  protected final DomMembrane membrane;
  NamedNodeMapWrapper(NamedNodeMap underlying, DomMembrane membrane) {
    this.underlying = underlying;
    this.membrane = membrane;
  }
  public int getLength() {
    return underlying.getLength();
  }
  public Node getNamedItem(String arg0) {
    return membrane.wrap(underlying.getNamedItem(arg0), Node.class);
  }
  public Node getNamedItemNS(String arg0, String arg1) throws DOMException {
    return membrane.wrap(underlying.getNamedItemNS(arg0, arg1), Node.class);
  }
  public Node item(int arg0) {
    return membrane.wrap(underlying.item(arg0), Node.class);
  }
  public Node removeNamedItem(String arg0) throws DOMException {
    return membrane.wrap(underlying.removeNamedItem(arg0), Node.class);
  }
  public Node removeNamedItemNS(String arg0, String arg1) throws DOMException {
    return membrane.wrap(underlying.removeNamedItemNS(arg0, arg1), Node.class);
  }
  public Node setNamedItem(Node arg0) throws DOMException {
    return membrane.wrap(underlying.setNamedItem(
        membrane.unwrap(arg0, Node.class)),
        Node.class);
  }
  public Node setNamedItemNS(Node arg0) throws DOMException {
    return membrane.wrap(underlying.setNamedItemNS(
        membrane.unwrap(arg0, Node.class)),
        Node.class);
  }
}
