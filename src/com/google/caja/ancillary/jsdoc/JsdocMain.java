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

package com.google.caja.ancillary.jsdoc;

import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.JsLexer;
import com.google.caja.lexer.JsTokenQueue;
import com.google.caja.lexer.ParseException;
import com.google.caja.lexer.TokenConsumer;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.js.ObjectConstructor;
import com.google.caja.parser.js.Parser;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.MessageType;
import com.google.caja.reporting.RenderContext;
import com.google.caja.reporting.SimpleMessageQueue;
import com.google.caja.tools.BuildCommand;
import com.google.caja.util.Callback;
import com.google.caja.util.Pair;
import com.google.caja.util.RhinoExecutor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Has a main method that given a set of files produces JSON documentation.
 *
 * @author mikesamuel@gmail.com
 */
public class JsdocMain {
  private final MessageContext mc = new MessageContext();
  private final MessageQueue mq = new SimpleMessageQueue();
  private final FileSystem fs;
  private final AnnotationHandlers handlers = new AnnotationHandlers(mc);
  private final Appendable errStream;
  private final Set<String> packages = new HashSet<String>();
  private final List<String> sourcePaths = new ArrayList<String>();
  private final List<CharProducer> sourceContent
       = new ArrayList<CharProducer>();
  private final List<ParseTreeNode> sources = new ArrayList<ParseTreeNode>();
  private final List<Pair<String, String>> initFiles
      = new ArrayList<Pair<String, String>>();

  JsdocMain(FileSystem fs, Appendable errStream) {
    this.fs = fs;
    this.errStream = errStream;
  }

  public static void main(String[] argv) {
    JsdocMain jsdm = new JsdocMain(new RealFileSystem(), System.err);

    int argi = 0;
    File docDir = null;
    for (; argi < argv.length; ++argi) {
      String flag = argv[argi];
      if (!flag.startsWith("--")) { break; }
      if ("--".equals(flag)) {
        ++argi;
        break;
      } else {
        int eq = flag.indexOf('=');
        String name, value;
        if (eq >= 0) {
          name = flag.substring(2, eq);
          value = flag.substring(eq + 1);
        } else {
          name = flag.substring(2);
          value = argv[++argi];
        }
        if ("doc_dir".equals(name)) {
          docDir = new File(value);
        } else {
          System.err.println("Unknown flag " + flag);
          System.exit(-1);
        }
      }
    }

    List<String> initPaths = new ArrayList<String>();
    List<String> sourcePaths = new ArrayList<String>();
    for (; argi < argv.length; ++argi) {
      if ("--init_file".equals(argv[argi])) {
        initPaths.add(argv[++argi]);
      } else {
        sourcePaths.add(argv[argi]);
      }
    }
    System.exit(jsdm.run(initPaths, sourcePaths, docDir,
                         docDir == null ? System.out : null)
                ? 0 : -1);
  }

  public static class Builder implements BuildCommand {
    public boolean build(List<File> inputs, List<File> deps, File output)
        throws IOException {
      List<String> initPaths = new ArrayList<String>();
      initPaths.add(resourceToPath("/js/jqueryjs/runtest/env.js"));
      initPaths.add(resourceToPath("jsdoc_init.js"));
      RhinoExecutor.enableContentUrls();  // content URLs used by jsdoc_init.js.

      List<String> files = pathList(inputs);
      files.addAll(initPaths);
      FileSystem fs = new RestrictedFileSystem(files, output);
      JsdocMain jsdm = new JsdocMain(fs, System.err);

      Writer out = null;
      File docDir = null;

      if (output.isDirectory()) {
        docDir = output;
      } else {
        out = new OutputStreamWriter(new FileOutputStream(output), "UTF-8");
      }

      Writer buf = new StringWriter();
      if (!jsdm.run(initPaths, pathList(inputs), docDir, buf)) { return false; }
      if (out != null) {
        try {
          out.write(buf.toString());
        } finally {
          out.close();
        }
      }
      return true;
    }

    private static String resourceToPath(String resource) throws IOException {
      try {
        return new File(Builder.class.getResource(resource).toURI()).toString();
      } catch (URISyntaxException ex) {
        FileNotFoundException fnf = new FileNotFoundException(resource);
        fnf.initCause(ex);
        throw fnf;
      }
    }
  }
  private static List<String> pathList(List<File> files) throws IOException {
    List<String> paths = new ArrayList<String>();
    for (File file : files) {
      paths.add(file.getCanonicalPath());
    }
    return paths;
  }

  boolean run(List<String> initPaths, List<String> srcPaths,
              File docDir, Appendable out) {
    String json;
    try {
      addInitPaths(initPaths);
      classifyFiles(srcPaths);
      if (mq.hasMessageAtLevel(MessageLevel.FATAL_ERROR)) { return false; }
      parseInputs();
      if (mq.hasMessageAtLevel(MessageLevel.FATAL_ERROR)) { return false; }
      try {
        Jsdoc jsd = new Jsdoc(handlers, mc, mq);
        for (Pair<String, String> initFile : initFiles) {
          jsd.addInitFile(initFile.a, initFile.b);
        }
        for (ParseTreeNode source : sources) { jsd.addSource(source); }
        for (Pair<InputSource, Comment> pkg : getPackageDocs()) {
          jsd.addPackage(pkg.a, pkg.b);
        }
        if (mq.hasMessageAtLevel(MessageLevel.FATAL_ERROR)) { return false; }
        ObjectConstructor docs = jsd.extract();
        StringBuilder jsonBuf = new StringBuilder();
        render(docs, jsonBuf);
        json = jsonBuf.toString();
      } catch (JsdocException ex) {
        ex.toMessageQueue(mq);
        return false;
      }

      if (mq.hasMessageAtLevel(MessageLevel.FATAL_ERROR)) { return false; }
      try {
        if (out != null) { out.append(json); }
        if (docDir != null) {
          HtmlRenderer.buildHtml(json, fs, docDir, sourceContent, mc);
        }
        return true;
      } catch (IOException ex) {
        mq.addMessage(
            MessageType.IO_ERROR, MessagePart.Factory.valueOf(ex.toString()));
        return false;
      } catch (JsdocException ex) {
        ex.toMessageQueue(mq);
        return false;
      }
    } finally {
      reportMessages();
    }
  }

  /**
   * Build the list of files that run before we take the first snapshot.
   * These JavaScript files set up the Rhino environment so that the code
   * to doc will run properly.
   */
  private void addInitPaths(List<String> paths) {
    for (String path : paths) {
      try {
        String f = fs.canonicalPath(path);
        if (!fs.exists(f)) {
          mq.addMessage(MessageType.NO_SUCH_FILE, fs.toInputSource(f));
          continue;
        }
        initFiles.add(Pair.pair(f, fs.read(f).toString()));
      } catch (IOException ex) {
        mq.addMessage(
            MessageType.IO_ERROR, MessagePart.Factory.valueOf(path));
      }
    }
  }

  /** Build the list of source files and packages from inputs. */
  private void classifyFiles(List<String> paths) {
    for (String path : paths) {
      try {
        String f = fs.canonicalPath(path);
        if (!fs.exists(f)) {
          mq.addMessage(MessageType.NO_SUCH_FILE, fs.toInputSource(f));
          continue;
        }
        if (fs.isFile(f)) {
          packages.add(fs.dirname(f));
          if (!"package.html".equals(fs.basename(f))) {
            sourcePaths.add(f);
          }
        } else if (fs.isDirectory(f)) {
          packages.add(f);
        }
      } catch (IOException ex) {
        mq.addMessage(
            MessageType.IO_ERROR, MessagePart.Factory.valueOf(path));
      }
    }
  }

  private CharProducer readSource(String path) throws IOException {
    CharProducer cp = fs.read(path);
    sourceContent.add(cp);
    mc.addInputSource(cp.getSourceBreaks(0).source());
    return cp;
  }

  /** Parse input source files. */
  private void parseInputs() {
    for (String path : sourcePaths) {
      try {
        CharProducer cp = readSource(path);
        InputSource is = cp.getSourceBreaks(0).source();
        JsLexer lexer = new JsLexer(cp, false);
        JsTokenQueue tq = new JsTokenQueue(lexer, is);
        Parser p = new Parser(tq, mq);
        sources.add(p.parse());
      } catch (IOException ex) {
        mq.addMessage(MessageType.IO_ERROR, MessagePart.Factory.valueOf(path));
      } catch (ParseException ex) {
        ex.toMessageQueue(mq);
      }
    }
  }

  /**
   * Store package documentation from <tt>package.html</tt> in the JSON output.
   */
  private List<Pair<InputSource, Comment>> getPackageDocs() {
    List<Pair<InputSource, Comment>> pkgs
        = new ArrayList<Pair<InputSource, Comment>>();
    for (String packagePath : packages) {
      try {
        String packageFile = fs.join(packagePath, "package.html");
        if (!fs.exists(packageFile)) { continue; }
        CharProducer cp = readSource(packageFile);
        try {
          Comment cmt = CommentParser.parseStructuredComment(cp);
          pkgs.add(Pair.pair(fs.toInputSource(packagePath), cmt));
        } catch (ParseException ex) {
          ex.toMessageQueue(mq);
          continue;
        }
      } catch (IOException ex) {
        mq.addMessage(MessageType.IO_ERROR, fs.toInputSource(packagePath));
      }
    }
    return pkgs;
  }

  private void reportMessages() {
    // Report the most serious problems first.
    MessageLevel maxMessageLevel = MessageLevel.SUMMARY;
    for (Message msg : mq.getMessages()) {
      MessageLevel ml = msg.getMessageLevel();
      if (ml.compareTo(maxMessageLevel) > 0) { maxMessageLevel = ml; }
    }
    for (Message msg : mq.getMessages()) {
      if (msg.getMessageLevel() != maxMessageLevel) { continue; }
      try {
        errStream.append(msg.getMessageLevel().name()).append(' ');
        msg.format(mc, errStream);
        errStream.append('\n');
      } catch (IOException ex) {
        // Can't recover from IOExceptions on errStream
        ex.printStackTrace();
      }
    }
  }

  private void render(final ParseTreeNode node, Appendable out) {
    TokenConsumer tc = node.makeRenderer(out, new Callback<IOException>() {
      public void handle(IOException ex) {
        mq.addMessage(MessageType.IO_ERROR, node.getFilePosition());
      }
    });
    node.render(new RenderContext(tc));
    tc.noMoreTokens();
  }

  static class RealFileSystem implements FileSystem {
    public String basename(String path) {
      return new File(path).getName();
    }

    public String canonicalPath(String path) throws IOException {
      return new File(path).getCanonicalPath();
    }

    public String dirname(String path) {
      return new File(path).getParent();
    }

    public boolean exists(String path) {
      return new File(path).exists();
    }

    public boolean isDirectory(String path) {
      return new File(path).isDirectory();
    }

    public boolean isFile(String path) {
      return new File(path).isFile();
    }

    public String join(String dir, String filename) {
      return "".equals(dir) ? filename : new File(dir, filename).getPath();
    }

    public CharProducer read(String path) throws IOException {
      return CharProducer.Factory.create(
          new InputStreamReader(new FileInputStream(path), "UTF-8"),
          toInputSource(path));
    }

    public InputSource toInputSource(String path) {
      return new InputSource(new File(path).toURI());
    }

    public Writer write(String path) throws IOException {
      return new OutputStreamWriter(new FileOutputStream(path), "UTF-8");
    }

    public OutputStream writeBytes(String path) throws IOException {
      return new FileOutputStream(path);
    }

    public void mkdir(String path) {
      (new File(path)).mkdir();
    }
  }

  static class RestrictedFileSystem implements FileSystem {
    private final Set<String> files = new HashSet<String>();
    private final File outputRoot;

    private RestrictedFileSystem(Collection<String> files, File outputRoot) {
      this.files.addAll(files);
      this.outputRoot = outputRoot;
    }

    public String basename(String path) {
      return new File(path).getName();
    }

    public String canonicalPath(String path) throws IOException {
      String canonPath = new File(path).getCanonicalPath();
      allowedInputFile(canonPath);
      return canonPath;
    }

    public String dirname(String path) {
      return new File(path).getParent();
    }

    public boolean exists(String path) {
      return files.contains(path) && new File(path).exists();
    }

    public boolean isDirectory(String path) {
      return files.contains(path) && new File(path).isDirectory();
    }

    public boolean isFile(String path) {
      return files.contains(path) && new File(path).isFile();
    }

    public String join(String dir, String filename) {
      return "".equals(dir) ? filename : new File(dir, filename).getPath();
    }

    public CharProducer read(String path) throws IOException {
      File f = allowedInputFile(path);
      return CharProducer.Factory.create(
          new InputStreamReader(new FileInputStream(f), "UTF-8"),
          toInputSource(path));
    }

    public InputSource toInputSource(String path) {
      return new InputSource(new File(path).toURI());
    }

    public Writer write(String path) throws IOException {
      return new OutputStreamWriter(
          new FileOutputStream(allowedOutputFile(path)), "UTF-8");
    }

    public OutputStream writeBytes(String path) throws IOException {
      return new FileOutputStream(allowedOutputFile(path));
    }

    public void mkdir(String path) throws IOException {
      allowedOutputFile(path).mkdirs();
    }

    private File allowedInputFile(String path) throws IOException {
      File f = new File(path);
      for (File anc = f; anc != null; anc = anc.getParentFile()) {
        if (files.contains(anc.getPath())) {
          return f;
        }
      }
      throw new FileNotFoundException(path);
    }

    private File allowedOutputFile(String path) throws IOException {
      File f = new File(path);
      for (File anc = f; anc != null; anc = anc.getParentFile()) {
        if (outputRoot.equals(anc)) { return f; }
      }
      throw new FileNotFoundException(path);
    }
  }
}
