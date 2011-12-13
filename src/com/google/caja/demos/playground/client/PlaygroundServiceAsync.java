// Copyright (C) 2009 Google Inc.
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

package com.google.caja.demos.playground.client;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * The async counterpart of <code>PlaygroundService</code>.
 *
 * @author Jasvir Nagra (jasvir@gmail.com)
 */
public interface PlaygroundServiceAsync {
  void getBuildInfo(AsyncCallback<String> callback);
  void fetch(String base, String url, AsyncCallback<String> callback);
  void cajole(String base, String uri, String input, boolean debugMode,
      String opt_idClass, AsyncCallback<CajolingServiceResult> asyncCallback);
}
