// Copyright (C) 2005 Google Inc.
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
 * A group of declarations as in
 *   {@code var a, b, c;}
 * or
 *   {@code for (var a, b, c; ;)}
 *
 * @author mikesamuel@gmail.com
 */
public final class MultiDeclaration extends AbstractStatement<Declaration> {
  public MultiDeclaration(List<? extends Declaration> decls) {
    this.children.addAll(decls);
    childrenChanged();
  }

  @Override
  protected void childrenChanged() {
    super.childrenChanged();
    for (ParseTreeNode child : children) {
      if (!(child instanceof Declaration)) {
        throw new IllegalArgumentException("Child must be a declaration");
      }
    }
  }

  @Override
  public Object getValue() { return null; }

  public void render(RenderContext rc) throws IOException {
    rc.out.append("var ");
    rc.indent += 2;
    boolean seen = false;
    for (Declaration decl : children) {
      if (seen) {
        rc.out.append(", ");
      } else {
        seen = true;
      }
      decl.renderShort(rc);
    }
    rc.indent -= 2;
  }
}
