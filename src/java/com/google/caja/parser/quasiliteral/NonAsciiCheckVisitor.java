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

import com.google.caja.parser.AncestorChain;
import com.google.caja.parser.Visitor;
import com.google.caja.parser.js.Identifier;
import com.google.caja.plugin.SyntheticNodes;
import com.google.caja.reporting.MessageQueue;

/**
 * Non-ASCII characters can change the semantics of a program depending on how
 * they're rendered, so we forbid them outside of strings.
 *
 * @author metaweta@gmail.com (Mike Stay)
 */
public final class NonAsciiCheckVisitor implements Visitor {
  private final MessageQueue mq;

  public NonAsciiCheckVisitor(MessageQueue mq) {
    this.mq = mq;
  }

  /**
   * Add an error to the queue if an identifier contains non-ASCII characters.
   */
  public boolean visit(AncestorChain<?> ac) {
    if (ac.node instanceof Identifier && 
        !ac.node.getAttributes().is(SyntheticNodes.SYNTHETIC) &&
        !((Identifier)ac.node).getName().matches("^[a-zA-Z_$][a-zA-Z0-9_$]*$")) {
      mq.addMessage(
          RewriterMessageType.NONASCII_IDENTIFIER,
          ac.node.getFilePosition(), ac.node);
    }
    return true;
  }
}
