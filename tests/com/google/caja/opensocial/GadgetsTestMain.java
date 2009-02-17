// Copyright 2007 Google Inc. All Rights Reserved.
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

package com.google.caja.opensocial;

import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.ExternalReference;
import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.InputSource;
import com.google.caja.plugin.Config;
import com.google.caja.reporting.BuildInfo;
import com.google.caja.reporting.HtmlSnippetProducer;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.MessageType;
import com.google.caja.reporting.MessageTypeInt;
import com.google.caja.reporting.SimpleMessageQueue;
import com.google.caja.reporting.SnippetProducer;
import com.google.caja.util.Json;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author jasvir@google.com (Jasvir Nagra)
 */
public class GadgetsTestMain {

  private MessageContext mc = new MessageContext();
  private Map<InputSource, CharSequence> originalSources
      = new HashMap<InputSource, CharSequence>();
  private ArrayList<URI> gadgetList;
  private JSONObject resultDoc;
  private BufferedWriter jsonOutput;

  private GadgetsTestMain() {
    resultDoc = new JSONObject();
  }

  public static void main(String[] argv) throws UriCallbackException {
    System.exit(new GadgetsTestMain().run(argv));
  }

  public boolean processArguments(String[] argv) {
    gadgetList = new ArrayList<URI>();
    if (argv.length == 0) {
      usage("GadgetsTestMain urls-of-gadgets.txt [outputfile]");
      return false;
    }
    try {
      String uri;
      BufferedReader ur = new BufferedReader(new FileReader(argv[0]));

      // Skip blank lines or comments
      while (null != (uri = ur.readLine())) {
        if (uri.matches("^[ \t]*$"))
          continue;
        if (uri.matches("^[ \t]*#"))
          continue;

        URI inputUri;
        try {
          if (uri.indexOf(':') >= 0) {
            inputUri = new URI(uri);
          } else {
            File inputFile = new File(uri);

            if (!inputFile.exists()) {
              System.err.println("WARNING: File \"" + uri + "\" does not exist");
              return false;
            }
            if (!inputFile.isFile()) {
              usage("File \"" + uri + "\" is not a regular file");
              return false;
            }

            inputUri = inputFile.getAbsoluteFile().toURI();
          }
          gadgetList.add(inputUri);
        } catch (URISyntaxException e) {
          System.err.println("WARNING: URI \"" + uri + "\" malformed");
        }
      }
    } catch (FileNotFoundException e) {
      usage("ERROR: Could not find file of urls:" + e.toString());
      return false;
    } catch (IOException e) {
      usage("ERROR: Could not read urls:" + e.toString());
      return false;
    }

    try {
      if (argv.length > 1) {
        jsonOutput = new BufferedWriter(new FileWriter(new File(argv[1])));
      } else {
        jsonOutput = new BufferedWriter(new OutputStreamWriter(System.out));
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return true;
  }

  private void writeResults(Writer w) {
    try {
      String json = resultDoc.toString();
      w.write(json);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private String getExceptionTrace(Exception e) {
    StringBuffer result = new StringBuffer();
    for (StackTraceElement st : e.getStackTrace()) {
      result.append(st.toString());
      result.append("\n");
    }
    return result.toString();
  }

  private void testGadget(URI gadget, JSONArray testResults,
                          Map<MessageTypeInt, Integer> errorCount)
      throws IOException, UriCallbackException {

    String[] argv = {
        "-o", "/tmp/xx",
        "--css_prop_schema",
        "resource:///com/google/caja/lang/css/css-extensions.json",
        "-i", gadget.toASCIIString()
    };

    GadgetRewriterMain grm = new GadgetRewriterMain();
    grm.init(argv);

    Config config = grm.getConfig();

    MessageQueue mq = new SimpleMessageQueue();
    DefaultGadgetRewriter rewriter = new DefaultGadgetRewriter(mq);
    rewriter.setCssSchema(config.getCssSchema(mq));
    rewriter.setHtmlSchema(config.getHtmlSchema(mq));

    JSONArray messages = new JSONArray();
    JSONObject gadgetElement
        = Json.formatAsJson("url", gadget.toString(), "title", "TODO", "messages", messages);
    Json.pushJson(testResults, gadgetElement);

    Writer w = new BufferedWriter(new FileWriter(config.getOutputBase()));

    MessageLevel worstErrorLevel = MessageLevel.LOG;
    MessageTypeInt worstErrorType = null;
    try {
      Callback cb = new Callback(config, mc, originalSources);
      URI baseUri = config.getBaseUri();
      for (URI input : config.getInputUris()) {
        System.err.println(input);
        Reader r = cb.retrieve(
            new ExternalReference(input, FilePosition.UNKNOWN), null);
        CharProducer cp = CharProducer.Factory.create(
            r, new InputSource(input));
        try {
          rewriter.rewrite(baseUri, cp, cb, "canvas", w);
        } catch (Exception e) {
          addMessageNode(messages,"Compiler threw uncaught exception: " + e,
              MessageLevel.FATAL_ERROR.toString(),
              MessageType.INTERNAL_ERROR.toString(),
              getExceptionTrace(e));
          worstErrorType = MessageType.INTERNAL_ERROR;
          worstErrorLevel = MessageLevel.FATAL_ERROR;

          int count = errorCount.containsKey(MessageType.INTERNAL_ERROR)
              ? errorCount.get(MessageType.INTERNAL_ERROR) : 0;
          errorCount.put(MessageType.INTERNAL_ERROR, count + 1);
        } finally {
          SnippetProducer sp = new HtmlSnippetProducer(originalSources, mc);
          for (Message msg : mq.getMessages()) {
            MessageTypeInt type = msg.getMessageType();
            if (type == MessageType.SEMICOLON_INSERTED) { continue; }
            addMessageNode(messages, msg, mc, sp);

            int count = errorCount.containsKey(type)
              ? errorCount.get(type) : 0;
            errorCount.put(type, count + 1);
            if (msg.getMessageLevel().compareTo(worstErrorLevel) > 0) {
              worstErrorType = msg.getMessageType();
              worstErrorLevel = msg.getMessageLevel();
            }
          }
          r.close();
        }
      }
    } catch (RuntimeException e) {
      addMessageNode(messages,"Compiler threw uncaught runtime exception: " + e,
          MessageLevel.FATAL_ERROR.toString(),
          MessageType.INTERNAL_ERROR.toString(), getExceptionTrace(e));
      worstErrorType = MessageType.INTERNAL_ERROR;
      worstErrorLevel = MessageLevel.FATAL_ERROR;
    } finally {
      addWorstErrorNode(gadgetElement, worstErrorLevel, worstErrorType);
      w.close();
    }
  }


  private void addSummaryResults(
      JSONArray summary, Map<MessageTypeInt, Integer> errorCount) {
    List<Map.Entry<MessageTypeInt, Integer>> entries
        = new ArrayList<Map.Entry<MessageTypeInt, Integer>>(
            errorCount.entrySet());
    Collections.sort(
        entries, new Comparator<Map.Entry<MessageTypeInt, Integer>>() {
      public int compare(Map.Entry<MessageTypeInt, Integer> a,
                         Map.Entry<MessageTypeInt, Integer> b) {
        return b.getValue() - a.getValue();
      }
    });
    for (Map.Entry<MessageTypeInt, Integer> e : entries) {
      Json.pushJson(
          summary,
          Json.formatAsJson("type", e.getKey(),
               "value", e.getValue(),
               "errorLevel", e.getKey().getLevel()));
    }
  }

  private int run(String[] argv) throws UriCallbackException {
    if (!processArguments(argv)) {
      return -1;
    }

    String timestamp = (new Date()).toString();
    System.out.println(timestamp);

    Map<MessageTypeInt, Integer> errorCount
        = new LinkedHashMap<MessageTypeInt, Integer>();

    JSONArray testResults = new JSONArray();
    JSONArray summary = new JSONArray();
    Json.putJson(
        resultDoc,
        "buildInfo", JSONObject.escape(BuildInfo.getInstance().getBuildInfo()),
        "timestamp", JSONObject.escape(timestamp),
        "gadgets", testResults,
        "summary", summary);

    try {
      for (URI gadgetUri : gadgetList) {
        try {
          testGadget(gadgetUri, testResults, errorCount);
        } catch (RuntimeException e) {
          e.printStackTrace();
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      addSummaryResults(summary,errorCount);
      writeResults(jsonOutput);
      try {
        jsonOutput.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return 0;
  }

  private void addMessageNode(JSONArray messages, String position,
                              String level, String type, String text) {
    Json.pushJson(messages,
        Json.formatAsJson("position", position, "level", level, "type", type, "text", text));
  }

  private void addWorstErrorNode(JSONObject gadget, MessageLevel mLevel,
                                 MessageTypeInt mType) {
    String levelOrdinal = mLevel == null ? "UNKNOWN" : "" + mLevel.ordinal();
    String level = mLevel == null ? "UNKNOWN" : mLevel.toString();
    String type = mType == null ? "UNKNOWN" : mType.toString();

    Json.putJson(gadget,
        "worstError",
        Json.formatAsJson("type", type, "level", level, "levelOrdinal", levelOrdinal));
  }

  private void addMessageNode(
      JSONArray messages, Message msg, MessageContext mc, SnippetProducer sp) {

    MessageLevel messageLevel = msg.getMessageLevel();
    MessagePart topMessage = msg.getMessageParts().get(0);
    StringBuffer position = new StringBuffer();
    String snippet = null;
    String type = msg.getMessageType().toString();

    if (topMessage instanceof FilePosition) {
      FilePosition filePosition = (FilePosition) topMessage;
      try {
        filePosition.format(mc, position);
      } catch (IOException e) {
        e.printStackTrace();
      }
    } else {
      position = new StringBuffer("Unknown");
    }

    snippet = sp.getSnippet(msg);

    addMessageNode(
        messages, msg.format(mc), messageLevel.name(), type, snippet);
  }

  public void usage(String msg) {
    System.err.println(BuildInfo.getInstance().getBuildInfo());
      System.err.println();
    if (msg != null && !"".equals(msg)) {
      System.err.println(msg);
      System.err.println();
    }
    System.err.println("usage: GadgetsTestMain listofurls.txt output.json");
  }
}
