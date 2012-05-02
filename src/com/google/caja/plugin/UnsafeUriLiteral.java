// Copyright (C) 2011 Google Inc.
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

package com.google.caja.plugin;

import com.google.caja.SomethingWidgyHappenedError;
import com.google.caja.lexer.FilePosition;
import com.google.caja.parser.css.CssTree;

import java.net.URI;

/**
 * A URI literal that has NOT been sanitized by a rewriting step such that it is
 * NOT safe to directly render in the output.
 */
class UnsafeUriLiteral extends CssTree.UriLiteral {
  UnsafeUriLiteral(FilePosition pos, URI value) { super(pos, value); }
  public void render(com.google.caja.reporting.RenderContext rc) {
    throw new SomethingWidgyHappenedError(
        "UnsafeUriLiteral must never be rendered");
  }
}