package com.google.caja.demos.playground.server;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.caja.demos.playground.client.CajolingServiceResult;
import com.google.caja.demos.playground.client.PlaygroundService;
import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.ExternalReference;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.escaping.UriUtil;
import com.google.caja.opensocial.DefaultGadgetRewriter;
import com.google.caja.opensocial.GadgetRewriteException;
import com.google.caja.plugin.PluginEnvironment;
import com.google.caja.reporting.BuildInfo;
import com.google.caja.reporting.HtmlSnippetProducer;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.SimpleMessageQueue;
import com.google.caja.reporting.SnippetProducer;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;

/**
 * Implements the GWT version of the cajoling service
 * @author jasvir@google.com (Jasvir Nagra)
 */
@SuppressWarnings("serial")
public class GWTCajolingServiceImpl extends RemoteServiceServlet
    implements PlaygroundService {

  private static final PluginEnvironment uriCallback = new PluginEnvironment() {
    public CharProducer loadExternalResource(
        ExternalReference ref, String mimeType) {
      return null;
    }

    // TODO(jasvir): URIs in some contexts (such as links to new pages) should
    // point back to the gwt cajoling service, while others that load media into
    // an existing page should go through a configurable cajoling service
    public String rewriteUri(ExternalReference extref, String mimeType) {
      if (mimeType.startsWith("image/")) {
        return extref.getUri().toString();
      }
      return (
          "http://caja.appspot.com/cajole"
          + "?url=" + UriUtil.encode(extref.getUri().toString())
          + "&mime-type=" + UriUtil.encode(mimeType));
    }
  };

  private static URI guessURI(String guess) {
    try {
      guess = UriUtil.normalizeUri(guess);
      if (guess != null) { return new URI(guess); }
    } catch (URISyntaxException e) {
      // fallback below
    }
    return URI.create("unknown:///unknown");
  }

  public String[] getMessageLevels() {
    MessageLevel[] values = MessageLevel.values();
    String[] result = new String[values.length];
    for (int i = 0; i < values.length; i++) {
      result[i] = values[i].name();
    }
    return result;
  }

  public CajolingServiceResult cajole(String url, String input) {
    MessageContext mc = new MessageContext();
    MessageQueue mq = new SimpleMessageQueue();

    Appendable output = new StringBuilder();
    String html = null;
    String javascript = null;

    Map<InputSource, ? extends CharSequence> originalSources
        = Collections.singletonMap(new InputSource(guessURI(url)), input);
    
    try {
      DefaultGadgetRewriter rw = new DefaultGadgetRewriter(
          BuildInfo.getInstance(), mq);
      StringReader in = new StringReader(input);
      rw.rewriteContent(guessURI(url), in, uriCallback, output);
      String[] htmlAndJs = output.toString().split("<script[^>]*>");
      html = htmlAndJs[0];
      javascript = htmlAndJs.length > 1 ?
        htmlAndJs[1].substring(0, htmlAndJs[1].length() - 9) : null;
    } catch (IOException e) {
      e.printStackTrace();
    } catch (GadgetRewriteException e) {
      // Reflected in the message queue which is serialized below
    }
    String[] messages = formatMessages(originalSources, mc, mq);
    return new CajolingServiceResult(html, javascript, messages);
  }

  private String[] formatMessages(
      Map<InputSource, ? extends CharSequence> inputMap,
      MessageContext mc, MessageQueue mq) {
    List<Message> messages = mq.getMessages();
    SnippetProducer sp = new HtmlSnippetProducer(inputMap, mc);
    List<String> result = new ArrayList<String>();

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

  public String fetch(String url) {
    try {
      URL address;
      address = new URL(url);
      Reader r = new InputStreamReader(address.openStream());
      int c;
      StringBuffer result = new StringBuffer();
      while ((c = r.read()) != -1) {
        result.append((char)c);
      }
      return result.toString();
    } catch (IOException e) {
      return null;
    }
  }
}
