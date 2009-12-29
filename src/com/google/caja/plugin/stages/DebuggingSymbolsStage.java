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
import com.google.caja.parser.ParseTreeNodeContainer;
import com.google.caja.parser.js.CajoledModule;
import com.google.caja.parser.js.StringLiteral;
import com.google.caja.parser.js.ObjectConstructor;
import com.google.caja.parser.quasiliteral.QuasiBuilder;
import com.google.caja.plugin.Job;
import com.google.caja.plugin.Jobs;
import com.google.caja.plugin.PluginMessageType;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.util.ContentType;
import com.google.caja.util.Pipeline;

import java.util.LinkedHashMap;
import java.util.ListIterator;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

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
        if (job.getType() != ContentType.JS
            // May occur if the cajita rewriter does not run due to errors.
            || !(job.getRoot().node instanceof CajoledModule)) {
          continue;
        }

        if (DEBUG) {
          System.err.println(
              "\n\nPre\n===\n"
              + (job.getRoot().cast(CajoledModule.class).node.toStringDeep(1))
              + "\n\n");
        }

        DebuggingSymbols symbols = new DebuggingSymbols();
        CajoledModule js = addSymbols(
            job.getRoot().cast(CajoledModule.class), symbols, mq);
        if (!symbols.isEmpty()) {
          if (DEBUG) {
            System.err.println("\n\nPost\n===\n" + js.toStringDeep() + "\n\n");
          }
          it.set(Job.cajoledJob(AncestorChain.instance(
              attachSymbols(symbols, js, mq))));
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
   * @return rewritten JS.
   */
  private CajoledModule addSymbols(
      AncestorChain<CajoledModule> js, DebuggingSymbols symbols,
      MessageQueue mq) {
    return (CajoledModule) new CajaRuntimeDebuggingRewriter(symbols, mq)
        .expand(js.node);
  }

  /**
   * Adds a call to ___.useDebugSymbols to a ___.loadModule call.
   */
  private CajoledModule attachSymbols(
      DebuggingSymbols symbols, CajoledModule js, MessageQueue mq) {
    Map<String, ParseTreeNode> envelopeBindings
        = new LinkedHashMap<String, ParseTreeNode>();

    if (!QuasiBuilder.match("({@keys*: @values*})",
            js.getModuleBody(), envelopeBindings)) {
      mq.addMessage(PluginMessageType.MALFORMED_ENVELOPE, js.getFilePosition());
      return js;
    }

    ParseTreeNode functionValue =
        getObjectLiteralValue(envelopeBindings, "instantiate");

    if (functionValue == null) {
      mq.addMessage(PluginMessageType.MALFORMED_ENVELOPE, js.getFilePosition());
      return js;
    }

    Map<String, ParseTreeNode> functionBindings
        = new LinkedHashMap<String, ParseTreeNode>();

    if (!QuasiBuilder.match("function (___, IMPORTS___) { @body* }",
            functionValue, functionBindings)) {
      mq.addMessage(PluginMessageType.MALFORMED_ENVELOPE, js.getFilePosition());
      return js;
    }

    functionValue = QuasiBuilder.substV(
        "  function (___, IMPORTS___) {"
        // Pass in varargs to avoid referencing the Array or Object symbol
        // before those are pulled from IMPORTS___ in @body.
        + "  ___.useDebugSymbols(@symbols*);"
        + "  @body*"
        + "}",
        "symbols", symbols.toJavascriptSideTable(),
        "body", functionBindings.get("body"));

    setObjectLiteralValue(envelopeBindings, "instantiate", functionValue);

    return new CajoledModule((ObjectConstructor) QuasiBuilder.substV(
        "({@keys*: @values*})",
        "keys", envelopeBindings.get("keys"),
        "values", envelopeBindings.get("values")));
  }


  // TODO(ihab.awad): http://code.google.com/p/google-caja/issues/detail?id=994
  private ParseTreeNode getObjectLiteralValue(
      Map<String, ParseTreeNode> keyValuePairs,
      String key) {
    List<? extends ParseTreeNode> keys =
        keyValuePairs.get("keys").children();
    List<? extends ParseTreeNode> values =
        keyValuePairs.get("values").children();

    for (int i = 0; i < keys.size(); i++) {
      String candidateKey = ((StringLiteral) keys.get(i)).getUnquotedValue();
      if (key.equals(candidateKey)) {
        return values.get(i);
      }
    }

    return null;
  }

  // TODO(ihab.awad): http://code.google.com/p/google-caja/issues/detail?id=994
  private void setObjectLiteralValue(
      Map<String, ParseTreeNode> keyValuePairs,
      String key,
      ParseTreeNode value) {
    List<? extends ParseTreeNode> keys =
        keyValuePairs.get("keys").children();
    List<ParseTreeNode> values =
        new ArrayList<ParseTreeNode>(keyValuePairs.get("values").children());

    for (int i = 0; i < keys.size(); i++) {
      String candidateKey = ((StringLiteral) keys.get(i)).getUnquotedValue();
      if (key.equals(candidateKey)) {
        values.set(i, value);
        keyValuePairs.put("values", new ParseTreeNodeContainer(values));
        return;
      }
    }
  }
}
