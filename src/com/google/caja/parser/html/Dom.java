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
import com.google.caja.parser.js.NoChildren;
import com.google.caja.render.Concatenator;
import com.google.caja.reporting.MarkupRenderMode;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.RenderContext;
import com.google.caja.util.Callback;

import java.io.IOException;

import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Node;

/**
 * A parse tree wrapper for an org.w3c.DOM tree. The root must be a
 * DocumentFragment.
 */
public final class Dom extends AbstractParseTreeNode {
  private static final long serialVersionUID = -5111504015682453850L;
  private final DocumentFragment fragment;

  public Dom(DocumentFragment fragment) {
    super(Nodes.getFilePositionFor(fragment), NoChildren.class);
    this.fragment = fragment;
  }

  /**
   * Create a Dom object containing a DocumentFragment containing the provided
   * node.
   *
   * If the provided node is a Document, transplant its root node. If the
   * provided node is a DocumentFragment, use it rather than creating a new
   * fragment.
   */
  public static Dom transplant(Node node) {
    if (node.getNodeType() == Node.DOCUMENT_NODE) {
      node = ((Document) node).getDocumentElement();
    } else if (node.getNodeType() == Node.DOCUMENT_FRAGMENT_NODE) {
      return new Dom((DocumentFragment) node);
    }
    DocumentFragment f = node.getOwnerDocument().createDocumentFragment();
    f.appendChild(node);
    Nodes.setFilePositionFor(f, Nodes.getFilePositionFor(node));
    return new Dom(f);
  }

  // Dom nodes can never be considered immutable since they point to an
  // org.w3c.dom.Node object, which is unavoidably mutable.
  @Override
  public boolean makeImmutable() { return false; }
  @Override
  public boolean isImmutable() { return false; }

  @Override
  public DocumentFragment getValue() { return fragment; }

  public TokenConsumer makeRenderer(
      Appendable out, Callback<IOException> handler) {
    return new Concatenator(out, handler);
  }

  public void render(RenderContext r) {
    Nodes.render(fragment, r);
  }

  @Override
  public void formatSelf(MessageContext context, int depth, Appendable out)
      throws IOException {
    out.append(this.getClass().getSimpleName()).append(" : ");
    String html = Nodes.render(fragment, MarkupRenderMode.XML)
        .replace("\\", "\\\\").replace("\n", "\\n").replace("\r", "\\r");
    out.append(html);
  }

  @Override
  public Dom clone() {
    return new Dom((DocumentFragment) fragment.cloneNode(true));
  }
}
