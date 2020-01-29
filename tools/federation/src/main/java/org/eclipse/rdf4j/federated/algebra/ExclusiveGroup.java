/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.federated.algebra;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.federated.endpoint.Endpoint;
import org.eclipse.rdf4j.federated.evaluation.FederationEvalStrategy;
import org.eclipse.rdf4j.federated.structures.QueryInfo;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.AbstractQueryModelNode;
import org.eclipse.rdf4j.query.algebra.QueryModelVisitor;
import org.eclipse.rdf4j.repository.RepositoryException;

/**
 * Represents a group of statements that can only produce results at a single endpoint, the owner.
 * 
 * @author Andreas Schwarte
 *
 */
public class ExclusiveGroup extends AbstractQueryModelNode implements StatementTupleExpr, FilterTuple {
	private static final long serialVersionUID = 9215353191021766797L;

	protected final List<ExclusiveStatement> owned = new ArrayList<>();
	protected final ArrayList<StatementSource> owner;
	protected final Set<String> freeVars = new HashSet<>();
	protected final String id;
	protected final transient QueryInfo queryInfo;
	protected FilterValueExpr filter = null;
	protected transient Endpoint ownedEndpoint = null;

	private final FederationEvalStrategy strategy;

	public ExclusiveGroup(Collection<ExclusiveStatement> ownedNodes, StatementSource owner, QueryInfo queryInfo) {
		owned.addAll(ownedNodes);
		this.owner = new ArrayList<>(1);
		this.owner.add(owner);
		init(); // init free vars + filter expr
		this.id = NodeFactory.getNextId();
		this.queryInfo = queryInfo;
		ownedEndpoint = queryInfo.getFederationContext().getEndpointManager().getEndpoint(owner.getEndpointID());

		strategy = queryInfo.getFederationContext().getStrategy();
	}

	/**
	 * Initialize free variables and filter expressions for owned children.
	 */
	protected void init() {
		HashSet<FilterExpr> conjExpr = new HashSet<>();
		for (ExclusiveStatement o : owned) {
			freeVars.addAll(o.getFreeVars());

			if (o.hasFilter()) {

				FilterValueExpr expr = o.getFilterExpr();
				if (expr instanceof ConjunctiveFilterExpr)
					conjExpr.addAll(((ConjunctiveFilterExpr) expr).getExpressions());
				else if (expr instanceof FilterExpr)
					conjExpr.add((FilterExpr) expr);
				else
					throw new RuntimeException(
							"Internal Error: Unexpected filter type: " + expr.getClass().getSimpleName());
			}
		}

		if (conjExpr.size() == 1)
			filter = conjExpr.iterator().next();
		else if (conjExpr.size() > 1) {
			filter = new ConjunctiveFilterExpr(conjExpr);
		}
	}

	@Override
	public <X extends Exception> void visitChildren(QueryModelVisitor<X> visitor)
			throws X {

		for (ExclusiveStatement s : owned) {
			s.visit(visitor);
		}
	}

	@Override
	public <X extends Exception> void visit(QueryModelVisitor<X> visitor)
			throws X {
		visitor.meetOther(this);
	}

	@Override
	public Set<String> getAssuredBindingNames() {
		return Collections.emptySet();
	}

	@Override
	public Set<String> getBindingNames() {
		return Collections.emptySet();
	}

	@Override
	public ExclusiveGroup clone() {
		throw new RuntimeException("Operation not supported on this node!");
	}

	public StatementSource getOwner() {
		return owner.get(0);
	}

	public Endpoint getOwnedEndpoint() {
		return ownedEndpoint;
	}

	public List<ExclusiveStatement> getStatements() {
		// XXX make a copy? (or copyOnWrite list?)
		return owned;
	}

	@Override
	public int getFreeVarCount() {
		return freeVars.size();
	}

	public Set<String> getFreeVarsSet() {
		return freeVars;
	}

	@Override
	public List<String> getFreeVars() {
		return new ArrayList<>(freeVars);
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public List<StatementSource> getStatementSources() {
		return owner;
	}

	@Override
	public boolean hasFreeVarsFor(BindingSet bindings) {
		for (String var : freeVars)
			if (!bindings.hasBinding(var))
				return true;
		return false;
	}

	@Override
	public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(BindingSet bindings)
			throws QueryEvaluationException {

		try {
			// use the particular evaluation strategy for evaluation
			return strategy.evaluateExclusiveGroup(this, bindings);
		} catch (RepositoryException | MalformedQueryException e) {
			throw new QueryEvaluationException(e);
		}

	}

	@Override
	public void addFilterExpr(FilterExpr expr) {
		/*
		 * Note: the operation is obsolete for this class: all filters are added already in the owned children during
		 * optimization (c.f. FilterOptimizer)
		 */
		throw new UnsupportedOperationException("Operation not supported for " + ExclusiveGroup.class.getCanonicalName()
				+ ", filters already to children during optimization.");

	}

	@Override
	public FilterValueExpr getFilterExpr() {
		return filter;
	}

	@Override
	public boolean hasFilter() {
		return filter != null;
	}

	@Override
	public void addBoundFilter(final String varName, final Value value) {
		/*
		 * Note: the operation is obsolete for this class: all bindings are set already in the owned children during
		 * optimization (c.f. FilterOptimizer)
		 */
		throw new UnsupportedOperationException("Operation not supported for " + ExclusiveGroup.class.getCanonicalName()
				+ ", bindings inserted during optimization.");
	}

	@Override
	public QueryInfo getQueryInfo() {
		return this.queryInfo;
	}
}
