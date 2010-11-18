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

package com.google.caja.ancillary.jsdoc;

import com.google.caja.lexer.ParseException;
import com.google.caja.parser.ParseTreeNodes;
import com.google.caja.parser.js.Expression;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessagePart;
import com.google.caja.util.CajaTestCase;

public class AnnotationHandlersTest extends CajaTestCase {
  private AnnotationHandlers handlers;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    handlers = new AnnotationHandlers(mc);
  }

  @Override
  public void tearDown() throws Exception {
    super.tearDown();
    handlers = null;
  }

  public final void testAuthor1() throws Exception {
    assertHandler(
        "/** @author me@foo.com */",
        ""
        + "{"
        + "  'url': 'mailto:me@foo.com'"
        + "}");
    assertNoErrors();
  }

  public final void testAuthor2() throws Exception {
    assertHandler(
        "/** @author Dr. Evil */",
        ""
        + "{"
        + "  'name': 'Dr. Evil'"
        + "}");
    assertNoErrors();
  }

  public final void testAuthor3() throws Exception {
    assertHandler(
        "/** @author Dr. Evil <me@foo.com> */",
        ""
        + "{"
        + "  'name': 'Dr. Evil',"
        + "  'url': 'mailto:me@foo.com'"
        + "}");
    assertNoErrors();
  }

  public final void testAuthor4() throws Exception {
    assertHandler(
        "/** @author me@foo.com (Dr. Evil) */",
        ""
        + "{"
        + "  'name': 'Dr. Evil',"
        + "  'url': 'mailto:me@foo.com'"
        + "}");
    assertNoErrors();
  }

  public final void testAuthor5() throws Exception {
    assertHandler("/** @author */", null);
    assertMessage(JsdocMessageType.EXPECTED_EMAIL_OR_NAME, MessageLevel.ERROR);
  }

  public final void testAuthor6() throws Exception {
    assertHandler("/** @author {@code h4xor} */", null);
    assertMessage(
        JsdocMessageType.EXPECTED_DOCUMENTATION_TEXT, MessageLevel.ERROR);
  }

  public final void testAuthor7() throws Exception {
    assertHandler("/** @author javascript:alert('h4xor') */", null);
    assertMessage(JsdocMessageType.EXPECTED_EMAIL_OR_NAME, MessageLevel.ERROR);
  }

  public final void testAuthor8() throws Exception {
    assertHandler(
        "/** @author <a href=mailto:me@foo.com>Dr. Evil</a> */",
        ""
        + "{"
        + "  'name': 'Dr. Evil',"
        + "  'url': 'mailto:me@foo.com'"
        + "}");
    assertNoErrors();
  }

  public final void testAuthor9() throws Exception {
    assertHandler(
        "/** @author <a name=mailto:me@foo.com>Dr. Evil</a> */", null);
    assertMessage(JsdocMessageType.EXPECTED_EMAIL_OR_NAME, MessageLevel.ERROR);
  }

  public final void testAuthor10() throws Exception {
    assertHandler("/** {@author me@foo.com} */", null);
    assertMessage(JsdocMessageType.ANNOTATION_OUT_OF_PLACE, MessageLevel.ERROR);
  }

  public final void testCode1() throws Exception {
    assertHandler(
        "/** {@code x=y} */",
        promiseWrapper(
            "'<code class=\\\"prettyprint\\\">'"
            + " + jsdoc___.html('x=y') + '</code>'"));
    assertNoErrors();
  }

  public final void testCode2() throws Exception {
    assertHandler("/** @code x=y */", null);
    assertMessage(JsdocMessageType.ANNOTATION_OUT_OF_PLACE, MessageLevel.ERROR);
  }

  public final void testCode3() throws Exception {
    assertHandler(
        "/** {@code} */",
        promiseWrapper(
            "'<code class=\\\"prettyprint\\\">' + jsdoc___.html('') + '</code>'"
            ));
    assertNoErrors();
  }

  public final void testConstructor1() throws Exception {
    assertHandler("/** @constructor */", "true");
    assertNoErrors();
  }

  public final void testConstructor2() throws Exception {
    assertHandler("/** {@constructor} */", null);
    assertMessage(JsdocMessageType.ANNOTATION_OUT_OF_PLACE, MessageLevel.ERROR);
  }

  public final void testConstructor3() throws Exception {
    assertHandler("/** @constructor foo */", null);
    assertMessage(
        JsdocMessageType.UNEXPECTED_CONTENT, MessageLevel.ERROR,
        MessagePart.Factory.valueOf("@constructor"));
  }

  public final void testExtends1() throws Exception {
    assertHandler(
        "/** @extends Foo */",
        promiseWrapper("jsdoc___.nameOf(Foo)"));
  }

  public final void testFileOverview1() throws Exception {
    assertHandler(
        "/** @fileoverview Fast, Spiffy, and {@link Taste delicious}. */",
        promiseWrapper(
            "'Fast, Spiffy, and ' + ('<a href=\\\"'"
            + " + jsdoc___.html("
                + "jsdoc___.linkTo(apiElement, apiElementName, Taste, 'Taste',"
                               + " 'testFileOverview1:1+44 - 49'))"
            + " + '\\\">' + jsdoc___.html('delicious') + '</a>') + '. '"));
    assertNoErrors();
  }

  public final void testFileOverview2() throws Exception {
    assertHandler("/** {@fileoverview howdy} */", null);
    assertMessage(JsdocMessageType.ANNOTATION_OUT_OF_PLACE, MessageLevel.ERROR);
  }

  public final void testLink1() throws Exception {
    assertHandler(
        "{@link foo}",
        promiseWrapper(
            "'<a href=\\\"'"
            + " + jsdoc___.html("
                + "jsdoc___.linkTo(apiElement, apiElementName, foo, 'foo',"
                               + " 'testLink1:1+1 - 12'))"
            + " + '\\\">'"
            + " + jsdoc___.html('foo')"
            + " + '</a>'"));
    assertNoErrors();
  }

  public final void testLink2() throws Exception {
    assertHandler(
        "{@link foo long description}",
        promiseWrapper(
            "'<a href=\\\"'"
            + " + jsdoc___.html("
                + "jsdoc___.linkTo(apiElement, apiElementName, foo, 'foo',"
                               + " 'testLink2:1+8 - 11'))"
            + " + '\\\">'"
            + " + jsdoc___.html('long description') + '</a>'"));
    assertNoErrors();
  }

  public final void testLink3() throws Exception {
    assertHandler("{@link}", null);
    assertMessage(
        JsdocMessageType.EXPECTED_DOCUMENTATION_TEXT, MessageLevel.ERROR);
  }

  public final void testLink4() throws Exception {
    assertHandler("{@link 4}", null);
    assertMessage(
        JsdocMessageType.EXPECTED_URL_OR_REFERENCE, MessageLevel.ERROR);
  }

  public final void testLink5() throws Exception {
    assertHandler(
        "{@link /foo.html}",
        promiseWrapper(
            "'<a href=\\\"' + jsdoc___.html('/foo.html') + '\\\">'"
            + " + jsdoc___.html('/foo.html') + '</a>'"));
    assertNoErrors();
  }

  public final void testLink6() throws Exception {
    assertHandler(
        "{@link /foo.html bar}",
        promiseWrapper(
            "'<a href=\\\"' + jsdoc___.html('/foo.html') + '\\\">'"
            + " + jsdoc___.html('bar') + '</a>'"));
    assertNoErrors();
  }

  public final void testParam1() throws Exception {
    assertHandler(
        "@param foo",
        promiseWrapper(
            "({"
            + " 'name': " + requireParamCall("'foo'") + ","
            + " 'summary': ''"
            + "})"));
    assertNoErrors();
  }

  public final void testParam2() throws Exception {
    assertHandler(
        "@param foo bar",
        promiseWrapper(
            "({"
            + " 'name': " + requireParamCall("'foo'") + ","
            + " 'summary': 'bar'"
            + "})"));
    assertNoErrors();
  }

  public final void testParam3() throws Exception {
    assertHandler(
        "@param {string} foo",
        promiseWrapper(
            "({"
            + " 'type': 'string',"
            + " 'name': " + requireParamCall("'foo'") + ","
            + " 'summary': ''"
            + "})"));
    assertNoErrors();
  }

  public final void testParam4() throws Exception {
    assertHandler(
        "@param {string | null} foo bar",
        promiseWrapper(
            "({"
            + " 'type': 'string | null',"
            + " 'name': " + requireParamCall("'foo'") + ","
            + " 'summary': 'bar'"
            + "})"));
    assertNoErrors();
  }

  public final void testParam5() throws Exception {
    assertHandler(
        "@param { Array.<number> } opt_foo defaults to {@code [1,2,3]}",
        promiseWrapper(
            "{"
            + " 'type': (jsdoc___.requireTypeAtoms("
            + "              'testParam5:1+8 - 26',"
            + "              [function () { return Array}, 'Array']),"
            + "          'Array.<number>'),"
            + " 'name': " + requireParamCall("'opt_foo'") + ","
            + " 'summary': ('defaults to '"
            + "             + ('<code class=\\\"prettyprint\\\">'"
            + "                + jsdoc___.html('[1,2,3]') + '</code>'))"
            + "}"));
    assertNoErrors();
  }

  public final void testParam6() throws Exception {
    assertHandler("@param {string}", null);
    assertMessage(JsdocMessageType.EXPECTED_IDENTIFIER, MessageLevel.ERROR);
  }

  public final void testParam7() throws Exception {
    assertHandler("@param", null);
    assertMessage(
        JsdocMessageType.EXPECTED_DOCUMENTATION_TEXT, MessageLevel.ERROR);
  }

  public final void testParam8() throws Exception {
    assertHandler("@param #foo", null);
    assertMessage(
        JsdocMessageType.EXPECTED_IDENTIFIER, MessageLevel.ERROR);
  }

  public final void testRequires() throws Exception {
    assertHandler(
        "@requires foo, bar",
        promiseWrapper(
            ""
            + "[ jsdoc___.linkTo(apiElement, apiElementName, foo, 'foo',"
                             + " 'testRequires:1+1 - 19'),"
            + "  jsdoc___.linkTo(apiElement, apiElementName, bar, 'bar',"
                             + " 'testRequires:1+1 - 19') ]"
            + ".join(' ')"));
  }

  public final void testProvides() throws Exception {
    assertHandler(
        "@provides foo, bar",
        promiseWrapper(
            ""
            + "["
            + "  jsdoc___.linkTo(apiElement, apiElementName, foo, 'foo',"
                             + " 'testProvides:1+1 - 19'),"
            + "  jsdoc___.linkTo(apiElement, apiElementName, bar, 'bar',"
                             + " 'testProvides:1+1 - 19') ]"
            + ".join(' ')"));
  }

  public final void testReturn1() throws Exception {
    assertHandler("@return {string}", "({ 'type': 'string', 'summary': '' })");
    assertNoErrors();
  }

  public final void testReturn2() throws Exception {
    assertHandler(
        "@return {string} foo like {@link #bar}",
        promiseWrapper(
            "({ 'type': 'string', 'summary': 'foo like '"
            + " + ('<a href=\\\"' + jsdoc___.html('#bar')"
            + " + '\\\">' + jsdoc___.html('#bar') + '</a>') })"));
    assertNoErrors();
  }

  public final void testReturn3() throws Exception {
    assertHandler("@return", null);
    assertMessage(
        JsdocMessageType.EXPECTED_DOCUMENTATION_TEXT, MessageLevel.ERROR);
  }

  public final void testReturn4() throws Exception {
    assertHandler("@return foo", "({ 'summary': 'foo' })");
    assertNoErrors();
  }

  public final void testSee1() throws Exception {
    assertHandler(
        "@see foo",
        promiseWrapper(
            "("
            + "{"
            + "  'url': jsdoc___.linkTo(apiElement, apiElementName, foo, 'foo',"
                                    + " 'testSee1:1+6 - 9')"
            + "})"));
    assertNoErrors();
  }

  public final void testSee2() throws Exception {
    assertHandler(
        "@see<A title=zoicks href=/foo.html>external link</A>",
        "({ 'name': 'external link', 'url': '/foo.html' })");
    assertNoErrors();
  }

  public final void testSee3() throws Exception {
    assertHandler("@see<A title=zoicks name='bar'>external link</A>", null);
    assertMessage(JsdocMessageType.BAD_LINK, MessageLevel.ERROR);
  }

  public final void testSee4() throws Exception {
    assertHandler(
        "@see <A href=\"javascript:alert('foo')\">external link</A>", null);
    assertMessage(JsdocMessageType.BAD_LINK, MessageLevel.ERROR);
  }

  public final void testSee5() throws Exception {
    assertHandler("@see", null);
    assertMessage(JsdocMessageType.BAD_LINK, MessageLevel.ERROR);
  }

  public final void testThrows1() throws Exception {
    assertHandler("@throws {string}", "({ 'type': 'string', 'summary': '' })");
    assertNoErrors();
  }

  public final void testThrows2() throws Exception {
    assertHandler(
        "@throws {string} foo like {@link #bar}",
        promiseWrapper(
            "({ 'type': 'string', 'summary': 'foo like '"
            + " + ('<a href=\\\"' + jsdoc___.html('#bar')"
            + " + '\\\">' + jsdoc___.html('#bar') + '</a>') })"));
    assertNoErrors();
  }

  public final void testThrows3() throws Exception {
    assertHandler("@throws", null);
    assertMessage(
        JsdocMessageType.EXPECTED_DOCUMENTATION_TEXT, MessageLevel.ERROR);
  }

  public final void testThrows4() throws Exception {
    assertHandler("@throws foo", "({ 'summary': 'foo' })");
    assertNoErrors();
  }

  public final void testThrows5() throws Exception {
    assertHandler("{@throws foo}", null);
    assertMessage(JsdocMessageType.ANNOTATION_OUT_OF_PLACE, MessageLevel.ERROR);
  }

  public final void testType1() throws Exception {
    assertHandler("@type {string}", "'string'");
    assertNoErrors();
  }

  public final void testType2() throws Exception {
    assertHandler("@type", null);
    assertMessage(JsdocMessageType.EXPECTED_TYPE, MessageLevel.ERROR);
  }

  public final void testType3() throws Exception {
    assertHandler("{@type {foo}}", null);
    assertMessage(JsdocMessageType.ANNOTATION_OUT_OF_PLACE, MessageLevel.ERROR);
  }

  public final void testUpdoc1() throws Exception {
    assertHandler(
        ""
        + "/**\n"
        + " * {@updoc\n"
        + " * $ abs(5)\n"
        + " * # 5\n"
        + " * $ abs(-5)\n"
        + " * # 5\n"
        + " * $ abs(0)\n"
        + " * # 0\n"
        + " * }\n"
        + " */",

        promiseWrapper(
            ""
            + "jsdoc___.updoc(["  // TODO: the positions below seem to be wrong
            + "    { doc:    '$ abs(5);\\n# 5;',"
            + "      input:  function () { return abs(5); },"
            + "      result: function () { return 5; },"
            + "      pos:    'testUpdoc1:2+6 - 3+4' },"
            + "    { doc:    '$ abs(-5);\\n# 5;',"
            + "      input:  function () { return abs(-5); },"
            + "      result: function () { return 5; },"
            + "      pos:    'testUpdoc1:4+3 - 5+4' },"
            + "    { doc:    '$ abs(0);\\n# 0;',"
            + "      input:  function () { return abs(0); },"
            + "      result: function () { return 0; },"
            + "      pos:    'testUpdoc1:6+3 - 7+4' }])"));
  }

  public final void testGuessAnnotation() throws Exception {
    assertHandler("/** @authop me@foo.com */", null);
    assertMessage(
        JsdocMessageType.UNRECOGNIZED_ANNOTATION, MessageLevel.ERROR,
        MessagePart.Factory.valueOf("authop"));
    assertMessage(
        JsdocMessageType.DID_YOU_MEAN, MessageLevel.LINT,
        MessagePart.Factory.valueOf("author"));
  }

  public final void testUngessableAnnotation() throws Exception {
    assertHandler("/** @Zzyzx */", null);
    assertMessage(JsdocMessageType.UNRECOGNIZED_ANNOTATION, MessageLevel.ERROR);
    assertNoMessage(JsdocMessageType.DID_YOU_MEAN);
  }

  private void assertHandler(String inputComment, String goldenJson)
      throws ParseException {
    Comment comment = CommentParser.parseStructuredComment(
        fromString(inputComment));
    Annotation toTest = null;
    for (Annotation a : comment.children()) {
      if (!(a instanceof TextAnnotation && "".equals(a.getValue().trim()))) {
        toTest = a;
        break;
      }
    }
    assert toTest != null;

    mq.getMessages().clear();
    Expression actual = handlers.handlerFor(toTest).handle(toTest, mq);
    if (goldenJson == null || actual == null) {
      assertEquals(goldenJson, actual != null ? render(actual) : null);
    } else {
      Expression golden = jsExpr(fromString(goldenJson));
      if (!ParseTreeNodes.deepEquals(golden, actual)) {
        assertEquals(render(golden), render(actual));
      }
    }
  }

  /**
   * Wraps code in a promise that will allow it to be executed after the
   * structural portion of the doc tree is built.
   */
  private static String promiseWrapper(String js) {
    return (
        "(function (docRoot, apiElementName) {"
        + "  var apiElement = this;"
        + "  return " + js + ";"
        + "})");
  }

  private static String requireParamCall(String paramNameJs) {
    return "jsdoc___.requireParam("
        + "    apiElement, apiElementName, jsdoc___.resolvePromise("
        + "        " + paramNameJs + ", docRoot, apiElementName, apiElement))";
  }
}
