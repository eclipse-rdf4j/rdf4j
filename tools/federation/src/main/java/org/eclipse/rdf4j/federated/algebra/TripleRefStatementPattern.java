/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.federated.algebra;

import java.util.HashSet;
import java.util.Set;
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
import org.eclipse.rdf4j.query.algebra.QueryModelVisitor;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.TripleRef;
import org.eclipse.rdf4j.repository.RepositoryException;

/**
 * A {@link FedXStatementPattern} representing a {@link StatementPattern} together with a {@link TripleRef}, i.e. a
 * SPARQL / RDF 1.2 triple term expression.
 */
public class TripleRefStatementPattern extends FedXStatementPattern {

	private static final long serialVersionUID = 841877125206379474L;
	protected final TripleRef tripleRef;
	protected final FederationContext federationContext;

	private Set<String> assuredBindingNames;

	public TripleRefStatementPattern(StatementPattern node, TripleRef tripleRef, QueryInfo queryInfo) {
		super(node, queryInfo);
		this.tripleRef = tripleRef;
		this.federationContext = queryInfo.getFederationContext();
		refineFreeVars();
	}

	protected void refineFreeVars() {
		freeVars.clear();

		// main statement subject
		if (getSubjectVar().getValue() == null) {
			freeVars.add(getSubjectVar().getName());
		}

		// main statement predicate
		if (getPredicateVar().getValue() == null) {
			freeVars.add(getPredicateVar().getName());
		}

		// triple ref subject
		if (tripleRef.getSubjectVar().getValue() == null) {
			freeVars.add(tripleRef.getSubjectVar().getName());
		}

		// triple ref predicate
		if (tripleRef.getPredicateVar().getValue() == null) {
			freeVars.add(tripleRef.getPredicateVar().getName());
		}

		// triple ref object
		if (tripleRef.getObjectVar().getValue() == null) {
			freeVars.add(tripleRef.getObjectVar().getName());
		}
	}

	@Override
	public Set<String> getAssuredBindingNames() {
		Set<String> assuredBindingNames = this.assuredBindingNames;
		if (assuredBindingNames == null) {
			// take assured binding names as defined for StatementPattern
			assuredBindingNames = new HashSet<>();

			if (!getSubjectVar().hasValue()) {
				assuredBindingNames.add(getSubjectVar().getName());
			}
			if (!getPredicateVar().hasValue()) {
				assuredBindingNames.add(getPredicateVar().getName());
			}
			if (getContextVar() != null && !getContextVar().hasValue()) {
				assuredBindingNames.add(getContextVar().getName());
			}

			// Note: ; the statement's object var is an internal join id

			// include triple term component variables
			if (!tripleRef.getSubjectVar().hasValue()) {
				assuredBindingNames.add(tripleRef.getSubjectVar().getName());
			}
			if (!tripleRef.getPredicateVar().hasValue()) {
				assuredBindingNames.add(tripleRef.getPredicateVar().getName());
			}
			if (!tripleRef.getObjectVar().hasValue()) {
				assuredBindingNames.add(tripleRef.getObjectVar().getName());
			}

			this.assuredBindingNames = assuredBindingNames;
		}
		return assuredBindingNames;
	}

	public TripleRef getTripleRef() {
		return tripleRef;
	}

	@Override
	public <X extends Exception> void visitChildren(QueryModelVisitor<X> visitor) throws X {

		super.visitChildren(visitor);

		tripleRef.visit(visitor);
	}

	@Override
	public CloseableIteration<BindingSet> evaluate(BindingSet bindings) throws QueryEvaluationException {

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

				if (t.usePreparedQuery(this, queryInfo)) {

					// queryString needs to be constructed only once for a given bindingset
					if (preparedQuery == null) {
						try {
							preparedQuery = QueryStringUtil.selectQueryString(this, bindings, filterExpr, isEvaluated,
									queryInfo.getDataset());
						} catch (IllegalQueryException e1) {
							/* all vars are bound, this must be handled as a check query, can occur in joins */
							CloseableIteration<BindingSet> res = handleStatementSourcePatternCheck(
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

	protected CloseableIteration<BindingSet> handleStatementSourcePatternCheck(
			BindingSet bindings) throws RepositoryException, MalformedQueryException, QueryEvaluationException {

		// if at least one source has statements, we can return this binding set as result

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
