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
import com.google.common.collect.Lists;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Node;

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
              HttpURLConnection conn = (HttpURLConnection)
                  ref.getUri().toURL().openConnection();
              // appengine has a caching http proxy; this limits it
              conn.setRequestProperty("Cache-Control", "max-age=10");
              return FetchedData.fromConnection(conn);
            } catch (ClassCastException ex) {
              throw new UriFetchException(ref, mimeType, ex);
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

  public static String[] getMessageLevels() {
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
      String base, String url, String input,
      boolean debugMode, String opt_idClass) {
    MessageContext mc = new MessageContext();
    MessageQueue mq = new SimpleMessageQueue();

    ParseTreeNode outputJs;
    Node outputHtml;

    Map<InputSource, ? extends CharSequence> originalSources
        = Collections.singletonMap(new InputSource(guessURI(base, url)), input);

    PluginMeta meta = new PluginMeta(fetcher, null);
    if (opt_idClass != null && opt_idClass.length() != 0) {
      meta.setIdClass(opt_idClass);
    }
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
      inputNode = Dom.transplant(p.parseDocument());
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
    TokenConsumer renderer = new JsPrettyPrinter(jsOut);
    RenderContext rc = new RenderContext(renderer);
    String htmlOut = outputHtml != null ? Nodes.render(outputHtml) : null;

    if (outputJs != null) {
      outputJs.render(rc);
      rc.getOut().noMoreTokens();
      return new CajolingServiceResult(htmlOut, jsOut.toString(), messages);
    } else {
      return new CajolingServiceResult(htmlOut, null, messages);
    }
  }

  private static String[] formatMessages(
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
      if (!"".equals(snippet)) {
        messageText.append(":").append(snippet);
      }
      result.add(messageText.toString());
    }
    return result.toArray(new String[0]);
  }

  public String getBuildInfo() {
    return BuildInfo.getInstance().getBuildInfo();
  }

  @SuppressWarnings("static-method")
  protected PluginCompiler makePluginCompiler(
      PluginMeta meta, MessageQueue mq) {
    PluginCompiler compiler = new PluginCompiler(
        BuildInfo.getInstance(), meta, mq);
    return compiler;
  }

  public String fetch(String base, String uri) {
    try {
      URI relUri = new URI(uri);
      URI absUri = null;
      URI baseAddress = new URI(base);
      absUri = relUri.isAbsolute() ? relUri : baseAddress.resolve(relUri);
      return fetcher.fetch(
          new ExternalReference(absUri, baseAddress, relUri,
              FilePosition.UNKNOWN), "*/*").getTextualContent().toString();
    } catch (URISyntaxException ex) {
      return null;
    } catch (UnsupportedEncodingException ex) {
      return null;
    } catch (UriFetcher.UriFetchException ex) {
      return null;
    }
  }
}
