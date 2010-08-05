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

import com.google.caja.lexer.ExternalReference;

import java.util.Map;

/**
 * Specifies how to map external resources present in untrusted content
 * or computed by cajoled scripts to URLs which the container can secure.
 *
 * <p>See the
 * <a href="http://code.google.com/p/google-caja/wiki/UriPolicy">UriPolicy</a>
 * wiki page for a discussion of the various kinds of URIs seen in web
 * applications.
 *
 * @author mikesamuel@gmail.com
 */
public interface UriPolicy {

  /**
   * Applies a URI policy and returns a URI that enforces that policy.
   *
   * @param u contains the URI to police.
   * @param effect the effect that loading the URI would have in the context in
   *     which it appears if not rewritten.
   * @param loader the type of loader who would load the URI
   *     (and any rewritten version).
   * @param hints describe the context in which the URI appears.
   *     If a hint is not present it should not be relied upon, but where
   *     available hints can be used to help dispatch events.
   * @see UriPolicyHintKey
   * @return null if the URI cannot be made safe.
   */
  String rewriteUri(
      ExternalReference u, UriEffect effect, LoaderType loader,
      Map<String, ?> hints);

  /** A policy that denies all URIs. */
  public static final UriPolicy DENY_ALL = new UriPolicy() {
    public String rewriteUri(
        ExternalReference u, UriEffect effect, LoaderType loader,
        Map<String, ?> hints) {
      return null;
    }
  };

  /** Leaves URIs unchanged. */
  public static final UriPolicy IDENTITY = new UriPolicy() {
    public String rewriteUri(
        ExternalReference u, UriEffect effect, LoaderType loader,
        Map<String, ?> hints) {
      return u.getUri().toString();
    }
  };

  /** Explains the effect that allowing a URI to load has. */
  public enum UriEffect {
    /** Describes a URI that is not loaded.  E.g. {@code <base href>}. */
    NOT_LOADED,
    /**
     * Describes a URI that is automatically loaded into the current document's
     * origin.
     * E.g. {@code <img src>}.
     */
    SAME_DOCUMENT,
    /**
     * Describes a URI that is loaded on user interaction, replacing the current
     * document with a new document, and that is loaded into the origin implied
     * in the URI.
     * This may or may not unload the current document.
     */
    NEW_DOCUMENT,
    ;
  }

  /** Explains what kind of entity is loading the URI. */
  public enum LoaderType {
    /**
     * A loader that will automatically interpret the result as code
     * (or that might embed code like CSS or HTML) with access to the
     * document.
     */
    UNSANDBOXED,
    /**
     * A loader that will interpret the result as code without access to the
     * document in which it is embedded.  For example, an image tag or a
     * video player : both automatically interpret structured content that might
     * have dynamic behavior but that do not have access to the embedding
     * document.
     */
    SANDBOXED,
    /**
     * A loader that will receive the result as data and not automatically
     * interpret it.  For example, {@code XMLHttpRequest} receives the result
     * as data.
     */
    DATA,
    ;
  }
}
