/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.iterator;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.LookAheadIteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.MultiProjection;
import org.eclipse.rdf4j.query.algebra.ProjectionElemList;

/**
 * @author Jerven Bolleman
 * @author Arjohn Kampman
 * @author James Leigh
 */
public class MultiProjectionIterator extends LookAheadIteration<BindingSet, QueryEvaluationException> {

	/*------------*
	 * Attributes *
	 *------------*/

	private final List<Function<BindingSet, BindingSet>> convertors;

	private final CloseableIteration<BindingSet, QueryEvaluationException> iter;

	private final BindingSet parentBindings;

	private final BindingSet[] previousBindings;

	private volatile BindingSet currentBindings;

	private volatile int nextProjectionIdx;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public MultiProjectionIterator(MultiProjection multiProjection,
			CloseableIteration<BindingSet, QueryEvaluationException> iter, BindingSet bindings) {
		List<ProjectionElemList> projections = multiProjection.getProjections();
		this.iter = iter;
		this.parentBindings = bindings;
		this.previousBindings = new BindingSet[projections.size()];
		// Initialize the converter lambda's. These will be used to convert each sub projection at a time.
		convertors = projections.stream()
				.map(pel -> ProjectionIterator.createConverter(pel, parentBindings, false))
				.collect(Collectors.toList());

		// initialize out-of-range to enforce a fetch of the first result upon
		// first use
		nextProjectionIdx = -1;
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	protected BindingSet getNextElement() throws QueryEvaluationException {
		while (true) {
			if (isClosed()) {
				return null;
			}

			int projIdx = nextProjectionIdx;

			if (projIdx >= 0 && projIdx < convertors.size()) {
				// Apply next projection in the list
				BindingSet result = convertors.get(projIdx).apply(currentBindings);

				nextProjectionIdx++;

				// ignore duplicates
				if (!result.equals(previousBindings[projIdx])) {
					previousBindings[projIdx] = result;
					return result;
				}
			} else if (iter.hasNext()) {
				// Continue with the next result
				currentBindings = iter.next();
				nextProjectionIdx = 0;
			} else {
				// no more results
				return null;
			}
		}
	}

	@Override
	protected void handleClose() throws QueryEvaluationException {
		try {
			super.handleClose();
		} finally {
			try {
				iter.close();
			} finally {
				nextProjectionIdx = -1;
				Arrays.fill(previousBindings, null);
			}
		}
	}
}
