// Copyright (C) 2006 Google Inc.
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

package com.google.caja.reporting;

import java.io.IOException;

/**
 * A type of {@link Message message} that knows how to display messages of its
 * type.
 *
 * @author mikesamuel@gmail.com
 */
public interface MessageTypeInt {

  /** Render the message and its parts to out using cues from context. */
  void format(MessagePart[] parts, MessageContext context, Appendable out)
      throws IOException;

  /** The default level of messages of this type. */
  MessageLevel getLevel();

  /** The number of parts required by {@link #format}. */
  int getParamCount();

  String name();
}
