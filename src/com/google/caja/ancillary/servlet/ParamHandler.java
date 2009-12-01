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

/**
 * Updates the {@link Request} based on a single CGI parameter.
 */
interface ParamHandler {
  /**
   * @param name the CGI parameter name, e.g. foo in {@code ?foo=bar}.
   * @param val the parameter value, e.g. bar in the example above.
   * @param c modified in place.
   */
  void handle(String name, String val, Request c) throws BadInputException;
  String manual();
}

