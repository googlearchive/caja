// Copyright (C) 2011 Google Inc.
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

package com.google.caja.demos.playground.server;

import com.google.caja.demos.playground.client.PlaygroundService;
import com.google.caja.lexer.ExternalReference;
import com.google.caja.lexer.FetchedData;
import com.google.caja.lexer.FilePosition;
import com.google.caja.parser.ParseTreeNode.ReflectiveCtor;
import com.google.caja.plugin.DataUriFetcher;
import com.google.caja.plugin.UriFetcher;
import com.google.caja.plugin.UriFetcher.ChainingUriFetcher;
import com.google.caja.reporting.BuildInfo;
import com.google.caja.reporting.MessageLevel;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Implements the GWT version of the cajoling service
 * @author jasvir@google.com (Jasvir Nagra)
 */
@SuppressWarnings("serial")
public class GWTCajolingServiceImpl extends RemoteServiceServlet
    implements PlaygroundService {
  private final UriFetcher fetcher;

  public GWTCajolingServiceImpl(UriFetcher fetcher) {
    this.fetcher = fetcher;
  }

  @ReflectiveCtor
  public GWTCajolingServiceImpl() {
    this(ChainingUriFetcher.make(
        new DataUriFetcher(),
        new UriFetcher() {
          public FetchedData fetch(ExternalReference ref, String mimeType)
              throws UriFetchException {
            try {
              HttpURLConnection conn = (HttpURLConnection)
                  ref.getUri().toURL().openConnection();
              // appengine has a caching http proxy; this limits it
              conn.setRequestProperty("Cache-Control", "max-age=10");
              return FetchedData.fromConnection(conn);
            } catch (ClassCastException ex) {
              throw new UriFetchException(ref, mimeType, ex);
            } catch (IOException ex) {
              throw new UriFetchException(ref, mimeType, ex);
            }
          }
        }));
  }

  public static String[] getMessageLevels() {
    MessageLevel[] values = MessageLevel.values();
    String[] result = new String[values.length];
    for (int i = 0; i < values.length; i++) {
      result[i] = values[i].name();
    }
    return result;
  }

  public String getBuildInfo() {
    return BuildInfo.getInstance().getBuildInfo();
  }

  public String fetch(String base, String uri) {
    try {
      URI relUri = new URI(uri);
      URI absUri = null;
      URI baseAddress = new URI(base);
      absUri = relUri.isAbsolute() ? relUri : baseAddress.resolve(relUri);
      return fetcher.fetch(
          new ExternalReference(absUri, baseAddress, relUri,
              FilePosition.UNKNOWN), "*/*").getTextualContent().toString();
    } catch (URISyntaxException ex) {
      return null;
    } catch (UnsupportedEncodingException ex) {
      return null;
    } catch (UriFetcher.UriFetchException ex) {
      return null;
    }
  }
}
