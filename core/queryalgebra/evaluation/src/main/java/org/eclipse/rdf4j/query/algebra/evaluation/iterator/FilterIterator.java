/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.iterator;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.FilterIteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.Filter;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryValueEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;

public class FilterIterator extends FilterIteration<BindingSet, QueryEvaluationException> {

	private final QueryValueEvaluationStep condition;
	private final EvaluationStrategy strategy;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public FilterIterator(Filter filter, CloseableIteration<BindingSet, QueryEvaluationException> iter,
			QueryValueEvaluationStep condition, EvaluationStrategy strategy) throws QueryEvaluationException {
		super(iter);
		this.condition = condition;
		this.strategy = strategy;

	}

	@Override
	protected boolean accept(BindingSet bindings) throws QueryEvaluationException {
		try {
			return strategy.isTrue(condition, bindings);
		} catch (ValueExprEvaluationException e) {
			// failed to evaluate condition
			return false;
		}
	}

}
