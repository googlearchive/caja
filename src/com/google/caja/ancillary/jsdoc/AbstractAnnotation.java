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
import com.google.caja.parser.AbstractParseTreeNode;
import com.google.caja.util.Callback;

import java.io.IOException;
import java.util.List;

/**
 * @author msamuel mikesamuel@gmail.com
 */
abstract class AbstractAnnotation extends AbstractParseTreeNode
    implements Annotation {
  private final String value;
  AbstractAnnotation(
      String value, List<? extends Annotation> children, FilePosition pos) {
    super(pos, Annotation.class);
    this.createMutation().appendChildren(children).execute();
    this.value = value;
  }
  @Override
  public List<? extends Annotation> children() {
    return childrenAs(Annotation.class);
  }
  @Override
  public final String getValue() { return value; }
  public final TokenConsumer makeRenderer(
      Appendable out, Callback<IOException> handler) {
    return new com.google.caja.render.Concatenator(out, handler);
  }
  @Override
  public String toString() {
    String className = getClass().getSimpleName();
    StringBuilder sb = new StringBuilder();
    sb.append('(').append(className.substring(className.indexOf('.') + 1));
    String value = getValue();
    if (value != null) { sb.append(" '").append(value).append('\''); }
    for (Annotation child : children()) {
      sb.append(' ').append(child);
    }
    return sb.append(')').toString();
  }
}
