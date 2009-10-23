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
import com.google.caja.lexer.TokenConsumer;
import com.google.caja.reporting.RenderContext;
import java.util.List;

/**
 * A Javadoc style comment decomposed into text chunks and annotations.
 *
 * @author mikesamuel@gmail.com
 */
public final class Comment extends AbstractAnnotation {
  Comment(List<Annotation> children, FilePosition pos) {
    super(null, children, pos);
  }

  public void render(RenderContext r) {
    TokenConsumer tc = r.getOut();
    tc.mark(getFilePosition());
    tc.consume("/**");
    for (Annotation a : children()) { a.render(r); }
    tc.consume("*/");
  }
}
