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

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.util.List;
import org.w3c.dom.Document;
import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.FetchedData;
import com.google.caja.lexer.HtmlLexer;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.ParseException;
import com.google.caja.parser.html.Dom;
import com.google.caja.parser.html.DomParser;
import com.google.caja.parser.js.Expression;
import com.google.caja.parser.quasiliteral.QuasiBuilder;
import com.google.caja.plugin.PipelineMaker;
import com.google.caja.plugin.PluginCompiler;
import com.google.caja.plugin.PluginMeta;
import com.google.caja.plugin.UriFetcher;
import com.google.caja.reporting.BuildInfo;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.util.Charsets;
import com.google.caja.util.ContentType;
import com.google.caja.util.Pair;

/**
 * Retrieves html files and cajoles them
 *
 * @author jasvir@google.com (Jasvir Nagra)
 */
public class HtmlHandler extends AbstractCajolingHandler {

  public HtmlHandler(BuildInfo buildInfo, final String hostedService,
      final UriFetcher uriFetcher) {
    super(buildInfo, hostedService, uriFetcher);
  }

  @Override
  public boolean canHandle(URI uri, CajolingService.Transform transform,
      List<CajolingService.Directive> directives,
      String inputContentType, String outputContentType,
      ContentTypeCheck checker) {
    return checker.check("text/html", inputContentType)
        && (checker.check(outputContentType, "text/html")
            || checker.check(outputContentType, "*/*")
            || checker.check(outputContentType, "application/json")
            || checker.check(outputContentType, "text/javascript"));
  }

  @Override
  public Pair<String,String> apply(URI uri,
                                   CajolingService.Transform transform,
                                   List<CajolingService.Directive> directives,
                                   ContentHandlerArgs args,
                                   String inputContentType,
                                   String outputContentType,
                                   ContentTypeCheck checker,
                                   FetchedData input,
                                   OutputStream response,
                                   MessageQueue mq)
      throws UnsupportedContentTypeException {
    PluginMeta meta = new PluginMeta(uriFetcher, uriPolicy);
    ContentType outputType = ContentType.fromMimeType(outputContentType);
    if (outputType == null) {
      if (outputContentType.matches("\\*/\\*(\\s*;.*)?")) {
        outputType = ContentType.HTML;
      } else {
        throw new UnsupportedContentTypeException(outputContentType);
      }
    } else {
      switch (outputType) {
        case JS: case HTML: case JSON: break;
        default:
          throw new UnsupportedContentTypeException(outputContentType);
      }
    }

    String moduleCallbackString = CajaArguments.MODULE_CALLBACK.get(args);
    Expression moduleCallback = (Expression)
        (moduleCallbackString == null
            ? null
            : QuasiBuilder.substV(moduleCallbackString));

    try {
      OutputStreamWriter writer = new OutputStreamWriter(
          response, Charsets.UTF_8);
      cajoleHtml(
          uri, input.getTextualContent(),
          meta, moduleCallback, outputType, writer, mq);
      writer.flush();
    } catch (IOException e) {
      // TODO(mikesamuel): this is not a valid assumption.
      throw new UnsupportedContentTypeException();
    }

    return Pair.pair(outputType.mimeType, Charsets.UTF_8.name());
  }

  private void cajoleHtml(URI inputUri, CharProducer cp, PluginMeta meta,
                          Expression moduleCallback, ContentType outputType,
                          Appendable output, MessageQueue mq) {
    InputSource is = new InputSource (inputUri);
    boolean okToContinue = true;
    try {
      DomParser p = new DomParser(new HtmlLexer(cp), false, is, mq);

      Dom html = new Dom(p.parseFragment());
      Document doc = html.getValue().getOwnerDocument();
      p.getTokenQueue().expectEmpty();

      PluginCompiler compiler = new PluginCompiler(buildInfo, meta, mq);
      if (outputType == ContentType.JS) {
        compiler.setGoals(
            compiler.getGoals().without(PipelineMaker.HTML_SAFE_STATIC));
      }

      compiler.addInput(html, inputUri);
      if (okToContinue) {
        okToContinue &= compiler.run();
      }
      if (outputType == ContentType.JS) {
        renderAsJavascript(okToContinue ? compiler.getJavascript() : null,
            moduleCallback, output);
      } else if (outputType == ContentType.JSON) {
        renderAsJSON(
            okToContinue ? compiler.getStaticHtml() : null,
            okToContinue ? compiler.getJavascript() : null,
            moduleCallback, mq, output);
      } else {
        assert outputType == ContentType.HTML;
        renderAsHtml(doc,
            okToContinue ? compiler.getStaticHtml() : null,
            okToContinue ? compiler.getJavascript() : null,
            moduleCallback, output);
      }
    } catch (ParseException e) {
      e.toMessageQueue(mq);
    } catch (IOException e) {
      mq.addMessage(
          ServiceMessageType.IO_ERROR,
          MessagePart.Factory.valueOf(e.getMessage()));
    }
  }
}
