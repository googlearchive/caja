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

import com.google.caja.demos.playground.client.Playground;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.ScriptElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DecoratedTabPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.HorizontalSplitPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.MultiWordSuggestOracle;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.SuggestBox;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;
import com.google.gwt.user.client.ui.VerticalPanel;

import java.util.HashMap;
import java.util.SortedMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * GUI elements of the playground client
 * 
 * @author Jasvir Nagra (jasvir@gmail.com)
 */
public class PlaygroundView {
  private HTML renderPanel;
  private HTML cajoledSource;
  private ListBox compileMessages;
  private ListBox runtimeMessages;
  private DecoratedTabPanel editorPanel;
  private Label version = new Label("Unknown");
  private Playground controller;
  private TextArea sourceText;
  
  public void setVersion(String v) {
    version.setText(v);
  }

  public void selectTab(Tabs tab) {
    editorPanel.selectTab(tab.ordinal());
  }
  
  private Panel createFeedbackPanel() {
    HorizontalPanel feedbackPanel = new HorizontalPanel();
    feedbackPanel.setWidth("100%");
    feedbackPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);
    for (Menu menu : Menu.values()) {
      Anchor menuItem = new Anchor();
      menuItem.setHTML(menu.description);
      menuItem.setHref(menu.url);
      menuItem.setWordWrap(false);
      menuItem.addStyleName("menuItems");
      feedbackPanel.add(menuItem);
      feedbackPanel.setCellWidth(menuItem, "100%");
    }
    return feedbackPanel;
  }
    
  private Panel createLogoPanel() {
    HorizontalPanel logoPanel = new HorizontalPanel();
    VerticalPanel infoPanel = new VerticalPanel();
    Label title = new Label("Caja Playground");
    infoPanel.add(title);
    infoPanel.add(version);
    logoPanel.add(
        new Image("http://cajadores.com/demos/testbed/caja_logo_small.png"));
    logoPanel.add(infoPanel);
    return logoPanel;
  }

  private Panel createSourcePanel() {
    MultiWordSuggestOracle oracle = new MultiWordSuggestOracle();
    for (Example eg : Example.values()) {
      oracle.add(eg.url);
    }
    final SuggestBox addressField = new SuggestBox(oracle);
    addressField.getTextBox().addFocusHandler(new FocusHandler() {
      public void onFocus(FocusEvent event) {
        addressField.showSuggestionList();        
      }
    });
    addressField.setText("http://");
    addressField.setWidth("100%");
    final Button goButton = new Button("Load");
    goButton.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        controller.loadSource(addressField.getText());
      }      
    });
    goButton.setWidth("100%");
    
    final Button cajoleButton = new Button("Cajole");
    cajoleButton.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        runtimeMessages.clear();
        compileMessages.clear();
        cajoledSource.setText("");
        renderPanel.setText("");
        controller.cajole(addressField.getText(), sourceText.getText());
      }      
    });
    cajoleButton.setWidth("100%");
    
    HorizontalPanel addressBar = new HorizontalPanel();
    addressBar.add(addressField);
    addressBar.add(goButton);
    addressBar.add(cajoleButton);
    addressBar.setWidth("95%");
    addressBar.setCellWidth(addressField, "90%");
    
    sourceText = new TextArea();
    sourceText.setText("<script>\n\n</script>");
    
    FlowPanel mainPanel = new FlowPanel();
    mainPanel.add(addressBar);
    mainPanel.add(sourceText);
    sourceText.setSize("95%", "100%");
    mainPanel.setSize("100%", "100%");
    
    return mainPanel;
  }

  private FlowPanel createCajoledSourcePanel() {
    FlowPanel fp = new FlowPanel();
    cajoledSource = new HTML();
    cajoledSource.setSize("100%", "100%");
    cajoledSource.getElement().setClassName("prettyPrint");
    fp.add(cajoledSource);
    return fp;
  }

  private FlowPanel createCompileMessagesPanel() {
    FlowPanel hp = new FlowPanel();
    hp.setSize("100%", "100%");
    compileMessages = new ListBox(true /*multilist box*/);
    compileMessages.setSize("100%", "100%");
    hp.add(compileMessages);
    return hp;
  }

  private ListBox createRuntimeMessagesPanel() {
    runtimeMessages = new ListBox(true /*multilist box*/);
    runtimeMessages.setSize("100%", "100%");
    setupNativeRuntimeMessageBridge();
    return runtimeMessages;
  }
  
  private native void setupNativeRuntimeMessageBridge() /*-{
    var that = this;
    $wnd.___.setLogFunc(function logMessage (msg) {
      that.@com.google.caja.demos.playground.client.ui.PlaygroundView::addRuntimeMessage(Ljava/lang/String;)(msg);
    });
  }-*/;

  private DecoratedTabPanel createEditorPanel() {
    editorPanel = new DecoratedTabPanel();
    editorPanel.add(createSourcePanel(), "Source");
    editorPanel.add(createCajoledSourcePanel(), "Cajoled Source");
    editorPanel.add(createRenderPanel(), "Rendered Result");
    editorPanel.add(createCompileMessagesPanel(), "Compile Warnings/Errors");
    editorPanel.add(createRuntimeMessagesPanel(), "Runtime Warnings/Errors");

    editorPanel.setSize("100%", "100%");
    editorPanel.getDeckPanel().setSize("100%", "100%");

    editorPanel.selectTab(0);
    return editorPanel;
  }
  
  private HTML createRenderPanel() {
    renderPanel = new HTML();
    return renderPanel;
  }
  
  private TreeItem addExampleItem(Map<Example.Type, TreeItem> menu, 
      Example eg) {
    if (!menu.containsKey(eg.type)) {
      TreeItem menuItem = new TreeItem(eg.type.description);
      menu.put(eg.type, menuItem);
    }
    TreeItem egItem = new TreeItem(eg.description);
    menu.get(eg.type).addItem(egItem);
    return egItem;
  }
  
  private DecoratedTabPanel createExamplePanel() {
    DecoratedTabPanel cp = new DecoratedTabPanel();
    Tree exampleTree = new Tree();
    SortedMap<Example.Type, TreeItem> menuMap = new TreeMap<Example.Type, TreeItem>();
    final Map<TreeItem, Example> entryMap =
      new HashMap<TreeItem, Example>();
    
    exampleTree.setTitle("Select an example");
    for (Example eg : Example.values()) {
      TreeItem it = addExampleItem(menuMap, eg);
      entryMap.put(it, eg);
    }
    
    for (TreeItem menuItem : menuMap.values()) {
      exampleTree.addItem(menuItem);
    }
    
    exampleTree.addSelectionHandler(new SelectionHandler<TreeItem>() {
      public void onSelection(SelectionEvent<TreeItem> event) {
        Example eg = entryMap.get(event.getSelectedItem());
        // No associated example - eg. when opening a subtree menu
        if (null == eg) {
          return;
        }
        controller.loadSource(eg.url);
      }
      
    });
    cp.setSize("100%", "auto");
    cp.add(exampleTree, "Examples");
    cp.selectTab(0);
    return cp;
  }
  
  public Panel createMainPanel() {
    HorizontalSplitPanel mainPanel = new HorizontalSplitPanel();
    mainPanel.add(createExamplePanel());
    mainPanel.add(createEditorPanel());
    mainPanel.setSplitPosition("15%");
    return mainPanel;
  }
  
  public PlaygroundView(Playground controller) {
    this.controller = controller;

    // Necessary to make full screen and eliminate scrollbars
    final FlowPanel vp = new FlowPanel();
    vp.add(createFeedbackPanel());
    vp.add(createLogoPanel());
    vp.add(createMainPanel());
    vp.setSize("100%", "100%");
    vp.setHeight(Window.getClientHeight() + "px");
    Window.addResizeHandler(new ResizeHandler() {
      public void onResize(ResizeEvent event) {
        int height = event.getHeight();
        vp.setSize(event.getWidth() + "px", event.getHeight() + "px");
      }
    });
    RootPanel.get().add(vp);
  }

  public void setOriginalSource(String result) {
    if (result == null) {
      sourceText.setText("");
    } else {
      sourceText.setText(result);
    }
  }
  
  public void setCajoledSource(String result) {
    if (result == null) {
      cajoledSource.setText("Nothing to show");
      return;
    }
    cajoledSource.setHTML(prettyPrint(result));
  }
  
  private native String prettyPrint(String result) /*-{
    return $wnd.prettyPrintOne($wnd.indentAndWrapCode(result));
  }-*/;

  public void setRenderedResult(String result) {
    if (result == null) {
      cajoledSource.setText("");
      return;
    }
    String[] htmlAndJs = result.split("<script[^>]*>");
    // remove the </script> tag
    htmlAndJs[1] = htmlAndJs[1].substring(0, htmlAndJs[1].length() - 9);

    renderPanel.setHTML(
        "<div id=\"cajoled-output\" class=\"g___\">\n" + 
          htmlAndJs[0] +
        "</div>\n");

    String cajoled =
      "    var imports = ___.copy(___.sharedImports);\n" +
      "    imports.outers = imports;\n" +
      "    var gadgetRoot = document.getElementById('cajoled-output');\n" +
      "    imports.htmlEmitter___ = new HtmlEmitter(gadgetRoot);\n" +
      "    attachDocumentStub('-g___', " +
      "      { rewrite: function() {return null;} }, imports, gadgetRoot);\n" +
      "    imports.$v = valijaMaker.CALL___(imports.outers);\n" +
      "    ___.getNewModuleHandler().setImports(imports);\n" +
    htmlAndJs[1];
    
    Element el = DOM.createElement("script");
    ScriptElement script = ScriptElement.as(el);
    script.setType("text/javascript");
    script.setInnerText(cajoled);
    renderPanel.getElement().appendChild(script);
    editorPanel.selectTab(2);    
  }
  
  public void addCompileMessage(String item) {
    compileMessages.addItem(item);
  }

  public void addRuntimeMessage(String item) {
    runtimeMessages.addItem(item);
  }
  
  public enum Tabs {
    SOURCE,
    CAJOLED_SOURCE,
    RENDER,
    COMPILE_WARNINGS,
    RUNTIME_WARNINGS,
    TAMING;
  }
}
