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

import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.DirectivePrologue;
import com.google.caja.parser.js.TranslatedCode;
import com.google.caja.parser.js.UncajoledModule;
import com.google.caja.parser.quasiliteral.CajitaRewriter;
import com.google.caja.parser.quasiliteral.DefaultValijaRewriter;
import com.google.caja.parser.quasiliteral.IllegalReferenceCheckRewriter;
import com.google.caja.parser.quasiliteral.ModuleManager;
import com.google.caja.parser.quasiliteral.NonAsciiCheckVisitor;
import com.google.caja.parser.quasiliteral.Rewriter;
import com.google.caja.parser.quasiliteral.ES53Rewriter;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.MessageLevel;

import java.net.URI;

/**
 * @author ihab.awad@gmail.com (Ihab Awad)
 */
public class ExpressionSanitizerCaja {
  private final ModuleManager mgr;
  private final URI baseUri;

  public ExpressionSanitizerCaja(ModuleManager mgr, URI baseUri) {
    this.mgr = mgr;
    this.baseUri = baseUri;
  }

  public ParseTreeNode sanitize(ParseTreeNode input) {
    return sanitize(input, false);
  }

  public ParseTreeNode sanitize(ParseTreeNode input, boolean useSES) {
    MessageQueue mq = mgr.getMessageQueue();
    ParseTreeNode result = null;
    if (input instanceof UncajoledModule) {
      Block body = ((UncajoledModule) input).getModuleBody();
      if (body.children().size() == 2
          && body.children().get(0) instanceof DirectivePrologue
          && ((DirectivePrologue) body.children().get(0))
              .hasDirective("use strict")
          && body.children().get(1) instanceof TranslatedCode) {
        result = input;
      }
    }
    if (useSES) {
      result = newES53Rewriter(mq).expand(input);
    } else {
      if (result == null) {
        result = newValijaRewriter(mq).expand(input);
      }
      if (!mq.hasMessageAtLevel(MessageLevel.ERROR)) {
        result = newCajitaRewriter(mgr).expand(result);
      }
    }
    if (!mq.hasMessageAtLevel(MessageLevel.ERROR)) {
      result = new IllegalReferenceCheckRewriter(mq, false).expand(result);
      if (!mq.hasMessageAtLevel(MessageLevel.ERROR)) {
        result.acceptPreOrder(new NonAsciiCheckVisitor(mq), null);
      }
    }
    return result;
  }

  /** Visible for testing. */
  protected Rewriter newCajitaRewriter(ModuleManager mgr) {
    return new CajitaRewriter(baseUri, mgr, false);
  }

  protected Rewriter newES53Rewriter(MessageQueue mq) {
    return new ES53Rewriter(baseUri, mgr, false);
  }
  
  protected Rewriter newValijaRewriter(MessageQueue mq) {
    return new DefaultValijaRewriter(mq, false);
  }
}
