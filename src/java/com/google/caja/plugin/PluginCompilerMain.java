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
import com.google.caja.lexer.ExternalReference;
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
import com.google.caja.parser.js.Identifier;
import com.google.caja.parser.js.Parser;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.MessageType;
import com.google.caja.reporting.RenderContext;
import com.google.caja.reporting.SimpleMessageQueue;
import com.google.caja.util.Criterion;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An executable that invokes the {@link PluginCompiler}.
 *
 * @author mikesamuel@gmail.com
 */
public final class PluginCompilerMain {
  private final MessageQueue mq;
  private final MessageContext mc;

  private PluginCompilerMain() {
    mq = new SimpleMessageQueue();
    mc = new MessageContext();
    mc.inputSources = new ArrayList<InputSource>();
  }

  private int run(String[] argv) {
    Config config = new Config(
        getClass(), System.err, "Cajoles HTML, CSS, and JS files to JS & CSS.");
    if (!config.processArguments(argv)) {
      return -1;
    }

    boolean success = false;
    MessageContext mc = null;
    try {
      PluginMeta meta = new PluginMeta(
          config.getCssPrefix(), makeEnvironment(config));
      PluginCompiler compiler = new PluginCompiler(meta, mq);
      mc = compiler.getMessageContext();
      compiler.setCssSchema(config.getCssSchema(mq));
      compiler.setHtmlSchema(config.getHtmlSchema(mq));

      success = parseInputs(config.getInputUris(), compiler) && compiler.run();

      if (success) {
        writeFile(config.getOutputJsFile(), compiler.getJavascript());
        writeFile(config.getOutputCssFile(), compiler.getCss());
      }
    } finally {
      if (mc == null) { mc = new MessageContext(); }
      MessageLevel maxMessageLevel = dumpMessages(mq, mc, System.err);
      success &= MessageLevel.ERROR.compareTo(maxMessageLevel) > 0;
    }

    return success ? 0 : -1;
  }

  private boolean parseInputs(Collection<URI> inputs, PluginCompiler pluginc) {
    boolean parsePassed = true;
    for (URI input : inputs) {
      try {
        ParseTreeNode parseTree = parseInput(input);
        if (null == parseTree) {
          parsePassed = false;
        } else {
          pluginc.addInput(new AncestorChain<ParseTreeNode>(parseTree));
        }
      } catch (ParseException ex) {
        ex.toMessageQueue(mq);
        parsePassed = false;
      } catch (IOException ex) {
        mq.addMessage(MessageType.IO_ERROR,
                      MessagePart.Factory.valueOf(ex.toString()));
        parsePassed = false;
      }
    }
    return parsePassed;
  }

  /** Parse one input from a URI. */
  private ParseTreeNode parseInput(URI input)
      throws IOException, ParseException {
    InputSource is = new InputSource(input);
    mc.inputSources.add(is);

    // TODO(mikesamuel): capture the input as bytes, guess encoding, and
    // store in a map so we can generate message snippets and side-by-side
    // output.
    InputStream in = input.toURL().openStream();
    CharProducer cp = CharProducer.Factory.create(
        new InputStreamReader(in, "UTF-8"), is);
    try {
      return parseInput(is, cp, mq);
    } finally {
      cp.close();
    }
  }

  /** Classify an input by extension and use the appropriate parser. */
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
      TokenQueue<HtmlTokenType> tq = new TokenQueue<HtmlTokenType>(lexer, is);
      input = new DomParser(tq, true, mq).parseDocument();
      tq.expectEmpty();
    } else if (path.endsWith(".html") || path.endsWith(".xhtml")) {
      DomParser p = new DomParser(new HtmlLexer(cp), is, mq);
      input = p.parseFragment();
      p.getTokenQueue().expectEmpty();
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
      Identifier name = null;
      List<Identifier> params = null;
      if (tq.checkToken("@template")) {
        lexer.allowSubstitutions(true);
        name = requireSingleStringLiteralCall(m, tq);

        params = new ArrayList<Identifier>();
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

  /**
   * Look for a construct like @foo('bar'); in CSS which serves as a CSS
   * template directive.
   */
  private static Identifier requireSingleStringLiteralCall(
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

    Identifier ident = new Identifier(m.group(1));
    ident.setFilePosition(t.pos);

    return ident;
  }

  /** Valid name for a css template or one of its parameters. */
  private static final Pattern CSS_TEMPLATE_OR_PARAM_NAME =
      Pattern.compile("[\"\'](_?[a-z][a-z0-9_]*)[\"\']");

  /** Write the given parse tree to the given file. */
  private void writeFile(File f, ParseTreeNode output) {
    if (output == null) { return; }
    try {
      Writer out = new OutputStreamWriter(new FileOutputStream(f), "UTF-8");
      try {
        RenderContext rc = new RenderContext(mc, out, true);
        output.render(rc);
        rc.newLine();
      } finally {
        out.close();
      }
    } catch (IOException ex) {
      mq.addMessage(MessageType.IO_ERROR,
                    MessagePart.Factory.valueOf(ex.toString()));
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

  private PluginEnvironment makeEnvironment(Config config) {
    try {
      return new FileSystemEnvironment(
          new File(config.getInputUris().iterator().next()).getParentFile());
    } catch (IllegalArgumentException ex) {  // Not a file: URI
      return PluginEnvironment.CLOSED_PLUGIN_ENVIRONMENT;
    }
  }

  public static void main(String[] args) {
    int exitCode;
    try {
      PluginCompilerMain main = new PluginCompilerMain();
      exitCode = main.run(args);
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

final class FileSystemEnvironment implements PluginEnvironment {
 private final File directory;

 FileSystemEnvironment(File directory) {
   this.directory = directory;
 }
 
 public CharProducer loadExternalResource(
     ExternalReference ref, String mimeType) {
   File f = toFileUnderSameDirectory(ref.getUri());
   if (f == null) { return null; }
   try {
     return CharProducer.Factory.create(
         new InputStreamReader(new FileInputStream(f), "UTF-8"),
         new InputSource(f.toURI()));
   } catch (UnsupportedEncodingException ex) {
     throw new AssertionError(ex);
   } catch (FileNotFoundException ex) {
     return null;
   }
 }

 public String rewriteUri(ExternalReference ref, String mimeType) {
   File f = toFileUnderSameDirectory(ref.getUri());
   if (f == null) { return null; }
   return f.toURI().relativize(directory.toURI()).toString();
 }

 private File toFileUnderSameDirectory(URI uri) {
   if (!uri.isAbsolute()
       && !uri.isOpaque()
       && uri.getScheme() == null
       && uri.getAuthority() == null
       && uri.getFragment() == null
       && uri.getPath() != null
       && uri.getQuery() == null
       && uri.getFragment() == null) {
     File f = new File(new File(directory, ".").toURI().resolve(uri));
     // Check that f is a descendant of directory
     for (File tmp = f; tmp != null; tmp = tmp.getParentFile()) {
       if (directory.equals(tmp)) {
         return f;
       }
     }
   }
   return null;
 }
}
