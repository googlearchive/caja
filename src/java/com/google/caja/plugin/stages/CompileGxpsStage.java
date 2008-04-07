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

package com.google.caja.plugin.stages;

import com.google.caja.lang.css.CssSchema;
import com.google.caja.lang.html.HtmlSchema;
import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.HtmlLexer;
import com.google.caja.lexer.HtmlTokenType;
import com.google.caja.lexer.ParseException;
import com.google.caja.lexer.Token;
import com.google.caja.lexer.TokenQueue;
import com.google.caja.parser.AncestorChain;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.Visitor;
import com.google.caja.parser.html.DomParser;
import com.google.caja.parser.html.DomTree;
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.Declaration;
import com.google.caja.parser.js.ExpressionStmt;
import com.google.caja.parser.js.Expression;
import com.google.caja.parser.js.FunctionConstructor;
import com.google.caja.parser.js.FunctionDeclaration;
import com.google.caja.parser.js.Identifier;
import com.google.caja.parser.js.Operation;
import com.google.caja.parser.js.Operator;
import com.google.caja.parser.js.Reference;
import com.google.caja.parser.js.StringLiteral;
import com.google.caja.plugin.Job;
import com.google.caja.plugin.Jobs;
import com.google.caja.plugin.GxpCompiler;
import com.google.caja.plugin.GxpValidator;
import com.google.caja.plugin.PluginMessageType;
import com.google.caja.plugin.PluginMeta;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.util.Criterion;
import com.google.caja.util.Pipeline;
import static com.google.caja.plugin.SyntheticNodes.s;

import java.io.StringReader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Compiles GXPs to javascript functions.
 *
 * @author mikesamuel@gmail.com
 */
public final class CompileGxpsStage implements Pipeline.Stage<Jobs> {
  private final CssSchema cssSchema;
  private final HtmlSchema htmlSchema;

  public CompileGxpsStage(CssSchema cssSchema, HtmlSchema htmlSchema) {
    if (null == cssSchema) { throw new NullPointerException(); }
    if (null == htmlSchema) { throw new NullPointerException(); }
    this.cssSchema = cssSchema;
    this.htmlSchema = htmlSchema;
  }

  public boolean apply(Jobs jobs) {
    MessageQueue mq = jobs.getMessageQueue();
    PluginMeta meta = jobs.getPluginMeta();

    List<GxpJob> gxpJobs = new ArrayList<GxpJob>();
    for (Iterator<Job> jobIt = jobs.getJobs().iterator(); jobIt.hasNext();) {
      Job job = jobIt.next();
      switch (job.getType()) {
        case JAVASCRIPT:
          GxpCompileDirectiveReplacer r = new GxpCompileDirectiveReplacer(mq);
          job.getRoot().cast(ParseTreeNode.class).node.acceptPreOrder(r, job.getRoot().parent);
          gxpJobs.addAll(r.getDoms());
          break;
        case GXP:
          gxpJobs.add(new GxpJob((DomTree.Tag) job.getRoot().node, null));
          jobIt.remove();
          break;
      }
    }
    GxpCompiler gxpc = new GxpCompiler(cssSchema, htmlSchema, meta, mq);
    GxpValidator v = new GxpValidator(htmlSchema, mq);
    for (Iterator<GxpJob> it = gxpJobs.iterator(); it.hasNext();) {
      GxpJob job = it.next();
      if (!v.validate(new AncestorChain<DomTree>(job.docRoot))) {
        it.remove();
        continue;
      }
      try {
        job.sig = gxpc.compileTemplateSignature(job.docRoot);
      } catch (GxpCompiler.BadContentException ex) {
        ex.toMessageQueue(mq);
        it.remove();
      }
    }
    for (Iterator<GxpJob> it = gxpJobs.iterator(); it.hasNext();) {
      GxpJob job = it.next();
      try {
        job.compiled = gxpc.compileDocument(job.sig);
      } catch (GxpCompiler.BadContentException ex) {
        ex.printStackTrace();
        ex.toMessageQueue(mq);
        it.remove();
        continue;
      }

      FunctionConstructor templateFn = job.compiled;
      FunctionDeclaration template = new FunctionDeclaration(
          templateFn.getIdentifier(), templateFn);
      // The name used by <gxp:call> tags to reach the template.
      Declaration privateName = s(new Declaration(
          s(new Identifier(job.sig.getAssignedName())),
          new Reference(new Identifier(templateFn.getIdentifierName()))));
      Block templateDecls = new Block(Arrays.asList(template, privateName));
      jobs.getJobs().add(new Job(new AncestorChain<Block>(templateDecls)));
    }

    for (FunctionDeclaration handler : gxpc.getEventHandlers()) {
      jobs.getJobs().add(
          new Job(new AncestorChain<FunctionDeclaration>(handler)));
    }

    return jobs.hasNoFatalErrors();
  }
}

final class GxpCompileDirectiveReplacer implements Visitor {
  private final List<GxpJob> jobs = new ArrayList<GxpJob>();
  private final MessageQueue mq;

  GxpCompileDirectiveReplacer(MessageQueue mq) {
    this.mq = mq;
  }

  List<GxpJob> getDoms() { return jobs; }

  public boolean visit(AncestorChain<?> ancestors) {
    ParseTreeNode node = ancestors.node;
    if (!(node instanceof Operation)) { return true; }
    Operation op = (Operation) node;
    if (Operator.FUNCTION_CALL != op.getOperator()
        || 2 != op.children().size()) {
      return true;
    }
    Expression fn = op.children().get(0),
              arg = op.children().get(1);
    if (!(fn instanceof Reference
          && "compileGxp".equals(((Reference) fn).getIdentifierName()))) {
      return true;
    }
    ParseTreeNode parent = ancestors.getParentNode();
    if (!(parent instanceof ExpressionStmt)) { return true; }
    try {
      CharProducer cp = stringExpressionAsCharProducer(arg);
      HtmlLexer lexer = new HtmlLexer(cp);
      lexer.setTreatedAsXml(true);
      TokenQueue<HtmlTokenType> tq = new TokenQueue<HtmlTokenType>(
          lexer, node.getFilePosition().source(),
          Criterion.Factory.<Token<HtmlTokenType>>optimist());
      DomTree doc = new DomParser(tq, true, mq).parseDocument();
      if (!(doc instanceof DomTree.Tag)) {
        throw new ParseException(new Message(
            PluginMessageType.CANT_CONVERT_TO_GXP, arg.getFilePosition(), arg));
      }
      jobs.add(new GxpJob((DomTree.Tag) doc, ancestors.parent));
    } catch (ParseException ex) {
      ex.toMessageQueue(mq);
    }

    return false;
  }

  private static CharProducer stringExpressionAsCharProducer(Expression e)
      throws ParseException {
    List<CharProducer> chunks = new ArrayList<CharProducer>();
    stringExpressionAsCharProducer(e, chunks);
    return chunks.size() == 1
           ? chunks.get(0)
           : CharProducer.Factory.chain(chunks.toArray(new CharProducer[0]));
  }
  private static void stringExpressionAsCharProducer(
      Expression e, List<CharProducer> chunks) throws ParseException {
    if (e instanceof StringLiteral) {
      String literal = ((StringLiteral) e).getValue();
      int n = literal.length();
      if (n >= 2) {
        char ch0 = literal.charAt(0);
        if (('\'' == ch0 || '\"' == ch0) && literal.charAt(n - 1) == ch0) {
          literal = " " + literal.substring(1, n - 1) + " ";
        }
      }
      chunks.add(CharProducer.Factory.fromJsString(
          CharProducer.Factory.create(
              new StringReader(literal), e.getFilePosition())));
      return;
    } else if (e instanceof Operation) {
      Operation op = (Operation) e;
      if (Operator.ADDITION == op.getOperator()) {
        for (Expression operand : op.children()) {
          stringExpressionAsCharProducer(operand, chunks);
        }
        return;
      }
    }

    throw new ParseException(new Message(
        PluginMessageType.CANT_CONVERT_TO_GXP, e.getFilePosition(), e));
  }
}

final class GxpJob {
  final DomTree.Tag docRoot;
  final AncestorChain<?> toReplace;
  GxpCompiler.TemplateSignature sig;
  FunctionConstructor compiled;

  GxpJob(DomTree.Tag docRoot, AncestorChain<?> toReplace) {
    assert null != docRoot;
    this.docRoot = docRoot;
    this.toReplace = toReplace;
  }

  @Override
  public String toString() {
    return "[GxpJob " + (sig != null ? sig.getTemplateName() : "<uncompiled>")
        + "]";
  }
}
