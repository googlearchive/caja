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

import com.google.caja.CajaException;
import com.google.caja.lexer.ExternalReference;
import com.google.caja.lexer.FetchedData;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessagePart;

/**
 * Specifies how the cajoler resolves external resources such as scripts and
 * stylesheets in the code being cajoled.
 *
 * @author mikesamuel@gmail.com
 */
public interface UriFetcher {

  /**
   * Loads an external resource such as the {@code src} of a {@code script}
   * tag or a stylesheet.
   *
   * @return null if the resource could not be loaded.
   */
  FetchedData fetch(ExternalReference ref, String mimeType)
      throws UriFetchException;

  /** A fetcher that will not load any URI. */
  public static final UriFetcher NULL_NETWORK = new UriFetcher() {
        public FetchedData fetch(ExternalReference ref, String mimeType)
            throws UriFetchException {
          throw new UriFetchException(ref, mimeType);
        }
      };

  public static class UriFetchException extends CajaException {
    public ExternalReference ref;
    public String expectedMimeType;

    public UriFetchException(
        ExternalReference ref, String mimeType, Throwable cause) {
      super(
          new Message(
              PluginMessageType.FAILED_TO_LOAD_EXTERNAL_URL,
              ref.getReferencePosition(),
              MessagePart.Factory.valueOf(ref.getUri().toString())),
          cause);
      this.ref = ref;
      this.expectedMimeType = mimeType;
    }

    public UriFetchException(ExternalReference ref, String mimeType) {
      this(ref, mimeType, null);
    }
  }
}
