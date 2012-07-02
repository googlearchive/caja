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

package com.google.caja.parser.html;

import com.google.caja.SomethingWidgyHappenedError;
import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.HtmlLexer;
import com.google.caja.lexer.HtmlTokenType;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.ParseException;
import com.google.caja.lexer.Token;
import com.google.caja.lexer.TokenQueue;
import com.google.caja.lexer.TokenQueue.Mark;
import com.google.caja.lexer.TokenStream;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.MessageType;
import com.google.caja.util.Criterion;
import com.google.caja.util.Function;
import com.google.caja.util.Lists;
import com.google.caja.util.Strings;

import java.io.IOException;
import java.io.Reader;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.xerces.dom.CoreDocumentImpl;
import org.w3c.dom.Attr;
import org.w3c.dom.DOMException;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * Parses a {@link Node} from a stream of XML or HTML tokens.
 * This is a non-validating parser, that will parse tolerantly when created
 * in HTML mode, or will require balanced tags when created in XML mode.
 * <p>
 * Since it's not validating, we don't bother to parse DTDs, and so does not
 * process external entities.  Parsing will not cause URI resolution or
 * fetching.
 *
 * @author mikesamuel@gmail.com
 */
public class DomParser {
  private final TokenQueue<HtmlTokenType> tokens;
  private final boolean asXml;
  private final MessageQueue mq;
  private final Namespaces ns;
  private boolean needsDebugData = true;
  private DOMImplementation domImpl = null;

  public DomParser(
      TokenQueue<HtmlTokenType> tokens, boolean asXml, MessageQueue mq) {
    this(tokens, asXml, Namespaces.HTML_DEFAULT, mq);
  }

  public DomParser(TokenQueue<HtmlTokenType> tokens, boolean asXml,
                   Namespaces ns, MessageQueue mq) {
    this.tokens = tokens;
    this.asXml = asXml;
    this.ns = ns;
    this.mq = mq;
  }

  /**
   * Guesses the markup type -- HTML vs XML -- by looking at the first token.
   */
  public DomParser(
      HtmlLexer lexer, boolean wantsComments, InputSource src, MessageQueue mq)
      throws ParseException {
    this(lexer, wantsComments, src, Namespaces.HTML_DEFAULT, mq);
  }

  public DomParser(
      HtmlLexer lexer, boolean wantsComments, InputSource src, Namespaces ns,
      MessageQueue mq)
      throws ParseException {
    this.mq = mq;
    this.ns = ns;
    LookaheadLexer la = new LookaheadLexer(lexer);
    lexer.setTreatedAsXml(
        this.asXml = (ns.forPrefix("").uri != Namespaces.HTML_NAMESPACE_URI
                      || guessAsXml(la, src)));
    this.tokens = new TokenQueue<HtmlTokenType>(
        la, src, wantsComments
            ? Criterion.Factory.<Token<HtmlTokenType>>optimist()
            : SKIP_COMMENTS);
  }

  public TokenQueue<HtmlTokenType> getTokenQueue() { return tokens; }

  public boolean asXml() { return asXml; }
  public boolean getNeedsDebugData() { return needsDebugData; }
  public boolean wantsComments() {
    return tokens.getTokenFilter().accept(Token.instance(
        "<!-- -->", HtmlTokenType.COMMENT, FilePosition.UNKNOWN));
  }

  /**
   * Sets a flag which determines whether subsequent parse calls will attach
   * file positions and raw text to DOM nodes.
   * {@link org.w3c.dom.Node#setUserData} is very slow in some implementations,
   * so if a client does not need File Position or raw text information, then
   * it can call this with {@code false} as the argument to improve parsing
   * performance.
   */
  public void setNeedsDebugData(boolean needsDebugData) {
    this.needsDebugData = needsDebugData;
  }

  private OpenElementStack makeElementStack(Document doc, MessageQueue mq) {
    Namespaces ns = this.ns;
    DocumentType doctype = doc.getDoctype();
    if (doctype != null) {
      // If we have a DOCTYPE, use its SYSTEM ID to determine the default
      // namespace.
      String sysid = doctype.getSystemId();
      String nsUri = DoctypeMaker.systemIdToNsUri(sysid);
      if (nsUri != null) { ns = new Namespaces(ns, "", nsUri); }
    }
    return asXml
        ? OpenElementStack.Factory.createXmlElementStack(
            doc, needsDebugData, ns, mq)
        : OpenElementStack.Factory.createHtml5ElementStack(
            doc, needsDebugData, mq);
  }

  public void setDomImpl(DOMImplementation domImpl) {
    this.domImpl = domImpl;
  }

  public static Document makeDocument(
      Function<DOMImplementation, DocumentType> doctypeMaker, String features,
      DOMImplementation domImpl) {
    if (features != null) { throw new SomethingWidgyHappenedError(); }
    if (domImpl == null) {
      domImpl = new CoreDocumentImpl().getImplementation();
    }

    DocumentType doctype = doctypeMaker != null
        ? doctypeMaker.apply(domImpl) : null;
    return domImpl.createDocument(null, null, doctype);
  }

  public static Document makeDocument(
      Function<DOMImplementation, DocumentType> doctypeMaker, String features) {
    return makeDocument(doctypeMaker, features, null);
  }

  /** Parse a document returning the document element. */
  public Element parseDocument() throws ParseException {
    return parseDocument(null);
  }

  /** Parse a document returning the document element. */
  private Element parseDocument(String features) throws ParseException {
    Function<DOMImplementation, DocumentType> doctypeMaker = findDoctype();
    Document doc = makeDocument(doctypeMaker, features, domImpl);
    OpenElementStack elementStack = makeElementStack(doc, mq);

    // Make sure the elementStack is empty.
    elementStack.open(false);

    skipTopLevelDocIgnorables(true);
    do {
      parseDom(elementStack);
      skipTopLevelDocIgnorables(false);
    } while (!tokens.isEmpty());

    FilePosition endPos = FilePosition.endOf(tokens.lastPosition());
    try {
      elementStack.finish(endPos);
    } catch (IllegalDocumentStateException ex) {
      throw new ParseException(ex.getCajaMessage(), ex);
    }

    DocumentFragment root = elementStack.getRootElement();
    Node firstChild = root.getFirstChild();
    if (firstChild == null || firstChild.getNodeType() != Node.ELEMENT_NODE) {
      throw new ParseException(new Message(
          DomParserMessageType.MISSING_DOCUMENT_ELEMENT, endPos));
    }

    // Check that there isn't any extraneous content after the root element.
    for (Node child = firstChild.getNextSibling(); child != null;
         child = child.getNextSibling()) {
      switch (child.getNodeType()) {
        case Node.COMMENT_NODE:
        case Node.DOCUMENT_TYPE_NODE:
          continue;
        case Node.TEXT_NODE:
          if ("".equals(child.getNodeValue().trim())) { continue; }
          break;
        default: break;
      }
      throw new ParseException(new Message(
          DomParserMessageType.MISPLACED_CONTENT,
          Nodes.getFilePositionFor(child)));
    }

    doc.appendChild(firstChild);

    if (elementStack.needsNamespaceFixup()) {
      firstChild = fixup(firstChild, ns);
    }

    return (Element) firstChild;
  }

  /**
   * Parses a snippet of markup.
   * The snippet need not be a single element as in an XML or HTML document.
   * For HTML, this will create no implied HTML, HEAD, or BODY elements.
   * If there is a DOCTYPE, it will be used to seed the default namespace.
   */
  public DocumentFragment parseFragment() throws ParseException {
    return parseFragment(makeDocument(findDoctype(), null, domImpl));
  }

  /**
   * Parses a snippet of markup creating new nodes using the given document.
   * The snippet need not be a single element as in an XML or HTML document.
   * For HTML, this will create no implied HTML, HEAD, or BODY elements.
   * Any doctype on the input will be ignored, and that on the input document
   * used instead.
   */
  public DocumentFragment parseFragment(Document doc) throws ParseException {
    OpenElementStack elementStack = makeElementStack(doc, mq);

    // Make sure the elementStack is empty.
    elementStack.open(true);

    skipFragmentIgnorables();

    while (!tokens.isEmpty()) {
      parseDom(elementStack);
      skipFragmentIgnorables();
    }

    FilePosition endPos = tokens.lastPosition();
    if (endPos != null) {
      endPos = FilePosition.endOf(endPos);
    } else {  // No lastPosition if the queue was empty.
      endPos = FilePosition.startOfFile(tokens.getInputSource());
    }
    try {
      elementStack.finish(endPos);
    } catch (IllegalDocumentStateException ex) {
      throw new ParseException(ex.getCajaMessage(), ex);
    }

    DocumentFragment fragment = elementStack.getRootElement();
    if (elementStack.needsNamespaceFixup()) {
      fragment = (DocumentFragment) fixup(fragment, ns);
    }
    return fragment;
  }

  /**
   * Skip over top level doctypes, and whitespace only text nodes.
   * Whitespace is significant for XML unless the schema specifies
   * otherwise, but whitespace outside the root element is not.  There is
   * one exception for whitespace preceding the prologue.
   * Comments are ignored by the underlying TreeBuilder unless explicitly
   * configured otherwise.
   */
  private void skipFragmentIgnorables() throws ParseException {
    while (!tokens.isEmpty()) {
      Token<HtmlTokenType> t = tokens.peek();
      switch (t.type) {
        case DIRECTIVE: break; // ignore DOCTYPEs
        default: return;
      }
      tokens.advance();
    }
  }

  private void skipTopLevelDocIgnorables(boolean atHead) throws ParseException {
    while (!tokens.isEmpty()) {
      Token<HtmlTokenType> t = tokens.peek();
      switch (t.type) {
        case TEXT:
          if (atHead && "".equals(t.text.trim())) { break; }
          return;
        case DIRECTIVE: break;
        default: return;
      }
      tokens.advance();
    }
  }

  private Node fixup(Node node, Namespaces ns) throws ParseException {
    switch (node.getNodeType()) {
      case Node.ELEMENT_NODE:
        Element el = (Element) node;
        Document doc = el.getOwnerDocument();
        // First, look at any xmlns:* attributes and add to the inScope
        // namespace.
        {
          NamedNodeMap attrs = el.getAttributes();
          for (int i = 0, n = attrs.getLength(); i < n; ++i) {
            Attr a = (Attr) attrs.item(i);
            if (a.getNamespaceURI() != null) { continue; }
            String name = a.getName();
            if (name.startsWith(AttributeNameFixup.XMLNS_PREFIX)) {
              String qname = AttributeNameFixup.qnameFromFixupName(name);
              String prefix = qname.substring(6);  // "xmlns:".length()
              String uri = a.getValue();
              ns = new Namespaces(ns, prefix, uri);
            }
          }
        }
        // Now we know what's in scope, find the element namespace.
        Namespaces elNs;
        if (el.getNamespaceURI() == null) {
          String qname = el.getTagName();
          elNs = ns.forElementName(qname);
          if (elNs == null) {
            // Try lower-casing the name to avoid HTML name fuzziness.
            String lQname = Strings.lower(qname);
            if (lQname != qname && (elNs = ns.forElementName(lQname)) != null) {
              qname = lQname;
            } else {
              FilePosition pos = Nodes.getFilePositionFor(el);
              ns = elNs = AbstractElementStack.unknownNamespace(
                  pos, ns, qname, mq);
            }
          }

          Element replacement;
          try {
            replacement = doc.createElementNS(elNs.uri, qname);
          } catch (DOMException e) {
            FilePosition pos = Nodes.getFilePositionFor(el);
            mq.addMessage(MessageType.INVALID_TAG_NAME, MessageLevel.WARNING,
                          FilePosition.startOf(pos),
                          MessagePart.Factory.valueOf(qname));
            if (!asXml) {
              // Try creating a non namespaced element as most likely so will
              // the browser.
              // At this point, we can be sure that createElement will not fail
              // because Html5ElementStack did not ignore this element.
              replacement = doc.createElement(qname);
            } else {
              throw new ParseException(new Message(
                  DomParserMessageType.IGNORING_TOKEN, pos,
                  MessagePart.Factory.valueOf("'" + qname + "'")), e);
            }
          }
          el.getParentNode().replaceChild(replacement, el);
          for (Node child; (child = el.getFirstChild()) != null; ) {
            replacement.appendChild(child);
          }
          NamedNodeMap attrs = el.getAttributes();
          while (attrs.getLength() != 0) {
            Attr a = (Attr) attrs.item(0);
            el.removeAttributeNode(a);
            replacement.setAttributeNodeNS(a);
          }
          if (needsDebugData) {
            Nodes.setFilePositionFor(replacement, Nodes.getFilePositionFor(el));
          }
          node = el = replacement;  // Return the replacement.
        } else {
          elNs = ns.forUri(el.getNamespaceURI());
          if (elNs == null) {
            FilePosition pos = Nodes.getFilePositionFor(el);
            ns = elNs = AbstractElementStack.unknownNamespace(
                pos, ns, el.getTagName(), mq);
          }
        }
        // And finally, namespace all the attributes.
        boolean modifiedAttrs;
        do {
          modifiedAttrs = false;
          NamedNodeMap attrs = el.getAttributes();
          for (int i = 0, n = attrs.getLength(); i < n; ++i) {
            Attr a = (Attr) attrs.item(i);
            String qname;
            {
              String name = a.getName();
              if (!name.startsWith(AttributeNameFixup.PREFIX)) { continue; }
              if (a.getNamespaceURI() != null) { continue; }
              qname = AttributeNameFixup.qnameFromFixupName(name);
            }
            Namespaces attrNs = ns.forAttrName(elNs, qname);
            if (attrNs == null) {
              String lQname = Strings.lower(qname);
              if (lQname != qname
                  && (attrNs = ns.forAttrName(elNs, lQname)) != null) {
                qname = lQname;
              } else {
                ns = attrNs = AbstractElementStack.unknownNamespace(
                    Nodes.getFilePositionFor(a), ns, qname, mq);
              }
            }

            // This may screw up the count or change the order of attributes in
            // attrs, so we do this operation in a loop to make sure that all
            // attributes are considered.
            createAttributeAndAddToElement(doc, attrNs.uri, qname, a, el);
            modifiedAttrs = true;
            n = el.getAttributes().getLength();
          }
        } while (modifiedAttrs);
        break;
      case Node.DOCUMENT_FRAGMENT_NODE:
        break;
      default: return node;
    }
    for (Node c = node.getFirstChild(); c != null; c = c.getNextSibling()) {
      c = fixup(c, ns);
    }
    return node;
  }

  /**
   * Helper method for creating a new attribute with the fixed up namespace
   * name. Ignores DOMException's raised by {@code createAttributeNS()} in case
   * the document being processed is html (removing the attribute that caused
   * the exception), rethrows it as a ParseException otherwise.
   *
   * @param doc The document being parsed.
   * @param attrNsUri The namespace uri for the attribute.
   * @param qname The fully qualified name of the attribute to insert.
   * @param attr The attribute to copy value from.
   * @param el The element to insert the attribute to.
   * @throws ParseException In case of errors.
   */
  void createAttributeAndAddToElement(Document doc, String attrNsUri,
                                      String qname, Attr attr, Element el)
      throws ParseException {
    Attr newAttr;
    try {
      newAttr = doc.createAttributeNS(attrNsUri, qname);
    } catch (DOMException e) {
      // Ignore DOMException's like NAMESPACE_ERR if the document being
      // processed is html. Also, remove the associated attribute node.
      if (!asXml) {
        mq.addMessage(DomParserMessageType.IGNORING_TOKEN,
                      Nodes.getFilePositionFor(attr),
                      MessagePart.Factory.valueOf("'" + qname + "'"));
        el.removeAttributeNode(attr);
        return;
      }
      throw new ParseException(new Message(DomParserMessageType.IGNORING_TOKEN,
          Nodes.getFilePositionFor(attr),
          MessagePart.Factory.valueOf("'" + qname + "'")), e);
    }

    newAttr.setValue(attr.getValue());
    if (needsDebugData) {
      Nodes.setFilePositionFor(newAttr, Nodes.getFilePositionFor(attr));
      Nodes.setFilePositionForValue(
          newAttr, Nodes.getFilePositionForValue(attr));
      Nodes.setRawValue(newAttr, Nodes.getRawValue(attr));
    }

    el.removeAttributeNode(attr);
    el.setAttributeNodeNS(newAttr);
  }

  /**
   * Creates a TokenQueue suitable for this class's parse methods.
   * @param asXml true to parse as XML, false as HTML.
   * @param in closed by this method.
   * @throws IOException when in raises an exception during read.
   */
  public static TokenQueue<HtmlTokenType> makeTokenQueue(
      InputSource is, Reader in, boolean asXml, boolean wantsComments)
      throws IOException {
    return makeTokenQueue(
        FilePosition.startOfFile(is), in, asXml, wantsComments);
  }

  /**
   * Creates a TokenQueue suitable for this class's parse methods.
   * @param pos the position of the first character on in.
   * @param in closed by this method.
   * @param asXml true to parse as XML, false as HTML.
   * @throws IOException when in raises an exception during read.
   */
  public static TokenQueue<HtmlTokenType> makeTokenQueue(
      FilePosition pos, Reader in, boolean asXml, boolean wantsComments)
      throws IOException {
    CharProducer cp = CharProducer.Factory.create(in, pos);
    HtmlLexer lexer = new HtmlLexer(cp);
    lexer.setTreatedAsXml(asXml);
    return new TokenQueue<HtmlTokenType>(
        lexer, pos.source(),
        wantsComments
            ? Criterion.Factory.<Token<HtmlTokenType>>optimist()
            : SKIP_COMMENTS);
  }

  public static final Criterion<Token<HtmlTokenType>> SKIP_COMMENTS
      = new Criterion<Token<HtmlTokenType>>() {
    public boolean accept(Token<HtmlTokenType> candidate) {
      return candidate.type != HtmlTokenType.COMMENT;
    }
  };

  /**
   * Parses a single top level construct, an element, or a text chunk from the
   * given queue.
   * @throws ParseException if elements are unbalanced -- sgml instead of xml
   *   attributes are missing values, or there is no top level construct to
   *   parse, or if there is a problem parsing the underlying stream.
   */
  private void parseDom(OpenElementStack out) throws ParseException {
    while (!tokens.isEmpty()) {
      Token<HtmlTokenType> t = tokens.pop();
      switch (t.type) {
        case TAGBEGIN:
          {
            List<AttrStub> attribs;
            Token<HtmlTokenType> end;
            if (isClose(t)) {
              attribs = Collections.emptyList();
              while (true) {
                end = tokens.pop();
                if (end.type == HtmlTokenType.TAGEND) { break; }
                // If this is not a tag end, then we should require
                // ignorable whitespace when we're parsing strictly.
                if (end.type != HtmlTokenType.IGNORABLE) {
                  mq.addMessage(
                      DomParserMessageType.IGNORING_TOKEN,
                      end.pos, MessagePart.Factory.valueOf(end.text));
                }
              }
            } else {
              attribs = Lists.newArrayList();
              end = parseTagAttributes(t.pos, attribs, out);
            }
            try {
              out.processTag(t, end, attribs);
            } catch (IllegalDocumentStateException ex) {
              throw new ParseException(ex.getCajaMessage(), ex);
            }
          }
          return;
        case CDATA:
        case TEXT:
        case UNESCAPED:
          out.processText(t);
          return;
        case COMMENT:
          out.processComment(t);
          return;
        case IE_DR_COMMENT_BEGIN:
        case IE_DR_COMMENT_END:
          out.processComment(t);
          if (!wantsComments()) {
            mq.addMessage(DomParserMessageType
                              .NOT_IGNORING_DOWNLEVEL_REVEALED_COMMENT,
                          t.pos, MessagePart.Factory.valueOf(t.text));
          }
          break;
        default:
          throw new ParseException(new Message(
              MessageType.MALFORMED_XHTML, t.pos,
              MessagePart.Factory.valueOf(t.text)));
      }
    }
  }

  /**
   * Parses attributes onto children and consumes and returns the end of tag
   * token.
   */
  private Token<HtmlTokenType> parseTagAttributes(
      FilePosition start, List<? super AttrStub> attrs, OpenElementStack out)
      throws ParseException {
    Token<HtmlTokenType> last;
    tokloop:
    while (true) {
      if (tokens.isEmpty()) {
        throw new ParseException(
            new Message(DomParserMessageType.UNCLOSED_TAG, start));
      }
      last = tokens.peek();
      switch (last.type) {
      case TAGEND:
        tokens.advance();
        break tokloop;
      case ATTRNAME:
        AttrStub a = parseAttrib(out);
        if (a != null) { attrs.add(a); }
        break;
      default:
        throw new ParseException(new Message(
            MessageType.MALFORMED_XHTML, FilePosition.span(start, last.pos),
            MessagePart.Factory.valueOf(last.text)));
      }
    }
    return last;
  }

  /**
   * Parses an element from a token stream.
   */
  private AttrStub parseAttrib(OpenElementStack out) throws ParseException {
    Token<HtmlTokenType> name = tokens.pop();
    Token<HtmlTokenType> value = tokens.peek();
    if (value.type == HtmlTokenType.ATTRVALUE) {
      tokens.advance();
      if (isAmbiguousAttributeValue(value.text)) {
        mq.addMessage(MessageType.AMBIGUOUS_ATTRIBUTE_VALUE,
                      FilePosition.span(name.pos, value.pos),
                      MessagePart.Factory.valueOf(name.text),
                      MessagePart.Factory.valueOf(value.text));
      }
    } else if (asXml) {
      if ("/".equals(name.text)) {
        mq.addMessage(MessageType.UNEXPECTED_IN_XML_TAG,
            FilePosition.span(name.pos, value.pos),
            MessagePart.Factory.valueOf(name.text));
        return null;
      }
      // XML does not allow valueless attributes.
      throw new ParseException(
          new Message(MessageType.MISSING_ATTRIBUTE_VALUE,
                      value.pos, MessagePart.Factory.valueOf(value.text)));
    } else {
      value = Token.instance(name.text, HtmlTokenType.ATTRVALUE, name.pos);
    }
    String rawValue = value.text;
    if (!asXml) {
      rawValue = out.fixBrokenEntities(rawValue, value.pos);
    }
    String decodedValue;
    int vlen = rawValue.length();
    if (vlen >= 2) {
      char ch0 = rawValue.charAt(0);
      char chn = rawValue.charAt(vlen - 1);
      int start = 0, end = vlen;
      if (chn == '"' || chn == '\'') {
        --end;
        // Handle unbalanced quotes as in <foo bar=baz">
        if (ch0 == chn) { start = 1; }
      }
      decodedValue = Nodes.decode(rawValue.substring(start, end));
    } else {
      decodedValue = Nodes.decode(rawValue);
    }
    return new AttrStub(name, value, decodedValue);
  }

  /**
   * True iff the given tag is an end tag.
   * @param t a token with type {@link HtmlTokenType#TAGBEGIN}
   */
  private static boolean isClose(Token<HtmlTokenType> t) {
    return t.text.startsWith("</");
  }

  private static boolean guessAsXml(LookaheadLexer la, InputSource is)
      throws ParseException {
    Token<HtmlTokenType> first = la.peek();
    Token<HtmlTokenType> firstNonSpace = first;
    if (firstNonSpace != null && "".equals(firstNonSpace.text.trim())) {
      Token<HtmlTokenType> space = firstNonSpace;
      la.next();
      firstNonSpace = la.peek();
      la.pushBack(space);
    }
    if (firstNonSpace == null) {
      return false;  // An empty document is not valid XML.
    }
    switch (firstNonSpace.type) {
      case DIRECTIVE:
        return firstNonSpace.text.startsWith("<?xml")
            || (firstNonSpace.text.startsWith("<!DOCTYPE")
                && !isHtmlDoctype(firstNonSpace.text));
      case TAGBEGIN:  // A namespaced tag name.
        if (firstNonSpace.text.indexOf(':') >= 0) { return true; }
        break;
      default: break;
    }
    // If we have a file extension, and this XML starts the file, instead
    // of being parsed from a CDATA section inside a larger document, then
    // guess based on the file extension.
    if (FilePosition.startOf(first.pos).equals(
            FilePosition.startOfFile(first.pos.source()))) {
      String path = is.getUri().getPath();
      if (path != null) {
        String ext = Strings.lower(
            path.substring(path.lastIndexOf('.') + 1));
        if ("html".equals(ext)) { return false; }
        if ("xml".equals(ext) || "xhtml".equals(ext)) { return true; }
      }
    }
    return false;
  }

  private Function<DOMImplementation, DocumentType> findDoctype()
      throws ParseException {
    if (tokens.isEmpty()) { return null; }
    Function<DOMImplementation, DocumentType> doctypeMaker = null;
    Mark start = tokens.mark();

    doctypeloop:
    while (!tokens.isEmpty()) {
      Token<HtmlTokenType> t = tokens.peek();
      switch (t.type) {
        case COMMENT:
        case IGNORABLE:
          tokens.pop();
          break;

        case DIRECTIVE:
          tokens.pop();
          final Function<DOMImplementation, DocumentType> maker
              = DoctypeMaker.parse(t.text);
          if (maker != null) {
            final FilePosition pos = t.pos;
            doctypeMaker = new Function<DOMImplementation, DocumentType>() {
              public DocumentType apply(DOMImplementation impl) {
                DocumentType t = maker.apply(impl);
                Nodes.setFilePositionFor(t, pos);
                return t;
              }
            };
            break doctypeloop;
          }
          break;

        case TEXT:
          String text = t.text;
          if (text.trim().equals("")) {
            // Ignore beginning whitespace.
            tokens.pop();
            break;
          }
          // Otherwise no doctype.
          break doctypeloop;

        default:
          break doctypeloop;
      }
    }
    tokens.rewind(start);
    return doctypeMaker;
  }

  private static boolean isHtmlDoctype(String s) {
    // http://htmlhelp.com/tools/validator/doctype.html
    return Pattern.compile("(?i:^<!DOCTYPE\\s+HTML\\b)").matcher(s).find()
        && !Pattern.compile("(?i:\\bXHTML\\b)").matcher(s).find();
  }

  private static final Pattern AMBIGUOUS_VALUE = Pattern.compile("^\\w+\\s*=");
  /**
   * True for the attribute value 'bar=baz' in {@code <a foo= bar=baz>}
   * which a naive reader might interpret as {@code <a foo="" bar="baz">}.
   */
  private static boolean isAmbiguousAttributeValue(String attributeText) {
    return AMBIGUOUS_VALUE.matcher(attributeText).find();
  }
}


/**
 * A TokenStream that wraps another TokenStream to provide an arbitrary
 * amount of token lookahead.
 * Used to allow the parser to examine the first non-whitespace & non-comment
 * token to determine whether to parse subsequent tokens as HTML or XML.
 */
final class LookaheadLexer implements TokenStream<HtmlTokenType> {
  private final TokenStream<HtmlTokenType> lexer;
  private final List<Token<HtmlTokenType>> pending
      = new LinkedList<Token<HtmlTokenType>>();

  LookaheadLexer(TokenStream<HtmlTokenType> lexer) {
    this.lexer = lexer;
  }

  /** True if {@link #next} is safe to call. */
  public boolean hasNext() throws ParseException {
    return !pending.isEmpty() || lexer.hasNext();
  }
  /** Returns the next token, and moves the stream position forward. */
  public Token<HtmlTokenType> next() throws ParseException {
    return pending.isEmpty() ? lexer.next() : pending.remove(0);
  }
  /** Returns the next token without consuming it, or null if no such token. */
  Token<HtmlTokenType> peek() throws ParseException {
    if (pending.isEmpty()) {
      if (!lexer.hasNext()) { return null; }
      Token<HtmlTokenType> next = lexer.next();
      pending.add(next);
      return next;
    }
    return pending.get(pending.size() - 1);
  }

  /**
   * Pushed t onto the head of the lookahead queue so that it becomes the
   * token returned by the next call to {@link #peek} or {@link #next}.
   */
  void pushBack(Token<HtmlTokenType> t) {
    pending.add(0, t);
  }
}
