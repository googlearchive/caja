// Copyright (C) 2007 Google Inc.
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

/**
 * Specifies how the plugin resolves external resources such as scripts and 
 * stylesheets.
 *
 * @author mikesamuel@gmail.com
 */
public interface PluginEnvironment {

  /**
   * Loads an externally resource such as the src of a script tags or
   * a stylesheet.
   *
   * @return null if it could not be loaded.
   */
  CharProducer loadExternalResource(ExternalReference ref, String mimeType);

  /**
   * May be overridden to apply a URI policy and return a URI that enforces that
   * policy.
   *
   * @return null if the URI cannot be made safe.
   */
  String rewriteUri(ExternalReference uri, String mimeType);
  
  /** A plugin environment that will not resolve or rewrite any URI. */
  public static final PluginEnvironment CLOSED_PLUGIN_ENVIRONMENT
      = new PluginEnvironment() {
        public CharProducer loadExternalResource(
          ExternalReference ref, String mimeType) {
          return null;
        }

        public String rewriteUri(ExternalReference uri, String mimeType) {
          return null;
        }
      };
}
