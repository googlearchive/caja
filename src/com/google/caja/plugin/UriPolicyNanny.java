// Copyright (C) 2012 Google Inc.
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
import com.google.caja.util.Strings;

import java.net.URI;
import java.util.Map;

/**
 * Pre-screen URIs before applying a UriPolicy, so that containers
 * do not have to implement checks for dangerous URIs like javascript:
 *
 * @author felix8a@gmail.com
 */
public class UriPolicyNanny {

  // This matches the logic in domado.js uriRewrite()
  public static String apply(UriPolicy policy, ExternalReference ref,
      UriEffect effect, LoaderType ltype, Map<String, ?> hints) {
    if (policy != null && ref != null) {
      URI uri = ref.getUri();
      if (uri != null) {
        String scheme = uri.getScheme();
        if (scheme == null || isAllowedScheme(scheme)) {
          return policy.rewriteUri(ref, effect, ltype, hints);
        }
      }
    }
    return null;
  }

  private static boolean isAllowedScheme(String scheme) {
    scheme = Strings.lower(scheme);
    return (
        "geo".equals(scheme) ||
        "http".equals(scheme) ||
        "https".equals(scheme) ||
        "mailto".equals(scheme) ||
        "sms".equals(scheme) ||
        "tel".equals(scheme));
  }
}
