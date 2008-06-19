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

package com.google.caja.parser.html;

import com.google.caja.lexer.TokenConsumer;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.RenderContext;

/**
 * Can switch rendering of a DomTree between XML and HTML mode.
 * @author mikesamuel@gmail.com
 */
public class MarkupRenderContext extends RenderContext {
  private final boolean asXml;

  public MarkupRenderContext(MessageContext msgContext, TokenConsumer out,
                             boolean asXml) {
    super(msgContext, out);
    this.asXml = asXml;
  }

  public boolean asXml() { return asXml; }
}
