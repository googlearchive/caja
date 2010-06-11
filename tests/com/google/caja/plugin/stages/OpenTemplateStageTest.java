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
import com.google.caja.lexer.FilePosition;
import com.google.caja.parser.AncestorChain;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.ExpressionStmt;
import com.google.caja.parser.js.Statement;
import com.google.caja.plugin.Job;
import com.google.caja.plugin.Jobs;
import com.google.caja.plugin.PluginMeta;
import com.google.caja.util.CajaTestCase;
import com.google.caja.util.ContentType;
import com.google.caja.util.Pipeline;

import java.util.List;

/**
 * @author mikesamuel@gmail.com
 */
public final class OpenTemplateStageTest extends CajaTestCase {
  public final void testSimpleRewrite1() throws Exception {
    assertRewritten(
        "new StringInterpolation([ 'foo ', bar, ' baz' ])",
        "eval(Template('foo $bar baz'))",
        true);
  }

  public final void testSimpleRewrite2() throws Exception {
    assertRewritten(
        "new StringInterpolation([ 'foo', bar, 'baz' ])",
        "eval(Template('foo${bar}baz'))",
        true);
  }

  public final void testExpressionSubstitution() throws Exception {
    assertRewritten(
        "new StringInterpolation([ 'foo', bar() * 3, 'baz' ])",
        "eval(Template('foo${bar() * 3}baz'))",
        true);
  }

  public final void testMaskedTemplate() throws Exception {
    assertRewritten(
        "{\n"
        + "  eval(Template('foo${bar}baz'));\n"  // not rewritten
        + "  var Template;\n"
        + "}",

        "{\n"
        + "  eval(Template('foo${bar}baz'));\n"
        + "  var Template;\n"
        + "}",
        true);
  }

  public final void testMaskedEval() throws Exception {
    assertRewritten(
        "{\n"
        + "  function eval() {}\n"
        + "  eval(Template('foo${bar}baz'));\n"  // not rewritten
        + "}",

        "{\n"
        + "  function eval() {}\n"
        + "  eval(Template('foo${bar}baz'));\n"
        + "}",
        true);
  }

  private void assertRewritten(
      String golden, String input, final boolean passes)
      throws Exception {
    mq.getMessages().clear();

    CssSchema cssSchema = CssSchema.getDefaultCss21Schema(mq);
    HtmlSchema htmlSchema = HtmlSchema.getDefault(mq);

    Pipeline<Jobs> pipeline = new Pipeline<Jobs>() {
      @Override
      public boolean applyStage(Stage<? super Jobs> stage, Jobs jobs) {
        boolean result = super.applyStage(stage, jobs);
        return passes ? result : true;  // continue on failure
      }
    };
    pipeline.getStages().add(new OpenTemplateStage());
    pipeline.getStages().add(new ValidateCssStage(cssSchema, htmlSchema));
    pipeline.getStages().add(new Consolidator());

    Block node = js(fromString(input));
    PluginMeta meta = new PluginMeta();
    Jobs jobs = new Jobs(mc, mq, meta);
    jobs.getJobs().add(Job.jsJob(null, AncestorChain.instance(node), null));

    assertTrue(pipeline.apply(jobs));
    assertEquals(
        "" + jobs.getMessageQueue().getMessages(),
        passes, jobs.hasNoErrors());
    assertEquals("" + jobs.getJobs(), 1, jobs.getJobs().size());

    ParseTreeNode bare = stripBoilerPlate(
        jobs.getJobs().get(0).getRoot().cast(ParseTreeNode.class).node);
    assertEquals(golden, render(bare));
  }

  private static ParseTreeNode stripBoilerPlate(ParseTreeNode node) {
    while (node instanceof Block && node.children().size() == 1) {
      node = node.children().get(0);
    }
    if (node instanceof ExpressionStmt) { node = node.children().get(0); }
    return node;
  }

  private static class Consolidator implements Pipeline.Stage<Jobs> {
    public boolean apply(Jobs jobs) {
      List<Job> jsJobs = jobs.getJobsByType(ContentType.JS);
      if (jsJobs.size() == 1) { return true; }
      jobs.getJobs().remove(jsJobs);
      Block block = new Block(FilePosition.UNKNOWN);
      for (Job job : jsJobs) {
        Statement s = job.getRoot().cast(Statement.class).node;
        block.appendChild(s);
      }
      jobs.getJobs().add(Job.jsJob(null, AncestorChain.instance(block), null));
      return true;
    }
  }
}
