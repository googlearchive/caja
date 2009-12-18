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

package com.google.caja.demos.playground.client.ui;

/**
 * List of caja examples
 * @author jasvir@google.com (Jasvir Nagra)
 */
public enum Example {
  HISTORY("http://www.thinkfu.com/history.html", 
      Type.ATTACK, "Sniffing history"),
  REDIRECTION("http://www.thinkfu.com/redirection.html", 
      Type.ATTACK, "Redirecting the window"),
  COOKIES("http://www.thinkfu.com/cookies.html", 
      Type.ATTACK, "Stealing cookies"),
  LIFE("http://www.thinkfu.com/cajalife/index.php", 
      Type.APPS, "Game of Life"),
  MARKDOWN("http://www.thinkfu.com/markdown.html", 
      Type.APPS, "Markdown Editor"),
  GOOGLE("http://www.google.com/", 
      Type.WEB, "Google"),
  YAHOO("http://www.yahoo.com/", 
      Type.WEB, "Yahoo"),
  ERIGHTS("http://www.erights.org/", 
      Type.WEB, "E-Rights");
  
  public final String url;
  public final Type type;
  public final String description;
  
  Example(String url, Type type, String description) {
    this.url = url;
    this.type = type;
    this.description = description;
  }
  public enum Type {
    WEB("Web pages"),
    APPS("Applications"),
    ATTACK("Attacks"),
    TAMING("Taming");
    
    public final String description;
    
    Type(String description) {
      this.description = description;
    }
  }
}


