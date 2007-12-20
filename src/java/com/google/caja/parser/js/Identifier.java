// Copyright (C) 2007 Google Inc.
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

package com.google.caja.parser.js;

import com.google.caja.parser.ParseTreeNode;
import com.google.caja.reporting.RenderContext;

import java.io.IOException;
import java.util.List;

/**
 * An identifier used in JavaScript source.
 *
 * @author ihab.awad@gmail.com
 */
public final class Identifier extends AbstractExpression<ParseTreeNode> {
  private String name;

  public Identifier(String name, List<? extends ParseTreeNode> children) {
    this(name);
    assert(children.isEmpty());
  }

  public Identifier(String name) {
    this.name = name;
  }

  @Override
  public String getValue() {
    return name;
  }

  public String getName() { return name; }

  public void setName(String name) {
    // TODO(mikesamuel): check that name is a valid identifier
    this.name = name;
  }

  public void clearName() {
    name = null;
  }

  public void render(RenderContext r) throws IOException {
    r.out.append(name);
  }
}
