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

package com.google.caja.parser.quasiliteral;

import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.ExternalReference;
import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.JsLexer;
import com.google.caja.lexer.JsTokenQueue;
import com.google.caja.lexer.ParseException;
import com.google.caja.lexer.escaping.UriUtil;
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.CajoledModule;
import com.google.caja.parser.js.Parser;
import com.google.caja.parser.js.StringLiteral;
import com.google.caja.parser.js.UncajoledModule;
import com.google.caja.parser.js.ValueProperty;
import com.google.caja.plugin.PluginMeta;
import com.google.caja.plugin.UriFetcher;
import com.google.caja.reporting.BuildInfo;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.MessageType;
import com.google.caja.util.ContentType;
import com.google.caja.util.Lists;
import com.google.caja.util.Maps;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Maintains the mapping from a absolute URI to a module name and the mapping
 * from a module name to the cajoled module.
 *
 * Responsible for retrieving and cajoling the embedded modules if necessary
 *
 * @author maoziqing@gmail.com
 */
public class ModuleManager {
  private final PluginMeta meta;
  private final BuildInfo buildInfo;
  private final UriFetcher uriFetcher;
  private final boolean isValija;
  private final MessageQueue mq;

  /** Maps to indices into {@link #modules}. */
  private final Map<URI, Integer> moduleNameMap = Maps.newHashMap();
  private final List<CajoledModule> modules = Lists.newArrayList();

  public ModuleManager(
      PluginMeta meta, BuildInfo buildInfo, UriFetcher uriFetcher,
      boolean isValija, MessageQueue mq) {
    assert uriFetcher != null;
    this.meta = meta;
    this.buildInfo = buildInfo;
    this.uriFetcher = uriFetcher;
    this.isValija = isValija;
    this.mq = mq;
  }

  public List<CajoledModule> getModuleMap() {
    return Collections.unmodifiableList(modules);
  }

  public PluginMeta getPluginMeta() { return meta; }

  public BuildInfo getBuildInfo() { return buildInfo; }

  public UriFetcher getUriFetcher() { return uriFetcher; }

  public boolean isValija() { return isValija; }

  public MessageQueue getMessageQueue() { return mq; }

  public int appendCajoledModule(URI uri, CajoledModule cajoledModule) {
    if (!uri.isAbsolute() || moduleNameMap.containsKey(uri)) {
      throw new IllegalArgumentException(uri.toString());
    }
    int index = modules.size();
    moduleNameMap.put(uri, index);
    modules.add(cajoledModule);
    return index;
  }

  /**
   * Look up the module URL in the local map
   * Retrieve the module if necessary
   * Return the index of the module in the local list
   *
   * @return -1 if error occurs
   */
  public int getModule(URI baseUri, StringLiteral src) {
    String uriStr;
    try {
      uriStr = UriUtil.normalizeUri(src.getUnquotedValue());
    } catch (URISyntaxException ex) {
      mq.addMessage(
          RewriterMessageType.INVALID_MODULE_URI,
          src.getFilePosition(),
          MessagePart.Factory.valueOf(src.getUnquotedValue()));
      return -1;
    }
    // Add a .js extension to the path component if there is none.
    URI relUri = URI.create(uriStr);
    String path = relUri.getRawPath();
    if (path != null && path.indexOf('.', path.lastIndexOf('/') + 1) < 0) {
      int n = uriStr.length();
      int fragmentStart = uriStr.lastIndexOf('#');
      if (fragmentStart < 0) { fragmentStart = n; }
      int queryStart = uriStr.lastIndexOf('?', fragmentStart);
      if (queryStart < 0) { queryStart = fragmentStart; }
      relUri = URI.create(
          uriStr.substring(0, queryStart) + ".js"
          + uriStr.substring(queryStart));
    }

    URI absoluteUri = null;
    if (baseUri != null) {
      try {
        absoluteUri = UriUtil.resolve(baseUri, relUri.toString());
      } catch (URISyntaxException ex) {
        // handled below.
      }
    }
    if (absoluteUri == null) {
      mq.addMessage(
          RewriterMessageType.INVALID_MODULE_URI,
          src.getFilePosition(),
          MessagePart.Factory.valueOf(src.getUnquotedValue()));
      return -1;
    }

    if (moduleNameMap.containsKey(absoluteUri)) {
      return moduleNameMap.get(absoluteUri);
    }

    ExternalReference er = new ExternalReference(
        absoluteUri, src.getFilePosition());

    CharProducer cp;
    try {
      cp = this.uriFetcher.fetch(er, ContentType.JS.mimeType)
          .getTextualContent();
    } catch (UriFetcher.UriFetchException ex) {
      ex.toMessageQueue(mq);
      mq.addMessage(
          RewriterMessageType.MODULE_NOT_FOUND,
          src.getFilePosition(),
          MessagePart.Factory.valueOf(src.getUnquotedValue()));
      return -1;
    } catch (UnsupportedEncodingException ex) {
      mq.addMessage(
          MessageType.IO_ERROR,
          MessagePart.Factory.valueOf(ex.toString()));
      return -1;
    }

    try {
      InputSource is = cp.getCurrentPosition().source();
      JsTokenQueue tq = new JsTokenQueue(new JsLexer(cp), is);
      Block input = new Parser(tq, mq).parse();
      tq.expectEmpty();

      CajoledModule cajoledModule = null;

      if (meta.getEnableES53()) {
        ES53Rewriter rewriter = new ES53Rewriter(absoluteUri, this, false);
        cajoledModule = (CajoledModule)
            rewriter.expand(new UncajoledModule(input));
      } else {
        Block intermediate = isValija ?
          (Block) new DefaultValijaRewriter(mq).expand(input) : input;
        CajitaRewriter cr = new CajitaRewriter(absoluteUri, this, false);
        UncajoledModule uncajoledModule = new UncajoledModule(intermediate);
        cajoledModule = (CajoledModule) cr.expand(uncajoledModule);
      }

      // Attach the name to the cajoledModule so that we can thread cache keys
      // through with cajoled modules.
      FilePosition unk = FilePosition.UNKNOWN;
      cajoledModule.getModuleBody().appendChild(new ValueProperty(
          StringLiteral.valueOf(unk, "src"),
          StringLiteral.valueOf(unk, "" + absoluteUri)));

      return appendCajoledModule(absoluteUri, cajoledModule);
    } catch (ParseException e) {
      e.toMessageQueue(mq);
      mq.addMessage(
          RewriterMessageType.PARSING_MODULE_FAILED,
          src.getFilePosition(),
          MessagePart.Factory.valueOf(src.getUnquotedValue()));
      return -1;
    }
  }
}
