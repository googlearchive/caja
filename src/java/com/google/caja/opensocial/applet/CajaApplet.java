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

package com.google.caja.opensocial.applet;

import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.ExternalReference;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.TokenConsumer;
import com.google.caja.lexer.escaping.Escaping;
import com.google.caja.plugin.Jobs;
import com.google.caja.plugin.PluginCompiler;
import com.google.caja.plugin.PluginMeta;
import com.google.caja.plugin.stages.ConsolidateCodeStage;
import com.google.caja.opensocial.DefaultGadgetRewriter;
import com.google.caja.opensocial.GadgetRewriteException;
import com.google.caja.opensocial.UriCallback;
import com.google.caja.opensocial.UriCallbackOption;
import com.google.caja.reporting.BuildInfo;
import com.google.caja.reporting.HtmlSnippetProducer;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.RenderContext;
import com.google.caja.reporting.SimpleMessageQueue;
import com.google.caja.reporting.SnippetProducer;
import com.google.caja.util.Pipeline;

import java.applet.Applet;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

/**
 * An Applet that can be embedded in a webpage to cajole output in browser.
 * See {@code index.html} in this same directory.
 *
 * @author mikesamuel@gmail.com
 */
public class CajaApplet extends Applet {
  @Override
  public String getAppletInfo() {
    return "Interactively cajoles an HTML gadget entered in a text box.";
  }

  /**
   * Invoked by javascript in the embedding page.
   * @param cajaInput as an HTML gadget.
   * @param featureNames {@link Feature} values as a comma separated list.
   *   We use a comma separated string instead of an array since IE 6's
   *   version of liveconnect does not convert javascript Arrays to java
   *   arrays.
   *   See discussion of IE applet/JS communication at
   *   http://www.rohitab.com/discuss/index.php?showtopic=28868&st=0&p=10029410
   * @return a tuple of {@code [ cajoledHtml, messageHtml ]}.
   *   If the cajoledHtml is non-null then cajoling succeeded.
   */
  public Object[] cajole(String cajaInput, String featureNames) {
    try {
      Set<Feature> features = EnumSet.noneOf(Feature.class);
      if (!"".equals(featureNames)) {
        for (String featureName : featureNames.split(",")) {
          features.add(Feature.valueOf(featureName));
        }
      }
      return runCajoler(cajaInput, features);
    } catch (RuntimeException ex) {
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      ex.printStackTrace(pw);
      pw.flush();
      return new Object[] { null, "<pre>" + html(sw.toString()) + "</pre>" };
    }
  }

  public String getVersion() {
    return BuildInfo.getInstance().getBuildInfo();
  }

  private Object[] runCajoler(String cajaInput, final Set<Feature> features) {
    // TODO(mikesamuel): If the text starts with a <base> tag, maybe use that
    // and white it out to preserve file positions.
    URI src = URI.create(getDocumentBase().toString());

    InputSource is = new InputSource(src);

    // Maps InputSource to the source code so we can generate snippets for
    // error messages.
    Map<InputSource, ? extends CharSequence> originalSources
        = Collections.singletonMap(is, cajaInput);

    CharProducer cp = CharProducer.Factory.create(
        new StringReader(cajaInput), is);

    MessageQueue mq = new SimpleMessageQueue();
    DefaultGadgetRewriter rw = new DefaultGadgetRewriter(mq) {
        @Override
        protected RenderContext createRenderContext(
            TokenConsumer out, MessageContext mc) {
          return new RenderContext(
              mc, features.contains(Feature.EMBEDDABLE), out);
        }
        @Override
        protected PluginCompiler createPluginCompiler(
            PluginMeta meta, MessageQueue mq) {
          PluginCompiler pc = super.createPluginCompiler(meta, mq);
          List<Pipeline.Stage<Jobs>> stages
              = pc.getCompilationPipeline().getStages();
          for (ListIterator<Pipeline.Stage<Jobs>> it = stages.listIterator();
               it.hasNext();) {
            // Add statements to print results from script blocks just before
            // all the script blocks are combined into one JS tree.
            if (it.next() instanceof ConsolidateCodeStage) {
              it.previous();
              it.add(new ExpressionLanguageStage());
              break;
            }
          }
          return pc;
        }
      };
    rw.setDebugMode(features.contains(Feature.DEBUG_SYMBOLS));
    rw.setWartsMode(features.contains(Feature.WARTS_MODE));

    StringBuilder cajoledOutput = new StringBuilder();
    UriCallback uriCallback = new UriCallback() {
        public UriCallbackOption getOption(
            ExternalReference extRef, String mimeType) {
          return UriCallbackOption.REWRITE;
        }

        public Reader retrieve(ExternalReference extref, String mimeType) {
          // If we do retreive content, make sure to stick the original source
          // in originalSources.
          return null;
        }

        public URI rewrite(ExternalReference extref, String mimeType) {
          try {
            return URI.create(
                "http://secure-proxy.google.com/?url="
                + URLEncoder.encode(extref.getUri().toString(), "UTF-8")
                + "&mimeType=" + URLEncoder.encode(mimeType, "UTF-8"));
          } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException("UTF-8 should be supported.", ex);
          }
        }
      };

    try {
      rw.rewriteContent(src, cp, uriCallback, cajoledOutput);
      return new Object[] {
        cajoledOutput.toString(),
        messagesToString(originalSources, mq)
      };
    } catch (IOException ex) {
      throw new RuntimeException(ex);
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
    mc.inputSources = originalSrc.keySet();
    SnippetProducer sp = new HtmlSnippetProducer(originalSrc, mc);

    StringBuilder messageText = new StringBuilder();
    for (Message msg : mq.getMessages()) {
      if (MessageLevel.LINT.compareTo(msg.getMessageLevel()) > 0) { continue; }
      String snippet = sp.getSnippet(msg);

      messageText.append("<div class=\"message ")
          .append(msg.getMessageLevel().name()).append("\">")
          .append(msg.getMessageLevel().name()).append(' ')
          .append(msg.format(mc));
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

  private static enum Feature {
    EMBEDDABLE,
    DEBUG_SYMBOLS,
    WARTS_MODE,
    ;
  }
}
