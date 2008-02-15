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

package com.google.caja.plugin.stages;

import com.google.caja.lexer.Keyword;
import com.google.caja.parser.AncestorChain;
import com.google.caja.parser.MutableParseTreeNode;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.Visitor;
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.Declaration;
import com.google.caja.parser.js.FunctionConstructor;
import com.google.caja.parser.js.FunctionDeclaration;
import com.google.caja.parser.js.Identifier;
import com.google.caja.parser.js.Operation;
import com.google.caja.parser.js.Operator;
import com.google.caja.parser.js.Reference;
import com.google.caja.plugin.Job;
import com.google.caja.plugin.Jobs;
import com.google.caja.plugin.PluginMeta;
import com.google.caja.plugin.SyntheticNodes;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.MessageType;
import com.google.caja.util.Pipeline;
import static com.google.caja.plugin.SyntheticNodes.s;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Replace references to globals with plugin accesses.
 * The handling of global declarations happens before consolidation
 * so that we know what is global.  This happens after consolidation
 * since it needs to affect references inside code compiled from html.
 * 
 * @author mikesamuel@gmail.com
 */
public final class RewriteGlobalReferencesStage
    implements Pipeline.Stage<Jobs> {
  public boolean apply(Jobs jobs) {

    for (Job job : jobs.getJobsByType(Job.JobType.JAVASCRIPT)) {
      new GlobalReferenceRewriter(jobs.getPluginMeta(), jobs.getMessageQueue())
          .rewrite(job.getRoot(), Collections.<String>emptySet());
    }

    return jobs.hasNoFatalErrors();
  }
}

final class GlobalReferenceRewriter {
  final PluginMeta meta;
  final MessageQueue mq;

  private static final Set<String> IMPLICIT_FUNCTION_DEFINITIONS =
      new HashSet<String>(Arrays.asList("arguments", Keyword.THIS.toString()));

  GlobalReferenceRewriter(PluginMeta meta, MessageQueue mq) {
    this.meta = meta;
    this.mq = mq;
  }

  void rewrite(AncestorChain<?> ancestors, final Set<? extends String> locals) {
    ancestors.node.acceptPreOrder(new Visitor() {
      public boolean visit(AncestorChain<?> ancestors) {
        ParseTreeNode node = ancestors.node;
        // if we see a function constructor, we need to compute a new set of
        // local declarations and recurse
        if (node instanceof FunctionConstructor) {
          FunctionConstructor c = (FunctionConstructor) node;
          Set<String> fnLocals = new HashSet<String>(locals);
          fnLocals.addAll(IMPLICIT_FUNCTION_DEFINITIONS);
          LocalDeclarationInspector insp =
              new LocalDeclarationInspector(fnLocals);
          for (ParseTreeNode child : c.children()) {
            child.acceptPreOrder(insp, ancestors);
          }
          rewrite(new AncestorChain<Block>(ancestors, c.getBody()), fnLocals);
          return false;
        }

        if (node instanceof Reference) {
          Reference ref = (Reference) node;
          String refName = ref.getIdentifierName();
          if (!node.getAttributes().is(SyntheticNodes.SYNTHETIC)) {
            if (refName.endsWith("__")) {
              mq.addMessage(MessageType.ILLEGAL_NAME, ref.getFilePosition(),
                            MessagePart.Factory.valueOf(refName));
            }
          }
          MutableParseTreeNode parent
              = (MutableParseTreeNode) ancestors.getParentNode();
          Operator parentOp = null;
          if (parent instanceof Operation) {
            parentOp = ((Operation) parent).getOperator();
          }
          // If node is part of a member access, and is not the leftmost
          // reference, then don't rewrite.  We don't want to rewrite the
          // b in a.b. Nor do we want to rewrite the b in "b in a".

          // We also don't want to rewrite synthetic nodes -- nodes created by
          // the PluginCompiler..
          boolean isFirstOperand = ancestors.isFirstSibling();
          if (!locals.contains(ref.getIdentifierName())
              && !ref.getAttributes().is(SyntheticNodes.SYNTHETIC)
              && !(!isFirstOperand && Operator.MEMBER_ACCESS == parentOp)
              && !(isFirstOperand && Operator.IN == parentOp)) {

            Reference placeholder = s(new Reference(s(new Identifier("_"))));
            Operation pluginReference = s(
                new Operation(
                    Operator.MEMBER_ACCESS,
                    s(new Reference(s(new Identifier(meta.namespaceName)))),
                    placeholder));
            parent.replaceChild(pluginReference, ref);
            pluginReference.replaceChild(ref, placeholder);
          }
        }
        return true;
      }
    }, ancestors.parent);
  }

  static final class LocalDeclarationInspector implements Visitor {
    final Set<String> locals;

    LocalDeclarationInspector(Set<String> locals) { this.locals = locals; }

    public boolean visit(AncestorChain<?> ancestors) {
      ParseTreeNode node = ancestors.node;
      if (node instanceof FunctionConstructor) { return false; }
      if (node instanceof Declaration
          && !(node instanceof FunctionDeclaration)) {
        locals.add(((Declaration) node).getIdentifierName());
      }
      return true;
    }
  }
}
