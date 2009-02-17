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

package com.google.caja.parser.js;

import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.JsLexer;
import com.google.caja.lexer.JsTokenQueue;
import com.google.caja.lexer.ParseException;
import com.google.caja.render.JsMinimalPrinter;
import com.google.caja.reporting.EchoingMessageQueue;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.MessageType;
import com.google.caja.reporting.RenderContext;
import com.google.caja.util.Callback;
import com.google.caja.util.Pair;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * An executable that compresses the input javascript.
 * Usage
 * <pre>
 * java com.google.caja.parser.js.Minify file1.js file2.js ... > minified.js
 * </pre>
 *
 * <p>This parses and renders JS so guarantees valid output, but does not
 * otherwise change the structure.  The output is semantically the same even
 * in the presence of aliased eval.
 *
 * <p>It does strip comments and unnecessary whitespace.</p>
 *
 * <p>It does not rename local variables, inline constants or functions,
 * eliminate dead code, or zip content.</p>
 *
 * @author mikesamuel@gmail.com
 */
public class Minify {
  public static void main(String[] jsFilePaths) throws IOException {
    List<Pair<InputSource, File>> inputs = checkInputs(jsFilePaths);
    boolean passed = minify(inputs, new PrintWriter(System.out),
                            new PrintWriter(System.err));
    System.exit(passed ? 0 : -1);
  }

  /** Called before opening files to checks that all input are readable. */
  private static List<Pair<InputSource, File>> checkInputs(String... jsFiles)
      throws IOException {
    List<Pair<InputSource, File>> inputs
        = new ArrayList<Pair<InputSource, File>>();
    for (String jsFile : jsFiles) {
      File f = new File(jsFile);
      if (!f.canRead()) { throw new IOException("Cannot read " + jsFile); }
      InputSource is = new InputSource(f.getAbsoluteFile().toURI());
      inputs.add(Pair.pair(is, f));
    }
    return inputs;
  }

  public static boolean minify(Iterable<Pair<InputSource, File>> inputs,
                               Writer out, PrintWriter err)
      throws IOException {
    MessageContext mc = new MessageContext();
    for (Pair<InputSource, File> input : inputs) {
      mc.addInputSource(input.a);
    }
    final MessageQueue errs = new EchoingMessageQueue(
        err, mc, false);
    RenderContext rc = new RenderContext(
        mc,
        new JsMinimalPrinter(out, new Callback<IOException>() {
          public void handle(IOException ex) {
            errs.addMessage(
                MessageType.IO_ERROR,
                MessagePart.Factory.valueOf(ex.getMessage()));
          }
        }));

    for (Pair<InputSource, File> input : inputs) {
      CharProducer cp = CharProducer.Factory.create(
          new InputStreamReader(new FileInputStream(input.b), "UTF-8"),
          input.a);
      JsLexer lexer = new JsLexer(cp);
      JsTokenQueue tq = new JsTokenQueue(lexer, input.a);
      Parser p = new Parser(tq, errs);
      try {
        while (!tq.isEmpty()) {
          Block b = p.parse();
          for (Statement topLevelStmt : b.children()) {
            topLevelStmt.render(rc);
            if (!topLevelStmt.isTerminal()) { rc.getOut().consume(";"); }
          }
        }
      } catch (ParseException ex) {
        ex.toMessageQueue(errs);
      }
    }
    rc.getOut().noMoreTokens();
    out.flush();

    MessageLevel maxMessageLevel = MessageLevel.values()[0];
    for (Message msg : errs.getMessages()) {
      if (msg.getMessageLevel().compareTo(maxMessageLevel) >= 0) {
        maxMessageLevel = msg.getMessageLevel();
      }
    }
    return maxMessageLevel.compareTo(MessageLevel.ERROR) < 0;
  }
}
