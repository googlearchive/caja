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

package com.google.caja.reporting;

import com.google.caja.lexer.TokenConsumer;
import com.google.caja.util.Callback;

import java.io.IOException;

/**
 * Something that can be rendered to a textual form.  E.g. an AST.
 *
 * @author mikesamuel@gmail.com
 */
public interface Renderable {

  /**
   * Output the textual form to 
   * {@link RenderContext#getOut <code>r.getOut()</code>}.
   */
  void render(RenderContext r);

  /**
   * Create a renderer capable of rendering this node.
   * @param out a channel to receive the rendered form.
   * @param handler receives IOExceptions thrown by out.  May be null if out
   *   will not throw an IOException on append.
   * @see com.google.caja.render
   */
  TokenConsumer makeRenderer(Appendable out, Callback<IOException> handler);
}
