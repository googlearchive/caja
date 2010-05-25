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

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

class NodeListWrapper implements NodeList {
  protected final NodeList underlying;
  protected final DomMembrane membrane;
  NodeListWrapper(NodeList underlying, DomMembrane membrane) {
    this.underlying = underlying;
    this.membrane = membrane;
  }
  public int getLength() {
    return underlying.getLength();
  }
  public Node item(int arg0) {
    return membrane.wrap(underlying.item(arg0), Node.class);
  }
}
