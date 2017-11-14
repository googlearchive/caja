// Copyright (C) 2013 Google Inc.
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

package com.google.caja.tracing;

import com.google.caja.lexer.ExternalReference;
import com.google.caja.lexer.FetchedData;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.ParseException;
import com.google.caja.lexer.TokenConsumer;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.ParserContext;
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.UncajoledModule;
import com.google.caja.plugin.PluginCompilerMain;
import com.google.caja.plugin.PluginMeta;
import com.google.caja.plugin.UriFetcher;
import com.google.caja.plugin.UriPolicy;
import com.google.caja.tracing.TracingRewriter;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.MessageType;
import com.google.caja.reporting.RenderContext;
import com.google.caja.reporting.SimpleMessageQueue;
import com.google.caja.util.Callback;
import com.google.caja.util.ContentType;

import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * @author ihab.awad@gmail.com
 */
public class TracingRewriterMain {

  public static void main(String[] argv) {
    MessageQueue mq = new SimpleMessageQueue();
    try {
      ParseTreeNode input = new ParserContext(mq)
          .withInput(new InputSource(new URI(argv[0])))
          .withInput(ContentType.JS)
          .withConfig(
              new PluginMeta(
                  new UriFetcher() {
                    public FetchedData fetch(
                        ExternalReference ref, String mimeType)
                        throws UriFetchException {
                      String uri = ref.getUri().toString();
                      try {
                        return FetchedData.fromConnection(
                            new URL(uri).openConnection());
                      } catch (IOException ex) {
                        throw new UriFetchException(ref, mimeType, ex);
                      }
                    }
                  },
                  UriPolicy.IDENTITY))
          .build();
      UncajoledModule inputModule = new UncajoledModule((Block) input);
      if (mq.hasMessageAtLevel(MessageLevel.WARNING)) {
        dumpMessages(mq);
        return;
      }
      ParseTreeNode output = new TracingRewriter(mq).expand(inputModule);
      if (mq.hasMessageAtLevel(MessageLevel.WARNING)) {
        dumpMessages(mq);
        return;
      }
      renderOutput(output, argv.length > 1 ? argv[1] : null, mq);
      if (mq.hasMessageAtLevel(MessageLevel.WARNING)) {
        dumpMessages(mq);
        return;
      }
    } catch (URISyntaxException e) {
      mq.addMessage(
          new Message(
              MessageType.IO_ERROR,
              MessagePart.Factory.valueOf(e.getMessage())));
    } catch (ParseException e) {
      mq.addMessage(
          new Message(
              MessageType.PARSE_ERROR,
              MessagePart.Factory.valueOf(e.getMessage())));
    } catch (IOException e) {
      mq.addMessage(
          new Message(
              MessageType.IO_ERROR,
              MessagePart.Factory.valueOf(e.getMessage())));
    }
  }

  private static void renderOutput(
      ParseTreeNode output,
      String outputFile,
      final MessageQueue mq) {
    try {
      TokenConsumer renderer = output.makeRenderer(
          (outputFile == null)
              ? new OutputStreamWriter(System.out)
              : new FileWriter(outputFile),
          new Callback<IOException>() {
            public void handle(IOException ex) {
              mq.addMessage(
                  MessageType.IO_ERROR,
                  MessagePart.Factory.valueOf(ex.toString()));
            }
          });
      output.render(new RenderContext(renderer));
      renderer.noMoreTokens();
    } catch (IOException e) {
      mq.addMessage(
          new Message(
              MessageType.IO_ERROR,
              MessagePart.Factory.valueOf(e.getMessage())));
    }
  }

  private static void dumpMessages(MessageQueue mq) {
    PluginCompilerMain.dumpMessages(mq, new MessageContext(), System.err);
  }
}
