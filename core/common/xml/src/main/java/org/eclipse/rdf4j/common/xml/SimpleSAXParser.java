/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.common.xml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 * An XML parser that generates "simple" SAX-like events from a limited subset of XML documents. The SimpleSAXParser can
 * parse simple XML documents; it doesn't support processing instructions or elements that contain both sub-element and
 * character data; character data is only supported in the "leaves" of the XML element tree.
 * <h3>Example:</h3>
 * <p>
 * Parsing the following XML:
 *
 * <pre>
 * &lt;?xml version='1.0' encoding='UTF-8'?&gt;
 * &lt;xml-doc&gt;
 *   &lt;foo a="1" b="2&amp;amp;3"/&gt;
 *   &lt;bar&gt;Hello World!&lt;/bar&gt;
 * &lt;/xml-doc&gt;
 * </pre>
 * <p>
 * will result in the following method calls to the <var>SimpleSAXListener</var>:
 *
 * <pre>
 * startDocument()
 * startTag("xml-doc", emptyMap, "")
 *
 * startTag("foo", a_b_Map, "")
 * endTag("foo")
 *
 * startTag("bar", emptyMap, "Hello World!")
 * endTag("bar")
 *
 * endTag("xml-doc")
 * endDocument()
 * </pre>
 */
public class SimpleSAXParser {

	/*-----------*
	 * Variables *
	 *-----------*/
	/**
	 * The XMLReader to use for parsing the XML.
	 */
	private final XMLReader xmlReader;

	/**
	 * The listener to report the events to.
	 */
	private SimpleSAXListener listener;

	/**
	 * Flag indicating whether leading and trailing whitespace in text elements should be preserved.
	 */
	private boolean preserveWhitespace = false;

	/**
	 * A Locator indicating a position in the text that is currently being parsed by the SAX parser.
	 */
	private Locator locator;

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new SimpleSAXParser that will use the supplied <var>XMLReader</var> for parsing the XML. One must set a
	 * <var>SimpleSAXListener</var> on this object before calling one of the <var>parse()</var> methods.
	 *
	 * @param xmlReader The XMLReader to use for parsing.
	 * @see #setListener
	 */
	public SimpleSAXParser(XMLReader xmlReader) {
		super();
		this.xmlReader = xmlReader;
	}

	/**
	 * Creates a new SimpleSAXParser that will try to create a new <var>XMLReader</var> using
	 * <var>info.aduna.xml.XMLReaderFactory</var> for parsing the XML. One must set a <var>SimpleSAXListener</var> on
	 * this object before calling one of the <var>parse()</var> methods.
	 *
	 * @throws SAXException If the SimpleSAXParser was unable to create an XMLReader.
	 * @see #setListener
	 * @see org.xml.sax.XMLReader
	 * @see org.eclipse.rdf4j.common.xml.XMLReaderFactory
	 */
	public SimpleSAXParser() throws SAXException {
		this(XMLReaderFactory.createXMLReader());
	}

	/*---------*
	 * Methods *
	 *---------*/

	/**
	 * Sets the (new) listener that should receive any events from this parser. This listener will replace any
	 * previously set listener.
	 *
	 * @param listener The (new) listener for events from this parser.
	 */
	public void setListener(SimpleSAXListener listener) {
		this.listener = listener;
	}

	/**
	 * Gets the listener that currently will receive any events from this parser.
	 *
	 * @return The listener for events from this parser.
	 */
	public SimpleSAXListener getListener() {
		return listener;
	}

	public Locator getLocator() {
		return locator;
	}

	/**
	 * Sets whether leading and trailing whitespace characters in text elements should be preserved. Such whitespace
	 * characters are discarded by default.
	 */
	public void setPreserveWhitespace(boolean preserveWhitespace) {
		this.preserveWhitespace = preserveWhitespace;
	}

	/**
	 * Checks whether leading and trailing whitespace characters in text elements are preserved. Defaults to
	 * <var>false</var>.
	 */
	public boolean isPreserveWhitespace() {
		return preserveWhitespace;
	}

	/**
	 * Parses the content of the supplied <var>File</var> as XML.
	 *
	 * @param file The file containing the XML to parse.
	 */
	public void parse(File file) throws SAXException, IOException {
		try (InputStream in = new FileInputStream(file)) {
			parse(in);
		}
	}

	/**
	 * Parses the content of the supplied <var>InputStream</var> as XML.
	 *
	 * @param in An <var>InputStream</var> containing XML data.
	 */
	public void parse(InputStream in) throws SAXException, IOException {
		parse(new InputSource(in));
	}

	/**
	 * Parses the content of the supplied <var>Reader</var> as XML.
	 *
	 * @param reader A <var>Reader</var> containing XML data.
	 */
	public void parse(Reader reader) throws SAXException, IOException {
		parse(new InputSource(reader));
	}

	/**
	 * Parses the content of the supplied <var>InputSource</var> as XML.
	 *
	 * @param inputSource An <var>InputSource</var> containing XML data.
	 */
	public synchronized void parse(InputSource inputSource) throws SAXException, IOException {
		xmlReader.setContentHandler(new SimpleSAXDefaultHandler());
		xmlReader.parse(inputSource);
	}

	/*-------------------------------------*
	 * Inner class SimpleSAXDefaultHandler *
	 *-------------------------------------*/
	class SimpleSAXDefaultHandler extends DefaultHandler {

		/*-----------*
		 * Variables *
		 *-----------*/
		/**
		 * StringBuilder used to collect text during parsing.
		 */
		private final StringBuilder charBuf = new StringBuilder(512);

		/**
		 * The tag name of a deferred start tag.
		 */
		private String deferredStartTag = null;

		/**
		 * The attributes of a deferred start tag.
		 */
		private Map<String, String> deferredAttributes = null;

		/*--------------*
		 * Constructors *
		 *--------------*/
		public SimpleSAXDefaultHandler() {
			super();
		}

		/*---------*
		 * Methods *
		 *---------*/
		// overrides DefaultHandler.startDocument()
		@Override
		public void startDocument() throws SAXException {
			listener.startDocument();
		}

		// overrides DefaultHandler.endDocument()
		@Override
		public void endDocument() throws SAXException {
			listener.endDocument();
		}

		// overrides DefaultHandler.characters()
		@Override
		public void characters(char[] ch, int start, int length) throws SAXException {
			charBuf.append(ch, start, length);
		}

		// overrides DefaultHandler.startElement()
		@Override
		public void startElement(String namespaceURI, String localName, String qName, Attributes attributes)
				throws SAXException {
			// Report any deferred start tag
			if (deferredStartTag != null) {
				reportDeferredStartElement();
			}

			// Make current tag new deferred start tag
			deferredStartTag = localName;

			// Copy attributes to deferredAttributes
			int attCount = attributes.getLength();
			if (attCount == 0) {
				deferredAttributes = Collections.emptyMap();
			} else {
				deferredAttributes = new LinkedHashMap<>(attCount * 2);

				for (int i = 0; i < attCount; i++) {
					deferredAttributes.put(attributes.getQName(i), attributes.getValue(i));
				}
			}

			// Clear character buffer
			charBuf.setLength(0);
		}

		private void reportDeferredStartElement() throws SAXException {
			listener.startTag(deferredStartTag, deferredAttributes, "");
			deferredStartTag = null;
			deferredAttributes = null;
		}

		// overrides DefaultHandler.endElement()
		@Override
		public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
			if (deferredStartTag != null) {
				// Check if any character data has been collected in the charBuf
				String text = charBuf.toString();
				if (!preserveWhitespace) {
					text = text.trim();
				}

				// Report deferred start tag
				listener.startTag(deferredStartTag, deferredAttributes, text);
				deferredStartTag = null;
				deferredAttributes = null;
			}

			// Report the end tag
			listener.endTag(localName);

			// Clear character buffer
			charBuf.setLength(0);
		}

		@Override
		public void setDocumentLocator(Locator loc) {
			locator = loc;
		}
	}
}
