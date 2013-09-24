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

import java.util.List;
import com.google.caja.CajaException;
import com.google.caja.lexer.ExternalReference;
import com.google.caja.lexer.FetchedData;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessagePart;
import com.google.common.collect.Lists;

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
   * @return non-null resource
   * @throws UriFetchException if the resource could not be loaded
   */
  FetchedData fetch(ExternalReference ref, String mimeType)
      throws UriFetchException;

  /** A fetcher that will not load any URI. */
  public static final UriFetcher NULL_NETWORK = new DataUriFetcher();

  public static class UriFetchException extends CajaException {
    private static final long serialVersionUID = -7915784512753732116L;
    public final ExternalReference ref;
    public final String expectedMimeType;

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

    public UriFetchException(
        ExternalReference ref, String mimeType, List<Throwable> causes) {
      this(ref, mimeType, causes.isEmpty() ? null : causes.get(0));
    }

    public UriFetchException(ExternalReference ref, String mimeType) {
      this(ref, mimeType, (Throwable) null);
    }
  }

  /**
   * Chains one of more uri fetchers in order and returns the first
   * successfully loaded resource
   */
  public final static class ChainingUriFetcher {
    public static UriFetcher make(final UriFetcher... fetchers) {
      return new UriFetcher() {
        List<Throwable> causes = Lists.newArrayList();
        public FetchedData fetch(ExternalReference ref, String mimeType)
            throws UriFetchException {
          for (UriFetcher fetcher : fetchers) {
            try {
              return fetcher.fetch(ref, mimeType);
            } catch (UriFetchException e) {
              causes.add(e);
            }
          }
          // None of the fetchers succeeded
          throw new UriFetchException(ref, mimeType, causes);
        }
      };
    }
  }

}
