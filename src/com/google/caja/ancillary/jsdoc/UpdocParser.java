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

package com.google.caja.ancillary.jsdoc;

import java.util.ArrayList;
import java.util.List;

import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.JsLexer;
import com.google.caja.lexer.JsTokenQueue;
import com.google.caja.lexer.JsTokenType;
import com.google.caja.lexer.ParseException;
import com.google.caja.lexer.Token;
import com.google.caja.lexer.TokenStream;
import com.google.caja.parser.js.Expression;
import com.google.caja.parser.js.Parser;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.ancillary.jsdoc.Updoc.Run;

/**
 * Parses {@link Updoc}.
 *
 * @author mikesamuel@gmail.com
 */
final class UpdocParser {
  private final MessageQueue mq;
  UpdocParser(MessageQueue mq) { this.mq = mq; }

  Updoc parseComplete(CharProducer cp) throws ParseException {
    JsTokenQueue tq = new JsTokenQueue(
        new UpdocLexer(cp), cp.getCurrentPosition().source());
    tq.setInputRange(cp.filePositionForOffsets(cp.getOffset(), cp.getLimit()));
    Updoc updoc = parse(tq);
    tq.expectEmpty();
    return updoc;
  }

  Updoc parse(JsTokenQueue tq) throws ParseException {
    Parser jsParser = new Parser(tq, mq, false);
    List<Run> runs = new ArrayList<Run>();
    do {
      tq.expectToken("$");
      FilePosition start = tq.currentPosition();
      Expression input = jsParser.parseExpression(true);
      tq.checkToken(";");
      tq.expectToken("#");
      Expression result = jsParser.parseExpression(true);
      tq.checkToken(";");
      Updoc.Run run = new Updoc.Run(
          FilePosition.span(start, tq.lastPosition()), input, result);
      runs.add(run);
    } while (tq.lookaheadToken("$"));
    Updoc updoc = new Updoc(
        FilePosition.span(
            runs.get(0).getFilePosition(),
            runs.get(runs.size() - 1).getFilePosition()),
        runs);
    return updoc;
  }

  static final class UpdocLexer implements TokenStream<JsTokenType> {
    private final JsLexer l;
    UpdocLexer(CharProducer cp) {
      this.l = new JsLexer(cp, false);
    }

    public boolean hasNext() throws ParseException {
      return l.hasNext();
    }

    public Token<JsTokenType> next() throws ParseException {
      return l.next();
    }
  }
}
