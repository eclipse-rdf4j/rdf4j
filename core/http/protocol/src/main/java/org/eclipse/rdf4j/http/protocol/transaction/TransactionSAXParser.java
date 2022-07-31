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
package org.eclipse.rdf4j.http.protocol.transaction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.eclipse.rdf4j.common.xml.SimpleSAXAdapter;
import org.eclipse.rdf4j.http.protocol.transaction.operations.AddStatementOperation;
import org.eclipse.rdf4j.http.protocol.transaction.operations.ClearNamespacesOperation;
import org.eclipse.rdf4j.http.protocol.transaction.operations.ClearOperation;
import org.eclipse.rdf4j.http.protocol.transaction.operations.RemoveNamespaceOperation;
import org.eclipse.rdf4j.http.protocol.transaction.operations.RemoveStatementsOperation;
import org.eclipse.rdf4j.http.protocol.transaction.operations.SPARQLUpdateOperation;
import org.eclipse.rdf4j.http.protocol.transaction.operations.SetNamespaceOperation;
import org.eclipse.rdf4j.http.protocol.transaction.operations.TransactionOperation;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.impl.SimpleBinding;
import org.eclipse.rdf4j.query.impl.SimpleDataset;
import org.eclipse.rdf4j.rio.helpers.RDFStarUtil;
import org.xml.sax.SAXException;

/**
 * Parses an RDF transaction document into a collection of {@link TransactionOperation} objects.
 *
 * @author Arjohn Kampman
 * @author Leo Sauermann
 */
class TransactionSAXParser extends SimpleSAXAdapter {

	private final ValueFactory valueFactory;

	protected Collection<TransactionOperation> txn;

	private final List<Value> parsedValues = new ArrayList<>();

	private List<Binding> bindings;

	private SPARQLUpdateOperation currentSPARQLUpdate = null;

	private SimpleDataset currentDataset;

	public TransactionSAXParser() {
		this(SimpleValueFactory.getInstance());
	}

	public TransactionSAXParser(ValueFactory valueFactory) {
		this.valueFactory = valueFactory;
	}

	/**
	 * get the parsed transaction
	 *
	 * @return the parsed transaction
	 */
	public Collection<TransactionOperation> getTxn() {
		return txn;
	}

	@Override
	public void startDocument() throws SAXException {
		txn = new ArrayList<>();
	}

	@Override
	public void startTag(String tagName, Map<String, String> atts, String text) throws SAXException {
		if (TransactionXMLConstants.TRIPLE_TAG.equals(tagName)) {
			// fixes GH-3048
			parsedValues.add(RDFStarUtil.fromRDFEncodedValue(valueFactory.createIRI(text)));
		} else if (TransactionXMLConstants.URI_TAG.equals(tagName)) {
			parsedValues.add(valueFactory.createIRI(text));
		} else if (TransactionXMLConstants.BNODE_TAG.equals(tagName)) {
			parsedValues.add(valueFactory.createBNode(text));
		} else if (TransactionXMLConstants.LITERAL_TAG.equals(tagName)) {
			String lang = atts.get(TransactionXMLConstants.LANG_ATT);
			String datatype = atts.get(TransactionXMLConstants.DATATYPE_ATT);
			String encoding = atts.get(TransactionXMLConstants.ENCODING_ATT);

			if (encoding != null && "base64".equalsIgnoreCase(encoding)) {
				text = new String(javax.xml.bind.DatatypeConverter.parseBase64Binary(text));
			}
			Literal lit;
			if (lang != null) {
				lit = valueFactory.createLiteral(text, lang);
			} else if (datatype != null) {
				IRI dtURI = valueFactory.createIRI(datatype);
				lit = valueFactory.createLiteral(text, dtURI);
			} else {
				lit = valueFactory.createLiteral(text);
			}

			parsedValues.add(lit);
		} else if (TransactionXMLConstants.NULL_TAG.equals(tagName)) {
			parsedValues.add(null);
		} else if (TransactionXMLConstants.SET_NAMESPACE_TAG.equals(tagName)) {
			String prefix = atts.get(TransactionXMLConstants.PREFIX_ATT);
			String name = atts.get(TransactionXMLConstants.NAME_ATT);
			txn.add(new SetNamespaceOperation(prefix, name));
		} else if (TransactionXMLConstants.REMOVE_NAMESPACE_TAG.equals(tagName)) {
			String prefix = atts.get(TransactionXMLConstants.PREFIX_ATT);
			txn.add(new RemoveNamespaceOperation(prefix));
		} else if (TransactionXMLConstants.CLEAR_NAMESPACES_TAG.equals(tagName)) {
			txn.add(new ClearNamespacesOperation());
		} else if (TransactionXMLConstants.SPARQL_UPDATE_TAG.equals(tagName)) {
			if (currentSPARQLUpdate != null) {
				throw new SAXException("unexpected start of SPARQL Update operation");
			}
			currentSPARQLUpdate = new SPARQLUpdateOperation();

			String baseURI = atts.get(TransactionXMLConstants.BASE_URI_ATT);
			boolean includeInferred = Boolean.parseBoolean(atts.get(TransactionXMLConstants.INCLUDE_INFERRED_ATT));

			currentSPARQLUpdate.setIncludeInferred(includeInferred);
			currentSPARQLUpdate.setBaseURI(baseURI);
		} else if (TransactionXMLConstants.UPDATE_STRING_TAG.equals(tagName)) {
			currentSPARQLUpdate.setUpdateString(text);
		} else if (TransactionXMLConstants.DATASET_TAG.equals(tagName)) {
			currentDataset = new SimpleDataset();
		} else if (TransactionXMLConstants.DEFAULT_INSERT_GRAPH.equals(tagName)) {
			currentDataset.setDefaultInsertGraph(valueFactory.createIRI(text));
		} else if (TransactionXMLConstants.GRAPH_TAG.equals(tagName)) {
			parsedValues.add(valueFactory.createIRI(text));
		} else if (TransactionXMLConstants.BINDINGS.equals(tagName)) {
			if (bindings != null) {
				throw new SAXException("unexpected start of SPARQL Update operation bindings");
			}

			bindings = new ArrayList<>();
		} else if (TransactionXMLConstants.BINDING_URI.equals(tagName)
				|| TransactionXMLConstants.BINDING_BNODE.equals(tagName)
				|| TransactionXMLConstants.BINDING_LITERAL.equals(tagName)) {
			if (bindings == null) {
				throw new SAXException("unexpected start of SPARQL Update operation binding (without <bindings>)");
			}

			String value = text;
			String name = atts.get(TransactionXMLConstants.NAME_ATT);

			if (name != null && value != null) {
				Value v;

				if (TransactionXMLConstants.BINDING_URI.equals(tagName)) {
					v = valueFactory.createIRI(value);
				} else if (TransactionXMLConstants.BINDING_BNODE.equals(tagName)) {
					v = valueFactory.createBNode(value);
				} else {
					String language = atts.get(TransactionXMLConstants.LANGUAGE_ATT);
					String dataType = atts.get(TransactionXMLConstants.DATA_TYPE_ATT);

					if (language != null) {
						v = valueFactory.createLiteral(value, language);
					} else if (dataType != null) {
						v = valueFactory.createLiteral(value, valueFactory.createIRI(dataType));
					} else {
						v = valueFactory.createLiteral(value);
					}
				}
				bindings.add(new SimpleBinding(name, v));
			}
		}
	}

	@Override
	public void endTag(String tagName) throws SAXException {
		if (TransactionXMLConstants.ADD_STATEMENT_TAG.equals(tagName)) {
			txn.add(createAddStatementOperation());
		} else if (TransactionXMLConstants.REMOVE_STATEMENTS_TAG.equals(tagName)) {
			txn.add(createRemoveStatementsOperation());
		} else if (TransactionXMLConstants.CLEAR_TAG.equals(tagName)) {
			txn.add(createClearOperation());
		} else if (TransactionXMLConstants.SPARQL_UPDATE_TAG.equals(tagName)) {
			txn.add(currentSPARQLUpdate);
			currentSPARQLUpdate = null;
		} else if (TransactionXMLConstants.DEFAULT_GRAPHS_TAG.equals(tagName)) {
			for (Value parsedValue : parsedValues) {
				try {
					currentDataset.addDefaultGraph((IRI) parsedValue);
				} catch (ClassCastException e) {
					throw new SAXException("unexpected value in default graph list: " + parsedValue);
				}
			}
			parsedValues.clear();
		} else if (TransactionXMLConstants.NAMED_GRAPHS_TAG.equals(tagName)) {
			for (Value parsedValue : parsedValues) {
				try {
					currentDataset.addNamedGraph((IRI) parsedValue);
				} catch (ClassCastException e) {
					throw new SAXException("unexpected value in named graph list: " + parsedValue);
				}
			}
			parsedValues.clear();
		} else if (TransactionXMLConstants.DEFAULT_REMOVE_GRAPHS_TAG.equals(tagName)) {
			for (Value parsedValue : parsedValues) {
				try {
					currentDataset.addDefaultRemoveGraph((IRI) parsedValue);
				} catch (ClassCastException e) {
					throw new SAXException("unexpected value in default remove graph list: " + parsedValue);
				}
			}
			parsedValues.clear();
		} else if (TransactionXMLConstants.DATASET_TAG.equals(tagName)) {
			currentSPARQLUpdate.setDataset(currentDataset);
			currentDataset = null;
		} else if (TransactionXMLConstants.BINDINGS.equals(tagName)) {
			Binding b[] = bindings.toArray(new Binding[0]);
			currentSPARQLUpdate.setBindings(b);
			bindings.clear();
			bindings = null;
		}
	}

	private TransactionOperation createClearOperation() throws SAXException {
		Resource[] contexts = createContexts(0);
		parsedValues.clear();

		return new ClearOperation(contexts);
	}

	private TransactionOperation createAddStatementOperation() throws SAXException {
		if (parsedValues.size() < 3) {
			throw new SAXException(
					"At least three values required for AddStatementOperation, found: " + parsedValues.size());
		}

		try {
			Resource subject = (Resource) parsedValues.get(0);
			IRI predicate = (IRI) parsedValues.get(1);
			Value object = parsedValues.get(2);
			Resource[] contexts = createContexts(3);

			parsedValues.clear();

			if (subject == null || predicate == null || object == null) {
				throw new SAXException("Subject, predicate and object cannot be null for an AddStatementOperation");
			}
			return new AddStatementOperation(subject, predicate, object, contexts);
		} catch (ClassCastException e) {
			throw new SAXException("Invalid argument(s) for AddStatementOperation", e);
		}
	}

	private TransactionOperation createRemoveStatementsOperation() throws SAXException {
		if (parsedValues.size() < 3) {
			throw new SAXException(
					"At least three values required for RemoveStatementsOperation, found: " + parsedValues.size());
		}

		try {
			Resource subject = (Resource) parsedValues.get(0);
			IRI predicate = (IRI) parsedValues.get(1);
			Value object = parsedValues.get(2);
			Resource[] contexts = createContexts(3);

			parsedValues.clear();

			return new RemoveStatementsOperation(subject, predicate, object, contexts);
		} catch (ClassCastException e) {
			throw new SAXException("Invalid argument(s) for RemoveStatementsOperation", e);
		}
	}

	private Resource[] createContexts(int startIdx) throws SAXException {
		List<Resource> contexts = new ArrayList<>();

		for (int i = startIdx; i < parsedValues.size(); i++) {
			Value contextCandidate = parsedValues.get(i);

			if (contextCandidate == null || contextCandidate instanceof Resource) {
				contexts.add((Resource) contextCandidate);
			} else {
				throw new SAXException("Invalid context value: " + contextCandidate.getClass());
			}
		}

		return contexts.toArray(new Resource[contexts.size()]);
	}
}
