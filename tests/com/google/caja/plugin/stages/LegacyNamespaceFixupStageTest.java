// Copyright (C) 2009 Google Inc.
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

import com.google.caja.lexer.InputSource;
import com.google.caja.parser.AncestorChain;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.html.DomParser;
import com.google.caja.plugin.Dom;
import com.google.caja.plugin.Job;
import com.google.caja.plugin.Jobs;
import com.google.caja.plugin.PluginMessageType;
import com.google.caja.plugin.PluginMeta;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessagePart;
import com.google.caja.util.CajaTestCase;
import com.google.caja.util.Lists;

import java.util.Arrays;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class LegacyNamespaceFixupStageTest extends CajaTestCase {

  public void testEmptyFragment() {
    assertFixed("", builder().job());
    assertTrue(mq.getMessages().isEmpty());
  }

  public void testOneHtmlTag() {
    assertFixed(
        "<b>Foo</b>",
        builder().open("b").text("Foo").close().job());
    assertTrue(mq.getMessages().isEmpty());
  }

  public void testHtmlTagAndAttrib() {
    assertFixed(
        "<a href=\"bar.html\">Foo</a>",
        builder().open("a").attr("href", "bar.html").text("Foo").close().job());
    assertTrue(mq.getMessages().isEmpty());
  }

  public void testPrefixedAttribWithWellKnownPrefix() {
    assertFixed(
        "<a href=\"bar.html\" xml:lang=\"en\">Foo</a>",
        builder().open("a").attr("href", "bar.html")
            .attr("xml:lang", "en").text("Foo").close().job());
    assertMessage(
        true, PluginMessageType.MISSING_XML_NAMESPACE, MessageLevel.LINT,
        MessagePart.Factory.valueOf("xml:lang"));
    assertTrue(mq.getMessages().isEmpty());
  }

  public void testPrefixedAttribWithUnknownPrefix() {
    assertFixed(
        ""
        + "<a href=\"bar.html\""
        + " xmlns:_ns8=\"http://example.net/unknown-xml-namespace/\""
        + " _ns8:lang=\"en\">Foo</a>",
        builder().open("a").attr("href", "bar.html")
            .attr("baz:lang", "en").text("Foo").close().job());
    assertMessage(
        true, PluginMessageType.MISSING_XML_NAMESPACE, MessageLevel.ERROR,
        MessagePart.Factory.valueOf("baz:lang"));
    assertTrue(mq.getMessages().isEmpty());
  }

  public void testPrefixedElementWithKnownPrefix() {
    // The unprefixed href attribute gets the namespace of the containing
    // element, not necessarily HTML.
    assertFixed(
        "<svg:a href=\"foo.svg\">Foo</svg:a>",
        builder().open("svg:a").attr("href", "foo.svg").text("Foo").close()
            .job());
    assertMessage(
        true, PluginMessageType.MISSING_XML_NAMESPACE, MessageLevel.LINT,
        MessagePart.Factory.valueOf("svg:a"));
    assertTrue(mq.getMessages().isEmpty());
  }

  public void testPrefixedElementWithUnknownPrefix() {
    // The unprefixed href attribute gets the namespace of the containing
    // element, not necessarily HTML.
    assertFixed(
        ""
        + "<_ns8:a xmlns:_ns8=\"http://example.net/unknown-xml-namespace/\""
        + " href=\"foo.svg\">Foo</_ns8:a>",
        builder().open("foo:a").attr("href", "foo.svg").text("Foo").close()
            .job());
    assertMessage(
        true, PluginMessageType.MISSING_XML_NAMESPACE, MessageLevel.ERROR,
        MessagePart.Factory.valueOf("foo:a"));
    // No need to warn on href attribute which automatically gets unknown
    // namespace from parent.
    assertTrue(mq.getMessages().isEmpty());
  }

  private void assertFixed(String golden, Job... inputs) {
    Jobs jobs = new Jobs(mc, mq, new PluginMeta());
    jobs.getJobs().addAll(Arrays.asList(inputs));
    new LegacyNamespaceFixupStage().apply(jobs);
    StringBuilder sb = new StringBuilder();
    for (Job job : jobs.getJobs()) {
      sb.append(render(job.getRoot().cast(ParseTreeNode.class).node));
      sb.append('\n');
    }
    assertEquals(golden, sb.toString().trim());
  }

  static NamespaceUnawareBuilder builder() {
    return new NamespaceUnawareBuilder();
  }

  static final class NamespaceUnawareBuilder {
    final Document doc = DomParser.makeDocument(null, null);
    final DocumentFragment root = doc.createDocumentFragment();
    final List<Node> stack = Lists.newArrayList((Node) root);

    private Node top() { return stack.get(stack.size() - 1); }

    Job job() {
      assertEquals(stack, Lists.newArrayList(root));
      return Job.domJob(
          AncestorChain.instance(new Dom(root)), InputSource.UNKNOWN.getUri());
    }
    NamespaceUnawareBuilder open(String qname) {
      Element el = doc.createElement(qname);
      top().appendChild(el);
      stack.add(el);
      return this;
    }
    NamespaceUnawareBuilder close() {
      assertTrue(stack.size() > 1);
      stack.remove(stack.size() - 1);
      return this;
    }
    NamespaceUnawareBuilder text(String s) {
      top().appendChild(doc.createTextNode(s));
      return this;
    }
    NamespaceUnawareBuilder attr(String name, String value) {
      ((Element) top()).setAttribute(name, value);
      return this;
    }
  }
}
