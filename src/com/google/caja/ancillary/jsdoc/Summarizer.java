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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.caja.lexer.FilePosition;

/**
 * Implements Javadoc summarization rules.
 *
 * @author mikesamuel@gmail.com
 */
final class Summarizer {
  /**
   * Matches the end of a sentence.
   * A non-space character followed by a '.' followed by a space character.
   * Note, this does not match a '.' at the end of a input, since the end
   * of input is always assumed to end the sentence.
   */
  private static final Pattern SENTENCE_END = Pattern.compile("\\S[.?](?=\\s)");
  private static int summaryLength(String text) {
    text = text.replaceAll("\\b([iI]\\.e\\.|[Ee]\\.g\\.)\\s", "$1#");
    Matcher m = SENTENCE_END.matcher(text);
    if (!m.find()) { return text.length(); }
    return m.end();
  }

  /**
   * Returns a block annotation containing the first sentence of the given
   * annotation using javadoc rules.
   */
  public static BlockAnnotation summarize(BlockAnnotation a) {
    List<? extends Annotation> children = a.children();
    int nWhole = 0;  // The number of whole children from the start of a to use.
    int partialLength = 0;  // The count of chars in the nWhole-th child to use.
    while (nWhole < children.size()) {
      Annotation child = children.get(nWhole);
      if (child instanceof TextAnnotation) {
        int summaryLength = summaryLength(child.getValue());
        if (summaryLength < child.getValue().length()) {
          partialLength = summaryLength;
          break;
        }
      }
      ++nWhole;
    }
    List<Annotation> summaryChildren = new ArrayList<Annotation>(
        children.subList(0, nWhole));
    if (partialLength != 0) {
      TextAnnotation last = (TextAnnotation) children.get(nWhole);
      summaryChildren.add(last.slice(0, partialLength));
    }
    Annotation lastChild = summaryChildren.get(summaryChildren.size() - 1);
    return new BlockAnnotation(
        a.getValue(), summaryChildren,
        FilePosition.span(a.getFilePosition(), lastChild.getFilePosition()));
  }

  private Summarizer() { /* not instantiable */ }
}
