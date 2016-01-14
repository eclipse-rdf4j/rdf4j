/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.iterator;

import java.util.Set;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.FilterIteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.Filter;
import org.eclipse.rdf4j.query.algebra.QueryModelNode;
import org.eclipse.rdf4j.query.algebra.SubQueryValueOperator;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryBindingSet;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;

public class FilterIterator extends FilterIteration<BindingSet, QueryEvaluationException> {

	/*-----------*
	 * Constants *
	 *-----------*/

	private final Filter filter;

	private final EvaluationStrategy strategy;

	/**
	 * The set of binding names that are "in scope" for the filter. The filter
	 * must not include bindings that are (only) included because of the
	 * depth-first evaluation strategy in the evaluation of the constraint.
	 */
	private final Set<String> scopeBindingNames;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public FilterIterator(Filter filter, CloseableIteration<BindingSet, QueryEvaluationException> iter,
			EvaluationStrategy strategy)
		throws QueryEvaluationException
	{
		super(iter);
		this.filter = filter;
		this.strategy = strategy;
		this.scopeBindingNames = filter.getBindingNames();
		
	}

	/*---------*
	 * Methods *
	 *---------*/

	private boolean isPartOfSubQuery(QueryModelNode node) {
		if (node instanceof SubQueryValueOperator) {
			return true;
		}
		
		QueryModelNode parent = node.getParentNode();
		if (parent == null) {
			return false;
		}
		else {
			return isPartOfSubQuery(parent);
		}
	}
	
	@Override
	protected boolean accept(BindingSet bindings)
		throws QueryEvaluationException
	{
		try {
			// Limit the bindings to the ones that are in scope for this filter
			QueryBindingSet scopeBindings = new QueryBindingSet(bindings);
			
			// FIXME J1 scopeBindingNames should include bindings from superquery if the filter
			// is part of a subquery. This is a workaround: we should fix the settings of scopeBindingNames, 
			// rather than skipping the limiting of bindings.
			if (!isPartOfSubQuery(filter)) {
				scopeBindings.retainAll(scopeBindingNames);
			}

			return strategy.isTrue(filter.getCondition(), scopeBindings);
		}
		catch (ValueExprEvaluationException e) {
			// failed to evaluate condition
			return false;
		}
	}
}
