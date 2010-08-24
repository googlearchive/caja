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
 * Explains what kind of entity is loading the URI.
 *
 * @see UriPolicy
 * @author Mike Samuel <mikesamuel@gmail.com>
 */
public enum LoaderType {
  /**
   * A loader that will automatically interpret the result as code
   * (or that might embed code like CSS or HTML) with access to the
   * document.
   */
  UNSANDBOXED,
  /**
   * A loader that will interpret the result as code without access to the
   * document in which it is embedded.  For example, an image tag or a
   * video player : both automatically interpret structured content that might
   * have dynamic behavior but that do not have access to the embedding
   * document.
   */
  SANDBOXED,
  /**
   * A loader that will receive the result as data and not automatically
   * interpret it.  For example, {@code XMLHttpRequest} receives the result
   * as data.
   */
  DATA,
  ;
}