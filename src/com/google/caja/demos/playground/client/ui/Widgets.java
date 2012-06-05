// Copyright (C) 2011 Google Inc.
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

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.datepicker.client.DatePicker;

public class Widgets {

  private AbsolutePanel gwtShim;

  public Widgets(AbsolutePanel gwtShim) {
    this.gwtShim = gwtShim;
  }

  public String getName() {
    return "constant";
  }

  public Blivit getBlivit(String name) {
    return new Blivit(name);
  }

  public Element getCalendar() {
    try {
      DatePicker g = new DatePicker();
      gwtShim.add(g);
      return g.getElement();
    } catch (Exception e) {
      System.err.println(e);
      return null;
    }
  }
}
