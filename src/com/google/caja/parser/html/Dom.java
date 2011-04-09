// Copyright (C) 2009 Google Inc.
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
import com.google.caja.parser.AbstractParseTreeNode;
import com.google.caja.parser.html.Nodes;
import com.google.caja.parser.js.NoChildren;
import com.google.caja.render.Concatenator;
import com.google.caja.reporting.MarkupRenderMode;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.RenderContext;
import com.google.caja.util.Callback;

import java.io.IOException;
import java.io.Serializable;

import org.w3c.dom.Node;

/**
 * A parse tree wrapper for an org.w3c.DOM node.
 */
public final class Dom extends AbstractParseTreeNode implements Serializable {
  private static final long serialVersionUID = -5111504015682453850L;
  private final Node n;

  public Dom(Node n) {
    super(Nodes.getFilePositionFor(n), NoChildren.class);
    this.n = n;
  }

  @Override
  public Node getValue() { return n; }

  public TokenConsumer makeRenderer(
      Appendable out, Callback<IOException> handler) {
    return new Concatenator(out, handler);
  }

  public void render(RenderContext r) {
    Nodes.render(n, r);
  }

  @Override
  public void formatSelf(MessageContext context, int depth, Appendable out)
      throws IOException {
    out.append(this.getClass().getSimpleName()).append(" : ");
    String html = Nodes.render(n, MarkupRenderMode.XML)
        .replace("\\", "\\\\").replace("\n", "\\n").replace("\r", "\\r");
    if (html.length() > 40) {
      html = html.substring(0, 37) + "...";
    }
    out.append(html);
  }

  @Override public Dom clone() { return new Dom(n.cloneNode(true)); }
}
