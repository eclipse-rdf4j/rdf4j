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

import static org.eclipse.rdf4j.query.resultio.sparqlxml.SPARQLResultsXMLConstants.BINDING_NAME_ATT;
import static org.eclipse.rdf4j.query.resultio.sparqlxml.SPARQLResultsXMLConstants.BINDING_TAG;
import static org.eclipse.rdf4j.query.resultio.sparqlxml.SPARQLResultsXMLConstants.BNODE_TAG;
import static org.eclipse.rdf4j.query.resultio.sparqlxml.SPARQLResultsXMLConstants.BOOLEAN_TAG;
import static org.eclipse.rdf4j.query.resultio.sparqlxml.SPARQLResultsXMLConstants.LITERAL_DATATYPE_ATT;
import static org.eclipse.rdf4j.query.resultio.sparqlxml.SPARQLResultsXMLConstants.LITERAL_LANG_ATT;
import static org.eclipse.rdf4j.query.resultio.sparqlxml.SPARQLResultsXMLConstants.LITERAL_TAG;
import static org.eclipse.rdf4j.query.resultio.sparqlxml.SPARQLResultsXMLConstants.OBJECT_TAG;
import static org.eclipse.rdf4j.query.resultio.sparqlxml.SPARQLResultsXMLConstants.O_TAG;
import static org.eclipse.rdf4j.query.resultio.sparqlxml.SPARQLResultsXMLConstants.PREDICATE_TAG;
import static org.eclipse.rdf4j.query.resultio.sparqlxml.SPARQLResultsXMLConstants.P_TAG;
import static org.eclipse.rdf4j.query.resultio.sparqlxml.SPARQLResultsXMLConstants.RESULT_SET_TAG;
import static org.eclipse.rdf4j.query.resultio.sparqlxml.SPARQLResultsXMLConstants.RESULT_TAG;
import static org.eclipse.rdf4j.query.resultio.sparqlxml.SPARQLResultsXMLConstants.STATEMENT_TAG;
import static org.eclipse.rdf4j.query.resultio.sparqlxml.SPARQLResultsXMLConstants.SUBJECT_TAG;
import static org.eclipse.rdf4j.query.resultio.sparqlxml.SPARQLResultsXMLConstants.S_TAG;
import static org.eclipse.rdf4j.query.resultio.sparqlxml.SPARQLResultsXMLConstants.TRIPLE_TAG;
import static org.eclipse.rdf4j.query.resultio.sparqlxml.SPARQLResultsXMLConstants.URI_TAG;
import static org.eclipse.rdf4j.query.resultio.sparqlxml.SPARQLResultsXMLConstants.VAR_NAME_ATT;
import static org.eclipse.rdf4j.query.resultio.sparqlxml.SPARQLResultsXMLConstants.VAR_TAG;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.rdf4j.common.xml.SimpleSAXAdapter;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.query.QueryResultHandler;
import org.eclipse.rdf4j.query.TupleQueryResultHandlerException;
import org.eclipse.rdf4j.query.impl.MapBindingSet;
import org.eclipse.rdf4j.query.resultio.QueryResultParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

class SPARQLResultsSAXParser extends SimpleSAXAdapter {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

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

	private final ValueFactory valueFactory;

	private final QueryResultHandler handler;

	/**
	 * stack for handling nested RDF-star triples
	 */
	private final Deque<TripleContainer> tripleStack = new ArrayDeque<>();

	public SPARQLResultsSAXParser(ValueFactory valueFactory, QueryResultHandler handler) {
		this.valueFactory = valueFactory;
		this.handler = handler;
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	public void startDocument() throws SAXException {
		bindingNames = new ArrayList<>();
		currentValue = null;
	}

	@Override
	public void endDocument() throws SAXException {
		try {
			if (handler != null) {
				handler.endQueryResult();
			}
		} catch (TupleQueryResultHandlerException e) {
			throw new SAXException(e);
		}
	}

	@Override
	public void startTag(String tagName, Map<String, String> atts, String text) throws SAXException {
		if (BINDING_TAG.equals(tagName)) {
			currentBindingName = atts.get(BINDING_NAME_ATT);

			if (currentBindingName == null) {
				throw new SAXException(BINDING_NAME_ATT + " attribute missing for " + BINDING_TAG + " element");
			}
		} else if (TRIPLE_TAG.equals(tagName) || STATEMENT_TAG.equals(tagName)) {
			tripleStack.push(new TripleContainer());
		} else if (URI_TAG.equals(tagName)) {
			try {
				currentValue = valueFactory.createIRI(text);
			} catch (IllegalArgumentException e) {
				// Malformed URI
				throw new SAXException(e.getMessage(), e);
			}
		} else if (BNODE_TAG.equals(tagName)) {
			currentValue = valueFactory.createBNode(text);
		} else if (LITERAL_TAG.equals(tagName)) {
			String xmlLang = atts.get(LITERAL_LANG_ATT);
			String datatype = atts.get(LITERAL_DATATYPE_ATT);

			if (xmlLang != null) {
				currentValue = valueFactory.createLiteral(text, xmlLang);
			} else if (datatype != null) {
				IRI datatypeIri;
				try {
					datatypeIri = valueFactory.createIRI(datatype);
				} catch (IllegalArgumentException e) {
					// Illegal datatype URI
					throw new SAXException(e.getMessage(), e);
				}

				// For broken SPARQL endpoints which return LANGSTRING without a language, fall back
				// to using STRING as the datatype
				if (RDF.LANGSTRING.equals(datatypeIri) && xmlLang == null) {
					logger.debug(
							"rdf:langString typed literal missing language tag: '{}'. Falling back to xsd:string.",
							StringUtils.abbreviate(text, 10)
					);
					datatypeIri = XSD.STRING;
				}

				currentValue = valueFactory.createLiteral(text, datatypeIri);
			} else {
				currentValue = valueFactory.createLiteral(text);
			}
		} else if (RESULT_TAG.equals(tagName)) {
			currentSolution = new MapBindingSet(bindingNames.size());
		} else if (VAR_TAG.equals(tagName)) {
			String varName = atts.get(VAR_NAME_ATT);

			if (varName == null) {
				throw new SAXException(VAR_NAME_ATT + " missing for " + VAR_TAG + " element");
			}

			bindingNames.add(varName);
		} else if (RESULT_SET_TAG.equals(tagName)) {
			try {
				if (handler != null) {
					handler.startQueryResult(bindingNames);
				}
			} catch (TupleQueryResultHandlerException e) {
				throw new SAXException(e);
			}
		} else if (BOOLEAN_TAG.equals(tagName)) {
			QueryResultParseException realException = new QueryResultParseException(
					"Found boolean results in tuple parser");
			throw new SAXException(realException);
		}
	}

	@Override
	public void endTag(String tagName) throws SAXException {
		TripleContainer currentTriple;
		switch (tagName) {
		case BINDING_TAG:
			if (currentValue == null) {
				throw new SAXException("Value missing for " + BINDING_TAG + " element");
			}
			currentSolution.addBinding(currentBindingName, currentValue);
			currentBindingName = null;
			currentValue = null;
			break;
		case SUBJECT_TAG:
		case S_TAG:
			currentTriple = tripleStack.peek();
			if (currentTriple.getSubject() != null) {
				throw new SAXException("RDF-star triple subject defined twice");
			}
			if (currentValue instanceof Resource) {
				currentTriple.setSubject((Resource) currentValue);
			} else {
				throw new SAXException("unexpected value type for subject: " + currentValue);
			}
			break;
		case PREDICATE_TAG:
		case P_TAG:
			currentTriple = tripleStack.peek();
			if (currentTriple.getPredicate() != null) {
				throw new SAXException("RDF-star triple predicate defined twice");
			}
			if (currentValue instanceof IRI) {
				currentTriple.setPredicate((IRI) currentValue);
			} else {
				throw new SAXException("unexpected value type for predicate: " + currentValue);
			}
			break;
		case OBJECT_TAG:
		case O_TAG:
			currentTriple = tripleStack.peek();
			if (currentTriple.getObject() != null) {
				throw new SAXException("RDF-star triple object defined twice");
			}
			currentTriple.setObject(currentValue);
			break;
		case TRIPLE_TAG:
		case STATEMENT_TAG:
			currentTriple = tripleStack.pop();
			currentValue = valueFactory.createTriple(currentTriple.getSubject(), currentTriple.getPredicate(),
					currentTriple.getObject());
			break;
		case RESULT_TAG:
			try {
				if (handler != null) {
					handler.handleSolution(currentSolution);
				}
				currentSolution = null;
			} catch (TupleQueryResultHandlerException e) {
				throw new SAXException(e);
			}
			break;
		}
	}

	private static class TripleContainer {

		private Resource subject;
		private IRI predicate;
		private Value object;

		/**
		 * @return the subject
		 */
		public Resource getSubject() {
			return subject;
		}

		/**
		 * @param subject the subject to set
		 */
		public void setSubject(Resource subject) {
			this.subject = subject;
		}

		/**
		 * @return the predicate
		 */
		public IRI getPredicate() {
			return predicate;
		}

		/**
		 * @param predicate the predicate to set
		 */
		public void setPredicate(IRI predicate) {
			this.predicate = predicate;
		}

		/**
		 * @return the object
		 */
		public Value getObject() {
			return object;
		}

		/**
		 * @param object the object to set
		 */
		public void setObject(Value object) {
			this.object = object;
		}

	}

}
