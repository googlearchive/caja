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

/**
 * @see Renderable
 * @author mikesamuel@gmail.com
 */
public class RenderContext {

  /** Produce output that can be safely embedded. */
  private final boolean paranoid;
  /** Produce output that only contains lower 7-bit characters. */
  private final boolean asciiOnly;
  private final MessageContext msgContext;
  private final TokenConsumer out;

  public RenderContext(MessageContext msgContext, TokenConsumer out) {
    this(msgContext, false, false, out);
  }

  public RenderContext(
      MessageContext msgContext, boolean asciiOnly, boolean paranoid,
      TokenConsumer out) {
    if (null == msgContext || null == out) { throw new NullPointerException(); }
    this.msgContext = msgContext;
    this.paranoid = paranoid;
    this.asciiOnly = asciiOnly;
    this.out = out;
  }

  /**
   * True if the renderer produces output that can be embedded inside a CDATA
   * section, or {@code script} element without further escaping?
   */
  public final boolean isParanoid() { return paranoid; }
  /**
   * True if the renderer produces output that only contains characters in
   * {@code [\1-\x7f]}.
   */
  public final boolean isAsciiOnly() { return asciiOnly; }
  public final MessageContext getMessageContext() { return msgContext; }
  public final TokenConsumer getOut() { return out; }
}
