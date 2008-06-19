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

import java.io.IOException;
import java.net.URI;

/**
 * Caja rewriter for the content portion of OpenSocial gadgets. The content portion is
 * essentially an HTML fragment containing scripts and styles. The content is rewritten
 * according to the rules described in
 * <a href="http://google-caja.googlecode.com/svn/trunk/doc/html/cajaOpenSocialGadgetRewriting/index.html">this document.</a>
 *
 * @author ihab.awad@gmail.com (Ihab Awad)
 */
public interface GadgetContentRewriter {

  /**
   * Given the source text of the content area of an OpenSocial gadget specification,
   * return a Caja parsed version of the content area.
   *
   * @param baseUri a URI relative to which URIs embedded in the gadget specification
   * will be interpreted.
   * @param gadgetContent the content of the gadget specification.
   * @param uriCallback a {@link UriCallback} object for resolving URIs.
   * @param output an {@code Appendable} to which the rewriter will write the Caja parsed
   * gadget specification, as a literal string of content.
   * @exception UriCallbackException if the {@code urlCallback} threw an exception.
   * @exception GadgetRewriteException if there was a problem parsing the gadget.
   * @exception IOException if there was an I/O problem.
   */
  void rewriteContent(URI baseUri,
                      Readable gadgetContent,
                      UriCallback uriCallback,
                      Appendable output)
      throws UriCallbackException, GadgetRewriteException, IOException;
}
