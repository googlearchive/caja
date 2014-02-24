// Copyright (C) 2006 Google Inc.
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

package com.google.caja.parser;

import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.JsTokenType;
import com.google.caja.lexer.Token;
import com.google.caja.lexer.TokenConsumer;
import com.google.caja.parser.js.AbstractExpression;
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.ExpressionStmt;
import com.google.caja.parser.js.IntegerLiteral;
import com.google.caja.parser.js.LabeledStatement;
import com.google.caja.parser.js.LabeledStmtWrapper;
import com.google.caja.parser.js.Noop;
import com.google.caja.parser.js.StringLiteral;
import com.google.caja.render.JsPrettyPrinter;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.RenderContext;
import com.google.caja.util.CajaTestCase;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author mikesamuel@gmail.com (Mike Samuel)
 */
@SuppressWarnings("static-method")
public class ParseTreeNodeTest extends CajaTestCase {
  LabeledStmtWrapper root;
  LabeledStmtWrapper b9;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    FilePosition unk = FilePosition.UNKNOWN;

    // $0: {
    //   $1: {
    //     2;
    //     3;
    //     4;
    //   }
    //   $5: {
    //     6;
    //     7;
    //     8;
    //   }
    // }
    // $9: {
    //   10;
    //   11;
    //   12;
    // }

    ExpressionStmt[] b = new ExpressionStmt[13];
    for (int i = b.length; --i >= 0;) {
      b[i] = new ExpressionStmt(unk, new IntegerLiteral(unk, i));
    }

    LabeledStmtWrapper b1 = new LabeledStmtWrapper(
        unk, "$1", new Block(unk, Arrays.asList(b[2], b[3], b[4])));

    LabeledStmtWrapper b5 = new LabeledStmtWrapper(
        unk, "$5", new Block(unk, Arrays.asList(b[6], b[7], b[8])));
    root = new LabeledStmtWrapper(
        unk, "$0", new Block(unk, Arrays.asList(b1, b5)));

    b9 = new LabeledStmtWrapper(
        unk, "$9", new Block(unk, Arrays.asList(b[10], b[11], b[12])));
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    root = null;
  }

  public final void testFormatTree() throws Exception {
    StringBuilder sb = new StringBuilder();
    MessageContext mc = new MessageContext();
    root.formatTree(mc, 0, sb);
    assertEquals(
        "LabeledStmtWrapper : $0\n"
        + "  Block\n"
        + "    LabeledStmtWrapper : $1\n"
        + "      Block\n"
        + "        ExpressionStmt\n"
        + "          IntegerLiteral : 2\n"
        + "        ExpressionStmt\n"
        + "          IntegerLiteral : 3\n"
        + "        ExpressionStmt\n"
        + "          IntegerLiteral : 4\n"
        + "    LabeledStmtWrapper : $5\n"
        + "      Block\n"
        + "        ExpressionStmt\n"
        + "          IntegerLiteral : 6\n"
        + "        ExpressionStmt\n"
        + "          IntegerLiteral : 7\n"
        + "        ExpressionStmt\n"
        + "          IntegerLiteral : 8",
        sb.toString());
  }

  public final void testRender() {
    StringBuilder sb = new StringBuilder();
    TokenConsumer tc = new JsPrettyPrinter(sb);
    root.render(new RenderContext(tc));
    tc.noMoreTokens();
    assertEquals(
        "$0: {\n"
        + "  $1: {\n"
        + "    2;\n"
        + "    3;\n"
        + "    4;\n"
        + "  }\n"
        + "  $5: {\n"
        + "    6;\n"
        + "    7;\n"
        + "    8;\n"
        + "  }\n"
        + "}",
        sb.toString());
  }

  public final void testAcceptPreOrder() {
    IntEnqueuer ie = new IntEnqueuer();
    root.acceptPreOrder(ie, null);
    assertEquals("[0, 1, 2, 3, 4, 5, 6, 7, 8]", ie.getNums().toString());
  }

  public final void testAcceptPreOrderReturnHandling() {
    IntEnqueuerExcept ie = new IntEnqueuerExcept(6);
    root.acceptPreOrder(ie, null);
    assertEquals("[0, 1, 2, 3, 4, 5, 6, 7, 8]", ie.getNums().toString());

    ie = new IntEnqueuerExcept(1);
    root.acceptPreOrder(ie, null);
    assertEquals("[0, 1, 5, 6, 7, 8]", ie.getNums().toString());
  }

  public final void testAcceptPostOrder() {
    IntEnqueuer ie = new IntEnqueuer();
    root.acceptPostOrder(ie, null);
    assertEquals("[2, 3, 4, 1, 6, 7, 8, 5, 0]", ie.getNums().toString());
  }

  public final void testAcceptPostOrderReturnHandling() {
    IntEnqueuerExcept ie = new IntEnqueuerExcept(6);
    root.acceptPostOrder(ie, null);
    assertEquals("[2, 3, 4, 1, 6]", ie.getNums().toString());
  }

  public final void testVisitPreOrder() {
    final List<Number> nums = Lists.newArrayList();
    root.visitPreOrder(new ParseTreeNodeVisitor() {
      public boolean visit(ParseTreeNode node) {
        if (node instanceof IntegerLiteral) {
          nums.add(((IntegerLiteral)node).getValue());
        }
        return true;
      }
    });
    assertEquals("[2, 3, 4, 6, 7, 8]", nums.toString());
  }

  public final void testVisitPreOrderSkip() {
    final List<Number> nums = Lists.newArrayList();
    root.visitPreOrder(new ParseTreeNodeVisitor() {
      public boolean visit(ParseTreeNode node) {
        if (node instanceof IntegerLiteral) {
          nums.add(((IntegerLiteral)node).getValue());
        } else if (node instanceof LabeledStatement) {
          String label = ((LabeledStatement)node).getLabel();
          if (label.equals("$1")) {
            return false;
          }
        }
        return true;
      }
    });
    assertEquals("[6, 7, 8]", nums.toString());
  }

  void doReplace() {
    ParseTreeNode rootBlock = root.children().get(0);
    assertTrue(rootBlock instanceof Block);
    ParseTreeNode b5 = rootBlock.children().get(1);
    assertTrue(b5 instanceof LabeledStatement);
    assertEquals("$5", ((LabeledStatement) b5).getLabel());

    ((Block) rootBlock).replaceChild(b9, b5);
  }

  public final void testFormatTreePostReplace() throws Exception {
    doReplace();
    StringBuilder sb = new StringBuilder();
    MessageContext mc = new MessageContext();
    root.formatTree(mc, 0, sb);
    assertEquals(
        "LabeledStmtWrapper : $0\n"
        + "  Block\n"
        + "    LabeledStmtWrapper : $1\n"
        + "      Block\n"
        + "        ExpressionStmt\n"
        + "          IntegerLiteral : 2\n"
        + "        ExpressionStmt\n"
        + "          IntegerLiteral : 3\n"
        + "        ExpressionStmt\n"
        + "          IntegerLiteral : 4\n"
        + "    LabeledStmtWrapper : $9\n"
        + "      Block\n"
        + "        ExpressionStmt\n"
        + "          IntegerLiteral : 10\n"
        + "        ExpressionStmt\n"
        + "          IntegerLiteral : 11\n"
        + "        ExpressionStmt\n"
        + "          IntegerLiteral : 12",
        sb.toString());
  }

  public final void testRenderPostReplace() {
    doReplace();
    StringBuilder sb = new StringBuilder();
    TokenConsumer tc = new JsPrettyPrinter(sb);
    root.render(new RenderContext(tc));
    tc.noMoreTokens();
    assertEquals(
        "$0: {\n"
        + "  $1: {\n"
        + "    2;\n"
        + "    3;\n"
        + "    4;\n"
        + "  }\n"
        + "  $9: {\n"
        + "    10;\n"
        + "    11;\n"
        + "    12;\n"
        + "  }\n"
        + "}",
        sb.toString());
  }

  public final void testVisitPreOrderPostReplace() {
    doReplace();
    IntEnqueuer ie = new IntEnqueuer();
    root.acceptPreOrder(ie, null);
    assertEquals("[0, 1, 2, 3, 4, 9, 10, 11, 12]", ie.getNums().toString());
  }

  public final void testVisitPreorderDoesntDescendIntoReplaced() {
    IntEnqueuerThatReplaces ie = new IntEnqueuerThatReplaces(5, b9);
    root.acceptPreOrder(ie, null);
    assertEquals("[0, 1, 2, 3, 4, 5]", ie.getNums().toString());
  }

  void doInsert(int before) {
    ParseTreeNode rootBlock = root.children().get(0);
    assertTrue(rootBlock instanceof Block);
    ParseTreeNode b;
    switch (before) {
      case -1:
        b = null;
        break;
      case 1:
        b = rootBlock.children().get(0);
        break;
      case 5:
        b = rootBlock.children().get(1);
        break;
      default:
        fail(String.valueOf(before));
        return;
    }
    if (null != b) {
      assertTrue(b instanceof LabeledStatement);
      assertEquals("$" + before, ((LabeledStatement) b).getLabel());
    } else {
      assertEquals(-1, before);
    }

    ((Block) rootBlock).insertBefore(b9, b);
  }

  public final void testFormatTreePostInsert() throws Exception {
    doInsert(5);
    StringBuilder sb = new StringBuilder();
    MessageContext mc = new MessageContext();
    root.formatTree(mc, 0, sb);
    assertEquals(
        "LabeledStmtWrapper : $0\n"
        + "  Block\n"
        + "    LabeledStmtWrapper : $1\n"
        + "      Block\n"
        + "        ExpressionStmt\n"
        + "          IntegerLiteral : 2\n"
        + "        ExpressionStmt\n"
        + "          IntegerLiteral : 3\n"
        + "        ExpressionStmt\n"
        + "          IntegerLiteral : 4\n"
        + "    LabeledStmtWrapper : $9\n"
        + "      Block\n"
        + "        ExpressionStmt\n"
        + "          IntegerLiteral : 10\n"
        + "        ExpressionStmt\n"
        + "          IntegerLiteral : 11\n"
        + "        ExpressionStmt\n"
        + "          IntegerLiteral : 12\n"
        + "    LabeledStmtWrapper : $5\n"
        + "      Block\n"
        + "        ExpressionStmt\n"
        + "          IntegerLiteral : 6\n"
        + "        ExpressionStmt\n"
        + "          IntegerLiteral : 7\n"
        + "        ExpressionStmt\n"
        + "          IntegerLiteral : 8",
        sb.toString());
  }

  public final void testRenderPostInsert() {
    doInsert(5);
    StringBuilder sb = new StringBuilder();
    TokenConsumer tc = new JsPrettyPrinter(sb);
    root.render(new RenderContext(tc));
    tc.noMoreTokens();
    assertEquals(
        "$0: {\n"
        + "  $1: {\n"
        + "    2;\n"
        + "    3;\n"
        + "    4;\n"
        + "  }\n"
        + "  $9: {\n"
        + "    10;\n"
        + "    11;\n"
        + "    12;\n"
        + "  }\n"
        + "  $5: {\n"
        + "    6;\n"
        + "    7;\n"
        + "    8;\n"
        + "  }\n"
        + "}",
        sb.toString());
  }

  public final void testVisitPreOrderPostInsert() {
    doInsert(5);
    IntEnqueuer ie = new IntEnqueuer();
    root.acceptPreOrder(ie, null);
    assertEquals("[0, 1, 2, 3, 4, 9, 10, 11, 12, 5, 6, 7, 8]",
                 ie.getNums().toString());
  }

  public final void testVisitPreorderDoesntDescendIntoInserted() {
    IntEnqueuerThatInserts ie = new IntEnqueuerThatInserts(5, b9);
    root.acceptPreOrder(ie, null);
    assertEquals("[0, 1, 2, 3, 4, 5, 6, 7, 8]",
                 ie.getNums().toString());
  }

  public final void testVisitPostOrderDoesntDescendIntoInserted() {
    IntEnqueuerThatInserts ie = new IntEnqueuerThatInserts(5, b9);
    root.acceptPostOrder(ie, null);
    assertEquals("[2, 3, 4, 1, 6, 7, 8, 5, 0]",
                 ie.getNums().toString());
  }

  public final void testFormatTreePostInsert2() throws Exception {
    doInsert(1);
    StringBuilder sb = new StringBuilder();
    MessageContext mc = new MessageContext();
    root.formatTree(mc, 0, sb);
    assertEquals(
        "LabeledStmtWrapper : $0\n"
        + "  Block\n"
        + "    LabeledStmtWrapper : $9\n"
        + "      Block\n"
        + "        ExpressionStmt\n"
        + "          IntegerLiteral : 10\n"
        + "        ExpressionStmt\n"
        + "          IntegerLiteral : 11\n"
        + "        ExpressionStmt\n"
        + "          IntegerLiteral : 12\n"
        + "    LabeledStmtWrapper : $1\n"
        + "      Block\n"
        + "        ExpressionStmt\n"
        + "          IntegerLiteral : 2\n"
        + "        ExpressionStmt\n"
        + "          IntegerLiteral : 3\n"
        + "        ExpressionStmt\n"
        + "          IntegerLiteral : 4\n"
        + "    LabeledStmtWrapper : $5\n"
        + "      Block\n"
        + "        ExpressionStmt\n"
        + "          IntegerLiteral : 6\n"
        + "        ExpressionStmt\n"
        + "          IntegerLiteral : 7\n"
        + "        ExpressionStmt\n"
        + "          IntegerLiteral : 8",
        sb.toString());
  }

  public final void testRenderPostInsert2() {
    doInsert(1);
    StringBuilder sb = new StringBuilder();
    TokenConsumer tc = new JsPrettyPrinter(sb);
    root.render(new RenderContext(tc));
    tc.noMoreTokens();
    assertEquals(
        "$0: {\n"
        + "  $9: {\n"
        + "    10;\n"
        + "    11;\n"
        + "    12;\n"
        + "  }\n"
        + "  $1: {\n"
        + "    2;\n"
        + "    3;\n"
        + "    4;\n"
        + "  }\n"
        + "  $5: {\n"
        + "    6;\n"
        + "    7;\n"
        + "    8;\n"
        + "  }\n"
        + "}",
        sb.toString());
  }

  public final void testVisitPreOrderPostInsert2() {
    doInsert(1);
    IntEnqueuer ie = new IntEnqueuer();
    root.acceptPreOrder(ie, null);
    assertEquals("[0, 9, 10, 11, 12, 1, 2, 3, 4, 5, 6, 7, 8]",
                 ie.getNums().toString());
  }

  public final void testVisitPreorderDoesntDescendIntoInserted2() {
    IntEnqueuerThatInserts ie = new IntEnqueuerThatInserts(1, b9);
    root.acceptPreOrder(ie, null);
    assertEquals("[0, 1, 2, 3, 4, 5, 6, 7, 8]",
                 ie.getNums().toString());
  }

  public final void testVisitPostOrderDoesntDescendIntoInserted2() {
    IntEnqueuerThatInserts ie = new IntEnqueuerThatInserts(1, b9);
    root.acceptPostOrder(ie, null);
    assertEquals("[2, 3, 4, 1, 6, 7, 8, 5, 0]",
                 ie.getNums().toString());
  }

  public final void testFormatTreePostInsert3() throws Exception {
    doInsert(-1);
    StringBuilder sb = new StringBuilder();
    MessageContext mc = new MessageContext();
    root.formatTree(mc, 0, sb);
    assertEquals(
        "LabeledStmtWrapper : $0\n"
        + "  Block\n"
        + "    LabeledStmtWrapper : $1\n"
        + "      Block\n"
        + "        ExpressionStmt\n"
        + "          IntegerLiteral : 2\n"
        + "        ExpressionStmt\n"
        + "          IntegerLiteral : 3\n"
        + "        ExpressionStmt\n"
        + "          IntegerLiteral : 4\n"
        + "    LabeledStmtWrapper : $5\n"
        + "      Block\n"
        + "        ExpressionStmt\n"
        + "          IntegerLiteral : 6\n"
        + "        ExpressionStmt\n"
        + "          IntegerLiteral : 7\n"
        + "        ExpressionStmt\n"
        + "          IntegerLiteral : 8\n"
        + "    LabeledStmtWrapper : $9\n"
        + "      Block\n"
        + "        ExpressionStmt\n"
        + "          IntegerLiteral : 10\n"
        + "        ExpressionStmt\n"
        + "          IntegerLiteral : 11\n"
        + "        ExpressionStmt\n"
        + "          IntegerLiteral : 12",
        sb.toString());
  }

  public final void testRenderPostInsert3() {
    doInsert(-1);
    StringBuilder sb = new StringBuilder();
    TokenConsumer tc = new JsPrettyPrinter(sb);
    root.render(new RenderContext(tc));
    tc.noMoreTokens();
    assertEquals(
        "$0: {\n"
        + "  $1: {\n"
        + "    2;\n"
        + "    3;\n"
        + "    4;\n"
        + "  }\n"
        + "  $5: {\n"
        + "    6;\n"
        + "    7;\n"
        + "    8;\n"
        + "  }\n"
        + "  $9: {\n"
        + "    10;\n"
        + "    11;\n"
        + "    12;\n"
        + "  }\n"
        + "}",
        sb.toString());
  }

  public final void testVisitPreOrderPostInsert3() {
    doInsert(-1);
    IntEnqueuer ie = new IntEnqueuer();
    root.acceptPreOrder(ie, null);
    assertEquals("[0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12]",
                 ie.getNums().toString());
  }

  void doRemove(int num) {
    ParseTreeNode rootBlock = root.children().get(0);
    assertTrue(rootBlock instanceof Block);
    ParseTreeNode b;
    switch (num) {
      case 1:
        b = rootBlock.children().get(0);
        break;
      case 5:
        b = rootBlock.children().get(1);
        break;
      default:
        fail(String.valueOf(num));
        return;
    }
    assertTrue(b instanceof LabeledStatement);
    assertEquals("$" + num, ((LabeledStatement) b).getLabel());

    ((Block) rootBlock).removeChild(b);
  }

  public final void testFormatTreePostRemove() throws Exception {
    doRemove(5);
    StringBuilder sb = new StringBuilder();
    MessageContext mc = new MessageContext();
    root.formatTree(mc, 0, sb);
    assertEquals(
        "LabeledStmtWrapper : $0\n"
        + "  Block\n"
        + "    LabeledStmtWrapper : $1\n"
        + "      Block\n"
        + "        ExpressionStmt\n"
        + "          IntegerLiteral : 2\n"
        + "        ExpressionStmt\n"
        + "          IntegerLiteral : 3\n"
        + "        ExpressionStmt\n"
        + "          IntegerLiteral : 4",
        sb.toString());
  }

  public final void testRenderPostRemove() {
    doRemove(5);
    StringBuilder sb = new StringBuilder();
    TokenConsumer tc = new JsPrettyPrinter(sb);
    root.render(new RenderContext(tc));
    tc.noMoreTokens();
    assertEquals(
        "$0: {\n"
        + "  $1: {\n"
        + "    2;\n"
        + "    3;\n"
        + "    4;\n"
        + "  }\n"
        + "}",
        sb.toString());
  }

  public final void testVisitPreOrderPostRemove() {
    doRemove(5);
    IntEnqueuer ie = new IntEnqueuer();
    root.acceptPreOrder(ie, null);
    assertEquals("[0, 1, 2, 3, 4]", ie.getNums().toString());
  }

  public final void testVisitPostOrderPostRemove() {
    doRemove(5);
    IntEnqueuer ie = new IntEnqueuer();
    root.acceptPostOrder(ie, null);
    assertEquals("[2, 3, 4, 1, 0]", ie.getNums().toString());
  }

  public final void testVisitPreorderDoesntDescendIntoRemoved() {
    IntEnqueuerThatRemoves ie = new IntEnqueuerThatRemoves(5);
    root.acceptPreOrder(ie, null);
    assertEquals("[0, 1, 2, 3, 4, 5]", ie.getNums().toString());
  }

  public final void testVisitPostOrderDescendsIntoRemoved() {
    IntEnqueuerThatRemoves ie = new IntEnqueuerThatRemoves(5);
    root.acceptPostOrder(ie, null);
    assertEquals("[2, 3, 4, 1, 6, 7, 8, 5, 0]", ie.getNums().toString());
  }

  public final void testFormatTreePostRemove2() throws Exception {
    doRemove(1);
    StringBuilder sb = new StringBuilder();
    MessageContext mc = new MessageContext();
    root.formatTree(mc, 0, sb);
    assertEquals(
        "LabeledStmtWrapper : $0\n"
        + "  Block\n"
        + "    LabeledStmtWrapper : $5\n"
        + "      Block\n"
        + "        ExpressionStmt\n"
        + "          IntegerLiteral : 6\n"
        + "        ExpressionStmt\n"
        + "          IntegerLiteral : 7\n"
        + "        ExpressionStmt\n"
        + "          IntegerLiteral : 8",
        sb.toString());
  }

  public final void testRenderPostRemove2() {
    doRemove(1);
    StringBuilder sb = new StringBuilder();
    TokenConsumer tc = new JsPrettyPrinter(sb);
    root.render(new RenderContext(tc));
    tc.noMoreTokens();
    assertEquals(
        "$0: {\n"
        + "  $5: {\n"
        + "    6;\n"
        + "    7;\n"
        + "    8;\n"
        + "  }\n"
        + "}",
        sb.toString());
  }

  public final void testVisitPreOrderPostRemove2() {
    doRemove(1);
    IntEnqueuer ie = new IntEnqueuer();
    root.acceptPreOrder(ie, null);
    assertEquals("[0, 5, 6, 7, 8]", ie.getNums().toString());
  }

  public final void testVisitPostOrderPostRemove2() {
    doRemove(1);
    IntEnqueuer ie = new IntEnqueuer();
    root.acceptPostOrder(ie, null);
    assertEquals("[6, 7, 8, 5, 0]", ie.getNums().toString());
  }

  public final void testVisitPreorderDoesntDescendIntoRemoved2() {
    IntEnqueuerThatRemoves ie = new IntEnqueuerThatRemoves(1);
    root.acceptPreOrder(ie, null);
    assertEquals("[0, 1, 5, 6, 7, 8]", ie.getNums().toString());
  }

  public final void testVisitPostOrderDescendsIntoRemoved2() {
    IntEnqueuerThatRemoves ie = new IntEnqueuerThatRemoves(1);
    root.acceptPostOrder(ie, null);
    assertEquals("[2, 3, 4, 1, 6, 7, 8, 5, 0]", ie.getNums().toString());
  }

  public final void testVisitPostOrderProceedsWhenNextDeleted() {
    doInsert(-1);
    IntEnqueuerThatMungesSiblings ie = new IntEnqueuerThatMungesSiblings(
        6, new long[] { 6, 7 }, new long[0]);
    root.acceptPreOrder(ie, null);
    //   $5: {
    //     6;
    //     7;
    //     8;
    //   }
    assertEquals("[0, 1, 2, 3, 4, 5, 8, 9, 10, 11, 12]",
                 ie.getNums().toString());
  }

  public final void testVisitPostOrderProceedsWhenAllNextDeleted1() {
    doInsert(-1);
    IntEnqueuerThatMungesSiblings ie = new IntEnqueuerThatMungesSiblings(
        7, new long[] { 7, 8 }, new long[] { 13 });
    root.acceptPreOrder(ie, null);
    assertEquals("[0, 1, 2, 3, 4, 5, 6, 13, 9, 10, 11, 12]",
                 ie.getNums().toString());
  }

  public final void testVisitPostOrderProceedsWhenAllNextDeleted2() {
    doInsert(-1);
    IntEnqueuerThatMungesSiblings ie = new IntEnqueuerThatMungesSiblings(
        6, new long[] { 6, 7, 8 }, new long[0]);
    root.acceptPreOrder(ie, null);
    assertEquals("[0, 1, 2, 3, 4, 5, 9, 10, 11, 12]",
                 ie.getNums().toString());
  }

  public final void testSerializable() throws Exception {
    assertSerializable(root);
  }

  public final void testIssue1369() throws Exception {
    ParseTreeNode p = js(fromString("var x = /asdf/;"));
    assertSerializable(p);
  }

  @SuppressWarnings("unchecked")
  // setComments causes a warning about generic array for varargs
  public final void testImmutability() throws Exception {
    Block n = new Block(
        FilePosition.UNKNOWN,
        Arrays.asList(
            new ExpressionStmt(new IntegerLiteral(FilePosition.UNKNOWN, 42)),
            new ExpressionStmt(new IntegerLiteral(FilePosition.UNKNOWN, 13))));
    assertTrue(n.makeImmutable());
    assertTrue(n.isImmutable());
    assertTrue(n.children().get(0).isImmutable());
    assertTrue(n.children().get(0).children().get(0).isImmutable());
    assertTrue(n.children().get(1).isImmutable());
    assertTrue(n.children().get(1).children().get(0).isImmutable());
    try {
      n.appendChild(new Noop(FilePosition.UNKNOWN));
      fail();
    } catch (UnsupportedOperationException e) { /* OK */ }
    try {
      n.setComments(Arrays.asList(
          Token.instance("test", JsTokenType.COMMENT, FilePosition.UNKNOWN)));
      fail();
    } catch (UnsupportedOperationException e) { /* OK */ }
  }

  private final class AlwaysMutable extends AbstractExpression {
    public AlwaysMutable() { super(FilePosition.UNKNOWN, StringLiteral.class); }
    @Override public Object getValue() { return null; }
    @Override public String typeOf() { return null; }
    @Override public void render(RenderContext r) { /* no output */ }
    @Override public boolean makeImmutable() { return false; /* refuse! */ }
  }

  public final void testIncompleteImmutability() throws Exception {
    // Immutability should be best-effort, stopping at a node that refuses
    Block n = new Block(
        FilePosition.UNKNOWN,
        Arrays.asList(
            new ExpressionStmt(new IntegerLiteral(FilePosition.UNKNOWN, 42)),
            new ExpressionStmt(new AlwaysMutable())));
    assertFalse(n.makeImmutable());
    assertFalse(n.isImmutable());
    assertTrue(n.children().get(0).isImmutable());
    assertTrue(n.children().get(0).children().get(0).isImmutable());
    assertFalse(n.children().get(1).isImmutable());
    assertFalse(n.children().get(1).children().get(0).isImmutable());
  }

  static class IntEnqueuer implements Visitor {
    private final List<Number> nums = new ArrayList<Number>();

    public final Number processNode(ParseTreeNode n) {
      Number num;
      if (n instanceof IntegerLiteral) {
        num = ((IntegerLiteral) n).getValue();
      } else if (n instanceof LabeledStatement) {
        num = Long.valueOf(((LabeledStatement) n).getLabel().substring(1));
      } else {
        return null;
      }
      nums.add(num);
      return num;
    }

    public boolean visit(AncestorChain<?> ancestors) {
      processNode(ancestors.node);
      return true;
    }

    List<Number> getNums() { return nums; }
  }

  static class IntEnqueuerExcept extends IntEnqueuer {
    private final long exception;

    IntEnqueuerExcept(long exception) {
      this.exception = exception;
    }

    @Override
    public boolean visit(AncestorChain<?> ancestors) {
      ParseTreeNode n = ancestors.node;
      Number num = processNode(n);
      return num == null || exception != num.longValue();
    }
  }

  static class IntEnqueuerThatReplaces extends IntEnqueuer {
    private final long toReplace;
    private final ParseTreeNode replacement;

    IntEnqueuerThatReplaces(long toReplace, ParseTreeNode replacement) {
      this.toReplace = toReplace;
      this.replacement = replacement;
    }

    @Override
    public boolean visit(AncestorChain<?> ancestors) {
      ParseTreeNode n = ancestors.node;
      Number num = processNode(n);
      if (null != num && num.longValue() == toReplace) {
        ((MutableParseTreeNode) ancestors.parent.node)
            .replaceChild(replacement, n);
      }
      return true;
    }
  }

  static class IntEnqueuerThatInserts extends IntEnqueuer {
    private final long num;
    private final ParseTreeNode toInsert;

    IntEnqueuerThatInserts(long num, ParseTreeNode toInsert) {
      this.num = num;
      this.toInsert = toInsert;
    }

    @Override
    public boolean visit(AncestorChain<?> ancestors) {
      ParseTreeNode n = ancestors.node;
      Number num = processNode(n);
      if (null != num && num.longValue() == this.num) {
        ((MutableParseTreeNode) ancestors.parent.node)
            .insertBefore(toInsert, n);
      }
      return true;
    }
  }

  static class IntEnqueuerThatRemoves extends IntEnqueuer {
    private final long toRemove;

    IntEnqueuerThatRemoves(long toRemove) {
      this.toRemove = toRemove;
    }

    @Override
    public boolean visit(AncestorChain<?> ancestors) {
      ParseTreeNode n = ancestors.node;
      Number num = processNode(n);
      if (null != num && num.longValue() == toRemove) {
        ((MutableParseTreeNode) ancestors.parent.node).removeChild(n);
      }
      return true;
    }
  }

  static class IntEnqueuerThatMungesSiblings extends IntEnqueuer {
    private final Set<Long> toRemove;
    private final long[] toAdd;
    private final long remover;

    IntEnqueuerThatMungesSiblings(long remover, long[] toRemove, long[] toAdd) {
      this.remover = remover;
      this.toRemove = new HashSet<Long>();
      for (int i = 0; i < toRemove.length; ++i) {
        this.toRemove.add(toRemove[i]);
      }
      this.toAdd = toAdd.clone();
    }

    @Override
    public boolean visit(AncestorChain<?> ancestors) {
      ParseTreeNode n = ancestors.node;
      processNode(n);

      if (n instanceof ExpressionStmt
          && (((IntegerLiteral) n.children().get(0)).getValue().longValue()
              == remover)) {
        MutableParseTreeNode p = (MutableParseTreeNode) ancestors.parent.node;

        List<ParseTreeNode> siblings
            = new ArrayList<ParseTreeNode>(p.children());
        for (ParseTreeNode sibling : siblings) {
          Number num = ((IntegerLiteral) sibling.children().get(0)).getValue();
          if (toRemove.contains(num.longValue())) {
            p.removeChild(sibling);
          }
        }
        for (int i = 0; i < toAdd.length; ++i) {
          p.appendChild(new ExpressionStmt(
              FilePosition.UNKNOWN,
              new IntegerLiteral(FilePosition.UNKNOWN, toAdd[i])));
        }
      }
      return true;
    }
  }
}
