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

import com.sun.web.core.Context;
import com.sun.web.server.HttpServer;
import java.net.InetAddress;
import java.net.URL;

/**
 * A executable that starts a cajoling service which proxies connections:<ul>
 *   <li>cajole any javascript
 *   <li>cajoles any gadgets
 *   <li>checks requested and retrieved mime-types
 * </ul>
 *
 * @author jasvir@gmail.com (Jasvir Nagra)
 */
public class CajolingServiceMain {
  public static void main(String[] args) throws Exception {
    CajolingService service = new CajolingService();
    HttpServer server = new HttpServer(8887, InetAddress.getLocalHost(), null);
    Context context = server.getContext("default");
    context.setDocumentBase(new URL("http://localhost/"));
    context.getContainer().addServlet("/", CajolingService.class);
    server.start();
  }
}
