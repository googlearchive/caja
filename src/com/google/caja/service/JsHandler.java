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
import java.io.Writer;
import java.net.URI;
import java.util.List;

import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.FetchedData;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.ParseException;
import com.google.caja.parser.ParserContext;
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.CajoledModule;
import com.google.caja.parser.js.UncajoledModule;
import com.google.caja.parser.quasiliteral.ES53Rewriter;
import com.google.caja.parser.quasiliteral.Rewriter;
import com.google.caja.reporting.BuildInfo;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.MessageType;
import com.google.caja.util.Charsets;
import com.google.caja.util.ContentType;
import com.google.caja.util.Pair;

/**
 * Retrieves javascript files and cajoles them
 *
 * @author jasvir@google.com (Jasvir Nagra)
 */
public class JsHandler extends AbstractCajolingHandler {

  public JsHandler(BuildInfo buildInfo) {
    super(buildInfo, null /* hostedService */,
        null /* uriFetcher */);
  }

  @Override
  public boolean canHandle(URI uri, CajolingService.Transform transform,
      List<CajolingService.Directive> directives, String inputContentType,
      ContentTypeCheck checker) {
    return checker.check("text/javascript", inputContentType)
        && (transform == null || transform == CajolingService.Transform.CAJOLE);
  }

  @Override
  public Pair<String,String> apply(URI uri,
      CajolingService.Transform transform,
      List<CajolingService.Directive> directive,
      ContentHandlerArgs args,
      String inputContentType,
      ContentTypeCheck checker,
      FetchedData input,
      OutputStream response,
      MessageQueue mq)
      throws UnsupportedContentTypeException {
    Pair<ContentType, String> contentParams = getReturnedContentParams(args);

    boolean pretty = CajolingService.RENDER_PRETTY.equals(
        CajaArguments.RENDERER.get(args));

    try {
      OutputStreamWriter writer = new OutputStreamWriter(response,
          Charsets.UTF_8.name());
      cajoleJs(
          uri, input.getTextualContent(), contentParams.b, writer, pretty, mq);
      writer.flush();
    } catch (IOException e) {
      throw new UnsupportedContentTypeException();
    }
    return Pair.pair(contentParams.a.mimeType, Charsets.UTF_8.name());
  }

  private void cajoleJs(URI inputUri,
                        CharProducer cp,
                        String jsonpCallback,
                        Writer output,
                        boolean pretty,
                        MessageQueue mq) {
    CajoledModule cajoledModule = null;
    try {
      Block input = (Block) new ParserContext(mq)
          .withInput(new InputSource(inputUri))
          .withInput(ContentType.JS)
          .withInput(cp)
          .build();

      UncajoledModule ucm = new UncajoledModule(input);
      Rewriter esrw = new ES53Rewriter(buildInfo, mq, false /* logging */);
      cajoledModule = (CajoledModule) esrw.expand(ucm);
    } catch (ParseException e) {
      e.toMessageQueue(mq);
    } catch (IllegalStateException e) {
      mq.addMessage(MessageType.INTERNAL_ERROR,
          MessagePart.Factory.valueOf(e.getMessage()));
    } catch (IOException e) {
      // Message already on queue
    }
    if (mq.hasMessageAtLevel(MessageLevel.ERROR)) {
      cajoledModule = null;
    }
    try {
      renderAsJSON(null, cajoledModule, jsonpCallback, mq, output, pretty);
    } catch (IOException e) {
      // Low level server error; must return HTTP status code
      throw new RuntimeException(e);
    }
  }
}