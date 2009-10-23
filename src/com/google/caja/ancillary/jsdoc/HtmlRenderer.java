// Copyright (C) 2009 Google Inc.
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
import com.google.caja.lexer.ParseException;
import com.google.caja.parser.html.DomParser;
import com.google.caja.parser.html.HtmlQuasiBuilder;
import com.google.caja.parser.html.Nodes;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageType;
import com.google.caja.util.Executor;
import com.google.caja.util.Strings;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Writer;
import java.net.URI;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Node;

/**
 * Builds HTML files from a documentation digest.
 *
 * @author mikesamuel@gmail.com
 */
class HtmlRenderer {
  static void buildHtml(
      String json, FileSystem fs, File htmlDir,
      Iterable<CharProducer> sources, MessageContext mc)
      throws IOException, JsdocException {
    HtmlRenderer r = new HtmlRenderer(fs, htmlDir);
    r.copySupportingFiles();
    r.buildSourceFiles(sources, mc);
    r.buildFiles(json);
    r.dumpJson(json);
  }

  private final FileSystem fs;
  private final File htmlDir;
  private final String title;
  private final Document outDoc;
  private final HtmlQuasiBuilder b;

  private HtmlRenderer(FileSystem fs, File htmlDir) {
    this.fs = fs;
    this.htmlDir = htmlDir;
    this.title = "JSDoc";
    this.outDoc = DomParser.makeDocument(null, null);
    this.b = HtmlQuasiBuilder.getBuilder(outDoc);
  }

  private void copySupportingFiles() throws IOException {
    copyResourceTo("jsdoc.css", fs.join(htmlDir.getPath(), "jsdoc.css"));
    copyResourceTo("/js/prettify/prettify.js",
                   fs.join(htmlDir.getPath(), "prettify.js"));
    copyResourceTo("/js/prettify/prettify.css",
                   fs.join(htmlDir.getPath(), "prettify.css"));
    copyResourceTo("searchbox.js", fs.join(htmlDir.getPath(), "searchbox.js"));
  }

  /**
   * A facet to the file-system that the RhinoExecutor will allow rhino
   * to interact with.
   */
  public static final class FileSystemSandBoxSafe {
    private final FileSystem underlying;

    FileSystemSandBoxSafe(FileSystem fs) { this.underlying = fs; }

    public String basename(String path) { return underlying.basename(path);  }
    public String canonicalPath(String path) throws IOException {
      return underlying.canonicalPath(path);
    }
    public String dirname(String path) { return underlying.dirname(path); }
    public boolean exists(String path) { return underlying.exists(path); }
    public boolean isDirectory(String path) {
      return underlying.isDirectory(path);
    }
    public boolean isFile(String path) { return underlying.isFile(path); }
    public String join(String dir, String path) {
      return underlying.join(dir, path);
    }
    public void mkdir(String path) throws IOException {
      underlying.mkdir(path);
    }
    public PrintStream write(String path) throws IOException {
      return new PrintStream(underlying.writeBytes(path), false, "UTF-8");
    }
  }

  public static class HtmlBuilderSandBoxSafe {
    private final HtmlQuasiBuilder underlying;

    HtmlBuilderSandBoxSafe(HtmlQuasiBuilder underlying) {
      this.underlying = underlying;
    }

    public Node substV(String quasi, Object... params) {
      return underlying.substV(quasi, params);
    }

    public DocumentFragment toFragment(String html) throws ParseException {
      return underlying.toFragment(html);
    }

    public String toHtml(Node node) { return Nodes.render(node); }
  }

  private void buildFiles(String json) throws IOException, JsdocException {
    try {
      Map<String, Object> bindings = new HashMap<String, Object>();
      bindings.put("fileSystem", new FileSystemSandBoxSafe(fs));
      bindings.put("docTitle", title);
      bindings.put("htmlDir", htmlDir.getPath());
      bindings.put("htmlBuilder", new HtmlBuilderSandBoxSafe(b));
      bindings.put("stderr", System.err);

      Executor.Factory.createJsExecutor(
          new Executor.Input(getClass(), "jsdoc_html_formatter.js"),
          new Executor.Input(getClass(), "jsdoc_renderer.js"),
          new Executor.Input(
              "renderHtml(" + json + ");", getClass().getSimpleName()))
          .run(bindings, Object.class);
    } catch (Executor.MalformedSourceException ex) {
      throw new JsdocException(new Message(
          MessageType.INTERNAL_ERROR,
          MessagePart.Factory.valueOf("Script not parseable by Rhino")), ex);
    } catch (Executor.AbnormalExitException ex) {
      throw new JsdocException(new Message(
          JsdocMessageType.SCRIPT_FAILED_AT_RUNTIME,
          MessagePart.Factory.valueOf(ex.getScriptTrace())), ex);
    }
  }

  private void buildSourceFiles(
      Iterable<CharProducer> sources, MessageContext mc) throws IOException {
    for (CharProducer src : sources) {
      String relUri = JsdocRewriter.format(src.getSourceBreaks(0).source(), mc);
      String relPath = fs.join(
          fs.dirname(relUri), "src-" + fs.basename(relUri) + ".html");
      String absPath = fs.join(htmlDir.getPath(), relPath);
      fs.mkdir(fs.dirname(absPath));
      String rootDirPath = ".";
      {
        String dn = fs.dirname(relPath);
        if (dn != null) {
          rootDirPath = "..";
          while ((dn = fs.dirname(dn)) != null) {
            rootDirPath = fs.join(dn, "..");
          }
        }
      }
      Writer out = fs.write(absPath);
      try {
        buildSourceFile(relUri, rootDirPath, src, out);
      } finally {
        out.close();
      }
    }
  }

  private void buildSourceFile(
      String relUri, String rootDirPath, CharProducer src, Writer out)
      throws IOException {
    String sourceCode = String.valueOf(src.getBuffer(), 0, src.getLimit());
    int startLineNo = src.filePositionForOffsets(0, 0).startLineNo();
    String extension = fileExtension(relUri);
    String lang = "";
    if (extension != null) {
      lang = "lang-" + Strings.toLowerCase(extension);
    }
    out.write(Nodes.render(b.substV(
        ""
        + "<html>\n"
        + "  <head>\n"
        + "    <title>@uri  Source Code</title>\n"
        + "    <link type=text/css rel=stylesheet\n"
        + "     href='@root_dir/prettify.css'/>\n"
        + "    <script type=text/javascript\n"
        + "     src='@root_dir/prettify.js'></script>\n"
        + "    <style type=text/css>\n"
        + "body, .odd { background-color: #fff }\n"
        + ".even { background-color: #eee }\n"
        + "    </style>\n"
        + "  </head>\n"
        + "  <body onload='@onload'>\n"
        + "    <h1><tt>@uri</tt> Source Code</h1>\n"
        + "    <pre class=\"prettyprint @lang\">@source_code</pre>\n"
        + "  </body>\n"
        + "</html>",

        "uri", relUri,
        "root_dir", rootDirPath,
        "source_code", numberLines(sourceCode, startLineNo),
        "lang", lang,
        "onload", ((sourceCode.length() < (1 << 16)
                   ? "setTimeout(prettyPrint, 250)" : "")))));
  }

  private static final DecimalFormat LINE_NUMBER_FORMAT
      = new DecimalFormat("0000;-000");

  private DocumentFragment numberLines(String sourceCode, int lineNo) {
    DocumentFragment numberedLines = outDoc.createDocumentFragment();
    for (String line : sourceCode.split("\r\n?|\n")) {
      Node lineMarkup = b.substV(
          ""
          + "<span class='@class'>"
          + "<span class=nocode id='line@num'>@num: </span>@src</span>&#10;",

          "num", LINE_NUMBER_FORMAT.format(lineNo),
          "class", (lineNo & 1) == 0 ? "even" : "odd",
          "src", line);
      if (lineMarkup instanceof DocumentFragment) {
        for (Node child; (child = lineMarkup.getFirstChild()) != null;) {
          numberedLines.appendChild(child);
        }
      } else {
        numberedLines.appendChild(lineMarkup);
      }
      ++lineNo;
    }
    return numberedLines;
  }

  private void dumpJson(String json) throws IOException {
    Writer out = fs.write(fs.join(htmlDir.getPath(), "jsdoc.json"));
    try {
      out.write(json);
    } finally {
      out.close();
    }
  }

  private void copyResourceTo(String resource, String dest) throws IOException {
    InputStream in = getClass().getResourceAsStream(resource);
    if (in == null) { throw new FileNotFoundException(resource); }
    try {
      OutputStream out = fs.writeBytes(dest);
      try {
        byte[] buf = new byte[4096];
        for (int n; (n = in.read(buf)) > 0;) {
          out.write(buf, 0, n);
        }
      } finally {
        out.close();
      }
    } finally {
      in.close();
    }
  }

  private static String fileExtension(String uriPath) {
    URI uri = URI.create(uriPath);
    String path = uri.getPath();
    int dot = path.lastIndexOf('.');
    int slash = path.lastIndexOf('/');
    if (dot <= slash || dot + 1 == path.length()) { return null; }
    return path.substring(dot + 1);
  }
}
