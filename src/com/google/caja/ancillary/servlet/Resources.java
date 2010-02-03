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

package com.google.caja.ancillary.servlet;

import com.google.caja.SomethingWidgyHappenedError;
import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.ParseException;
import com.google.caja.reporting.EchoingMessageQueue;
import com.google.caja.reporting.MessageContext;
import com.google.caja.util.ContentType;
import com.google.caja.util.Lists;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Utility for dealing with resources loadable via the classpath.
 *
 * @author mikesamuel@gmail.com
 */
final class Resources {
  static String readOptimized(Class<?> clazz, String resourcePath) {
    CharProducer cp = readRequired(clazz, resourcePath);
    ContentType t = GuessContentType.guess(null, resourcePath, cp);
    EchoingMessageQueue mq = new EchoingMessageQueue(
        new PrintWriter(System.err), new MessageContext(), true);
    URI base = cp.getCurrentPosition().source().getUri();
    Request req = Request.create(Verb.ECHO, null, null);
    req.otype = t;
    Processor p = new Processor(req, mq);
    Job j;
    try {
      j = p.parse(cp.clone(), t, null, base);
    } catch (ParseException ex) {
      ex.toMessageQueue(mq);
      return cp.toString();
    }
    try {
      Content c = p.reduce(p.process(Lists.newArrayList(j)));
      if (c.type == t) {
        return c.getText();
      }
    } catch (IOException ex) {
      ex.printStackTrace();
      return cp.toString();
    }
    return cp.toString();
  }

  static CharProducer readRequired(Class<?> clazz, String resourcePath) {
    try {
      return read(clazz, resourcePath);
    } catch (IOException ex) {
      // This is a resource we know exists -- not derived from user input.
      // So bail if not present.
      throw new SomethingWidgyHappenedError(ex);
    }
  }

  static CharProducer read(Class<?> clazz, String resourcePath)
      throws IOException {
    URL src = clazz.getResource(resourcePath);
    if (src == null) { throw new FileNotFoundException(resourcePath); }
    InputStream in = clazz.getResourceAsStream(resourcePath);
    try {
      return CharProducer.Factory.create(
          new InputStreamReader(in, "UTF-8"), new InputSource(src.toURI()));
    } catch (URISyntaxException ex) {
      throw (IOException) new IOException("Bad path " + resourcePath)
          .initCause(ex);
    }
  }
}
