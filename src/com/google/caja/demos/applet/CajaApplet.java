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

package com.google.caja.demos.applet;

import com.google.caja.SomethingWidgyHappenedError;
import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.ExternalReference;
import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.TokenConsumer;
import com.google.caja.lexer.escaping.Escaping;
import com.google.caja.lexer.escaping.UriUtil;
import com.google.caja.parser.js.ArrayConstructor;
import com.google.caja.parser.js.Expression;
import com.google.caja.parser.js.NullLiteral;
import com.google.caja.parser.js.StringLiteral;
import com.google.caja.plugin.PipelineMaker;
import com.google.caja.plugin.PluginEnvironment;
import com.google.caja.opensocial.DefaultGadgetRewriter;
import com.google.caja.opensocial.GadgetRewriteException;
import com.google.caja.render.Concatenator;
import com.google.caja.render.JsMinimalPrinter;
import com.google.caja.reporting.BuildInfo;
import com.google.caja.reporting.HtmlSnippetProducer;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.RenderContext;
import com.google.caja.reporting.SimpleMessageQueue;
import com.google.caja.reporting.SnippetProducer;

import java.applet.Applet;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An Applet that can be embedded in a webpage to cajole output in browser.
 * See {@code index.html} in this same directory.
 *
 * @author mikesamuel@gmail.com
 */
public class CajaApplet extends Applet {
  private final boolean standAlone;
  private BuildInfo buildInfo = BuildInfo.getInstance();

  public CajaApplet() {
    standAlone = false;
  }

  public CajaApplet(boolean standalone) {
    this.standAlone = standalone;
  }

  void setBuildInfo(BuildInfo buildInfo) { this.buildInfo = buildInfo; }

  @Override
  public String getAppletInfo() {
    return "Interactively cajoles an HTML gadget entered in a text box.";
  }

  /**
   * Invoked by javascript in the embedding page.
   * @param cajaInput as an HTML gadget.
   * @param featureNames {@link Feature} values and other configuration
   *   parameters as a comma separated list.
   *   We use a comma separated string instead of an array since IE 6's
   *   version of liveconnect does not convert javascript Arrays to java
   *   arrays.
   *   See discussion of IE applet/JS communication at
   *   http://www.rohitab.com/discuss/index.php?showtopic=28868&st=0&p=10029410
   * @return a javascript tuple of {@code [ cajoledHtml, messageHtml ]}.
   *   If the cajoledHtml is non-null then cajoling succeeded.
   *   We return a string instead of an Array or a Pair since IE's JS/java
   *   bridge deals poorly with values that can't be converted to JS primitives.
   */
  public String cajole(String cajaInput, String featureNames) {
    try {
      Set<Feature> features = EnumSet.noneOf(Feature.class);
      String testbedServer = null;
      if (!"".equals(featureNames)) {
        for (String featureName : featureNames.split(",")) {
          if (featureName.startsWith("testbedServer=")) {
            testbedServer = featureName.substring(featureName.indexOf('=') + 1);
          } else {
            features.add(Feature.valueOf(featureName));
          }
        }
      }

      final String proxyServer = testbedServer;

      PluginEnvironment env = new PluginEnvironment() {
          public CharProducer loadExternalResource(
              ExternalReference ref, String mimeType) {
            // If we do retrieve content, make sure to stick the original source
            // in originalSources.
            return null;
          }

          public String rewriteUri(ExternalReference extref, String mimeType) {
            return (
                proxyServer + "/proxy?url="
                + UriUtil.encode(extref.getUri().toString())
                + "&mimeType=" + UriUtil.encode(mimeType));
          }
        };

      return serializeJsArray(runCajoler(cajaInput, env, features));
    } catch (RuntimeException ex) {
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      ex.printStackTrace(pw);
      pw.flush();
      return serializeJsArray(null, "<pre>" + html(sw.toString()) + "</pre>");
    }
  }

  public String getVersion() {
    return buildInfo.getBuildInfo();
  }

  private Object[] runCajoler(String cajaInput, PluginEnvironment env,
                              final Set<Feature> features) {
    // TODO(mikesamuel): If the text starts with a <base> tag, maybe use that
    // and white it out to preserve file positions.
    String url = standAlone
        ? "http://www.example.com/" : getDocumentBase().toString();
    URI src = URI.create(url);
    InputSource is = new InputSource(src);

    // Maps InputSource to the source code so we can generate snippets for
    // error messages.
    Map<InputSource, ? extends CharSequence> originalSources
        = Collections.singletonMap(is, cajaInput);

    CharProducer cp = CharProducer.Factory.create(
        new StringReader(cajaInput), is);

    MessageQueue mq = new SimpleMessageQueue();
    DefaultGadgetRewriter rw = new DefaultGadgetRewriter(buildInfo, mq) {
      @Override
      protected RenderContext createRenderContext(TokenConsumer out) {
        return new RenderContext(out)
            .withAsciiOnly(features.contains(Feature.ASCII_ONLY))
            .withEmbeddable(features.contains(Feature.EMBEDDABLE));
      }
    };
    if (features.contains(Feature.DEBUG_SYMBOLS)) {
      rw.setGoals(
          rw.getGoals()
          .with(PipelineMaker.CAJOLED_MODULE_DEBUG)
          .without(PipelineMaker.CAJOLED_MODULE));
    }

    StringBuilder cajoledOutput = new StringBuilder();

    try {
      rw.rewriteContent(src, cp, env, cajoledOutput);
      return new Object[] {
        cajoledOutput.toString(),
        messagesToString(originalSources, mq)
      };
    } catch (IOException ex) {
      throw new SomethingWidgyHappenedError("Unexpected I/O error", ex);
    } catch (GadgetRewriteException ex) {
      return new Object[] {
        null,
        messagesToString(originalSources, mq)
      };
    }
  }

  private String messagesToString(
      Map<InputSource, ? extends CharSequence> originalSrc, MessageQueue mq) {
    MessageContext mc = new MessageContext();
    for (InputSource is : originalSrc.keySet()) {
      mc.addInputSource(is);
    }
    SnippetProducer sp = new HtmlSnippetProducer(originalSrc, mc);

    StringBuilder messageText = new StringBuilder();
    for (Message msg : mq.getMessages()) {
      if (MessageLevel.LINT.compareTo(msg.getMessageLevel()) > 0) { continue; }
      String snippet = sp.getSnippet(msg);

      messageText.append("<div class=\"message ")
          .append(msg.getMessageLevel().name()).append("\">")
          .append(msg.getMessageLevel().name()).append(' ')
          .append(html(msg.format(mc)));
      if (!"".equals(snippet)) {
        messageText.append("<br />").append(snippet);
      }
      messageText.append("</div>");
    }
    return messageText.toString();
  }

  private static String html(CharSequence s) {
    StringBuilder sb = new StringBuilder();
    Escaping.escapeXml(s, false, sb);
    return sb.toString();
  }

  private static String serializeJsArray(Object... values) {
    List<Expression> valueExprs = new ArrayList<Expression>();
    for (Object value : values) {
      if (value == null) {
        valueExprs.add(new NullLiteral(FilePosition.UNKNOWN));
      } else {
        valueExprs.add(
            StringLiteral.valueOf(FilePosition.UNKNOWN, (String) value));
      }
    }
    StringBuilder sb = new StringBuilder();
    JsMinimalPrinter pp = new JsMinimalPrinter(new Concatenator(sb));
    (new ArrayConstructor(FilePosition.UNKNOWN, valueExprs)).render(
        new RenderContext(pp).withAsciiOnly(true).withEmbeddable(true));
    pp.noMoreTokens();
    return sb.toString();
  }

  static enum Feature {
    /** Present if rendered output should only contain ASCII characters. */
    ASCII_ONLY,
    /** Present to output debug symbols for use with cajita-debugmode.js. */
    DEBUG_SYMBOLS,
    /** Present if the output should be embeddable in HTML or XML. */
    EMBEDDABLE,
    ;
  }
}
