// Copyright (C) 2007 Google Inc.
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

import com.google.caja.parser.AbstractParseTreeNode;
import com.google.caja.parser.AncestorChain;
import com.google.caja.parser.quasiliteral.DefaultCajaRewriter;
import com.google.caja.reporting.MessageQueue;

/**
 * @author ihab.awad@gmail.com (Ihab Awad)
 */
public class ExpressionSanitizerCaja {
  final MessageQueue mq;
  final PluginMeta meta;

  public ExpressionSanitizerCaja(MessageQueue mq, PluginMeta meta) {
    this.mq = mq;
    this.meta = meta;
  }

  @SuppressWarnings("unchecked")
  public boolean sanitize(AncestorChain<?> toSanitize) {
    AbstractParseTreeNode<? extends AbstractParseTreeNode> input =
        (AbstractParseTreeNode<? extends AbstractParseTreeNode>)
        toSanitize.node;
    AbstractParseTreeNode<? extends AbstractParseTreeNode> result =
        (AbstractParseTreeNode<? extends AbstractParseTreeNode>)
        new DefaultCajaRewriter().expand(input, this.mq);

    for (AbstractParseTreeNode<?> child : input.children()) {
      input.removeChild(child);
    }

    for (AbstractParseTreeNode<?> child : result.children()) {
      input.appendChild(child);
    }

    return true;
  }
}
