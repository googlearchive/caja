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
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.TypeInfo;

abstract class ElementWrapper extends NodeWrapper implements Element {
  private final Element underlyingEl;

  ElementWrapper(Element underlying, DomMembrane membrane) {
    super(underlying, membrane);
    this.underlyingEl = underlying;
  }

  public String getAttribute(String arg0) {
    return underlyingEl.getAttribute(arg0);
  }

  public String getAttributeNS(String arg0, String arg1) throws DOMException {
    return underlyingEl.getAttributeNS(arg0, arg1);
  }

  public Attr getAttributeNode(String arg0) {
    return membrane.wrap(underlyingEl.getAttributeNode(arg0), Attr.class);
  }

  public Attr getAttributeNodeNS(String arg0, String arg1) throws DOMException {
    return membrane.wrap(
        underlyingEl.getAttributeNodeNS(arg0, arg1), Attr.class);
  }

  public NodeList getElementsByTagName(String arg0) {
    return membrane.wrap(
        underlyingEl.getElementsByTagName(arg0), NodeList.class);
  }

  public NodeList getElementsByTagNameNS(String arg0, String arg1)
      throws DOMException {
    return membrane.wrap(
        underlyingEl.getElementsByTagNameNS(arg0, arg1), NodeList.class);
  }

  public TypeInfo getSchemaTypeInfo() {
    return underlyingEl.getSchemaTypeInfo();
  }

  public String getTagName() {
    return underlyingEl.getTagName();
  }

  public boolean hasAttribute(String arg0) {
    return underlyingEl.hasAttribute(arg0);
  }

  public boolean hasAttributeNS(String arg0, String arg1) throws DOMException {
    return underlyingEl.hasAttributeNS(arg0, arg1);
  }

  public void removeAttribute(String arg0) throws DOMException {
    underlyingEl.removeAttribute(arg0);
  }

  public void removeAttributeNS(String arg0, String arg1) throws DOMException {
    underlyingEl.removeAttributeNS(arg0, arg1);
  }

  public Attr removeAttributeNode(Attr arg0) throws DOMException {
    return membrane.wrap(
        underlyingEl.removeAttributeNode(membrane.unwrap(arg0, Attr.class)),
        Attr.class);
  }

  public void setAttribute(String arg0, String arg1) throws DOMException {
    underlyingEl.setAttribute(arg0, arg1);
  }

  public void setAttributeNS(String arg0, String arg1, String arg2)
      throws DOMException {
    underlyingEl.setAttributeNS(arg0, arg1, arg2);
  }

  public Attr setAttributeNode(Attr arg0) throws DOMException {
    return membrane.wrap(
        underlyingEl.setAttributeNode(membrane.unwrap(arg0, Attr.class)),
        Attr.class);
  }

  public Attr setAttributeNodeNS(Attr arg0) throws DOMException {
    return membrane.wrap(
        underlyingEl.setAttributeNodeNS(membrane.unwrap(arg0, Attr.class)),
        Attr.class);
  }

  public void setIdAttribute(String arg0, boolean arg1) throws DOMException {
    underlyingEl.setIdAttribute(arg0, arg1);
  }

  public void setIdAttributeNS(String arg0, String arg1, boolean arg2)
      throws DOMException {
    underlyingEl.setIdAttributeNS(arg0, arg1, arg2);
  }

  public void setIdAttributeNode(Attr arg0, boolean arg1) throws DOMException {
    underlyingEl.setIdAttributeNode(membrane.unwrap(arg0, Attr.class), arg1);
  }
}
