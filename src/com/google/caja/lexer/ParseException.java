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

package com.google.caja.lexer;

import com.google.caja.CajaException;
import com.google.caja.reporting.Message;

public class ParseException extends CajaException {
  private static final long serialVersionUID = 4049620558360901134L;

  public ParseException(Message msg, Throwable cause) {
    super(msg, cause);
  }

  public ParseException(Message msg) {
    super(msg, null);
  }
}
