// Copyright (C) 2005 Google Inc.
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

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import junit.framework.TestCase;

/**
 * testcases for {@link PunctuationTrie}.
 *
 * @author mikesamuel@gmail.com
 */
public class PunctuationTrieTest extends TestCase {
  PunctuationTrie<?> jsPunc;
  PunctuationTrie<Integer> skinny;

  @Override
  public void setUp() throws Exception {
    Map<String, Void> jsPuncStrs = new TreeMap<String, Void>();
    for (Punctuation p : Punctuation.values()) {
      if (p.toString().equals("..")) {
        // Check the PunctuationTrie works correctly when there is a
        // non-terminal that is not a prefix of a terminal.
        continue;
      }
      jsPuncStrs.put(p.toString(), null);
    }
    this.jsPunc = new PunctuationTrie<Void>(jsPuncStrs);
    this.skinny = new PunctuationTrie<Integer>(
        Collections.singletonMap("hellooooooo", 4));
  }

  @Override
  public void tearDown() throws Exception {
    this.jsPunc = null;
    this.skinny = null;
  }

  public final void testTreeStructure() {
    String jsTree = (
        "nonterminal\n" +
        "\t'!' terminal\n" +
        "\t\t'=' terminal\n" +
        "\t\t\t'=' terminal\n" +
        "\t'%' terminal\n" +
        "\t\t'=' terminal\n" +
        "\t'&' terminal\n" +
        "\t\t'&' terminal\n" +
        "\t\t\t'=' terminal\n" +
        "\t\t'=' terminal\n" +
        "\t'(' terminal\n" +
        "\t')' terminal\n" +
        "\t'*' terminal\n" +
        "\t\t'=' terminal\n" +
        "\t'+' terminal\n" +
        "\t\t'+' terminal\n" +
        "\t\t'=' terminal\n" +
        "\t',' terminal\n" +
        "\t'-' terminal\n" +
        "\t\t'-' terminal\n" +
        "\t\t'=' terminal\n" +
        "\t'.' terminal\n" +
        "\t\t'.' nonterminal\n" +
        "\t\t\t'.' terminal\n" +
        "\t'/' terminal\n" +
        "\t\t'=' terminal\n" +
        "\t':' terminal\n" +
        "\t\t':' terminal\n" +
        "\t';' terminal\n" +
        "\t'<' terminal\n" +
        "\t\t'<' terminal\n" +
        "\t\t\t'=' terminal\n" +
        "\t\t'=' terminal\n" +
        "\t'=' terminal\n" +
        "\t\t'=' terminal\n" +
        "\t\t\t'=' terminal\n" +
        "\t'>' terminal\n" +
        "\t\t'=' terminal\n" +
        "\t\t'>' terminal\n" +
        "\t\t\t'=' terminal\n" +
        "\t\t\t'>' terminal\n" +
        "\t\t\t\t'=' terminal\n" +
        "\t'?' terminal\n" +
        "\t'[' terminal\n" +
        "\t']' terminal\n" +
        "\t'^' terminal\n" +
        "\t\t'=' terminal\n" +
        "\t'{' terminal\n" +
        "\t'|' terminal\n" +
        "\t\t'=' terminal\n" +
        "\t\t'|' terminal\n" +
        "\t\t\t'=' terminal\n" +
        "\t'}' terminal\n" +
        "\t'~' terminal"
    );
    String skinnyTree = (
        "nonterminal"
        + "\n\t'h' nonterminal"
        + "\n\t\t'e' nonterminal"
        + "\n\t\t\t'l' nonterminal"
        + "\n\t\t\t\t'l' nonterminal"
        + "\n\t\t\t\t\t'o' nonterminal"
        + "\n\t\t\t\t\t\t'o' nonterminal"
        + "\n\t\t\t\t\t\t\t'o' nonterminal"
        + "\n\t\t\t\t\t\t\t\t'o' nonterminal"
        + "\n\t\t\t\t\t\t\t\t\t'o' nonterminal"
        + "\n\t\t\t\t\t\t\t\t\t\t'o' nonterminal"
        + "\n\t\t\t\t\t\t\t\t\t\t\t'o' terminal"
        );

    assertEquals(jsTree, jsPunc.toString());
    assertEquals(skinnyTree, skinny.toString());
  }

  public final void testPunctuationTrie() {
    // make sure that we can find strings in jsPunc
    Set<PunctuationTrie<?>> uniq = new HashSet<PunctuationTrie<?>>();
    for (Punctuation p : Punctuation.values()) {
      if (p.toString().equals("..")) { continue; }
      PunctuationTrie<?> t = jsPunc.lookup(p.toString());
      assertTrue(t != null && t.isTerminal());
      assertTrue(uniq.add(t));
    }

    // check that we can't find other strings
    assertEquals(null, jsPunc.lookup("foo"));
    assertEquals(null, jsPunc.lookup("hi"));
    assertEquals(null, jsPunc.lookup("<<<<<<<"));
    assertEquals(null, jsPunc.lookup("<<<<"));
    assertEquals(null, jsPunc.lookup("===="));

    // check substrings that are not themselves punctuation
    assertTrue(!jsPunc.lookup("..").isTerminal());
  }

  public final void testSkinnyTrie() {
    String s = "hellooooooo";
    PunctuationTrie<Integer> t = skinny;
    for (int i = 0; i < s.length(); ++i) {
      assertTrue(!t.isTerminal());
      assertEquals(null, t.lookup(' '));
      assertEquals(null, t.getValue());
      t = t.lookup(s.charAt(i));
    }
    assertTrue(t.isTerminal());
    assertEquals(null, t.lookup('o'));
    assertEquals(4, t.getValue().intValue());
  }
}
