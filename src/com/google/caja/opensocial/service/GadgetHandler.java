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

package com.google.caja.opensocial.service;

import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.ExternalReference;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.ParseException;
import com.google.caja.opensocial.DefaultGadgetRewriter;
import com.google.caja.opensocial.GadgetRewriteException;
import com.google.caja.opensocial.UriCallback;
import com.google.caja.opensocial.UriCallbackOption;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.SimpleMessageQueue;
import com.google.caja.reporting.BuildInfo;
import com.google.caja.util.Pair;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;

public class GadgetHandler implements ContentHandler {
  private final BuildInfo buildInfo;

  public GadgetHandler(BuildInfo buildInfo) {
    this.buildInfo = buildInfo;
  }

  public boolean canHandle(URI uri, String contentType, ContentTypeCheck checker) {
    return checker.check("application/xml", contentType);
  }

  public Pair<String, String> apply(URI uri, String contentType, String charSet,
                                    byte[] content, OutputStream response)
      throws UnsupportedContentTypeException {
    try {
      OutputStreamWriter writer = new OutputStreamWriter(response, "UTF-8");
      cajoleGadget(uri, new String(content, charSet), writer);
      writer.flush();
      return new Pair<String, String>("text/javascript", "UTF-8");
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

  private void cajoleGadget(URI inputUri, String cajaInput, Appendable output)
      throws ParseException, GadgetRewriteException, IOException {
    MessageQueue mq = new SimpleMessageQueue();
    DefaultGadgetRewriter rewriter = new DefaultGadgetRewriter(buildInfo, mq);

    UriCallback uriCallback = new UriCallback() {
      public UriCallbackOption getOption(
          ExternalReference extRef, String mimeType) {
        return UriCallbackOption.REWRITE;
      }
      public Reader retrieve(ExternalReference extref, String mimeType) {
        return null;
      }

      public URI rewrite(ExternalReference extref, String mimeType) {
        try {
          return URI.create(
              "http://localhost:8887/?url="
              + URLEncoder.encode(extref.getUri().toString(), "UTF-8")
              + "&mime-type=" + URLEncoder.encode(mimeType, "UTF-8"));
        } catch (UnsupportedEncodingException ex) {
          throw new RuntimeException("UTF-8 should be supported.", ex);
        }
      }
    };

    CharProducer p = CharProducer.Factory.create(
        new StringReader(cajaInput), new InputSource(inputUri));
    rewriter.rewrite(inputUri, p, uriCallback, "canvas", output);
  }
}
