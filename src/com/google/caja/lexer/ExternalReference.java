// Copyright (C) 2007 Google Inc.
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

/**
 * A reference to an external resource from an input.
 * <p>
 * In {@code <script src="foo.js"/>} the URI foo.js is an external reference,
 * and the reference position is the position of the text {code "foo.js"}.
 * 
 * @author mikesamuel@gmail.com (Mike Samuel)
 */
public final class ExternalReference {
  private final URI uri;
  private final FilePosition pos;

  /**
   * @param uri the location of the external resource.
   * @param pos the location, within an already parsed input, of the reference
   *     to uri.
   */
  public ExternalReference(URI uri, FilePosition pos) {
    this.uri = uri;
    this.pos = pos;
  }

  /** The external URI. */
  public URI getUri() { return uri; }

  /** The location, within an already parsed input, of the reference. */
  public FilePosition getReferencePosition() { return pos; }

  @Override
  public int hashCode() {
    return uri.hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof ExternalReference)) { return false; }
    ExternalReference that = (ExternalReference) o;
    return this.uri.equals(that.uri);
  }

  @Override
  public String toString() {
    return uri.toString() + " @ " + pos;
  }
}
