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

import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.TupleQueryResultHandler;
import org.eclipse.rdf4j.query.TupleQueryResultHandlerException;
import org.eclipse.rdf4j.repository.sail.SailTupleQuery;

/**
 * @author Arjohn Kampman
 */
class DatasetTupleQuery extends DatasetQuery implements TupleQuery {

	protected DatasetTupleQuery(DatasetRepositoryConnection con, SailTupleQuery sailQuery) {
		super(con, sailQuery);
	}

	@Override
	public TupleQueryResult evaluate() throws QueryEvaluationException {
		con.loadDataset(sailQuery.getActiveDataset());
		return ((TupleQuery) sailQuery).evaluate();
	}

	@Override
	public void evaluate(TupleQueryResultHandler handler)
			throws QueryEvaluationException, TupleQueryResultHandlerException {
		con.loadDataset(sailQuery.getActiveDataset());
		((TupleQuery) sailQuery).evaluate(handler);
	}
}
