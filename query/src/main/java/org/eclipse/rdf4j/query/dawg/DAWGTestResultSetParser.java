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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.rdf4j.model.Graph;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.GraphImpl;
import org.eclipse.rdf4j.model.util.GraphUtil;
import org.eclipse.rdf4j.model.util.GraphUtilException;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.TupleQueryResultHandler;
import org.eclipse.rdf4j.query.TupleQueryResultHandlerException;
import org.eclipse.rdf4j.query.impl.MapBindingSet;
import org.eclipse.rdf4j.query.impl.SimpleBinding;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.helpers.AbstractRDFHandler;

/**
 * @author Arjohn Kampman
 */
public class DAWGTestResultSetParser extends AbstractRDFHandler {

	/*-----------*
	 * Constants *
	 *-----------*/

	/**
	 * RDFHandler to report the generated statements to.
	 */
	private final TupleQueryResultHandler tqrHandler;

	/*-----------*
	 * Variables *
	 *-----------*/

	private Graph graph = new GraphImpl();

	/*--------------*
	 * Constructors *
	 *--------------*/

	public DAWGTestResultSetParser(TupleQueryResultHandler tqrHandler) {
		this.tqrHandler = tqrHandler;
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	public void startRDF() throws RDFHandlerException {
		graph.clear();
	}

	@Override
	public void handleStatement(Statement st) throws RDFHandlerException {
		graph.add(st);
	}

	@Override
	public void endRDF() throws RDFHandlerException {
		try {
			Resource resultSetNode = GraphUtil.getUniqueSubject(graph, RDF.TYPE, RESULTSET);

			List<String> bindingNames = getBindingNames(resultSetNode);
			tqrHandler.startQueryResult(bindingNames);

			Iterator<Value> solIter = GraphUtil.getObjectIterator(graph, resultSetNode, SOLUTION);
			while (solIter.hasNext()) {
				Value solutionNode = solIter.next();

				if (solutionNode instanceof Resource) {
					reportSolution((Resource) solutionNode, bindingNames);
				} else {
					throw new RDFHandlerException("Value for " + SOLUTION + " is not a resource: " + solutionNode);
				}
			}

			tqrHandler.endQueryResult();
		} catch (GraphUtilException e) {
			throw new RDFHandlerException(e.getMessage(), e);
		} catch (TupleQueryResultHandlerException e) {
			throw new RDFHandlerException(e.getMessage(), e);
		}
	}

	private List<String> getBindingNames(Resource resultSetNode) throws RDFHandlerException {
		List<String> bindingNames = new ArrayList<>(16);

		Iterator<Value> varIter = GraphUtil.getObjectIterator(graph, resultSetNode, RESULTVARIABLE);

		while (varIter.hasNext()) {
			Value varName = varIter.next();

			if (varName instanceof Literal) {
				bindingNames.add(((Literal) varName).getLabel());
			} else {
				throw new RDFHandlerException("Value for " + RESULTVARIABLE + " is not a literal: " + varName);
			}
		}

		return bindingNames;
	}

	private void reportSolution(Resource solutionNode, List<String> bindingNames)
			throws RDFHandlerException, GraphUtilException {
		MapBindingSet bindingSet = new MapBindingSet(bindingNames.size());

		Iterator<Value> bindingIter = GraphUtil.getObjectIterator(graph, solutionNode, BINDING);
		while (bindingIter.hasNext()) {
			Value bindingNode = bindingIter.next();

			if (bindingNode instanceof Resource) {
				Binding binding = getBinding((Resource) bindingNode);
				bindingSet.addBinding(binding);
			} else {
				throw new RDFHandlerException("Value for " + BINDING + " is not a resource: " + bindingNode);
			}
		}

		try {
			tqrHandler.handleSolution(bindingSet);
		} catch (TupleQueryResultHandlerException e) {
			throw new RDFHandlerException(e.getMessage(), e);
		}
	}

	private Binding getBinding(Resource bindingNode) throws GraphUtilException {
		Literal name = GraphUtil.getUniqueObjectLiteral(graph, bindingNode, VARIABLE);
		Value value = GraphUtil.getUniqueObject(graph, bindingNode, VALUE);
		return new SimpleBinding(name.getLabel(), value);
	}
}
