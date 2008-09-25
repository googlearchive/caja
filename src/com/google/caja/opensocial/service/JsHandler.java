// Copyright 2008 Google Inc. All Rights Reserved.
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
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.JsLexer;
import com.google.caja.lexer.JsTokenQueue;
import com.google.caja.lexer.ParseException;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.js.Parser;
import com.google.caja.parser.quasiliteral.CajitaRewriter;
import com.google.caja.parser.quasiliteral.Rewriter;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.SimpleMessageQueue;
import com.google.caja.util.Pair;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.net.URI;

/**
 * Retrieves javascript files and cajoles them
 *
 * @author jasvir@google.com (Jasvir Nagra)
 */
public class JsHandler implements ContentHandler {

  public boolean canHandle(URI uri, String contentType, ContentTypeCheck checker) {
    return checker.check("text/javascript",contentType);
  }

  public Pair<String,String> apply(
      URI uri, String contentType, String charset, byte[] content,
      OutputStream response)
      throws UnsupportedContentTypeException {
    try {
      OutputStreamWriter writer = new OutputStreamWriter(response, "UTF-8");
      cajoleJs(uri, new StringReader(new String(content, charset)), writer);
      writer.flush();
    } catch (IOException e) {
      throw new UnsupportedContentTypeException();
    }
    return new Pair<String, String>("text/javascript", "UTF-8");
  }

  private void cajoleJs(URI inputUri, Reader cajaInput, Appendable output)
      throws UnsupportedContentTypeException {
    InputSource is = new InputSource (inputUri);
    CharProducer cp = CharProducer.Factory.create(cajaInput,is);
    MessageQueue mq = new SimpleMessageQueue();
    try {
      ParseTreeNode input;
      JsLexer lexer = new JsLexer(cp);
      JsTokenQueue tq = new JsTokenQueue(lexer, is);
      Parser p = new Parser(tq, mq);
      input = p.parse();
      tq.expectEmpty();

      CajitaRewriter dcr = new CajitaRewriter(true);
      output.append(Rewriter.format(dcr.expand(input, mq)));
    } catch (ParseException e) {
      throw new UnsupportedContentTypeException();
    } catch (IllegalArgumentException e) {
      throw new UnsupportedContentTypeException();
    } catch (IOException e) {
      throw new UnsupportedContentTypeException();
    }
  }
}
