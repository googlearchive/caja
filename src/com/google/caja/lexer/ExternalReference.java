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

import javax.annotation.Nullable;

/**
 * A reference to an external resource from an input.
 *
 * <p>For example, parsing a file {@code x/y/foo.html} with contents:
 *
 * <pre>
 *   &lt;script src="../z/bar.js"/&gt;
 * </pre>
 *
 * will result in an {@code ExternalReference} with:
 *
 * <ul>
 *
 *   <li>A {@link #getReferencePosition() reference position} which is the span
 *   in {@code foo.html} where the reference occurred. This position contains,
 *   among other things, the URI {@code "x/y/foo.html"}.</li>
 *
 *   <li>The target {@link #getUri() URI}, {@code "../z/bar.js"}.
 *
 * </ul>
 *
 * <p>Note that, in the case of HTML links (as in the above example), the
 * URI is implicitly relative to the reference position; in other words, the
 * author of {@code x/y/foo.html} meant to retrieve a script at
 * {@code "x/z/bar.js"}. The {@code ExternalReference} itself, however, is
 * oblivious to this level of interpretation.
 *
 * @author mikesamuel@gmail.com (Mike Samuel)
 */
public final class ExternalReference {
  private final URI uri;
  private final URI base;
  private final URI relUri;
  private final FilePosition pos;

  public ExternalReference(URI uri, FilePosition pos) {
    this(uri, uri, uri, pos);
  }

  /**
   * @param uri the location of the external resource.
   * @param base base uri of document in which the external reference appears
   * @param relUri location of the resource as it appears in the source
   * @param pos the location, within an already parsed input, of the reference
   *     to uri.
   */
  public ExternalReference(URI uri, URI base, URI relUri, FilePosition pos) {
    this.uri = uri;
    this.base = base;
    this.relUri = relUri;
    this.pos = pos;
  }

  /** The external URI. */
  public URI getUri() { return uri; }

  /** The base URI. */
  public URI getBase() { return base; }

  public URI getUnresolvedUri() { return relUri; }

  /** The location, within an already parsed input, of the reference. */
  public FilePosition getReferencePosition() { return pos; }

  @Override
  public int hashCode() {
    return uri.hashCode();
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (!(o instanceof ExternalReference)) { return false; }
    ExternalReference that = (ExternalReference) o;
    return this.uri.equals(that.uri);
  }

  @Override
  public String toString() {
    return uri.toString() + " @ " + pos;
  }
}
