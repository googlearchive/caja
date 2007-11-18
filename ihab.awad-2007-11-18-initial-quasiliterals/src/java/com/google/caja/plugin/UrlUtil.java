// Copyright (C) 2006 Google Inc.
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

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Utilities for validating and rewriting urls.
 *
 * @author mikesamuel@gmail.com (Mike Samuel)
 */
final class UrlUtil {

  /** True iff the given string is a relative url with no domain part. */
  public static boolean isDomainlessUrl(String url) throws URISyntaxException {
    if ("".equals(url)) { return true; }
    return isDomainlessUrl(new URI(url));
  }

  /** True iff the given url is relative with no domain part. */
  public static boolean isDomainlessUrl(URI uri) {
    return !uri.isAbsolute() && null == uri.getRawAuthority()
           && null == uri.getScheme();
  }

  /** Apply a path prefix to the given uri. */
  public static String translateUrl(URI uri, String pathPrefix) {
    assert isDomainlessUrl(uri);
    StringBuilder sb = new StringBuilder(pathPrefix.length() + 64);
    String path = uri.getRawPath();
    sb.append(pathPrefix);
    if (null != path && !"".equals(path)) {
      if (!path.startsWith("/")) { sb.append('/'); }
      sb.append(path);
    }
    String query = uri.getRawQuery();
    if (null != query) {
      sb.append('?').append(query);
    }
    String fragment = uri.getFragment();
    if (null != fragment) {
      sb.append('#').append(fragment);
    }

    return sb.toString();
  }

  private UrlUtil() {
    // uninstantiable
  }
}
