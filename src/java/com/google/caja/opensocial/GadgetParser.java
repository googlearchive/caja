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

import com.google.caja.util.AppendableWriter;
import com.google.caja.util.ReadableReader;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;

/**
 * Safe XML parser for gadget specifications. Rejects invalid markup.
 *
 * <p>TODO(ihab.awad): Sanitize and escape text in attribute values.
 * 
 * @author ihab.awad@gmail.com (Ihab Awad)
 */
public class GadgetParser {

  /**
   * Parse an OpenSocial gadget specification and return the result as an object.
   *
   * @param gadgetSpec a gadget specification, represented as a string of text.
   * @return a {@code GadgetSpec}.
   * @exception GadgetRewriteException if a problem in parsing occurs, or if the gadget
   * specification is not syntactically valid.
   * @exception IOException if an I/O problem occurs
   */
  public GadgetSpec parse(Readable gadgetSpec) throws GadgetRewriteException, IOException {
    Document d;
    try {
      d = newDocumentBuilder().parse(new InputSource(new ReadableReader(gadgetSpec)));
    } catch (SAXException e) {
      throw new GadgetRewriteException(e);
    }

    GadgetSpec spec = new GadgetSpec();
    getModulePrefs(d, spec);
    getRequiredFeatures(d, spec);
    getContent(d, spec);

    return spec;
  }

  private void getModulePrefs(Document doc, GadgetSpec spec) throws GadgetRewriteException {
    NodeList list = doc.getElementsByTagName("ModulePrefs");
    check(list.getLength() == 1, "Must have exactly one <ModulePrefs>");
    Node modulePrefs = list.item(0);
    NamedNodeMap m = modulePrefs.getAttributes();
    for (int i = 0; i < m.getLength(); i++) {
      spec.getModulePrefs().put(
          ((Attr)m.item(i)).getName(),
          ((Attr)m.item(i)).getValue());
    }
  }

  private void getRequiredFeatures(Document doc, GadgetSpec spec) throws GadgetRewriteException {
    NodeList list = doc.getElementsByTagName("Require");
    for (int i = 0; i < list.getLength(); i++) {
      Node n = list.item(i);
      check(n.getAttributes().getLength() == 1, "<Require> can only have one attribute");
      String name = ((Attr)n.getAttributes().item(0)).getName();
      String value = ((Attr)n.getAttributes().item(0)).getValue();
      check("feature".equals(name), "<Require> can only have \"feature\" attribute");
      spec.getRequiredFeatures().add(value);
    }
  }

  private void getContent(Document doc, GadgetSpec spec) throws GadgetRewriteException {
    NodeList list = doc.getElementsByTagName("Content");
    check(list.getLength() == 1, "Must have exactly one <Content>");
    Node contentNode = list.item(0);
    check(contentNode.getAttributes().getLength() == 1, "<Content> can only have one attribute");

    String name = ((Attr)contentNode.getAttributes().item(0)).getName();
    String value = ((Attr)contentNode.getAttributes().item(0)).getValue();
    check("type".equals(name), "<Content> can only have \"type\" attribute");

    spec.setContentType(value);
    
    StringBuilder content = new StringBuilder();
    
    for (int i = 0; i < contentNode.getChildNodes().getLength(); i++) {
      if (contentNode.getChildNodes().item(i) instanceof Text) {
        Text text = (Text)contentNode.getChildNodes().item(i);
        content.append(text.getNodeValue());
      }
    }

    spec.setContent(content.toString());
  }

  /**
   * Given a {@code GadgetSpec}, render its contents as a string of XML text.
   *
   * @param gadgetSpec a gadget specification object as a {@code GadgetSpec}.
   * @param output an {@code Appendable} to which the contents will be written.
   * @exception GadgetRewriteException if there is a problem in rendering.
   * @exception IOException if an I/O problem occurs.
   */
  public void render(GadgetSpec gadgetSpec, Appendable output)
      throws GadgetRewriteException, IOException {
    try {
      TransformerFactory.newInstance().newTransformer().transform(
          new DOMSource(getDocument(gadgetSpec)),
          new StreamResult(new AppendableWriter(output)));
    } catch (TransformerConfigurationException e) {
      throw new GadgetRewriteException(e);
    } catch (TransformerException e) {
      throw new GadgetRewriteException(e);
    }
  }

  private Document getDocument(GadgetSpec gadgetSpec) throws GadgetRewriteException {
    Document d = newDocumentBuilder().newDocument();
    Node module = d.createElement("Module");
    d.appendChild(module);

    Node modulePrefs = d.createElement("ModulePrefs");
    module.appendChild(modulePrefs);

    for (String key : gadgetSpec.getModulePrefs().keySet()) {
      Attr attr = d.createAttribute(key);
      attr.setValue(gadgetSpec.getModulePrefs().get(key));
      modulePrefs.getAttributes().setNamedItem(attr);
    }

    for (String feature : gadgetSpec.getRequiredFeatures()) {
      Node require = d.createElement("Require");
      Attr attr = d.createAttribute("feature");
      attr.setValue(feature);
      require.getAttributes().setNamedItem(attr);
      modulePrefs.appendChild(require);
    }

    Node contentNode = d.createElement("Content");
    module.appendChild(contentNode);

    {
      Attr attr = d.createAttribute("type");
      attr.setValue(gadgetSpec.getContentType());
      contentNode.getAttributes().setNamedItem(attr);
    }

    contentNode.appendChild(d.createCDATASection(gadgetSpec.getContent()));

    return d;
  }

  private DocumentBuilder newDocumentBuilder() throws GadgetRewriteException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder;

    try {
      builder = factory.newDocumentBuilder();
    } catch (ParserConfigurationException e) {
      throw new GadgetRewriteException(e);
    }

    builder.setEntityResolver(new EntityResolver() {
      public InputSource resolveEntity(String publicId, String systemId)
          throws SAXException, IOException {
        throw new IOException("Entity resolution not supported");
      }
    });

    return builder;
  }

  private void check(boolean condition, String msg) throws GadgetRewriteException {
    if (!condition) throw new GadgetRewriteException(msg);
  }
}