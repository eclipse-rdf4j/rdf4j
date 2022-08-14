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
package org.eclipse.rdf4j.federated.algebra;

import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.EmptyIteration;
import org.eclipse.rdf4j.federated.FederationContext;
import org.eclipse.rdf4j.federated.endpoint.Endpoint;
import org.eclipse.rdf4j.federated.evaluation.TripleSource;
import org.eclipse.rdf4j.federated.evaluation.iterator.InsertBindingsIteration;
import org.eclipse.rdf4j.federated.evaluation.iterator.SingleBindingSetIteration;
import org.eclipse.rdf4j.federated.evaluation.union.ParallelPreparedUnionTask;
import org.eclipse.rdf4j.federated.evaluation.union.ParallelUnionTask;
import org.eclipse.rdf4j.federated.evaluation.union.WorkerUnionBase;
import org.eclipse.rdf4j.federated.exception.IllegalQueryException;
import org.eclipse.rdf4j.federated.structures.QueryInfo;
import org.eclipse.rdf4j.federated.util.QueryStringUtil;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.repository.RepositoryException;

/**
 * Represents statements that can produce results at a some particular endpoints, the statement sources.
 *
 * @author Andreas Schwarte
 *
 * @see StatementSource
 */
public class StatementSourcePattern extends FedXStatementPattern {

	private static final long serialVersionUID = 7548505818766482715L;

	protected boolean usePreparedQuery = false;
	protected final FederationContext federationContext;

	public StatementSourcePattern(StatementPattern node, QueryInfo queryInfo) {
		super(node, queryInfo);
		this.federationContext = queryInfo.getFederationContext();
	}

	public void addStatementSource(StatementSource statementSource) {
		statementSources.add(statementSource);
	}

	@Override
	public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(BindingSet bindings)
			throws QueryEvaluationException {

		WorkerUnionBase<BindingSet> union = null;
		try {

			AtomicBoolean isEvaluated = new AtomicBoolean(false); // is filter evaluated in prepared query
			String preparedQuery = null; // used for some triple sources
			union = federationContext.getManager().createWorkerUnion(queryInfo);

			for (StatementSource source : statementSources) {

				Endpoint ownedEndpoint = queryInfo.getFederationContext()
						.getEndpointManager()
						.getEndpoint(source.getEndpointID());
				TripleSource t = ownedEndpoint.getTripleSource();

				/*
				 * Implementation note: for some endpoint types it is much more efficient to use prepared queries as
				 * there might be some overhead (obsolete optimization) in the native implementation. This is for
				 * instance the case for SPARQL connections. In contrast for NativeRepositories it is much more
				 * efficient to use getStatements(subj, pred, obj) instead of evaluating a prepared query.
				 */

				if (t.usePreparedQuery(this, queryInfo)) {

					// queryString needs to be constructed only once for a given bindingset
					if (preparedQuery == null) {
						try {
							preparedQuery = QueryStringUtil.selectQueryString(this, bindings, filterExpr, isEvaluated,
									queryInfo.getDataset());
						} catch (IllegalQueryException e1) {
							/* all vars are bound, this must be handled as a check query, can occur in joins */
							CloseableIteration<BindingSet, QueryEvaluationException> res = handleStatementSourcePatternCheck(
									bindings);
							if (boundFilters != null && !(res instanceof EmptyIteration)) {
								res = new InsertBindingsIteration(res, boundFilters);
							}
							return res;
						}
					}

					union.addTask(new ParallelPreparedUnionTask(union, preparedQuery, ownedEndpoint, bindings,
							(isEvaluated.get() ? null : filterExpr), queryInfo));

				} else {
					union.addTask(new ParallelUnionTask(union, this, ownedEndpoint, bindings, filterExpr, queryInfo));
				}

			}

			union.run(); // execute the union in this thread

			if (boundFilters != null) {
				// make sure to insert any values from FILTER expressions that are directly
				// bound in this expression
				return new InsertBindingsIteration(union, boundFilters);
			} else {
				return union;
			}

		} catch (RepositoryException | MalformedQueryException e) {
			if (union != null) {
				union.close();
			}
			throw new QueryEvaluationException(e);
		} catch (Throwable t) {
			if (union != null) {
				union.close();
			}
			throw t;
		}
	}

	protected CloseableIteration<BindingSet, QueryEvaluationException> handleStatementSourcePatternCheck(
			BindingSet bindings) throws RepositoryException, MalformedQueryException, QueryEvaluationException {

		// if at least one source has statements, we can return this binding set as result

		// XXX do this in parallel for the number of endpoints ?
		for (StatementSource source : statementSources) {
			Endpoint ownedEndpoint = queryInfo.getFederationContext()
					.getEndpointManager()
					.getEndpoint(source.getEndpointID());
			TripleSource t = ownedEndpoint.getTripleSource();
			if (t.hasStatements(this, bindings, queryInfo, queryInfo.getDataset())) {
				return new SingleBindingSetIteration(bindings);
			}
		}

		return new EmptyIteration<>();
	}
}
