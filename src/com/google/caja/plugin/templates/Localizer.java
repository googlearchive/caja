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

package com.google.caja.plugin.templates;

import com.google.caja.SomethingWidgyHappenedError;
import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.HtmlLexer;
import com.google.caja.lexer.HtmlTokenType;
import com.google.caja.lexer.ParseException;
import com.google.caja.lexer.Token;
import com.google.caja.parser.html.Namespaces;
import com.google.caja.parser.html.Nodes;
import com.google.caja.render.Concatenator;
import com.google.caja.reporting.MarkupRenderMode;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.RenderContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Replaces message strings in IHTML with ones from a message bundle.
 *
 * <p>
 * This does not require that the IHTML document contain any templates, so can
 * be used on a subset of IHTML that only includes {@code ihtml:message},
 * {@code ihtml:ph} and {@code ihtml:eph} to do message message substitution
 * that returns plain HTML.
 *
 * <p>
 * This assumes that the input has been {@link IhtmlSanityChecker sanitized}.
 *
 * @author mikesamuel@gmail.com
 */
public class Localizer {
  private final MessageQueue mq;

  /**
   * @param mq receives errors and warnings about undefined messages; and
   *     missing, and extraneous placeholders.
  */
  public Localizer(MessageQueue mq) {
    this.mq = mq;
  }

  /**
   * Replace <code>ihtml:message</code> elements with the messages in cx.
   */
  public void localize(Element root, IhtmlL10NContext cx) {
    final Document doc = root.getOwnerDocument();
    for (Element message : snapshot(allMessages(root))) {
      String name = IHTML.getName(message).getValue();
      // presence of name verified by the IhtmlSanityCheck class
      final Map<String, Placeholder> placeholders
          = byName(extractPlaceholders(message));
      LocalizedHtml substitution = cx.getMessageByName(name);
      if (substitution == null) {
        notifyUntranslatedMessage(message, cx.getLocale());
        substitution = messageToLocalizedHtml(message);
      }
      DocumentFragment substitutedMessage;
      try {
        substitutedMessage = substitution.substitute(
            doc, new LocalizedHtml.PlaceholderHandler() {
              public Iterator<Token<HtmlTokenType>> substitutePlaceholder(
                  String placeholderName, FilePosition placeholderLoc) {
                Placeholder ph = placeholders.get(placeholderName);
                if (ph == null) {
                  notifyMissingPlaceholder(placeholderLoc);
                  return Collections.<Token<HtmlTokenType>>emptyList()
                      .iterator();
                }
                return Localizer.tokensFromNode(ph.start).iterator();
              }
            });
      } catch (ParseException ex) {
        notifyMalformedMessage(message, name);
        ex.toMessageQueue(mq);
        substitutedMessage = doc.createDocumentFragment();
      }
      for (Node child : Nodes.childrenOf(substitutedMessage)) {
        message.getParentNode().insertBefore(child, message);
      }
      message.getParentNode().removeChild(message);
    }
  }

  private List<Placeholder> extractPlaceholders(Element message) {
    List<Placeholder> placeholders = new ArrayList<Placeholder>();
    Element ph = null;
    for (Element el : IHTML.getPlaceholders(message)) {
      // Allow placeholders that are not siblings as per
      // <ihtml:ph name="startLink"/>
      // <a><ihtml:attribute><ihtml:dynamic expr="href"/></ihtml:attribute>
      // <ihtml:eph/>
      // Click
      // <ihtml:ph name="endLink"/>
      // </a>
      // <ihtml:eph/>
      if (ph != null) {
        Element eph = el;
        assert IHTML.isPh(ph);  // Enforced by IhtmlSanityCheck
        assert IHTML.isEph(eph);  // Enforced by IhtmlSanityCheck
        placeholders.add(new Placeholder(ph));
        ph = null;
      } else {
        ph = el;
      }
    }
    assert ph == null;  // Enforced by IhtmlSanityCheck
    return placeholders;
  }

  private Map<String, Placeholder> byName(List<Placeholder> phs) {
    Map<String, Placeholder> byName = new LinkedHashMap<String, Placeholder>();
    for (Placeholder ph : phs) {
      String name = IHTML.getName(ph.start).getValue();
      if (byName.containsKey(name)) {
        notifyDupePlaceholder(ph, byName.get(name));
      }
      byName.put(name, ph);
    }
    return byName;
  }

  private static final String XML_NS = Namespaces.XML_NAMESPACE_URI;
  public IhtmlL10NContext extractMessages(Element ihtmlRoot) {
    Locale locale = ihtmlRoot.hasAttributeNS(XML_NS, "lang")
        ? new Locale(ihtmlRoot.getAttributeNS(XML_NS, "lang").replace('-', '_'))
        // Choose a default that is independent of the default locale since we
        // typically run tests in the Turkish locale.
        : Locale.ENGLISH;
    Map<String, Element> messageEls = new HashMap<String, Element>();
    Map<String, LocalizedHtml> messages
        = new LinkedHashMap<String, LocalizedHtml>();
    for (Element message : allMessages(ihtmlRoot)) {
      LocalizedHtml extracted = messageToLocalizedHtml(message);
      if (extracted == null) { continue; }
      LocalizedHtml orig = messages.get(extracted.getName());
      if (orig != null) {
        if (!orig.getSerializedForm().equals(extracted.getSerializedForm())) {
          notifyDupeMessage(message, messageEls.get(extracted.getName()));
        }
      } else {
        messages.put(extracted.getName(), extracted);
        messageEls.put(extracted.getName(), message);
      }
    }
    return new IhtmlL10NContext(locale, messages);
  }

  private LocalizedHtml messageToLocalizedHtml(Element message) {
    // Clone the message.
    message = (Element) message.cloneNode(true);
    String name = IHTML.getName(message).getValue();
    StringBuilder filteredXhtml = new StringBuilder();
    if (message.getFirstChild() != null) {
      // Render an XHTML string containing the message content, with embedded
      // <ph> elements.
      StringBuilder xhtml = new StringBuilder();
      RenderContext rc = new RenderContext(new Concatenator(xhtml))
          .withMarkupRenderMode(MarkupRenderMode.XML);
      for (Node c : Nodes.childrenOf(message)) {
        Nodes.render(c, rc);
      }
      rc.getOut().noMoreTokens();
      HtmlLexer lexer = new HtmlLexer(
          CharProducer.Factory.fromString(
              xhtml.toString(),
              Nodes.getFilePositionFor(message.getFirstChild())));
      lexer.setTreatedAsXml(true);
      // 1 - saw <ihtml:ph
      // 2 - saw <ihtml:ph >
      // 3 - saw <ihtml:ph > </ihtml:ph
      // 4 - saw <ihtml:ph > </ihtml:ph > or <ihtml:ph />
      // 5 - saw <ihtml:eph
      // 6 - saw <ihtml:eph >
      // 7 - saw <ihtml:eph > </ihtml:eph
      int state = 0;
      // Filter out everything from the end of the <ihtml:ph> exclusive to the
      // end of the <ihtml:eph> inclusive.
      try {
        while (lexer.hasNext()) {
          Token<HtmlTokenType> tt = lexer.next();
          boolean emit = state < 4;
          switch (tt.type) {
            case TAGBEGIN:
              if (state == 0 && "<ihtml:ph".equals(tt.text)) {
                state = 1;
              } else if (state == 2 && "</ihtml:ph".equals(tt.text)) {
                state = 3;
              } else if (state == 4 && "<ihtml:eph".equals(tt.text)) {
                state = 5;
              } else if (state == 6 && "</ihtml:eph".equals(tt.text)) {
                state = 7;
              }
              break;
            case TAGEND:
              boolean selfclose = "/>".equals(tt.text);
              if (state == 1 && !selfclose) {
                state = 2;
              } else if (state == 1 && selfclose || state == 3) {
                state = 4;
              } else if (state == 5 && !selfclose) {
                state = 6;
              } else if (state == 5 && selfclose || state == 7) {
                state = 0;
              }
              if (emit && "/>".equals(tt.text)) { filteredXhtml.append(' '); }
              break;
            case ATTRNAME:
              if (emit) { filteredXhtml.append(' '); }
              break;
            case ATTRVALUE:
              if (emit) { filteredXhtml.append('='); }
              break;
            default: break;
          }
          if (emit) { filteredXhtml.append(tt.text); }
        }
      } catch (ParseException ex) {
        throw new SomethingWidgyHappenedError(
            "IOException reading from String", ex);
      }
    }

    return new LocalizedHtml(name, filteredXhtml.toString());
  }

  private static class Placeholder {
    final Element start;
    Placeholder(Element ph) {
      this.start = ph;
    }
  }

  private static Iterable<Element> allMessages(Element root) {
    return Nodes.nodeListIterable(
        root.getElementsByTagNameNS(IHTML.NAMESPACE, "message"), Element.class);
  }

  private static <T> List<T> snapshot(Iterable<T> it) {
    List<T> snapshot = new ArrayList<T>();
    for (T el : it) {
      snapshot.add(el);
    }
    return snapshot;
  }

  private void notifyUntranslatedMessage(Element message, Locale locale) {
    mq.addMessage(
        IhtmlMessageType.UNTRANSLATED_MESSAGE,
        Nodes.getFilePositionFor(message),
        MessagePart.Factory.valueOf(IHTML.getName(message).getValue()),
        MessagePart.Factory.valueOf(locale.toString()));
  }

  private void notifyMissingPlaceholder(FilePosition phLoc) {
    mq.addMessage(IhtmlMessageType.MISSING_PLACEHOLDER, phLoc);
  }

  private void notifyMalformedMessage(Element message, String name) {
    mq.addMessage(
        IhtmlMessageType.MALFORMED_MESSAGE,
        Nodes.getFilePositionFor(message), MessagePart.Factory.valueOf(name));
  }

  private void notifyDupeMessage(Element a, Element b) {
    mq.addMessage(
        IhtmlMessageType.DUPLICATE_MESSAGE,
        Nodes.getFilePositionFor(a),
        MessagePart.Factory.valueOf(IHTML.getName(a).getValue()),
        Nodes.getFilePositionFor(b));
  }

  private void notifyDupePlaceholder(Placeholder ph, Placeholder orig) {
    mq.addMessage(
        IhtmlMessageType.DUPLICATE_PLACEHOLDER,
        Nodes.getFilePositionFor(ph.start),
        MessagePart.Factory.valueOf(IHTML.getName(ph.start).getValue()),
        Nodes.getFilePositionFor(orig.start));
  }

  public static Iterable<Token<HtmlTokenType>> tokensFromNode(Node n) {
    List<Token<HtmlTokenType>> toks = new ArrayList<Token<HtmlTokenType>>();

    // The end placeholder must be a descendant of a sibling of an ancestor of
    // n as in
    // (1) <a><ihtml:ph/></a><ihtml:eph/>
    //        ^^^^^^^^^^^    ^^^^^^^^^^^^
    //     This is n.     Stop here.
    // where the DOM structure looks like
    // DocumentFragment
    //   Element : a
    //     Element : ihtml:ph   <-- n
    //   Element : ihtml:eph
    // and in
    // (2) <ihtml:ph/><a><ihtml:eph/>
    //     ^^^^^^^^^^    ^^^^^^^^^^^^
    //     This is n.     Stop here.
    // where the DOM structure looks like
    // DocumentFragment
    //   Element : ihtml:ph   <-- n
    //   Element : a
    //     Element : ihtml:eph
    //
    // In (1) the below finds no siblings of n, so it descends to the parent,
    // emitting the close element for the <a> element in the process.  Then
    // it reenters the loop and calls emitTokens with the next sibling of the
    // <a> element.  emitTokens recognizes that it was passed an <ihtml:eph>
    // element and returns false stopping the whole process.
    // In (2) next sibling is an <a> element, and so emitTokens writes out
    // the start tag, before recursing.  On the first recursion, it finds
    // the <ihtml:eph> and returns false.  That false bubbles all the way up
    // to this method which stops the whole process.
    Node ancestor = n;
    emit_loop:
    do {
      for (Node sib = ancestor; (sib = sib.getNextSibling()) != null ;) {
        if (!emitTokens(sib, toks)) { break emit_loop; }
      }
      ancestor = ancestor.getParentNode();
      if (ancestor == null) { break; }
      emitEndOf(ancestor, toks);
    } while (true);
    return toks;
  }

  private static boolean emitTokens(Node n, List<Token<HtmlTokenType>> out) {
    FilePosition pos = Nodes.getFilePositionFor(n);
    switch (n.getNodeType()) {
      case Node.TEXT_NODE:
      case Node.CDATA_SECTION_NODE:
        out.add(Token.instance(
            Nodes.encode(n.getNodeValue()), HtmlTokenType.TEXT, pos));
        break;
      case Node.ELEMENT_NODE:
        if (IHTML.isEph(n)) { return false; }
        Element e = (Element) n;
        FilePosition spos = FilePosition.startOf(pos);
        out.add(Token.instance("<" + tagName(e), HtmlTokenType.TAGBEGIN, spos));
        FilePosition cpos = spos;
        for (Attr a : Nodes.attributesOf(e)) {
          emitTokens(a, out);
          cpos = Nodes.getFilePositionForValue(a);
        }
        cpos = FilePosition.endOf(cpos);
        if (n.getFirstChild() == null) {
          out.add(Token.instance("/>", HtmlTokenType.TAGEND, cpos));
        } else {
          out.add(Token.instance(">", HtmlTokenType.TAGEND, cpos));
          for (Node child : Nodes.childrenOf(n)) {
            if (!emitTokens(child, out)) { return false; }
          }
          emitEndOf(e, out);
        }
        break;
      case Node.ATTRIBUTE_NODE:
        Attr a = (Attr) n;
        out.add(Token.instance(a.getName(), HtmlTokenType.ATTRNAME, pos));
        out.add(Token.instance(
            '"' + Nodes.encode(a.getValue()) + '"', HtmlTokenType.ATTRVALUE,
            Nodes.getFilePositionForValue(a)));
        break;
      default: break;
    }
    return true;
  }

  private static void emitEndOf(Node n, List<Token<HtmlTokenType>> out) {
    if (n instanceof Element) {
      Element e = (Element) n;
      FilePosition epos = FilePosition.endOf(Nodes.getFilePositionFor(e));
      out.add(Token.instance(
          "</" + tagName(e), HtmlTokenType.TAGBEGIN, epos));
      out.add(Token.instance(">", HtmlTokenType.TAGEND, epos));
    }
  }

  private static String tagName(Element e) {
    Namespaces ns = Namespaces.COMMON.forUri(e.getNamespaceURI());
    String prefix = ns != null ? ns.prefix : "";
    String localName = e.getLocalName();
    return "".equals(prefix) ? localName : prefix + ":" + localName;
  }
}
