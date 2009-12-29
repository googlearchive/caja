// Copyright (C) 2008 Google Inc.
//
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

package com.google.caja.plugin.stages;

import com.google.caja.lexer.ParseException;
import com.google.caja.plugin.Jobs;
import com.google.caja.plugin.PluginMessageType;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessageType;
import com.google.caja.util.ContentType;

public class InlineCssImportsStageTest extends PipelineStageTestCase {
  public final void testNoImports() throws Exception {
    assertPipeline(
        job("p { color: purple }", ContentType.CSS),
        job("p {\n  color: purple\n}", ContentType.CSS));
  }

  public final void testOneImport() throws Exception {
    addUrlToPluginEnvironment("foo.css", "p { color: purple }");
    assertPipeline(
        job("@import 'foo.css';", ContentType.CSS),
        job("p {\n  color: purple\n}", ContentType.CSS));
  }

  public final void testImportResolvedRelativeToImporter() throws Exception {
    addUrlToPluginEnvironment("foo/bar.css", "@import 'baz.css';");
    addUrlToPluginEnvironment("baz.css", "p { color: #f88 }");
    addUrlToPluginEnvironment("foo/baz.css", "p { color: purple }");
    assertPipeline(
        job("@import 'foo/bar.css';", ContentType.CSS),
        job("p {\n  color: purple\n}", ContentType.CSS));
  }

  public final void testMultipleImport() throws Exception {
    addUrlToPluginEnvironment("foo.css", "p { color: purple }");
    addUrlToPluginEnvironment("bar.css", "b { color: blue }");
    assertPipeline(
        job("@import 'foo.css'; @import 'bar.css'; i { color: #ff8 }",
            ContentType.CSS),
        job("p {\n  color: purple\n}\n"
            + "b {\n  color: blue\n}\n"
            + "i {\n  color: #ff8\n}", ContentType.CSS));
  }

  public final void testMediaTypesUnioned() throws Exception {
    addUrlToPluginEnvironment("all1.css", "a { content: 'all1' }");
    addUrlToPluginEnvironment("all1.css", "a { content: 'all1' }");
    addUrlToPluginEnvironment(
        "all2.css", "@media all { a { content: 'all2' } }");
    addUrlToPluginEnvironment(
        "all2.css", "@media all { a { content: 'all2' } }");
    addUrlToPluginEnvironment(
        "multi.css", "@media print, screen { b { content: 'multi' } }");
    addUrlToPluginEnvironment(
        "print.css", "@media print { p { content: 'print' } }");
    addUrlToPluginEnvironment(
        "print.css", "@media print { p { content: 'print' } }");
    addUrlToPluginEnvironment(
        "screen.css", "@media screen { s { content: 'screen' } }");
    assertPipeline(
        job("@import 'print.css' screen;\n"  // Removed entirely
            + "@import 'screen.css' print, screen;\n"  // Choose one of importer
            + "@import 'print.css' all;\n"  // Expanded against all
            + "@import 'multi.css' print;\n"  // Choose one from importer
            + "@import 'all1.css' screen, print;\n"  // Cross with implicit all
            + "@import 'all2.css' screen, print;\n"  // Cross with explicit all
            + "@import 'all1.css' all;\n"  // All x implicit all
            + "@import 'all2.css' all;",  // All x explicit all
            ContentType.CSS),
        job("@media screen {\n  s {\n    content: 'screen'\n  }\n}\n"
            + "@media print {\n  p {\n    content: 'print'\n  }\n}\n"
            + "@media print {\n  b {\n    content: 'multi'\n  }\n}\n"
            + "@media screen, print {\n  a {\n    content: 'all1'\n  }\n}\n"
            + "@media screen, print {\n  a {\n    content: 'all2'\n  }\n}\n"
            + "a {\n  content: 'all1'\n}\n"
            + "@media all {\n  a {\n    content: 'all2'\n  }\n}",
            ContentType.CSS));
  }

  public final void testUnresolveableImport() throws Exception {
    assertPipelineFails(
        job("@import 'bogus.css';", ContentType.CSS),
        job("@import url('bogus.css');", ContentType.CSS));
    assertMessage(PluginMessageType.FAILED_TO_LOAD_EXTERNAL_URL, MessageLevel.ERROR);
  }

  public final void testCyclicImport() throws Exception {
    for (int i = 50; --i >= 0;) {
      addUrlToPluginEnvironment("foo.css", "@import 'foo.css';");
    }
    assertPipelineFails(
        job("@import 'foo.css';", ContentType.CSS),
        job("@import url('foo.css');", ContentType.CSS));
    assertMessage(PluginMessageType.CYCLIC_INCLUDE, MessageLevel.ERROR);
  }

  public final void testMalformedUrl() throws Exception {
    try {
      assertPipelineFails(
          job("@import ':::';", ContentType.CSS),
          job("@import url(':::');", ContentType.CSS));
    } catch (ParseException ex) {
      ex.toMessageQueue(mq);
    }
    assertMessage(MessageType.MALFORMED_URI, MessageLevel.FATAL_ERROR);
  }

  @Override
  protected boolean runPipeline(Jobs jobs) {
    return (new InlineCssImportsStage()).apply(jobs);
  }
}
