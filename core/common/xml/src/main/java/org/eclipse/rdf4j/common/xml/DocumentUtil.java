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

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.validation.Schema;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

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
	 * Create a Document representing the XML file at the specified location.
	 *
	 * @param location       the location of an XML document
	 * @param validating     whether the XML parser used in the construction of the document should validate the XML
	 * @param namespaceAware whether the XML parser used in the construction of the document should be aware of
	 *                       namespaces
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
		Document result;

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(validating);
		factory.setNamespaceAware(namespaceAware);
		factory.setSchema(schema);

		try (InputStream in = new BufferedInputStream(location.openConnection().getInputStream())) {
			DocumentBuilder builder = factory.newDocumentBuilder();
			result = builder.parse(in);
		} catch (SAXParseException e) {
			String message = "Parsing error" + ", line " + e.getLineNumber() + ", uri " + e.getSystemId() + ", "
					+ e.getMessage();
			throw toIOE(message, e);
		} catch (SAXException | ParserConfigurationException e) {
			throw toIOE(e);
		}

		return result;
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
