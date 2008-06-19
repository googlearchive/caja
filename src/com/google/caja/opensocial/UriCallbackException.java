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

import com.google.caja.CajaException;
import com.google.caja.lexer.ExternalReference;
import com.google.caja.plugin.PluginMessageType;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessagePart;

/**
 * Thrown when a URL cannot or should not be retrieved.
 *
 * @author ihab.awad@gmail.com (Ihab Awad)
 */
public class UriCallbackException extends CajaException {
  private final ExternalReference extref;

  public UriCallbackException(
      ExternalReference extref, Throwable th) {
    super(new Message(PluginMessageType.FAILED_TO_LOAD_EXTERNAL_URL,
                      extref.getReferencePosition(),
                      MessagePart.Factory.valueOf(extref.getUri().toString())),
          th);
    this.extref = extref;
  }

  public UriCallbackException(ExternalReference extref) {
    this(extref, null);
  }

  public ExternalReference getExternalReference() { return extref; }
}
