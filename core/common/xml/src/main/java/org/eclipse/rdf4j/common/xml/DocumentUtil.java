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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.xml.XMLConstants;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.validation.Schema;
import javax.xml.validation.ValidatorHandler;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;

/**
 * Utilities to make working with DOM documents easier.
 *
 * @author Herko ter Horst
 */
public class DocumentUtil {

	/**
	 * Create a Document representing the XML file at the specified location.
	 *
	 * @param location the location of an XML document
	 * @return a Document representing the XML file
	 * @throws IOException when there was a problem retrieving or parsing the document.
	 */
	public static Document getDocument(URL location) throws IOException {
		return getDocument(location, false, false, null);
	}

	/**
	 * Create a Document representing the XML data from the supplied stream.
	 *
	 * @param inputStream an XML input stream. The caller remains responsible for closing it.
	 * @return a Document representing the XML data
	 * @throws IOException when there was a problem parsing the document.
	 */
	public static Document getDocument(InputStream inputStream) throws IOException {
		return getDocument(inputStream, null, null);
	}

	/**
	 * Create a Document representing the XML file at the specified location.
	 *
	 * @param location       the location of an XML document
	 * @param validating     retained for compatibility; validation is controlled by the supplied {@link Schema}
	 * @param namespaceAware retained for compatibility; XML readers created by {@link XMLReaderFactory} are namespace
	 *                       aware
	 * @return a Document representing the XML file
	 * @throws IOException when there was a problem retrieving or parsing the document.
	 */
	public static Document getDocument(URL location, boolean validating, boolean namespaceAware) throws IOException {
		return getDocument(location, validating, namespaceAware, null);
	}

	/**
	 * Create a Document representing the XML file at the specified location.
	 *
	 * @param location the location of an XML document
	 * @param schema   a Schama instance to validate against
	 * @return a Document representing the XML file
	 * @throws IOException when there was a problem retrieving or parsing the document.
	 */
	public static Document getDocument(URL location, Schema schema) throws IOException {
		return getDocument(location, false, true, schema);
	}

	private static Document getDocument(URL location, boolean validating, boolean namespaceAware, Schema schema)
			throws IOException {
		try (InputStream in = new BufferedInputStream(location.openConnection().getInputStream())) {
			return getDocument(in, location.toExternalForm(), schema);
		}
	}

	private static Document getDocument(InputStream inputStream, String systemId, Schema schema) throws IOException {
		try {
			DOMResult domResult = new DOMResult();
			ContentHandler contentHandler = createDomContentHandler(domResult);
			if (schema != null) {
				ValidatorHandler validatorHandler = schema.newValidatorHandler();
				validatorHandler.setContentHandler(contentHandler);
				contentHandler = validatorHandler;
			}

			XMLReader reader = XMLReaderFactory.createXMLReader();
			reader.setContentHandler(contentHandler);

			InputSource inputSource = new InputSource(inputStream);
			inputSource.setSystemId(systemId);
			reader.parse(inputSource);

			Node node = domResult.getNode();
			if (node instanceof Document) {
				return (Document) node;
			}

			throw new IOException("XML parser did not produce a DOM Document");
		} catch (SAXParseException e) {
			String message = "Parsing error" + ", line " + e.getLineNumber() + ", uri " + e.getSystemId() + ", "
					+ e.getMessage();
			throw toIOE(message, e);
		} catch (SAXException | TransformerConfigurationException e) {
			throw toIOE(e);
		}
	}

	private static ContentHandler createDomContentHandler(DOMResult domResult)
			throws TransformerConfigurationException {
		SAXTransformerFactory transformerFactory = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
		transformerFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
		setTransformerAttribute(transformerFactory, XMLConstants.ACCESS_EXTERNAL_DTD, "");
		setTransformerAttribute(transformerFactory, XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
		TransformerHandler transformerHandler = transformerFactory.newTransformerHandler();
		transformerHandler.setResult(domResult);
		return transformerHandler;
	}

	private static void setTransformerAttribute(SAXTransformerFactory transformerFactory, String name, String value) {
		try {
			transformerFactory.setAttribute(name, value);
		} catch (IllegalArgumentException ignored) {
			// Optional JAXP 1.5 hardening attribute; older transformer implementations may not support it.
		}
	}

	private static IOException toIOE(Exception e) {
		return toIOE(e.getMessage(), e);
	}

	private static IOException toIOE(String message, Exception e) {
		IOException result = new IOException(message);
		result.initCause(e);
		return result;
	}
}
