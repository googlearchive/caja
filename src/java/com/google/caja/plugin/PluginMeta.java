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

/**
 * For a plugin, determines how its external dependencies are translated.
 */
public final class PluginMeta {
  /** A CSS&HTML identifier prefix used to namespace ids and classes. */
  public final String namespacePrefix;
  /** Used to generate names that are unique within the plugin's namespace. */
  private int guidCounter;
  /** Describes how resources external to the plugin definition are resolved. */
  private final PluginEnvironment env;

  public PluginMeta(String namespacePrefix) {
    this(namespacePrefix, PluginEnvironment.CLOSED_PLUGIN_ENVIRONMENT);
  }

  public PluginMeta(String namespacePrefix, PluginEnvironment env) {
    if (null == namespacePrefix || env == null) {
      throw new NullPointerException();
    }
    this.namespacePrefix = namespacePrefix;
    this.env = env;
  }

  /**
   * Generates a name that can be used as an identifier in the plugin's
   * namespace.
   * @param prefix a valid javascript identifier prefix.
   */
  public String generateUniqueName(String prefix) {
    return prefix + "_" + (++guidCounter) + "___";
  }

  public PluginEnvironment getPluginEnvironment() { return env; }
}
