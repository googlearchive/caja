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

import com.google.caja.parser.AncestorChain;
import com.google.caja.parser.MutableParseTreeNode;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.quasiliteral.DefaultCajaRewriter;
import com.google.caja.parser.quasiliteral.IllegalReferenceCheckRewriter;
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

  public boolean sanitize(AncestorChain<?> toSanitize) {
    MutableParseTreeNode input = (MutableParseTreeNode) toSanitize.node;
    ParseTreeNode result = new DefaultCajaRewriter(false)
        .expand(input, this.mq);
    result = new IllegalReferenceCheckRewriter(false)
        .expand(result, this.mq);

    MutableParseTreeNode.Mutation mut = input.createMutation();
    for (ParseTreeNode child : input.children()) {
      mut.removeChild(child);
    }
    for (ParseTreeNode child : result.children()) {
      mut.appendChild(child);
    }
    mut.execute();

    return true;
  }
}
