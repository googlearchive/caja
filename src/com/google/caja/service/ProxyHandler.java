// Copyright 2012 Google Inc. All Rights Reserved.
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

import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.ExternalReference;
import com.google.caja.lexer.FetchedData;
import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.ParseException;
import com.google.caja.parser.ParserContext;
import com.google.caja.parser.html.Dom;
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

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

/**
 * Proxies js/css files as json
 * 
 * @author jasvir@google.com (Jasvir Nagra)
 */
public class ProxyHandler extends AbstractCajolingHandler {

  public ProxyHandler(
      BuildInfo buildInfo, UriFetcher uriFetcher) {
    super(buildInfo, null, uriFetcher);
  }

  @Override
  public boolean canHandle(URI uri, CajolingService.Transform transform,
      List<CajolingService.Directive> directives,
      String inputContentType,
      ContentTypeCheck checker) {
    return (checker.check("text/css", inputContentType) 
        || checker.check("text/javascript", inputContentType))
        && (transform == CajolingService.Transform.PROXY);
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
    Pair<ContentType, String> contentParams = getReturnedContentParams(args);

    OutputStreamWriter writer;
    try {
      writer = new OutputStreamWriter(response, Charsets.UTF_8);
      FetchedData result = uriFetcher.fetch(
        new ExternalReference(uri, uri, uri, 
          FilePosition.startOfFile(new InputSource(uri))), "*/*");
      if (checker.check("text/css", result.getContentType()) ||
          checker.check("text/javascript", result.getContentType())) {
        renderAsJSON(
          result.getTextualContent().toString(),
          null,
          contentParams.b, mq, writer, true);
        writer.flush();
        return Pair.pair(result.getContentType(), result.getCharSet());
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
}
