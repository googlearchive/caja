// Copyright (C) 2005 Google Inc.
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

package com.google.caja.lexer;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * InputSource obtained from a String.
 *
  * <p>TODO(ihab): Delete and replaced with an InputSource with a content: URI.
 */
public final class StringInputSource extends InputSource {

  private static int uriCounter = 1;
  private String content;

  public StringInputSource(String content) {
    super(getSyntheticUri());
    this.content = content;
  }

  public StringInputSource(InputSource src) throws ParseException {
    super(src.getUri());
    this.content = src.readSource();
  }

  private static URI getSyntheticUri() {
    try {
      // TODO: figure out what to put here.  "line_number" works well because
      // errors get output like: "line_number:37+22: [error message]"
      return new URI("rewriterString:///line_number");
    } catch (URISyntaxException e) {
      e.printStackTrace();
      System.err.println("Bad fake URI");
      return null;
    }
  }

  public String getContent() {
    return this.content;
  }

  public char charAt(int pos) {
    return this.content.charAt(pos);
  }

  @Override
  public String readSource() {
    return this.content;
  }
}
