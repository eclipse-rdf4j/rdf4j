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
 * An implementation of <var>SimpleSAXListener</var> providing dummy implementations for all its methods.
 */
public class SimpleSAXAdapter implements SimpleSAXListener {

	// implements SimpleSAXListener.startDocument()
	@Override
	public void startDocument() throws SAXException {
	}

	// implements SimpleSAXListener.endDocument()
	@Override
	public void endDocument() throws SAXException {
	}

	// implements SimpleSAXListener.startTag()
	@Override
	public void startTag(String tagName, Map<String, String> atts, String text) throws SAXException {
	}

	// implements SimpleSAXListener.endTag()
	@Override
	public void endTag(String tagName) throws SAXException {
	}
}
