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

import com.google.caja.ancillary.opt.JsOptimizer;
import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.FilePosition;
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
import com.google.caja.util.Lists;
import com.google.caja.util.Pair;
import com.google.caja.util.Sets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.regex.Pattern;

/**
 * A store of user agent environment files.
 *
 * @author mikesamuel@gmail.com
 */
final class UserAgentDb {
  static final List<String> USER_AGENTS;
  private static final List<Pair<String, ObjectConstructor>> ENV_JSON
      = Lists.newArrayList();

  private UserAgentDb() {}

  static {
    List<String> userAgents = Lists.newArrayList();
    String[] envJsonFiles = new String[0];
    try {
      envJsonFiles = Resources.read(JsOptimizer.class, "env.json.list.txt")
         .toString().split("[\r\n]+");
    } catch (IOException ex) {
      ex.printStackTrace();
    }
    for (String envJsonFile : envJsonFiles) {
      if ("".equals(envJsonFile)) { continue; }
      assert envJsonFile.endsWith(".env.json");
      String userAgent = envJsonFile.substring(0, envJsonFile.length() - 9)
          .replace('_', ' ');
      userAgents.add(userAgent);
      try {
        CharProducer cp = Resources.read(JsOptimizer.class, envJsonFile);
        ObjectConstructor json = parseEnvJson(cp, logQueue());
        if (json != null) {
          ENV_JSON.add(Pair.pair(userAgent, json));
        }
      } catch (IOException ex) {
        ex.printStackTrace();
      } catch (ParseException ex) {
        ex.printStackTrace();
      }
    }
    USER_AGENTS = Collections.unmodifiableList(userAgents);
  }

  private static SortedSet<String> parseUserAgents(
      String commaSeparatedUserAgents) {
    Set<String> uas = Sets.newTreeSet(
        commaSeparatedUserAgents.split("\\s*,\\s*"));
    uas.remove("");
    return (SortedSet<String>) uas;
  }

  static ObjectConstructor lookupEnvJson(String commaSeparatedUserAgents) {
    return lookupLocally(parseUserAgents(commaSeparatedUserAgents));
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
