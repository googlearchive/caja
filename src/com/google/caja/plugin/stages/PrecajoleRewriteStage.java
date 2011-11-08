// Copyright (C) 2011 Google Inc.
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
import com.google.caja.parser.js.CajoledModule;
import com.google.caja.plugin.Job;
import com.google.caja.plugin.JobEnvelope;
import com.google.caja.plugin.Jobs;
import com.google.caja.plugin.Placeholder;
import com.google.caja.precajole.PrecajoleMap;
import com.google.caja.util.ContentType;
import com.google.caja.util.Pipeline.Stage;
import com.google.caja.util.Strings;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * This stage looks for script tags that refer to URIs that are in
 * StaticPrecajoleMap, and uses the pre-cajoled result when possible.
 */

public class PrecajoleRewriteStage implements Stage<Jobs> {
  private final PrecajoleMap precajoleMap;
  private final boolean minify;

  public PrecajoleRewriteStage(PrecajoleMap map, boolean minify) {
    this.precajoleMap = map;
    this.minify = minify;
  }

  @Override
  public boolean apply(Jobs jobs) {
    if (precajoleMap == null) {
      return true;
    }
    for (JobEnvelope env: jobs.getJobsByType(ContentType.HTML)) {
      Node root = ((Dom) env.job.getRoot()).getValue();
      rewriteChildren(jobs, root);
    }
    return true;
  }

  private void rewriteChildren(Jobs jobs, Node node) {
    Node c = node.getFirstChild();
    for (; c != null; c = c.getNextSibling()) {
      if (!rewriteElement(jobs, c)) {
        rewriteChildren(jobs, c);
      }
    }
  }

  // Returns true if element rewritten
  private boolean rewriteElement(Jobs jobs, Node node) {
    if (!(node instanceof Element)) {
      return false;
    }
    Element el = (Element) node;
    String tagName = Strings.toLowerCase(el.getLocalName());
    if ("script".equals(tagName)) {
      String src = getAttr(el, "src");
      if (src != null && src.length() != 0) {
        CajoledModule pre = precajoleMap.lookupUri(src, minify);
        if (pre != null) {
          replaceScript(jobs, el, pre);
          return true;
        }
      }
    }
    return false;
  }

  private void replaceScript(Jobs jobs, Element el, CajoledModule pre) {
    Node parent = el.getParentNode();
    if (parent == null) {
      return;
    }

    String id = "$" + jobs.getPluginMeta().generateGuid();
    Element placeholder = Placeholder.make(el, id);

    if (Strings.equalsIgnoreCase(getAttr(el, "defer"), "defer")) {
      parent.removeChild(el);
      parent.appendChild(placeholder);
    } else {
      parent.replaceChild(placeholder, el);
    }

    Job job = Job.cajoledJob(pre);
    jobs.getJobs().add(
        new JobEnvelope(id, JobCache.none(), ContentType.JS, true, job));
  }

  private String getAttr(Node n, String attrib) {
    if (n instanceof Element) {
      return ((Element) n).getAttributeNS(HTML_NS, attrib);
    } else {
      return null;
    }
  }

  private static final String HTML_NS = Namespaces.HTML_NAMESPACE_URI;
}
