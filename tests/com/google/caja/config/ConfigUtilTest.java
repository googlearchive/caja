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

package com.google.caja.config;

import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.ParseException;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.SimpleMessageQueue;
import com.google.caja.util.MoreAsserts;
import com.google.caja.util.TestUtil;

import java.io.StringReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

public class ConfigUtilTest extends TestCase {
  private MessageQueue mq;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    mq = new SimpleMessageQueue();
  }

  @Override
  public void tearDown() throws Exception {
    super.tearDown();
    mq = null;
  }

  // TODO(mikesamuel): better file positions for error messages.

  public void testEmptyConfigAllowNothing() throws Exception {
    WhiteList w = ConfigUtil.loadWhiteListFromJson(
        new StringReader("{}"), makeSrc(), mq);
    assertTrue(w.allowedItems().isEmpty());
    assertMessages();
  }

  public void testAllowed() throws Exception {
    WhiteList w = ConfigUtil.loadWhiteListFromJson(
        new StringReader(
            "{ \"allowed\": [ \"foo\", { \"key\" : \"bar\" } ] }"),
        makeSrc(), mq);
    assertTrue(w.allowedItems().contains("foo"));
    assertTrue(w.allowedItems().contains("bar"));
    assertEquals(2, w.allowedItems().size());
    assertMessages();
  }

  public void testDenied() throws Exception {
    WhiteList w = ConfigUtil.loadWhiteListFromJson(
        new StringReader(
            "{"
            + " \"denied\": [ \"foo\", { \"key\" : \"bar\" } ],"
            + " \"allowed\": [ \"bar\", { \"key\" : \"boo\" } ],"
            + "}"),
        makeSrc(), mq);
    assertTrue("boo", w.allowedItems().contains("boo"));
    assertFalse("bar", w.allowedItems().contains("bar"));
    assertFalse("foo", w.allowedItems().contains("foo"));
    assertEquals(1, w.allowedItems().size());
    assertMessages();
  }

  public void testMisspelledDenied() throws Exception {
    WhiteList w = ConfigUtil.loadWhiteListFromJson(
        new StringReader(
            "{"
            + " \"denies\": [ \"foo\", { \"key\" : \"bar\" } ],"
            + " \"allowed\": [ \"bar\", { \"key\" : \"boo\" } ],"
            + "}"),
        makeSrc(), mq);
    assertMessages(
        "WARNING: testMisspelledDenied:1+1: unrecognized key denies");
  }

  public void testAllowedOverridden() throws Exception {
    WhiteList w = ConfigUtil.loadWhiteListFromJson(
        new StringReader(
            "{"
            + " \"inherits\": [\"" + TestUtil.makeContentUrl(
                "{"
                + " \"allowed\": [ \"foo\", \"bar\" ]"
                + "}"
                ) + "\"],"
            + " \"denied\": [ \"foo\" ],"
            + "}"),
        makeSrc(), mq);
    assertTrue("bar", w.allowedItems().contains("bar"));
    assertFalse("foo", w.allowedItems().contains("foo"));
    assertEquals(1, w.allowedItems().size());
    assertMessages();
  }

  public void testDeniedOverridden() throws Exception {
    WhiteList w = ConfigUtil.loadWhiteListFromJson(
        new StringReader(
            "{"
            + " \"inherits\": [\"" + TestUtil.makeContentUrl(
                "{"
                + " \"allowed\": [ \"foo\" ],"
                + " \"denied\": [ \"foo\", \"bar\" ]"
                + "}"
                ) + "\"],"
            + " \"allowed\": [ \"bar\" ],"
            + "}"),
        makeSrc(), mq);
    assertTrue("bar", w.allowedItems().contains("bar"));
    assertFalse("foo", w.allowedItems().contains("foo"));
    assertEquals(1, w.allowedItems().size());
    assertMessages();
  }

  public void testDefinitionOverridden() throws Exception {
    WhiteList w = ConfigUtil.loadWhiteListFromJson(
        new StringReader(
            "{"
            + " \"inherits\": [\"" + TestUtil.makeContentUrl(
                "{"
                + " \"types\": ["
                + "     { \"key\": \"foo\", \"name\": \"Foo\" },"
                + "     { \"key\": \"bar\", \"name\": \"Bar\" }"
                + "     ],"
                + "}"
                ) + "\"],"
            + " \"types\": ["
            + "     { \"key\": \"foo\", \"name\": \"FOO\" },"
            + "     { \"key\": \"baz\", \"name\": \"BAZ\" }"
            + "     ]"
            + "}"),
        makeSrc(), mq);
    assertEquals("FOO", w.typeDefinitions().get("foo").get("name", null));
    assertEquals("Bar", w.typeDefinitions().get("bar").get("name", null));
    assertEquals("BAZ", w.typeDefinitions().get("baz").get("name", null));
    assertMessages();
  }

  public void testMissingUrl() throws Exception {
    try {
      WhiteList w = ConfigUtil.loadWhiteListFromJson(
          new StringReader(
              "{"
              + " \"inherits\": [ {} ]"
              + "}"),
          makeSrc(), mq);
      fail("parsing not aborted");
    } catch (ParseException ex) {
      assertMessages();
      ex.toMessageQueue(mq);
    }
    assertMessages(
        "FATAL_ERROR: testMissingUrl:1+1"
        + ": malformed config file: expected inherits src, not null");
  }

  public void testDuplicatedDefinitionsOk() throws Exception {
    WhiteList w = ConfigUtil.loadWhiteListFromJson(
        new StringReader(
            "{"
            + " \"inherits\": [\"" + TestUtil.makeContentUrl(
                "{"
                + " \"types\": ["
                + "     { \"key\": \"foo\", \"name\": \"Foo\" },"
                + "     ]"
                + "}")
            + "\", \"" + TestUtil.makeContentUrl(
                "{"
                + " \"types\": ["
                + "     { \"key\": \"foo\", \"name\": \"Foo\" },"
                + "     ],"
                + "}"
                ) + "\"]"
            + "}"),
        makeSrc(), mq);
    assertEquals("Foo", w.typeDefinitions().get("foo").get("name", null));
    assertMessages();
  }

  public void testOverriddenDuplicatedDefinitionsOk() throws Exception {
    WhiteList w = ConfigUtil.loadWhiteListFromJson(
        new StringReader(
            "{"
            + " \"inherits\": [\"" + TestUtil.makeContentUrl(
                "{"
                + " \"types\": ["
                + "     { \"key\": \"foo\", \"name\": \"Foo\" },"
                + "     ]"
                + "}")
            + "\", \"" + TestUtil.makeContentUrl(
                "{"
                + " \"types\": ["
                + "     { \"key\": \"foo\", \"name\": \"Foo!!\" },"
                + "     ],"
                + "}"
                ) + "\"],"
            + " \"types\": ["
            + "     { \"key\": \"foo\", \"name\": \"FOO\" },"
            + "     ]"
            + "}"),
        makeSrc(), mq);
    assertEquals("FOO", w.typeDefinitions().get("foo").get("name", null));
    assertMessages();
  }

  public void testUnresolvedAmbiguousDefinition() throws Exception {
    WhiteList w = ConfigUtil.loadWhiteListFromJson(
        new StringReader(
            "{"
            + " \"inherits\": [\"" + TestUtil.makeContentUrl(
                "{"
                + " \"types\": ["
                + "     { \"key\": \"foo\", \"name\": \"Foo\" },"
                + "     ]"
                + "}")
            + "\", \"" + TestUtil.makeContentUrl(
                "{"
                + " \"types\": ["
                + "     { \"key\": \"foo\", \"name\": \"Foo!!\" },"
                + "     ],"
                + "}"
                ) + "\"]"
            + "}"),
        makeSrc(), mq);
    assertEquals("Foo", w.typeDefinitions().get("foo").get("name", null));
    assertMessages("FATAL_ERROR: testUnresolvedAmbiguousDefinition:1+1:"
                   + " ambiguous type definition {@} != {@}");
  }

  public void testConflictsBetweenInheritedTypesResolved() throws Exception {
    WhiteList w = ConfigUtil.loadWhiteListFromJson(
        new StringReader(
            "{"
            + " \"inherits\": [\"" + TestUtil.makeContentUrl(
                // This one has an ambiguous for foo.
                "{"
                + " \"inherits\": [\"" + TestUtil.makeContentUrl(
                    "{"
                    + " \"types\": ["
                    + "     { \"key\": \"foo\", \"name\": \"Foo-1\" },"
                    + "     ]"
                    + "}")
                + "\", \"" + TestUtil.makeContentUrl(
                    "{"
                    + " \"types\": ["
                    + "     { \"key\": \"foo\", \"name\": \"Foo-2\" },"
                    + "     ],"
                    + "}"
                    ) + "\"]"
                + "}"
                ) + "\"],"
            // But the ambiguity is resolved in a containing white-list.
            + " \"types\": ["
            + "     { \"key\": \"foo\", \"name\": \"Foo-3\" },"
            + "     ]"
            + "}"),
        makeSrc(), mq);
    assertEquals("Foo-3", w.typeDefinitions().get("foo").get("name", null));
    assertMessages();
  }

  private void assertMessages(String... golden) {
    List<String> actual = new ArrayList<String>();
    for (Message msg : mq.getMessages()) {
      // Simple JSON doesn't preserve key ordering.
      String msgText = msg.toString().replaceAll(JSON_FLAT_OBJECT, "{@}");
      actual.add(msg.getMessageLevel() + ": " + msgText);
    }
    MoreAsserts.assertListsEqual(Arrays.asList(golden), actual);
  }

  private FilePosition makeSrc() throws Exception {
    return makeSrc(null);
  }

  private FilePosition makeSrc(String suffix) throws Exception {
    InputSource is = new InputSource(URI.create(
        "test:///" + getName() + (suffix != null ? "/" + suffix : "")));
    return FilePosition.startOfFile(is);
  }

  private static final String JSON_STRING = "(?:\"(?:[^\"\\\\]|\\\\.)*\")";
  private static final String JSON_NUMBER = (
      "(?:[+-]?(?:(?:(?:0|[1-9][0-9]*)(?:\\.[0-9]*)?)|\\.[0-9]+)"
      + "(?:[eE][+-]?[0-9]+)?)");
  private static final String JSON_ATOM = (
      "(?:" + JSON_STRING + "|null|true|false|" + JSON_NUMBER + ")");
  private static final String JSON_FLAT_OBJECT = (
      "\\{(?:" + JSON_STRING + ":" + JSON_ATOM + ",?)*\\}");
}
