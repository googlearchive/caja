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

package com.google.caja.util;

import java.io.IOException;

/**
 * Given any {@code Appendable} and an {@code} exception handler, implements
 * the {@code Appendable} interface in a manner that does not throw.
 *
 * @author ihab.awad@gmail.com (Ihab Awad)
 */
public class HandledAppendable implements Appendable {
  private final Callback<IOException> exHandler;
  private final Appendable delegate;

  public HandledAppendable(Callback<IOException> exHandler,
                           Appendable delegate) {
    this.exHandler = exHandler;
    this.delegate = delegate;
  }

  public HandledAppendable append(CharSequence charSequence) {
    try {
      delegate.append(charSequence);
    } catch (IOException e) {
      exHandler.handle(e);
    }
    return this;
  }

  public HandledAppendable append(CharSequence charSequence, int i, int j) {
    try {
      delegate.append(charSequence, i, j);
    } catch (IOException e) {
      exHandler.handle(e);
    }
    return this;
  }

  public HandledAppendable append(char c) {
    try {
      delegate.append(c);
    } catch (IOException e) {
      exHandler.handle(e);
    }
    return this;
  }
}
