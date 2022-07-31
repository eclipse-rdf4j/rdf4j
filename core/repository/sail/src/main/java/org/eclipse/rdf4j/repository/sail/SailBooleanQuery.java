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
package org.eclipse.rdf4j.repository.sail;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.parser.ParsedBooleanQuery;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.SailException;

/**
 * @author Arjohn Kampman
 */
public class SailBooleanQuery extends SailQuery implements BooleanQuery {

	protected SailBooleanQuery(ParsedBooleanQuery tupleQuery, SailRepositoryConnection sailConnection) {
		super(tupleQuery, sailConnection);
	}

	@Override
	public ParsedBooleanQuery getParsedQuery() {
		return (ParsedBooleanQuery) super.getParsedQuery();
	}

	@Override
	public boolean evaluate() throws QueryEvaluationException {
		ParsedBooleanQuery parsedBooleanQuery = getParsedQuery();
		TupleExpr tupleExpr = parsedBooleanQuery.getTupleExpr();
		Dataset dataset = getDataset();
		if (dataset == null) {
			// No external dataset specified, use query's own dataset (if any)
			dataset = parsedBooleanQuery.getDataset();
		}

		CloseableIteration<? extends BindingSet, QueryEvaluationException> bindingsIter1 = null;
		CloseableIteration<? extends BindingSet, QueryEvaluationException> bindingsIter2 = null;

		try {
			SailConnection sailCon = getConnection().getSailConnection();

			bindingsIter1 = sailCon.evaluate(tupleExpr, dataset, getBindings(), getIncludeInferred());

			bindingsIter2 = enforceMaxQueryTime(bindingsIter1);

			return bindingsIter2.hasNext();
		} catch (SailException e) {
			throw new QueryEvaluationException(e.getMessage(), e);
		} finally {
			// Always cleanup all iterators, as they are not persistently visible outside of this method
			try {
				if (bindingsIter2 != null) {
					bindingsIter2.close();
				}
			} finally {
				if (bindingsIter1 != null) {
					bindingsIter1.close();
				}
			}
		}
	}

}
