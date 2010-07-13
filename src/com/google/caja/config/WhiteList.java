// Copyright (C) 2008 Google Inc.
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

package com.google.caja.config;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * A configuration file that defines a schema and specifies which items in that
 * schema are safe to allow.
 *
 * <p>
 * See <a href="http://code.google.com/p/google-caja/wiki/CajaWhitelists">
 * the CajaWhitelists</a> wiki for details.
 *
 * <dl>
 * <dt>Item
 *   <dd>A string that identifies an item that can be allowed or denied.
 * <dt>Type Definition
 &   <dd>MetaData about an item (not necessarily an allowed item).
 * </dl>
 *
 * @author mikesamuel@gmail.com
 */
public interface WhiteList {

  /** An immutable set of the names of all allowed items. */
  Set<String> allowedItems();

  /** An immutable mapping of names to type definition for all items. */
  Map<String, TypeDefinition> typeDefinitions();

  /** An immutable description of an item. */
  public interface TypeDefinition  {
    Object get(String key, Object defaultValue);
  }

  public static final class Factory {
    public static WhiteList empty() {
      return new WhiteList() {
        public Set<String> allowedItems() { return Collections.emptySet(); }
        public Map<String, TypeDefinition> typeDefinitions() {
          return Collections.emptyMap();
        }
      };
    }

    private Factory() { /* uninstantiable */ }
  }
}
