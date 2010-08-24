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

/**
 * Explains the effect that allowing a URI to load has.
 *
 * @see UriPolicy
 * @author Mike Samuel <mikesamuel@gmail.com>
 */
public enum UriEffect {
  /** Describes a URI that is not loaded.  E.g. {@code <base href>}. */
  NOT_LOADED,
  /**
   * Describes a URI that is automatically loaded into the current document's
   * origin.
   * E.g. {@code <img src>}.
   */
  SAME_DOCUMENT,
  /**
   * Describes a URI that is loaded on user interaction, replacing the current
   * document with a new document, and that is loaded into the origin implied
   * in the URI.
   * This may or may not unload the current document.
   */
  NEW_DOCUMENT,
  ;
}
