// Copyright (C) 2005 Google Inc.
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
import com.google.caja.reporting.MessageQueue;

public class CajaException extends Exception {

  private static final long serialVersionUID = 140514940128501163L;
  private Message msg;

  public CajaException(Message msg) { this(msg, null); }

  public CajaException(Message msg, Throwable cause) {
    super(msg.format(new MessageContext()), cause);
    this.msg = msg;
  }

  public void toMessageQueue(MessageQueue q) {
    Throwable cause = getCause();
    if (cause instanceof CajaException) {
      ((CajaException) cause).toMessageQueue(q);
    }
    q.getMessages().add(msg);
  }

  public Message getCajaMessage() { return msg; }
}
