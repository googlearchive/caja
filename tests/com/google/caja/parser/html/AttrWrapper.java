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
import org.w3c.dom.TypeInfo;

class AttrWrapper extends NodeWrapper implements Attr {
  private final Attr underlyingAttr;
  AttrWrapper(Attr underlyingAttr, DomMembrane membrane) {
    super(underlyingAttr, membrane);
    this.underlyingAttr = underlyingAttr;
  }
  public String getName() {
    return underlyingAttr.getName();
  }
  public Element getOwnerElement() {
    return membrane.wrap(underlyingAttr.getOwnerElement(), Element.class);
  }
  public TypeInfo getSchemaTypeInfo() {
    return underlyingAttr.getSchemaTypeInfo();
  }
  public boolean getSpecified() {
    return underlyingAttr.getSpecified();
  }
  public String getValue() {
    return underlyingAttr.getValue();
  }
  public boolean isId() {
    return underlyingAttr.isId();
  }
  public void setValue(String arg0) throws DOMException {
    underlyingAttr.setValue(arg0);
  }
}
