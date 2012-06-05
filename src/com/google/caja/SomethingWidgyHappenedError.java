// Copyright (C) 2009 Google Inc.
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

package com.google.caja;

import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessageContext;

/**
 * Parent class of all caja runtime exceptions.
 * Only thrown when the cajoler encounters an irrecoverable situation.
 * This exception should not be caught by the cajoler.
 *
 * @author jasvir@google.com (Jasvir Nagra)
 */
public class SomethingWidgyHappenedError extends RuntimeException {

  private static final long serialVersionUID = 115104119101116104L;

  public SomethingWidgyHappenedError() { super(); }
  public SomethingWidgyHappenedError(Message msg) { this(msg, null); }
  public SomethingWidgyHappenedError(String msg) { super(msg); }
  public SomethingWidgyHappenedError(Throwable t) { super(t); }
  public SomethingWidgyHappenedError(String m, Throwable t) { super(m, t); }

  public SomethingWidgyHappenedError(Message msg, Throwable cause) {
    super(msg.format(new MessageContext()), cause);
  }
}
