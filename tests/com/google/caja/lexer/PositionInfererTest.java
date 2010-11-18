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

package com.google.caja.lexer;

import com.google.caja.parser.html.Nodes;
import com.google.caja.util.CajaTestCase;
import com.google.caja.util.Lists;
import com.google.caja.util.MoreAsserts;
import com.google.caja.util.Sets;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import org.w3c.dom.Attr;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class PositionInfererTest extends CajaTestCase {
  public final void testNoGuides() throws ParseException {
    DocumentFragment f = xmlFragment(fromString("<br/>"));
    FilePosition spanningPos = Nodes.getFilePositionFor(f);  // 1+1 - 6

    clearAllPositionsExcept(f, Sets.<String>newHashSet());

    assertPositions(
        Arrays.asList(
            "#document-fragment ???",
            "  br ???"),
        f);

    doInference(f, spanningPos);

    assertPositions(
        Arrays.asList(
            "#document-fragment 1+6",
            "  br 1+6"),
        f);
  }

  public final void testGaps() throws ParseException {
    DocumentFragment f = xmlFragment(fromString("<br/>Foo<br/>"));
    FilePosition spanningPos = Nodes.getFilePositionFor(f);

    clearAllPositionsExcept(f, Sets.newHashSet("br"));

    assertPositions(
        Arrays.asList(
            "#document-fragment ???",
            "  br 1+1 - 6",
            "  #text ???",  // Adjacent to two known points
            "  br 1+9 - 14"),
        f);

    doInference(f, spanningPos);

    assertPositions(
        Arrays.asList(
            "#document-fragment 1+1 - 14",
            "  br 1+1 - 6",
            "  #text 1+6 - 9",
            "  br 1+9 - 14"),
        f);
  }

  public final void testSparseRelations() throws ParseException {
    DocumentFragment f = xmlFragment(fromString(
        ""
        + "<b><hr/><br/></b>\n"
        + "<img src='10' title='foo' />\n"
        + "<p><img/></p>"));
    FilePosition spanningPos = Nodes.getFilePositionFor(f);

    clearAllPositionsExcept(
        f, Sets.newHashSet("hr", "br", "title", "p"));

    assertPositions(
        Arrays.asList(
            "#document-fragment ???",
            "  b ???",
            "    hr 1+4 - 9",
            "    br 1+9 - 14",
            "  #text ???",
            "  img ???",
            "    src ???",
            "      #text ???",
            "    title 2+15 - 20",
            "      #text ???",
            "  #text ???",
            "  p 3+1 - 14",
            "    img ???"),
        f);

    System.err.println("spanningPos=" + spanningPos);
    doInference(f, spanningPos);

    assertPositions(
        Arrays.asList(
            "#document-fragment 1+4 - 3+14",
            "  b 1+4 - 14",
            "    hr 1+4 - 9",
            "    br 1+9 - 14",
            "  #text 1+14",
            "  img 1+14 - 2+20",
            "    src 1+14",
            "      #text 1+14",
            "    title 2+15 - 20",
            "      #text 2+20",
            "  #text 2+20 - 3+1",
            "  p 3+1 - 14",
            "    img 3+1"),
        f);
  }

  public final void testDoesNotDamageKnownData() throws ParseException {
    DocumentFragment f = xmlFragment(fromString(
        ""
        + "<b><hr/><br/></b>\n"
        + "<img src='10' title='foo' />\n"
        + "<p><img/></p>"));
    FilePosition spanningPos = Nodes.getFilePositionFor(f);

    // Don't clear any positions

    List<String> positions = Arrays.asList(
        "#document-fragment 1+1 - 3+14",
        "  b 1+1 - 18",
        "    hr 1+4 - 9",
        "    br 1+9 - 14",
        "  #text 1+18 - 2+1",
        "  img 2+1 - 29",
        "    src 2+6 - 9",
        "      #text 2+10 - 14",
        "    title 2+15 - 20",
        "      #text 2+21 - 26",
        "  #text 2+29 - 3+1",
        "  p 3+1 - 14",
        "    img 3+4 - 10");

    assertPositions(positions, f);
    doInference(f, spanningPos);
    assertPositions(positions, f);
  }

  private static void clearAllPositionsExcept(Node n, Set<String> exceptions) {
    if (!exceptions.contains(n.getNodeName())) {
      Nodes.setFilePositionFor(n, FilePosition.UNKNOWN);
    }
    for (Node child : Nodes.childrenOf(n)) {
      clearAllPositionsExcept(child, exceptions);
    }
    if (n instanceof Element) {
      for (Attr a : Nodes.attributesOf((Element) n)) {
        clearAllPositionsExcept(a, exceptions);
      }
    }
  }

  private void assertPositions(List<String> golden, Node n) {
    List<String> actual = Lists.newArrayList();
    appendNodePositions(n, 0, actual);
    MoreAsserts.assertListsEqual(golden, actual);
  }

  /**
   * Produce a list of strings describing the position of each node.
   * @param depth the number of edges between n and the root.
   * @param out appended to.
   */
  private static void appendNodePositions(Node n, int depth, List<String> out) {
    StringBuilder sb = new StringBuilder();
    for (int i = depth; --i >= 0;) { sb.append("  "); }
    sb.append(n.getNodeName()).append(" ");
    FilePosition p = Nodes.getFilePositionFor(n);
    if (FilePosition.UNKNOWN.equals(p)) {
      sb.append("???");
    } else {
      sb.append(p.startLineNo()).append('+').append(p.startCharInLine());
      if (p.length() != 0) {
        sb.append(" - ");
        if (p.endLineNo() != p.startLineNo()) {
          sb.append(p.endLineNo()).append('+');
        }
        sb.append(p.endCharInLine());
      }
    }
    out.add(sb.toString());

    if (n instanceof Element) {
      for (Attr a : Nodes.attributesOf((Element) n)) {
        appendNodePositions(a, depth + 1, out);
      }
    }
    for (Node c : Nodes.childrenOf(n)) {
      appendNodePositions(c, depth + 1, out);
    }
  }

  /**
   * Run the inferer and update the positions of nodes under n that come from
   * an unknown source.  Typically, nodes created by a quasi-literal come from
   * unknown.
   */
  private static void doInference(Node n, final FilePosition spanningPos) {
    PositionInferer inferer = new PositionInferer(spanningPos) {
      @Override
      protected FilePosition getPosForNode(@Nullable Object o) {
        // We use a fake null node, as a convenient way to cause
        // the entire tree falls within the spanningPos.
        if (o == null) { return spanningPos; }
        return Nodes.getFilePositionFor((Node) o);
      }

      @Override
      protected void setPosForNode(@Nullable Object o, FilePosition pos) {
        if (o == null) { return; }
        Node n = (Node) o;
        FilePosition old = Nodes.getFilePositionFor(n);
        if (old.source().equals(InputSource.UNKNOWN)) {
          Nodes.setFilePositionFor((Node) o, pos);
        }
      }
    };
    inferer.contains(null, n);
    addRelations(n, inferer);

    inferer.solve();
  }

  private static void addRelations(Node n, PositionInferer inferer) {
    if (n instanceof Element) {
      // An element contains its attributes, but there are no ordering
      // relations between them.
      Node firstChild = n.getFirstChild();
      for (Attr a : Nodes.attributesOf((Element) n)) {
        inferer.contains(n, a);
        inferer.precedes(a, a.getFirstChild());
        if (firstChild != null) { inferer.precedes(a, firstChild); }
      }
    }
    // Each node contains its children.
    for (Node child : Nodes.childrenOf(n)) {
      inferer.contains(n, child);
      addRelations(child, inferer);
    }
    // Each node is immediately adjacent to its next sibling.
    Node next = n.getNextSibling();
    if (next != null) { inferer.adjacent(n, next); }
  }
}
