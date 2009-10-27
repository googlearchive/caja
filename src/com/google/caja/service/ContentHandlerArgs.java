// Copyright 2009 Google Inc. All Rights Reserved.
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

package com.google.caja.service;

/**
 * Encapsulates a set of arguments supplied to a content handler.
 *
 * @author ihab.awad@gmail.com (Ihab Awad)
 */
public abstract class ContentHandlerArgs {
  /**
   * Get the value of an argument.
   *
   * @param name the name of the argument.
   * @return the argument value, or {@code null}.
   */
  public abstract String get(String name);

  /**
   * Get the value of an argument.
   *
   * @param name the name of the argument.
   * @param required whether the argument is required. If {@code true} and the
   * parameter is missing, this method throws a
   * {@link com.google.caja.service.InvalidArgumentsException}. If
   * {@code false} and the argument is missing, this method returns
   * {@code null}.
   * @return the argument value, or {@code null}.
   */
  public final String get(String name, boolean required)
      throws InvalidArgumentsException {
    String value = get(name);
    if (value == null && required) {
      throw InvalidArgumentsException.missing(name);
    }
    return value;
  }
}