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

package com.google.caja.opensocial;

import com.google.caja.lexer.ExternalReference;

import java.io.Reader;
import java.net.URI;

/**
 * A callback that retrieves or rewrites URIs on behalf of a {@link GadgetRewriter}.
 * 
 * @author ihab.awad@gmail.com (Ihab Awad)
 */
public interface UriCallback {

  /**
   * Inform the caller what to do with a content URI.
   *
   * @param extref a reference for some content in a gadget.
   * @param mimeType the MIME type that is expected to be retrieved from that URI,
   * such as {@code text/plain} or {@code image/*}.
   * @return a {@code UriCallbackOption} indicating how to proceed.
   */
  UriCallbackOption getOption(ExternalReference extref, String mimeType);

  /**
   * Retrieve the literal content of the specified URI.
   * Note: Implementations are responsible for checking and complying with
   * the site's robots.txt file.
   *
   * @param extref a reference for some content in a gadget.
   * @param mimeType the MIME type that is expected to be retrieved from the URI,
   * such as {@code text/plain} or {@code image/*}.
   * @return the content returned by a GET on that URI.
   * @exception UriCallbackException if the URI could (or should) not be retrieved. A possible
   * error case is where the server returns some content not matching the expected MIME type,
   * which could indicate an attempted attack.
   */
  Reader retrieve(ExternalReference extref, String mimeType) throws UriCallbackException;

  /**
   * Rewrites a URI, perhaps to point to a proxy.
   *
   * @param extref a reference for some content in a gadget.
   * @param mimeType the MIME type that is expected to be retrieved from the URI,
   * such as {@code text/plain} or {@code image/*}. The rewritten URI may refer
   * to a server that only passes through data of the expected MIME type, in order
   * to shut off certain classes of attacks.
   * @return a rewritten form of the URI.
   * @exception UriCallbackException if the URI could (or should) not be rewritten.
   */
  URI rewrite(ExternalReference extref, String mimeType) throws UriCallbackException;
}
