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
import com.google.caja.parser.AncestorChain;
import com.google.caja.parser.html.DomTree;
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.Declaration;
import com.google.caja.parser.js.FunctionConstructor;
import com.google.caja.parser.js.FunctionDeclaration;
import com.google.caja.parser.js.Identifier;
import com.google.caja.parser.js.Statement;
import com.google.caja.plugin.Job;
import com.google.caja.plugin.Jobs;
import com.google.caja.plugin.GxpCompiler;
import com.google.caja.plugin.GxpValidator;
import com.google.caja.plugin.PluginMeta;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.util.Pair;
import com.google.caja.util.Pipeline;
import static com.google.caja.plugin.SyntheticNodes.s;

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

    List<GxpCompiler.TemplateSignature> sigs
        = new ArrayList<GxpCompiler.TemplateSignature>();

    GxpCompiler gxpc = new GxpCompiler(cssSchema, htmlSchema, meta, mq);
    GxpValidator v = new GxpValidator(htmlSchema, mq);

    for (Iterator<Job> jobIt = jobs.getJobs().iterator(); jobIt.hasNext();) {
      Job job = jobIt.next();
      if (job.getType() != Job.JobType.GXP) { continue; }

      AncestorChain<DomTree.Tag> docRoot
          = job.getRoot().cast(DomTree.Tag.class);
      if (v.validate(docRoot)) {
        try {
          sigs.add(gxpc.compileTemplateSignature(docRoot.node));
        } catch (GxpCompiler.BadContentException ex) {
          ex.toMessageQueue(mq);
        }
      }
      jobIt.remove();
    }

    // Add new jobs at the beginning since GXPs are hoistable declarations.
    int hoistedJobPos = 0;

    for (GxpCompiler.TemplateSignature sig : sigs) {
      Pair<FunctionConstructor, FunctionConstructor> fns;
      try {
        fns = gxpc.compileDocument(sig);
      } catch (GxpCompiler.BadContentException ex) {
        ex.toMessageQueue(mq);
        continue;
      }

      Block templateDecls = new Block(Arrays.asList(
          fnDeclaration(fns.a),
          // Use a var declaration so that the cajoler can cajole the function
          // without worrying about the fact that its name is in the privileged
          // namespace.
          varDeclaration(fns.b)));

      jobs.getJobs().add(hoistedJobPos++,
                         new Job(new AncestorChain<Block>(templateDecls)));
    }

    for (Statement handler : gxpc.getEventHandlers()) {
      jobs.getJobs().add(hoistedJobPos++,
                         new Job(new AncestorChain<Statement>(handler)));
    }

    return jobs.hasNoFatalErrors();
  }

  private static FunctionDeclaration fnDeclaration(FunctionConstructor fn) {
    FunctionDeclaration decl = new FunctionDeclaration(fn.getIdentifier(), fn);
    decl.setFilePosition(fn.getFilePosition());
    return decl;
  }

  private static Declaration varDeclaration(FunctionConstructor fn) {
    Identifier ident = fn.getIdentifier( );
    fn.replaceChild(new Identifier(null), ident);
    return s(new Declaration(ident, fn));
  }
}
