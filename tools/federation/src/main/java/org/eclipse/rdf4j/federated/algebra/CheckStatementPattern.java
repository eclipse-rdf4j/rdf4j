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

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.EmptyIteration;
import org.eclipse.rdf4j.federated.endpoint.Endpoint;
import org.eclipse.rdf4j.federated.evaluation.TripleSource;
import org.eclipse.rdf4j.federated.evaluation.iterator.SingleBindingSetIteration;
import org.eclipse.rdf4j.federated.structures.QueryInfo;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.QueryModelNode;
import org.eclipse.rdf4j.query.algebra.QueryModelVisitor;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.repository.RepositoryException;

/**
 * A statement pattern with no free variables when provided with some particular BindingSet in evaluate. For evaluation
 * a boolean ASK query is performed.
 *
 * Wraps a StatementTupleExpr
 *
 * @author Andreas Schwarte
 */
public class CheckStatementPattern implements StatementTupleExpr, BoundJoinTupleExpr {

	private static final long serialVersionUID = -4063951571744144255L;

	protected final StatementTupleExpr stmt;
	protected final String id;
	protected final QueryInfo queryInfo;

	private double resultSizeEstimate = -1;
	private double costEstimate = -1;
	private long resultSizeActual = -1;
	private long totalTimeNanosActual = -1;

	public CheckStatementPattern(StatementTupleExpr stmt, QueryInfo queryInfo) {
		super();
		this.stmt = stmt;
		this.id = NodeFactory.getNextId();
		this.queryInfo = queryInfo;
	}

	public StatementPattern getStatementPattern() {
		return (StatementPattern) stmt;
	}

	@Override
	public int getFreeVarCount() {
		return 0;
	}

	@Override
	public List<String> getFreeVars() {
		return Collections.emptyList();
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public List<StatementSource> getStatementSources() {
		return stmt.getStatementSources();
	}

	@Override
	public boolean hasFreeVarsFor(BindingSet binding) {
		return false;
	}

	@Override
	public Set<String> getAssuredBindingNames() {
		return stmt.getAssuredBindingNames();
	}

	@Override
	public Set<String> getBindingNames() {
		return stmt.getBindingNames();
	}

	@Override
	public QueryModelNode getParentNode() {
		return stmt.getParentNode();
	}

	@Override
	public String getSignature() {
		return stmt.getSignature();
	}

	@Override
	public void replaceChildNode(QueryModelNode current,
			QueryModelNode replacement) {
		stmt.replaceChildNode(current, replacement);
	}

	@Override
	public void replaceWith(QueryModelNode replacement) {
		stmt.replaceWith(replacement);
	}

	@Override
	public void setParentNode(QueryModelNode parent) {
		stmt.setParentNode(parent);
	}

	@Override
	public <X extends Exception> void visit(QueryModelVisitor<X> visitor)
			throws X {
		stmt.visit(visitor);
	}

	@Override
	public <X extends Exception> void visitChildren(QueryModelVisitor<X> visitor)
			throws X {
		stmt.visitChildren(visitor);
	}

	@Override
	public CheckStatementPattern clone() {
		throw new RuntimeException("Operation not supported on this node!");
	}

	@Override
	public double getResultSizeEstimate() {
		return resultSizeEstimate;
	}

	@Override
	public void setResultSizeEstimate(double resultSizeEstimate) {
		this.resultSizeEstimate = resultSizeEstimate;
	}

	@Override
	public long getResultSizeActual() {
		return resultSizeActual;
	}

	@Override
	public void setResultSizeActual(long resultSizeActual) {
		this.resultSizeActual = resultSizeActual;
	}

	@Override
	public double getCostEstimate() {
		return costEstimate;
	}

	@Override
	public void setCostEstimate(double costEstimate) {
		this.costEstimate = costEstimate;
	}

	@Override
	public long getTotalTimeNanosActual() {
		return totalTimeNanosActual;
	}

	@Override
	public void setTotalTimeNanosActual(long totalTimeNanosActual) {
		this.totalTimeNanosActual = totalTimeNanosActual;
	}

	@Override
	public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(BindingSet bindings)
			throws QueryEvaluationException {

		StatementPattern st = (StatementPattern) stmt;

		try {
			// return true if at least one endpoint has a result for this binding set
			for (StatementSource source : stmt.getStatementSources()) {
				Endpoint ownedEndpoint = queryInfo.getFederationContext()
						.getEndpointManager()
						.getEndpoint(source.getEndpointID());
				TripleSource t = ownedEndpoint.getTripleSource();
				if (t.hasStatements(st, bindings, queryInfo, queryInfo.getDataset())) {
					return new SingleBindingSetIteration(bindings);
				}
			}
		} catch (RepositoryException | MalformedQueryException e) {
			throw new QueryEvaluationException(e);
		}

		// XXX return NULL instead and add an additional check?
		return new EmptyIteration<>();
	}

	@Override
	public QueryInfo getQueryInfo() {
		return stmt.getQueryInfo();
	}
}
