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

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import com.google.caja.demos.playground.client.Playground;
import com.google.caja.demos.playground.client.PlaygroundResource;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.MultiWordSuggestOracle;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.SuggestBox;
import com.google.gwt.user.client.ui.TreeItem;

/**
 * GUI elements of the playground client
 *
 * @author Jasvir Nagra (jasvir@gmail.com)
 */
public class PlaygroundView {
  private final Playground controller;
  private final MultiWordSuggestOracle sourceExamples;
  private MultiWordSuggestOracle policyExamples;

  private final PlaygroundUI playgroundUI;

  private int idSeq = 0;
  private Boolean mode;

  private String genId() {
    return "CajaGadget" + (idSeq++) + "___";
  }

  public void setVersion(String v) {
    playgroundUI.version.setText(v);
  }

  public void setPolicyUrl(String url) {
    playgroundUI.policyAddressField.setText(url);
    policyExamples.add(url);
  }

  public void setUrl(String url) {
    playgroundUI.addressField.setText(url);
    sourceExamples.add(url);
  }

  public void selectTab(Tabs tab) {
    playgroundUI.editorPanel.selectTab(tab.ordinal());
  }

  private void initSourcePanel() {
    for (Example eg : Example.values()) {
      sourceExamples.add(eg.url);
    }
    playgroundUI.addressField.getTextBox().addFocusHandler(new FocusHandler() {
      public void onFocus(FocusEvent event) {
        playgroundUI.addressField.showSuggestionList();
      }
    });
    playgroundUI.addressField.setText("https://");

    playgroundUI.goButton.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        controller.loadSource(playgroundUI.addressField.getText());
      }
    });
    playgroundUI.cajoleButton.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        playgroundUI.runtimeMessages.clear();
        playgroundUI.compileMessages.clear();
        playgroundUI.cajoledSource.setText("");
        playgroundUI.renderPanel.setText("");
        controller.cajole(
            playgroundUI.addressField.getText(),
            playgroundUI.sourceText.getText(),
            playgroundUI.policyText.getText(),
            true /* debug */,
            mode /* es5 */,
            genId()
        );
      }
    });
  }

  private void initFeedbackPanel() {
    playgroundUI.feedbackPanel.setHorizontalAlignment(
        HasHorizontalAlignment.ALIGN_RIGHT);
    for (Menu menu : Menu.values()) {
      Anchor menuItem = new Anchor();
      menuItem.setHTML(menu.description);
      menuItem.setHref(menu.url);
      menuItem.setWordWrap(false);
      menuItem.addStyleName("menuItems");
      playgroundUI.feedbackPanel.add(menuItem);
      playgroundUI.feedbackPanel.setCellWidth(menuItem, "100%");
    }
  }

  private void initPolicyPanel() {
    policyExamples = new MultiWordSuggestOracle();
    playgroundUI.policyAddressField = new SuggestBox(policyExamples);
    playgroundUI.policyAddressField.getTextBox().addFocusHandler(new FocusHandler() {
      public void onFocus(FocusEvent event) {
        playgroundUI.policyAddressField.showSuggestionList();
      }
    });
    playgroundUI.policyAddressField.setText("http://");

    playgroundUI.clearButton.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        controller.clearPolicy();
      }
    });

    playgroundUI.loadButton.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        controller.loadPolicy(playgroundUI.policyAddressField.getText());
      }
    });

    playgroundUI.defaultButton.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        setPolicySource(defaultPolicy());
      }
    });

    setPolicySource(defaultPolicy());
  }

  private String defaultPolicy() {
    return PlaygroundResource.INSTANCE.defaultPolicy().getText();
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

  /*
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
  */

  private native void setupNativeSelectLineBridge() /*-{
    var that = this;
    $wnd.selectLine = function (uri, start, sOffset, end, eOffset) {
      that.@com.google.caja.demos.playground.client.ui.PlaygroundView::selectTab(Lcom/google/caja/demos/playground/client/ui/PlaygroundView$Tabs;)(
          @com.google.caja.demos.playground.client.ui.PlaygroundView.Tabs::SOURCE);
      that.@com.google.caja.demos.playground.client.ui.PlaygroundView::highlightSource(Ljava/lang/String;IIII)(uri, start, sOffset, end, eOffset);
    }
  }-*/;

  private void initEditor() {
    setupNativeSelectLineBridge();
    selectTab(Tabs.SOURCE);
  }

  private native void initPlusOne() /*-{
    try {
      $wnd.gapi.plusone.render("plusone",{size: "medium"});
    } catch (e) {
      // failure to initialize +1 button should not prevent load of page
    }
  }-*/;

  private void initMode() {
    if (null == mode) {
      playgroundUI.mode.setSelectedIndex(0);
    } else if (mode) {
      playgroundUI.mode.setSelectedIndex(1);
    } else {
      playgroundUI.mode.setSelectedIndex(2);
    }
    playgroundUI.mode.addChangeHandler(new ChangeHandler() {
      @Override
      public void onChange(ChangeEvent event) {
        // Need an absolute URL for IE<=8
        String base =
            Window.Location.getProtocol() + "//" +
            Window.Location.getHost() +
            Window.Location.getPath();
        String hash = Window.Location.getHash();
        int s = playgroundUI.mode.getSelectedIndex();
        switch (s) {
          case 1:
            Window.Location.assign(base + "?es5=true" + hash);
            break;
          case 2:
            Window.Location.assign(base + "?es5=false" + hash);
            break;
          default:
            Window.Location.assign(base + "?es5=auto" + hash);
            break;
        }
      }
    });
  }

  private void initUnsafe() {
    playgroundUI.unsafe.addValueChangeHandler(new ValueChangeHandler() {
      @Override
      public void onValueChange(ValueChangeEvent event) {
        setUnsafe(playgroundUI.unsafe.getValue());
      }
    });
  }

  private static TreeItem addExampleItem(Map<Example.Type, TreeItem> menu,
      Example eg) {
    if (!menu.containsKey(eg.type)) {
      TreeItem menuItem = new TreeItem(eg.type.description);
      menu.put(eg.type, menuItem);
    }
    TreeItem egItem = new TreeItem(eg.description);
    menu.get(eg.type).addItem(egItem);
    return egItem;
  }

  private void initExamples() {
    Map<Example.Type, TreeItem> menuMap = new EnumMap<Example.Type, TreeItem>(
        Example.Type.class);
    final Map<TreeItem, Example> entryMap = new HashMap<TreeItem, Example>();

    playgroundUI.exampleTree.setTitle("Select an example");
    for (Example eg : Example.values()) {
      TreeItem it = addExampleItem(menuMap, eg);
      entryMap.put(it, eg);
    }

    boolean first = true;
    for (TreeItem menuItem : menuMap.values()) {
      if (first) {
        first = false;
        menuItem.setState(true);
      }
      playgroundUI.exampleTree.addItem(menuItem);
    }

    playgroundUI.exampleTree.addSelectionHandler(new SelectionHandler<TreeItem>() {
      public void onSelection(SelectionEvent<TreeItem> event) {
        Example eg = entryMap.get(event.getSelectedItem());
        // No associated example - e.g. when opening a subtree menu
        if (null == eg) {
          return;
        }
        controller.loadSource(eg.url);
      }
    });
  }

  private native void initCaja(
      boolean debug,
      int forceES5) /*-{
    $wnd.caja.initialize({
      server: '.',
      debug: debug,
      forceES5Mode: (forceES5 < 0) ? undefined : (forceES5 > 0)
    });
  }-*/;

  private native void setUnsafe(boolean unsafe) /*-{
    $wnd.caja.disableSecurityForDebugger(unsafe);
  }-*/;

  public PlaygroundView(Playground controller, Boolean mode) {
    this.controller = controller;
    this.sourceExamples = new MultiWordSuggestOracle();
    this.policyExamples = new MultiWordSuggestOracle();
    this.mode = mode;

    this.playgroundUI =
      new com.google.caja.demos.playground.client.ui.PlaygroundUI(
          sourceExamples, policyExamples);
    RootLayoutPanel.get().add(playgroundUI);
    initSourcePanel();
    initPolicyPanel();
    initFeedbackPanel();
    initExamples();
    initEditor();
    initCaja(true, (mode == null) ? -1 : (mode ? 1 : 0));
    initPlusOne();
    initMode();
    initUnsafe();
  }

  public void setOriginalSource(String result) {
    if (result == null) {
      playgroundUI.sourceText.setText("");
    } else {
      playgroundUI.sourceText.setText(result);
    }
  }

  public void setPolicySource(String result) {
    if (result == null) {
      playgroundUI.policyText.setText("");
    } else {
      playgroundUI.policyText.setText(result);
    }
  }

  public void setCajoledSource(String html, String js) {
    if (html == null && js == null) {
      playgroundUI.cajoledSource.setText("There were cajoling errors");
      return;
    }
    playgroundUI.cajoledSource.setHTML(prettyPrint(html, "html") +
      "&lt;script&gt;" + prettyPrint(js, "lang-js") + "&lt;/script&gt;");
  }

  public void setLoading(boolean isLoading) {
    playgroundUI.loadingLabel.setVisible(isLoading);
  }

  private native String prettyPrint(String result, String lang) /*-{
    return $wnd.prettyPrintOne($wnd.indentAndWrapCode(result), lang);
  }-*/;

  public void setRenderedResult(final boolean es5, String baseUrl,
      final String policy, final String html, final String js,
      final String idClass)
  {
    if (html == null && js == null) {
      playgroundUI.renderResult.setText("There were cajoling errors");
      return;
    }

    // Make the cajoled content visible so that the DOM will be laid out before
    // the script checks DOM geometry.
    selectTab(Tabs.RENDER);

    setRenderedResultNative(
        playgroundUI.renderPanel.getElement(),
        baseUrl,
        makeUriPolicy(),
        idClass,
        policy,
        es5,
        html,
        js);
  }

  private native void setRenderedResultNative(
      Element element,
      String baseUrl,
      JavaScriptObject uriPolicy,
      String idClass,
      String policy,
      boolean es5,
      String html,
      String js) /*-{
    var that = this;
    $wnd.caja.load(
        element,
        uriPolicy,
        function(frame) {
          var api = that.@com.google.caja.demos.playground.client.ui.PlaygroundView::makeExtraImports(Lcom/google/gwt/core/client/JavaScriptObject;Lcom/google/gwt/core/client/JavaScriptObject;Ljava/lang/String;)($wnd.caja, frame, policy);
          frame = frame.api(api);
          frame = es5
              ? frame.code(baseUrl, "text/html", html)
              : frame.cajoled(baseUrl, js, html);
          frame.run(function(r) {
            that.@com.google.caja.demos.playground.client.ui.PlaygroundView::setRenderedResult(Ljava/lang/String;)(r + '');
          });
        },
        {
          idClass: idClass,
          title: 'Playground Untrusted Content'
        });
  }-*/;

  private void setRenderedResult(String result) {
    playgroundUI.renderResult.setText(result);
  }

  private native JavaScriptObject makeExtraImports(
      JavaScriptObject caja,
      JavaScriptObject guestFrame,
      String policy) /*-{
    var that = this;
    var extraImports = {};
    try {
      var tamings___ = eval(policy);
    } catch (e) {
      that.@com.google.caja.demos.playground.client.ui.PlaygroundView::addRuntimeError(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)
          (e, "evaluating policy");
    }
    for (var i=0; i < tamings___.length; i++) {
      try {
        tamings___[i].call(undefined, caja, extraImports);
      } catch (e) {
        that.@com.google.caja.demos.playground.client.ui.PlaygroundView::addRuntimeError(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)
            (e, "evaluating " + i + "th policy function");
      }
    }

    extraImports.onerror = caja.tame(caja.markFunction(
      function (message, source, lineNum) {
        that.@com.google.caja.demos.playground.client.ui.PlaygroundView::addRuntimeError(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)
            (message, source, lineNum);
      }));
    extraImports.alert = caja.tame(
      caja.markFunction(
        function(msg) { alert('Untrusted code says: ' + String(msg)); }));
      if ("undefined" !== typeof guestFrame.iframe.contentWindow.___ &&
        "function" === typeof guestFrame.iframe.contentWindow.___.setLogFunc) {
        guestFrame.iframe.contentWindow.___.setLogFunc(caja.tame(
          caja.markFunction(function(msg) {
            that.@com.google.caja.demos.playground.client.ui.PlaygroundView::addRuntimeMessage(Ljava/lang/String;)
                (msg);
          })));
      }
    return extraImports;
  }-*/;

  private native JavaScriptObject makeUriPolicy() /*-{
        return {
          fetch: $wnd.caja.policy.net.ALL.fetch,
          rewrite: function (uri, uriEffect, loaderType, hints) {
            if (uriEffect === $wnd.html4.ueffects.NEW_DOCUMENT) {
              return uri;
            }
            if (uriEffect === $wnd.html4.ueffects.SAME_DOCUMENT &&
                 loaderType === $wnd.html4.ltypes.SANDBOXED) {
              if (hints && hints.XHR) {
                return uri;
              }
              return "http://www.gmodules.com/gadgets/proxy"
                  + "?url=" + encodeURIComponent(uri)
                  + "&container=caja";
            }
            return null;
          }
        };
  }-*/;

  public void addCompileMessage(String item) {
    // Rendered using HTMLSnippetProducer serverside
    HTML i = new HTML(item);
    playgroundUI.compileMessages.add(i);
  }

  public void addRuntimeError(String message, String source, String lineNum) {
    // Labels are texty, so no escaping needed
    Label i = new Label(
        "Uncaught script error: '" + message +
        "' in source: '" + source +
        "' at line: " + lineNum + "\n");
    playgroundUI.runtimeMessages.add(i);
  }

  public void addRuntimeMessage(String message) {
    // Labels are texty, so no escaping needed
    Label i = new Label(message);
    playgroundUI.runtimeMessages.add(i);
  }

  /** @param uri unused but provided for consistency with native GWT caller. */
  public void highlightSource(String uri,
      int start, int sOffset, int end, int eOffset) {
    playgroundUI.sourceText.setCursorPos(start);
    playgroundUI.sourceText.setSelectionRange(start, sOffset, end, eOffset);
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
