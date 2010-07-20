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

import com.google.caja.util.Function;

import java.io.File;
import java.io.IOException;
import java.net.URI;

class UriToFile implements Function<URI, File> {
  final File directory;

  public UriToFile(File directory) throws IOException {
    assert directory.isDirectory();
    this.directory = directory.getCanonicalFile();
  }

  public File apply(URI uri) {
    return toFileUnderSameDirectory(uri);
  }

  private File toFileUnderSameDirectory(URI uri) {
    if (uri.isOpaque()) {
      // An opaque URI is not interpretable as a relative path
      return null;
    }

    if (uri.getScheme() != null && !"file".equals(uri.getScheme())) {
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
