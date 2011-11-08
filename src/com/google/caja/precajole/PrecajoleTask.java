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
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.ParseException;
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
import com.google.common.base.Charsets;
import com.google.common.io.Files;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

public class PrecajoleTask extends Task {

  private String to;
  private final ArrayList<Source> sources = new ArrayList<Source>();

  private StaticPrecajoleMap map;

  public Source createSource() {
    Source s = new Source();
    sources.add(s);
    return s;
  }

  public void setTo(String value) {
    to = value;
  }

  @Override
  public void execute() {
    if (to == null) {
      throw new BuildException("missing to= attribute");
    }

    long now = System.currentTimeMillis();

    new File(to).mkdirs();
    map = new StaticPrecajoleMap(to);

    // If build.xml is newer, rebuild all
    File antFile = new File(getProject().getUserProperty("ant.file"));
    if (map.getModTime() <= antFile.lastModified()) {
      map.setModTime(0);
    }

    try {
      for (Source s: sources) {
        s.precajole();
      }
      map.setModTime(now);
      map.finish();
    } catch (Exception e) {
      throw new BuildException(e);
    }
  }

  public class Source {
    public Source() {}

    String fileName;
    String text;

    @Override
    public String toString() {
      return fileName + " : " + text;
    }

    public void setFile(String value) {
      fileName = value;
    }

    public void addText(String value) {
      text = getProject().replaceProperties(value);
    }

    // Returns true if changed
    boolean precajole() {
      String[] uris = text.trim().split("\\s+");
      if (0 < uris.length) {
        File f = new File(fileName);
        if (map.getModTime() <= f.lastModified()) {
          String source = readSource(f);
          CajoledModule cajoled = cajole(f);
          map.put(uris, source, cajoled);
          return true;
        }
      }
      return false;
    }

    private String readSource(File f) {
      try {
        return Files.toString(f, Charsets.UTF_8);
      } catch (IOException e) {
        return null;
      }
    }

    private CajoledModule cajole(File src) {
      MessageQueue mq = new EchoingMessageQueue(
          new PrintWriter(System.err), new MessageContext(), false);
      PluginMeta pm = new PluginMeta();
      pm.setPrecajoleMap(null);
      ModuleManager mgr = new ModuleManager(
          pm, BuildInfo.getInstance(),
          UriFetcher.NULL_NETWORK, false, mq);
      UncajoledModule input = uncajoled(src, mq);

      // TODO(felix8a): maybe should use compilation pipeline
      ArrayIndexOptimization.optimize(input);
      ParseTreeNode result = new ExpressionSanitizerCaja(mgr, null)
          .sanitize(input);

      if (mq.hasMessageAtLevel(MessageLevel.ERROR)) {
        throw new BuildException("Failed to cajole " + src);
      }
      if (!(result instanceof CajoledModule)) {
        throw new BuildException("No CajoledModule for " + src);
      }
      return (CajoledModule) result;
    }

    private UncajoledModule uncajoled(File src, MessageQueue mq) {
      try {
        ParseTreeNode node = new ParserContext(mq)
            .withInput(ContentType.JS)
            .withInput(CharProducer.Factory.create(
                new FileReader(src),
                new InputSource(src.toURI())))
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
                FilePosition.UNKNOWN, src.getName()),
            "line", StringLiteral.valueOf(
                FilePosition.UNKNOWN, "0"));
        return UncajoledModule.of(node);
      } catch (ParseException e) {
        throw new BuildException(e);
      } catch (IOException e) {
        throw new BuildException(e);
      }
    }
  }
}
