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

package com.google.caja.plugin;

import com.google.caja.lang.css.CssSchema;
import com.google.caja.lang.html.HtmlSchema;
import com.google.caja.lexer.ExternalReference;
import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.ParseException;
import com.google.caja.parser.AncestorChain;
import com.google.caja.parser.MutableParseTreeNode;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.Visitor;
import com.google.caja.parser.css.CssTree;
import com.google.caja.parser.js.ArrayConstructor;
import com.google.caja.parser.js.Expression;
import com.google.caja.parser.js.ObjProperty;
import com.google.caja.parser.js.ObjectConstructor;
import com.google.caja.parser.js.Operation;
import com.google.caja.parser.js.Operator;
import com.google.caja.parser.js.StringLiteral;
import com.google.caja.parser.js.ValueProperty;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessagePart;
import com.google.caja.util.CajaTestCase;
import com.google.caja.util.Function;
import com.google.caja.util.Lists;
import com.google.caja.util.MoreAsserts;
import com.google.caja.util.Name;
import com.google.common.collect.Sets;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.AssertionFailedError;

/**
 *
 * @author mikesamuel@gmail.com
 */
public class CssRewriterTest extends CajaTestCase {

  public final void testCssRewriterEquivalence() throws Exception {
    Expression tests = jsExpr(fromResource("css-stylesheet-tests.js"));
    // tests is a JSONP style JavaScript expression.
    // Normalize "foo" + "bar" -> "foo bar"
    tests.acceptPostOrder(new Visitor() {
      @Override
      public boolean visit(AncestorChain<?> chain) {
        if (Operation.is(chain.node, Operator.ADDITION)) {
          Operation op = chain.cast(Operation.class).node;
          Expression left = op.children().get(0);
          Expression right = op.children().get(1);
          if (left instanceof StringLiteral && right instanceof StringLiteral) {
            StringLiteral concatenation = StringLiteral.valueOf(
                FilePosition.span(
                    left.getFilePosition(), right.getFilePosition()),
                ((StringLiteral) left).getUnquotedValue()
                + ((StringLiteral) right).getUnquotedValue());
            chain.parent.cast(MutableParseTreeNode.class).node.replaceChild(
                concatenation, op);
          }
        }
        return true;
      }
    }, null);

    AssertionFailedError failure = null;

    // InputSource for file positions in error message goldens.
    is = new InputSource(new URI("http://example.org/test"));

    // Extract the JSON style-object from the call.
    assertTrue(render(tests), Operation.is(tests, Operator.FUNCTION_CALL));
    Operation call = (Operation) tests;
    assertEquals(2, call.children().size());
    Expression testArray = call.children().get(1);
    // testArray is an array like
    // [{ test_name: ..., tests: [] }]
    for (Expression test : ((ArrayConstructor) testArray).children()) {
      ObjectConstructor obj = (ObjectConstructor) test;
      ValueProperty es5Prop = (ValueProperty) obj.propertyWithName("es5only");
      if (es5Prop != null && (Boolean) es5Prop.getValueExpr().getValue()) {
        continue;
      }
      String name = (String)
           ((ValueProperty) obj.propertyWithName("test_name"))
           .getValueExpr().getValue();
      ValueProperty testcases = (ValueProperty) obj.propertyWithName("tests");
      // testcases is an object like
      // [{ cssText: ..., golden: ..., messages: ... }]
      for (Expression testCase
           : ((ArrayConstructor) testcases.getValueExpr()).children()) {
        ObjectConstructor testCaseObj = (ObjectConstructor) testCase;
        String cssText = null;
        String golden = null;
        ArrayConstructor messages = null;
        for (ObjProperty oprop : testCaseObj.children()) {
          ValueProperty prop = (ValueProperty) oprop;
          String pname = prop.getPropertyName();
          try {
            if ("cssText".equals(pname)) {
              cssText = ((StringLiteral) prop.getValueExpr())
                  .getUnquotedValue();
            } else if ("golden".equals(pname)) {
              golden = ((StringLiteral) prop.getValueExpr())
                  .getUnquotedValue();
            } else if ("messages".equals(pname)) {
              messages = (ArrayConstructor) prop.getValueExpr();
            } else if ("altGolden".equals(pname)) {
              // OK.
            } else {
              fail(
                  "Unrecognized testcase property " + pname + " in "
                  + render(testCase) + " at " + testCase.getFilePosition());
            }
          } catch (RuntimeException ex) {
            System.err.println(
                "Type mismatch in " + name
                + " at " + testCase.getFilePosition());
            throw ex;
          }
        }

        String normalizedGolden = "".equals(golden)
            ? "" : render(css(fromString(golden)));

        mq.getMessages().clear();
        try {
          runTest(cssText, normalizedGolden);
          if (messages != null) {
            for (Expression message : messages.children()) {
              ObjectConstructor messageObj = (ObjectConstructor) message;
              String type = ((StringLiteral)
                  ((ValueProperty) messageObj.propertyWithName("type"))
                  .getValueExpr())
                  .getUnquotedValue();
              String level = ((StringLiteral)
                  ((ValueProperty) messageObj.propertyWithName("level"))
                  .getValueExpr())
                  .getUnquotedValue();
              List<String> args = Lists.newArrayList();
              ArrayConstructor argsArray = (ArrayConstructor)
                  ((ValueProperty) messageObj.propertyWithName("args"))
                  .getValueExpr();
              for (Expression argExpr : argsArray.children()) {
                args.add(((StringLiteral) argExpr).getUnquotedValue());
              }
              consumeMessage(message.getFilePosition(), type, level, args);
            }
            assertNoErrors();
          }
        } catch (Exception ex) {
          System.err.println("Test " + name + "\n" + render(testCase));
          throw ex;
        } catch (AssertionFailedError ex) {
          System.err.println("Test " + name + "\n" + render(testCase));
          ex.printStackTrace();
          if (failure == null) {
            failure = ex;
          }
        }
      }
    }
    if (failure != null) { throw failure; }
  }

  public final void testGradients() throws Exception {
    runTest("p { background-image:gradient(linear, left top, left bottom)}",
            "", false);
    assertNoErrors();

    String pre = ".namespace__ p {\n  ";

    // A gradient on 45deg axis starting blue and finishing red
    runTest(
        "p {   background-image: linear-gradient(45deg, blue, red) }",
        pre + "background-image: linear-gradient(45deg, blue, red)\n}");
    assertNoErrors();

    // TODO(felix8a): -vendor-func() doesn't work yet
    /*
    runTest(
        "p {   background-image: -webkit-linear-gradient(1deg, blue, red) }",
        pre + "background-image: -webkit-linear-gradient(1deg, blue, red)\n}");
    assertNoErrors();
    */

    // A gradient going from the bottom right to the top left starting blue and
    // finishing red
    runTest(
        "p   { background-image:linear-gradient( to left top, blue, red); }",
        pre + "background-image: linear-gradient(to left top, blue, red)\n}");
    assertNoErrors();

    // A gradient going from the bottom to top, starting blue, being green
    // after 40%  and finishing red
    runTest(
        "p { background-image:linear-gradient( 0deg, blue, green 40%, red ); }",
        pre + "background-image: linear-gradient(0deg, blue, green 40%, red)\n}"
        );

    runTest(
       ""
       + "li {"
       + "  list-style-image:repeating-radial-gradient("
       + "    circle closest-side at 20px 30px,"
       + "    red, yellow, green 100%, yellow 150%, red 200%"
       + "  );"
       + "}",
       "");

  }

  public final void testSubstitutions() throws Exception {
    try {
      runTest("#foo { left: ${x * 4}px; top: ${y * 4}px; }",
              "", false);
      fail("allowed substitutions when parsing of substitutions disabled");
    } catch (ParseException ex) {
      // pass
    }
    runTest(
        "#foo { left: ${x * 4}px; top: ${y * 4}px; }",
        ".namespace__ #foo-namespace__"
        + " {\n  left: ${x * 4}px;\n  top: ${y * 4}px\n}",
        true);
  }

  public final void testZIndexRange() throws Exception {
    runTest("div { z-index: 0 }", ".namespace__ div {\n  z-index: 0\n}", false);
    assertNoErrors();
    runTest(
        "div { z-index: -9999999 }",
        ".namespace__ div {\n  z-index: -9999999\n}",
        false);
    assertNoErrors();
    runTest(
        "div { z-index: 9999999 }",
        ".namespace__ div {\n  z-index: 9999999\n}",
        false);
    assertNoErrors();
    runTest(
        "div { z-index: -10000000 }",
        ".namespace__ div {\n  z-index: -10000000\n}",
        false);
    assertMessage(PluginMessageType.CSS_VALUE_OUT_OF_RANGE,
        MessageLevel.WARNING,
        Name.css("z-index"));
    runTest(
        "div { z-index: 10000000 }",
        ".namespace__ div {\n  z-index: 10000000\n}",
        false);
    assertMessage(PluginMessageType.CSS_VALUE_OUT_OF_RANGE,
        MessageLevel.WARNING,
        Name.css("z-index"));
  }


  public final void testUrisCalledWithProperPropertyPart() throws Exception {
    // The CssRewriter needs to rewrite URIs.
    // When it does so it passes a context string.
    assertCallsUriRewriterWithPropertyPart(
        "background: 'foo.png'",
        "background::bg-image::image");
    assertCallsUriRewriterWithPropertyPart(
        ""
        + "img.trans {"
        + "  filter: progid:DXImageTransform.Microsoft.AlphaImageLoader("
        + "      src='bar.png', sizingMethod='image');"
        + "}",
        "filter::prog-id::prog-id-alpha-image-loader::page-url");
  }

  private void assertUriPolicy(
      UriPolicy uriPolicy,
      String css,
      List<String> urisExpectedSafe,
      List<String> urisExpectedUnsafe)
      throws Exception {
    final List<String> urisFoundSafe = Lists.newArrayList();
    final List<String> urisFoundUnsafe = Lists.newArrayList();
    CssTree t = css(fromString(css), false);
    new CssValidator(CssSchema.getDefaultCss21Schema(mq),
        HtmlSchema.getDefault(mq), mq)
        .validateCss(AncestorChain.instance(t));
    new CssRewriter(uriPolicy, HtmlSchema.getDefault(mq), mq)
        .rewrite(AncestorChain.instance(t));
    t.acceptPreOrder(new Visitor() {
      public boolean visit(AncestorChain<?> ancestors) {
        ParseTreeNode node = ancestors.node;
        if (node instanceof CssTree.UriLiteral) {
          String value = ((CssTree.CssLiteral) node).getValue();
          if (node instanceof SafeUriLiteral) {
            urisFoundSafe.add(value);
          } else if (node instanceof UnsafeUriLiteral) {
            urisFoundUnsafe.add(value);
          } else {
            fail("Tree should not contain any plain CssTree.UriLiteral");
          }
        }
        return true;
      }
    }, null);
    MoreAsserts.assertListsEqual(
        urisExpectedSafe,
        Lists.newArrayList(urisFoundSafe));
    MoreAsserts.assertListsEqual(
        urisExpectedUnsafe,
        Lists.newArrayList(urisFoundUnsafe));
  }

  public final void testUriPolicyPresent() throws Exception {
    assertUriPolicy(
        UriPolicy.IDENTITY,
        ""
            + "div { background: url(bar.png); }",
        Arrays.asList("http://example.org/bar.png"),
        Arrays.<String>asList());
    assertUriPolicy(
        UriPolicy.IDENTITY,
        ""
        + "div { background: 'bar.png' }",
        Arrays.asList("http://example.org/bar.png"),
        Arrays.<String>asList());
  }

  public final void testUriPolicyAbsent() throws Exception {
    assertUriPolicy(
        null,
        ""
        + "div { background-image: url(bar.png); }",
        Arrays.<String>asList(),
        Arrays.asList("http://example.org/bar.png"));
    assertUriPolicy(
        null,
        ""
        + "div { background-image: url(bar.png); }",
        Arrays.<String>asList(),
        Arrays.asList("http://example.org/bar.png"));
  }

  private void runTest(String css, String golden) throws Exception {
    runTest(css, golden, false);
  }

  private void runTest(String css, String golden, boolean allowSubstitutions)
      throws Exception {
    mq.getMessages().clear();
    mc.relevantKeys = Collections.singleton(CssValidator.INVALID);

    CssTree t = css(fromString(css), allowSubstitutions);

    String msg;
    {
      StringBuilder msgBuf = new StringBuilder();
      t.formatTree(mc, 0, msgBuf);
      msg = msgBuf.toString();
    }

    CssSchema cssSchema = CssSchema.getDefaultCss21Schema(mq);
    HtmlSchema htmlSchema = HtmlSchema.getDefault(mq);
    new CssValidator(cssSchema, htmlSchema, mq)
        .validateCss(AncestorChain.instance(t));
    new CssRewriter(
        new UriPolicy() {
          public String rewriteUri(
              ExternalReference ref, UriEffect effect, LoaderType loader,
              Map<String, ?> hints) {
            URI uri = ref.getUri();

            if ("http".equals(uri.getScheme())  // Used by CajaTestCase
                && "example.org".equals(uri.getHost())
                && uri.getPath() != null
                && uri.getPath().startsWith("/")) {
              try {
                return new URI(null, null, "/foo" + uri.getPath(),
                               uri.getQuery(), uri.getFragment())
                    .toString();
              } catch (URISyntaxException ex) {
                ex.printStackTrace();
                return null;
              }
            } else if ("whitelisted-host.com".equals(uri.getHost())) {
              return uri.toString();
            } else {
              return null;
            }
          }
        },
        htmlSchema, mq)
        .rewrite(AncestorChain.instance(t));

    {
      StringBuilder msgBuf = new StringBuilder();
      t.formatTree(mc, 0, msgBuf);
      msg += "\n  ->\n" + msgBuf.toString();
    }

    assertEquals(msg, golden, render(t));
  }

  private void assertCallsUriRewriterWithPropertyPart(
      String cssCode, String... expectedParts)
      throws ParseException {
    final Set<String> propertyParts = Sets.newLinkedHashSet();

    CssTree t = cssCode.trim().endsWith("}")
        ? css(fromString(cssCode)) : cssDecls(fromString(cssCode));

    CssSchema cssSchema = CssSchema.getDefaultCss21Schema(mq);
    HtmlSchema htmlSchema = HtmlSchema.getDefault(mq);
    new CssValidator(cssSchema, htmlSchema, mq)
        .validateCss(AncestorChain.instance(t));
    new CssRewriter(
        new UriPolicy() {
          public String rewriteUri(
              ExternalReference ref, UriEffect effect, LoaderType loader,
              Map<String, ?> hints) {
            propertyParts.add(
                UriPolicyHintKey.CSS_PROP.valueFrom(hints)
                    .getCanonicalForm());
            return ref.getUri().toString();
          }
        },
        htmlSchema, mq)
        .rewrite(AncestorChain.instance(t));

    MoreAsserts.assertListsEqual(
        Arrays.asList(expectedParts),
        Lists.newArrayList(propertyParts));
  }

  private void consumeMessage(
      FilePosition pos, final String type, final String level,
      final List<String> parts) {
    try {
      assertMessage(
          true,
          new Function<Message, Integer>() {
            @Override
            public Integer apply(Message msg) {
              int score = 0;
              if (msg.getMessageType().name().equals(type)) { ++score; }
              if (msg.getMessageLevel().name().equals(level)) { ++score; }
              score -= partsMissing(msg, parts);
              return (score == 2) ? Integer.MAX_VALUE : score;
            }
          }, "type=" + type + ", level=" + level);
    } catch (AssertionFailedError err) {
      System.err.println("Message specified at " + pos + " was not found");
      throw err;
    }
  }

  private static int partsMissing(Message msg, List<? extends String> parts) {
    int missing = 0;
    outerLoop:
    for (String expectedPart : parts) {
      for (MessagePart candidate : msg.getMessageParts()) {
        String candidatePart = candidate.toString();
        if (candidatePart.equals(expectedPart)) { continue outerLoop; }
      }
      ++missing;
    }
    return missing;
  }
}
