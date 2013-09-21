// Copyright 2013 Google Inc. All Rights Reserved.
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

package com.google.caja.tracing;

import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.ExternalReference;
import com.google.caja.lexer.FetchedData;
import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.ParseException;
import com.google.caja.lexer.TokenConsumer;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.ParserContext;
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.UncajoledModule;
import com.google.caja.service.AbstractCajolingHandler;
import com.google.caja.service.CajolingService;
import com.google.caja.service.ContentHandlerArgs;
import com.google.caja.service.ContentTypeCheck;
import com.google.caja.service.UnsupportedContentTypeException;
import com.google.caja.tracing.TracingRewriter;
import com.google.caja.plugin.PluginMeta;
import com.google.caja.plugin.UriFetcher;
import com.google.caja.plugin.UriPolicy;
import com.google.caja.reporting.BuildInfo;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.MessageType;
import com.google.caja.reporting.RenderContext;
import com.google.caja.util.Callback;
import com.google.caja.util.Charsets;
import com.google.caja.util.ContentType;
import com.google.caja.util.Pair;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URI;
import java.util.List;

/**
 * Retrieves javascript files and adds tracing annotations to them.
 *
 * @author ihab.awad@gmail.com (Ihab Awad)
 */
public class TracingHandler extends AbstractCajolingHandler {

  public TracingHandler(BuildInfo buildInfo, UriFetcher uriFetcher) {
    super(buildInfo, null /* hostedService */, uriFetcher);
  }

  @Override
  public boolean canHandle(URI uri, CajolingService.Transform transform,
      List<CajolingService.Directive> directives, String inputContentType,
      ContentTypeCheck checker) {
    return checker.check("text/javascript", inputContentType)
        && (transform == CajolingService.Transform.TRACING);
  }

  @Override
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
    OutputStreamWriter writer;
    try {
      writer = new OutputStreamWriter(response, Charsets.UTF_8);
      FetchedData result = uriFetcher.fetch(
          new ExternalReference(uri, uri, uri,
              FilePosition.startOfFile(new InputSource(uri))), "*/*");
      if (checker.check("text/javascript", result.getContentType())) {
        addTracingAnnotations(result.getTextualContent(), writer, mq);
        writer.flush();
        return Pair.pair(ContentType.JS.mimeType, Charsets.UTF_8.name());
      }
      return null;
    } catch (UnsupportedEncodingException ex) {
      return null;
    } catch (UriFetcher.UriFetchException ex) {
      return null;
    } catch (IOException e) {
      throw new UnsupportedContentTypeException();
    }
  }

  private void addTracingAnnotations(
      CharProducer input,
      Writer writer,
      MessageQueue mq) {
    try {
      UncajoledModule inputModule =
          new UncajoledModule(
              (Block) new ParserContext(mq)
                  .withInput(input)
                  .withInput(ContentType.JS)
                  .withConfig(new PluginMeta(uriFetcher, UriPolicy.DENY_ALL))
                  .build());
      if (!mq.hasMessageAtLevel(MessageLevel.WARNING)) {
        ParseTreeNode outputNode = new TracingRewriter(mq).expand(inputModule);
        if (!mq.hasMessageAtLevel(MessageLevel.WARNING)) {
          renderOutput(outputNode, writer, mq);
        }
      }
    } catch (ParseException e) {
      mq.addMessage(
          new Message(
              MessageType.PARSE_ERROR,
              MessagePart.Factory.valueOf(e.getMessage())));
    } catch (IOException e) {
      mq.addMessage(
          new Message(
              MessageType.IO_ERROR,
              MessagePart.Factory.valueOf(e.getMessage())));
    }
  }

  private void renderOutput(
      ParseTreeNode outputNode,
      Writer writer,
      final MessageQueue mq) {
    TokenConsumer renderer = outputNode.makeRenderer(
        writer,
        new Callback<IOException>() {
          public void handle(IOException ex) {
            mq.addMessage(
                MessageType.IO_ERROR,
                MessagePart.Factory.valueOf(ex.toString()));
          }
        });
    outputNode.render(new RenderContext(renderer));
    renderer.noMoreTokens();
  }
}