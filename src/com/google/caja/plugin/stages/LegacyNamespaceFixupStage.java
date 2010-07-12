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

import com.google.caja.parser.html.Dom;
import com.google.caja.parser.html.Namespaces;
import com.google.caja.parser.html.Nodes;
import com.google.caja.plugin.Job;
import com.google.caja.plugin.Jobs;
import com.google.caja.plugin.PluginMessageType;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.util.ContentType;
import com.google.caja.util.Lists;
import com.google.caja.util.Pipeline;

import java.util.List;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Adds namespaces to DOM nodes that lack them so that we can deal with input
 * from legacy systems that use {@code document.createElement} or
 * {@code document.setAttribute} instead of the namespace-aware variants.
 *
 * @author mikesamuel@gmail.com
 */
public class LegacyNamespaceFixupStage implements Pipeline.Stage<Jobs> {

  public boolean apply(Jobs jobs) {
    Fixer f = new Fixer(jobs.getMessageQueue());
    for (Job job : jobs.getJobsByType(ContentType.HTML)) { f.fix(job); }
    return jobs.hasNoFatalErrors();
  }

  private static class Fixer {
    final MessageQueue mq;

    Fixer(MessageQueue mq) { this.mq = mq; }

    void fix(Job job) {
      fix(((Dom) job.getRoot()).getValue());
    }

    private void fix(Node node) {
      for (Node c = node.getFirstChild(); c != null;) {
        Node next = c.getNextSibling();  // Correct whether c is replaced or not
        fix(c);
        c = next;
      }
      if (!(node instanceof Element)) { return; }
      Element e = (Element) node;
      if (e.getNamespaceURI() == null) {
        e = fixElement(e);
      }
      List<Attr> toFix = null;
      for (Attr a : Nodes.attributesOf(e)) {
        if (a.getNamespaceURI() == null) {
          if (toFix == null) { toFix = Lists.newArrayList(); }
          toFix.add(a);
        }
      }
      if (toFix != null) {
        String elNsUri = e.getNamespaceURI();
        for (Attr a : toFix) { fixAttr(elNsUri, a); }
      }
    }

    private Element fixElement(Element e) {
      String ns = guessNamespaceAndWarn(Namespaces.HTML_NAMESPACE_URI, e);
      // Create a namespace aware version of e.
      Element newE = e.getOwnerDocument().createElementNS(ns, e.getTagName());
      Nodes.setFilePositionFor(newE, Nodes.getFilePositionFor(e));
      // Move all children from old to new.
      for (Node c; (c = e.getFirstChild()) != null;) {
        newE.appendChild(c);
      }
      // Move all attributes from old to new.
      List<Attr> attrs = Lists.newArrayList();
      for (Attr a : Nodes.attributesOf(e)) {
        attrs.add(a);
      }
      // We use setAttributeNode here since we have not yet verified that
      // the attributes are properly namespaced, and indeed, we cannot do so
      // until the parent namespace is known.
      for (Attr a : attrs) {
        e.removeAttributeNode(a);
        newE.setAttributeNode(a);
      }
      e.getParentNode().replaceChild(newE, e);
      return newE;
    }

    private void fixAttr(String elNsUri, Attr a) {
      Element e = a.getOwnerElement();
      String ns = guessNamespaceAndWarn(elNsUri, a);
      Attr newA = a.getOwnerDocument().createAttributeNS(ns, a.getName());
      newA.setNodeValue(a.getValue());
      Nodes.setFilePositionFor(newA, Nodes.getFilePositionFor(a));
      Nodes.setFilePositionForValue(newA, Nodes.getFilePositionForValue(a));
      Nodes.setRawValue(newA, Nodes.getRawValue(a));
      e.removeAttributeNode(a);
      e.setAttributeNodeNS(newA);
    }

    private String guessNamespaceAndWarn(String defaultNsUri, Node n) {
      String xmlIdent = n.getNodeName();
      int colon = xmlIdent.indexOf(':');
      if (colon < 0) {  // Don't warn if no prefix
        return defaultNsUri;
      }
      MessageLevel level;
      String prefix = xmlIdent.substring(0, colon);
      Namespaces ns = Namespaces.HTML_DEFAULT.forPrefix(prefix);
      String nsUri;
      if (ns == null) {
        level = PluginMessageType.MISSING_XML_NAMESPACE.getLevel();
        nsUri = "http://example.net/unknown-xml-namespace/";
      } else {
        level = MessageLevel.LINT;
        nsUri = ns.uri;
      }
      mq.addMessage(
          PluginMessageType.MISSING_XML_NAMESPACE, level,
          Nodes.getFilePositionFor(n),
          MessagePart.Factory.valueOf(xmlIdent));
      return nsUri;
    }
  }
}
