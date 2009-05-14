// Copyright 2009 Google Inc. All Rights Reserved.
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

package com.google.caja.service;

import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.ExternalReference;
import com.google.caja.lexer.HtmlLexer;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.ParseException;
import com.google.caja.parser.AncestorChain;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.html.DomParser;
import com.google.caja.parser.quasiliteral.Rewriter;
import com.google.caja.plugin.Dom;
import com.google.caja.plugin.PluginCompiler;
import com.google.caja.plugin.PluginEnvironment;
import com.google.caja.plugin.PluginMeta;
import com.google.caja.reporting.BuildInfo;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.SimpleMessageQueue;
import com.google.caja.util.Pair;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;

/**
 * Retrieves html files and cajoles them
 *
 * @author jasvir@google.com (Jasvir Nagra)
 */
public class HtmlHandler implements ContentHandler {
  private final BuildInfo buildInfo;
  private PluginMeta meta;
  private final static String DEFAULT_HOSTED_SERVICE = 
    "http://caja.appsport.com/cajoler";
  
  public HtmlHandler(BuildInfo buildInfo) {
    this(buildInfo, DEFAULT_HOSTED_SERVICE);
  }
  
  
  public HtmlHandler(BuildInfo buildInfo, final String hostedService) {
    this.buildInfo = buildInfo;
    this.meta = new PluginMeta(new PluginEnvironment() {
      public CharProducer loadExternalResource(ExternalReference ref,
          String mimeType) {
        return null;
      }

      public String rewriteUri(ExternalReference uri, String mimeType) {
        if (hostedService != null) {
          try {
            return hostedService
            + "?url=" 
            + URLEncoder.encode(uri.getUri().toString(),
                "UTF-8")
            + "&mime-type=" + mimeType;
          } catch (UnsupportedEncodingException e) {
            return null;
          }
        } else {
          return null;
        }
      }
      
    });
    // HtmlHandler only cajoles in valija mode
    meta.setValijaMode(true);
  }

  public boolean canHandle(URI uri, CajolingService.Transform transform, 
      String contentType, ContentTypeCheck checker) {
    return checker.check("text/html", contentType);
  }

  public Pair<String,String> apply(URI uri, CajolingService.Transform transform, 
      String contentType, String charset, byte[] content, OutputStream response)
      throws UnsupportedContentTypeException {
    if (charset == null) { charset = "UTF-8"; }
    try {
      OutputStreamWriter writer = new OutputStreamWriter(response, "UTF-8");
      cajoleHtml(uri, new StringReader(new String(content, charset)), writer);
      writer.flush();
    } catch (IOException e) {
      throw new UnsupportedContentTypeException();
    }
    return new Pair<String, String>("text/javascript", "UTF-8");
  }

  public void printMessages(MessageQueue mq, MessageContext mc,
      Appendable out) {
    try {
      for (Message m : mq.getMessages()) {
        MessageLevel level = m.getMessageLevel();
        out.append(level.name() + ": ");
        m.format(mc, out);
        out.append("\n");
      }
    } catch (IOException ex) {
      ex.printStackTrace();
    }
  }
  
  private void cajoleHtml(final URI inputUri, Reader cajaInput,
      Appendable output) 
      throws IOException, UnsupportedContentTypeException {
    InputSource is = new InputSource (inputUri);
    CharProducer cp = CharProducer.Factory.create(cajaInput,is);
    MessageQueue mq = new SimpleMessageQueue();
    boolean okToContinue = true;
    try {
      DomParser p = new DomParser(new HtmlLexer(cp), is, mq);
      if (p.getTokenQueue().isEmpty()) { okToContinue = false; }

      ParseTreeNode html = new Dom(p.parseFragment(
          DomParser.makeDocument(null, null)));
      p.getTokenQueue().expectEmpty();

      PluginCompiler compiler = new PluginCompiler(buildInfo, meta, mq);

      compiler.addInput(new AncestorChain<ParseTreeNode>(html));
      if (okToContinue) {
        okToContinue &= compiler.run();
      }
      if (okToContinue) {
        output.append(Rewriter.render(compiler.getJavascript()));
      } else {
        MessageContext mc = new MessageContext();
        printMessages(mq, mc, System.err);
      }
    } catch (ParseException e) {
      throw new UnsupportedContentTypeException();
    } catch (IllegalArgumentException e) {
      throw new UnsupportedContentTypeException();
    } catch (IOException e) {
      throw new UnsupportedContentTypeException();
    }
  }
}
