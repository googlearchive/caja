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

import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.ParseException;
import com.google.caja.parser.AncestorChain;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.html.DomParser;
import com.google.caja.parser.html.DomTree;
import com.google.caja.parser.html.OpenElementStack;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.MessageType;
import com.google.caja.reporting.RenderContext;
import com.google.caja.reporting.SimpleMessageQueue;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.InputStreamReader;
import java.io.Writer;

/**
 * Executable that invokes {@link HtmlPluginCompiler}.
 *
 * @author ihab.awad@gmail.com
 */
public final class HtmlPluginCompilerMain {
  final MessageQueue mq;

  private HtmlPluginCompilerMain() {
    mq = new SimpleMessageQueue();
  }

  public static void main(String[] argv) {
    System.exit(new HtmlPluginCompilerMain().run(argv));
  }

  private int run(String[] argv) {
    Config config = new Config(getClass(), System.err,
                               "Cajoles an HTML file to JS and CSS.");
    if (!config.processArguments(argv)) {
      return -1;
    }

    MessageLevel maxMessageLevel;
    MessageContext mc = null;
    try {
      HtmlPluginCompiler compiler = new HtmlPluginCompiler(
          mq, new PluginMeta(config.getCssPrefix()));
      mc = compiler.getMessageContext();
      compiler.setCssSchema(config.getCssSchema(mq));
      compiler.setHtmlSchema(config.getHtmlSchema(mq));
      try {
        for (File input : config.getInputFiles()) {
          compiler.addInput(
              new AncestorChain<DomTree.Fragment>(
                  parseHtmlFromFile(input, mq)));
        }

        if (!compiler.run()) {
          throw new RuntimeException();
        }
      } catch (ParseException e) {
        e.toMessageQueue(compiler.getMessageQueue());
        return -1;
      } catch (IOException e) {
        mq.addMessage(MessageType.IO_ERROR,
                      MessagePart.Factory.valueOf(e.toString()));
        return -1;
      }

      writeFile(config.getOutputJsFile(), compiler.getJavascript());
      writeFile(config.getOutputCssFile(), compiler.getCss());

    } finally {
      if (mc == null) { mc = new MessageContext(); }

      maxMessageLevel = dumpMessages(mq, mc, System.err);
    }

    return MessageLevel.ERROR.compareTo(maxMessageLevel) > 0 ? 0 : -1;
  }

  private DomTree.Fragment parseHtmlFromFile(File f, MessageQueue mq)
      throws IOException, ParseException {
    InputSource is = new InputSource(f.toURI());
    Reader in = new InputStreamReader(new FileInputStream(f), "UTF-8");
    try {
      return DomParser.parseFragment(
          DomParser.makeTokenQueue(is, in, false),
          OpenElementStack.Factory.createHtml5ElementStack(mq));
    } finally {
      in.close();
    }
  }

  private void writeFile(File path, ParseTreeNode contents) {
    Writer w;
    try {
      w = new BufferedWriter(new FileWriter(path, false));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    if (contents != null) {
      RenderContext rc = new RenderContext(new MessageContext(), w, true);
      try {
        contents.render(rc);
        w.write("\n");
      } catch (IOException e)  {
        throw new RuntimeException(e);
      }
    }

    try {
      w.flush();
      w.close();
    } catch (IOException e) {
      mq.addMessage(MessageType.IO_ERROR,
                    MessagePart.Factory.valueOf(e.toString()));
    }
  }

  /**
   * Dumps messages to the given output stream, returning the highest message
   * level seen.
   */
  static MessageLevel dumpMessages(
      MessageQueue mq, MessageContext mc, Appendable out) {
    MessageLevel maxLevel = MessageLevel.values()[0];
    for (Message m : mq.getMessages()) {
      MessageLevel level = m.getMessageLevel();
      if (maxLevel.compareTo(level) < 0) { maxLevel = level; }
    }
    MessageLevel ignoreLevel = null;
    if (maxLevel.compareTo(MessageLevel.LINT) < 0) {
      // If there's only checkpoints, be quiet.
      ignoreLevel = MessageLevel.LOG;
    }
    try {
      for (Message m : mq.getMessages()) {
        MessageLevel level = m.getMessageLevel();
        if (ignoreLevel != null && level.compareTo(ignoreLevel) <= 0) {
          continue;
        }
        out.append(level.name() + ": ");
        m.format(mc, out);
        out.append("\n");

        if (maxLevel.compareTo(level) < 0) { maxLevel = level; }
      }
    } catch (IOException ex) {
      ex.printStackTrace();
    }
    return maxLevel;
  }
}
