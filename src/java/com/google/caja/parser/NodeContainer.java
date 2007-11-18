// Copyright (C) 2007 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.caja.parser;

import java.io.IOException;
import java.util.Map;

import com.google.caja.reporting.RenderContext;

/**
 * A simple container for parse tree nodes.
 * 
 * @author ihab.awad@gmail.com
 */
public class NodeContainer
    extends AbstractParseTreeNode<AbstractParseTreeNode<?>> {
  public NodeContainer() {
    childrenChanged();
  }
 
  @Override
  public Object getValue() { return null; }

  public Map<String, ParseTreeNode> matchHere(ParseTreeNode specimen) {
    throw new UnsupportedOperationException();    
  }
  
  @Override
  public boolean shallowEquals(ParseTreeNode specimen) {
    // TODO(ihab): implement
    throw new UnsupportedOperationException();
  }

  public void render(RenderContext r) throws IOException {
    // TODO(ihab): implement
  }
}
