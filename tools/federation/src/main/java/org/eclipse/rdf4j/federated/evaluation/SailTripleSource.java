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
package org.eclipse.rdf4j.federated.evaluation;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.EmptyIteration;
import org.eclipse.rdf4j.federated.FederationContext;
import org.eclipse.rdf4j.federated.algebra.FilterValueExpr;
import org.eclipse.rdf4j.federated.endpoint.Endpoint;
import org.eclipse.rdf4j.federated.evaluation.iterator.FilteringIteration;
import org.eclipse.rdf4j.federated.evaluation.iterator.StatementConversionIteration;
import org.eclipse.rdf4j.federated.structures.QueryInfo;
import org.eclipse.rdf4j.federated.util.FedXUtil;
import org.eclipse.rdf4j.federated.util.QueryAlgebraUtil;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryResult;

/**
 * A triple source to be used on any repository.
 *
 * @author Andreas Schwarte
 */
public class SailTripleSource extends TripleSourceBase {

	SailTripleSource(Endpoint endpoint, FederationContext federationContext) {
		super(federationContext, endpoint);
	}

	@Override
	public CloseableIteration<BindingSet> getStatements(
			StatementPattern stmt,
			final BindingSet bindings, FilterValueExpr filterExpr, QueryInfo queryInfo)
			throws RepositoryException, MalformedQueryException,
			QueryEvaluationException {

		Value subjValue = QueryAlgebraUtil.getVarValue(stmt.getSubjectVar(), bindings);
		Value predValue = QueryAlgebraUtil.getVarValue(stmt.getPredicateVar(), bindings);
		Value objValue = QueryAlgebraUtil.getVarValue(stmt.getObjectVar(), bindings);

		return withConnection((conn, resultHolder) -> {

			// TODO we need to fix this here: if the dataset contains FROM NAMED, we cannot use
			// the API and require to write as query

			RepositoryResult<Statement> repoResult = null;
			try {
				repoResult = conn.getStatements((Resource) subjValue, (IRI) predValue, objValue,
						queryInfo.getIncludeInferred(), FedXUtil.toContexts(stmt, queryInfo.getDataset()));

				// XXX implementation remark and TODO taken from Sesame
				// The same variable might have been used multiple times in this
				// StatementPattern, verify value equality in those cases.

				// an iterator that converts the statements to var bindings
				resultHolder.set(new StatementConversionIteration(repoResult, bindings, stmt));

				// if filter is set, apply it
				if (filterExpr != null) {
					FilteringIteration filteredRes = new FilteringIteration(filterExpr, resultHolder.get(),
							queryInfo.getStrategy());
					if (!filteredRes.hasNext()) {
						filteredRes.close();
						resultHolder.set(new EmptyIteration<>());
						return;
					}
					resultHolder.set(filteredRes);
				}
			} catch (Throwable t) {
				if (repoResult != null) {
					repoResult.close();
				}
				throw t;
			}

		});
	}

	@Override
	public CloseableIteration<Statement> getStatements(
			Resource subj, IRI pred, Value obj, QueryInfo queryInfo, Resource... contexts)
			throws RepositoryException,
			MalformedQueryException, QueryEvaluationException {

		return withConnection((conn, resultHolder) -> {

			RepositoryResult<Statement> repoResult = null;
			try {
				repoResult = conn.getStatements(subj, pred, obj,
						queryInfo.getIncludeInferred(), contexts);

				resultHolder.set(repoResult);
			} catch (Throwable t) {
				if (repoResult != null) {
					repoResult.close();
				}
				throw t;
			}

		});
	}

	@Override
	public boolean hasStatements(StatementPattern stmt,
			BindingSet bindings, QueryInfo queryInfo, Dataset dataset)
			throws RepositoryException, MalformedQueryException,
			QueryEvaluationException {

		Value subjValue = QueryAlgebraUtil.getVarValue(stmt.getSubjectVar(), bindings);
		Value predValue = QueryAlgebraUtil.getVarValue(stmt.getPredicateVar(), bindings);
		Value objValue = QueryAlgebraUtil.getVarValue(stmt.getObjectVar(), bindings);

		Resource[] contexts = FedXUtil.toContexts(dataset);

		try (RepositoryConnection conn = endpoint.getConnection()) {
			return conn.hasStatement((Resource) subjValue, (IRI) predValue, objValue, queryInfo.getIncludeInferred(),
					contexts);
		}
	}

	@Override
	public boolean usePreparedQuery(StatementPattern stmt, QueryInfo queryInfo) {
		// we use a prepared query for variable GRAPH patterns (=> cannot be done
		// using the Repository API).
		if (stmt.getContextVar() != null && !stmt.getContextVar().hasValue()) {
			return true;
		}
		Dataset ds = queryInfo.getDataset();
		if (ds != null) {

			// if FROM NAMED is used we rely on a prepared query
			return !ds.getNamedGraphs().isEmpty();
		}

		// in all other cases: try to use the Repository API
		return false;
	}
}
