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
import java.util.Collections;

/**
 * An annotation that marks up a range of text, such as
 * <pre>{&#x40;code code here}</pre>
 *
 * @author mikesamuel@gmail.com
 */
public final class InlineAnnotation extends AbstractAnnotation {
  InlineAnnotation(String name, TextAnnotation value, FilePosition pos) {
    super(name, Collections.singletonList(value), pos);
    if (name == null) { throw new NullPointerException(); }
  }

  public void render(RenderContext r) {
    TokenConsumer tc = r.getOut();
    tc.mark(getFilePosition());
    tc.consume("{");
    tc.consume("@" + getValue());
    tc.consume(" ");
    for (Annotation child : children()) { child.render(r); }
    tc.consume("}");
  }
}
