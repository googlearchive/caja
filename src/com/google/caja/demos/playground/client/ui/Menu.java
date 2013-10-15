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
 * Menu options for the top row of the playground page
 * @author jasvir@google.com (Jasvir Nagra)
 */
public enum Menu {
  FEEDBACK("http://spreadsheets.google.com/viewform?" +
      "hl=en&formkey=ckZDVUxsWUx6b21CYlI1S2QxNkg4Umc6MA..",
      "<font color='red'>Tells us what you think</font>"),
  ISSUE("http://code.google.com/p/google-caja/issues/entry",
      "File a bug"),
  HELP("http://code.google.com/p/google-caja/wiki/PlaygroundHelp",
      "Help!");

  public final String url;
  public final String description;

  Menu(String url, String description) {
    this.url = url;
    this.description = description;
  }
}


