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

package com.google.caja.html;

/**
 * HtmlWhitelist is an interface that defines methods required by HtmlParser for
 * looking up accepted HTML elements and attributes.
 *
 * @author Sammy Leong
 */
public interface HtmlWhitelist {
  /**
   * Looks up the HTML.Element object associated with the given element tag
   * name.
   *
   * @param name The tag name of the element to lookup
   * @return The HTML.Element object associated with the given element tag name,
   * or null if the given name is not in the whitelist.
   */
  public HTML.Element lookupElement(String name);

  /**
   * Looks up the HTML.Attribute object associated with the given attribute
   * name.
   *
   * @param name The name of the attribute to lookup
   * @return The HTML.Attribute object associated with the given attribute name,
   * or null if the given name is not in the whitelist.
   */
  public HTML.Attribute lookupAttribute(String name);
}
