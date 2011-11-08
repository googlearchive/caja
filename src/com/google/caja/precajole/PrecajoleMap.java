// Copyright (C) 2011 Google Inc.
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

package com.google.caja.precajole;

import com.google.caja.parser.js.CajoledModule;

/**
 * This is an interface for storage of previously cajoled modules,
 * retrieved either by URI or by uncajoled source text.
 */

public interface PrecajoleMap {
  /**
   * Returns cajoled JS for the given URI, or null.
   */
  public CajoledModule lookupUri(String uri, boolean minify);

  /**
   * Returns cajoled JS for the given source JS, or null.
   */
  public CajoledModule lookupSource(String source, boolean minify);
}
