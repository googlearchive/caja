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
 * Thrown when a {@link com.google.caja.service.ContentHandler} does not receive
 * arguments it needs as part of its
 * {@link com.google.caja.service.ContentHandlerArgs}.
 *
 * TODO(ihab.awad): Would a {@link com.google.caja.reporting.MessageType} be
 * better for this?
 *
 * @author ihab.awad@gmail.com (Ihab Awad)
 */
public class InvalidArgumentsException extends Exception {
  private static final long serialVersionUID = 4477255992360724145L;

  /**
   * @param detail detail information about the exception.
   */
  public InvalidArgumentsException(String detail) {
    super(detail);
  }

  /**
   * @param name the name of the missing argument.
   */
  public static InvalidArgumentsException missing(String name) {
    return new InvalidArgumentsException(
        "Missing argument \"" + name + "\"");
  }

  /**
   * @param name the name of the missing argument.
   * @param reason explanation for why the argument is required.
   */
  public static InvalidArgumentsException missing(String name, String reason) {
    return new InvalidArgumentsException(
        "Missing argument \"" + name + "\": " + reason);
  }

  /**
   * @param name the name of the missing argument.
   * @param suppliedValue the supplied value of the argument.
   * @param reason explanation for why the supplied argument value is invalid.
   */
  public static InvalidArgumentsException invalid(String name,
                                                  String suppliedValue,
                                                  String reason) {
    return new InvalidArgumentsException(
        "Invalid value \"" + suppliedValue + "\" for argument \"" + name
        + "\": " + reason);
  }
}
