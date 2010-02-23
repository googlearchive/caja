// Copyright (C) 2009 Google Inc.
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
import com.google.caja.lexer.InputSource;
import com.google.caja.plugin.PluginEnvironment;
import com.google.caja.reporting.MessageContext;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.util.Map;

public class Callback implements PluginEnvironment {
  private MessageContext mc;
  private Map<InputSource, CharSequence> originalSources;

  public Callback(
      MessageContext mc, Map<InputSource, CharSequence> originalSources) {
    this.mc = mc;
    this.originalSources = originalSources;
  }

  public CharProducer loadExternalResource(
      ExternalReference extref, String mimeType) {
    URI uri = extref.getUri();
    InputSource is = new InputSource(uri);

    CharProducer resource;
    try {
      Reader in = new InputStreamReader(uri.toURL().openStream(), "UTF-8");
      resource = CharProducer.Factory.create(in, is);
    } catch (IOException e) {
      return null;
    }

    originalSources.put(
        is,
        String.valueOf(
            resource.toString(resource.getOffset(), resource.getLength())));
    mc.addInputSource(is);

    return resource;
  }

  public String rewriteUri(ExternalReference extref, String mimeType) {
    return extref.getUri().toString();
  }
}
