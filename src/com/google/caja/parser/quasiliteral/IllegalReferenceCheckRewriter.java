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

package com.google.caja.parser.quasiliteral;

import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.js.Identifier;
import com.google.caja.parser.js.SyntheticNodes;
import com.google.caja.reporting.MessageQueue;

/**
 * Rejects any identifiers in the reserved namespace that are not
 * {@link com.google.caja.parser.AbstractParseTreeNode#isSynthetic synthetic}.
 *
 * @author ihab.awad@gmail.com (Ihab Awad)
 */
@RulesetDescription(
    name="IlegalReferenceCheckRewriter",
    synopsis="Checks that any reserved references are synthetic")
public class IllegalReferenceCheckRewriter extends Rewriter {
  final public Rule[] cajaRules = {
    new Rule () {
      @Override
      @RuleDescription(
          name="identifierUnderscores",
          synopsis="Check that any identifier ending with '__' is synthetic",
          reason="Double underscore identifiers may not be mentioned by Caja "
              + "code")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        if (node instanceof Identifier && !SyntheticNodes.is(node)) {
          String name = ((Identifier)node).getValue();
          if (name != null && name.endsWith("__")) {
            mq.addMessage(
                RewriterMessageType.ILLEGAL_IDENTIFIER_LEFT_OVER,
                node.getFilePosition(), node);
            return node;
          }
        }
        return NONE;
      }
    },

    new Rule () {
      @Override
      @RuleDescription(
          name="recurse",
          synopsis="Recurse into children",
          reason="")
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        return expandAll(node, scope);
      }
    },
  };

  public IllegalReferenceCheckRewriter(MessageQueue mq) {
    this(mq, true);
  }

  public IllegalReferenceCheckRewriter(MessageQueue mq, boolean logging) {
    super(mq, false, logging);
    addRules(cajaRules);
  }
}
