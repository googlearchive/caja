// Copyright (C) 2011 Google Inc.
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

package com.google.caja.precajole;

import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.FilePosition;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.ParserContext;
import com.google.caja.parser.js.CajoledModule;
import com.google.caja.parser.js.StringLiteral;
import com.google.caja.parser.js.UncajoledModule;
import com.google.caja.parser.quasiliteral.ModuleManager;
import com.google.caja.parser.quasiliteral.opt.ArrayIndexOptimization;
import com.google.caja.plugin.ExpressionSanitizerCaja;
import com.google.caja.plugin.PluginMeta;
import com.google.caja.plugin.UriFetcher;
import com.google.caja.plugin.templates.QuasiUtil;
import com.google.caja.reporting.BuildInfo;
import com.google.caja.reporting.EchoingMessageQueue;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.util.ContentType;
import com.google.caja.util.Strings;
import com.google.common.base.Charsets;
import com.google.common.io.Files;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

public class PrecajoleTask extends Task {

  private String spec;
  private String dest;
  private final List<String> deps = new ArrayList<String>();
  private StaticPrecajoleMap map;

  public void setSpec(String value) { spec = value; }

  public void setDest(String value) { dest = value; }

  public void setDepend(String value) {
    deps.add(value);
  }

  @Override
  public void execute() {
    if (spec == null) {
      throw new BuildException("missing spec= attribute");
    }
    if (dest == null) {
      throw new BuildException("missing dest= attribute");
    }

    long now = System.currentTimeMillis();

    map = new StaticPrecajoleMap(dest);
    outdatedCheck();

    try {
      new SpecHandler(spec).process();
      map.setModTime(now);
      map.finish();
    } catch (Exception e) {
      throw new BuildException(e);
    }
  }

  // If any dependency files are newer than map, force rebuild all
  private void outdatedCheck() {
    long mt = map.getModTime();
    if (mt <= new File(spec).lastModified()) {
      map.setModTime(0);
      return;
    }
    for (String dep : deps) {
      if (map.getModTime() <= new File(dep).lastModified()) {
        map.setModTime(0);
        return;
      }
    }
  }

  private class SpecHandler extends DefaultHandler {
    private final File specFile;
    private final File specDir;

    private Locator here = null;
    private String sourceName = null;
    private String sourceText = null;
    private String cdata = "";
    private boolean upToDate = false;
    private List<String> uris = null;

    public SpecHandler(String filename) {
      specFile = new File(filename);
      specDir = specFile.getParentFile();
    }

    public void process()
        throws SAXException, FileNotFoundException, IOException {
      XMLReader sax = XMLReaderFactory.createXMLReader();
      sax.setContentHandler(this);
      sax.parse(new InputSource(new FileInputStream(specFile)));
    }

    @Override
    public void setDocumentLocator(Locator here) {
      this.here = here;
    }

    @Override
    public void startElement(
        String uri, String name, String qName, Attributes attrs)
    throws SAXException
    {
      if ("precajole-spec".equals(name)) {
        // ignore
      } else if ("precajole".equals(name)) {
        startPrecajole(attrs);
      } else if ("uri".equals(name)) {
        startUri();
      } else {
        throw new SAXParseException("Unknown tag " + name, here);
      }
    }

    @Override
    public void endElement(String uri, String name, String qName)
    throws SAXParseException {
      if ("precajole-spec".equals(name)) {
        // ignore
      } else if ("precajole".equals(name)) {
        endPrecajole();
      } else if ("uri".equals(name)) {
        endUri();
      } else {
        throw new SAXParseException("Unknown tag " + name, here);
      }
    }

    @Override
    public void characters(char[] ch, int start, int len) {
      cdata += new String(ch, start, len);
    }

    private void startPrecajole(Attributes attrs) throws SAXException {
      if (attrs != null) {
        sourceName = attrs.getValue("file");
        if (sourceName != null) {
          File source = new File(specDir, sourceName);
          upToDate = source.exists()
              && source.lastModified() < map.getModTime();
          if (!upToDate) {
            sourceText = readFile(source);
            uris = new ArrayList<String>();
          }
          return;
        }
      }
      throw new SAXParseException("missing file= attribute", here);
    }

    private void startUri() {
      cdata = "";
    }

    private void endUri() throws SAXParseException {
      if (cdata != null) {
        cdata = cdata.trim();
        if (!cdata.isEmpty()) {
          if (!upToDate) {
            uris.add(cdata);
            if (Strings.lower(cdata).startsWith("http:")) {
              uris.add("https:" + cdata.substring("http:".length()));
            }
          }
          return;
        }
      }
      throw new SAXParseException("<uri> has no content", here);
    }

    private void endPrecajole() {
      if (!upToDate) {
        CajoledModule cajoled = cajole(sourceText, sourceName);
        map.put(uris.toArray(new String[0]), sourceText, cajoled);
      }
    }

    private String readFile(File f) throws SAXException {
      try {
        return Files.toString(f, Charsets.UTF_8);
      } catch (IOException e) {
        throw new SAXException(e);
      }
    }

    private CajoledModule cajole(String text, String name) {
      MessageQueue mq = new EchoingMessageQueue(
          new PrintWriter(System.err), new MessageContext(), false);
      PluginMeta pm = new PluginMeta();
      pm.setPrecajoleMap(null);
      ModuleManager mgr = new ModuleManager(
          pm, BuildInfo.getInstance(),
          UriFetcher.NULL_NETWORK, mq);
      UncajoledModule input = uncajoled(text, name, mq);

      // TODO(felix8a): maybe should use compilation pipeline
      ArrayIndexOptimization.optimize(input);
      ParseTreeNode result = new ExpressionSanitizerCaja(mgr, null)
          .sanitize(input);

      if (mq.hasMessageAtLevel(MessageLevel.ERROR)) {
        throw new BuildException("Failed to cajole " + name);
      }
      if (!(result instanceof CajoledModule)) {
        throw new BuildException("No CajoledModule for " + name);
      }
      return (CajoledModule) result;
    }

    private UncajoledModule uncajoled(
        String text, String name, MessageQueue mq)
    {
      String imaginaryUri = "precajole:///" + name;
      try {
        ParseTreeNode node = new ParserContext(mq)
            .withInput(ContentType.JS)
            .withInput(CharProducer.Factory.create(
                new StringReader(text),
                new com.google.caja.lexer.InputSource(new URI(imaginaryUri))))
            .build();
        // TODO(felix8a): duplicated from SafeHtmlMaker
        node = QuasiUtil.quasiStmt(
            ""
            + "try {"
            + "  @scriptBody;"
            + "} catch (ex___) {"
            + "  ___./*@synthetic*/ getNewModuleHandler()"
            + "      ./*@synthetic*/ handleUncaughtException("
            + "          ex___, onerror, @sourceFile, @line);"
            + "}",
            "scriptBody", node,
            "sourceFile", StringLiteral.valueOf(
                FilePosition.UNKNOWN, imaginaryUri),
            "line", StringLiteral.valueOf(
                FilePosition.UNKNOWN, "0"));
        return UncajoledModule.of(node);
      } catch (Exception e) {
        throw new BuildException(e);
      }
    }
  }
}
