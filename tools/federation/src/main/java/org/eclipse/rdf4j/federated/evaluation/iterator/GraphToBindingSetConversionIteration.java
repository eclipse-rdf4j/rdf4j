/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.federated.evaluation.iterator;

import java.util.NoSuchElementException;

import org.eclipse.rdf4j.common.iteration.AbstractCloseableIteration;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryBindingSet;

/**
 * Converts graph results into a binding set iteration
 *
 * @author Andreas Schwarte
 */
public class GraphToBindingSetConversionIteration
		extends AbstractCloseableIteration<BindingSet, QueryEvaluationException> {

	protected final GraphQueryResult graph;

	public GraphToBindingSetConversionIteration(GraphQueryResult graph) {
		super();
		this.graph = graph;
	}

	@Override
	public boolean hasNext() throws QueryEvaluationException {
		return graph.hasNext();
	}

	@Override
	public BindingSet next() throws QueryEvaluationException {

		try {
			return convert(graph.next());
		} catch (NoSuchElementException | IllegalStateException e) {
			throw e;
		}
	}

	@Override
	public void remove() throws QueryEvaluationException {

		try {
			graph.remove();
		} catch (UnsupportedOperationException | IllegalStateException e) {
			throw e;
		}
	}

	protected BindingSet convert(Statement st) {
		QueryBindingSet result = new QueryBindingSet();
		result.addBinding("subject", st.getSubject());
		result.addBinding("predicate", st.getPredicate());
		result.addBinding("object", st.getObject());
		if (st.getContext() != null) {
			result.addBinding("context", st.getContext());
		}

		return result;
	}

	@Override
	protected void handleClose() throws QueryEvaluationException {
		graph.close();
	}
}
