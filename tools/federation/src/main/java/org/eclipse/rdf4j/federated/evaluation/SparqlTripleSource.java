/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.federated.evaluation;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.EmptyIteration;
import org.eclipse.rdf4j.common.iteration.ExceptionConvertingIteration;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.federated.FederationContext;
import org.eclipse.rdf4j.federated.algebra.ExclusiveGroup;
import org.eclipse.rdf4j.federated.algebra.FilterValueExpr;
import org.eclipse.rdf4j.federated.endpoint.Endpoint;
import org.eclipse.rdf4j.federated.endpoint.SparqlEndpointConfiguration;
import org.eclipse.rdf4j.federated.evaluation.iterator.ConsumingIteration;
import org.eclipse.rdf4j.federated.evaluation.iterator.FilteringInsertBindingsIteration;
import org.eclipse.rdf4j.federated.evaluation.iterator.FilteringIteration;
import org.eclipse.rdf4j.federated.evaluation.iterator.InsertBindingsIteration;
import org.eclipse.rdf4j.federated.exception.ExceptionUtil;
import org.eclipse.rdf4j.federated.util.QueryStringUtil;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.impl.EmptyBindingSet;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryResult;

/**
 * A triple source to be used for (remote) SPARQL endpoints.
 * <p>
 * 
 * This triple source supports the {@link SparqlEndpointConfiguration} for defining whether ASK queries are to be used
 * for source selection.
 * 
 * The query result of {@link #getStatements(String, BindingSet, FilterValueExpr)} is wrapped in a
 * {@link ConsumingIteration} to avoid blocking behavior..
 * 
 * @author Andreas Schwarte
 *
 */
public class SparqlTripleSource extends TripleSourceBase implements TripleSource {

	private boolean useASKQueries = true;

	SparqlTripleSource(Endpoint endpoint, FederationContext federationContext) {
		super(federationContext, endpoint);
		if (endpoint.getEndpointConfiguration() instanceof SparqlEndpointConfiguration) {
			SparqlEndpointConfiguration c = (SparqlEndpointConfiguration) endpoint.getEndpointConfiguration();
			this.useASKQueries = c.supportsASKQueries();
		}
	}

	@Override
	public CloseableIteration<BindingSet, QueryEvaluationException> getStatements(
			String preparedQuery, BindingSet bindings, FilterValueExpr filterExpr)
			throws RepositoryException, MalformedQueryException,
			QueryEvaluationException {

		return withConnection((conn, resultHolder) -> {

			TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, preparedQuery, null);
			applyMaxExecutionTimeUpperBound(query);
			disableInference(query);

			// evaluate the query
			monitorRemoteRequest();
			CloseableIteration<BindingSet, QueryEvaluationException> res = query.evaluate();
			resultHolder.set(res);

			// apply filter and/or insert original bindings
			if (filterExpr != null) {
				if (bindings.size() > 0)
					res = new FilteringInsertBindingsIteration(filterExpr, bindings, res,
							SparqlTripleSource.this.strategy);
				else
					res = new FilteringIteration(filterExpr, res, SparqlTripleSource.this.strategy);
				if (!res.hasNext()) {
					Iterations.closeCloseable(res);
					conn.close();
					resultHolder.set(new EmptyIteration<>());
					return;
				}
			} else if (bindings.size() > 0) {
				res = new InsertBindingsIteration(res, bindings);
			}

			resultHolder.set(new ConsumingIteration(res));

		});
	}

	@Override
	public CloseableIteration<BindingSet, QueryEvaluationException> getStatements(
			StatementPattern stmt, BindingSet bindings, FilterValueExpr filterExpr)
			throws RepositoryException, MalformedQueryException,
			QueryEvaluationException {

		throw new RuntimeException("NOT YET IMPLEMENTED.");
	}

	@Override
	public boolean hasStatements(Resource subj,
			IRI pred, Value obj, Resource... contexts)
			throws RepositoryException {

		if (!useASKQueries) {
			StatementPattern st = new StatementPattern(new Var("s", subj), new Var("p", pred), new Var("o", obj));
			try {
				return hasStatements(st, EmptyBindingSet.getInstance());
			} catch (Exception e) {
				throw new RepositoryException(e);
			}
		}
		return super.hasStatements(subj, pred, obj, contexts);
	}

	@Override
	public boolean hasStatements(StatementPattern stmt,
			BindingSet bindings) throws RepositoryException,
			MalformedQueryException, QueryEvaluationException {

		// decide whether to use ASK queries or a SELECT query
		if (useASKQueries) {
			/* remote boolean query */
			String queryString = QueryStringUtil.askQueryString(stmt, bindings);

			try (RepositoryConnection conn = endpoint.getConnection()) {
				BooleanQuery query = conn.prepareBooleanQuery(QueryLanguage.SPARQL, queryString, null);
				disableInference(query);
				applyMaxExecutionTimeUpperBound(query);

				monitorRemoteRequest();
				boolean hasStatements = query.evaluate();
				return hasStatements;
			} catch (Throwable ex) {
				// convert into QueryEvaluationException with additional info
				throw ExceptionUtil.traceExceptionSourceAndRepair(endpoint, ex, "Subquery: " + queryString);
			}

		} else {
			/* remote select limit 1 query */
			try (RepositoryConnection conn = endpoint.getConnection()) {
				String queryString = QueryStringUtil.selectQueryStringLimit1(stmt, bindings);
				TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
				disableInference(query);
				applyMaxExecutionTimeUpperBound(query);

				monitorRemoteRequest();
				try (TupleQueryResult qRes = query.evaluate()) {

					boolean hasStatements = qRes.hasNext();
					return hasStatements;
				} catch (Throwable ex) {
					// convert into QueryEvaluationException with additional info
					throw ExceptionUtil.traceExceptionSourceAndRepair(endpoint, ex, "Subquery: " + queryString);
				}
			}
		}

	}

	@Override
	public boolean hasStatements(ExclusiveGroup group,
			BindingSet bindings)
			throws RepositoryException, MalformedQueryException,
			QueryEvaluationException {

		if (!useASKQueries) {

			/* remote select limit 1 query */
			try (RepositoryConnection conn = endpoint.getConnection()) {
				String queryString = QueryStringUtil.selectQueryStringLimit1(group, bindings);
				TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
				disableInference(query);
				applyMaxExecutionTimeUpperBound(query);

				monitorRemoteRequest();
				try (TupleQueryResult qRes = query.evaluate()) {

					boolean hasStatements = qRes.hasNext();
					return hasStatements;
				} catch (Throwable ex) {
					// convert into QueryEvaluationException with additional info
					throw ExceptionUtil.traceExceptionSourceAndRepair(endpoint, ex, "Subquery: " + queryString);
				}
			}
		}

		// default handling: use ASK query
		return super.hasStatements(group, bindings);
	}

	@Override
	public boolean usePreparedQuery() {
		return true;
	}

	@Override
	public CloseableIteration<BindingSet, QueryEvaluationException> getStatements(
			TupleExpr preparedQuery, BindingSet bindings, FilterValueExpr filterExpr)
			throws RepositoryException, MalformedQueryException,
			QueryEvaluationException {

		throw new RuntimeException("NOT YET IMPLEMENTED.");
	}

	@Override
	public CloseableIteration<Statement, QueryEvaluationException> getStatements(
			Resource subj, IRI pred, Value obj,
			Resource... contexts) throws RepositoryException,
			MalformedQueryException, QueryEvaluationException {

		// TODO add handling for contexts
		return withConnection((conn, resultHolder) -> {
			monitorRemoteRequest();
			RepositoryResult<Statement> repoResult = conn.getStatements(subj, pred, obj, true);

			resultHolder.set(new ExceptionConvertingIteration<Statement, QueryEvaluationException>(repoResult) {
				@Override
				protected QueryEvaluationException convert(Exception ex) {
					if (ex instanceof QueryEvaluationException) {
						return (QueryEvaluationException) ex;
					}
					return new QueryEvaluationException(ex);
				}
			});
		});
	}

	@Override
	public String toString() {
		return "Sparql Triple Source: Endpoint - " + endpoint.getId();
	}
}
