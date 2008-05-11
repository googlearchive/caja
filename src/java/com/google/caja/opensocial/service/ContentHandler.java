// Copyright 2007 Google Inc. All Rights Reserved.
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

import com.google.caja.util.Pair;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

/**
 * Loads content from streams it can handle
 * possibly modifying them as it does so
 * 
 * @author jasvir@google.com (Jasvir Nagra)
 */
public interface ContentHandler {
  /**
   * Returns if this content handler can check the given {@code uri} and ensure it has 
   * the correct {@code contentType}.  Testing {@code contentType} equality is done using
   * {@code checker}
   *  
   * @param uri URI of content
   * @param contentType Expected content-type
   * @param checker Used to check whether two content-types are compatible
   * @return true if this content-handler is appropriate for this URL and content-type
   */
  public boolean canHandle(URI uri, String contentType, ContentTypeCheck checker);
  
  /**
   * Reads content from {@code stream} and writes it to {@code response}.
   * Checks to ensure that content has type compatible with {@code contentType}
   * if fetched from {@code uri} 
   * 
   * @param uri URI of content
   * @param contentType Expected content-type
   * @param contentEncoding TODO
   * @param stream reads content from uri
   * @param response writes modified content to user
   * @return the content-type and content-encoding of the resulting output
   */
  public Pair<String, String> apply(URI uri, String contentType, 
      String contentEncoding, String charSet, 
      InputStream stream, OutputStream response)
    throws UnsupportedContentTypeException;
}
