// Copyright 2008 Google Inc. All Rights Reserved.
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

package com.google.caja.opensocial.service;

import java.util.HashMap;
import java.util.Map;

/**
 * Tests if two content-types denoted the same type of content.
 *
 * A "loose" content-type check is for javascript because while
 * "text/javascript" is recognized by all major browsers it is not a
 * <a href="http://www.iana.org/assignments/media-types/">registered</a> MIME
 * type.
 *
 * Different server return other MIME types for javascript and xml files. This
 * checker maps other variants of content-type to the canonical one.
 *
 * @author jasvir@google.com (Jasvir Nagra)
 */
public class LooseContentTypeCheck extends ContentTypeCheck {

  private final Map<String, String> canonicalMimeType = new HashMap<String,String>();

  public LooseContentTypeCheck () {
    canonicalMimeType.put("application/x-javascript", "text/javascript");
    canonicalMimeType.put("text/xml", "application/xml");
  }

  /**
   * @return true iff {@code candidate} is consistent with {@code spec}
   */
  @Override
  public boolean check(String spec, String candidate) {
    if ("*/*".equals(spec)) { return true; }

    int semi = candidate.indexOf(';');
    if (semi >= 0) { candidate = candidate.substring(0, semi).trim(); }
    String canon = canonicalMimeType.get(candidate);
    if (canon != null) { candidate = canon; }

    if (spec.endsWith("*")) {
      spec = spec.substring(0, spec.length() - 1);
      int slash = candidate.lastIndexOf('/');
      if (slash < 0) { return false; }
      candidate = candidate.substring(0, slash + 1);
    }
    return spec.equals(candidate);
  }
}
