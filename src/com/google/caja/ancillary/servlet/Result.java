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

package com.google.caja.ancillary.servlet;

import com.google.caja.reporting.MessageQueue;
import com.google.caja.util.Lists;
import com.google.caja.util.Pair;

import java.util.List;

/**
 * Abstracts away a servlet response so the servlet can be easily tested.
 *
 * @author mikesamuel@gmail.com
 */
final class Result {
  /** A response code, e.g. 200 for OK. */
  final int status;
  /** The response content. */
  final Content content;
  /** Any messages that should be reported to the user. */
  final MessageQueue mq;
  /** Extra response headers. */
  final List<Pair<String, String>> headers = Lists.newArrayList();

  Result(int status, Content content, MessageQueue mq) {
    if (content == null) { throw new NullPointerException(); }
    this.status = status;
    this.content = content;
    this.mq = mq;
    if (content.type != null) {
      headers.add(Pair.pair("Content-type", getContentType()));
    }
  }

  String getContentType() {
    if (content.type == null) { return null; }
    String contentType = content.type.mimeType;
    return content.type.isText ? contentType + "; charset=UTF-8" : contentType;
  }
}
