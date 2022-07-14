/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.federated.algebra;

import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.EmptyIteration;
import org.eclipse.rdf4j.federated.endpoint.Endpoint;
import org.eclipse.rdf4j.federated.evaluation.TripleSource;
import org.eclipse.rdf4j.federated.evaluation.iterator.InsertBindingsIteration;
import org.eclipse.rdf4j.federated.evaluation.iterator.SingleBindingSetIteration;
import org.eclipse.rdf4j.federated.exception.IllegalQueryException;
import org.eclipse.rdf4j.federated.structures.QueryInfo;
import org.eclipse.rdf4j.federated.util.QueryStringUtil;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.repository.RepositoryException;

/**
 * Represents a StatementPattern that can only produce results at a single endpoint, the owner.
 *
 * @author Andreas Schwarte
 */
public class ExclusiveStatement extends FedXStatementPattern implements ExclusiveTupleExpr {

	private static final long serialVersionUID = -6963394279179263763L;

	public ExclusiveStatement(StatementPattern node, StatementSource owner, QueryInfo queryInfo) {
		super(node, queryInfo);
		statementSources.add(owner);
	}

	@Override
	public StatementSource getOwner() {
		return getStatementSources().get(0);
	}

	@Override
	public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(
			BindingSet bindings) throws QueryEvaluationException {

		try {

			Endpoint ownedEndpoint = queryInfo.getFederationContext()
					.getEndpointManager()
					.getEndpoint(getOwner().getEndpointID());
			TripleSource t = ownedEndpoint.getTripleSource();

			/*
			 * Implementation note: for some endpoint types it is much more efficient to use prepared queries as there
			 * might be some overhead (obsolete optimization) in the native implementation. This is for instance the
			 * case for SPARQL connections. In contrast for NativeRepositories it is much more efficient to use
			 * getStatements(subj, pred, obj) instead of evaluating a prepared query.
			 */

			CloseableIteration<BindingSet, QueryEvaluationException> res;
			if (t.usePreparedQuery(this, queryInfo)) {

				AtomicBoolean isEvaluated = new AtomicBoolean(false); // is filter evaluated
				String preparedQuery;
				try {
					preparedQuery = QueryStringUtil.selectQueryString(this, bindings, filterExpr, isEvaluated,
							queryInfo.getDataset());
				} catch (IllegalQueryException e1) {
					// TODO there might be an issue with filters being evaluated => investigate
					/* all vars are bound, this must be handled as a check query, can occur in joins */
					if (t.hasStatements(this, bindings, queryInfo, queryInfo.getDataset())) {
						res = new SingleBindingSetIteration(bindings);
						if (boundFilters != null) {
							// make sure to insert any values from FILTER expressions that are directly
							// bound in this expression
							res = new InsertBindingsIteration(res, boundFilters);
						}
						return res;
					}
					return new EmptyIteration<>();
				}

				res = t.getStatements(preparedQuery, bindings, (isEvaluated.get() ? null : filterExpr), queryInfo);

			} else {
				res = t.getStatements(this, bindings, filterExpr, queryInfo);
			}

			if (boundFilters != null) {
				// make sure to insert any values from FILTER expressions that are directly
				// bound in this expression
				res = new InsertBindingsIteration(res, boundFilters);
			}

			return res;

		} catch (RepositoryException | MalformedQueryException e) {
			throw new QueryEvaluationException(e);
		}
	}
}
