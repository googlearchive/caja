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

import com.google.caja.demos.playground.client.ui.PlaygroundView;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * Main entry point for GWT-based Caja playground
 *
 * @author Jasvir Nagra (jasvir@gmail.com)
 */
public class Playground implements EntryPoint {
  private PlaygroundView gui;

  private PlaygroundServiceAsync cajolingService =
    GWT.create(PlaygroundService.class);

  public void loadSource(String url) {
    gui.setLoading(true);
    gui.setUrl(url);
    cajolingService.fetch(url, new AsyncCallback<String>() {
      public void onFailure(Throwable caught) {
        gui.setLoading(false);
        gui.addCompileMessage(caught.getMessage());
        gui.selectTab(PlaygroundView.Tabs.COMPILE_WARNINGS);
      }

      public void onSuccess(String result) {
        gui.setLoading(false);
        gui.setOriginalSource(result);
        gui.setCajoledSource("", "");
        gui.selectTab(PlaygroundView.Tabs.SOURCE);
      }
    });
  }

  public void cajole(String uri, String input) {
    gui.setLoading(true);
    cajolingService.cajole(uri, input, new AsyncCallback<CajolingServiceResult>() {
      public void onFailure(Throwable caught) {
        gui.setLoading(false);
        gui.addCompileMessage(caught.getMessage());
      }

      public void onSuccess(CajolingServiceResult result) {
        gui.setLoading(false);
        if (result == null) {
          gui.addCompileMessage("An unknown error occurred");
          gui.selectTab(PlaygroundView.Tabs.COMPILE_WARNINGS);
          return;
        }
        for (String message: result.getMessages()) {
          gui.addCompileMessage(message);
        }
        if (result.getHtml() != null) {
          gui.setCajoledSource(result.getHtml(), result.getJavascript());
          gui.setRenderedResult(result.getHtml(), result.getJavascript());
          gui.selectTab(PlaygroundView.Tabs.RENDER);
        } else {
          gui.setCajoledSource(null, null);
          gui.setRenderedResult(null, null);
          gui.selectTab(PlaygroundView.Tabs.COMPILE_WARNINGS);
        }
      }
    });
  }

  public void onModuleLoad() {
    gui = new PlaygroundView(this);
    gui.setLoading(true);
    cajolingService.getBuildInfo(new AsyncCallback<String>() {
      public void onFailure(Throwable caught) {
        gui.setLoading(false);
        gui.addCompileMessage(caught.getMessage());
        gui.setVersion("Unknown");
      }

      public void onSuccess(String result) {
        gui.setLoading(false);
        gui.setVersion(result);
      }
    });
  }
}
