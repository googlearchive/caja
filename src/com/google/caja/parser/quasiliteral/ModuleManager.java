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
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.JsLexer;
import com.google.caja.lexer.JsTokenQueue;
import com.google.caja.lexer.ParseException;
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.CajoledModule;
import com.google.caja.parser.js.Parser;
import com.google.caja.parser.js.StringLiteral;
import com.google.caja.parser.js.UncajoledModule;
import com.google.caja.plugin.UriFetcher;
import com.google.caja.reporting.BuildInfo;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.MessageType;
import com.google.caja.util.ContentType;
import com.google.caja.util.Maps;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
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
  private final UriFetcher uriFetcher;
  private final BuildInfo buildInfo;
  private final MessageQueue mq;
  private final boolean isValija;

  private final Map<String, Integer> moduleNameMap = Maps.newHashMap();
  private final Map<Integer, CajoledModule> moduleIndexMap = Maps.newHashMap();
  private int moduleCounter = 0;

  public ModuleManager(
      BuildInfo buildInfo, UriFetcher uriFetcher, MessageQueue mq,
      boolean isValija) {
    this.buildInfo = buildInfo;
    this.uriFetcher = uriFetcher;
    this.mq = mq;
    this.isValija = isValija;
  }

  public Map<Integer, CajoledModule> getModuleIndexMap() {
    return moduleIndexMap;
  }

  public void appendUncajoledModule(UncajoledModule uncajoledModule) {
    moduleCounter++;
    CajitaRewriter dcr = new CajitaRewriter(buildInfo, this, mq, false);
    CajoledModule cajoledModule = (CajoledModule) dcr.expand(uncajoledModule);
    moduleIndexMap.put(0, cajoledModule);
  }

  /**
   * Look up the module URL in the local map
   * Retrieve the module if necessary
   * Return the index of the module in the local list
   *
   * Return -1 if error occurs
   */
  public int getModule(StringLiteral src) {
    String loc = src.getUnquotedValue();
    if (!loc.toLowerCase().endsWith(".js")) {
      loc = loc + ".js";
    }

    URI inputUri;
    try {
      inputUri = new URI(loc);
    } catch (URISyntaxException ex) {
      mq.addMessage(
          RewriterMessageType.INVALID_MODULE_URI,
          src.getFilePosition(),
          MessagePart.Factory.valueOf(src.getUnquotedValue()));
      return -1;
    }

    ExternalReference er = new ExternalReference(
        inputUri, src.getFilePosition());

    CharProducer cp;
    try {
      cp = this.uriFetcher.fetch(er, ContentType.JS.mimeType)
          .getTextualContent();
    } catch (UriFetcher.UriFetchException ex) {
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

    String absoluteUri = cp.getCurrentPosition().source().getUri().toString();
    if (moduleNameMap.containsKey(absoluteUri)) {
      return moduleNameMap.get(absoluteUri);
    }

    int cur = moduleCounter;
    moduleCounter++;
    moduleNameMap.put(absoluteUri, cur);

    InputSource is = new InputSource(cp.getCurrentPosition().source().getUri());
    try {
      JsTokenQueue tq = new JsTokenQueue(new JsLexer(cp), is);
      Block input = new Parser(tq, mq).parse();
      tq.expectEmpty();

      Block intermediate;
      if (isValija) {
        DefaultValijaRewriter dvr = new DefaultValijaRewriter(mq);
        intermediate = (Block) dvr.expand(input);
      } else {
        intermediate = input;
      }

      CajitaRewriter dcr =
          new CajitaRewriter(buildInfo, this, mq, false);
      UncajoledModule uncajoledModule = new UncajoledModule(intermediate);
      CajoledModule cajoledModule = (CajoledModule) dcr.expand(uncajoledModule);

      moduleIndexMap.put(cur, cajoledModule);
    } catch (ParseException e) {
      mq.addMessage(
          RewriterMessageType.PARSING_MODULE_FAILED,
          src.getFilePosition(),
          MessagePart.Factory.valueOf(src.getUnquotedValue()));
      return -1;
    }

    return cur;
  }
}
