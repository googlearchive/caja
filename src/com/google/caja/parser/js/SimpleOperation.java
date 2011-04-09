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

package com.google.caja.parser.js;

import com.google.caja.lexer.FilePosition;
import java.util.List;

/**
 * An Operation that simply evaluates all its children in order as rValues and
 * then returns a result computed from the resulting values.
 *
 * @author erights@gmail.com
 */
public final class SimpleOperation extends Operation {
  private static final long serialVersionUID = 5200781674229059403L;

  @ReflectiveCtor
  public SimpleOperation(
      FilePosition pos, Operator value, List<? extends Expression> children) {
    super(pos, value, children);
  }
}
