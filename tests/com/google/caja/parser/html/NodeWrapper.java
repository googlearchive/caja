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
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.UserDataHandler;

class NodeWrapper implements Node {
  protected final Node underlying;
  protected final DomMembrane membrane;

  NodeWrapper(Node underlying, DomMembrane membrane) {
    this.underlying = underlying;
    this.membrane = membrane;
  }

  public Node appendChild(Node arg0) throws DOMException {
    return membrane.wrap(
        underlying.appendChild(membrane.unwrap(arg0, Node.class)),
        Node.class);
  }

  public Node cloneNode(boolean arg0) {
    return membrane.wrap(underlying.cloneNode(arg0), Node.class);
  }

  public short compareDocumentPosition(Node arg0) throws DOMException {
    return underlying.compareDocumentPosition(membrane.unwrap(arg0, Node.class));
  }

  public NamedNodeMap getAttributes() {
    return membrane.wrap(underlying.getAttributes(), NamedNodeMap.class);
  }

  public String getBaseURI() {
    return underlying.getBaseURI();
  }

  public NodeList getChildNodes() {
    return membrane.wrap(underlying.getChildNodes(), NodeList.class);
  }

  public Object getFeature(String arg0, String arg1) {
    return underlying.getFeature(arg0, arg1);
  }

  public Node getFirstChild() {
    return membrane.wrap(underlying.getFirstChild(), Node.class);
  }

  public Node getLastChild() {
    return membrane.wrap(underlying.getLastChild(), Node.class);
  }

  public String getLocalName() {
    return underlying.getLocalName();
  }

  public String getNamespaceURI() {
    return underlying.getNamespaceURI();
  }

  public Node getNextSibling() {
    return membrane.wrap(underlying.getNextSibling(), Node.class);
  }

  public String getNodeName() {
    return underlying.getNodeName();
  }

  public short getNodeType() {
    return underlying.getNodeType();
  }

  public String getNodeValue() throws DOMException {
    return underlying.getNodeValue();
  }

  public Document getOwnerDocument() {
    return membrane.wrap(underlying.getOwnerDocument(), Document.class);
  }

  public Node getParentNode() {
    return membrane.wrap(underlying.getParentNode(), Node.class);
  }

  public String getPrefix() {
    return underlying.getPrefix();
  }

  public Node getPreviousSibling() {
    return membrane.wrap(underlying.getPreviousSibling(), Node.class);
  }

  public String getTextContent() throws DOMException {
    return underlying.getTextContent();
  }

  public Object getUserData(String arg0) {
    return underlying.getUserData(arg0);
  }

  public boolean hasAttributes() {
    return underlying.hasAttributes();
  }

  public boolean hasChildNodes() {
    return underlying.hasChildNodes();
  }

  public Node insertBefore(Node arg0, Node arg1) throws DOMException {
    return membrane.wrap(
        underlying.insertBefore(
            membrane.unwrap(arg0, Node.class),
            membrane.unwrap(arg1, Node.class)),
        Node.class);
  }

  public boolean isDefaultNamespace(String arg0) {
    return underlying.isDefaultNamespace(arg0);
  }

  public boolean isEqualNode(Node arg0) {
    return underlying.isEqualNode(membrane.unwrap(arg0, Node.class));
  }

  public boolean isSameNode(Node arg0) {
    return underlying.isSameNode(membrane.unwrap(arg0, Node.class));
  }

  public boolean isSupported(String arg0, String arg1) {
    return underlying.isSupported(arg0, arg1);
  }

  public String lookupNamespaceURI(String arg0) {
    return underlying.lookupNamespaceURI(arg0);
  }

  public String lookupPrefix(String arg0) {
    return underlying.lookupPrefix(arg0);
  }

  public void normalize() {
    underlying.normalize();
  }

  public Node removeChild(Node arg0) throws DOMException {
    return membrane.wrap(
        underlying.removeChild(membrane.unwrap(arg0, Node.class)),
        Node.class);
  }

  public Node replaceChild(Node arg0, Node arg1) throws DOMException {
    return membrane.wrap(
        underlying.replaceChild(
            membrane.unwrap(arg0, Node.class),
            membrane.unwrap(arg1, Node.class)),
        Node.class);
  }

  public void setNodeValue(String arg0) throws DOMException {
    underlying.setNodeValue(arg0);
  }

  public void setPrefix(String arg0) throws DOMException {
    underlying.setPrefix(arg0);
  }

  public void setTextContent(String arg0) throws DOMException {
    underlying.setTextContent(arg0);
  }

  public Object setUserData(String arg0, Object arg1, UserDataHandler arg2) {
    return underlying.setUserData(arg0, arg1, arg2);
  }
}
