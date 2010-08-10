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

package com.google.caja.reporting;

/**
 * Explains the dialect of markup to use for output.
 */
public enum MarkupRenderMode {
  /**
   * Render as HTML, assuming that CDATA tags like {@code <script>} have raw
   * content.
   */
  HTML,
  /**
   * Render as XML, either encoding or using CDATA sections
   * ({@code <![[CDATA[...]]>}) for content that would be raw in HTML, and
   * not using any entities not defined for all of XML such as {@code &apos;}.
   */
  XML,
  /**
   * Render as HTML, but make sure to be backwards compatible with HTML4
   * quirks such as disallowing values on boolean attributes like
   * the {@code checked} attribute of {@code <input>}.
   */
  HTML4_BACKWARDS_COMPAT,
  ;
}
