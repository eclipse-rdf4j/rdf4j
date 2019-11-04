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
import org.eclipse.rdf4j.federated.algebra.FilterValueExpr;
import org.eclipse.rdf4j.federated.algebra.PrecompiledQueryNode;
import org.eclipse.rdf4j.federated.endpoint.Endpoint;
import org.eclipse.rdf4j.federated.evaluation.iterator.FilteringInsertBindingsIteration;
import org.eclipse.rdf4j.federated.evaluation.iterator.FilteringIteration;
import org.eclipse.rdf4j.federated.evaluation.iterator.InsertBindingsIteration;
import org.eclipse.rdf4j.federated.evaluation.iterator.StatementConversionIteration;
import org.eclipse.rdf4j.federated.util.QueryAlgebraUtil;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.impl.EmptyBindingSet;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.SailConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A triple source to be used on any repository.
 * 
 * @author Andreas Schwarte
 *
 */
public class SailTripleSource extends TripleSourceBase implements TripleSource {

	private static final Logger log = LoggerFactory.getLogger(SailTripleSource.class);

	SailTripleSource(Endpoint endpoint, FederationContext federationContext) {
		super(federationContext, endpoint);
	}

	@Override
	public CloseableIteration<BindingSet, QueryEvaluationException> getStatements(
			String preparedQuery, final BindingSet bindings, final FilterValueExpr filterExpr)
			throws RepositoryException, MalformedQueryException,
			QueryEvaluationException {

		return withConnection((conn, resultHolder) -> {

			TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, preparedQuery, null);
			disableInference(query);
			applyMaxExecutionTimeUpperBound(query);

			// evaluate the query
			CloseableIteration<BindingSet, QueryEvaluationException> res = query.evaluate();
			resultHolder.set(res);

			// apply filter and/or insert original bindings
			if (filterExpr != null) {
				if (bindings.size() > 0)
					res = new FilteringInsertBindingsIteration(filterExpr, bindings, res,
							SailTripleSource.this.strategy);
				else
					res = new FilteringIteration(filterExpr, res, SailTripleSource.this.strategy);
				if (!res.hasNext()) {
					Iterations.closeCloseable(res);
					resultHolder.set(new EmptyIteration<>());
					return;
				}
			} else if (bindings.size() > 0) {
				res = new InsertBindingsIteration(res, bindings);
			}

			resultHolder.set(res);
		});
	}

	@Override
	public CloseableIteration<BindingSet, QueryEvaluationException> getStatements(
			StatementPattern stmt,
			final BindingSet bindings, FilterValueExpr filterExpr)
			throws RepositoryException, MalformedQueryException,
			QueryEvaluationException {

		Value subjValue = QueryAlgebraUtil.getVarValue(stmt.getSubjectVar(), bindings);
		Value predValue = QueryAlgebraUtil.getVarValue(stmt.getPredicateVar(), bindings);
		Value objValue = QueryAlgebraUtil.getVarValue(stmt.getObjectVar(), bindings);

		return withConnection((conn, resultHolder) -> {

			RepositoryResult<Statement> repoResult = conn.getStatements((Resource) subjValue, (IRI) predValue, objValue,
					true, new Resource[0]);

			// XXX implementation remark and TODO taken from Sesame
			// The same variable might have been used multiple times in this
			// StatementPattern, verify value equality in those cases.

			// an iterator that converts the statements to var bindings
			resultHolder.set(new StatementConversionIteration(repoResult, bindings, stmt));

			// if filter is set, apply it
			if (filterExpr != null) {
				FilteringIteration filteredRes = new FilteringIteration(filterExpr, resultHolder.get(),
						SailTripleSource.this.strategy);
				if (!filteredRes.hasNext()) {
					Iterations.closeCloseable(filteredRes);
					resultHolder.set(new EmptyIteration<>());
					return;
				}
				resultHolder.set(filteredRes);
			}
		});
	}

	@Override
	public CloseableIteration<Statement, QueryEvaluationException> getStatements(
			Resource subj, IRI pred, Value obj, Resource... contexts)
			throws RepositoryException,
			MalformedQueryException, QueryEvaluationException {

		// TODO add handling for contexts
		return withConnection((conn, resultHolder) -> {

			RepositoryResult<Statement> repoResult = conn.getStatements(subj, pred, obj, true);

			// XXX implementation remark and TODO taken from Sesame
			// The same variable might have been used multiple times in this
			// StatementPattern, verify value equality in those cases.

			resultHolder.set(new ExceptionConvertingIteration<Statement, QueryEvaluationException>(repoResult) {
				@Override
				protected QueryEvaluationException convert(Exception arg0) {
					return new QueryEvaluationException(arg0);
				}
			});
		});
	}

	@Override
	public boolean hasStatements(StatementPattern stmt,
			BindingSet bindings)
			throws RepositoryException, MalformedQueryException,
			QueryEvaluationException {

		Value subjValue = QueryAlgebraUtil.getVarValue(stmt.getSubjectVar(), bindings);
		Value predValue = QueryAlgebraUtil.getVarValue(stmt.getPredicateVar(), bindings);
		Value objValue = QueryAlgebraUtil.getVarValue(stmt.getObjectVar(), bindings);

		try (RepositoryConnection conn = endpoint.getConnection()) {
			return conn.hasStatement((Resource) subjValue, (IRI) predValue, objValue, true, new Resource[0]);
		}
	}

	@Override
	public boolean usePreparedQuery() {
		return false;
	}

	@SuppressWarnings("unchecked")
	@Override
	public CloseableIteration<BindingSet, QueryEvaluationException> getStatements(
			TupleExpr preparedQuery,
			BindingSet bindings, FilterValueExpr filterExpr)
			throws RepositoryException, MalformedQueryException,
			QueryEvaluationException {

		/*
		 * Implementation note:
		 * 
		 * a special strategy is registered for NativeStore instances. The specialized strategy allows to evaluate
		 * prepared queries without prior (obsolete) optimization.
		 */
		return withConnection((conn, resultHolder) -> {

			CloseableIteration<BindingSet, QueryEvaluationException> res;
			SailConnection sailConn = ((SailRepositoryConnection) conn).getSailConnection();

			try {

				// optimization attempt: use precompiled query
				PrecompiledQueryNode precompiledQueryNode = new PrecompiledQueryNode(preparedQuery);
				res = (CloseableIteration<BindingSet, QueryEvaluationException>) sailConn.evaluate(precompiledQueryNode,
						null, EmptyBindingSet.getInstance(), true);

			} catch (Exception e) {
				log.warn(
						"Precompiled query optimization for native store could not be applied: " + e.getMessage());
				log.debug("Details:", e);

				// fallback: attempt the original tuple expression
				res = (CloseableIteration<BindingSet, QueryEvaluationException>) sailConn.evaluate(preparedQuery,
						null, EmptyBindingSet.getInstance(), true);
			}

			if (bindings.size() > 0) {
				res = new InsertBindingsIteration(res, bindings);
			}

			resultHolder.set(res);
		});
	}

}
