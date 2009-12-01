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

import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.InputSource;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Utility for dealing with resources loadable via the classpath.
 *
 * @author mikesamuel@gmail.com
 */
final class Resources {
  static CharProducer readRequired(Class<?> clazz, String resourcePath) {
    try {
      return read(clazz, resourcePath);
    } catch (IOException ex) {
      // This is a resource we know exists -- not derived from user input.
      // So bail if not present.
      throw new RuntimeException(ex);
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
