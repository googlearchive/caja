// Copyright (C) 2008 Google Inc.
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

package com.google.caja.plugin;

import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.ExternalReference;
import com.google.caja.lexer.InputSource;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * @author mikesamuel@gmail.com
 */
public abstract class FileSystemEnvironment implements PluginEnvironment {
  private final File directory;

  FileSystemEnvironment(File directory) {
    this.directory = directory;
  }

  public CharProducer loadExternalResource(
      ExternalReference ref, String mimeType) {
    File f = toFileUnderSameDirectory(ref.getUri());
    if (f == null) { return null; }
    try {
      return CharProducer.Factory.create(
          newReader(f),
          new InputSource(f.toURI()));
    } catch (IOException ex) {
      return null;
    }
  }

  /** Return a new URI with a different fragment. */
  private URI refragUri(URI uri, String frag) throws URISyntaxException {
    return new URI(uri.getScheme(), uri.getSchemeSpecificPart(), frag);
  }

  public String rewriteUri(ExternalReference ref, String mimeType) {
    try {
      URI fragless = refragUri(ref.getUri(), null);

      // allow uri references within the base directory
      File f = toFileUnderSameDirectory(fragless);
      if (f != null) {
        URI base = new File(directory, ".").toURI();
        URI rel = base.relativize(fragless);
        return refragUri(rel, ref.getUri().getFragment()).toString();
      }

      // allow bare fragments
      URI self = ref.getReferencePosition().source().getUri();
      String uristr = self.relativize(ref.getUri()).toString();
      if (uristr.startsWith("#")) {
        return uristr;
      }
    } catch (URISyntaxException e) { }

    // denied
    return null;
  }

  protected abstract Reader newReader(File f) throws FileNotFoundException;

  private File toFileUnderSameDirectory(URI uri) {
    if (uri.isOpaque()) {
      // An opaque URI is not interpretable as a relative path
      return null;
    }

    if (uri.getScheme() != null &&
        !"file".equals(uri.getScheme())) {
      // Not a "file://..." URL so cannot be relative to a directory
      return null;
    }

    if (uri.getAuthority() != null
        || uri.getFragment() != null
        || uri.getQuery() != null) {
      // URI contains stuff that does not apply to filesystems
      return null;
    }

    if (uri.getPath() == null) {
      // Cannot resolve as a file without a path
      return null;
    }

    File f = new File(new File(directory, ".").toURI().resolve(uri));
    // Check that f is a descendant of directory
    for (File tmp = f; tmp != null; tmp = tmp.getParentFile()) {
      if (directory.equals(tmp)) {
        return f;
      }
    }

    return null;
  }
}
