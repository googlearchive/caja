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

import com.google.caja.lexer.ExternalReference;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import java.util.Map;

final class FileSystemUriPolicy implements UriPolicy {
  private final UriToFile uriToFile;

  FileSystemUriPolicy(UriToFile uriToFile) { this.uriToFile = uriToFile; }

  /** Return a new URI with a different fragment. */
  private static URI refragUri(URI uri, String frag) throws URISyntaxException {
    return new URI(uri.getScheme(), uri.getSchemeSpecificPart(), frag);
  }

  public String rewriteUri(
      ExternalReference u, UriEffect effect, LoaderType loader,
      Map<String, ?> hints) {
    URI uri = u.getUri();
    try {
      URI fragless = refragUri(uri, null);

      // allow uri references within the base directory
      File f = uriToFile.apply(fragless);
      if (f != null) {
        URI base = new File(uriToFile.directory, ".").toURI();
        URI rel = base.relativize(fragless);
        return refragUri(rel, uri.getFragment()).toString();
      }

      // allow bare fragments
      URI self = u.getReferencePosition().source().getUri();
      String uristr = self.relativize(uri).toString();
      if (uristr.startsWith("#")) { return uristr; }
    } catch (URISyntaxException e) {
      // return null below
    }

    // denied
    return null;
  }
}
