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
import java.io.Reader;
import java.net.URI;

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
          newReader(f), // new InputStreamReader(new FileInputStream(f), "UTF-8"),
          new InputSource(f.toURI()));
    } catch (FileNotFoundException ex) {
      return null;
    }
  }

  public String rewriteUri(ExternalReference ref, String mimeType) {
    File f = toFileUnderSameDirectory(ref.getUri());
    if (f == null) {
      String uristr = ref.getUri().toString();
      if (uristr.equals("#")) { return uristr; }
      else { return null; }
    }
    return new File(directory, ".").toURI().relativize(f.toURI()).toString();
  }

  protected abstract Reader newReader(File f) throws FileNotFoundException;

  private File toFileUnderSameDirectory(URI uri) {
    if (!uri.isAbsolute()
        && !uri.isOpaque()
        && uri.getScheme() == null
        && uri.getAuthority() == null
        && uri.getFragment() == null
        && uri.getPath() != null
        && uri.getQuery() == null) {
      File f = new File(new File(directory, ".").toURI().resolve(uri));
      // Check that f is a descendant of directory
      for (File tmp = f; tmp != null; tmp = tmp.getParentFile()) {
        if (directory.equals(tmp)) {
          return f;
        }
      }
    }
    return null;
  }
}
