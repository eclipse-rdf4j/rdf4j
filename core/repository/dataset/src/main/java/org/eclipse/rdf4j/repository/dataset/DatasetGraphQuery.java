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
package org.eclipse.rdf4j.repository.dataset;

import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.repository.sail.SailGraphQuery;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.RDFHandlerException;

/**
 * @author Arjohn Kampman
 */
class DatasetGraphQuery extends DatasetQuery implements GraphQuery {

	protected DatasetGraphQuery(DatasetRepositoryConnection con, SailGraphQuery sailQuery) {
		super(con, sailQuery);
	}

	@Override
	public GraphQueryResult evaluate() throws QueryEvaluationException {
		con.loadDataset(sailQuery.getActiveDataset());
		return ((GraphQuery) sailQuery).evaluate();
	}

	@Override
	public void evaluate(RDFHandler handler) throws QueryEvaluationException, RDFHandlerException {
		con.loadDataset(sailQuery.getActiveDataset());
		((GraphQuery) sailQuery).evaluate(handler);
	}
}
