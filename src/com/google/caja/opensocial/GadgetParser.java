// Copyright (C) 2007 Google Inc.
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

package com.google.caja.opensocial;

import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.HtmlLexer;
import com.google.caja.lexer.HtmlTokenType;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.ParseException;
import com.google.caja.lexer.Token;
import com.google.caja.lexer.TokenConsumer;
import com.google.caja.lexer.TokenQueue;
import com.google.caja.parser.AncestorChain;
import com.google.caja.parser.Visitor;
import com.google.caja.parser.html.DomParser;
import com.google.caja.parser.html.DomTree;
import com.google.caja.parser.html.MarkupRenderContext;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessageQueue;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Safe XML parser for gadget specifications. Rejects invalid markup.
 *
 * @author ihab.awad@gmail.com (Ihab Awad)
 */
public class GadgetParser {

  /**
   * Parse an OpenSocial gadget specification and return the result as a
   * {@link GadgetSpec}.
   *
   * @param gadgetSpec a gadget specification.
   * @param src the source of gadgetSpec.
   * @param view the view to parse
   * @exception ParseException if gadgetSpec is malformed.
   * @exception GadgetRewriteException if gadgetSpec doesn't validate.
   */
  public GadgetSpec parse(
      CharProducer gadgetSpec, InputSource src, String view, MessageQueue mq)
      throws GadgetRewriteException, ParseException {
    HtmlLexer lexer = new HtmlLexer(gadgetSpec);
    lexer.setTreatedAsXml(true);
    DomTree d = new DomParser(
        new TokenQueue<HtmlTokenType>(lexer, src), true, mq).parseDocument();

    //System.err.println(d.toStringDeep());

    GadgetSpec spec = new GadgetSpec();
    readModulePrefs(d, spec);
    readRequiredFeatures(d, spec);
    readContent(d, spec, view);

    return spec;
  }

  private void readModulePrefs(DomTree doc, GadgetSpec spec)
      throws GadgetRewriteException {
    List<DomTree.Tag> list = getElementsByTagName(doc, "ModulePrefs");
    check(list.size() == 1, "Must have exactly one <ModulePrefs>");
    DomTree.Tag modulePrefs = list.get(0);
    for (DomTree.Attrib attr : modulePrefs.getAttributeNodes()) {
      spec.getModulePrefs().put(attr.getAttribName(), attr.getAttribValue());
    }
  }

  private void readRequiredFeatures(DomTree doc, GadgetSpec spec)
      throws GadgetRewriteException {
    for (DomTree.Tag require : getElementsByTagName(doc, "Require")) {
      List<DomTree.Attrib> attribs = require.getAttributeNodes();
      check(attribs.size() == 1
            && "feature".equals(attribs.get(0).getAttribName()),
            "<Require> can only have a \"feature\" attribute");
      spec.getRequiredFeatures().add(attribs.get(0).getAttribValue());
    }
  }

  private void readContent(DomTree doc, GadgetSpec spec, String view)
      throws GadgetRewriteException {
    for (final DomTree.Tag contentNode : getElementsByTagName(doc, "Content")) {
      DomTree.Attrib viewAttr = contentNode.getAttribute("view");
      if (viewAttr == null
          || Arrays.asList(viewAttr.getAttribValue().trim().split("\\s*,\\s*"))
             .contains(view)) {
        DomTree.Attrib typeAttr = contentNode.getAttribute("type");
        check(typeAttr != null, "No 'type' attribute for view '" + view + "'");
        String value = typeAttr.getAttribValue();

        check(value.equals("html"), "Can't handle Content type '" + value +"'");

        spec.setContentType(value);

        spec.setContent(
            new GadgetSpec.CharProducerFactory() {
              public CharProducer producer() {
                List<CharProducer> chunks = new ArrayList<CharProducer>();
                for (DomTree child : contentNode.children()) {
                  if (child instanceof DomTree.Text) {
                    chunks.add(CharProducer.Factory.fromHtmlAttribute(
                        CharProducer.Factory.create(
                            new StringReader(
                                ((DomTree.Text) child).getToken().text),
                            child.getFilePosition())));
                  } else if (child instanceof DomTree.CData) {
                    String cdata = ((DomTree.CData) child).getValue();
                    FilePosition pos = child.getFilePosition();
                    // reduce the position to exclude the <![CDATA[ and ]]>
                    pos = FilePosition.instance(
                        pos.source(),
                        pos.startLineNo(), pos.startLogicalLineNo(),
                        pos.startCharInFile() + 9, pos.startCharInLine() + 9,
                        pos.endLineNo(), pos.endLogicalLineNo(),
                        pos.endCharInFile() - 3, pos.endCharInLine() - 3);
                    chunks.add(CharProducer.Factory.create(
                        new StringReader(cdata), pos));
                  }
                }
                return CharProducer.Factory.chain(
                    chunks.toArray(new CharProducer[0]));
              }
            });
        return;
      }
    }

    throw new GadgetRewriteException("No content for view '" + view + "'");
  }

  /**
   * Render the given gadgetSpec as XML.
   *
   * @param output to which XML is written.
   */
  public void render(GadgetSpec gadgetSpec, Appendable output) {
    DomTree doc = toDocument(gadgetSpec);
    TokenConsumer tc = doc.makeRenderer(output, null);
    doc.render(new MarkupRenderContext(new MessageContext(), tc, true));
  }

  private DomTree toDocument(GadgetSpec gadgetSpec) {
    List<DomTree.Attrib> prefs = new ArrayList<DomTree.Attrib>();
    for (Map.Entry<String, String> e : gadgetSpec.getModulePrefs().entrySet()) {
      prefs.add(attrib(e.getKey(), e.getValue()));
    }

    List<DomTree.Tag> features = new ArrayList<DomTree.Tag>();
    for (String feature : gadgetSpec.getRequiredFeatures()) {
      features.add(el("Require", attrib("feature", feature)));
    }

    DomTree.Tag modulePrefs = el("ModulePrefs");
    modulePrefs.createMutation()
        .appendChildren(prefs)
        .appendChildren(features)
        .execute();

    DomTree.Tag content = el(
        "Content", attrib("type", gadgetSpec.getContentType()),
        cdata(drain(gadgetSpec.getContent())));

    return el("Module", modulePrefs, content);
  }

  private static DomTree.Tag el(String name, DomTree... children) {
    return new DomTree.Tag(Arrays.asList(children), Token.instance(
        "<" + name, HtmlTokenType.TAGBEGIN, FilePosition.UNKNOWN),
        FilePosition.UNKNOWN);
  }

  private static DomTree.Attrib attrib(String name, String value) {
    return new DomTree.Attrib(
        new DomTree.Value(
            Token.instance(value, HtmlTokenType.ATTRVALUE,
                           FilePosition.UNKNOWN)),
        Token.instance(name, HtmlTokenType.ATTRNAME, FilePosition.UNKNOWN),
        FilePosition.UNKNOWN);
  }

  private static DomTree.CData cdata(String text) {
    return new DomTree.CData(
        Token.instance("<![CDATA[" + text + "]]>", HtmlTokenType.CDATA,
                       FilePosition.UNKNOWN));
  }

  private void check(boolean condition, String msg)
      throws GadgetRewriteException {
    if (!condition) throw new GadgetRewriteException(msg);
  }

  private String drain(CharProducer cp) {
    try {
      StringBuilder sb = new StringBuilder();
      for (int ch = 0; (ch = cp.read()) >= 0;) { sb.append((char) ch); }
      return sb.toString();
    } catch (IOException ex) {
      throw new RuntimeException();
    }
  }

  private static List<DomTree.Tag> getElementsByTagName(
      DomTree t, final String name) {
    final List<DomTree.Tag> els = new ArrayList<DomTree.Tag>();
    t.acceptPreOrder(
        new Visitor() {
          public boolean visit(AncestorChain<?> ac) {
            if (ac.node instanceof DomTree.Tag) {
              DomTree.Tag el = (DomTree.Tag) ac.node;
              if (name.equals(el.getTagName())) { els.add(el); }
            }
            return true;
          }
        }, null);
    return els;
  }
}
