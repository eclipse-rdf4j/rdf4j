/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.federated.evaluation;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.ExceptionConvertingIteration;
import org.eclipse.rdf4j.federated.FederationContext;
import org.eclipse.rdf4j.federated.algebra.ExclusiveTupleExpr;
import org.eclipse.rdf4j.federated.algebra.FilterValueExpr;
import org.eclipse.rdf4j.federated.endpoint.Endpoint;
import org.eclipse.rdf4j.federated.endpoint.SparqlEndpointConfiguration;
import org.eclipse.rdf4j.federated.evaluation.iterator.ConsumingIteration;
import org.eclipse.rdf4j.federated.exception.ExceptionUtil;
import org.eclipse.rdf4j.federated.structures.QueryInfo;
import org.eclipse.rdf4j.federated.util.FedXUtil;
import org.eclipse.rdf4j.federated.util.QueryStringUtil;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.query.Dataset;
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
 * The query result of {@link #getStatements(String, BindingSet, FilterValueExpr, QueryInfo)} is wrapped in a
 * {@link ConsumingIteration} to avoid blocking behavior..
 *
 * @author Andreas Schwarte
 *
 */
public class SparqlTripleSource extends TripleSourceBase {

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
			StatementPattern stmt, BindingSet bindings, FilterValueExpr filterExpr, QueryInfo queryInfo)
			throws RepositoryException, MalformedQueryException,
			QueryEvaluationException {

		throw new RuntimeException("NOT YET IMPLEMENTED.");
	}

	@Override
	public boolean hasStatements(Resource subj,
			IRI pred, Value obj, QueryInfo queryInfo, Resource... contexts)
			throws RepositoryException {

		if (!useASKQueries) {
			StatementPattern st = new StatementPattern(new Var("s", subj), new Var("p", pred), new Var("o", obj));
			Dataset dataset = FedXUtil.toDataset(contexts);
			try {
				return hasStatements(st, EmptyBindingSet.getInstance(), queryInfo, dataset);
			} catch (Exception e) {
				throw new RepositoryException(e);
			}
		}
		return super.hasStatements(subj, pred, obj, queryInfo, contexts);
	}

	@Override
	public boolean hasStatements(StatementPattern stmt,
			BindingSet bindings, QueryInfo queryInfo, Dataset dataset) throws RepositoryException,
			MalformedQueryException, QueryEvaluationException {

		// decide whether to use ASK queries or a SELECT query
		if (useASKQueries) {
			/* remote boolean query */
			String queryString = QueryStringUtil.askQueryString(stmt, bindings, dataset);

			try (RepositoryConnection conn = endpoint.getConnection()) {
				BooleanQuery query = conn.prepareBooleanQuery(QueryLanguage.SPARQL, queryString, null);
				configureInference(query, queryInfo);
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
				String queryString = QueryStringUtil.selectQueryStringLimit1(stmt, bindings, dataset);
				TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
				configureInference(query, queryInfo);
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
	public boolean hasStatements(ExclusiveTupleExpr expr,
			BindingSet bindings)
			throws RepositoryException, MalformedQueryException,
			QueryEvaluationException {

		if (!useASKQueries) {

			/* remote select limit 1 query */
			try (RepositoryConnection conn = endpoint.getConnection()) {
				String queryString = QueryStringUtil.selectQueryStringLimit1(expr, bindings,
						expr.getQueryInfo().getDataset());
				TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
				configureInference(query, expr.getQueryInfo());
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
		return super.hasStatements(expr, bindings);
	}

	@Override
	public boolean usePreparedQuery(StatementPattern stmt, QueryInfo queryInfo) {
		return true;
	}

	@Override
	public CloseableIteration<BindingSet, QueryEvaluationException> getStatements(
			TupleExpr preparedQuery, BindingSet bindings, FilterValueExpr filterExpr, QueryInfo queryInfo)
			throws RepositoryException, MalformedQueryException,
			QueryEvaluationException {

		throw new RuntimeException("NOT YET IMPLEMENTED.");
	}

	@Override
	public CloseableIteration<Statement, QueryEvaluationException> getStatements(
			Resource subj, IRI pred, Value obj, QueryInfo queryInfo,
			Resource... contexts) throws RepositoryException,
			MalformedQueryException, QueryEvaluationException {

		return withConnection((conn, resultHolder) -> {
			monitorRemoteRequest();
			RepositoryResult<Statement> repoResult = conn.getStatements(subj, pred, obj,
					queryInfo.getIncludeInferred(), contexts);

			resultHolder.set(new ExceptionConvertingIteration<>(repoResult) {
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
