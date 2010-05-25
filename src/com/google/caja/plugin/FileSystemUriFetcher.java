// Copyright (C) 2010 Google Inc.
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
import com.google.caja.lexer.FetchedData;
import com.google.caja.lexer.GuessContentType;
import com.google.caja.lexer.InputSource;
import com.google.caja.util.Charsets;
import com.google.caja.util.ContentType;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URI;

/**
 * @author mikesamuel@gmail.com
 */
abstract class FileSystemUriFetcher implements UriFetcher {
  private final UriToFile uriToFile;

  FileSystemUriFetcher(UriToFile uriToFile) { this.uriToFile = uriToFile; }

  public final FetchedData fetch(ExternalReference ref, String mimeType)
      throws UriFetchException {
    URI uri = ref.getUri();
    File f = uriToFile.apply(uri);
    if (f == null) { throw new UriFetchException(ref, mimeType); }
    try {
      CharProducer cp = CharProducer.Factory.create(
          newReader(f), new InputSource(uri));
      ContentType ct = GuessContentType.guess(null, f.getName(), cp);
      return FetchedData.fromCharProducer(
          cp, ct != null ? ct.mimeType : "", Charsets.UTF_8.name());
    } catch (IOException ex) {
      throw new UriFetchException(ref, mimeType, ex);
    }
  }

  public final byte[] fetchBinary(ExternalReference ref, String mimeType)
      throws UriFetchException {
    File f = uriToFile.apply(ref.getUri());
    if (f == null) { throw new UriFetchException(ref, mimeType); }
    ByteArrayOutputStream out = new ByteArrayOutputStream((int) f.length());
    try {
      InputStream in = newInputStream(f);
      try {
        byte[] buf = new byte[4096];
        for (int n; (n = in.read(buf)) > 0;) { out.write(buf, 0, n); }
      } finally {
        in.close();
      }
    } catch (IOException ex) {
      throw new UriFetchException(ref, mimeType, ex);
    }
    return out.toByteArray();
  }

  protected abstract Reader newReader(File f) throws FileNotFoundException;
  protected abstract InputStream newInputStream(File f)
      throws FileNotFoundException;
}
