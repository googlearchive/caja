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

package com.google.caja.ancillary.opt;

import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.JsLexer;
import com.google.caja.lexer.JsTokenQueue;
import com.google.caja.lexer.ParseException;
import com.google.caja.parser.js.Expression;
import com.google.caja.parser.js.Parser;
import com.google.caja.render.JsMinimalPrinter;
import com.google.caja.reporting.DevNullMessageQueue;
import com.google.caja.reporting.EchoingMessageQueue;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.RenderContext;
import com.google.common.collect.Maps;

import java.io.PrintWriter;
import java.util.Map;

/**
 * Encapsulates an environment data file as produced by "environment-checks.js."
 *
 * @author mikesamuel@gmail.com
 */
public class EnvironmentData {
  private final Map<String, Object> data;

  public EnvironmentData(Map<? extends String, ?> data) throws ParseException {
    this.data = Maps.newLinkedHashMap();
    for (Map.Entry<? extends String, ?> e : data.entrySet()) {
      this.data.put(normJsQuiet(e.getKey()), e.getValue());
    }
  }

  public Object get(String code) throws IllegalArgumentException {
    try {
      return data.get(normJsQuiet(code));
    } catch (ParseException ex) {
      throw new IllegalArgumentException("Bad code snippet " + code, ex);
    }
  }

  public Object get(EnvironmentDatum d) {
    return data.get(d.getCode());
  }

  public static String normJsQuiet(String js) throws ParseException {
    return normJs(js, DevNullMessageQueue.singleton());
  }

  static final MessageQueue LOUD_MQ = new EchoingMessageQueue(
      new PrintWriter(System.err), new MessageContext(), true);
  static String normJs(String js, MessageQueue mq) throws ParseException {
    JsLexer lexer = new JsLexer(
        CharProducer.Factory.fromString(js, FilePosition.UNKNOWN));
    JsTokenQueue tq = new JsTokenQueue(lexer, InputSource.UNKNOWN);
    Expression e = new Parser(tq, mq).parseExpression(true);
    tq.expectEmpty();
    StringBuilder sb = new StringBuilder(js.length() + 16);
    RenderContext rc = new RenderContext(new JsMinimalPrinter(sb));
    e.render(rc);
    rc.getOut().noMoreTokens();
    return sb.toString();
  }
}
