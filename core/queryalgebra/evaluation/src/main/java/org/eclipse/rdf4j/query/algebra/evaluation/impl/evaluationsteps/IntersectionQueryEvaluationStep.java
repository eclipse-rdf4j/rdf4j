/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.impl.evaluationsteps;

import java.util.Set;
import java.util.function.Function;

import org.eclipse.rdf4j.collection.factory.api.CollectionFactory;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.IntersectIteration;
import org.eclipse.rdf4j.common.iteration.Iteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryEvaluationStep;

/**
 * A step that prepares the arguments of an Intersection operator before execution.
 */
public class IntersectionQueryEvaluationStep implements QueryEvaluationStep {

	private static final class IntersectIterationUsingSetFromCollectionFactory
			extends IntersectIteration<BindingSet, QueryEvaluationException> {
		private final CollectionFactory cf;

		private IntersectIterationUsingSetFromCollectionFactory(Iteration<BindingSet, QueryEvaluationException> arg1,
				Iteration<BindingSet, QueryEvaluationException> arg2, CollectionFactory cf) {
			super(arg1, arg2, false, cf.createSetOfBindingSets());
			this.cf = cf;
		}

		@Override
		protected void handleClose() throws QueryEvaluationException {
			try {
				cf.close();
			} catch (QueryEvaluationException e) {
				super.handleClose();
				throw e;
			}
		}
	}

	private final QueryEvaluationStep leftArg;
	private final Function<BindingSet, DelayedEvaluationIteration> rightArgDelayed;
	private final CollectionFactory collectionFactory;

	public IntersectionQueryEvaluationStep(QueryEvaluationStep leftArg, QueryEvaluationStep rightArg,
			CollectionFactory collectionFactory) {
		this.collectionFactory = collectionFactory;
		this.leftArg = leftArg;
		rightArgDelayed = bs -> new DelayedEvaluationIteration(rightArg, bs);
	}

	@Override
	public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(BindingSet bs) {
		return new IntersectIterationUsingSetFromCollectionFactory(leftArg.evaluate(bs), rightArgDelayed.apply(bs),
				collectionFactory);
	}
}
