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
    cajolingService.fetch(url, new AsyncCallback<String>() {
      public void onFailure(Throwable caught) {
        gui.addCompileMessage(caught.getMessage());
        gui.selectTab(PlaygroundView.Tabs.COMPILE_WARNINGS);
      }

      public void onSuccess(String result) {
        gui.setOriginalSource(result);
        gui.setCajoledSource("");
        gui.selectTab(PlaygroundView.Tabs.SOURCE);
      }
    });
  }
  
  public void cajole(String uri, String input) {
    cajolingService.cajole(uri, input, new AsyncCallback<String[]>() {
      public void onFailure(Throwable caught) {
        caught.printStackTrace();
      }

      public void onSuccess(String[] result) {
        if (result == null) {
          gui.addCompileMessage("An unknown error occurred");
          gui.selectTab(PlaygroundView.Tabs.COMPILE_WARNINGS);
          return;
        }
        gui.setCajoledSource(result[0]);
        gui.setRenderedResult(result[0]);
        for (int i = 2; i < result.length - 2; i++) {
          gui.addCompileMessage(result[i]);
        }
        gui.selectTab(PlaygroundView.Tabs.RENDER);
      }
    });
  }
  
  public void onModuleLoad() {
    gui = new PlaygroundView(this);
    cajolingService.getBuildInfo(new AsyncCallback<String>() {
      public void onFailure(Throwable caught) {
        gui.addCompileMessage(caught.getMessage());
        gui.setVersion("Unknown");
      }

      public void onSuccess(String result) {
        gui.setVersion(result);
      }      
    });
  }
}
