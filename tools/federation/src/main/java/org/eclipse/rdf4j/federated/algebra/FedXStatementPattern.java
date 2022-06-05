/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.federated.algebra;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.rdf4j.federated.structures.QueryInfo;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.algebra.AbstractQueryModelNode;
import org.eclipse.rdf4j.query.algebra.QueryModelVisitor;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryBindingSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class providing all common functionality for FedX StatementPatterns
 *
 * @author Andreas Schwarte
 * @see StatementSourcePattern
 * @see ExclusiveStatement
 *
 */
public abstract class FedXStatementPattern extends StatementPattern
		implements StatementTupleExpr, FilterTuple, BoundJoinTupleExpr {
	private static final Logger log = LoggerFactory.getLogger(FedXStatementPattern.class);

	private static final long serialVersionUID = 6588020780262348806L;

	protected final List<StatementSource> statementSources = new ArrayList<>();
	protected final String id;
	protected final QueryInfo queryInfo;
	protected final List<String> freeVars = new ArrayList<>(3);
	protected FilterValueExpr filterExpr = null;
	protected QueryBindingSet boundFilters = null; // contains bound filter bindings, that need to be added as
	// additional bindings
	protected long upperLimit = -1; // if set to a positive number, this upper limit is applied to any subquery

	public FedXStatementPattern(StatementPattern node, QueryInfo queryInfo) {
		super(node.getScope(), node.getSubjectVar(), node.getPredicateVar(), node.getObjectVar(), node.getContextVar());
		this.id = NodeFactory.getNextId();
		this.queryInfo = queryInfo;
		initFreeVars();
	}

	@Override
	public <X extends Exception> void visitChildren(QueryModelVisitor<X> visitor)
			throws X {
		super.visitChildren(visitor);
		for (StatementSource s : sort(statementSources)) {
			s.visit(visitor);
		}

		if (boundFilters != null) {
			BoundFiltersNode.visit(visitor, boundFilters);
		}

		if (upperLimit > 0) {
			new UpperLimitNode(upperLimit).visit(visitor);
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

	protected void initFreeVars() {
		if (getSubjectVar().getValue() == null) {
			freeVars.add(getSubjectVar().getName());
		}
		if (getPredicateVar().getValue() == null) {
			freeVars.add(getPredicateVar().getName());
		}
		if (getObjectVar().getValue() == null) {
			freeVars.add(getObjectVar().getName());
		}
	}

	@Override
	public int getFreeVarCount() {
		return freeVars.size();
	}

	@Override
	public List<String> getFreeVars() {
		return freeVars;
	}

	@Override
	public QueryInfo getQueryInfo() {
		return this.queryInfo;
	}

	@Override
	public String getId() {
		return id;
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
	public List<StatementSource> getStatementSources() {
		return statementSources;
	}

	public int getSourceCount() {
		return statementSources.size();
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
	public void addBoundFilter(String varName, Value value) {

		if (!freeVars.contains(varName)) {
			log.debug("Invalid call to addBoundFilter: variable " + varName + " is not known as a free variable");
			return;
		}

		// lazy initialization of bound filters
		if (boundFilters == null) {
			boundFilters = new QueryBindingSet();
		}

		// visit Var nodes and set value for matching var names
		if (getSubjectVar().getName().equals(varName)) {
			getSubjectVar().setValue(value);
		}
		if (getPredicateVar().getName().equals(varName)) {
			getPredicateVar().setValue(value);
		}
		if (getObjectVar().getName().equals(varName)) {
			getObjectVar().setValue(value);
		}

		boundFilters.addBinding(varName, value);

		freeVars.remove(varName);

		// XXX recheck owned source if it still can deliver results, otherwise prune it
		// optimization: keep result locally for this query
		// if no free vars AND hasResults => replace by TrueNode to avoid additional remote requests
	}

	/**
	 * Set the upper limit for this statement expression (i.e. applied in the evaluation to individual subqueries of
	 * this expr)
	 *
	 * @param upperLimit the upper limit, a negative number means unlimited
	 */
	public void setUpperLimit(long upperLimit) {
		this.upperLimit = upperLimit;
	}

	/**
	 *
	 * @return the upper limit or a negative number (meaning no LIMIT)
	 */
	public long getUpperLimit() {
		return this.upperLimit;
	}

	private List<StatementSource> sort(List<StatementSource> stmtSources) {
		List<StatementSource> res = new ArrayList<>(stmtSources);
		res.sort(Comparator.comparing((StatementSource o) -> o.id));
		return res;
	}

	static class UpperLimitNode extends AbstractQueryModelNode {

		private static final long serialVersionUID = -1331709574582152474L;

		private final long upperLimit;

		public UpperLimitNode(long upperLimit) {
			super();
			this.upperLimit = upperLimit;
		}

		@Override
		public String getSignature() {
			return "Upper Limit: " + upperLimit;
		}

		@Override
		public <X extends Exception> void visit(QueryModelVisitor<X> visitor) throws X {
			visitor.meetOther(this);
		}

		@Override
		public <X extends Exception> void visitChildren(QueryModelVisitor<X> visitor) throws X {
			// no-op
		}
	}
}
