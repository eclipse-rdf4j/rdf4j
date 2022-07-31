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

import java.util.Map;

import org.xml.sax.SAXException;

/**
 * A listener for events reported by <var>SimpleSAXParser</var>.
 */
public interface SimpleSAXListener {

	/**
	 * Notifies the listener that the parser has started parsing.
	 */
	void startDocument() throws SAXException;

	/**
	 * Notifies the listener that the parser has finished parsing.
	 */
	void endDocument() throws SAXException;

	/**
	 * Reports a start tag to the listener. The method call reports the tag's name, the attributes that were found in
	 * the start tag and any text that was found after the start tag.
	 *
	 * @param tagName The tag name.
	 * @param atts    A map containing key-value-pairs representing the attributes that were found in the start tag.
	 * @param text    The text immediately following the start tag, or an empty string if the start tag was followed by
	 *                a nested start tag or if no text (other than whitespace) was found between start- and end tag.
	 */
	void startTag(String tagName, Map<String, String> atts, String text) throws SAXException;

	/**
	 * Reports an end tag to the listener.
	 *
	 * @param tagName The tag name.
	 */
	void endTag(String tagName) throws SAXException;
}
