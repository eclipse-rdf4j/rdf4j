/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.repository.sparql.query;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.IterationWrapper;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;

/**
 * Adds more bindings to each of the results.
 *
 * @author James Leigh
 */
public class InsertBindingSetCursor extends IterationWrapper<BindingSet, QueryEvaluationException> {

	private final BindingSet bindings;

	public InsertBindingSetCursor(CloseableIteration<BindingSet, QueryEvaluationException> delegate,
			BindingSet bindings) {
		super(delegate);
		this.bindings = bindings;
	}

	@Override
	public BindingSet next() throws QueryEvaluationException {
		BindingSet next = super.next();
		if (next == null) {
			return null;
		}
		int size = bindings.size() + next.size();
		SPARQLQueryBindingSet set = new SPARQLQueryBindingSet(size);
		set.addAll(bindings);
		for (Binding binding : next) {
			set.setBinding(binding);
		}
		return set;
	}

}
