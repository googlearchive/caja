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

import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessagePart;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Collection;

/**
 * A file of source code.  This is identified by a URI, since it may not
 * correspond to an actual file on disk.  The URI is assumed to uniquely
 * identify the content within a single run of the program, and should be
 * human readable.
 *
 * @author mikesamuel@gmail.com
 */
public final class InputSource implements MessagePart {
  private URI uri;
  private String uriStr;

  public InputSource(File f) {
    this(f.getAbsoluteFile().toURI());
  }

  public InputSource(URI uri) {
    if (!uri.isAbsolute()) {
      throw new IllegalArgumentException(uri.toString());
    }
    this.uri = uri;
    this.uriStr = uri.toString();
  }

  public URI getUri() { return this.uri; }

  /**
   * A descriptive name for error messages which is not ambiguous with any of
   * the given srcs.
   * @param srcs the set of sources the viewer knows about, and that this source
   *     might be ambiguous with.
   */
  public String getShortName(Collection<InputSource> srcs) {
    String s = uriStr;
    // strip the longest common path prefix from the front.
    for (InputSource src : srcs) {
      s = longestCommonPrefix(src.uriStr, s);
    }
    return uriStr.substring(s.lastIndexOf('/') + 1);
  }

  public void format(MessageContext context, Appendable out)
      throws IOException {
    out.append(getShortName(context.inputSources));
  }

  @Override
  public String toString() { return uriStr; }

  @Override
  public boolean equals(Object o) {
    return (o instanceof InputSource)
        && this.uri.equals(((InputSource) o).uri);
  }

  @Override
  public int hashCode() { return this.uri.hashCode(); }

  private static final String longestCommonPrefix(String a, String b) {
    int max = Math.min(a.length(), b.length());
    for (int i = 0; i < max; ++i) {
      if (a.charAt(i) != b.charAt(i)) {
        return a.substring(0, i);
      }
    }
    return a.length() == max ? a : b;
  }
}
