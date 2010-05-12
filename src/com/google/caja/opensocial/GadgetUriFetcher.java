// Copyright (C) 2010 Google Inc.
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

package com.google.caja.opensocial;

import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.ExternalReference;
import com.google.caja.lexer.FetchedData;
import com.google.caja.lexer.InputSource;
import com.google.caja.plugin.UriFetcher;
import com.google.caja.reporting.MessageContext;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.Map;

final class GadgetUriFetcher implements UriFetcher {
  private final MessageContext mc;
  private final Map<? super InputSource, ? super CharSequence> originalSources;

  GadgetUriFetcher(
      MessageContext mc,
      Map<? super InputSource, ? super CharSequence> originalSources) {
    this.mc = mc;
    this.originalSources = originalSources;
  }

  public FetchedData fetch(ExternalReference extref, String mimeType)
      throws UriFetchException {
    URI uri = extref.getUri();
    InputSource is = new InputSource(uri);

    FetchedData data;
    try {
      data = FetchedData.fromConnection(uri.toURL().openConnection());
    } catch (IOException ex) {
      throw new UriFetchException(extref, mimeType, ex);
    }

    try {
      CharProducer resource = data.getTextualContent();
      originalSources.put(
          is, resource.toString(resource.getOffset(), resource.getLength()));
      mc.addInputSource(is);
    } catch (UnsupportedEncodingException ex) {
      // OK -- binary content
    }

    return data;
  }
}
