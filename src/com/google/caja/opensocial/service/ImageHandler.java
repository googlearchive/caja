// Copyright 2008 Google Inc. All Rights Reserved.
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

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;

/**
 * Retrieves image objects and returns them unchecked
 * to the requester
 *
 * @author jasvir@google.com (Jasvir Nagra)
 */
public class ImageHandler implements ContentHandler {

  public boolean canHandle(URI uri, String contentType, ContentTypeCheck checker) {
    return checker.check("image/*", contentType);
  }

  public Pair<String, String> apply(URI uri,
      String contentType, String charSet,
      byte[] content, OutputStream response)
    throws UnsupportedContentTypeException {
    try {
      response.write(content);
      return new Pair<String,String>(contentType, "");
    } catch (IOException e) {
      throw new UnsupportedContentTypeException();
    }
  }
}
