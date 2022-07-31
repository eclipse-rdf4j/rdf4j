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

import java.util.ArrayList;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.TupleQueryResultHandler;
import org.eclipse.rdf4j.query.TupleQueryResultHandlerException;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.impl.IteratingTupleQueryResult;
import org.eclipse.rdf4j.query.parser.ParsedTupleQuery;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.SailException;

/**
 * @author Arjohn Kampman
 */
public class SailTupleQuery extends SailQuery implements TupleQuery {

	public SailTupleQuery(ParsedTupleQuery tupleQuery, SailRepositoryConnection sailConnection) {
		super(tupleQuery, sailConnection);
	}

	@Override
	public ParsedTupleQuery getParsedQuery() {
		return (ParsedTupleQuery) super.getParsedQuery();
	}

	@Override
	public TupleQueryResult evaluate() throws QueryEvaluationException {
		TupleExpr tupleExpr = getParsedQuery().getTupleExpr();

		CloseableIteration<? extends BindingSet, QueryEvaluationException> bindingsIter = null;

		boolean allGood = false;
		try {
			SailConnection sailCon = getConnection().getSailConnection();

			bindingsIter = sailCon.evaluate(tupleExpr, getActiveDataset(), getBindings(), getIncludeInferred());
			bindingsIter = enforceMaxQueryTime(bindingsIter);

			IteratingTupleQueryResult result = new IteratingTupleQueryResult(
					new ArrayList<>(tupleExpr.getBindingNames()), bindingsIter);
			allGood = true;
			return result;
		} catch (SailException e) {
			throw new QueryEvaluationException(e.getMessage(), e);
		} finally {
			if (!allGood) {
				if (bindingsIter != null) {
					bindingsIter.close();
				}
			}
		}
	}

	@Override
	public void evaluate(TupleQueryResultHandler handler)
			throws QueryEvaluationException, TupleQueryResultHandlerException {
		TupleQueryResult queryResult = evaluate();
		QueryResults.report(queryResult, handler);
	}

}
