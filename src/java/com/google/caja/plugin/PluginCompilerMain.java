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
import com.google.caja.lexer.CssLexer;
import com.google.caja.lexer.CssTokenType;
import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.HtmlLexer;
import com.google.caja.lexer.HtmlTokenType;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.JsLexer;
import com.google.caja.lexer.JsTokenQueue;
import com.google.caja.lexer.ParseException;
import com.google.caja.lexer.Token;
import com.google.caja.lexer.TokenQueue;
import com.google.caja.lexer.TokenQueue.Mark;
import com.google.caja.parser.AncestorChain;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.css.CssParser;
import com.google.caja.parser.css.CssTree;
import com.google.caja.parser.html.DomParser;
import com.google.caja.parser.js.Parser;
import com.google.caja.parser.js.Statement;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.RenderContext;
import com.google.caja.reporting.SimpleMessageQueue;
import com.google.caja.util.Criterion;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An executable that invokes the {@link PluginCompiler}.
 *
 * @author mikesamuel@gmail.com
 */
public class PluginCompilerMain {
  private String outPath = null;
  private String namespaceName = "PLUGIN";
  private String namespacePrefix = "plugin";
  private String pathPrefix = "/plugin";
  private MessageQueue mq = new SimpleMessageQueue();
  private MessageContext mc = new MessageContext();
  private PluginMeta meta;
  private PluginCompiler compiler;

  {
    mc.inputSources = new ArrayList<InputSource>();
  }

  private boolean run(String[] args) {
    try {
      args = parseFlags(args);

      meta = new PluginMeta(
          namespaceName, namespacePrefix, pathPrefix, "", PluginMeta.TranslationScheme.AAJA);
      compiler = new PluginCompiler(meta);

      boolean success = true;
      if (args.length == 0) {
        usage();
        System.err.println(
            "Please specify 1 or more input .js, .css, or .gxp files");
        success = false;
      }
      if (success) {
        success = parseInputs(args);
      }
      if (success) {
        success = compiler.run();
      }
      if (success) {
        try {
          dumpOutputs();
        } catch (IOException ex) {
          ex.printStackTrace();
          System.err.println("Failed to write outputs");
          success = false;
        }
      }
      return success;
    } finally {
      dumpMessages();
    }
  }

  private String[] parseFlags(String[] args) {
    // TODO(mikesamuel): replace with a CLI parsing library.
    int i = 0;
    while (i < args.length) {
      String arg = args[i];
      if (!arg.startsWith("--")) { break; }
      if ("--".equals(arg)) {
        ++i;
        break;
      }

      String name, value;
      int eq = arg.indexOf('=');
      if (eq >= 0) {
        name = arg.substring(0, eq);
        value = arg.substring(eq + 1);
        ++i;
      } else {
        name = arg;
        value = args.length > ++i ? args[i++] : "";
      }
      if ("--out".equals(name)) {
        this.outPath = value;
      } else if ("--name".equals(name)) {
        this.namespaceName = value;
      } else if ("--prefix".equals(name)) {
        this.namespacePrefix = value;
      } else if ("--pathPrefix".equals(name)) {
        while (value.endsWith("/") && value.length() > 1) {
          value = value.substring(0, value.length() - 1);
        }
        this.pathPrefix = value;
      } else {
        usage();
        System.exit(1);
      }
    }
    if (i > 0) {
      String[] argsLeft = new String[args.length - i];
      System.arraycopy(args, i, argsLeft, 0, argsLeft.length);
      args = argsLeft;
    }
    if (null == this.outPath) {
      usage();
      System.err.println("Please specify an output path prefix via --out");
      System.exit(1);
    }
    return args;
  }

  private void usage() {
    System.err.println(
        "Usage: --out=<dir> [--name=<jsIdent>]"
        + " [--prefix=<cssIdent>] [--pathPrefix=<uriPath>]");
    // TODO: flesh out
  }

  private boolean parseInputs(String[] inputs) {
    boolean parsePassed = true;
    for (String input : inputs) {
      try {
        ParseTreeNode parseTree = parseInput(input);
        if (null == parseTree) {
          parsePassed = false;
        } else {
          compiler.addInput(new AncestorChain<ParseTreeNode>(parseTree));
        }
      } catch (ParseException ex) {
        ex.toMessageQueue(mq);
      }
    }
    return parsePassed;
  }

  private static final Pattern ABS_URI_RE = Pattern.compile("^\\w+://");

  private ParseTreeNode parseInput(String inputPath)
      throws ParseException {
    InputSource is;
    if (ABS_URI_RE.matcher(inputPath).find()) {
      try {
        is = new InputSource(new URI(inputPath));
      } catch (URISyntaxException ex) {
        System.err.println("Bad input URI: " + inputPath);
        System.exit(-1);
        return null;
      }
    } else {
      is = new InputSource(new File(inputPath));
    }

    mc.inputSources.add(is);
    try {
      InputStream in = new URL(is.getUri().toString()).openStream();
      CharProducer cp = CharProducer.Factory.create(
          new InputStreamReader(in, "UTF-8"), is);
      try {
        return parseInput(is, cp, mq);
      } finally {
        cp.close();
      }
    } catch (IOException ex) {
      ex.printStackTrace();
      System.err.println("Failed to read from " + is.getUri());
      return null;
    }
  }

  static ParseTreeNode parseInput(
      InputSource is, CharProducer cp, MessageQueue mq)
      throws ParseException {

    String path = is.getUri().getPath();

    ParseTreeNode input;
    if (path.endsWith(".js")) {
      JsLexer lexer = new JsLexer(cp);
      JsTokenQueue tq = new JsTokenQueue(lexer, is);
      Parser p = new Parser(tq, mq);
      input = p.parse();
      tq.expectEmpty();
    } else if (path.endsWith(".gxp")) {
      HtmlLexer lexer = new HtmlLexer(cp);
      lexer.setTreatedAsXml(true);
      TokenQueue<HtmlTokenType> tq = new TokenQueue<HtmlTokenType>(
          lexer, is, Criterion.Factory.<Token<HtmlTokenType>>optimist());
      input = DomParser.parseDocument(tq);
      tq.expectEmpty();
    } else if (path.endsWith(".css")) {
      CssLexer lexer = new CssLexer(cp);
      TokenQueue<CssTokenType> tq = new TokenQueue<CssTokenType>(
          lexer, is, new Criterion<Token<CssTokenType>>() {
            public boolean accept(Token<CssTokenType> tok) {
              return tok.type != CssTokenType.COMMENT
              && tok.type != CssTokenType.SPACE;
            }
          });

      // if this is a template, then there will be a call like
      //   @template('myTemplateName');
      // followed by parameter declarations like
      //   @param('myParam');
      Mark m = tq.mark();
      CssTree.FunctionCall name = null;
      List<CssTree.FunctionCall> params = null;
      if (tq.checkToken("@template")) {
        lexer.allowSubstitutions(true);
        name = requireSingleStringLiteralCall(m, tq);

        params = new ArrayList<CssTree.FunctionCall>();
        while (!tq.isEmpty()) {
          m = tq.mark();
          if (!tq.checkToken("@param")) { break; }
          params.add(requireSingleStringLiteralCall(m, tq));
        }
      }

      CssParser p = new CssParser(tq);

      if (name != null) {
        CssTree.DeclarationGroup decs = p.parseDeclarationGroup();
        input = new CssTemplate(
            FilePosition.span(name.getFilePosition(), decs.getFilePosition()),
            name, params, decs);
      } else {
        input = p.parseStyleSheet();
      }
      tq.expectEmpty();
    } else {
      throw new AssertionError("Can't classify input " + is);
    }
    return input;
  }

  private static CssTree.FunctionCall requireSingleStringLiteralCall(
      Mark startMark, TokenQueue<CssTokenType> tq) throws ParseException {
    tq.expectToken("(");
    Token<CssTokenType> t = tq.expectTokenOfType(CssTokenType.STRING);
    tq.expectToken(")");
    tq.expectToken(";");

    Mark endMark = tq.mark();

    tq.rewind(startMark);
    String name = tq.peek().text;
    FilePosition start = tq.currentPosition();

    tq.rewind(endMark);

    FilePosition pos = FilePosition.span(start, tq.lastPosition());

    // The value must be a javascript identifier.
    // Do some simple sanity checks
    Matcher m = CSS_TEMPLATE_OR_PARAM_NAME.matcher(t.text);
    if (!m.matches()) {
      throw new ParseException(
          new Message(PluginMessageType.BAD_IDENTIFIER, t.pos,
                      MessagePart.Factory.valueOf(t.text)));
    }

    CssTree.StringLiteral literal =
      new CssTree.StringLiteral(t.pos, m.group(1));
    CssTree.Term term = new CssTree.Term(t.pos, null, literal);
    CssTree.Expr arg = new CssTree.Expr(t.pos, Collections.singletonList(term));

    return new CssTree.FunctionCall(pos, name, arg);
  }

  /** Valid name for a css template or one of its parameters. */
  private static final Pattern CSS_TEMPLATE_OR_PARAM_NAME =
    Pattern.compile("[\"\'](_?[a-z][a-z0-9_]*)[\"\']");

  private void dumpOutputs() throws IOException {
    File f = new File(this.outPath);
    File prefix = f.isDirectory() ? new File(f, "plugin") : f;
    File cssFile = new File(prefix + ".css");
    File jsFile = new File(prefix + ".js");
    Writer cssOut = null, jsOut = null;
    try {
      for (ParseTreeNode output : compiler.getOutputs()) {
        Writer out;
        if (output instanceof Statement) {
          if (null == jsOut) {
            jsOut = new OutputStreamWriter(
                new FileOutputStream(jsFile), "UTF-8");
          }
          out = jsOut;
        } else if (output instanceof CssTree) {
          if (null == cssOut) {
            cssOut = new OutputStreamWriter(
                new FileOutputStream(cssFile), "UTF-8");
          }
          out = cssOut;
        } else {
          throw new AssertionError(output.getClass().getName());
        }
        RenderContext rc = new RenderContext(mc, out, true);
        output.render(rc);
        rc.newLine();
      }
    } finally {
      try {
        if (null != cssOut) { cssOut.close(); }
      } catch (IOException ex) {
        ex.printStackTrace();
      }
      try {
        if (null != jsOut) { jsOut.close(); }
      } catch (IOException ex) {
        ex.printStackTrace();
      }
    }
  }

  private void dumpMessages() {
    try {
      for (Message m : mq.getMessages()) {
        m.format(mc, System.out);
      }
    } catch (IOException ex) {
      ex.printStackTrace();
    }
  }

  public static void main(String[] args) {
    int exitCode;
    try {
      PluginCompilerMain main = new PluginCompilerMain();
      boolean success = main.run(args);
      exitCode = success ? 0 : 1;
    } catch (Exception ex) {
      ex.printStackTrace();
      exitCode = -1;
    }
    try {
      System.exit(exitCode);
    } catch (SecurityException ex) {
      // This method may be invoked under a SecurityManager, e.g. by Ant,
      // so just suppress the security exception and return normally.
    }
  }
}
