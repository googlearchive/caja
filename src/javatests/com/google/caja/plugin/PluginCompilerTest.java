// Copyright (C) 2006 Google Inc.
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

import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.InputSource;
import com.google.caja.parser.AncestorChain;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.reporting.EchoingMessageQueue;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.RenderContext;
import com.google.caja.util.Pair;
import com.google.caja.util.TestUtil;

import java.io.BufferedReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

/**
 *
 * @author mikesamuel@gmail.com
 */
public class PluginCompilerTest extends TestCase {

  /** A simple end to end test. */
  public void testHelloWorld() throws Exception {
    runTest(
        "plugintest-helloworld.input",
        "plugintest-helloworld.golden",
        true);
  }

  /**
   * Test rewriting of references to make sure global/local references are
   * rewritten appropriately
   */
  public void testReferenceRewriting() throws Exception {
    runTest(
        "plugintest-references.input",
        "plugintest-references.golden",
        true);
  }

  /**
   * Test rewriting of references to make sure global/local references are
   * rewritten appropriately
   */
  public void testCssParsing() throws Exception {
    runTest(
        "plugintest-csstest.input",
        "plugintest-csstest.golden",
        true);
  }

  private void runTest(
      String inputFile, String goldenFile, boolean passes)
      throws Exception {
    String golden = TestUtil.readResource(getClass(), goldenFile);

    PluginMeta meta = new PluginMeta(
        "MY_TEST_PLUGIN","pre", "/plugin1", "rootDiv",
        PluginMeta.TranslationScheme.AAJA,
        PluginEnvironment.CLOSED_PLUGIN_ENVIRONMENT);

    MessageContext mc = new MessageContext();
    MessageQueue mq = new EchoingMessageQueue(
        new PrintWriter(new OutputStreamWriter(System.err)), mc);

    PluginCompiler pc = new PluginCompiler(meta, mq);

    List<Pair<InputSource, String>> parts = parseConsolidatedPlugin(inputFile);
    List<InputSource> srcs = new ArrayList<InputSource>();
    for (Pair<InputSource, String> part : parts) {
      InputSource is = part.a;
      String content = part.b;
      srcs.add(is);

      CharProducer cp = CharProducer.Factory.create(
          new StringReader(content), is);
      try {
        ParseTreeNode input = PluginCompilerMain.parseInput(is, cp, mq);
        pc.addInput(new AncestorChain<ParseTreeNode>(input));
      } finally {
        cp.close();
      }
    }

    boolean success = pc.run();
    if (!success && passes) {
      StringBuilder buf = new StringBuilder();
      for (Message msg : mq.getMessages()) {
        buf.append(msg.format(mc)).append('\n');
      }
      fail(buf.toString());
    }
    assertEquals(passes, success);

    StringBuilder buf = new StringBuilder();
    RenderContext rc = new RenderContext(mc, buf);
    for (ParseTreeNode input : pc.getOutputs()) {
      input.render(rc);
      rc.newLine();
      rc.newLine();
    }
    for (Message msg : mq.getMessages()) {
      if (msg.getMessageLevel().compareTo(MessageLevel.LINT) < 0) { continue; }
      rc.newLine();
      buf.append(msg.getMessageType().toString()).append(" : ")
          .append(msg.getMessageParts().get(0));
    }

    String actual = buf.toString();
    System.err.println("\n" + actual + "\n\n");
    assertEquals(actual, golden.trim(), actual.trim());
  }

  /**
   * Takes a test file which bundles a bunch of css, html, and javascript files
   * together and split it into separate parse trees.
   */
  private List<Pair<InputSource, String>>
      parseConsolidatedPlugin(String filename)
      throws Exception {
    List<Pair<InputSource, String>> parts =
      new ArrayList<Pair<InputSource, String>>();

    String content = TestUtil.readResource(getClass(), filename);
    BufferedReader in = new BufferedReader(new StringReader(content));
    InputSource currentSrc = null;
    StringBuilder current = null;
    for (String line; (line = in.readLine()) != null;) {
      if (line.startsWith("=== ")) {
        if (null != current) {
          parts.add(Pair.pair(currentSrc, current.toString()));
        }
        currentSrc = new InputSource(new URI(line.substring(4)));
        current = new StringBuilder();
      } else {
        current.append(line).append('\n');
      }
    }
    if (null != current) {
      parts.add(Pair.pair(currentSrc, current.toString()));
    }
    return parts;
  }
}
