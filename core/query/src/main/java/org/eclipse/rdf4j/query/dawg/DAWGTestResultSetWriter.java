/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.dawg;

import static org.eclipse.rdf4j.query.dawg.DAWGTestResultSetSchema.BINDING;
import static org.eclipse.rdf4j.query.dawg.DAWGTestResultSetSchema.RESULTSET;
import static org.eclipse.rdf4j.query.dawg.DAWGTestResultSetSchema.RESULTVARIABLE;
import static org.eclipse.rdf4j.query.dawg.DAWGTestResultSetSchema.SOLUTION;
import static org.eclipse.rdf4j.query.dawg.DAWGTestResultSetSchema.VALUE;
import static org.eclipse.rdf4j.query.dawg.DAWGTestResultSetSchema.VARIABLE;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryResultHandlerException;
import org.eclipse.rdf4j.query.TupleQueryResultHandler;
import org.eclipse.rdf4j.query.TupleQueryResultHandlerException;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.RDFHandlerException;

/**
 * A {@link TupleQueryResultHandler} that converts query results to an RDF graph using the Data Access Working Group
 * Test Result Set RDF Vocabulary (http://www.w3.org/2001/sw/DataAccess/tests/result-set#).
 */
public class DAWGTestResultSetWriter implements TupleQueryResultHandler {

	/*-----------*
	 * Constants *
	 *-----------*/

	/**
	 * RDFHandler to report the generated statements to.
	 */
	private final RDFHandler rdfHandler;

	private final ValueFactory vf;

	/*-----------*
	 * Variables *
	 *-----------*/

	private BNode resultSetNode;

	private final Map<BNode, BNode> bnodeMap = new HashMap<>();

	/*--------------*
	 * Constructors *
	 *--------------*/

	public DAWGTestResultSetWriter(RDFHandler rdfHandler) {
		this(rdfHandler, SimpleValueFactory.getInstance());
	}

	public DAWGTestResultSetWriter(RDFHandler rdfHandler, ValueFactory vf) {
		this.rdfHandler = rdfHandler;
		this.vf = vf;
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	public void startQueryResult(List<String> bindingNames) throws TupleQueryResultHandlerException {
		try {
			rdfHandler.startRDF();

			resultSetNode = vf.createBNode();
			bnodeMap.clear();

			reportStatement(resultSetNode, RDF.TYPE, RESULTSET);

			for (String bindingName : bindingNames) {
				Literal bindingNameLit = vf.createLiteral(bindingName);
				reportStatement(resultSetNode, RESULTVARIABLE, bindingNameLit);
			}
		} catch (RDFHandlerException e) {
			throw new TupleQueryResultHandlerException(e);
		}
	}

	@Override
	public void endQueryResult() throws TupleQueryResultHandlerException {
		resultSetNode = null;

		try {
			rdfHandler.endRDF();
		} catch (RDFHandlerException e) {
			throw new TupleQueryResultHandlerException(e);
		}
	}

	@Override
	public void handleSolution(BindingSet bindingSet) throws TupleQueryResultHandlerException {
		try {
			BNode solutionNode = vf.createBNode();

			reportStatement(resultSetNode, SOLUTION, solutionNode);

			for (Binding binding : bindingSet) {
				BNode bindingNode = vf.createBNode();

				reportStatement(solutionNode, BINDING, bindingNode);
				reportStatement(bindingNode, VARIABLE, vf.createLiteral(binding.getName()));

				Value value = binding.getValue();

				// Map bnodes to new bnodes to prevent collisions with the bnodes
				// generated for the result format
				if (value instanceof BNode) {
					BNode mappedBNode = bnodeMap.get(value);

					if (mappedBNode == null) {
						mappedBNode = vf.createBNode();
						bnodeMap.put((BNode) value, mappedBNode);
					}

					value = mappedBNode;
				}

				reportStatement(bindingNode, VALUE, value);
			}
		} catch (RDFHandlerException e) {
			throw new TupleQueryResultHandlerException(e);
		}
	}

	private void reportStatement(Resource subject, IRI predicate, Value object) throws RDFHandlerException {
		rdfHandler.handleStatement(vf.createStatement(subject, predicate, object));
	}

	@Override
	public void handleBoolean(boolean value) throws QueryResultHandlerException {
		throw new UnsupportedOperationException("Cannot handle boolean results");
	}

	@Override
	public void handleLinks(List<String> linkUrls) throws QueryResultHandlerException {
		// We do not support links, so do nothing
	}
}
