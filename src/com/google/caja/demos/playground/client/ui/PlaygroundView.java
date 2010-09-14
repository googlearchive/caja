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

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import com.google.caja.demos.playground.client.Playground;
import com.google.caja.demos.playground.client.PlaygroundResource;
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
import com.google.gwt.user.client.ui.DisclosurePanel;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.HorizontalSplitPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.MultiWordSuggestOracle;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.SuggestBox;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * GUI elements of the playground client
 *
 * @author Jasvir Nagra (jasvir@gmail.com)
 */
public class PlaygroundView {
  private final boolean EXPERIMENTAL_MODE = false;

  private HTML renderPanel;
  private TextBox renderResult;
  private HTML cajoledSource;
  private FlexTable compileMessages;
  private FlexTable runtimeMessages;
  private Button speedtracerManifestButton;
  private DecoratedTabPanel editorPanel;
  private Label version = new Label("Unknown");
  private Playground controller;
  private PlaygroundEditor sourceText;
  private TextArea policyText;
  private String currentPolicy;
  private HorizontalPanel loadingLabel;
  private SuggestBox addressField;
  private SuggestBox policyAddressField;
  private MultiWordSuggestOracle sourceExamples;
  private MultiWordSuggestOracle policyExamples;
  private RadioButton es53ModeButton;
  private RadioButton valijaModeButton;

  public void setVersion(String v) {
    version.setText(v);
  }

  public void setPolicyUrl(String url) {
    policyAddressField.setText(url);
    policyExamples.add(url);
  }

  public void setUrl(String url) {
    addressField.setText(url);
    sourceExamples.add(url);
  }

  public void selectTab(Tabs tab) {
    editorPanel.selectTab(tab.ordinal());
  }

  private Widget createFeedbackPanel() {
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

  private Widget createLogoPanel() {
    HorizontalPanel logoPanel = new HorizontalPanel();
    VerticalPanel infoPanel = new VerticalPanel();
    Label title = new Label("Caja Playground");
    infoPanel.add(title);
    infoPanel.add(version);
    infoPanel.setStyleName("pg_info");
    logoPanel.add(new Image(PlaygroundResource.INSTANCE.logo().getURL()));
    logoPanel.add(infoPanel);

    loadingLabel = new HorizontalPanel();
    loadingLabel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
    loadingLabel.add(new Label("Loading... "));
    loadingLabel.add(new Image(PlaygroundResource.INSTANCE.loading().getURL()));
    loadingLabel.setStyleName("loadingLabel");
    loadingLabel.setVisible(false);
    logoPanel.add(loadingLabel);
    return logoPanel;
  }
  
  private Widget createSourcePanel() {
    sourceExamples = new MultiWordSuggestOracle();
    for (Example eg : Example.values()) {
      sourceExamples.add(eg.url);
    }
    addressField = new SuggestBox(sourceExamples);
    addressField.getTextBox().addFocusHandler(new FocusHandler() {
      public void onFocus(FocusEvent event) {
        addressField.showSuggestionList();
      }
    });
    addressField.setText("http://");
    addressField.setWidth("100%");
    final Button goButton = new Button("\u21B4\u00A0Load");
    goButton.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        controller.loadSource(addressField.getText());
      }
    });

    final Button cajoleButton = new Button("Cajole\u00A0\u21B1");

    es53ModeButton = new RadioButton("inputLanguage", "ES5");
    es53ModeButton.setTitle("Input in ES5 targetting ES3 browsers");
    valijaModeButton = new RadioButton("inputLanguage", "Valija");
    valijaModeButton.setTitle("Input in Valija targetting ES3 browsers");
    valijaModeButton.setValue(true);

    cajoleButton.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        runtimeMessages.clear();
        compileMessages.clear();
        cajoledSource.setText("");
        renderPanel.setText("");
        controller.cajole(
            addressField.getText(), sourceText.getText(), currentPolicy,
            Boolean.TRUE.equals(es53ModeButton.getValue()));
      }
    });

    Grid addressBar = new Grid(1,5);
    int item = 0;
    addressBar.setStyleName("playgroundUI");
    addressBar.setWidget(0, item, addressField);
    addressBar.getCellFormatter().setWidth(0, item++, "80%");

    addressBar.setWidget(0, item++, goButton);
    addressBar.setWidget(0, item++, es53ModeButton);
    addressBar.setWidget(0, item++, valijaModeButton);
    addressBar.setWidget(0, item++, cajoleButton);
    addressBar.setWidth("95%");

    sourceText = new PlaygroundEditor();
    sourceText.setSize("95%", "100%");

    VerticalPanel mainPanel = new VerticalPanel();
    mainPanel.setWidth("100%");
    mainPanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_TOP);
    mainPanel.add(addressBar);
    mainPanel.setCellHeight(addressBar, "0%");
    mainPanel.add(sourceText);
    mainPanel.setCellHeight(sourceText, "100%");

    return mainPanel;
  }

  private Widget createPolicyPanel() {
    policyExamples = new MultiWordSuggestOracle();
    policyAddressField = new SuggestBox(policyExamples);
    policyAddressField.getTextBox().addFocusHandler(new FocusHandler() {
      public void onFocus(FocusEvent event) {
        policyAddressField.showSuggestionList();
      }
    });
    policyAddressField.setText("http://");
    policyAddressField.setWidth("100%");

    final Button clearButton = new Button("Clear");
    clearButton.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        currentPolicy = "";
        controller.clearPolicy();
      }
    });

    final Button loadButton = new Button("\u21B4\u00A0Load");
    loadButton.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        controller.loadPolicy(policyAddressField.getText());
      }
    });

    final Button applyButton = new Button("Apply\u00A0\u21B1");
    applyButton.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        currentPolicy = policyText.getText();
      }
    });

    Grid addressBar = new Grid(1,4);
    int item = 0;
    addressBar.setStyleName("playgroundUI");
    addressBar.setWidget(0, item, policyAddressField);
    addressBar.getCellFormatter().setWidth(0, item++, "80%");
    addressBar.setWidget(0, item++, loadButton);
    addressBar.setWidget(0, item++, applyButton);
    addressBar.setWidget(0, item++, clearButton);
    addressBar.setWidth("95%");

    policyText = new TextArea();
    setDefaultPolicy(policyText);
    policyText.setSize("95%", "100%");

    VerticalPanel mainPanel = new VerticalPanel();
    mainPanel.setWidth("100%");
    mainPanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_TOP);
    mainPanel.add(addressBar);
    mainPanel.setCellHeight(addressBar, "0%");
    mainPanel.add(policyText);
    mainPanel.setCellHeight(policyText, "100%");

    return mainPanel;
  }

  private void setDefaultPolicy(TextArea policyText) {
    currentPolicy = PlaygroundResource.INSTANCE.defaultPolicy().getText();
    policyText.setText(currentPolicy);
  }

  private Widget createCajoledSourcePanel() {
    FlowPanel fp = new FlowPanel();
    cajoledSource = new HTML();
    cajoledSource.setSize("100%", "100%");
    cajoledSource.getElement().setClassName("prettyPrint");
    fp.add(cajoledSource);
    return fp;
  }

  private Widget createCompileMessagesPanel() {
    FlowPanel hp = new FlowPanel();
    hp.setSize("100%", "100%");
    compileMessages = new FlexTable();
    compileMessages.setWidth("100%");
    hp.add(compileMessages);
    return hp;
  }

  private Widget createRuntimeMessagesPanel() {
    FlowPanel hp = new FlowPanel();
    hp.setSize("100%", "100%");
    runtimeMessages = new FlexTable();
    runtimeMessages.setWidth("100%");
    hp.add(runtimeMessages);
    setupNativeRuntimeMessageBridge();
    return hp;
  }

  native static String encodeURIComponent(String uri) /*-{
    return $wnd.encodeURIComponent(uri);
  }-*/;

  /**
   * Extracts the location map and original source from content cajoled in
   * debug mode.
   * The format is described at <a href=
   * "http://google-caja.googlecode.com/svn/trunk/doc/html/compiledModuleFormat/index.html"
   * ><tt>doc/html/compiledModuleFormat/index.html</tt></a>.
   */
  private static native boolean srcLocMapAndOriginalSrc(
      String source, String[] out) /*-{
    var str = "'(?:[^'\\\\]|\\\\.)*'";
    var colon = "\\s*:\\s*";
    var comma = "\\s*,\\s*";
    var block = str + colon + "\\{(?:\\s*" + str + colon + str + comma + ")*?"
        + "\\s*'content'" + colon + "\\[\\s*"
        + str + "(?:" + comma + str + ")*\\s*\\]\\s*\\}";
    // TODO(mikesamuel): extract this a better way once we're providing module
    // output in an easy to consume JSON format.
    var re = new RegExp(
        // sourceLocationMap in group 1
        "'sourceLocationMap'" + colon + "\\{"
        + "(?:\\s*" + str + colon + str + comma + ")*?"  // any number of pairs
        + "\\s*'content'" + colon + "\\[\\s*(" + str + "(?:" + comma + str
        + ")*)\\s*\\]\\s*\\}" + comma
        // originalSource in group 2
        + "'originalSource'" + colon + "\\{\\s*("
        + block + "(?:" + comma + block
        + ")*)\\s*\\}\\s*\\}\\s*\\)\\s*;?\\s*\\}\\s*<\\/script>\s*$");
    var match = source.match(re);
    if (match) {
      out[0] = match[0];
      out[1] = match[1];
      return true;
    } else {
      return false;
    }
  }-*/;

  private Widget createSpeedtracerPanel() {
    FlowPanel hp = new FlowPanel();
    hp.setSize("100%", "100%");
    speedtracerManifestButton = new Button("Manifest URI", new ClickHandler() {
      PopupPanel panel;
      Label uriLbl;

      private String getManifestUri() {
        String[] locMapAndSrc = new String[2];
        if (srcLocMapAndOriginalSrc(cajoledSource.getText(), locMapAndSrc)) {
          String json = "[[" + locMapAndSrc[0] + "],[" + locMapAndSrc[1] + "]]";
          return "data:text/plain," + encodeURIComponent(json);
        } else {
          return null;
        }
      }

      public void onClick(ClickEvent event) {
        String dataUri = getManifestUri();
        if (panel == null) {
          HorizontalPanel body = new HorizontalPanel();
          body.add(uriLbl = new Label());
          body.add(new Button("\u00d7", new ClickHandler() {
            public void onClick(ClickEvent ev) { panel.hide(); }
          }));
          panel = new PopupPanel();
          panel.setWidget(body);
          panel.setTitle("Manifest URI");
        }
        uriLbl.setText(dataUri);
        if (panel.isShowing()) {
          panel.hide();
        } else {
          panel.show();
        }
      }
    });
    hp.add(speedtracerManifestButton);
    return hp;
  }

  private native void setupNativeRuntimeMessageBridge() /*-{
    var that = this;
    $wnd.caja___.logFunc = function logFunc (msg) {
      that.@com.google.caja.demos.playground.client.ui.PlaygroundView::addRuntimeMessage(Ljava/lang/String;)(msg);
    };
  }-*/;

  private native void setupNativeSelectLineBridge() /*-{
    var that = this;
    $wnd.selectLine = function (uri, start, sOffset, end, eOffset) {
      that.@com.google.caja.demos.playground.client.ui.PlaygroundView::selectTab(Lcom/google/caja/demos/playground/client/ui/PlaygroundView$Tabs;)(
          @com.google.caja.demos.playground.client.ui.PlaygroundView.Tabs::SOURCE);
      that.@com.google.caja.demos.playground.client.ui.PlaygroundView::highlightSource(Ljava/lang/String;IIII)(uri, start, sOffset, end, eOffset);
    }
  }-*/;

  private Widget createEditorPanel() {
    editorPanel = new DecoratedTabPanel();
    editorPanel.setStyleName("clearPadding");
    editorPanel.add(createSourcePanel(), "Source");
    editorPanel.add(createPolicyPanel(), "Policy");
    editorPanel.add(createCajoledSourcePanel(), "Cajoled Source");
    editorPanel.add(createRenderPanel(), "Rendered Result");
    editorPanel.add(createCompileMessagesPanel(), "Compile Warnings/Errors");
    editorPanel.add(createRuntimeMessagesPanel(), "Runtime Warnings/Errors");
    if (EXPERIMENTAL_MODE) {
      editorPanel.add(createSpeedtracerPanel(), "Speedtracer");
    }

    setupNativeSelectLineBridge();
    editorPanel.setSize("100%", "100%");
    editorPanel.getDeckPanel().setSize("100%", "100%");

    selectTab(Tabs.SOURCE);
    return editorPanel;
  }

  private Widget createRenderPanel() {
    DisclosurePanel resultBar = new DisclosurePanel("Eval Result");
    resultBar.setStyleName("playgroundUI");
    renderResult = new TextBox();
    renderResult.setWidth("100%");
    resultBar.add(renderResult);
    resultBar.setWidth("100%");
    renderPanel = new HTML();
    FlowPanel mainPanel = new FlowPanel();
    mainPanel.add(resultBar);
    mainPanel.add(renderPanel);
    renderPanel.setSize("100%", "100%");
    return mainPanel;
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

  private Widget createExamplePanel() {
    DecoratedTabPanel cp = new DecoratedTabPanel();
    cp.setStyleName("clearPadding");
    Tree exampleTree = new Tree();
    Map<Example.Type, TreeItem> menuMap = new TreeMap<Example.Type, TreeItem>();
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
        // No associated example - e.g. when opening a subtree menu
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

  public Widget createMainPanel() {
    HorizontalSplitPanel mainPanel = new HorizontalSplitPanel();
    mainPanel.add(createExamplePanel());
    mainPanel.add(createEditorPanel());
    mainPanel.setSplitPosition("15%");
    mainPanel.setSize("100%", "100%");
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

  public void setPolicySource(String result) {
    if (result == null) {
      policyText.setText("");
    } else {
      policyText.setText(result);
    }
  }

  public void setCajoledSource(String html, String js) {
    if (html == null && js == null) {
      cajoledSource.setText("There were cajoling errors");
      return;
    }
    cajoledSource.setHTML(prettyPrint(html) +
      "&lt;script&gt;" + prettyPrint(js) + "&lt;/script&gt;");
  }

  public void setLoading(boolean isLoading) {
    loadingLabel.setVisible(isLoading);
  }

  private native String prettyPrint(String result) /*-{
    return $wnd.prettyPrintOne($wnd.indentAndWrapCode(result));
  }-*/;

  private ScriptElement scriptOf(String text) {
    Element el = DOM.createElement("script");
    ScriptElement script = ScriptElement.as(el);
    script.setType("text/javascript");
    script.setInnerText(text);
    return script;
  }

  public void setRenderedResult(String policy, String html, String js) {
    if (html == null && js == null) {
      renderPanel.setText("There were cajoling errors");
      return;
    }

    // Make the cajoled content visible so that the DOM will be laid out before
    // the script checks DOM geometry.
    selectTab(Tabs.RENDER);

    setRenderedResultBridge(Boolean.TRUE.equals(es53ModeButton.getValue()),
        renderPanel.getElement(),
        policy, html != null ? html : "", js != null ? js : "");

    renderResult.setText(getRenderResult());
  }
  
  private native void setRenderedResultBridge(boolean es53,
      Element div, String policy, String html, String js) /*-{
    $wnd.caja___.enable(es53, div, policy, html, js);
  }-*/;

  private native String getRenderResult() /*-{
    return "" + $wnd.___.getNewModuleHandler().getLastValue();
  }-*/;

  public void addCompileMessage(String item) {
    compileMessages.insertRow(0);
    compileMessages.setWidget(0, 0, new HTML(item));
  }

  public void addRuntimeMessage(String item) {
    runtimeMessages.insertRow(0);
    runtimeMessages.setWidget(0, 0, new Label(item));
  }

  /** @param uri unused but provided for consistency with native GWT caller. */
  public void highlightSource(String uri, int start, int sOffset, int end, int eOffset) {
    sourceText.setCursorPos(start);
    sourceText.setSelectionRange(start, sOffset, end, eOffset);
  }

  public enum Tabs {
    SOURCE,
    POLICY,
    CAJOLED_SOURCE,
    RENDER,
    COMPILE_WARNINGS,
    RUNTIME_WARNINGS,
    TAMING,
    MANIFEST;
  }
}
