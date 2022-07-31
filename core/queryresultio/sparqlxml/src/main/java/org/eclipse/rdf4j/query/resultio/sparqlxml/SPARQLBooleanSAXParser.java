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
package org.eclipse.rdf4j.query.resultio.sparqlxml;

import static org.eclipse.rdf4j.query.resultio.sparqlxml.SPARQLResultsXMLConstants.BOOLEAN_FALSE;
import static org.eclipse.rdf4j.query.resultio.sparqlxml.SPARQLResultsXMLConstants.BOOLEAN_TAG;
import static org.eclipse.rdf4j.query.resultio.sparqlxml.SPARQLResultsXMLConstants.BOOLEAN_TRUE;
import static org.eclipse.rdf4j.query.resultio.sparqlxml.SPARQLResultsXMLConstants.RESULT_SET_TAG;
import static org.eclipse.rdf4j.query.resultio.sparqlxml.SPARQLResultsXMLConstants.RESULT_TAG;

import java.util.Map;

import org.eclipse.rdf4j.common.xml.SimpleSAXAdapter;
import org.eclipse.rdf4j.query.resultio.QueryResultParseException;
import org.xml.sax.SAXException;

class SPARQLBooleanSAXParser extends SimpleSAXAdapter {

	/*-----------*
	 * Variables *
	 *-----------*/

	private Boolean value;

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	public void startTag(String tagName, Map<String, String> atts, String text) throws SAXException {
		if (BOOLEAN_TAG.equals(tagName)) {
			if (BOOLEAN_TRUE.equalsIgnoreCase(text)) {
				value = true;
			} else if (BOOLEAN_FALSE.equalsIgnoreCase(text)) {
				value = false;
			} else {
				throw new SAXException("Illegal value for element " + BOOLEAN_TAG + ": " + text);
			}
		} else if (RESULT_SET_TAG.equals(tagName) || RESULT_TAG.equals(tagName)) {
			QueryResultParseException realException = new QueryResultParseException(
					"Found tuple results in boolean parser");
			throw new SAXException(realException);
		}
	}

	@Override
	public void endDocument() throws SAXException {
		if (value == null) {
			throw new SAXException("Malformed document, " + BOOLEAN_TAG + " element not found");
		}
	}

	public boolean getValue() {
		return value != null && value;
	}
}
