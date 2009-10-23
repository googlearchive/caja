// Copyright (C) 2008 Google Inc.
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

package com.google.caja.ancillary.jsdoc;

import com.google.caja.lexer.FilePosition;
import com.google.caja.reporting.RenderContext;
import java.util.List;


/**
 * An annotation that can only appear as a direct child of {@link Comment}.
 *
 * @author mikesamuel@gmail.com
 */
public final class BlockAnnotation extends AbstractAnnotation {
  BlockAnnotation(
      String name, List<? extends Annotation> children, FilePosition pos) {
    super(name, children, pos);
    if (name == null) { throw new NullPointerException(); }
  }

  public void render(RenderContext r) {
    r.getOut().mark(getFilePosition());
    r.getOut().consume("@" + getValue());
    r.getOut().consume(" ");
    for (Annotation a : children()) {
      a.render(r);
    }
  }
}
