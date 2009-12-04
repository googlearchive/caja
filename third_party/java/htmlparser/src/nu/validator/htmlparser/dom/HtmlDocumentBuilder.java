/*
 * Copyright (c) 2007 Henri Sivonen
 * Copyright (c) 2007-2008 Mozilla Foundation
 *
 * Permission is hereby granted, free of charge, to any person obtaining a 
 * copy of this software and associated documentation files (the "Software"), 
 * to deal in the Software without restriction, including without limitation 
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, 
 * and/or sell copies of the Software, and to permit persons to whom the 
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in 
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL 
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER 
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING 
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER 
 * DEALINGS IN THE SOFTWARE.
 */

package nu.validator.htmlparser.dom;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import nu.validator.htmlparser.common.DoctypeExpectation;
import nu.validator.htmlparser.common.DocumentModeHandler;
import nu.validator.htmlparser.common.Heuristics;
import nu.validator.htmlparser.common.XmlViolationPolicy;
import nu.validator.htmlparser.io.Driver;

import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * This class implements an HTML5 parser that exposes data through the DOM 
 * interface. 
 * 
 * <p>By default, when using the constructor without arguments, the 
 * this parser coerces XML 1.0-incompatible infosets into XML 1.0-compatible
 * infosets. This corresponds to <code>ALTER_INFOSET</code> as the general 
 * XML violation policy. To make the parser support non-conforming HTML fully 
 * per the HTML 5 spec while on the other hand potentially violating the SAX2 
 * API contract, set the general XML violation policy to <code>ALLOW</code>. 
 * This does not work with a standard DOM implementation.
 * It is possible to treat XML 1.0 infoset violations as fatal by setting 
 * the general XML violation policy to <code>FATAL</code>. 
 * 
 * <p>The doctype is not represented in the tree.
 * 
 * <p>The document mode is represented as user data <code>DocumentMode</code> 
 * object with the key <code>nu.validator.document-mode</code> on the document 
 * node. 
 * 
 * <p>The form pointer is also stored as user data with the key 
 * <code>nu.validator.form-pointer</code>.
 * 
 * @version $Id: HtmlDocumentBuilder.java 463 2008-10-03 11:46:38Z hsivonen $
 * @author hsivonen
 */
public class HtmlDocumentBuilder extends DocumentBuilder {

    /**
     * Returns the JAXP DOM implementation.
     * 
     * @return the JAXP DOM implementation
     */
    private static DOMImplementation jaxpDOMImplementation() {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder;
        try {
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
        return builder.getDOMImplementation();
    }

    /**
     * The tokenizer.
     */
    private final Driver tokenizer;

    /**
     * The tree builder.
     */
    private final DOMTreeBuilder domTreeBuilder;

    /**
     * The DOM impl.
     */
    private final DOMImplementation implementation;

    /**
     * The entity resolver.
     */
    private EntityResolver entityResolver;

    /**
     * Instantiates the document builder with a specific DOM 
     * implementation and XML violation policy.
     * 
     * @param implementation
     *            the DOM implementation
     *            @param xmlPolicy the policy
     */
    public HtmlDocumentBuilder(DOMImplementation implementation,
            XmlViolationPolicy xmlPolicy) {
        this.implementation = implementation;
        this.domTreeBuilder = new DOMTreeBuilder(implementation);
        this.tokenizer = new Driver(domTreeBuilder);
        this.tokenizer.setXmlnsPolicy(XmlViolationPolicy.ALTER_INFOSET);
        setXmlPolicy(xmlPolicy);
    }

    /**
     * Instantiates the document builder with a specific DOM implementation 
     * and the infoset-altering XML violation policy.
     * 
     * @param implementation
     *            the DOM implementation
     */
    public HtmlDocumentBuilder(DOMImplementation implementation) {
        this(implementation, XmlViolationPolicy.ALTER_INFOSET);
    }

    /**
     * Instantiates the document builder with the JAXP DOM implementation 
     * and the infoset-altering XML violation policy.
     */
    public HtmlDocumentBuilder() {
        this(XmlViolationPolicy.ALTER_INFOSET);
    }

    /**
     * Instantiates the document builder with the JAXP DOM implementation 
     * and a specific XML violation policy.
     *            @param xmlPolicy the policy
     */
    public HtmlDocumentBuilder(XmlViolationPolicy xmlPolicy) {
        this(jaxpDOMImplementation(), xmlPolicy);
    }

    /**
     * Returns the DOM implementation
     * @return the DOM implementation
     * @see javax.xml.parsers.DocumentBuilder#getDOMImplementation()
     */
    @Override public DOMImplementation getDOMImplementation() {
        return implementation;
    }

    /**
     * Returns <code>true</code>.
     * @return <code>true</code>
     * @see javax.xml.parsers.DocumentBuilder#isNamespaceAware()
     */
    @Override public boolean isNamespaceAware() {
        return true;
    }

    /**
     * Returns <code>false</code>
     * @return <code>false</code>
     * @see javax.xml.parsers.DocumentBuilder#isValidating()
     */
    @Override public boolean isValidating() {
        return false;
    }

    /**
     * For API compatibility.
     * @see javax.xml.parsers.DocumentBuilder#newDocument()
     */
    @Override public Document newDocument() {
        return implementation.createDocument(null, null, null);
    }

    /**
     * Parses a document from a SAX <code>InputSource</code>.
     * @param is the source
     * @return the doc
     * @throws SAXException if stuff goes wrong
     * @throws IOException if IO goes wrong
     * @see javax.xml.parsers.DocumentBuilder#parse(org.xml.sax.InputSource)
     */
    @Override public Document parse(InputSource is) throws SAXException,
            IOException {
        domTreeBuilder.setFragmentContext(null);
        tokenize(is);
        return domTreeBuilder.getDocument();
    }

    /**
     * Parses a document fragment from a SAX <code>InputSource</code>.
     * @param is the source
     * @param context the context element name
     * @return the doc
     * @throws SAXException if stuff goes wrong
     * @throws IOException if IO goes wrong
     */
    public DocumentFragment parseFragment(InputSource is, String context)
            throws IOException, SAXException {
        domTreeBuilder.setFragmentContext(context.intern());
        tokenize(is);
        return domTreeBuilder.getDocumentFragment();
    }

    /**
     * Sets the entity resolver for URI-only inputs.
     * @param resolver the resolver
     * @see javax.xml.parsers.DocumentBuilder#setEntityResolver(org.xml.sax.EntityResolver)
     */
    @Override public void setEntityResolver(EntityResolver resolver) {
        this.entityResolver = resolver;
    }

    /**
     * Sets the error handler.
     * @param errorHandler the handler
     * @see javax.xml.parsers.DocumentBuilder#setErrorHandler(org.xml.sax.ErrorHandler)
     */
    @Override public void setErrorHandler(ErrorHandler errorHandler) {
        domTreeBuilder.setErrorHandler(errorHandler);
        tokenizer.setErrorHandler(errorHandler);
    }

    /**
     * Sets whether comment nodes appear in the tree.
     * @param ignoreComments <code>true</code> to ignore comments
     * @see nu.validator.htmlparser.impl.TreeBuilder#setIgnoringComments(boolean)
     */
    public void setIgnoringComments(boolean ignoreComments) {
        domTreeBuilder.setIgnoringComments(ignoreComments);
    }

    /**
     * Sets whether the parser considers scripting to be enabled for noscript treatment.
     * @param scriptingEnabled <code>true</code> to enable
     * @see nu.validator.htmlparser.impl.TreeBuilder#setScriptingEnabled(boolean)
     */
    public void setScriptingEnabled(boolean scriptingEnabled) {
        domTreeBuilder.setScriptingEnabled(scriptingEnabled);
    }

    /**
     * Toggles the checking of the NFC normalization of source.
     * @param enable <code>true</code> to check normalization
     * @see nu.validator.htmlparser.impl.Tokenizer#setCheckingNormalization(boolean)
     */
    public void setCheckingNormalization(boolean enable) {
        tokenizer.setCheckingNormalization(enable);
    }

    /**
     * Sets the policy for consecutive hyphens in comments.
     * @param commentPolicy the policy
     * @see nu.validator.htmlparser.impl.Tokenizer#setCommentPolicy(nu.validator.htmlparser.common.XmlViolationPolicy)
     */
    public void setCommentPolicy(XmlViolationPolicy commentPolicy) {
        tokenizer.setCommentPolicy(commentPolicy);
    }

    /**
     * Sets the policy for non-XML characters except white space.
     * @param contentNonXmlCharPolicy the policy
     * @see nu.validator.htmlparser.impl.Tokenizer#setContentNonXmlCharPolicy(nu.validator.htmlparser.common.XmlViolationPolicy)
     */
    public void setContentNonXmlCharPolicy(
            XmlViolationPolicy contentNonXmlCharPolicy) {
        tokenizer.setContentNonXmlCharPolicy(contentNonXmlCharPolicy);
    }

    /**
     * Sets the policy for non-XML white space.
     * @param contentSpacePolicy the policy
     * @see nu.validator.htmlparser.impl.Tokenizer#setContentSpacePolicy(nu.validator.htmlparser.common.XmlViolationPolicy)
     */
    public void setContentSpacePolicy(XmlViolationPolicy contentSpacePolicy) {
        tokenizer.setContentSpacePolicy(contentSpacePolicy);
    }

    /**
     * Whether the HTML 4 mode reports boolean attributes in a way that repeats
     * the name in the value.
     * @param html4ModeCompatibleWithXhtml1Schemata
     */
    public void setHtml4ModeCompatibleWithXhtml1Schemata(
            boolean html4ModeCompatibleWithXhtml1Schemata) {
        tokenizer.setHtml4ModeCompatibleWithXhtml1Schemata(html4ModeCompatibleWithXhtml1Schemata);
    }

    /**
     * Whether to map the HTML <code>lang</code> attribute to <code>xml:lang</code>.
     * @param mappingLangToXmlLang <code>true</code> to map <code>lang</code> to <code>xml:lang</code>
     * @see nu.validator.htmlparser.impl.Tokenizer#setMappingLangToXmlLang(boolean)
     */
    public void setMappingLangToXmlLang(boolean mappingLangToXmlLang) {
        tokenizer.setMappingLangToXmlLang(mappingLangToXmlLang);
    }

    /**
     * Sets the policy for dealing with names that aren't XML 1.0 4th ed. plus Namespaces NCNames.
     * @param namePolicy the policy
     * @see nu.validator.htmlparser.impl.Tokenizer#setNamePolicy(nu.validator.htmlparser.common.XmlViolationPolicy)
     */
    public void setNamePolicy(XmlViolationPolicy namePolicy) {
        tokenizer.setNamePolicy(namePolicy);
        domTreeBuilder.setNamePolicy(namePolicy);
    }

    /**
     * This is a catch-all convenience method for setting name, content space,
     * content non-XML char and comment policies in one go.
     * 
     * @param namePolicy the policy
     */
    public void setXmlPolicy(XmlViolationPolicy xmlPolicy) {
        setNamePolicy(xmlPolicy);
        setContentSpacePolicy(xmlPolicy);
        setContentNonXmlCharPolicy(xmlPolicy);
        setCommentPolicy(xmlPolicy);
    }

    /**
     * Does nothing.
     * @deprecated
     */
    public void setBogusXmlnsPolicy(XmlViolationPolicy bogusXmlnsPolicy) {
    }

    /**
     * Sets the doctype expectation.
     * 
     * @param doctypeExpectation
     *            the doctypeExpectation to set
     * @see nu.validator.htmlparser.impl.TreeBuilder#setDoctypeExpectation(nu.validator.htmlparser.common.DoctypeExpectation)
     */
    public void setDoctypeExpectation(DoctypeExpectation doctypeExpectation) {
        domTreeBuilder.setDoctypeExpectation(doctypeExpectation);
    }

    /**
     * Sets the document mode handler.
     * 
     * @param documentModeHandler
     * @see nu.validator.htmlparser.impl.TreeBuilder#setDocumentModeHandler(nu.validator.htmlparser.common.DocumentModeHandler)
     */
    public void setDocumentModeHandler(DocumentModeHandler documentModeHandler) {
        domTreeBuilder.setDocumentModeHandler(documentModeHandler);
    }

    /**
     * Sets the encoding sniffing heuristics.
     * 
     * @param heuristics the heuristics to set
     * @see nu.validator.htmlparser.impl.Tokenizer#setHeuristics(nu.validator.htmlparser.common.Heuristics)
     */
    public void setHeuristics(Heuristics heuristics) {
        tokenizer.setHeuristics(heuristics);
    }

    /**
     * Tokenizes the input source.
     * 
     * @param is the source
     * @throws SAXException if stuff goes wrong
     * @throws IOException if IO goes wrong
     * @throws MalformedURLException if the system ID is malformed and the entity resolver is <code>null</code>
     */
    private void tokenize(InputSource is) throws SAXException, IOException,
            MalformedURLException {
        if (is == null) {
            throw new IllegalArgumentException("Null input.");
        }
        if (is.getByteStream() == null && is.getCharacterStream() == null) {
            String systemId = is.getSystemId();
            if (systemId == null) {
                throw new IllegalArgumentException(
                        "No byte stream, no character stream nor URI.");
            }
            if (entityResolver != null) {
                is = entityResolver.resolveEntity(is.getPublicId(), systemId);
            }
            if (is.getByteStream() == null || is.getCharacterStream() == null) {
                is = new InputSource();
                is.setSystemId(systemId);
                is.setByteStream(new URL(systemId).openStream());
            }
        }
        tokenizer.tokenize(is);
    }

}
