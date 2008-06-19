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

/**
 * Strict content-type check checks if the requested content-type
 * is exactly the same as the received content-type
 *
 * @author jasvir@google.com (Jasvir Nagra)
 */
public class StrictContentTypeCheck extends ContentTypeCheck {

  /**
   * Checks if the {@code spec} matches {@code candidate} exactly
   * @return true if the primary and subtype of {@code spec}
   * matches {@code candidate}, else returns false
   */
  @Override
  public boolean check(String spec, String candidate) {
    if ("*/*".equals(spec)) { return true; }

    int semi = candidate.indexOf(';');
    if (semi >= 0) { candidate = candidate.substring(0, semi).trim(); }
    if (spec.endsWith("*")) {
      spec = spec.substring(0, spec.length() - 1);
      int slash = candidate.lastIndexOf('/');
      if (slash < 0) { return false; }
      candidate = candidate.substring(0, slash + 1);
    }
    return spec.equals(candidate);
  }
}
