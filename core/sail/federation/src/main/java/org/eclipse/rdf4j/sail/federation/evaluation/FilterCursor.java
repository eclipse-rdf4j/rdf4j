/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.federation.evaluation;

import java.util.Set;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.EmptySet;
import org.eclipse.rdf4j.query.algebra.Filter;
import org.eclipse.rdf4j.query.algebra.ValueExpr;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.iterator.FilterIterator;

/**
 * Provides a convenient constructor for {@link FilterIterator} using the
 * condition.
 * 
 * @author James Leigh
 */
public class FilterCursor extends FilterIterator {

	public FilterCursor(CloseableIteration<BindingSet, QueryEvaluationException> result, ValueExpr condition,
			final Set<String> scopeBindingNames, EvaluationStrategy strategy)
		throws QueryEvaluationException
	{
		super(new Filter(new EmptySet() {

			@Override
			public Set<String> getBindingNames() {
				return scopeBindingNames;
			}
		}, condition), result, strategy);
	}

}
