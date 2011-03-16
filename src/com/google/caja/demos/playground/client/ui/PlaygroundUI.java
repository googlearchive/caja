/**
 * 
 */
package com.google.caja.demos.playground.client.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.MultiWordSuggestOracle;
import com.google.gwt.user.client.ui.SuggestBox;
import com.google.gwt.user.client.ui.TabLayoutPanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.Widget;

/**
 * Playground UI binder initialization
 *
 * @author Jasvir Nagra (jasvir@gmail.com)
 */
public class PlaygroundUI extends Composite {
  @UiField protected TextBox renderResult;
  @UiField protected HTML renderPanel;
  @UiField protected HTML compileMessages;
  @UiField protected HTML runtimeMessages;
  @UiField protected HTML cajoledSource;
  @UiField protected TabLayoutPanel editorPanel;
  @UiField protected PlaygroundEditor sourceText;
  @UiField protected TextArea policyText;
  @UiField protected Label version;
  @UiField protected HorizontalPanel loadingLabel;
  @UiField protected Button goButton;
  @UiField protected Button cajoleButton;
  @UiField protected Button loadButton;
  @UiField protected Button applyButton;
  @UiField protected Button clearButton;
  @UiField protected Tree exampleTree;
  @UiField protected HorizontalPanel feedbackPanel;

  @UiField(provided=true)
  protected SuggestBox addressField;
  
  @UiField(provided=true)
  protected SuggestBox policyAddressField;

  private static PlaygroundUiBinder uiBinder = 
    GWT.create(PlaygroundUiBinder.class);

  interface PlaygroundUiBinder extends UiBinder<Widget, PlaygroundUI> {}

  public PlaygroundUI(MultiWordSuggestOracle sourceSuggestions,
      MultiWordSuggestOracle policySuggestions) {
    addressField = new SuggestBox(sourceSuggestions);
    policyAddressField = new SuggestBox(policySuggestions);
    
    initWidget(uiBinder.createAndBindUi(this));
  }
}
