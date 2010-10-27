// Copyright 2008 Google Inc. All Rights Reserved.
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

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.util.List;

import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.FetchedData;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.JsLexer;
import com.google.caja.lexer.JsTokenQueue;
import com.google.caja.lexer.ParseException;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.Parser;
import com.google.caja.parser.quasiliteral.InnocentCodeRewriter;
import com.google.caja.parser.quasiliteral.Rewriter;
import com.google.caja.reporting.BuildInfo;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.util.Charsets;
import com.google.caja.util.ContentType;
import com.google.caja.util.Pair;

/**
 * Retrieves javascript files and cajoles them
 *
 * @author jasvir@google.com (Jasvir Nagra)
 */
public class InnocentHandler extends AbstractCajolingHandler {
  public InnocentHandler(BuildInfo buildInfo) {
    super(buildInfo, null /* hostedService */,
        null /* uriFetcher */);
  }
  
  public boolean canHandle(URI uri, CajolingService.Transform transform,
      List<CajolingService.Directive> directives,
      String inputContentType,
      ContentTypeCheck checker) {
    return CajolingService.Transform.INNOCENT.equals(transform)
      && checker.check("text/javascript", inputContentType);
  }

  public Pair<String,String> apply(URI uri,
                                   CajolingService.Transform transform,
                                   List<CajolingService.Directive> directives,
                                   ContentHandlerArgs args,
                                   String inputContentType,
                                   ContentTypeCheck checker,
                                   FetchedData input,
                                   OutputStream response,
                                   MessageQueue mq)
      throws UnsupportedContentTypeException {
    Pair<ContentType, String> contentParams = getReturnedContentParams(args);

    try {
      OutputStreamWriter writer = new OutputStreamWriter(response,
          Charsets.UTF_8.name());
      innocentJs(uri, input.getTextualContent(), writer, contentParams.b, mq);
      writer.flush();
    } catch (IOException e) {
      // TODO(ihab.awad): Fix the "unrecoverable" error responses throughout
      throw new UnsupportedContentTypeException();
    }
    return Pair.pair(contentParams.a.mimeType, Charsets.UTF_8.name());
  }

  private void innocentJs(
      URI inputUri, CharProducer cp, Appendable output,
      String jsonpCallback, MessageQueue mq)
      throws IOException {
    ParseTreeNode result = null;
    InputSource is = new InputSource (inputUri);
    try {
      JsTokenQueue tq = new JsTokenQueue(new JsLexer(cp), is);
      Block input = new Parser(tq, mq).parse();
      tq.expectEmpty();
      Rewriter rw = new InnocentCodeRewriter(mq, false /* logging */);
      result = rw.expand(input);
    } catch (ParseException e) {
      e.toMessageQueue(mq);
    }
    this.renderAsJSON(null, result, jsonpCallback, mq, output);
  }
}
