// Copyright (C) 2010 Google Inc.
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


import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.TextArea;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;

/**
 * Editor that is a thin wrapper around CodeMirror
 *
 * @author jasvir@gmail.com (Jasvir Nagra)
 */
public class PlaygroundEditor extends Composite {
  @SuppressWarnings("unused")  // Used in GWT native methods
  private JavaScriptObject editor;
  private TextArea feralTextArea;

  public PlaygroundEditor() {
    feralTextArea = new TextArea();
    initWidget(feralTextArea);
  }

  @Override
  public void onLoad() {
    editor = initialize(feralTextArea.getElement());
  }

  public native JavaScriptObject initialize(Element el) /*-{
    el.focus();
    var jsEditor = $wnd.CodeMirror.fromTextArea(el, {
      parserfile: ["parsecss.js", "tokenizejavascript.js",
        "parsejavascript.js", "parsexml.js", "parsehtmlmixed.js" ],
      stylesheet: ["/css/xmlcolors.css","/css/jscolors.css","/css/csscolors.css"],
      autoMatchParens : true,
      path : '/js/',
      height : '100%',
      textWrapping: false,
      lineNumbers: true,
      breakPoints: true,
    });
    return jsEditor;
  }-*/;

  public native String getText() /*-{
    var e = this.
        @com.google.caja.demos.playground.client.ui.PlaygroundEditor::editor;
    return e.getCode();
  }-*/;

  public native void setText(String html) /*-{
    var e = this.
      @com.google.caja.demos.playground.client.ui.PlaygroundEditor::editor;
    e.setCode(html);
  }-*/;

  public native void setCursorPos(int start) /*-{
    var e = this.
        @com.google.caja.demos.playground.client.ui.PlaygroundEditor::editor;
    e.selectLines(e.nthLine(start), 0);
  }-*/;

  public native void setSelectionRange(int s, int sOffset, int t, int tOffset) /*-{
    var e = this.
        @com.google.caja.demos.playground.client.ui.PlaygroundEditor::editor;
    e.selectLines(e.nthLine(s), sOffset-1, e.nthLine(t), tOffset-1);
  }-*/;
}

enum Lang {
  HTML,
  JS,
  CSS
}


