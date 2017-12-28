/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.federation.evaluation;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.IterationWrapper;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryBindingSet;

/**
 * Adds more bindings to each of the results.
 * 
 * @author James Leigh
 */
public class InsertBindingSetCursor extends IterationWrapper<BindingSet, QueryEvaluationException> {

	private final BindingSet bindings;

	public InsertBindingSetCursor(CloseableIteration<BindingSet, QueryEvaluationException> delegate,
			BindingSet bindings)
	{
		super(delegate);
		this.bindings = bindings;
	}

	@Override
	public BindingSet next()
		throws QueryEvaluationException
	{
		BindingSet next = super.next();
		QueryBindingSet result;
		if (next == null) {
			result = null; // NOPMD
		}
		else {
			int size = bindings.size() + next.size();
			result = new QueryBindingSet(size);
			result.addAll(bindings);
			for (Binding binding : next) {
				result.setBinding(binding);
			}
		}
		return result;
	}

}
