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

import java.io.Serializable;

/**
 * Result of cajoling using the cajoling service.  Contains the static html,
 * css and javascript as well as errors and warnings from cajoling.
 * 
 * @author jasvir@google.com (Jasvir Nagra)
 */
public class CajolingServiceResult implements Serializable {
  private String html;
  private String javascript;
  private String[] messages;
  
  public CajolingServiceResult(String html, String javascript, 
      String[] messages) {
    this.html = html;
    this.javascript = javascript;
    this.messages = messages;
  }
  
  public String getHtml() {
    return html;
  }
  public String getJavascript() {
    return javascript;
  }
  public String[] getMessages() {
    return messages;
  }
}
