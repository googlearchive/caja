// Copyright (C) 2010 Google Inc.
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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Map;

import com.google.caja.SomethingWidgyHappenedError;
import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.Chardet;
import com.google.caja.lexer.ExternalReference;
import com.google.caja.lexer.FetchedData;
import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.GuessContentType;
import com.google.caja.lexer.HtmlLexer;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.JsLexer;
import com.google.caja.lexer.JsTokenQueue;
import com.google.caja.lexer.ParseException;
import com.google.caja.parser.html.Dom;
import com.google.caja.parser.html.DomParser;
import com.google.caja.parser.js.Parser;
import com.google.caja.plugin.PluginMeta;
import com.google.caja.plugin.UriFetcher.UriFetchException;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.util.ContentType;
import com.google.caja.util.Pair;

/**
 * Parse tree nodes builder
 *
 * @author Jasvir Nagra jasvir@gmail.com
 */
public final class ParserContext {

  // Inputs all parsers must have to build a parse tree
  private MessageQueue mq;
  private InputSource is;
  private CharProducer cp;
  private ContentType type;

  // Inputs that might be needed to configure a parser
  private Charset charset;
  private InputStream inputStream;
  private String content;
  private PluginMeta meta;

  // Inputs that help make a parser more useful
  private Map<InputSource, CharSequence> sourceMap;
  private MessageContext mc;

  // Optional settings
  private boolean comments;

  public ParserContext(MessageQueue mq) {
    this(mq, InputSource.UNKNOWN, null, null, null, null, null, null, null,
        null, false);
  }

  private ParserContext(MessageQueue mq, InputSource is, CharProducer cp,
      ContentType type, Charset charset, InputStream inputStream,
      String content, PluginMeta meta,
      Map<InputSource, CharSequence> sourceMap, MessageContext mc,
      boolean comments) {
    this.mq = mq;
    this.is = is;
    this.cp = cp;
    this.type = type;
    this.charset = charset;
    this.inputStream = inputStream;
    this.content = content;
    this.meta = meta;
    this.sourceMap = sourceMap;
    this.mc = mc;
    this.comments = comments;
  }

  public ParserContext withInput(ContentType type) {
    return this.type != type ?
        new ParserContext(mq, is, cp, type, charset, inputStream, content,
            meta, sourceMap, mc, comments) : this;
  }

  public ParserContext withInput(InputSource is) {
    return this.is != is ?
        new ParserContext(mq, is, cp, type, charset, inputStream, content,
            meta, sourceMap, mc, comments) : this;
  }

  public ParserContext withInput(CharProducer cp) {
    return this.cp != cp ?
        new ParserContext(mq, is, cp, type, charset, inputStream, content,
            meta, sourceMap, mc, comments) : this;
  }

  public ParserContext withConfig(MessageContext mc) {
    return this.mc != mc ?
        new ParserContext(mq, is, cp, type, charset, inputStream, content,
            meta, sourceMap, mc, comments) : this;
  }

  public ParserContext withInput(String content) {
    return this.content != content ?
        new ParserContext(mq, is, cp, type, charset, inputStream, content,
            meta, sourceMap, mc, comments) : this;
  }

  public ParserContext withInput(File file) throws IOException {
    return new ParserContext(mq, is, cp, type, charset,
        new FileInputStream(file), content, meta, sourceMap, mc, comments);
  }

  public ParserContext withInput(InputStream inputStream) {
    return this.inputStream != inputStream ?
        new ParserContext(mq, is, cp, type, charset, inputStream, content,
            meta, sourceMap, mc, comments) : this;
  }

  public ParserContext withInput(Charset charset) {
    return this.charset != charset ?
        new ParserContext(mq, is, cp, type, charset, inputStream, content,
            meta, sourceMap, mc, comments) : this;
  }

  public ParserContext withInput(InputStream inputStream, Charset charset) {
    return this.inputStream != inputStream && this.charset != charset ?
        new ParserContext(mq, is, cp, type, charset, inputStream, content,
            meta, sourceMap, mc, comments) : this;
  }

  public ParserContext withConfig(PluginMeta meta) {
    return this.meta != meta ?
        new ParserContext(mq, is, cp, type, charset, inputStream, content,
            meta, sourceMap, mc, comments) : this;
  }

  public ParserContext withSourceMap(Map<InputSource, CharSequence> sourceMap) {
    return this.sourceMap != sourceMap ?
        new ParserContext(mq, is, cp, type, charset, inputStream, content,
            meta, sourceMap, mc, comments) : this;
  }

  private static InputSource guessInputSource(InputSource is) {
    if (null == is) {
      return InputSource.UNKNOWN;
    }
    return is;
  }

  /**
   * Fetches content from an input source
   * @param is input source from which to fetch content
   * @param sourceMap store content in a source map
   * @return contents from file input source
   */
  private static CharProducer guessCharProducer(CharProducer cp, String content,
      Map<InputSource, CharSequence> sourceMap, PluginMeta meta, InputSource is,
      InputStream stream, Charset charset)
      throws IOException {
    if (null != cp) {
      return cp;
    }

    if (null != content) {
      return CharProducer.Factory.fromString(content, is);
    }

    if (null != sourceMap && sourceMap.containsKey(is)) {
      return CharProducer.Factory.fromString(sourceMap.get(is), is);
    }

    if (null == stream && null != is) {
      CharProducer candidate = guessCharProducer(is, meta);
      if (null != candidate) {
        return candidate;
      }
    }

    if (null != stream) {
      Reader reader;
      if (null != charset) {
        reader = new InputStreamReader(stream, charset);
      } else {
        Pair<Reader, String> guess = Chardet.guessCharset(stream);
        reader = guess.a;
      }
      return CharProducer.Factory.create(reader, is);
    }
    throw new IllegalStateException(
        "Not enough arguments to create a CharProducer");
  }

  private static CharProducer guessCharProducer(
      InputSource is, PluginMeta meta) {
    try {
      if (null == meta) {
        return null;
      }
      FetchedData data = meta.getUriFetcher().fetch(
          new ExternalReference(is.getUri(), FilePosition.UNKNOWN), "*/*");
      return data.getTextualContent();
    } catch (UriFetchException e) {
      e.printStackTrace();
      // Failed heuristic, return null
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
      // Failed heuristic, return null
    }
    return null;
  }

  private static void cacheInMessageContext(MessageContext mc, InputSource is) {
    if (null != mc) {
      mc.addInputSource(is);
    }
  }

  private static void cacheInSourceMap(Map<InputSource, CharSequence> sourceMap,
      InputSource is, CharProducer cp, String content) {
    if (null != sourceMap) {
      if (null == content) {
        content = cp.toString(cp.getOffset(), cp.getLength());
      }

      sourceMap.put(is, content);
    }
  }

  /**
   * Guess and a non-null content type
   * @param is non-null input source
   * @param cp non-null character producer
   */
  private ContentType guessContentType(
      CharProducer cp, InputSource is, ContentType type) {
    assert null != is;
    assert null != cp;

    if (null != type) {
      return type;
    }

    String path = is.getUri().getPath();
    return GuessContentType.guess(null /* mimeType */, path, content);
  }

  private ParseTreeNode parse() throws ParseException {
    ParseTreeNode input;
    if (ContentType.JS == type) {
      JsLexer lexer = new JsLexer(cp);
      JsTokenQueue tq = new JsTokenQueue(lexer, is);
      if (tq.isEmpty()) { return null; }
      Parser p = new Parser(tq, mq);
      input = p.parse();
      tq.expectEmpty();
    } else if (ContentType.HTML == type) {
      DomParser p = new DomParser(new HtmlLexer(cp), false, is, mq);
      //if (p.getTokenQueue().isEmpty()) { return null; }
      input = Dom.transplant(p.parseDocument());
      p.getTokenQueue().expectEmpty();
    } else {
      throw new SomethingWidgyHappenedError("Can't classify input " + is);
    }
    return input;

  }

  /**
   * Builds and returns a parse tree node
   */
  public ParseTreeNode build()
    throws ParseException, IllegalStateException, IOException {
    // Determine the input name
    is = guessInputSource(is);

    // Determine the input
    cp = guessCharProducer(
        cp, content, sourceMap, meta, is, inputStream, charset);

    // Cache content
    cacheInSourceMap(sourceMap, is, cp, content);
    cacheInMessageContext(mc, is);

    // Determine the content type
    type = guessContentType(cp, is, type);

    // Parse!
    ParseTreeNode node = parse();

    return node;
  }
}
