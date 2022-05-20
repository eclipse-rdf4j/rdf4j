/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.impl;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.CloseableIteratorIteration;
import org.eclipse.rdf4j.common.iteration.IterationWrapper;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.TupleQueryResult;

/**
 * An iterating implementation of the {@link TupleQueryResult} interface.
 */
@Deprecated(since = "4.1.0")
public class IteratingTupleQueryResult extends IterationWrapper<BindingSet, QueryEvaluationException>
		implements TupleQueryResult {

	/*-----------*
	 * Variables *
	 *-----------*/

	private final List<String> bindingNames;

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a query result object with the supplied binding names. <em>The supplied list of binding names is assumed
	 * to be constant</em>; care should be taken that the contents of this list doesn't change after supplying it to
	 * this solution.
	 *
	 * @param bindingNames The binding names, in order of projection.
	 */
	public IteratingTupleQueryResult(List<String> bindingNames, Iterable<? extends BindingSet> bindingSets) {
		this(bindingNames, bindingSets.iterator());
	}

	public IteratingTupleQueryResult(List<String> bindingNames, Iterator<? extends BindingSet> bindingSetIter) {
		this(bindingNames, new CloseableIteratorIteration<BindingSet, QueryEvaluationException>(bindingSetIter));
	}

	/**
	 * Creates a query result object with the supplied binding names. <em>The supplied list of binding names is assumed
	 * to be constant</em>; care should be taken that the contents of this list doesn't change after supplying it to
	 * this solution.
	 *
	 * @param bindingNames The binding names, in order of projection.
	 */
	public IteratingTupleQueryResult(List<String> bindingNames,
			CloseableIteration<? extends BindingSet, QueryEvaluationException> bindingSetIter) {
		super(bindingSetIter);
		// Don't allow modifications to the binding names when it is accessed
		// through getBindingNames:
		this.bindingNames = Collections.unmodifiableList(bindingNames);
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	public List<String> getBindingNames() throws QueryEvaluationException {
		return bindingNames;
	}
}
