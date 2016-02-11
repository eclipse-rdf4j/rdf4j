/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.parser.ParsedBooleanQuery;

/**
 * @author Arjohn Kampman
 */
public class SailConnectionBooleanQuery extends SailConnectionQuery implements BooleanQuery {

	public SailConnectionBooleanQuery(ParsedBooleanQuery tupleQuery, SailConnection sailConnection) {
		super(tupleQuery, sailConnection);
	}

	@Override
	public ParsedBooleanQuery getParsedQuery() {
		return (ParsedBooleanQuery)super.getParsedQuery();
	}

	@Override
	public boolean evaluate()
		throws QueryEvaluationException
	{
		ParsedBooleanQuery parsedBooleanQuery = getParsedQuery();
		TupleExpr tupleExpr = parsedBooleanQuery.getTupleExpr();
		Dataset dataset = getDataset();
		if (dataset == null) {
			// No external dataset specified, use query's own dataset (if any)
			dataset = parsedBooleanQuery.getDataset();
		}

		try {
			SailConnection sailCon = getSailConnection();

			CloseableIteration<? extends BindingSet, QueryEvaluationException> bindingsIter;
			bindingsIter = sailCon.evaluate(tupleExpr, dataset, getBindings(), getIncludeInferred());

			bindingsIter = enforceMaxQueryTime(bindingsIter);

			try {
				return bindingsIter.hasNext();
			}
			finally {
				bindingsIter.close();
			}
		}
		catch (SailException e) {
			throw new QueryEvaluationException(e.getMessage(), e);
		}
	}
}
