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

import com.google.caja.lexer.FilePosition;
import com.google.caja.parser.AncestorChain;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.js.Block;
import com.google.caja.parser.quasiliteral.QuasiBuilder;
import com.google.caja.plugin.Job;
import com.google.caja.plugin.Jobs;
import com.google.caja.plugin.PluginMessageType;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.util.Pipeline;

import java.util.LinkedHashMap;
import java.util.ListIterator;
import java.util.Map;

/**
 * Adds debugging symbols to cajoled code.  This looks for calls into the TCB
 * object, {@code ___}, and adds an optional parameter to each call which is an
 * index into a table of file positions.
 * <p>
 * This stage first walks over cajoled code looking for patterns like
 * {@code ___.readPub(obj, 'key')}, and rewrites them to include an index into a
 * table of debugging symbols: {@code ___.readPub(obj, 'key', 123)}.
 * <p>
 * It then rewrites the module envelope to make a symbol table to checks that
 * may fail at runtime:
 * <pre>
 * ___.loadModule(
 *     function (___, IMPORTS___) {
 *       <b>___.useDebugSymbols('foo.js:1+12-15',7,'2+4-18');</b>
 *       ...
 *     })
 * </pre>
 * The debugSymbols are a list of the form
 * <code>'[' &lt;{@link FilePosition}&gt; (',' &lt;prefixLength&gt; ','
 *           &lt;&Delta;{@link FilePosition}&gt;)* ']'</code>
 * where the &Delta;{@code FilePosition}s are turned into {@code FilePosition}s
 * by prepending them with the first prefixLength characters of the preceding
 * {@code FilePosition}.
 * <p>
 * See also <tt>cajita-debugmode.js</tt> for javascript which supports this
 * stage.
 *
 * @author mikesamuel@gmail.com
 */
public final class DebuggingSymbolsStage implements Pipeline.Stage<Jobs> {
  private static final boolean DEBUG = false;

  public boolean apply(Jobs jobs) {
    if (jobs.getPluginMeta().isDebugMode()) {
      MessageQueue mq = jobs.getMessageQueue();
      for (ListIterator<Job> it = jobs.getJobs().listIterator();
           it.hasNext();) {
        Job job = it.next();
        if (job.getType() != Job.JobType.JAVASCRIPT
            // May occur if the cajita rewriter does not run due to errors.
            || !(job.getRoot().node instanceof Block)) {
          continue;
        }

        if (DEBUG) {
          System.err.println(
              "\n\nPre\n===\n"
              + (job.getRoot().cast(Block.class).node.toStringDeep(1))
              + "\n\n");
        }

        DebuggingSymbols symbols = new DebuggingSymbols();
        Block js = addSymbols(job.getRoot().cast(Block.class), symbols, mq);
        if (!symbols.isEmpty()) {
          if (DEBUG) {
            System.err.println("\n\nPost\n===\n" + js.toStringDeep() + "\n\n");
          }
          it.set(
              new Job(AncestorChain.instance(attachSymbols(symbols, js, mq))));
        }
      }
    }
    return jobs.hasNoFatalErrors();
  }

  /**
   * Rewrites cajoled code to add position indices to caja operations.
   * @param js cajoled javascript.
   * @param symbols added to.
   * @param mq receives rewriting messages.
   * @return js rewritten.
   */
  private Block addSymbols(
      AncestorChain<Block> js, DebuggingSymbols symbols, MessageQueue mq) {
    return (Block) new CajaRuntimeDebuggingRewriter(symbols)
        .expand(js.node, mq);
  }

  /**
   * Adds a call to ___.useDebugSymbols to a ___.loadModule call.
   */
  private Block attachSymbols(
      DebuggingSymbols symbols, Block js, MessageQueue mq) {
    Map<String, ParseTreeNode> bindings
        = new LinkedHashMap<String, ParseTreeNode>();
    if (!QuasiBuilder.match(
            "{ ___.loadModule(function (___, IMPORTS___) { @body* }); }",
            js, bindings)) {
      mq.addMessage(PluginMessageType.MALFORMED_ENVELOPE, js.getFilePosition());
      return js;
    }
    return (Block) QuasiBuilder.substV(
        "{"
        + "___.loadModule("
        + "    function (___, IMPORTS___) {"
        // Pass in varargs to avoid referencing the Array or Object symbol
        // before those are pulled from IMPORTS___ in @body.
        + "      ___.useDebugSymbols(@symbols*);"
        + "      @body*"
        + "    });"
        + "}",

        "symbols", symbols.toJavascriptSideTable(),
        "body", bindings.get("body"));
  }
}
