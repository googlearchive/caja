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

package com.google.caja.demos.playground.server;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Node;

import com.google.caja.demos.playground.client.CajolingServiceResult;
import com.google.caja.demos.playground.client.PlaygroundService;
import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.ExternalReference;
import com.google.caja.lexer.FetchedData;
import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.HtmlLexer;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.ParseException;
import com.google.caja.lexer.TokenConsumer;
import com.google.caja.lexer.escaping.UriUtil;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.ParseTreeNode.ReflectiveCtor;
import com.google.caja.parser.html.Dom;
import com.google.caja.parser.html.DomParser;
import com.google.caja.parser.html.Nodes;
import com.google.caja.plugin.DataUriFetcher;
import com.google.caja.plugin.PluginCompiler;
import com.google.caja.plugin.PluginMeta;
import com.google.caja.plugin.UriFetcher;
import com.google.caja.plugin.UriFetcher.ChainingUriFetcher;
import com.google.caja.render.Concatenator;
import com.google.caja.render.JsPrettyPrinter;
import com.google.caja.reporting.BuildInfo;
import com.google.caja.reporting.HtmlSnippetProducer;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.RenderContext;
import com.google.caja.reporting.SimpleMessageQueue;
import com.google.caja.reporting.SnippetProducer;
import com.google.caja.util.Lists;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;

/**
 * Implements the GWT version of the cajoling service
 * @author jasvir@google.com (Jasvir Nagra)
 */
@SuppressWarnings("serial")
public class GWTCajolingServiceImpl extends RemoteServiceServlet
    implements PlaygroundService {
  private final UriFetcher fetcher;

  public GWTCajolingServiceImpl(UriFetcher fetcher) {
    this.fetcher = fetcher;
  }

  @ReflectiveCtor
  public GWTCajolingServiceImpl() {
    this(ChainingUriFetcher.make(
        new DataUriFetcher(),
        new UriFetcher() {
          public FetchedData fetch(ExternalReference ref, String mimeType)
              throws UriFetchException {
            try {
              return FetchedData.fromConnection(
                  ref.getUri().toURL().openConnection());
            } catch (IOException ex) {
              throw new UriFetchException(ref, mimeType, ex);
            }
          }
        }));
  }

  private static URI guessURI(String base, String guess) {
    URI unknown = URI.create("unknown:///unknown");
    try {
      base = null == base ? null : UriUtil.normalizeUri(base);
      guess = null == guess ? null : UriUtil.normalizeUri(guess);
      if (null != base && null != guess) {
        return unknown.resolve(base).resolve(guess);
      }
      if (null != guess) {
        return unknown.resolve(guess);
      }
    } catch (URISyntaxException e) {
      return unknown;
    } catch (IllegalArgumentException e) {
      return unknown;
    }
    return unknown;
  }

  public String[] getMessageLevels() {
    MessageLevel[] values = MessageLevel.values();
    String[] result = new String[values.length];
    for (int i = 0; i < values.length; i++) {
      result[i] = values[i].name();
    }
    return result;
  }

  // TODO(jasvir): Outline this and all the other examples of cajole
  // into a more usable api for the cajoler
  public CajolingServiceResult cajole(
      String base, String url, String input, boolean es53Mode) {
    MessageContext mc = new MessageContext();
    MessageQueue mq = new SimpleMessageQueue();

    ParseTreeNode outputJs;
    Node outputHtml;

    Map<InputSource, ? extends CharSequence> originalSources
        = Collections.singletonMap(new InputSource(guessURI(base, url)), input);
    
    PluginMeta meta = new PluginMeta(fetcher, null);
    meta.setEnableES53(es53Mode);
    PluginCompiler compiler = makePluginCompiler(meta, mq);
    compiler.setJobCache(new AppEngineJobCache());
    compiler.setMessageContext(mc);
    
    URI uri = guessURI(base, url);
    InputSource is = new InputSource(uri);
    CharProducer cp = CharProducer.Factory.fromString(input, is);
    boolean okToContinue = true;
    Dom inputNode = null;
    try {
      DomParser p = new DomParser(new HtmlLexer(cp), false, is, mq);
      inputNode = new Dom(p.parseFragment());
      p.getTokenQueue().expectEmpty();
    } catch (ParseException e) {
      mq.addMessage(e.getCajaMessage());
      okToContinue = false;
    }
    
    if (okToContinue && inputNode != null) {
      compiler.addInput(inputNode, uri);
      okToContinue &= compiler.run();
    }
    
    outputJs = okToContinue ? compiler.getJavascript() : null;
    outputHtml = okToContinue ? compiler.getStaticHtml() : null;
    
    String[] messages = formatMessages(originalSources, mc, mq);
    
    StringBuilder jsOut = new StringBuilder();
    TokenConsumer renderer = new JsPrettyPrinter(new Concatenator(jsOut));
    RenderContext rc = new RenderContext(renderer)
        .withAsciiOnly(true)
        .withEmbeddable(true);
    String htmlOut = outputHtml != null ? Nodes.render(outputHtml) : null;

    if (outputJs != null) {
      outputJs.render(rc);
      rc.getOut().noMoreTokens();
      return new CajolingServiceResult(htmlOut, jsOut.toString(), messages);
    } else {
      return new CajolingServiceResult(htmlOut, null, messages);
    }
  }

  private String[] formatMessages(
      Map<InputSource, ? extends CharSequence> inputMap,
      MessageContext mc, MessageQueue mq) {
    List<Message> messages = mq.getMessages();
    SnippetProducer sp = new HtmlSnippetProducer(inputMap, mc);
    List<String> result = Lists.newArrayList();

    for (Message msg : messages) {
      String snippet = sp.getSnippet(msg);
      StringBuilder messageText = new StringBuilder();
      messageText.append(msg.getMessageLevel().name())
                 .append(" ")
                 .append(msg.format(mc));
      messageText.append(":").append(snippet);
      result.add(messageText.toString());
    }
    return result.toArray(new String[0]);
  }

  public String getBuildInfo() {
    return BuildInfo.getInstance().getBuildInfo();
  }

  protected PluginCompiler makePluginCompiler(
      PluginMeta meta, MessageQueue mq) {
    PluginCompiler compiler = new PluginCompiler(
        BuildInfo.getInstance(), meta, mq);
    return compiler;
  }

  public String fetch(String base, String uri) {
    try {
      URI address = new URI(uri);
      if (!address.isAbsolute()) {
        URI baseAddress = new URI(base);
        address = baseAddress.resolve(address);
      }
      return fetcher.fetch(
          new ExternalReference(address, FilePosition.UNKNOWN), "*/*")
          .getTextualContent().toString();
    } catch (URISyntaxException ex) {
      return null;
    } catch (UnsupportedEncodingException ex) {
      return null;
    } catch (UriFetcher.UriFetchException ex) {
      return null;
    }
  }
}
