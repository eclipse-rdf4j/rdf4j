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
import org.eclipse.rdf4j.federated.structures.QueryInfo;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.AbstractQueryModelNode;
import org.eclipse.rdf4j.query.algebra.QueryModelVisitor;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryBindingSet;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a group of {@link ExclusiveTupleExpr} that can only produce results at a single endpoint, the
 * {@link StatementSource}.
 *
 * @author Andreas Schwarte
 *
 */
public class ExclusiveGroup extends AbstractQueryModelNode
		implements StatementTupleExpr, FilterTuple, ExclusiveTupleExpr {
	private static final long serialVersionUID = 9215353191021766797L;

	private static final Logger log = LoggerFactory.getLogger(ExclusiveGroup.class);

	protected final List<ExclusiveTupleExpr> owned = new ArrayList<>();
	protected final StatementSource owner;
	protected final Set<String> freeVars = new HashSet<>();
	protected final String id;
	protected final transient QueryInfo queryInfo;
	protected FilterValueExpr filterExpr = null;
	protected QueryBindingSet boundFilters = null; // contains bound filter bindings, that need to be added as
	// additional bindings
	protected transient Endpoint ownedEndpoint;

	public ExclusiveGroup(Collection<? extends ExclusiveTupleExpr> ownedNodes, StatementSource owner,
			QueryInfo queryInfo) {
		owned.addAll(ownedNodes);
		this.owner = owner;
		init(); // init free vars + filter expr
		this.id = NodeFactory.getNextId();
		this.queryInfo = queryInfo;
		ownedEndpoint = queryInfo.getFederationContext().getEndpointManager().getEndpoint(owner.getEndpointID());

		ownedNodes.forEach(node -> node.setParentNode(this));
	}

	/**
	 * Initialize free variables
	 */
	protected void init() {
		for (ExclusiveTupleExpr o : owned) {
			freeVars.addAll(o.getFreeVars());
		}
	}

	@Override
	public <X extends Exception> void visitChildren(QueryModelVisitor<X> visitor)
			throws X {

		for (ExclusiveTupleExpr s : owned) {
			s.visit(visitor);
		}

		if (boundFilters != null) {
			BoundFiltersNode.visit(visitor, boundFilters);
		}
		if (filterExpr != null) {
			filterExpr.visit(visitor);
		}
	}

	@Override
	public <X extends Exception> void visit(QueryModelVisitor<X> visitor)
			throws X {
		visitor.meetOther(this);
	}

	@Override
	public Set<String> getAssuredBindingNames() {
		Set<String> res = new HashSet<>();
		owned.forEach(e -> res.addAll(e.getAssuredBindingNames()));
		return res;
	}

	@Override
	public Set<String> getBindingNames() {
		Set<String> res = new HashSet<>();
		owned.forEach(e -> res.addAll(e.getBindingNames()));
		return res;
	}

	@Override
	public ExclusiveGroup clone() {
		throw new RuntimeException("Operation not supported on this node!");
	}

	@Override
	public StatementSource getOwner() {
		return owner;
	}

	public Endpoint getOwnedEndpoint() {
		return ownedEndpoint;
	}

	public List<ExclusiveTupleExpr> getExclusiveExpressions() {
		return owned;
	}

	@Override
	public int getFreeVarCount() {
		return freeVars.size();
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
		return Collections.singletonList(owner);
	}

	@Override
	public boolean hasFreeVarsFor(BindingSet bindings) {
		for (String var : freeVars) {
			if (!bindings.hasBinding(var)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(BindingSet bindings)
			throws QueryEvaluationException {

		try {
			// use the particular evaluation strategy for evaluation
			return queryInfo.getStrategy().evaluateExclusiveGroup(this, bindings);
		} catch (RepositoryException | MalformedQueryException e) {
			throw new QueryEvaluationException(e);
		}

	}

	@Override
	public void addFilterExpr(FilterExpr expr) {

		if (filterExpr == null) {
			filterExpr = expr;
		} else if (filterExpr instanceof ConjunctiveFilterExpr) {
			((ConjunctiveFilterExpr) filterExpr).addExpression(expr);
		} else if (filterExpr instanceof FilterExpr) {
			filterExpr = new ConjunctiveFilterExpr((FilterExpr) filterExpr, expr);
		} else {
			throw new RuntimeException("Unexpected type: " + filterExpr.getClass().getCanonicalName());
		}

	}

	@Override
	public FilterValueExpr getFilterExpr() {
		return filterExpr;
	}

	@Override
	public BindingSet getBoundFilters() {
		return this.boundFilters;
	}

	@Override
	public boolean hasFilter() {
		return filterExpr != null;
	}

	@Override
	public void addBoundFilter(final String varName, final Value value) {

		if (!freeVars.contains(varName)) {
			log.debug("Invalid call to addBoundFilter: variable " + varName + " is not known as a free variable");
			return;
		}

		// lazy initialization of bound filters
		if (boundFilters == null) {
			boundFilters = new QueryBindingSet();
		}

		// Note: Var nodes of children are visited in optimizer
		// => i.e. actual values are set

		boundFilters.addBinding(varName, value);

		freeVars.remove(varName);

	}

	@Override
	public QueryInfo getQueryInfo() {
		return this.queryInfo;
	}
}
