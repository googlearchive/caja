// Copyright 2007 Google Inc. All Rights Reserved.
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
import java.util.Map;

import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.ExternalReference;
import com.google.caja.lexer.FetchedData;
import com.google.caja.lexer.ParseException;
import com.google.caja.lexer.escaping.UriUtil;
import com.google.caja.opensocial.DefaultGadgetRewriter;
import com.google.caja.opensocial.GadgetRewriteException;
import com.google.caja.plugin.UriFetcher;
import com.google.caja.plugin.UriPolicy;
import com.google.caja.reporting.BuildInfo;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.util.Charsets;
import com.google.caja.util.Pair;

public class GadgetHandler implements ContentHandler {
  private final BuildInfo buildInfo;
  private final UriFetcher uriFetcher;

  public GadgetHandler(BuildInfo buildInfo, UriFetcher uriFetcher) {
    this.buildInfo = buildInfo;
    this.uriFetcher = uriFetcher;
  }

  public boolean canHandle(URI uri, CajolingService.Transform transform,
      List<CajolingService.Directive> directives,
      String inputContentType, String outputContentType,
      ContentTypeCheck checker) {
    return checker.check("application/xml", inputContentType)
        && checker.check(outputContentType, "text/javascript");
  }

  public Pair<String, String> apply(URI uri,
                                    CajolingService.Transform trans,
                                    List<CajolingService.Directive> d,
                                    ContentHandlerArgs args,
                                    String inputContentType,
                                    String outputContentType,
                                    ContentTypeCheck checker,
                                    FetchedData input,
                                    OutputStream response,
                                    MessageQueue mq)
      throws UnsupportedContentTypeException {
    try {
      OutputStreamWriter writer = new OutputStreamWriter(
          response, Charsets.UTF_8);
      cajoleGadget(uri, input.getTextualContent(), writer, mq);
      writer.flush();
      return Pair.pair("text/javascript", Charsets.UTF_8.name());
    } catch (ParseException e) {
      e.printStackTrace();
      throw new UnsupportedContentTypeException();
    } catch (IllegalArgumentException e) {
      e.printStackTrace();
      throw new UnsupportedContentTypeException();
    } catch (IOException e) {
      e.printStackTrace();
      throw new UnsupportedContentTypeException();
    } catch (GadgetRewriteException e) {
      e.printStackTrace();
      throw new UnsupportedContentTypeException();
    }
  }

  private void cajoleGadget(URI inputUri, CharProducer cajaInput,
      Appendable output, MessageQueue mq)
      throws ParseException, GadgetRewriteException, IOException {
    DefaultGadgetRewriter rewriter = new DefaultGadgetRewriter(buildInfo, mq);

    UriFetcher fetcher = uriFetcher;
    if (fetcher == null) { fetcher = UriFetcher.NULL_NETWORK; }
    UriPolicy policy = new UriPolicy() {
      public String rewriteUri(
          ExternalReference u, UriEffect effect, LoaderType loader,
          Map<String, ?> hint) {
        return (
            "http://localhost:8887/?url="
            + UriUtil.encode(u.getUri().toString())
            + "&effect=" + effect + "&loader=" + loader);
      }
    };
    rewriter.rewrite(inputUri, cajaInput, fetcher, policy, "canvas", output);
  }
}
