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

package com.google.caja.ancillary.linter;

import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.InputSource;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.MessageTypeInt;
import com.google.caja.reporting.SnippetProducer;
import com.google.caja.util.Lists;
import com.google.common.collect.Maps;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * @author mikesamuel@gmail.com
 */
final class ErrorReporter {
  /** Dumps error messages to the output stream. */
  static MessageLevel reportErrors(
      Map<InputSource, CharSequence> inputs,
      MessageContext mc, MessageQueue mq, Appendable out)
      throws IOException {
    MessageLevel max = MessageLevel.values()[0];
    SnippetProducer sp = new SnippetProducer(inputs, mc);
    // HACK: do not commit
    Map<MessageTypeInt, Integer> counts = Maps.newHashMap();
    for (Message msg : sortMessages(mq.getMessages())) {
      counts.put(
          msg.getMessageType(),
          Integer.valueOf(counts.containsKey(msg.getMessageType())
                          ? counts.get(msg.getMessageType()) + 1
                          : 1));
      MessageLevel level = msg.getMessageLevel();
      if (level.compareTo(max) > 0) { max = level; }
      String snippet = sp.getSnippet(msg);
      out.append(
          level.name() + " : " + msg.format(mc)
          + ("".equals(snippet) ? "" : "\n" + snippet) + "\n");
    }

    List<Map.Entry<MessageTypeInt, Integer>> countsInOrder
        = Lists.newArrayList(counts.entrySet());
    Collections.sort(
        countsInOrder, new Comparator<Map.Entry<MessageTypeInt, Integer>>() {
      public int compare(
          Map.Entry<MessageTypeInt, Integer> a,
          Map.Entry<MessageTypeInt, Integer> b) {
        int delta = a.getValue() - b.getValue();
        if (delta != 0) { return delta; }
        return a.getKey().name().compareTo(b.getKey().name());
      }
    });
    for (Map.Entry<MessageTypeInt, Integer> e : countsInOrder) {
      out.append(String.format("%3d %s\n", e.getValue(), e.getKey().name()));
    }
    return max;
  }

  static List<Message> sortMessages(List<Message> messages) {
    List<Message> sorted = Lists.newArrayList(messages);
    Collections.sort(sorted, new Comparator<Message>() {
      public int compare(Message a, Message b) {
        MessagePart a0 = firstPartOf(a);
        MessagePart b0 = firstPartOf(b);
        InputSource aSrc = toInputSource(a0), bSrc = toInputSource(b0);
        // Compure by source first.
        if (aSrc != null && bSrc != null) {
          int delta = aSrc.getUri().compareTo(bSrc.getUri());
          if (delta != 0) { return delta; }
        }
        // Sort positionless parts after ones with a position.
        long aSPos = Integer.MAX_VALUE + 1L, aEPos = Integer.MAX_VALUE + 1L;
        long bSPos = Integer.MAX_VALUE + 1L, bEPos = Integer.MAX_VALUE + 1L;
        if (a0 instanceof FilePosition) {
          FilePosition pos = (FilePosition) a0;
          aSPos = pos.startCharInFile();
          aEPos = pos.endCharInFile();
        } else if (a0 instanceof InputSource) {
          // sort file level messages before messages within file
          aSPos = aEPos = -1;
        }
        if (b0 instanceof FilePosition) {
          FilePosition pos = (FilePosition) b0;
          bSPos = pos.startCharInFile();
          bEPos = pos.endCharInFile();
        } else if (b0 instanceof InputSource) {
          // sort file level messages before messages within file
          bSPos = bEPos = -1;
        }
        int delta = Long.signum(aSPos - bSPos);
        if (delta != 0) { return delta; }
        delta = Long.signum(aEPos - bEPos);
        if (delta != 0) { return delta; }

        StringBuilder aBuf = new StringBuilder(), bBuf = new StringBuilder();
        MessageContext mc = new MessageContext();
        try {
          a0.format(mc, aBuf);
          b0.format(mc, bBuf);
        } catch (IOException ex) {
          throw new RuntimeException(ex);
        }
        return aBuf.toString().compareTo(bBuf.toString());
      }
      private InputSource toInputSource(MessagePart p) {
        if (p instanceof FilePosition) { return ((FilePosition) p).source(); }
        if (p instanceof InputSource) { return (InputSource) p; }
        return null;
      }
    });
    return sorted;
  }

  private static MessagePart firstPartOf(Message m) {
    List<MessagePart> parts = m.getMessageParts();
    return parts.isEmpty() ? null : parts.get(0);
  }

  private ErrorReporter() { /* uninstantiable */ }
}
