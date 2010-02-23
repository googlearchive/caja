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

package com.google.caja.service;

import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.ExternalReference;
import com.google.caja.lexer.InputSource;
import com.google.caja.plugin.PluginEnvironment;
import com.google.caja.reporting.BuildInfo;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.util.Lists;
import com.google.caja.util.Pair;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Vector;

/**
 * A cajoling service which proxies connections:<ul>
 *   <li> cajole any javascript
 *   <li> cajoles any gadgets
 *   <li> checks requested and retrieved mime-types
 * </ul>
 *
 * @author jasvir@gmail.com (Jasvir Nagra)
 */
public class CajolingService {
  private static final String DEFAULT_HOST = "http://caja.appspot.com/cajole";

  private List<ContentHandler> handlers = new Vector<ContentHandler>();
  private ContentTypeCheck typeCheck = new LooseContentTypeCheck();
  private String host;
  private PluginEnvironment env;

  public CajolingService() {
    this(BuildInfo.getInstance());
  }

  public CajolingService(BuildInfo buildInfo) {
    this(buildInfo, DEFAULT_HOST);
  }

  public CajolingService(BuildInfo buildInfo, String host) {
    this.host = host;
    this.env = new PluginEnvironment() {

          public CharProducer loadExternalResource(
              ExternalReference extref, String mimeType) {
            try {
              FetchedData data = fetch(extref.getUri());
              return CharProducer.Factory.create(new InputStreamReader(
                  new ByteArrayInputStream(data.getContent()), data.getCharSet()),
                  new InputSource(extref.getUri()));
            } catch (IOException ex) {
              return null;
            }
          }

          public String rewriteUri(ExternalReference extref, String mimeType) {
            return extref.getUri().toString();
          }
        };
    registerHandlers(buildInfo);
  }

  public CajolingService(
      BuildInfo buildInfo, String host, PluginEnvironment env) {
    this.host = host;
    this.env = env;
    registerHandlers(buildInfo);
  }

  /**
   * Main entry point for the cajoling service.
   *
   * @param inputFetchedData the input content. If this is {@code null}, the
   *     service will attempt to fetch the content from the location given by
   *     the {@link CajaArguments#URL} parameter.
   * @param args a set of arguments to the cajoling service.
   * @param mq a message queue into which status and error messages will be
   *     placed. The caller should query for the most severe status of the
   *     messages in this queue to determine the overall success of the
   *     invocation.
   * @return the output content, or {@code null} if a serious error occurred
   *     that prevented the content from being generated.
   */
  @SuppressWarnings("deprecation")
  public FetchedData handle(FetchedData inputFetchedData,
                            ContentHandlerArgs args,
                            MessageQueue mq) {
    String inputUrlString = CajaArguments.URL.get(args);
    URI inputUri;
    if (inputUrlString == null && inputFetchedData == null) {
      mq.addMessage(
          ServiceMessageType.MISSING_ARGUMENT,
          MessagePart.Factory.valueOf(CajaArguments.URL.toString()));
      return null;
    } else if (inputUrlString == null) {
      inputUri = InputSource.UNKNOWN.getUri();
    } else {
      try {
        inputUri = new URI(inputUrlString);
      } catch (URISyntaxException ex) {
        mq.addMessage(
            ServiceMessageType.INVALID_INPUT_URL,
            MessagePart.Factory.valueOf(inputUrlString));
        return null;
      }
    }

    String expectedInputContentType = CajaArguments.INPUT_MIME_TYPE.get(args);
    if (expectedInputContentType == null) {
      expectedInputContentType = CajaArguments.OLD_INPUT_MIME_TYPE.get(args);
    }
    if (expectedInputContentType == null) {
      mq.addMessage(
          ServiceMessageType.MISSING_ARGUMENT,
          MessagePart.Factory.valueOf(
              CajaArguments.INPUT_MIME_TYPE.toString()));
      return null;
    }

    if (inputFetchedData == null) {
      try {
        inputFetchedData = fetch(inputUri);
      } catch (IOException ex) {
        mq.addMessage(
            ServiceMessageType.CANNOT_FETCH_INPUT_URL,
            MessagePart.Factory.valueOf(inputUri.toString()));
        return null;
      }
    }

    if (!typeCheck.check(
            expectedInputContentType,
            inputFetchedData.getContentType())) {
      mq.addMessage(
          ServiceMessageType.UNEXPECTED_INPUT_MIME_TYPE,
          MessagePart.Factory.valueOf(expectedInputContentType),
          MessagePart.Factory.valueOf(inputFetchedData.getContentType()));
      return null;
    }

    String outputContentType = CajaArguments.OUTPUT_MIME_TYPE.get(args);
    if (outputContentType == null) {
      outputContentType = "*/*";
    }

    String transformName = CajaArguments.TRANSFORM.get(args);
    Transform transform = null;
    if (transformName != null) {
      try {
        transform = Transform.valueOf(transformName);
      } catch (Exception e) {
        mq.addMessage(
            ServiceMessageType.INVALID_ARGUMENT,
            MessagePart.Factory.valueOf(transformName),
            MessagePart.Factory.valueOf(CajaArguments.TRANSFORM.toString()));
        return null;
      }
    }

    // TODO(jasvir): Change CajaArguments to handle >1 occurrence of arg
    String directiveName = CajaArguments.DIRECTIVE.get(args);
    List<Directive> directive = Lists.newArrayList();
    if (directiveName != null) {
      try {
        directive.add(Directive.valueOf(directiveName));
      } catch (Exception e) {
        mq.addMessage(
            ServiceMessageType.INVALID_ARGUMENT,
            MessagePart.Factory.valueOf(directiveName),
            MessagePart.Factory.valueOf(CajaArguments.DIRECTIVE.toString()));
        return null;
      }
    }
    
    ByteArrayOutputStream intermediateResponse = new ByteArrayOutputStream();
    Pair<String, String> contentInfo;
    try {
      contentInfo = applyHandler(
          inputUri,
          transform,
          directive,
          args,
          inputFetchedData.getContentType(),
          outputContentType,
          inputFetchedData.getCharSet(),
          inputFetchedData.getContent(),
          intermediateResponse,
          mq);
    } catch (UnsupportedContentTypeException e) {
      mq.addMessage(ServiceMessageType.UNSUPPORTED_CONTENT_TYPES);
      return null;
    } catch (RuntimeException e) {
      mq.addMessage(
          ServiceMessageType.EXCEPTION_IN_SERVICE, 
          MessagePart.Factory.valueOf(e.toString()));
      return null;
    }

    return new FetchedData(
        intermediateResponse.toByteArray(),
        contentInfo.a,
        contentInfo.b);
  }

  private void registerHandlers(BuildInfo buildInfo) {
    handlers.add(new JsHandler(buildInfo));
    handlers.add(new ImageHandler());
    handlers.add(new GadgetHandler(buildInfo, env));
    handlers.add(new InnocentHandler());
    handlers.add(new HtmlHandler(buildInfo, host, env));
  }

  protected FetchedData fetch(URI uri) throws IOException {
    return new FetchedData(uri);
  }

  private Pair<String, String> applyHandler(
      URI uri, Transform t, List<Directive> d, ContentHandlerArgs args,
      String inputContentType, String outputContentType,
      String charSet, byte[] content, OutputStream response, MessageQueue mq)
      throws UnsupportedContentTypeException {
    for (ContentHandler handler : handlers) {
      if (handler.canHandle(uri, t, d, inputContentType,
          outputContentType, typeCheck)) {
        return handler.apply(uri, t, d, args, inputContentType,
            outputContentType, typeCheck, charSet, content, response, mq);
      }
    }
    throw new UnsupportedContentTypeException();
  }

  public static enum Directive {
    CAJITA,
    STRICT;
  }

  public static enum Transform {
    INNOCENT,
    CAJOLE;
  }
}
