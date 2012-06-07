// Copyright (C) 2012 Google Inc.
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

/**
 * This exception is thrown by {@link Nodes#render} when a DOM tree cannot
 * be safely converted to a String. It's an unchecked exception for
 * backwards compatibility.
 * <p>
 * This is kind of a last-ditch defense.  Most non-malicious cases of
 * "this cannot be rendered safely" can be caught much earlier, at parse
 * time, and it's easier to emit a meaningful error message then.
 * <p>
 * TODO(felix8a): deprecate render() and use a checked exception.
 * TODO(felix8a): parser should report these unrenderable issues.
 *
 * @author felix8a@gmail.com
 */
public class UncheckedUnrenderableException extends RuntimeException {
  public UncheckedUnrenderableException(String string) {
    super(string);
  }
}
