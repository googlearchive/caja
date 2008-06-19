// Copyright (C) 2008 Google Inc.
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

package com.google.caja.opensocial.service;

/**
 * A executable that starts a cajoling service which proxies connections:
 *      - cajole any javascript
 *      - cajoles any gadgets
 *      - checks requested and retrieved mime-types
 *
 * @author jasvir@gmail.com (Jasvir Nagra)
 */
public class CajolingServiceMain {
  public static void main(String[] args) {
    CajolingService service = new CajolingService();
    service.start();
  }
}
