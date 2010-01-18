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

package com.google.caja.ancillary.servlet;

import com.google.caja.ancillary.opt.EnvironmentDatum;
import com.google.caja.ancillary.opt.JsOptimizer;
import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.JsLexer;
import com.google.caja.lexer.JsTokenQueue;
import com.google.caja.lexer.ParseException;
import com.google.caja.parser.ParseTreeNodes;
import com.google.caja.parser.js.Expression;
import com.google.caja.parser.js.Literal;
import com.google.caja.parser.js.ObjectConstructor;
import com.google.caja.parser.js.Parser;
import com.google.caja.parser.js.StringLiteral;
import com.google.caja.reporting.EchoingMessageQueue;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.util.Join;
import com.google.caja.util.Lists;
import com.google.caja.util.Maps;
import com.google.caja.util.Pair;
import com.google.caja.util.Sets;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * A store of user agent environment files.
 *
 * @author mikesamuel@gmail.com
 */
final class UserAgentDb {
  static final List<Pair<String, ObjectConstructor>> ENV_JSON
      = Lists.newArrayList();
  static final URI BROWSERSCOPE_WEB_SERVICE = URI.create(
      "http://www.browserscope.org/jskb/json");
  private static final int WEBSERVICE_TIMEOUT = 2000;
  final Map<String, Object> webCache = Maps.newHashMap();
  final URI webService;

  private UserAgentDb(URI webService) {
    // See Main.java for the typical origin, and how to override for testing
    // with a local browserscope build.
    this.webService = webService;
  }

  static UserAgentDb create(URI webService) {
    return new UserAgentDb(webService);
  }

  static {
    String[] envJsonFiles = new String[0];
    try {
      envJsonFiles = Resources.read(JsOptimizer.class, "env.json.list.txt")
         .toString().split("[\r\n]+");
    } catch (IOException ex) {
      ex.printStackTrace();
    }
    for (String envJsonFile : envJsonFiles) {
      if ("".equals(envJsonFile)) { continue; }
      try {
        CharProducer cp = Resources.read(JsOptimizer.class, envJsonFile);
        ObjectConstructor json = parseEnvJson(cp, logQueue());
        if (json != null) {
          Expression userAgent = json.getValue(
              EnvironmentDatum.NAV_USER_AGENT.getCode());
          if (userAgent != null) {
            ENV_JSON.add(Pair.pair(
                ((StringLiteral) userAgent).getUnquotedValue(), json));
          }
        }
      } catch (IOException ex) {
        ex.printStackTrace();
      } catch (ParseException ex) {
        ex.printStackTrace();
      }
    }
  }

  private static String webServiceKey(SortedSet<String> userAgents) {
    return Join.join(",", userAgents);
  }

  private static SortedSet<String> parseUserAgents(
      String commaSeparatedUserAgents) {
    Set<String> uas = Sets.newTreeSet(
        commaSeparatedUserAgents.split("\\s*,\\s*"));
    uas.remove("");
    return (SortedSet<String>) uas;
  }

  void prefetchEnvJson(String commaSeparatedUserAgents) {
    if (webService == null) { return; }
    SortedSet<String> uas = parseUserAgents(commaSeparatedUserAgents);
    fetchJsonAsync(webServiceKey(uas));
  }

  private void fetchJsonAsync(final String uaKey) {
    final Object mutex;
    synchronized (webCache) {
      if (webCache.containsKey(uaKey)) { return; }
      mutex = new Object();
      webCache.put(uaKey, mutex);
    }
    Runnable r = new Runnable() {
      public void run() {
        ObjectConstructor json = null;
        try {
          URL url = urlWithQuery(
              webService,
              ("ua=" + URLEncoder.encode(uaKey, "UTF-8")
               + "&ot=application%2Fjson"));
          URLConnection conn = url.openConnection();
          conn.setConnectTimeout(WEBSERVICE_TIMEOUT);
          conn.setAllowUserInteraction(false);
          conn.setDoInput(true);
          conn.setDoOutput(false);
          InputStream in = conn.getInputStream();
          try {
            if (!conn.getContentType().startsWith("application/json")) {
              throw new IOException("Content type " + conn.getContentType());
            }
            Reader r = new InputStreamReader(in, "UTF-8");
            CharProducer cp = CharProducer.Factory.create(
                r, new InputSource(webService));
            json = parseEnvJson(cp, logQueue());
          } finally {
            in.close();
          }
        } catch (IOException ex) {
          ex.printStackTrace();
        } catch (ParseException ex) {
          ex.printStackTrace();
        } finally {
          synchronized (webCache) {
            if (json != null) {
              webCache.put(uaKey, json);
            } else {
              webCache.remove(uaKey);
            }
          }
          synchronized (mutex) { mutex.notifyAll(); }
        }
      }
    };
    executor.execute(r);
  }

  private static final ExecutorService executor = new ThreadPoolExecutor(
      2 /* core threads */, 10 /* max threads */,
      WEBSERVICE_TIMEOUT * 5, TimeUnit.MILLISECONDS /* timeout */,
      new ArrayBlockingQueue<Runnable>(10));

  ObjectConstructor lookupEnvJson(
      String commaSeparatedUserAgents, long timeout) {
    SortedSet<String> uas = parseUserAgents(commaSeparatedUserAgents);
    if (webService != null) {  // Try the browserscope database.
      String uaKey = webServiceKey(uas);
      fetchJsonAsync(uaKey);
      Object mutex = null;
      ObjectConstructor json = null;
      synchronized (webCache) {
        Object o = webCache.get(uaKey);
        if (o == null || o instanceof ObjectConstructor) {
          json = (ObjectConstructor) o;
        } else {
          mutex = o;
        }
      }
      if (json == null) { json = lookupLocally(uas); }
      if (json == null && mutex != null) {
        synchronized (mutex) {
          try {
            mutex.wait(timeout);
          } catch (InterruptedException ex) {
            // stop
          }
        }
        synchronized (webCache) {
          Object o = webCache.get(uaKey);
          if (o instanceof ObjectConstructor) { json = (ObjectConstructor) o; }
        }
      }
      if (json != null) { return (ObjectConstructor) json.clone(); }
      return new ObjectConstructor(FilePosition.UNKNOWN);
    }
    return lookupLocally(uas);
  }

  private static ObjectConstructor lookupLocally(SortedSet<String> uas) {
    // Failover to local stored version.
    Pattern p;
    {
      StringBuilder patternBuf = new StringBuilder();
      for (String ua : uas) {
        if (patternBuf.length() != 0) { patternBuf.append('|'); }
        patternBuf.append("(?:").append(Pattern.quote(ua)).append(')');
      }
      p = Pattern.compile(patternBuf.toString());
    }
    ObjectConstructor result = null;
    for (Pair<String, ObjectConstructor> pair : ENV_JSON) {
      if (p.matcher(pair.a).find()) {
        if (result == null) {
          result = (ObjectConstructor) pair.b.clone();
        } else {
          result = merge(result, pair.b);
        }
      }
    }
    return result;
  }

  private static ObjectConstructor merge(
      ObjectConstructor a, ObjectConstructor b) {
    List<Pair<Literal, Expression>> props = Lists.newArrayList();
    List<? extends Expression> children = a.children();
    for (int i = 0, n = children.size(); i < n; i += 2) {
      StringLiteral key = (StringLiteral) children.get(i);
      Expression avalue = children.get(i + 1);
      Expression bvalue = b.getValue(key.getUnquotedValue());
      if (bvalue != null && ParseTreeNodes.deepEquals(avalue, bvalue)) {
        props.add(Pair.pair((Literal) key, avalue));
      }
    }
    return new ObjectConstructor(FilePosition.UNKNOWN, props);
  }

  private static URL urlWithQuery(URI base, String query)
      throws IOException {
    String uriStr = base.toString();
    int hash = uriStr.indexOf('#');
    if (hash >= 0) { uriStr = uriStr.substring(0, hash); }
    int qmark = uriStr.indexOf('?');
    uriStr += (qmark >= 0 ? '&' : '?') + query;
    return new URL(uriStr);
  }

  private static ObjectConstructor parseEnvJson(
      CharProducer cp, MessageQueue mq)
      throws ParseException {
    FilePosition pos = cp.filePositionForOffsets(
        cp.getOffset(), cp.getLimit());
    JsLexer lexer = new JsLexer(cp);
    JsTokenQueue tq = new JsTokenQueue(lexer, pos.source());
    tq.setInputRange(pos);
    Expression e = new Parser(tq, mq).parseExpression(true);
    if (e instanceof ObjectConstructor) { return (ObjectConstructor) e; }
    return null;
  }

  private static MessageQueue logQueue() {
    return new EchoingMessageQueue(
        new PrintWriter(System.err), new MessageContext());
  }
}
