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

import java.io.IOException;
import java.io.Reader;
import java.net.URI;

import com.google.caja.lexer.FilePosition;
import com.google.caja.util.Pair;

/**
 * Used to resolve references in configuration files.
 *
 * @author mikesamuel@gmail.com
 * @see ConfigUtil#loadWhiteListFromJson
 */
public interface ImportResolver {
  /**
   * @param ref a relative URI or absolute URI.
   * @param base the URI of the containing page.
   * @param refPos the position of ref in base.
   */
  Pair<Reader, FilePosition> resolve(URI ref, URI base, FilePosition refPos)
      throws IOException;
}
