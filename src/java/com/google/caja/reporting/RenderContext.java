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

import java.io.IOException;

/**
 * @see Renderable
 * @author mikesamuel@gmail.com
 */
public class RenderContext {
  // TODO(mikesamuel): make indent private and add setter&getter

  /** Amount of space to indent on {@link #newLine}. */
  public int indent;
  public final MessageContext msgContext;
  /** to which output is written. */
  public final Appendable out;
  /** Produce output that can be safely embedded. */
  public final boolean paranoid;

  public RenderContext(MessageContext msgContext, Appendable out) {
    this(msgContext, out, false);
  }

  public RenderContext(
      MessageContext msgContext, Appendable out, boolean paranoid) {
    if (null == msgContext || null == out) { throw new NullPointerException(); }
    this.msgContext = msgContext;
    this.out = out;
    this.paranoid = paranoid;
  }

  /** Write a new line and indent. */
  public void newLine() throws IOException {
    out.append("\n");
    indent(indent);
  }

  /** Write n spaces to {@link #out}. */
  public void indent(int n) throws IOException {
    if (n < 0) return;
    while (n >= SPACES.length()) {
      out.append(SPACES);
      n -= SPACES.length();
    }
    out.append(SPACES, 0, n);
  }

  private static final String SPACES = "                ";
}
