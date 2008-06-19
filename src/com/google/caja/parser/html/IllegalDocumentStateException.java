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

package com.google.caja.parser.html;

import com.google.caja.CajaException;
import com.google.caja.reporting.Message;

/**
 * Thrown when an HTML token cannot be incorporated into a document that is
 * in the process of being built.
 *
 * @author mikesamuel@gmail.com
 */
public class IllegalDocumentStateException extends CajaException {
  public IllegalDocumentStateException(Message msg, Throwable cause) {
    super(msg, cause);
  }

  public IllegalDocumentStateException(Message msg) {
    super(msg);
  }
}
