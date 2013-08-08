// Copyright (C) 2009 Google Inc.
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

package com.google.caja.plugin.stages;

import com.google.caja.SomethingWidgyHappenedError;
import com.google.caja.lang.html.HTML;
import com.google.caja.lang.html.HtmlSchema;
import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.CssTokenType;
import com.google.caja.lexer.ExternalReference;
import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.JsLexer;
import com.google.caja.lexer.JsTokenQueue;
import com.google.caja.lexer.TokenQueue;
import com.google.caja.lexer.escaping.UriUtil;
import com.google.caja.parser.css.CssParser;
import com.google.caja.parser.html.AttribKey;
import com.google.caja.parser.html.ElKey;
import com.google.caja.parser.html.Nodes;
import com.google.caja.parser.js.Parser;
import com.google.caja.parser.js.StringLiteral;
import com.google.caja.plugin.PluginMessageType;
import com.google.caja.plugin.UriFetcher;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.util.ContentType;
import com.google.caja.util.Function;
import com.google.caja.util.Lists;
import com.google.caja.util.Strings;

import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

/**
 * Finds embedded styles and scripts in a DOM.
 * For example, finds all attributes, text content, and external content in:
 * <pre>
 * &lt;script src=foo.js&gt;&lt;/script&gt;
 * &lt;script&gt;foo()&lt;/script&gt;
 * &lt;link rel=stylesheet href=bar.css&gt;
 * &lt;style&gt;foo { color: red }&lt;/style&gt;
 * &lt;a href="javascript:clicked()" onmousedown="baz()" style="color:red"&gt;
 * </pre>
 *
 * @author mikesamuel@gmail.com
 */
public class HtmlEmbeddedContentFinder {
  private final HtmlSchema schema;
  private final URI baseUri;
  private final MessageQueue mq;
  private final MessageContext mc;

  public HtmlEmbeddedContentFinder(
      HtmlSchema schema, URI baseUri, MessageQueue mq, MessageContext mc) {
    assert schema != null && mq != null
        && baseUri.isAbsolute() && !baseUri.isOpaque();
    this.schema = schema;
    this.baseUri = baseUri;
    this.mq = mq;
    this.mc = mc;
  }

  public Iterable<EmbeddedContent> findEmbeddedContent(Node node) {
    List<EmbeddedContent> out = Lists.newArrayList();
    findEmbeddedContent(node, out);
    return out;
  }

  public URI getBaseUri() { return baseUri; }

  private static final ElKey LINK = ElKey.forHtmlElement("link");
  private static final ElKey SCRIPT = ElKey.forHtmlElement("script");
  private static final ElKey STYLE = ElKey.forHtmlElement("style");
  private static final AttribKey LINK_HREF = AttribKey.forHtmlAttrib(
      LINK, "href");
  private static final AttribKey LINK_REL = AttribKey.forHtmlAttrib(
      LINK, "rel");
  private static final AttribKey SCRIPT_ASYNC = AttribKey.forHtmlAttrib(
      ElKey.HTML_WILDCARD, "async");
  private static final AttribKey SCRIPT_DEFER = AttribKey.forHtmlAttrib(
      ElKey.HTML_WILDCARD, "defer");
  private static final AttribKey SCRIPT_SRC = AttribKey.forHtmlAttrib(
      SCRIPT, "src");
  private static final AttribKey TYPE = AttribKey.forHtmlAttrib(
      ElKey.HTML_WILDCARD, "type");

  private void findEmbeddedContent(Node node, List<EmbeddedContent> out) {
    if (node instanceof Element) {
      Element el = (Element) node;
      ElKey key = ElKey.forElement(el);
      ContentType expected = null;
      ExternalReference extRef = null;
      String defaultMimeType = null;
      EmbeddedContent.Scheduling scheduling = EmbeddedContent.Scheduling.NORMAL;
      if (SCRIPT.equals(key)) {
        expected = ContentType.JS;
        extRef = externalReferenceFromAttr(el, SCRIPT_SRC);
        if (el.hasAttributeNS(SCRIPT_DEFER.ns.uri, SCRIPT_DEFER.localName)) {
          scheduling = EmbeddedContent.Scheduling.DEFERRED;
        } else if (el.hasAttributeNS(SCRIPT_ASYNC.ns.uri, SCRIPT_ASYNC.localName)) {
          scheduling = EmbeddedContent.Scheduling.ASYNC;
        }
      } else if (STYLE.equals(key)) {
        expected = ContentType.CSS;
      } else if (LINK.equals(key)
                 && Strings.eqIgnoreCase(
                     "stylesheet",
                     el.getAttributeNS(LINK_REL.ns.uri, LINK_REL.localName))) {
        extRef = externalReferenceFromAttr(el, LINK_HREF);
        if (extRef != null) {
          expected = ContentType.CSS;
          defaultMimeType = ContentType.CSS.mimeType;
        }
      }
      if (expected != null) {
        String mimeType = getMimeTypeFromHtmlTypeAttribute(el, key);
        if (mimeType == null) { mimeType = defaultMimeType; }
        ContentType actualType = mimeType != null
            ? ContentType.fromMimeType(mimeType) : null;
        if (actualType == expected) {
          if (extRef == null) {
            out.add(fromElementBody(el, expected, scheduling));
          } else {
            out.add(fromExternalReference(el, expected, extRef, scheduling));
          }
        } else {
          FilePosition typePos = Nodes.getFilePositionFor(el);
          Attr a = el.getAttributeNodeNS(TYPE.ns.uri, TYPE.localName);
          if (a != null) {
            typePos = Nodes.getFilePositionForValue(a);
          }
          mq.addMessage(
              PluginMessageType.UNRECOGNIZED_CONTENT_TYPE,
              typePos, MessagePart.Factory.valueOf(
                  mimeType != null ? mimeType : "(missing mimeType)"),
              key);
          out.add(fromBadContent(el));
        }
      }
      for (Attr a : Nodes.attributesOf(el)) {
        AttribKey aKey = AttribKey.forAttribute(key, a);
        HTML.Attribute aInfo = schema.lookupAttribute(aKey);
        if (aInfo != null) {
          switch (aInfo.getType()) {
            case URI:
              boolean isCode = false;
              try {
                String uriText = UriUtil.normalizeUri(a.getValue());
                URI uri = new URI(uriText);
                if (Strings.eqIgnoreCase("javascript", uri.getScheme())) {
                  isCode = true;
                }
              } catch (URISyntaxException ex) {
                // not code
              }
              if (isCode) {
                out.add(fromAttrib(a, true, ContentType.JS)); break;
              }
              break;
            // This should depend on the Content-Script-Type header:
            //   http://www.w3.org/TR/REC-html40/interact/scripts.html#h-18.2.2
            case SCRIPT:
              out.add(fromAttrib(a, false, ContentType.JS));
              break;
            case STYLE:
              out.add(fromAttrib(a, false, ContentType.CSS));
              break;
            default: break;
          }
        }
      }
    }
    for (Node c = node.getFirstChild(); c != null; c = c.getNextSibling()) {
      findEmbeddedContent(c, out);
    }
  }

  private EmbeddedContent fromExternalReference(
      Element el, final ContentType t, final ExternalReference extRef,
      EmbeddedContent.Scheduling scheduling) {
    return new EmbeddedContent(
        this, extRef.getReferencePosition(),
        new Function<UriFetcher, CharProducer>() {
          boolean loaded;
          CharProducer cp = null;
          public CharProducer apply(UriFetcher fetcher) {
            if (!loaded) {
              URI uri = extRef.getUri();
              ExternalReference toLoad = extRef;
              if (!uri.isAbsolute() && baseUri != null) {
                toLoad = new ExternalReference(
                    baseUri.resolve(uri),
                    extRef.getReferencePosition());
              }
              try {
                cp = fetcher.fetch(toLoad, t.mimeType).getTextualContent();
              } catch (UriFetcher.UriFetchException ex) {
                cp = null;  // Handled below.
              } catch (UnsupportedEncodingException ex) {
                cp = null;  // Handled below.
              }
              mc.addInputSource(new InputSource(toLoad.getUri()));
              loaded = true;
            }
            if (cp == null) {
              URI srcUri = extRef.getUri();
              String errUri = srcUri.isAbsolute()
                  ? mc.abbreviate(new InputSource(srcUri)) : srcUri.toString();
              mq.addMessage(
                  PluginMessageType.FAILED_TO_LOAD_EXTERNAL_URL,
                  extRef.getReferencePosition(),
                  MessagePart.Factory.valueOf(errUri));
              switch (t) {
                case JS:
                  // Throw an exception so any user installed error handler
                  // will fire.
                  cp = CharProducer.Factory.fromString(
                      "throw new Error("
                      + StringLiteral.toQuotedValue("Failed to load " + errUri)
                      + ");",
                      extRef.getReferencePosition());
                  break;
                case CSS:
                  // Record the fact that the content failed to load.
                  cp = CharProducer.Factory.fromString(
                      "/* Failed to load "
                      + errUri.replaceAll("\\*/", "* /") + " */",
                      extRef.getReferencePosition());
                  break;
                default:
                  throw new SomethingWidgyHappenedError(t.toString());
              }
            }
            return cp.clone();
          }
        },
        extRef, scheduling, el, t);
  }

  private EmbeddedContent fromElementBody(
      Element el, ContentType t, EmbeddedContent.Scheduling scheduling) {
    final CharProducer cp = textNodesToCharProducer(el, t == ContentType.JS);
    return new EmbeddedContent(
        this, cp.filePositionForOffsets(0, cp.getLimit()),
        new Function<UriFetcher, CharProducer>() {
          public CharProducer apply(UriFetcher fetcher) { return cp.clone(); }
        },
        null, scheduling, el, t);
  }

  private EmbeddedContent fromAttrib(final Attr a, final boolean uriDecode,
      ContentType t) {
    final String rawValue = Nodes.getRawValue(a);
    final String value = a.getValue();
    return new EmbeddedContent(
        this, Nodes.getFilePositionForValue(a),
        new Function<UriFetcher, CharProducer>() {
          CharProducer cp;
          public CharProducer apply(UriFetcher fetcher) {
            if (this.cp == null) {
              CharProducer cp;
              String rawText = rawValue;
              if (rawText != null) {
                int n = rawText.length();
                if (n >= 2) {  // Strip quotes
                  char lastCh = rawText.charAt(n - 1);
                  if (lastCh == '"' || lastCh == '\'') {
                    if (rawText.charAt(0) == lastCh) {
                      rawText = " " + rawText.substring(1, n - 1) + " ";
                    } else {
                      rawText = rawText.substring(0, n - 1) + " ";
                    }
                  }
                }
              }
              if (rawText != null
                  && Nodes.decode(rawText).trim().equals(value.trim())) {
                if (uriDecode) { rawText = blankOutScheme(rawText); }
                cp = CharProducer.Factory.fromHtmlAttribute(
                    CharProducer.Factory.fromString(
                        rawText, Nodes.getFilePositionForValue(a)));
              } else {
                String decodedText = value;
                if (uriDecode) { decodedText = blankOutScheme(decodedText); }
                cp = CharProducer.Factory.fromString(
                    decodedText, Nodes.getFilePositionForValue(a));
              }
              if (uriDecode) {
                cp = CharProducer.Factory.fromUri(cp);
              }
              this.cp = cp;
            }
            return this.cp.clone();
          }
        },
        null, EmbeddedContent.Scheduling.NORMAL, a, t);
  }

  private EmbeddedContent fromBadContent(Element el) {
    final FilePosition pos = FilePosition.startOf(Nodes.getFilePositionFor(el));
    return new EmbeddedContent(
        this, pos,
        new Function<UriFetcher, CharProducer>() {
          public CharProducer apply(UriFetcher fetcher) {
            return CharProducer.Factory.fromString("", pos);
          }
        },
        null, EmbeddedContent.Scheduling.NORMAL, el, null);
  }

  private ExternalReference externalReferenceFromAttr(Element el, AttribKey a) {
    Attr attr = el.getAttributeNodeNS(a.ns.uri, a.localName);
    if (attr == null || "".equals(attr.getValue())) { return null; }
    URI uri;
    try {
      uri = new URI(attr.getNodeValue());
    } catch (URISyntaxException ex) {
      mq.getMessages().add(
          new Message(PluginMessageType.MALFORMED_URL, MessageLevel.ERROR,
                      Nodes.getFilePositionFor(attr), a));
      return null;
    }
    return new ExternalReference(uri, Nodes.getFilePositionForValue(attr));
  }

  /**
   * A CharProducer that produces characters from the concatenation of all
   * the text nodes in the given node list.
   */
  private static CharProducer textNodesToCharProducer(
      Element el, boolean stripComments) {
    List<Text> textNodes = Lists.newArrayList();
    for (Node node : Nodes.childrenOf(el)) {
      if (node instanceof Text) { textNodes.add((Text) node); }
    }
    if (textNodes.isEmpty()) {
      return CharProducer.Factory.create(
          new StringReader(""),
          FilePosition.endOf(Nodes.getFilePositionFor(el)));
    }
    List<CharProducer> content = Lists.newArrayList();
    for (int i = 0, n = textNodes.size(); i < n; ++i) {
      Text node = textNodes.get(i);
      String text = node.getNodeValue();
      if (stripComments) {
        if (i == 0) {
          text = text.replaceFirst("^(\\s*)<!--", "$1     ");
        }
        if (i + 1 == n) {
          text = text.replaceFirst("-->(\\s*)$", "   $1");
        }
      }
      content.add(CharProducer.Factory.create(
          new StringReader(text),
          FilePosition.startOf(Nodes.getFilePositionFor(node))));
    }
    if (content.size() == 1) {
      return content.get(0);
    } else {
      return CharProducer.Factory.chain(content.toArray(new CharProducer[0]));
    }
  }


  private String getMimeTypeFromHtmlTypeAttribute(Element el, ElKey elKey) {
    Attr type = el.getAttributeNodeNS(TYPE.ns.uri, TYPE.localName);
    if (type != null) { return type.getValue(); }
    HTML.Attribute attr = schema.lookupAttribute(TYPE.onElement(elKey));
    if (attr == null) { return null; }
    return attr.getDefaultValue();
  }

  /** May be overridden to affect JS {@link EmbeddedContent#parse parsing}. */
  @SuppressWarnings("static-method")
  protected boolean shouldAllowJsQuasis() { return false; }
  /** May be overridden to affect JS {@link EmbeddedContent#parse parsing}. */
  @SuppressWarnings("static-method")
  protected boolean shouldJsRecover() { return false; }
  /** May be overridden to affect CSS {@link EmbeddedContent#parse parsing}. */
  @SuppressWarnings("static-method")
  protected boolean shouldAllowCssSubsts() { return false; }
  /** May be overridden to affect CSS {@link EmbeddedContent#parse parsing}. */
  @SuppressWarnings("static-method")
  protected MessageLevel getCssTolerance() { return MessageLevel.WARNING; }

  Parser makeJsParser(CharProducer cp, MessageQueue mq) {
    boolean quasis = shouldAllowJsQuasis();
    FilePosition p = cp.filePositionForOffsets(cp.getOffset(), cp.getLimit());
    JsLexer lexer = new JsLexer(cp, quasis);
    JsTokenQueue tq = new JsTokenQueue(lexer, p.source());
    tq.setInputRange(p);
    Parser parser = new Parser(tq, mq, quasis);
    parser.setRecoverFromFailure(shouldJsRecover());
    return parser;
  }

  CssParser makeCssParser(CharProducer cp, MessageQueue mq) {
    boolean allowSubsts = shouldAllowCssSubsts();
    FilePosition p = cp.filePositionForOffsets(cp.getOffset(), cp.getLimit());
    TokenQueue<CssTokenType> tq = CssParser.makeTokenQueue(cp, mq, allowSubsts);
    tq.setInputRange(p);
    return new CssParser(tq, mq, getCssTolerance());
  }

  private static String blankOutScheme(String s) {
    int colon = s.indexOf(':');
    StringBuilder sb = new StringBuilder(s);
    for (int i = colon + 1; --i >= 0;) {
      char ch = sb.charAt(i);
      if (!Character.isWhitespace(ch)) { sb.setCharAt(i, ' '); }
    }
    return sb.toString();
  }
}
