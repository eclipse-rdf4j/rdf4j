/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.resultio.sparqlxml;

import static org.eclipse.rdf4j.query.resultio.sparqlxml.SPARQLResultsXMLConstants.BINDING_NAME_ATT;
import static org.eclipse.rdf4j.query.resultio.sparqlxml.SPARQLResultsXMLConstants.BINDING_TAG;
import static org.eclipse.rdf4j.query.resultio.sparqlxml.SPARQLResultsXMLConstants.BNODE_TAG;
import static org.eclipse.rdf4j.query.resultio.sparqlxml.SPARQLResultsXMLConstants.BOOLEAN_TAG;
import static org.eclipse.rdf4j.query.resultio.sparqlxml.SPARQLResultsXMLConstants.LITERAL_DATATYPE_ATT;
import static org.eclipse.rdf4j.query.resultio.sparqlxml.SPARQLResultsXMLConstants.LITERAL_LANG_ATT;
import static org.eclipse.rdf4j.query.resultio.sparqlxml.SPARQLResultsXMLConstants.LITERAL_TAG;
import static org.eclipse.rdf4j.query.resultio.sparqlxml.SPARQLResultsXMLConstants.RESULT_SET_TAG;
import static org.eclipse.rdf4j.query.resultio.sparqlxml.SPARQLResultsXMLConstants.RESULT_TAG;
import static org.eclipse.rdf4j.query.resultio.sparqlxml.SPARQLResultsXMLConstants.URI_TAG;
import static org.eclipse.rdf4j.query.resultio.sparqlxml.SPARQLResultsXMLConstants.VAR_NAME_ATT;
import static org.eclipse.rdf4j.query.resultio.sparqlxml.SPARQLResultsXMLConstants.VAR_TAG;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.rdf4j.common.xml.SimpleSAXAdapter;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.QueryResultHandler;
import org.eclipse.rdf4j.query.TupleQueryResultHandlerException;
import org.eclipse.rdf4j.query.impl.MapBindingSet;
import org.eclipse.rdf4j.query.resultio.QueryResultParseException;
import org.xml.sax.SAXException;

class SPARQLResultsSAXParser extends SimpleSAXAdapter {

	/*-----------*
	 * Variables *
	 *-----------*/

	/**
	 * The variable names that are specified in the header.
	 */
	private List<String> bindingNames;

	/**
	 * The most recently parsed binding name.
	 */
	private String currentBindingName;

	/**
	 * The most recently parsed value.
	 */
	private Value currentValue;

	/**
	 * The bound variables for the current result.
	 */
	private MapBindingSet currentSolution;

	private ValueFactory valueFactory;

	private QueryResultHandler handler;

	public SPARQLResultsSAXParser(ValueFactory valueFactory, QueryResultHandler handler) {
		this.valueFactory = valueFactory;
		this.handler = handler;
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	public void startDocument()
		throws SAXException
	{
		bindingNames = new ArrayList<String>();
		currentValue = null;
	}

	@Override
	public void endDocument()
		throws SAXException
	{
		try {
			if (handler != null) {
				handler.endQueryResult();
			}
		}
		catch (TupleQueryResultHandlerException e) {
			throw new SAXException(e);
		}
	}

	@Override
	public void startTag(String tagName, Map<String, String> atts, String text)
		throws SAXException
	{
		if (BINDING_TAG.equals(tagName)) {
			currentBindingName = atts.get(BINDING_NAME_ATT);

			if (currentBindingName == null) {
				throw new SAXException(BINDING_NAME_ATT + " attribute missing for " + BINDING_TAG + " element");
			}
		}
		else if (URI_TAG.equals(tagName)) {
			try {
				currentValue = valueFactory.createIRI(text);
			}
			catch (IllegalArgumentException e) {
				// Malformed URI
				throw new SAXException(e.getMessage(), e);
			}
		}
		else if (BNODE_TAG.equals(tagName)) {
			currentValue = valueFactory.createBNode(text);
		}
		else if (LITERAL_TAG.equals(tagName)) {
			String xmlLang = atts.get(LITERAL_LANG_ATT);
			String datatype = atts.get(LITERAL_DATATYPE_ATT);

			if (xmlLang != null) {
				currentValue = valueFactory.createLiteral(text, xmlLang);
			}
			else if (datatype != null) {
				try {
					currentValue = valueFactory.createLiteral(text, valueFactory.createIRI(datatype));
				}
				catch (IllegalArgumentException e) {
					// Illegal datatype URI
					throw new SAXException(e.getMessage(), e);
				}
			}
			else {
				currentValue = valueFactory.createLiteral(text);
			}
		}
		else if (RESULT_TAG.equals(tagName)) {
			currentSolution = new MapBindingSet(bindingNames.size());
		}
		else if (VAR_TAG.equals(tagName)) {
			String varName = atts.get(VAR_NAME_ATT);

			if (varName == null) {
				throw new SAXException(VAR_NAME_ATT + " missing for " + VAR_TAG + " element");
			}

			bindingNames.add(varName);
		}
		else if (RESULT_SET_TAG.equals(tagName)) {
			try {
				if (handler != null) {
					handler.startQueryResult(bindingNames);
				}
			}
			catch (TupleQueryResultHandlerException e) {
				throw new SAXException(e);
			}
		}
		else if (BOOLEAN_TAG.equals(tagName)) {
			QueryResultParseException realException = new QueryResultParseException(
					"Found boolean results in tuple parser");
			throw new SAXException(realException);
		}
	}

	@Override
	public void endTag(String tagName)
		throws SAXException
	{
		if (BINDING_TAG.equals(tagName)) {
			if (currentValue == null) {
				throw new SAXException("Value missing for " + BINDING_TAG + " element");
			}

			currentSolution.addBinding(currentBindingName, currentValue);

			currentBindingName = null;
			currentValue = null;
		}
		else if (RESULT_TAG.equals(tagName)) {
			try {
				if (handler != null) {
					handler.handleSolution(currentSolution);
				}
				currentSolution = null;
			}
			catch (TupleQueryResultHandlerException e) {
				throw new SAXException(e);
			}
		}
	}
}